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
import kotlinx.coroutines.launch

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val events: List<Event>) : MainUiState()
    // Stale ahora contiene un MessageWrapper para el mensaje
    data class Stale(val events: List<Event>, val message: MessageWrapper<String>) : MainUiState()
    // Error ahora contiene un MessageWrapper para el mensaje
    data class Error(val message: MessageWrapper<String>) : MainUiState()
}

class MainViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _uiState = MutableLiveData<MainUiState>()
    val uiState: LiveData<MainUiState> get() = _uiState

    fun loadNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double) {
        viewModelScope.launch {
            _uiState.postValue(MainUiState.Loading)
            when (val result = eventRepository.getNearbyEvents(userLocation, radiusInMeters)) {
                is RepositoryResult.Success -> _uiState.postValue(MainUiState.Success(result.data))
                // Envuelve el mensaje de "stale" (datos de caché)
                is RepositoryResult.Stale -> _uiState.postValue(MainUiState.Stale(result.data, MessageWrapper("Estás sin conexión. Mostrando últimos datos guardados.")))
                // Envuelve el mensaje de error
                is RepositoryResult.Error -> _uiState.postValue(MainUiState.Error(MessageWrapper(result.message)))
            }
        }
    }

    fun loadAllEvents() {
        viewModelScope.launch {
            _uiState.postValue(MainUiState.Loading)
            when (val result = eventRepository.getAllEvents()) {
                is RepositoryResult.Success -> _uiState.postValue(MainUiState.Success(result.data))
                 // Envuelve el mensaje de "stale"
                is RepositoryResult.Stale -> _uiState.postValue(MainUiState.Stale(result.data, MessageWrapper("Estás sin conexión. Mostrando últimos datos guardados.")))
                // Envuelve el mensaje de error
                is RepositoryResult.Error -> _uiState.postValue(MainUiState.Error(MessageWrapper(result.message)))
            }
        }
    }
}