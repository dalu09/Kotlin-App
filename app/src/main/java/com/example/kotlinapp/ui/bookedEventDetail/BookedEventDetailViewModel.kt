package com.example.kotlinapp.ui.bookedEventDetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class BookedEventDetailViewModelFactory(private val eventRepository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookedEventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookedEventDetailViewModel(eventRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class BookedEventDetailViewModel(private val repo: EventRepository) : ViewModel() {

    private val _eventDetails = MutableLiveData<BookedEventDetails?>()
    val eventDetails: LiveData<BookedEventDetails?> = _eventDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    private val _cancellationSuccess = MutableLiveData<Boolean>()
    val cancellationSuccess: LiveData<Boolean> = _cancellationSuccess

    fun loadEventAndVenue(eventId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val eventResult = repo.getEventById(eventId)
            if (eventResult.isFailure) {
                _error.value = "Error al cargar el evento."
                _isLoading.value = false
                return@launch
            }

            val event = eventResult.getOrNull()
            if (event?.venueid == null) {
                _eventDetails.value = BookedEventDetails(event!!, null)
                _isLoading.value = false
                return@launch
            }

            val venueResult = repo.getVenueByReference(event.venueid)
            val venue = venueResult.getOrNull()

            _eventDetails.value = BookedEventDetails(event, venue)
            _isLoading.value = false
        }
    }

    fun onCancelBookingClicked(eventId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _error.value = "Debes estar autenticado para cancelar."
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            repo.cancelBooking(eventId, userId)
                .onSuccess {
                    _cancellationSuccess.value = true
                }
                .onFailure { e ->
                    _error.value = e.message ?: "No se pudo cancelar la reserva."
                }
            _isLoading.value = false
        }
    }

    fun onErrorShown() {
        _error.value = null
    }
}
