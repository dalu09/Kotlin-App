package com.example.kotlinapp.ui.createEvent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Date

sealed class CreateEventUiState {
    object IDLE : CreateEventUiState()
    object SUCCESS : CreateEventUiState()
    data class ERROR(val message: String, val isFatal: Boolean = false) : CreateEventUiState()
}

data class CreateEventFormState(
    val sports: List<String> = emptyList(),
    val skillLevels: List<String> = emptyList(),
    val venues: List<Venue> = emptyList(),
)

class CreateEventViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _formState = MutableLiveData<CreateEventFormState>()
    val formState: LiveData<CreateEventFormState> = _formState

    private val _uiState = MutableLiveData<CreateEventUiState>(CreateEventUiState.IDLE)
    val uiState: LiveData<CreateEventUiState> = _uiState

    fun loadInitialData() {
        viewModelScope.launch {
            if (!eventRepository.isOnline()) {
                _uiState.value = CreateEventUiState.ERROR("No internet connection to load required data.", isFatal = true)
                return@launch
            }
            
            try {
                val sports = eventRepository.getSports()
                val skillLevels = eventRepository.getSkillLevels()
                val venues = eventRepository.getVenues()
                _formState.postValue(CreateEventFormState(sports, skillLevels, venues))
                _uiState.value = CreateEventUiState.IDLE
            } catch (e: Exception) {
                _uiState.value = CreateEventUiState.ERROR("Failed to load form data: ${e.message}", isFatal = true)
            }
        }
    }

    fun createEvent(
        name: String,
        description: String,
        sport: String,
        skillLevel: String,
        venueName: String,
        maxParticipants: String,
        startTime: Date?,
        endTime: Date?
    ) {
        if (!eventRepository.isOnline()) {
            _uiState.value = CreateEventUiState.ERROR("No internet connection to create event.")
            return
        }

        if (name.isBlank() || description.isBlank() || sport.isBlank() || skillLevel.isBlank() || venueName.isBlank() || maxParticipants.isBlank() || startTime == null || endTime == null) {
            _uiState.value = CreateEventUiState.ERROR("Please fill all fields")
            return
        }
        if (endTime.before(startTime) || endTime == startTime) {
            _uiState.value = CreateEventUiState.ERROR("End time must be after the start time")
            return
        }
        val participants = maxParticipants.toIntOrNull()
        if (participants == null || participants <= 0) {
            _uiState.value = CreateEventUiState.ERROR("Invalid number of participants")
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _uiState.value = CreateEventUiState.ERROR("You must be logged in to create an event")
            return
        }

        val venueId = _formState.value?.venues?.find { it.name == venueName }?.id
        if (venueId == null) {
            _uiState.value = CreateEventUiState.ERROR("Venue not found")
            return
        }
        
        val organizerRef: DocumentReference = FirebaseFirestore.getInstance().collection("users").document(userId)
        val venueRef: DocumentReference = FirebaseFirestore.getInstance().collection("venues").document(venueId)

        val newEvent = Event(
            name = name,
            description = description,
            sport = sport,
            skill_level = skillLevel,
            max_capacity = participants,
            start_time = startTime,
            end_time = endTime,
            venueid = venueRef,
            organizerid = organizerRef
        )

        viewModelScope.launch {
            val result = eventRepository.createEvent(newEvent)
            result.onSuccess {
                _uiState.value = CreateEventUiState.SUCCESS
            }.onFailure {
                _uiState.value = CreateEventUiState.ERROR(it.message ?: "Error creating event")
            }
        }
    }

    fun onDone() {
        _uiState.value = CreateEventUiState.IDLE
    }
}