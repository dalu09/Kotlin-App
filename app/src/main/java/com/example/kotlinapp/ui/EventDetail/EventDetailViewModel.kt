package com.example.kotlinapp.ui.eventdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import kotlinx.coroutines.launch

sealed class BookingUiState {
    object AVAILABLE : BookingUiState()
    object BOOKED : BookingUiState()
    object OFFLINE : BookingUiState()
    object LOADING : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(application)

    private val _event = MutableLiveData<Event>()
    val event: LiveData<Event> = _event

    private val _bookingUiState = MutableLiveData<BookingUiState>()
    val bookingUiState: LiveData<BookingUiState> = _bookingUiState

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            updateButtonState(eventId)

            val res = repo.getEventById(eventId)
            res.onSuccess { evt ->
                _event.value = evt
                if (repo.isOnline() && evt.booked >= evt.max_capacity && _bookingUiState.value != BookingUiState.BOOKED) {
                    _bookingUiState.value = BookingUiState.BOOKED 
                }
            }.onFailure { e ->
                if (_bookingUiState.value != BookingUiState.OFFLINE) {
                    _bookingUiState.value = BookingUiState.Error(e.message ?: "Could not load event")
                }
            }
        }
    }

    private fun updateButtonState(eventId: String) {
        if (!repo.isOnline()) {
            _bookingUiState.value = BookingUiState.OFFLINE
            return
        }
        viewModelScope.launch {
            if (repo.isEventBooked(eventId)) {
                _bookingUiState.value = BookingUiState.BOOKED
            } else {
                _bookingUiState.value = BookingUiState.AVAILABLE
            }
        }
    }

    fun createBooking(eventId: String, userId: String) {
        if (_bookingUiState.value == BookingUiState.LOADING || _bookingUiState.value == BookingUiState.BOOKED) {
            return
        }

        if (!repo.isOnline()) {
            _bookingUiState.value = BookingUiState.OFFLINE
            return
        }

        viewModelScope.launch {
            _bookingUiState.value = BookingUiState.LOADING
            val res = repo.createBooking(eventId, userId)
            res.onSuccess {
                _bookingUiState.value = BookingUiState.BOOKED
                loadEvent(eventId)
            }.onFailure { e ->
                _bookingUiState.value = BookingUiState.Error(e.message ?: "Could not complete booking")
            }
        }
    }

    fun cancelBooking(eventId: String, userId: String) {
        if (_bookingUiState.value == BookingUiState.LOADING || _bookingUiState.value != BookingUiState.BOOKED) {
            return
        }

        if (!repo.isOnline()) {
            _bookingUiState.value = BookingUiState.Error("No internet connection to cancel booking.")
            return
        }

        viewModelScope.launch {
            _bookingUiState.value = BookingUiState.LOADING
            val res = repo.cancelBooking(eventId, userId)
            res.onSuccess {
                _bookingUiState.value = BookingUiState.AVAILABLE
                loadEvent(eventId)
            }.onFailure { e ->
                _bookingUiState.value = BookingUiState.Error(e.message ?: "Could not cancel booking")
            }
        }
    }
}