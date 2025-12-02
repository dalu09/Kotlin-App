package com.example.kotlinapp.ui.bookedEventDetail

import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue

data class BookedEventDetails(
    val event: Event,
    val venue: Venue?
)