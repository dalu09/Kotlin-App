package com.example.kotlinapp.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class Event(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val sport: String = "",
    val skill_level: String = "",
    val start_time: Date? = null,
    val end_time: Date? = null,
    val max_capacity: Int = 0,
    val booked: Int = 0,
    val venueid: DocumentReference? = null,
    val organizerid: DocumentReference? = null,

    // Datos del "venue"
    var location: GeoPoint? = null
)
