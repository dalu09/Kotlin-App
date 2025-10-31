// En: com/example/kotlinapp/ui/searchTab/SearchViewModel.kt

package com.example.kotlinapp.ui.searchTab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.data.service.ProfileServiceAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val eventRepository = EventRepository()
    private val profileAdapter = ProfileServiceAdapter()

    private val _recommendedEvents = MutableLiveData<List<Event>>()
    val recommendedEvents: LiveData<List<Event>> = _recommendedEvents

    private val _allEvents = MutableLiveData<List<Event>>()
    val allEvents: LiveData<List<Event>> = _allEvents

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        fetchEvents()
    }

    private fun fetchEvents() {
        viewModelScope.launch {

            val userId = profileAdapter.getCurrentUserId()
            if (userId != null) {

                val user = profileAdapter.getUserProfileFlow(userId).first()
                if (user != null) {
                    val recommendationsResult = eventRepository.getRecommendedEvents(user)
                    recommendationsResult.onSuccess { events ->
                        _recommendedEvents.value = events
                    }.onFailure { exception ->
                        _error.value = "Error al cargar recomendados: ${exception.message}"
                    }
                }
            }

            val allEventsResult = eventRepository.getAllEvents()
            allEventsResult.onSuccess { events ->
                _allEvents.value = events
            }.onFailure { exception ->

                _error.value = "Error al cargar todos los eventos: ${exception.message}"
            }
        }
    }
}