package com.example.kotlinapp.data.repository

import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.collection.ArrayMap
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.service.EventServiceAdapter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
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

    private val reservedEventsCache = ArrayMap<String, Event>()
    private var cachedEvents: List<Event>? = null
    private val bookedStatusCache = ArrayMap<String, Boolean>()

    private suspend fun enrichEvent(event: Event): Event {
        return try {
            event.venueid?.get()?.await()?.toObject(Venue::class.java)?.let {
                venue -> event.copy(location = GeoPoint(venue.latitude, venue.longitude))
            } ?: event
        } catch (e: Exception) {
            Log.e(TAG, "Error enriching event ${event.id}: ${e.message}")
            event // Return original event on failure
        }
    }

    private suspend fun enrichEventsWithLocation(events: List<Event>): List<Event> = coroutineScope {
        events.map { event ->
            async { enrichEvent(event) }
        }.awaitAll()
    }

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
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection to create the event."))
        }
        return try {
            db.collection("events").add(event).await()
            cachedEvents = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating event: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(eventId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            eventServiceAdapter.updateEvent(eventId, updates)
            cachedEvents = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllEvents(): RepositoryResult<List<Event>> {
        if (!isOnline()) {
            val bookedEvents = reservedEventsCache.values.toList()
            return RepositoryResult.Stale(bookedEvents)
        }

        return try {
            val baseEvents = eventServiceAdapter.getAllEvents()
            val enrichedEvents = enrichEventsWithLocation(baseEvents)
            cachedEvents = enrichedEvents
            enrichedEvents.forEach {
                if (reservedEventsCache.containsKey(it.id)) {
                    reservedEventsCache[it.id] = it
                }
            }
            RepositoryResult.Success(enrichedEvents)
        } catch (e: Exception) {
            cachedEvents?.let {
                return RepositoryResult.Stale(it)
            }
            return RepositoryResult.Error("Error fetching events and no cache available: ${e.message}")
        }
    }

    suspend fun getNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double): RepositoryResult<List<Event>> {
        val allEventsResult = getAllEvents()
        return when (allEventsResult) {
            is RepositoryResult.Success -> RepositoryResult.Success(filterNearby(allEventsResult.data, userLocation, radiusInMeters))
            is RepositoryResult.Stale -> RepositoryResult.Stale(filterNearby(allEventsResult.data, userLocation, radiusInMeters))
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

    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        if (!isOnline()) return Result.failure(Exception("No connection to get recommendations."))
        return try {
            Result.success(eventServiceAdapter.getRecommendedEvents(user, limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostedEvents(userId: String): Result<List<Event>> {
        return try {
            val events = eventServiceAdapter.getEventsByOrganizer(userId)
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posted events: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getEventById(eventId: String): Result<Event> {
        reservedEventsCache[eventId]?.let { return Result.success(it) }
        cachedEvents?.find { it.id == eventId }?.let { return Result.success(it) }

        if (!isOnline()) {
            return Result.failure(Exception("Offline and event not found in cache."))
        }

        return try {
            val snapshot = db.collection("events").document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)
            if (event != null) {
                val enrichedEvent = enrichEvent(event)
                Result.success(enrichedEvent)
            } else {
                Result.failure(Exception("Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBooking(eventId: String, userId: String): Result<Unit> {
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection to make a reservation."))
        }
        return try {
            val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)
            if (hasBooking(eventId, userId).getOrDefault(false)) {
                 return Result.failure(Exception("You have already booked this event."))
            }
            analytics.logEvent("booking_completed", Bundle().apply { putString(FirebaseAnalytics.Param.ITEM_ID, eventId); putString("user_id", userId) })
            db.runTransaction { transaction ->
                val newBookingRef = db.collection("booked").document()
                transaction.set(newBookingRef, mapOf("eventId" to eventRef, "userId" to userRef, "timestamp" to FieldValue.serverTimestamp()))
                val yyyyMM = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
                val monthlyUniqueUserRef = db.collection("monthly_unique_bookers").document(yyyyMM).collection("users").document(userId)
                transaction.set(monthlyUniqueUserRef, mapOf("first_booking_at" to FieldValue.serverTimestamp()), SetOptions.merge())
                transaction.update(eventRef, "booked", FieldValue.increment(1))
                null
            }.await()
            val freshEvent = db.collection("events").document(eventId).get().await().toObject(Event::class.java)
            if (freshEvent != null) {
                val enrichedEvent = enrichEvent(freshEvent)
                reservedEventsCache[eventId] = enrichedEvent
                bookedStatusCache[eventId] = true
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun hasBooking(eventId: String, userId: String): Result<Boolean> {
        if (bookedStatusCache.containsKey(eventId)) {
            return Result.success(bookedStatusCache[eventId]!!)
        }
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection to check booking status."))
        }
        return try {
            val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)
            val bookingQuery = db.collection("booked")
                .whereEqualTo("eventId", eventRef)
                .whereEqualTo("userId", userRef)
                .limit(1)
                .get()
                .await()
            val hasBooking = !bookingQuery.isEmpty
            bookedStatusCache[eventId] = hasBooking
            Result.success(hasBooking)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for existing booking: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun populateUserBookings(userId: String) {
        if (!isOnline()) {
            Log.d(TAG, "Offline, cannot populate booking cache from network.")
            return
        }
        try {
            val userRef = db.collection("users").document(userId)
            val snapshot = db.collection("booked").whereEqualTo("userId", userRef).get().await()

            val eventRefs = snapshot.documents.mapNotNull { it.getDocumentReference("eventId") }

            if (eventRefs.isEmpty()) {
                Log.d(TAG, "User has no booked events to cache.")
                clearBookingCache()
                return
            }

            val eventSnapshots = db.collection("events").whereIn(FieldPath.documentId(), eventRefs.map { it.id }).get().await()
            val fetchedEvents = eventSnapshots.toObjects(Event::class.java)
            val enrichedEvents = enrichEventsWithLocation(fetchedEvents)

            val newReservedCache = ArrayMap<String, Event>()
            val newBookedStatusCache = ArrayMap<String, Boolean>()
            enrichedEvents.forEach { event ->
                newReservedCache[event.id] = event
                newBookedStatusCache[event.id] = true
            }
            reservedEventsCache.clear()
            reservedEventsCache.putAll(newReservedCache as Map<String, Event>)
            bookedStatusCache.clear()
            bookedStatusCache.putAll(newBookedStatusCache as Map<String, Boolean>)

            Log.d(TAG, "Booking and Reserved Event caches populated with ${reservedEventsCache.size} events.")

        } catch (e: Exception) {
            Log.e(TAG, "Error populating booking cache: ${e.message}", e)
        }
    }

    fun clearBookingCache() {
        bookedStatusCache.clear()
        reservedEventsCache.clear()
        Log.d(TAG, "Booking and reserved event caches cleared.")
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
        if (!isOnline()) {
            return Result.failure(Exception("No internet connection to cancel booking."))
        }
        return try {
            val eventRef = db.collection("events").document(eventId)
            val userRef = db.collection("users").document(userId)

            val bookingQuery = db.collection("booked")
                .whereEqualTo("eventId", eventRef)
                .whereEqualTo("userId", userRef)
                .limit(1)
                .get()
                .await()

            if (bookingQuery.isEmpty) {
                bookedStatusCache[eventId] = false
                return Result.failure(Exception("Booking not found for this event."))
            }

            val bookingDocToDelete = bookingQuery.documents.first()

            db.runTransaction { transaction ->
                transaction.update(eventRef, "booked", FieldValue.increment(-1))
                transaction.delete(bookingDocToDelete.reference)
                null
            }.await()
            bookedStatusCache.remove(eventId)
            reservedEventsCache.remove(eventId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling booking: ${e.message}", e)
            Result.failure(Exception("Error cancelling booking: ${e.message}"))
        }
    }
}