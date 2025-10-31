package com.example.kotlinapp.ui.mainView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val eventRepository = EventRepository()

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadAllEvents() {
        viewModelScope.launch {
            val result = eventRepository.getAllEvents()
            result.onSuccess {
                _events.postValue(it)
            }.onFailure {
                _error.postValue("Error al cargar todos los eventos: ${it.message}")
            }
        }
    }

    fun loadNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double) {
        viewModelScope.launch {
            val result = eventRepository.getNearbyEvents(userLocation, radiusInMeters)
            result.onSuccess {
                _events.postValue(it)
            }.onFailure {
                _error.postValue("Error al cargar eventos cercanos: ${it.message}")
            }
        }
    }
}