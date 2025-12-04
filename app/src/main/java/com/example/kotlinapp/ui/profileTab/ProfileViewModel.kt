package com.example.kotlinapp.ui.profileTab

import android.app.Application
import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.EventRepository
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

    private val eventsCache = LruCache<String, List<Event>>(4)

    private val _networkError = MutableLiveData<Boolean>(false)
    val networkError: LiveData<Boolean> = _networkError

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

    fun loadUserEvents() {
        val userId = authRepository.currentUserId() ?: return

        val cachedPosted = eventsCache.get("posted_events") as? List<Event>
        val cachedUpcoming = eventsCache.get("upcoming_events") as? List<Event>

        if (cachedPosted != null) {
            _postedEvents.value = cachedPosted
        }
        if (cachedUpcoming != null) {
            _upcomingEvents.value = cachedUpcoming
        }


        val hasContentFromCache = (cachedPosted?.isNotEmpty() == true) || (cachedUpcoming?.isNotEmpty() == true)

        viewModelScope.launch {

            val upcomingResult = runCatching { serviceAdapter.getUpcomingBookedEvents(userId) }
            val postedResult = runCatching { eventRepository.getPostedEvents(userId).getOrThrow() }

            // 4. Si la primera llamada (upcoming) fue exitosa, actualizamos todo.
            upcomingResult.onSuccess { networkEvents ->
                _upcomingEvents.postValue(networkEvents)
                eventsCache.put("upcoming_events", networkEvents)
            }

            // 5. Si la segunda llamada (posted) fue exitosa, actualizamos todo.
            postedResult.onSuccess { networkEvents ->
                _postedEvents.postValue(networkEvents)
                eventsCache.put("posted_events", networkEvents)
            }

            if (upcomingResult.isFailure && postedResult.isFailure) {
                if (!hasContentFromCache) {
                    _networkError.postValue(true)
                }


            } else {
                _networkError.postValue(false)
            }
        }
    }



    fun onRetry() {
        _networkError.value = false
        startListeningForUserProfile()
        loadUserEvents()
    }

    private fun loadProfileImage(userId: String) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                FileStorageManager.loadProfileImage(getApplication(), userId)
            }
            _profileImage.value = bitmap
        }
    }

    fun onSignOutClicked() {
        eventsCache.evictAll()
        authRepository.signOut()
        _navigateToLogin.value = true
    }

    fun onNavigationComplete() {
        _navigateToLogin.value = false
    }
}
