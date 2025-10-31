package com.example.kotlinapp.data.repository

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val MOST_VIEWED_SPORT = "most_viewed_sport"
        private const val SPORT_VIEW_COUNT_PREFIX = "sport_view_count_"
    }

    fun incrementSportView(sport: String) {
        val key = "$SPORT_VIEW_COUNT_PREFIX$sport"
        val currentCount = prefs.getInt(key, 0)
        prefs.edit().putInt(key, currentCount + 1).apply()
        updateMostViewedSport()
    }

    private fun updateMostViewedSport() {
        val allPrefs = prefs.all
        var maxCount = -1
        var mostViewedSport: String? = null

        for ((key, value) in allPrefs) {
            if (key.startsWith(SPORT_VIEW_COUNT_PREFIX) && value is Int) {
                if (value > maxCount) {
                    maxCount = value
                    mostViewedSport = key.removePrefix(SPORT_VIEW_COUNT_PREFIX)
                }
            }
        }

        if (mostViewedSport != null) {
            prefs.edit().putString(MOST_VIEWED_SPORT, mostViewedSport).apply()
        }
    }

    fun getMostViewedSport(): String? {
        return prefs.getString(MOST_VIEWED_SPORT, null)
    }
}
