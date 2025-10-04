package com.example.kotlinapp.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.kotlinapp.R

class EventDetailFragment : Fragment() {

    private val viewModel: EventDetailViewModel by viewModels()

    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var participantsText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_event_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleText = view.findViewById(R.id.eventTitle)
        descriptionText = view.findViewById(R.id.eventDescription)
        participantsText = view.findViewById(R.id.participants)
        progressBar = view.findViewById(R.id.progressParticipants)

        setupObservers()

        // Obtenemos el eventId directamente de los argumentos del fragmento
        val eventId = arguments?.getString("event_id")
        
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        } else {
            // Si por alguna razón el ID no llega, mostramos un error
            Toast.makeText(requireContext(), "Error: No se pudo encontrar el ID del evento.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            titleText.text = event.name
            descriptionText.text = event.description
            val participantsString = "${event.booked} / ${event.max_capacity} participantes"
            participantsText.text = participantsString
            if (event.max_capacity > 0) {
                progressBar.progress = (100 * event.booked) / event.max_capacity
            } else {
                progressBar.progress = 0
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }
}
