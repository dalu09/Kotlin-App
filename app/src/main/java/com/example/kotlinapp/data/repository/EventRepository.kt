package com.example.kotlinapp.data.repository

import android.location.Location
import android.util.Log
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
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

    suspend fun createBooking(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)

            // Previene que un usuario reserve el mismo evento dos veces
            val alreadyBookedQuery = db.collection("booked")
                .whereEqualTo("eventId", eventRef)
                .whereEqualTo("userId", userRef)
                .limit(1)
                .get().await()

            if (!alreadyBookedQuery.isEmpty) {
                return Result.failure(Exception("Ya has reservado este evento."))
            }

            // Transacción para crear la reserva y marcar al usuario como activo del mes
            db.runTransaction { transaction ->
                // Crea el documento de reserva en la colección 'booked'
                val newBookingRef = db.collection("booked").document()
                val bookingData = mapOf(
                    "eventId" to eventRef,
                    "userId" to userRef,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                transaction.set(newBookingRef, bookingData)

                // Marca al usuario como único para el mes actual.
                val yyyyMM = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
                val monthlyUniqueUserRef = db.collection("monthly_unique_bookers")
                    .document(yyyyMM)
                    .collection("users")
                    .document(userId)

                // SetOptions.merge() crea el documento una vez por mes por usuario.
                val firstBookingMark = mapOf(
                    "first_booking_at" to FieldValue.serverTimestamp()
                )
                transaction.set(monthlyUniqueUserRef, firstBookingMark, SetOptions.merge())

                null // La transacción fue exitosa
            }.await()

            // Envía una señal a Google Analytics
            Firebase.analytics.logEvent("booking_completed") {
                param("event_id", eventId)
                param("user_id", userId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear la reserva: ${e.message}", e)
            Result.failure(e)
        }
    }
}
