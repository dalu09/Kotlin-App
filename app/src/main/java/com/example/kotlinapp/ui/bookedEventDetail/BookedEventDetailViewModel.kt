package com.example.kotlinapp.ui.bookedEventDetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue
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

    private val _networkError = MutableLiveData<Boolean>(false)
    val networkError: LiveData<Boolean> = _networkError

    private val _cancellationSuccess = MutableLiveData<Boolean>()
    val cancellationSuccess: LiveData<Boolean> = _cancellationSuccess

    fun loadEventAndVenue(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _networkError.value = false

            val eventResult = repo.getEventById(eventId)

            eventResult.onSuccess { event ->

                val venueResult = event.venueid?.let { repo.getVenueByReference(it) }
                _eventDetails.value = BookedEventDetails(event, venueResult?.getOrNull())
                _isLoading.value = false
            }.onFailure {

                _isLoading.value = false
                _networkError.value = true
                _eventDetails.value = null
            }
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
                    _isLoading.value = false
                    _cancellationSuccess.value = true
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "No se pudo cancelar la reserva."
                }
        }
    }

    fun onRetry(eventId: String) {
        loadEventAndVenue(eventId)
    }

    fun onErrorShown() {
        _error.value = null
    }
}
