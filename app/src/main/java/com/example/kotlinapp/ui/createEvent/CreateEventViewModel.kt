package com.example.kotlinapp.ui.createEvent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.repository.EventRepository
import kotlinx.coroutines.launch

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
                // Asumimos que estas funciones existirán en el repositorio
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

    // Aquí irá la función para crear el evento
    // fun createEvent(...) { ... }
}
