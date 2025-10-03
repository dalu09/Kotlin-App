package com.example.kotlinapp.data

import com.example.kotlinapp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()


    suspend fun createUserInAuth(email: String, password: String): FirebaseUser {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        return authResult.user ?: throw Exception("Error al crear el usuario en Authentication.")
    }


    suspend fun createUserProfileInFirestore(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }
}