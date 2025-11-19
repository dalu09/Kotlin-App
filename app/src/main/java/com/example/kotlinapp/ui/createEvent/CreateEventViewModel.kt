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

// Estado para el formulario de creación de eventos
data class CreateEventFormState(
    val sports: List<String> = emptyList(),
    val skillLevels: List<String> = emptyList(),
    val venues: List<Venue> = emptyList(),
    val isEventCreated: Boolean = false,
    val error: String? = null
)

class CreateEventViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _formState = MutableLiveData<CreateEventFormState>()
    val formState: LiveData<CreateEventFormState> = _formState

    fun loadInitialData() {
        viewModelScope.launch {
            try {
                val sports = eventRepository.getSports()
                val skillLevels = eventRepository.getSkillLevels()
                val venues = eventRepository.getVenues()

                _formState.postValue(
                    CreateEventFormState(
                        sports = sports,
                        skillLevels = skillLevels,
                        venues = venues
                    )
                )
            } catch (e: Exception) {
                _formState.postValue(CreateEventFormState(error = "Failed to load form data: ${e.message}"))
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
        if (name.isBlank() || description.isBlank() || sport.isBlank() || skillLevel.isBlank() || venueName.isBlank() || maxParticipants.isBlank() || startTime == null || endTime == null) {
            _formState.value = _formState.value?.copy(error = "Please fill all fields")
            return
        }

        if (name.length > 50) {
            _formState.value = _formState.value?.copy(error = "Event name cannot exceed 50 characters")
            return
        }

        if (description.length > 200) {
            _formState.value = _formState.value?.copy(error = "Description cannot exceed 200 characters")
            return
        }

        if (endTime.before(startTime) || endTime == startTime) {
            _formState.value = _formState.value?.copy(error = "End time must be after the start time")
            return
        }

        val participants = maxParticipants.toIntOrNull()
        if (participants == null || participants <= 0) {
            _formState.value = _formState.value?.copy(error = "Invalid number of participants")
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _formState.value = _formState.value?.copy(error = "User not logged in")
            return
        }
        val organizerRef: DocumentReference = FirebaseFirestore.getInstance().collection("users").document(userId)

        val venueId = _formState.value?.venues?.find { it.name == venueName }?.id
        if (venueId == null) {
            _formState.value = _formState.value?.copy(error = "Venue not found")
            return
        }
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
                _formState.value = _formState.value?.copy(isEventCreated = true, error = null)
            }.onFailure {
                _formState.value = _formState.value?.copy(error = "Error creating event: ${it.message}")
            }
        }
    }

    fun onEventCreationNotified() {
        _formState.value = _formState.value?.copy(isEventCreated = false, error = null)
    }
}