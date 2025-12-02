package com.example.kotlinapp.ui.editevent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import kotlinx.coroutines.launch

class EditEventViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EventRepository(application)


    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event


    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult


    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val result = repository.getEventById(eventId)
            result.onSuccess {
                _event.value = it
            }.onFailure {
                _error.value = it.message ?: "Error al cargar el evento"
            }
        }
    }

    fun updateEvent(eventId: String, name: String, description: String, sport: String) {
        if (name.isBlank() || description.isBlank() || sport.isBlank()) {
            _updateResult.value = Result.failure(Exception("Todos los campos son obligatorios"))
            return
        }

        viewModelScope.launch {

            val updates = mapOf(
                "name" to name,
                "description" to description,
                "sport" to sport
            )

            val result = repository.updateEvent(eventId, updates)
            _updateResult.value = result
        }
    }
}