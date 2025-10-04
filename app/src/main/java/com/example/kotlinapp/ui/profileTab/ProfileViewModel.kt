// ProfileViewModel.kt
package com.example.kotlinapp.ui.profileTab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUserProfile() {
        _isLoading.value = true
        val currentUser = auth.currentUser

        if (currentUser == null) {
            _error.value = "No se encontró un usuario autenticado."
            _isLoading.value = false
            return
        }

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    _username.value = document.getString("username")
                    _description.value = document.getString("description")
                    _avgRating.value = document.getDouble("avg_rating")

                    _sportList.value = document.get("sport_list") as? List<String> ?: emptyList()


                    _error.value = null
                } else {
                    _error.value = "No se encontró el perfil del usuario."
                }
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _error.value = "Error al obtener el perfil: ${exception.message}"
                _isLoading.value = false
            }
    }
}
