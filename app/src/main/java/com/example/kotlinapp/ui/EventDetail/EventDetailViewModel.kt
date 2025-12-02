package com.example.kotlinapp.ui.eventdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _bookingUiState.value = BookingUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            // Primero, intentar cargar los detalles del evento (puede usar la caché)
            repo.getEventById(eventId).onSuccess { evt ->
                _event.value = evt

                // Si tenemos el evento, ahora verificamos el estado de la reserva
                repo.hasBooking(eventId, userId).onSuccess { hasBooking ->
                    _bookingUiState.value = if (hasBooking) BookingUiState.BOOKED else BookingUiState.AVAILABLE
                }.onFailure { bookingError ->
                    // Si falla la verificación de la reserva y estamos offline, mostramos el banner pero mantenemos los datos
                    if (!repo.isOnline()) {
                        _bookingUiState.value = BookingUiState.OFFLINE
                    } else {
                        _bookingUiState.value = BookingUiState.Error(bookingError.message ?: "Could not check booking status")
                    }
                }
            }.onFailure { eventError ->
                // Si falla la carga del evento y estamos offline, significa que no está en caché
                if (!repo.isOnline()) {
                    _bookingUiState.value = BookingUiState.OFFLINE
                } else {
                    _bookingUiState.value = BookingUiState.Error(eventError.message ?: "Could not load event details")
                }
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
                repo.getEventById(eventId).onSuccess { _event.value = it } // Actualizar participantes
            }.onFailure { e ->
                if (e.message == "You have already booked this event.") {
                    _bookingUiState.value = BookingUiState.BOOKED
                } else {
                    _bookingUiState.value = BookingUiState.AVAILABLE
                    _bookingUiState.value = BookingUiState.Error(e.message ?: "Could not complete booking")
                }
            }
        }
    }

    fun cancelBooking(eventId: String, userId: String) {
        if (_bookingUiState.value == BookingUiState.LOADING || _bookingUiState.value != BookingUiState.BOOKED) {
            return
        }

        if (!repo.isOnline()) {
            _bookingUiState.value = BookingUiState.OFFLINE
            return
        }

        viewModelScope.launch {
            _bookingUiState.value = BookingUiState.LOADING
            val res = repo.cancelBooking(eventId, userId)
            res.onSuccess {
                _bookingUiState.value = BookingUiState.AVAILABLE
                repo.getEventById(eventId).onSuccess { _event.value = it } // Actualizar participantes
            }.onFailure { e ->
                _bookingUiState.value = BookingUiState.BOOKED
                _bookingUiState.value = BookingUiState.Error(e.message ?: "Could not cancel booking")
            }
        }
    }
}
