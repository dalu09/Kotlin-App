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

    // --- NUEVAS FUNCIONES PARA EL FORMULARIO ---

    suspend fun getSports(): List<String> {
        // Se asume que el ID del documento en 'sport_counts' es el nombre del deporte
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

    suspend fun getAllEvents(): RepositoryResult<List<Event>> {
        if (isOnline()) {
            return try {
                Log.d(TAG, "Hay conexión. Obteniendo datos base del EventServiceAdapter.")
                // Obtener los datos de la lista de eventos base (sin ubicación).
                val baseEvents = eventServiceAdapter.getAllEvents()

                // enriquece la lista con los datos de ubicación.
                Log.d(TAG, "Enriqueciendo ${baseEvents.size} eventos con datos de ubicación.")
                val enrichedEvents = enrichEventsWithLocation(baseEvents)

                // 3. Guarda la lista COMPLETA en la caché y la devuelve.
                cachedEvents = enrichedEvents
                RepositoryResult.Success(enrichedEvents)
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener o enriquecer eventos: ${e.message}", e)
                RepositoryResult.Error("Error al obtener eventos: ${e.message}")
            }
        } else {
            return cachedEvents?.let {
                Log.d(TAG, "No hay conexión. Devolviendo datos de la caché.")
                RepositoryResult.Stale(it)
            } ?: RepositoryResult.Error("No hay conexión y la caché está vacía.")
        }
    }

    private suspend fun enrichEventsWithLocation(events: List<Event>): List<Event> = coroutineScope {
        events.map { event ->
            async {
                // Por cada evento, busca su 'venue' para obtener el GeoPoint.
                event.venueid?.get()?.await()?.toObject(Venue::class.java)?.let { venue ->
                    event.location = GeoPoint(venue.latitude, venue.longitude)
                }
                event // Devuelve el evento (modificado o no).
            }
        }.awaitAll() // Espera a que todos los trabajos asíncronos terminen.
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
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
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

    suspend fun createBooking(eventId: String, userId: String): Result<Unit> {
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

                null
            }.await()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al crear la reserva: ${e.message}", e)
            Result.failure(e)
        }
    }
}