package com.example.kotlinapp.ui.mainView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.data.repository.RepositoryResult
import com.example.kotlinapp.util.MessageWrapper
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val events: List<Event>) : MainUiState()
    data class Stale(val events: List<Event>, val message: MessageWrapper<String>) : MainUiState()
    data class Error(val message: MessageWrapper<String>) : MainUiState()
}

class MainViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _uiState = MutableLiveData<MainUiState>()
    val uiState: LiveData<MainUiState> get() = _uiState

    private val _selectedEvents = MutableLiveData<List<Event>>()
    val selectedEvents: LiveData<List<Event>> get() = _selectedEvents

    // --- CAMBIO EMPIEZA AQUÍ: Lógica de navegación con eventos de un solo uso ---
    private val _navigateToEventDetail = MutableLiveData<MessageWrapper<String>>()
    val navigateToEventDetail: LiveData<MessageWrapper<String>> get() = _navigateToEventDetail

    fun onEventSelectedFromList(eventId: String) {
        _navigateToEventDetail.value = MessageWrapper(eventId)
    }
    // --- CAMBIO TERMINA AQUÍ ---

    fun loadNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double) {
        viewModelScope.launch {
            _uiState.postValue(MainUiState.Loading)
            when (val result = eventRepository.getNearbyEvents(userLocation, radiusInMeters)) {
                is RepositoryResult.Success -> _uiState.postValue(MainUiState.Success(result.data))
                is RepositoryResult.Stale -> _uiState.postValue(MainUiState.Stale(result.data, MessageWrapper("Estás sin conexión. Mostrando últimos datos guardados.")))
                is RepositoryResult.Error -> _uiState.postValue(MainUiState.Error(MessageWrapper(result.message)))
            }
        }
    }

    fun loadAllEvents() {
        viewModelScope.launch {
            _uiState.postValue(MainUiState.Loading)
            when (val result = eventRepository.getAllEvents()) {
                is RepositoryResult.Success -> _uiState.postValue(MainUiState.Success(result.data))
                is RepositoryResult.Stale -> _uiState.postValue(MainUiState.Stale(result.data, MessageWrapper("Estás sin conexión. Mostrando últimos datos guardados.")))
                is RepositoryResult.Error -> _uiState.postValue(MainUiState.Error(MessageWrapper(result.message)))
            }
        }
    }

    fun loadEventsByIds(ids: List<String>) {
        viewModelScope.launch {
            val events = ids.map { id ->
                async { eventRepository.getEventById(id) }
            }.awaitAll().mapNotNull { result ->
                result.getOrNull()
            }
            _selectedEvents.postValue(events)
        }
    }
}