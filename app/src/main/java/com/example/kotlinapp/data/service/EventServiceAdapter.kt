package com.example.kotlinapp.data.service

import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class EventServiceAdapter {

    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")

    suspend fun getRecommendedEvents(user: User, limit: Long = 5): List<Event> {
        if (user.sportList.isEmpty()) {
            return emptyList() // No hay deportes, no hay recomendaciones.
        }

        val querySnapshot = eventsCollection
            .whereIn("sport", user.sportList)
            .limit(limit)
            .get()
            .await()

        return querySnapshot.toObjects(Event::class.java)
    }

    suspend fun getAllEvents(): List<Event> {
        val querySnapshot = eventsCollection
            .orderBy("start_time", Query.Direction.ASCENDING)
            .get()
            .await()

        return querySnapshot.toObjects(Event::class.java)
    }

    suspend fun getEventsByOrganizer(userId: String): List<Event> {
        val userRef = db.collection("users").document(userId)

        val querySnapshot = eventsCollection
            .whereEqualTo("organizerid", userRef)
            .get()
            .await()

        return querySnapshot.toObjects(Event::class.java)
    }


    fun updateEvent(eventId: String, updates: Map<String, Any>) {
        eventsCollection.document(eventId).update(updates)
    }
}