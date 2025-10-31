package com.example.kotlinapp.ui.editprofile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {


    private val authRepository = AuthRepository()


    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile


    private val _updateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val updateState: LiveData<UpdateState> = _updateState


    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    fun loadUserProfile() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId == null) {
                _error.value = "Error: No se pudo encontrar el ID del usuario."
                return@launch
            }

            try {
                val user = authRepository.fetchUserProfile(userId)
                _userProfile.value = user
            } catch (e: Exception) {
                _error.value = "Error al cargar el perfil: ${e.message}"
            }
        }
    }


    fun updateUserProfile(username: String, description: String, sports: List<String>) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            val currentProfile = _userProfile.value
            val userId = authRepository.currentUserId()

            if (userId == null || currentProfile == null) {
                _updateState.value = UpdateState.Error("No se pudo actualizar el perfil.")
                return@launch
            }


            if (username.isBlank() || sports.isEmpty()) {
                _updateState.value = UpdateState.Error("Username y Sports no pueden estar vacíos.")
                return@launch
            }


            val updates = mapOf(
                "username" to username,
                "description" to description,
                "sportList" to sports
            )

            try {

                authRepository.updateUserProfile(userId, updates)
                _updateState.value = UpdateState.Success
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("Error al guardar: ${e.message}")
            }
        }
    }


    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }
}