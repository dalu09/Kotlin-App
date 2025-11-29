package com.example.kotlinapp.data.repository

import androidx.collection.LruCache
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.serviceadapter.AuthServiceAdapter
import com.google.firebase.auth.FirebaseUser

class AuthRepository {

    private val authService = AuthServiceAdapter()

    private val userCache = LruCache<String, User>(5)

    suspend fun createUserInAuth(email: String, password: String): FirebaseUser {
        return authService.createUserInAuth(email, password)
    }

    suspend fun createUserProfileInFirestore(user: User) {
        authService.createUserProfileInFirestore(user)
        userCache.put(user.uid, user)
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

    fun signOut() {
        userCache.evictAll()
        authService.signOut()
    }

    suspend fun fetchUserProfile(uid: String): User? {
        val cachedUser = userCache.get(uid)
        if (cachedUser != null) {
            return cachedUser
        }

        val fetchedUser = authService.fetchUserProfile(uid)
        fetchedUser?.let {
            userCache.put(uid, it)
        }

        return fetchedUser
    }

    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>,
        addedSports: List<String>,
        removedSports: List<String>
    ) {
        authService.updateUserProfile(userId, updates, addedSports, removedSports)
        userCache.remove(userId)
    }
}