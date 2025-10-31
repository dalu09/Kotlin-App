package com.example.kotlinapp.data.repository

import android.location.Location
import android.os.Bundle
import android.util.Log
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.service.EventServiceAdapter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventRepository {

    companion object { private const val TAG = "EventRepository" }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val analytics: FirebaseAnalytics = Firebase.analytics

    private val eventServiceAdapter: EventServiceAdapter = object : EventServiceAdapter {
        override suspend fun getAllEvents(): List<Event> = coroutineScope {
            val eventsSnapshot = db.collection("events").get().await()
            val events = eventsSnapshot.toObjects(Event::class.java)
            events.map {
                async {
                    it.venueid?.get()?.await()?.toObject(Venue::class.java)?.let { venue ->
                        it.location = GeoPoint(venue.latitude, venue.longitude)
                    }
                    it
                }
            }.awaitAll()
        }

        override suspend fun getRecommendedEvents(user: User, limit: Long): List<Event> {
            if (user.sportList.isEmpty()) return emptyList()

            val querySnapshot = db.collection("events")
                .whereIn("sport", user.sportList)
                .whereGreaterThan("start_time", Date())
                .orderBy("start_time", Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .await()
            return querySnapshot.toObjects(Event::class.java)
        }
    }


    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val snapshot = db.collection("events").document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)
            if (event != null) Result.success(event)
            else Result.failure(Exception("Evento no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double): Result<List<Event>> {
        return try {
            val eventsSnapshot = db.collection("events").get().await()
            val events = eventsSnapshot.toObjects(Event::class.java)

            val eventsWithLocations = coroutineScope {
                events.map { event ->
                    async {
                        event.venueid?.get()?.await()?.toObject(Venue::class.java)?.let { venue ->
                            event.location = GeoPoint(venue.latitude, venue.longitude)
                        }
                        event
                    }
                }.awaitAll()
            }

            val nearby = eventsWithLocations.filter { event ->
                event.location?.let { loc ->
                    val d = FloatArray(1)
                    Location.distanceBetween(userLocation.latitude, userLocation.longitude, loc.latitude, loc.longitude, d)
                    d[0] <= radiusInMeters
                } ?: false
            }
            Result.success(nearby)
        } catch (e: Exception) {
            Log.e(TAG, "Fallo consulta de eventos: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRecommendedEvents(user: User, limit: Long = 4L): Result<List<Event>> {
        return try {
            val events = eventServiceAdapter.getRecommendedEvents(user, limit)
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al obtener eventos recomendados desde el repo: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val events = eventServiceAdapter.getAllEvents()
            Result.success(events)
        } catch (e: Exception) {
            // Si la consulta falla (ej. por índice faltante), el error se registrará aquí.
            Log.e(TAG, "Fallo al obtener todos los eventos desde el repo: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createBooking(eventId: String, userId: String): Result<Unit> {

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, eventId)
        bundle.putString("user_id", userId)
        analytics.logEvent("booking_completed", bundle)

        return try {
            val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)


            val alreadyBookedQuery = db.collection("booked")
                .whereEqualTo("eventId", eventRef)
                .whereEqualTo("userId", userRef)
                .limit(1)
                .get().await()

            if (!alreadyBookedQuery.isEmpty) {
                return Result.failure(Exception("Ya has reservado este evento."))
            }

            db.runTransaction { transaction ->
                val newBookingRef = db.collection("booked").document()
                val bookingData = mapOf(
                    "eventId" to eventRef,
                    "userId" to userRef,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                transaction.set(newBookingRef, bookingData)

                val yyyyMM = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
                val monthlyUniqueUserRef = db.collection("monthly_unique_bookers")
                    .document(yyyyMM)
                    .collection("users")
                    .document(userId)

                val firstBookingMark = mapOf(
                    "first_booking_at" to FieldValue.serverTimestamp()
                )
                transaction.set(monthlyUniqueUserRef, firstBookingMark, SetOptions.merge())

                null
            }.await()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear la reserva: ${e.message}", e)
            Result.failure(e)
        }
    }
}
