package com.example.kotlinapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.User
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.data.repository.UserPreferencesRepository

class RecommendationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userPrefsRepository = UserPreferencesRepository(applicationContext)
        val eventRepository = EventRepository()

        val mostViewedSport = userPrefsRepository.getMostViewedSport() ?: return Result.success()
        val userForRecommendation = User(sportList = listOf(mostViewedSport))
        val recommendedEventsResult = eventRepository.getRecommendedEvents(userForRecommendation)

        val eventToShow = recommendedEventsResult.getOrNull()?.firstOrNull() ?: return Result.success()

        sendNotification(eventToShow.name, eventToShow.description)

        return Result.success()
    }

    private fun sendNotification(title: String?, description: String?) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "recommendation_channel",
                "Recomendaciones de Eventos",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Usar la descripción del evento si existe; de lo contrario, usar un texto por defecto.
        val contentText = description ?: "A otros usuarios como tú les gustó este evento."

        val notification = NotificationCompat.Builder(applicationContext, "recommendation_channel")
            .setContentTitle("Evento recomendado: ${title ?: "No te lo pierdas"}")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_visibility)
            // Usar BigTextStyle para mostrar el texto completo en la notificación expandida
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .build()

        notificationManager.notify(1, notification)
    }
}