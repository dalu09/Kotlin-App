package com.example.kotlinapp.data.service

import com.example.kotlinapp.data.models.User
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
                    // Cierra el Flow
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot?.toObject(User::class.java))
            }

        awaitClose { listenerRegistration.remove() }
    }
}