package com.example.kotlinapp.ui.eventdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import kotlinx.coroutines.launch

class EventDetailViewModel : ViewModel() {

    private val eventRepository = EventRepository()

    private val _event = MutableLiveData<Event>()
    val event: LiveData<Event> get() = _event

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val result = eventRepository.getEventById(eventId)
            result.onSuccess {
                _event.postValue(it)
                _error.postValue(null) // Clear previous errors
            }.onFailure {
                _error.postValue("Error al cargar el evento: ${it.message}")
            }
        }
    }
}
