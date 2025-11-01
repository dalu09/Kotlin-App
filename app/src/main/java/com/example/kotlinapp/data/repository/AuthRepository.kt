package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.serviceadapter.AuthServiceAdapter
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    private val authService = AuthServiceAdapter()

    suspend fun createUserInAuth(email: String, password: String): FirebaseUser {
        return authService.createUserInAuth(email, password)
    }

    suspend fun createUserProfileInFirestore(user: User) {
        authService.createUserProfileInFirestore(user)
    }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        return authService.signIn(email, password)
    }

    suspend fun sendPasswordReset(email: String) {
        authService.sendPasswordReset(email)
    }

    fun isLoggedIn(): Boolean = authService.isLoggedIn()

    fun currentUser(): FirebaseUser? = authService.currentUser()

    fun currentUserId(): String? = authService.currentUserId()

    fun signOut() = authService.signOut()

    suspend fun fetchUserProfile(uid: String): User? {
        return authService.fetchUserProfile(uid)
    }


    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>,
        addedSports: List<String>,
        removedSports: List<String>
    ) {
        authService.updateUserProfile(userId, updates, addedSports, removedSports)
    }
}