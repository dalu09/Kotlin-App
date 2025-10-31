package com.example.kotlinapp.data.service

import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.User

interface EventServiceAdapter {
    suspend fun getAllEvents(): List<Event>
    suspend fun getRecommendedEvents(user: User, limit: Long): List<Event>
}
