package com.example.kotlinapp.ui.eventdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EventDetailViewModel : ViewModel() {

    private val _title = MutableLiveData("Título del evento")
    val title: LiveData<String> = _title

    private val _description = MutableLiveData("Descripción del evento")
    val description: LiveData<String> = _description

    private val _participants = MutableLiveData("0 participantes")
    val participants: LiveData<String> = _participants

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress
}
