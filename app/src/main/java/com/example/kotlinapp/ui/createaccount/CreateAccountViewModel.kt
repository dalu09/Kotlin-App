package com.example.kotlinapp.ui.createaccount

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.models.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class CreateAccountViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _profileCreationState = MutableLiveData<ProfileCreationState>(ProfileCreationState.Idle)
    val profileCreationState: LiveData<ProfileCreationState> = _profileCreationState


    fun createAccount(uid: String, username: String, sports: List<String>, description: String) {
        val email = Firebase.auth.currentUser?.email ?: ""


        if (username.isBlank() || sports.isEmpty()) {
            _profileCreationState.value = ProfileCreationState.Error("Username y Sports son campos obligatorios.")
            return
        }

        viewModelScope.launch {
            try {
                _profileCreationState.value = ProfileCreationState.Loading


                val newUser = User(
                    uid = uid,
                    username = username,
                    email = email,
                    description = description,
                    sportList = sports
                )

                authRepository.createUserProfileInFirestore(newUser)

                _profileCreationState.value = ProfileCreationState.Success

            } catch (e: Exception) {
                _profileCreationState.value = ProfileCreationState.Error(e.message ?: "Error al crear el perfil.")
            }
        }
    }

    sealed class ProfileCreationState {
        object Idle : ProfileCreationState()
        object Loading : ProfileCreationState()
        object Success : ProfileCreationState()
        data class Error(val message: String) : ProfileCreationState()
    }
}