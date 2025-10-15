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


    suspend fun signIn(email: String, password: String): FirebaseUser {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        return authResult.user ?: throw Exception("No fue posible iniciar sesión.")
    }

    suspend fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

    fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    fun signOut() = firebaseAuth.signOut()


    suspend fun fetchUserProfile(uid: String): User? {
        val snap = firestore.collection("users").document(uid).get().await()
        return snap.toObject(User::class.java)
    }
}