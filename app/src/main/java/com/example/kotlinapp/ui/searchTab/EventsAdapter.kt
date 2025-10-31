package com.example.kotlinapp.ui.searchTab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecommendedEventsAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, RecommendedEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.event_name_text)
        private val dateTextView: TextView = itemView.findViewById(R.id.event_date_text)

        fun bind(event: Event) {
            nameTextView.text = event.name
            dateTextView.text = event.start_time?.toFormattedString() ?: "Fecha no disponible"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_recommended, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }
}

class AllEventsAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, AllEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.event_name_text)
        private val sportTextView: TextView = itemView.findViewById(R.id.event_sport_text)
        private val dateTextView: TextView = itemView.findViewById(R.id.event_date_text)

        fun bind(event: Event) {
            nameTextView.text = event.name
            sportTextView.text = event.sport
            dateTextView.text = "Se jugará el ${event.start_time?.toFormattedString() ?: "fecha no definida"}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_full, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}

fun Date.toFormattedString(): String {
    val formatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, hh:mm a", Locale("es", "ES"))
    return formatter.format(this)
}
