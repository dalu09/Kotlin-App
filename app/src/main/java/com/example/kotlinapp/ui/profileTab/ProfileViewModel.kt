package com.example.kotlinapp.ui.profileTab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _avgRating = MutableLiveData<Double>()
    val avgRating: LiveData<Double> = _avgRating

    private val _sportList = MutableLiveData<List<String>>()
    val sportList: LiveData<List<String>> = _sportList

    private val _numRating = MutableLiveData<Long>()
    val numRating: LiveData<Long> = _numRating

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var userProfileListener: ListenerRegistration? = null

    fun startListeningForUserProfile() {
        _isLoading.value = true
        val currentUser = auth.currentUser

        if (currentUser == null) {
            _error.value = "No se encontró un usuario autenticado."
            _isLoading.value = false
            return
        }

        // addSnapshotListener para una actualización en tiempo real
        userProfileListener = db.collection("users").document(currentUser.uid)
            .addSnapshotListener { document, firestoreError ->

                if (firestoreError != null) {
                    _error.value = "Error al escuchar cambios en el perfil: ${firestoreError.message}"
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    _username.value = document.getString("username")
                    _description.value = document.getString("description")
                    _avgRating.value = document.getDouble("avgRating") ?: 0.0
                    _numRating.value = document.getLong("numRating") ?: 0L

                    val rawSportList = document.get("sportList")
                    if (rawSportList is List<*>) {
                        _sportList.value = rawSportList.mapNotNull { it as? String }
                    } else {
                        _sportList.value = emptyList()
                    }

                    _error.value = null

                } else {
                    _error.value = "Perfil no encontrado. Esperando datos..."
                }

                _isLoading.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
    }
}
