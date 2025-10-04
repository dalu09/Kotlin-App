package com.example.kotlinapp.data.repository

import android.location.Location
import android.util.Log
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class EventRepository {

    companion object {
        private const val TAG = "EventRepository"
    }

    private val db = FirebaseFirestore.getInstance()

    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val snapshot = db.collection("events").document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)
            if (event != null) {
                Result.success(event)
            } else {
                Result.failure(Exception("Evento no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double): Result<List<Event>> {
        Log.d(TAG, "Iniciando búsqueda de eventos cercanos...")
        return try {
            val eventsSnapshot = db.collection("events").get().await()
            val events = eventsSnapshot.toObjects(Event::class.java)
            Log.d(TAG, "Paso 1: Se encontraron ${events.size} eventos en total en la colección 'events'.")

            val eventsWithLocations = coroutineScope {
                events.map { event ->
                    async {
                        if (event.venueid == null) {
                            Log.w(TAG, "El evento '${event.name}' no tiene un campo 'venueid'. Saltando.")
                        } else {
                            Log.d(TAG, "Paso 2: Procesando evento '${event.name}'. Buscando su venue...")
                            try {
                                val venueSnapshot = event.venueid.get().await()
                                if (!venueSnapshot.exists()) {
                                    Log.e(TAG, "¡ERROR! La referencia 'venueid' para '${event.name}' apunta a un documento que NO EXISTE.")
                                } else {
                                    val venue = venueSnapshot.toObject(Venue::class.java)
                                    if (venue != null) {
                                        Log.i(TAG, "Paso 3: ¡Éxito! Venue '${venue.name}' encontrado para el evento '${event.name}'. Creando GeoPoint.")
                                        event.location = GeoPoint(venue.latitude, venue.longitude)
                                    } else {
                                        Log.e(TAG, "¡ERROR! Se encontró el documento del venue para '${event.name}', pero no se pudo convertir al objeto Venue. Revisa los campos.")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "¡ERROR CRÍTICO! Falló la obtención del venue para '${event.name}'. Causa: ${e.message}", e)
                                event.location = null
                            }
                        }
                        event
                    }
                }.awaitAll()
            }

            val nearbyEvents = eventsWithLocations.filter { event ->
                event.location?.let { eventLocation ->
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                        userLocation.latitude,
                        userLocation.longitude,
                        eventLocation.latitude,
                        eventLocation.longitude,
                        distance
                    )
                    val isNearby = distance[0] <= radiusInMeters
                    if(isNearby) Log.d(TAG, "Paso 4: El evento '${event.name}' ESTÁ CERCA.")
                    else Log.w(TAG, "Paso 4: El evento '${event.name}' está demasiado lejos (${distance[0]/1000} km). Descartado.")
                    isNearby
                } ?: false
            }
            Log.i(TAG, "Paso 5: Búsqueda finalizada. Número de eventos cercanos encontrados: ${nearbyEvents.size}")
            Result.success(nearbyEvents)
        } catch (e: Exception) {
            Log.e(TAG, "¡ERROR FATAL! La consulta inicial a la colección 'events' falló. Causa: ${e.message}", e)
            Result.failure(e)
        }
    }
}
