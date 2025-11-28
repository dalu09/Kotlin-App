package com.example.kotlinapp.data.repository

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.service.EventServiceAdapter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.DocumentReference
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

sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Stale<T>(val data: T) : RepositoryResult<T>()
    data class Error(val message: String) : RepositoryResult<Nothing>()
}

class EventRepository(private val context: Context) {

    companion object { private const val TAG = "EventRepository" }

    private val eventServiceAdapter = EventServiceAdapter()
    private val analytics: FirebaseAnalytics = Firebase.analytics
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var cachedEvents: List<Event>? = null

    suspend fun getSports(): List<String> {
        val snapshot = db.collection("sport_counts").get().await()
        return snapshot.documents.map { it.id }
    }

    suspend fun getSkillLevels(): List<String> {
        return listOf("Rookie", "Amateur", "Mid-Level", "Pro")
    }

    suspend fun getVenues(): List<Venue> {
        val snapshot = db.collection("venues").get().await()
        return snapshot.toObjects(Venue::class.java)
    }

    suspend fun createEvent(event: Event): Result<Unit> {
        return try {
            db.collection("events").add(event).await()
            cachedEvents = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear el evento: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun getAllEvents(): RepositoryResult<List<Event>> {
        if (cachedEvents != null) {
            Log.d(TAG, "Devolviendo ${cachedEvents!!.size} eventos de la caché.")
            return if (isOnline()) RepositoryResult.Success(cachedEvents!!) else RepositoryResult.Stale(cachedEvents!!)
        }

        if (isOnline()) {
            return try {
                Log.d(TAG, "Caché vacía y hay conexión. Obteniendo datos de la red.")
                val baseEvents = eventServiceAdapter.getAllEvents()

                Log.d(TAG, "Enriqueciendo ${baseEvents.size} eventos con datos de ubicación.")
                val enrichedEvents = enrichEventsWithLocation(baseEvents)

                cachedEvents = enrichedEvents
                RepositoryResult.Success(enrichedEvents)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener o enriquecer eventos: ${e.message}", e)
                RepositoryResult.Error("Error al obtener eventos: ${e.message}")
            }
        } else {
            return RepositoryResult.Error("No hay conexión y la caché está vacía.")
        }
    }

    private suspend fun enrichEventsWithLocation(events: List<Event>): List<Event> = coroutineScope {
        events.map { event ->
            async {
                event.venueid?.get()?.await()?.toObject(Venue::class.java)?.let { venue ->
                    event.location = GeoPoint(venue.latitude, venue.longitude)
                }
                event
            }
        }.awaitAll()
    }

    suspend fun getNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double): RepositoryResult<List<Event>> {
        val allEventsResult = getAllEvents()

        return when (allEventsResult) {
            is RepositoryResult.Success -> {
                val nearby = filterNearby(allEventsResult.data, userLocation, radiusInMeters)
                RepositoryResult.Success(nearby)
            }
            is RepositoryResult.Stale -> {
                val nearby = filterNearby(allEventsResult.data, userLocation, radiusInMeters)
                RepositoryResult.Stale(nearby)
            }
            is RepositoryResult.Error -> allEventsResult
        }
    }

    private fun filterNearby(events: List<Event>, userLocation: GeoPoint, radiusInMeters: Double): List<Event> {
        return events.filter { event ->
            event.location?.let { loc ->
                val d = FloatArray(1)
                Location.distanceBetween(userLocation.latitude, userLocation.longitude, loc.latitude, loc.longitude, d)
                d[0] <= radiusInMeters
            } ?: false
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    suspend fun getRecommendedEvents(user: User, limit: Long = 4L): Result<List<Event>> {
        return try {
            val events = eventServiceAdapter.getRecommendedEvents(user, limit)
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al obtener eventos recomendados: ${e.message}", e)
            Result.failure(e)
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

    fun isDeviceOnline(): Boolean {
        return isOnline()
    }


    suspend fun createBooking(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)

            // 1. Verificar si ya reservó
            val alreadyBookedQuery = db.collection("booked")
                .whereEqualTo("eventId", eventRef)
                .whereEqualTo("userId", userRef)
                .limit(1)
                .get().await()

            if (!alreadyBookedQuery.isEmpty) {
                return Result.failure(Exception("Ya has reservado este evento."))
            }

            analytics.logEvent("booking_completed", Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, eventId)
                putString("user_id", userId)
            })

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


                transaction.update(eventRef, "booked", FieldValue.increment(1))

                null
            }.await()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear la reserva: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getVenueByReference(venueRef: DocumentReference): Result<Venue?> {
        return try {
            val venueSnapshot = venueRef.get().await()
            val venue = venueSnapshot.toObject(Venue::class.java)
            Result.success(venue)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching venue by reference: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun cancelBooking(eventId: String, userId: String): Result<Unit> {
        return try {        val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)

            // 1. Primero, encontramos el documento de la reserva que vamos a eliminar.
            val bookingQuery = db.collection("booked")
                .whereEqualTo("eventId", eventRef)
                .whereEqualTo("userId", userRef)
                .limit(1)
                .get()
                .await()

            if (bookingQuery.isEmpty) {
                // Si no se encuentra la reserva, no se puede cancelar.
                return Result.failure(Exception("No se encontró una reserva para este evento."))
            }

            val bookingDocToDelete = bookingQuery.documents.first()

            // 2. Ejecutamos una transacción para garantizar que ambas operaciones (borrar y decrementar) se completen.
            db.runTransaction { transaction ->
                // Decrementa el contador 'booked' en el documento del evento.
                transaction.update(eventRef, "booked", FieldValue.increment(-1))

                // Elimina el documento de la reserva en la colección 'booked'.
                transaction.delete(bookingDocToDelete.reference)

                null // Éxito de la transacción
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar la reserva: ${e.message}", e)
            Result.failure(Exception("Error al cancelar la reserva: ${e.message}"))
        }
    }
}