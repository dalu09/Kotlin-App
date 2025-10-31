package com.example.kotlinapp.data.repository

import android.location.Location
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.service.EventServiceAdapter
import com.example.kotlinapp.data.service.FirebaseEventServiceAdapter
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Date

class EventRepository(
    private val service: EventServiceAdapter = FirebaseEventServiceAdapter()
) {

    // Detalle de evento
    suspend fun getEventById(eventId: String): Result<Event> = runCatching {
        service.fetchEvent(eventId) ?: throw IllegalStateException("Evento no encontrado")
    }

    // Obtener todos los eventos
    suspend fun getAllEvents(): Result<List<Event>> = runCatching {
        val events = service.fetchAllEvents()
        coroutineScope {
            events.map { event ->
                async {
                    val ref = event.venueid
                    if (ref != null) {
                        val venue = service.fetchVenue(ref)
                        if (venue != null) {
                            event.location = GeoPoint(venue.latitude, venue.longitude)
                        }
                    }
                    event
                }
            }.awaitAll()
        }
    }

    // Eventos cercanos
    suspend fun getNearbyEvents(
        userLocation: GeoPoint,
        radiusInMeters: Double
    ): Result<List<Event>> = runCatching {
        val events = service.fetchAllEvents()

        val withLocations = coroutineScope {
            events.map { event ->
                async {
                    val ref = event.venueid
                    if (ref != null) {
                        val venue = service.fetchVenue(ref)
                        if (venue != null) {
                            event.location = GeoPoint(venue.latitude, venue.longitude)
                        }
                    }
                    event
                }
            }.awaitAll()
        }

        // Filtrar por distancia
        withLocations.filter { evt ->
            evt.location?.let { loc ->
                val out = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    loc.latitude, loc.longitude,
                    out
                )
                out[0] <= radiusInMeters
            } ?: false
        }
    }

    suspend fun getUpcomingEventsBySport(sport: String): Result<List<Event>> = runCatching {
        val allEvents = service.fetchAllEvents()
        val upcomingEvents = allEvents.filter { event ->
            event.sport == sport && (event.start_time?.after(Date()) ?: false)
        }
        upcomingEvents
    }


    // Reservar
    suspend fun createBooking(eventId: String, userId: String): Result<Unit> = runCatching {
        // Evita duplicado
        val already = service.hasUserBooking(eventId, userId)
        if (already) throw IllegalStateException("Ya has reservado este evento.")

        
        service.runBookingTransaction(eventId, userId)
    }
}
