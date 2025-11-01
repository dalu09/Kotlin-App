package com.example.kotlinapp.ui.editprofile

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.utils.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()


    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _updateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val updateState: LiveData<UpdateState> = _updateState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    private val _profileImage = MutableLiveData<Bitmap?>(null)
    val profileImage: LiveData<Bitmap?> = _profileImage

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


    fun loadInitialProfileImage() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId == null) {
                _profileImage.value = null // No hay usuario, no hay imagen
                return@launch
            }


            val bitmap = withContext(Dispatchers.IO) {
                FileStorageManager.loadProfileImage(getApplication(), userId)
            }
            _profileImage.value = bitmap
        }
    }


    fun saveProfileImage(bitmap: Bitmap) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId()
            if (userId == null) {
                _error.postValue("Error: No se pudo guardar la imagen (ID de usuario no encontrado).")
                return@launch
            }


            val success = withContext(Dispatchers.IO) {

                FileStorageManager.saveProfileImage(getApplication(), bitmap, userId)
            }

            if (success) {

                _profileImage.postValue(bitmap)
            } else {
                _error.postValue("Error al guardar la imagen localmente.")
            }
        }
    }

    fun updateUserProfile(username: String, description: String, newSportList: List<String>) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            val currentProfile = _userProfile.value
            val userId = authRepository.currentUserId()

            if (userId == null || currentProfile == null) {
                _updateState.value = UpdateState.Error("No se pudo actualizar el perfil.")
                return@launch
            }

            if (username.isBlank() || newSportList.isEmpty()) {
                _updateState.value = UpdateState.Error("Username y Sports no pueden estar vacíos.")
                return@launch
            }

            val oldSportList = currentProfile.sportList
            val addedSports = newSportList.filter { !oldSportList.contains(it) }
            val removedSports = oldSportList.filter { !newSportList.contains(it) }


            val profileUpdates = mapOf(
                "username" to username,
                "description" to description,
                "sportList" to newSportList
            )

            try {
                authRepository.updateUserProfile(userId, profileUpdates, addedSports, removedSports)
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