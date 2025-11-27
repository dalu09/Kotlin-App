package com.example.kotlinapp.data.service

import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ProfileServiceAdapter {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getUserProfileFlow(uid: String): Flow<User?> = callbackFlow {
        val listenerRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Cierra el Flow
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toObject(User::class.java))
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getUpcomingBookedEvents(userId: String): List<Event> {
        return try {
            val userRef = db.collection("users").document(userId)

            val bookingsSnapshot = db.collection("booked")
                .whereEqualTo("userId", userRef)
                .get()
                .await()

            val eventReferences = bookingsSnapshot.documents.mapNotNull { doc ->
                doc.getDocumentReference("eventId")
            }

            if (eventReferences.isEmpty()) {
                return emptyList()
            }

            val eventsSnapshot = db.collection("events")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), eventReferences)
                .get()
                .await()

            val now = Date()
            eventsSnapshot.toObjects(Event::class.java).filter { event ->
                event.start_time != null && event.start_time.after(now)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileService", "Error fetching upcoming events: ${e.message}", e)
            emptyList()
        }
    }

}