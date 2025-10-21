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

    // Usamos un único LiveData que contiene todo el estado del usuario.
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    // LiveData para manejar errores
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // El init es un buen lugar para empezar a cargar datos.
    init {
        startListeningForUserProfile()
    }

    private fun startListeningForUserProfile() {
        // Obtenemos el ID del usuario a través del adaptador
        val currentUserId = serviceAdapter.getCurrentUserId()

        if (currentUserId == null) {
            _error.value = "No se encontró un usuario autenticado."
            return
        }

        // Lanzamos una corrutina para consumir el Flow del adaptador.
        viewModelScope.launch {
            serviceAdapter.getUserProfileFlow(currentUserId)
                .catch { exception ->
                    // Si el Flow emite un error, lo capturamos aquí.
                    _error.value = "Error al obtener perfil: ${exception.message}"
                }
                .collect { userFromFlow ->
                    // CADA VEZ que el Flow emite un nuevo perfil (por un cambio en DB),
                    // actualizamos nuestro LiveData. La UI reaccionará a esto.
                    _user.value = userFromFlow
                    _error.value = null // Limpiamos errores si todo fue bien
                }
        }
    }

}
