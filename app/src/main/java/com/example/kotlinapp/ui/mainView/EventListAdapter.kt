package com.example.kotlinapp.ui.mainView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventListAdapter(
    private val events: List<Event>,
    private val onEventClicked: (String) -> Unit
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.event_name_text_view)
        val timeTextView: TextView = view.findViewById(R.id.event_time_text_view)
        val sportTextView: TextView = view.findViewById(R.id.event_sport_text_view)

        val dateTextView: TextView = view.findViewById(R.id.event_date_text_view)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_in_list, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.nameTextView.text = event.name
        holder.sportTextView.text = event.sport

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timeTextView.text = event.start_time?.let { timeFormat.format(it) } ?: ""


        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.dateTextView.text = event.start_time?.let { dateFormat.format(it) } ?: ""


        holder.itemView.setOnClickListener {
            onEventClicked(event.id)
        }
    }

    override fun getItemCount() = events.size
}