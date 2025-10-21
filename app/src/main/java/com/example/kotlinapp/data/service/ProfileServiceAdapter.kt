package com.example.kotlinapp.data.service

import com.example.kotlinapp.data.models.User // Asegúrate de tener este modelo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
                    // Cierra el Flow con un error si algo sale mal
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toObject(User::class.java))
            }


        awaitClose { listenerRegistration.remove() }
    }
}

data class User(
    val uid: String = "",
    val username: String? = null,
    val description: String? = null,
    val avgRating: Double = 0.0,
    val numRating: Long = 0,
    val sportList: List<String> = emptyList()
)
