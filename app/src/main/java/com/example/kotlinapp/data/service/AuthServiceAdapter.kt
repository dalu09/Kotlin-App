package com.example.kotlinapp.data.serviceadapter

import com.example.kotlinapp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthServiceAdapter {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()


    suspend fun createUserInAuth(email: String, password: String): FirebaseUser {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        return authResult.user ?: throw Exception("Error al crear el usuario en Authentication.")
    }

    suspend fun createUserProfileInFirestore(user: User) {
        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(user.uid)
        batch.set(userRef, user)

        for (sport in user.sportList) {
            val sportCountRef = firestore.collection("sport_counts").document(sport)
            val increment = FieldValue.increment(1)
            batch.update(sportCountRef, "userCount", increment)
        }


        batch.commit()
    }


    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("users").document(userId)

            val snapshot = transaction.get(userRef)
            @Suppress("UNCHECKED_CAST")
            val oldSportList = snapshot.get("sportList") as? List<String> ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val newSportList = updates["sportList"] as? List<String> ?: emptyList()

            val addedSports = newSportList.filter { !oldSportList.contains(it) }
            val removedSports = oldSportList.filter { !newSportList.contains(it) }

            for (sport in removedSports) {
                val sportCountRef = firestore.collection("sport_counts").document(sport)
                val decrement = FieldValue.increment(-1)
                transaction.update(sportCountRef, "userCount", decrement)
            }

            for (sport in addedSports) {
                val sportCountRef = firestore.collection("sport_counts").document(sport)
                val increment = FieldValue.increment(1)
                transaction.update(sportCountRef, "userCount", increment)
            }

            transaction.update(userRef, updates)

        }

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