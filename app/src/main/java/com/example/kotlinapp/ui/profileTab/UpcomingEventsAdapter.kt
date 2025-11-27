package com.example.kotlinapp.ui.profileTab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

class UpcomingEventsAdapter(private var events: List<Event>) : RecyclerView.Adapter<UpcomingEventsAdapter.EventViewHolder>() {

    private val dateFormat = SimpleDateFormat("EEE, d MMM - hh:mm a", Locale.getDefault())

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.event_title_text)
        val dateTextView: TextView = view.findViewById(R.id.event_date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_item_profile, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.titleTextView.text = event.name
        holder.dateTextView.text = event.start_time?.let { dateFormat.format(it) } ?: "Fecha no disponible"
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
    }
}
