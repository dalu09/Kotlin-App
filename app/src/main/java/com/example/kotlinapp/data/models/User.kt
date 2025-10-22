package com.example.kotlinapp.data.models

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val description: String = "",
    val sportList: List<String> = emptyList(),
    val role: String = "user",
    val assistanceRate: Double = 0.0,
    val avgRating: Double = 0.0,
    val numRating: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)