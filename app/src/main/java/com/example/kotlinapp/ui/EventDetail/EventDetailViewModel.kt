package com.example.kotlinapp.ui.eventdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import kotlinx.coroutines.launch

class EventDetailViewModel : ViewModel() {

    private val repo = EventRepository()

    private val _event = MutableLiveData<Event>()
    val event: LiveData<Event> = _event

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _bookingResult = MutableLiveData<String?>()
    val bookingResult: LiveData<String?> = _bookingResult

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val res = repo.getEventById(eventId)
            res.onSuccess { evt ->
                _event.value = evt
            }.onFailure { e ->
                _error.value = e.message ?: "No se pudo cargar el evento"
            }
        }
    }

    fun createBooking(eventId: String, userId: String) {
        viewModelScope.launch {
            val res = repo.createBooking(eventId, userId)
            res.onSuccess {
                _bookingResult.value = "Reserva confirmada"
            }.onFailure { e ->
                _error.value = e.message ?: "No se pudo reservar"
            }
        }
    }
}
