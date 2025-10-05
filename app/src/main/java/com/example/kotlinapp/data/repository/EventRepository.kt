package com.example.kotlinapp.data.repository

import android.location.Location
import android.util.Log
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.models.Venue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventRepository {

    companion object { private const val TAG = "EventRepository" }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --------- Event detail ----------
    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val snapshot = db.collection("events").document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)
            if (event != null) Result.success(event)
            else Result.failure(Exception("Evento no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------- Nearby events ----------
    suspend fun getNearbyEvents(userLocation: GeoPoint, radiusInMeters: Double): Result<List<Event>> {
        Log.d(TAG, "Iniciando búsqueda de eventos cercanos…")
        return try {
            val eventsSnapshot = db.collection("events").get().await()
            val events = eventsSnapshot.toObjects(Event::class.java)
            Log.d(TAG, "Se encontraron ${events.size} eventos en 'events'.")

            val eventsWithLocations = coroutineScope {
                events.map { event ->
                    async {
                        val ref = event.venueid
                        if (ref == null) {
                            Log.w(TAG, "Evento '${event.name}' sin venueid.")
                        } else {
                            try {
                                val venueSnap = ref.get().await()
                                if (!venueSnap.exists()) {
                                    Log.e(TAG, "venueid para '${event.name}' apunta a doc inexistente.")
                                } else {
                                    val venue = venueSnap.toObject(Venue::class.java)
                                    if (venue != null) {
                                        event.location = GeoPoint(venue.latitude, venue.longitude)
                                    } else {
                                        Log.e(TAG, "No se pudo parsear Venue de '${event.name}'.")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error obteniendo venue de '${event.name}': ${e.message}", e)
                                event.location = null
                            }
                        }
                        event
                    }
                }.awaitAll()
            }

            val nearby = eventsWithLocations.filter { event ->
                event.location?.let { loc ->
                    val d = FloatArray(1)
                    Location.distanceBetween(
                        userLocation.latitude, userLocation.longitude,
                        loc.latitude, loc.longitude, d
                    )
                    d[0] <= radiusInMeters
                } ?: false
            }

            Log.i(TAG, "Eventos cercanos: ${nearby.size}")
            Result.success(nearby)
        } catch (e: Exception) {
            Log.e(TAG, "Fallo consulta de eventos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --------- Booking + contador + marker mensual ----------
    suspend fun createBooking(eventId: String, userId: String): Result<Unit> {
        return try {
            val bookingsCol = db.collection("bookings")
            val eventsCol   = db.collection("events")

            // 1) Evitar doble reserva del mismo usuario
            val already = bookingsCol
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get().await()

            if (!already.isEmpty) return Result.failure(Exception("Ya reservaste este evento"))

            // 2) Transacción: crear booking + incrementar 'booked' del evento + marcar MAU por mes
            db.runTransaction { tx ->
                val eventRef = eventsCol.document(eventId)
                val snap = tx.get(eventRef)
                if (!snap.exists()) throw Exception("Evento no existe")

                val booked   = snap.getLong("booked")?.toInt() ?: 0
                val maxCap   = snap.getLong("max_capacity")?.toInt() ?: 0
                if (booked >= maxCap) throw Exception("Cupo lleno")

                // 2.1 Crea booking
                val bookingRef = bookingsCol.document()
                tx.set(
                    bookingRef,
                    mapOf(
                        "eventId" to eventId,
                        "userId" to userId,
                        "timestamp" to Timestamp.now()
                    )
                )

                // 2.2 Incrementa contador del evento
                tx.update(eventRef, "booked", FieldValue.increment(1))

                // 2.3 Marca usuario como "booker" del mes (para conteo único)
                val yyyyMM = SimpleDateFormat("yyyyMM", Locale.US).format(Date())
                val monthlyRef = db.collection("ma_bookers")
                    .document(yyyyMM)
                    .collection("users")
                    .document(userId)

                // merge para que sea idempotente
                tx.set(monthlyRef, mapOf("present" to true), SetOptions.merge())

                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
