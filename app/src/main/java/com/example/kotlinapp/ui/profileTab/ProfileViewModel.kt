package com.example.kotlinapp.ui.profileTab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.service.ProfileServiceAdapter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val serviceAdapter = ProfileServiceAdapter()

    // único LiveData que contiene todo el estado del usuario.
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // carga de datos.
    init {
        startListeningForUserProfile()
    }

    private fun startListeningForUserProfile() {
        val currentUserId = serviceAdapter.getCurrentUserId()

        if (currentUserId == null) {
            _error.value = "No se encontró un usuario autenticado."
            return
        }

        // consume el Flow del adaptador.
        viewModelScope.launch {
            serviceAdapter.getUserProfileFlow(currentUserId)
                .catch { exception ->
                    _error.value = "Error al obtener perfil: ${exception.message}"
                }
                .collect { userFromFlow ->

                    _user.value = userFromFlow
                    _error.value = null
                }
        }
    }

}
