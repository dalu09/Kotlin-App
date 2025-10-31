package com.example.kotlinapp.data.service

import android.os.Bundle
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.models.Venue
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified service adapter that provides:
 *  - low-level event/venue reads
 *  - booking transaction
 *  - helper queries (hasUserBooking)
 *  - recommendation and listing helpers
 *
 * Use by creating an instance of FirebaseEventServiceAdapter() or inject it.
 */
interface EventServiceAdapter {
    // Reads
    suspend fun fetchEvent(eventId: String): Event?
    suspend fun fetchAllEvents(): List<Event>
    suspend fun fetchVenue(ref: DocumentReference): Venue?

    // Auxiliary query
    suspend fun hasUserBooking(eventId: String, userId: String): Boolean

    // Writes/transactions
    suspend fun runBookingTransaction(eventId: String, userId: String)

    // Recommendations / helpers
    suspend fun getRecommendedEvents(user: User, limit: Long = 5): List<Event>
    suspend fun getAllEvents(): List<Event>
}

class FirebaseEventServiceAdapter(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val analytics: FirebaseAnalytics = Firebase.analytics
) : EventServiceAdapter {

    private val eventsCol = db.collection("events")
    private val usersCol = db.collection("users")
    private val bookedCol = db.collection("booked")

    // -------- Lectura --------
    override suspend fun fetchEvent(eventId: String): Event? {
        val snap = eventsCol.document(eventId).get().await()
        return snap.toObject(Event::class.java)
    }

    override suspend fun fetchAllEvents(): List<Event> {
        val snap = eventsCol.get().await()
        return snap.toObjects(Event::class.java)
    }

    override suspend fun fetchVenue(ref: DocumentReference): Venue? {
        val snap = ref.get().await()
        return if (snap.exists()) snap.toObject(Venue::class.java) else null
    }

    // -------- Consultas auxiliares --------
    override suspend fun hasUserBooking(eventId: String, userId: String): Boolean {
        val eventRef = eventsCol.document(eventId)
        val userRef = usersCol.document(userId)

        val q = db.collection("booked")
            .whereEqualTo("eventId", eventRef)
            .whereEqualTo("userId", userRef)
            .limit(1)
            .get()
            .await()

        return !q.isEmpty
    }

    // -------- Escrituras (transacción) --------
    override suspend fun runBookingTransaction(eventId: String, userId: String) {
        // Analytics
        analytics.logEvent("booking_completed", Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, eventId)
            putString("user_id", userId)
        })

        db.runTransaction { tx ->
            val eventRef = eventsCol.document(eventId)
            val eventSnap = tx.get(eventRef)
            if (!eventSnap.exists()) throw Exception("Evento no existe")

            // Optionally check capacity before increasing (read current booked and max_capacity)
            val currentBooked = eventSnap.getLong("booked") ?: 0L
            val maxCapacity = eventSnap.getLong("max_capacity") ?: Long.MAX_VALUE
            if (currentBooked >= maxCapacity) {
                throw Exception("Event is full")
            }

            // Crear booking
            val bookingRef = bookedCol.document()
            tx.set(
                bookingRef,
                mapOf(
                    "eventId" to eventRef,
                    "userId" to usersCol.document(userId),
                    "timestamp" to Timestamp.now()
                )
            )

            // Increment booked atomically
            tx.update(eventRef, "booked", FieldValue.increment(1))

            // Marcar usuario único del mes
            val yyyyMM = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
            val monthlyRef = db.collection("monthly_unique_bookers")
                .document(yyyyMM)
                .collection("users")
                .document(userId)

            tx.set(
                monthlyRef,
                mapOf("first_booking_at" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
            null
        }.await()
    }

    // -------- Recomendaciones / Listados --------
    override suspend fun getRecommendedEvents(user: User, limit: Long): List<Event> {
        if (user.sportList.isEmpty()) {
            return emptyList()
        }

        // Firestore whereIn supports up to 10 values — ensure user.sportList size <= 10 or trim
        val sports = if (user.sportList.size > 10) user.sportList.take(10) else user.sportList

        val querySnapshot = eventsCol
            .whereIn("sport", sports)
            .limit(limit)
            .get()
            .await()

        return querySnapshot.toObjects(Event::class.java)
    }

    override suspend fun getAllEvents(): List<Event> {
        val querySnapshot = eventsCol
            .orderBy("start_time", Query.Direction.ASCENDING)
            .get()
            .await()

        return querySnapshot.toObjects(Event::class.java)
    }
}
