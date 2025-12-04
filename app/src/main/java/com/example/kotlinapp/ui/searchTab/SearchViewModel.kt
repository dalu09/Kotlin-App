package com.example.kotlinapp.ui.searchTab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.data.repository.RepositoryResult
import com.example.kotlinapp.data.service.ProfileServiceAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val profileAdapter = ProfileServiceAdapter()

    private val _recommendedEvents = MutableLiveData<List<Event>>()
    val recommendedEvents: LiveData<List<Event>> = _recommendedEvents

    private val _allEvents = MutableLiveData<List<Event>>()
    val allEvents: LiveData<List<Event>> = _allEvents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _networkError = MutableLiveData<Boolean>(false)
    val networkError: LiveData<Boolean> = _networkError

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _networkError.value = false
            if (!eventRepository.isOnline()) {
                _isLoading.value = false
                _networkError.value = true
                return@launch
            }

            try {
                val userId = profileAdapter.getCurrentUserId()
                if (userId != null) {
                    val user = profileAdapter.getUserProfileFlow(userId).first()
                    if (user != null) {
                        val recommendationsResult = eventRepository.getRecommendedEvents(user)
                        recommendationsResult.onSuccess { events ->
                            _recommendedEvents.value = events
                        }.onFailure { exception ->
                            _error.value = "Error al cargar recomendados: ${exception.message}"
                            _recommendedEvents.value = emptyList()
                        }
                    }
                }

                // Carga de todos los eventos
                when (val allEventsResult = eventRepository.getAllEvents()) {
                    is RepositoryResult.Success -> {
                        _allEvents.value = allEventsResult.data
                    }
                    is RepositoryResult.Stale -> {
                        _allEvents.value = allEventsResult.data
                    }
                    is RepositoryResult.Error -> {
                        throw Exception(allEventsResult.message)
                    }
                }

                // Si todo ha ido bien
                _isLoading.value = false

            } catch (e: Exception) {

                _isLoading.value = false
                _networkError.value = true
                _error.value = "Fallo la carga de datos: ${e.message}"
            }
        }
    }

    fun onRetry() {
        loadInitialData()
    }

    fun onErrorShown() {
        _error.value = null
    }
}
