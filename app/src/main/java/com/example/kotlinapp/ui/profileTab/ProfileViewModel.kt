package com.example.kotlinapp.ui.profileTab

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.EventRepository // AÑADIDO
import com.example.kotlinapp.data.service.ProfileServiceAdapter
import com.example.kotlinapp.utils.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val serviceAdapter = ProfileServiceAdapter()
    private val authRepository = AuthRepository()
    private val eventRepository = EventRepository(application)

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToLogin = MutableLiveData<Boolean>(false)
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private val _profileImage = MutableLiveData<Bitmap?>(null)
    val profileImage: LiveData<Bitmap?> = _profileImage

    private val _upcomingEvents = MutableLiveData<List<Event>>()
    val upcomingEvents: LiveData<List<Event>> = _upcomingEvents

    private val _postedEvents = MutableLiveData<List<Event>>()
    val postedEvents: LiveData<List<Event>> = _postedEvents

    init {
        startListeningForUserProfile()
    }

    private fun startListeningForUserProfile() {
        val currentUserId = authRepository.currentUserId()

        if (currentUserId == null) {
            _error.value = "No se encontró un usuario autenticado."
            return
        }

        loadProfileImage(currentUserId)
        loadUpcomingEvents(currentUserId)

        loadPostedEvents(currentUserId)

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

    private fun loadProfileImage(userId: String) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                FileStorageManager.loadProfileImage(getApplication(), userId)
            }
            _profileImage.value = bitmap
        }
    }

    private fun loadUpcomingEvents(userId: String) {
        viewModelScope.launch {
            try {
                val events = withContext(Dispatchers.IO) {
                    serviceAdapter.getUpcomingBookedEvents(userId)
                }
                _upcomingEvents.postValue(events)
            } catch (e: Exception) {

            }
        }
    }

    private fun loadPostedEvents(userId: String) {
        viewModelScope.launch {
            val result = eventRepository.getPostedEvents(userId)

            result.onSuccess { events ->
                _postedEvents.value = events
            }.onFailure { e ->
                _error.value = "Error al cargar eventos publicados: ${e.message}"
            }
        }
    }

    fun onSignOutClicked() {
        authRepository.signOut()
        _navigateToLogin.value = true
    }

    fun onNavigationComplete() {
        _navigateToLogin.value = false
    }
}