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
            // Creamos una referencia al documento del usuario, que es como se guarda en 'booked'
            val userRef = db.collection("users").document(userId)

            // 1. Consulta la colección 'booked' para encontrar las reservas del usuario
            val bookingsSnapshot = db.collection("booked")
                .whereEqualTo("userId", userRef) // Busca coincidencias exactas con la referencia del usuario
                .get()
                .await()

            // 2. Extrae las referencias a los eventos de los documentos de reserva
            val eventReferences = bookingsSnapshot.documents.mapNotNull { doc ->
                doc.getDocumentReference("eventId")
            }

            if (eventReferences.isEmpty()) {
                return emptyList() // No hay reservas, no hay nada que mostrar
            }

            // 3. Obtiene los detalles de todos los eventos en una sola consulta eficiente
            val eventsSnapshot = db.collection("events")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), eventReferences)
                .get()
                .await()

            // 4. Convierte a objetos 'Event' y filtra solo los que tienen fecha de inicio futura
            val now = Date()
            eventsSnapshot.toObjects(Event::class.java).filter { event ->
                event.start_time != null && event.start_time.after(now)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileService", "Error fetching upcoming events: ${e.message}", e)
            emptyList() // Devuelve una lista vacía en caso de error para evitar crashes
        }
    }

}