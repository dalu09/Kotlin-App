package com.example.kotlinapp.data.service

import android.os.Bundle
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface EventServiceAdapter {
    // Lectura
    suspend fun fetchEvent(eventId: String): Event?
    suspend fun fetchAllEvents(): List<Event>
    suspend fun fetchVenue(ref: DocumentReference): Venue?

    // Consulta auxiliar
    suspend fun hasUserBooking(eventId: String, userId: String): Boolean

    // Escritura
    suspend fun runBookingTransaction(eventId: String, userId: String)
}

class FirebaseEventServiceAdapter(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val analytics: FirebaseAnalytics = Firebase.analytics
) : EventServiceAdapter {

    // -------- Lectura --------
    override suspend fun fetchEvent(eventId: String): Event? {
        val snap = db.collection("events").document(eventId).get().await()
        return snap.toObject(Event::class.java)
    }

    override suspend fun fetchAllEvents(): List<Event> {
        val snap = db.collection("events").get().await()
        return snap.toObjects(Event::class.java)
    }

    override suspend fun fetchVenue(ref: DocumentReference): Venue? {
        val snap = ref.get().await()
        return if (snap.exists()) snap.toObject(Venue::class.java) else null
    }

    // -------- Consultas auxiliares --------
    override suspend fun hasUserBooking(eventId: String, userId: String): Boolean {
        val eventRef = db.collection("events").document(eventId)
        val userRef  = db.collection("users").document(userId)

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

        val eventsCol = db.collection("events")
        val bookedCol = db.collection("booked")

        db.runTransaction { tx ->
            val eventRef = eventsCol.document(eventId)
            val eventSnap = tx.get(eventRef)
            if (!eventSnap.exists()) throw Exception("Evento no existe")

            // Crear booking
            val bookingRef = bookedCol.document()
            tx.set(
                bookingRef,
                mapOf(
                    "eventId" to eventRef,
                    "userId" to db.collection("users").document(userId),
                    "timestamp" to Timestamp.now()
                )
            )

            // Validar +1 y capacidad
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
}
