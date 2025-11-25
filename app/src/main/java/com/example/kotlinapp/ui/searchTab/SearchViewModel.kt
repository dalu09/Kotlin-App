package com.example.kotlinapp.ui.searchTab

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.data.repository.RepositoryResult // Importar el nuevo sealed class
import com.example.kotlinapp.data.service.ProfileServiceAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline


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


            when (val allEventsResult = eventRepository.getAllEvents()) {
                is RepositoryResult.Success -> _allEvents.value = allEventsResult.data
                is RepositoryResult.Stale -> _allEvents.value = allEventsResult.data // También mostramos datos de caché
                is RepositoryResult.Error -> _error.value = "Error al cargar todos los eventos: ${allEventsResult.message}"
            }
        }
    }
}