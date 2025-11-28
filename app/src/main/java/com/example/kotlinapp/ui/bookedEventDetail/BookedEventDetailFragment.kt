package com.example.kotlinapp.ui.bookedEventDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.kotlinapp.R
import com.example.kotlinapp.data.repository.EventRepository
import java.text.SimpleDateFormat
import java.util.*

class BookedEventDetailFragment : Fragment() {

    private val viewModel: BookedEventDetailViewModel by viewModels {
        BookedEventDetailViewModelFactory(EventRepository(requireContext().applicationContext))
    }
    private val args: BookedEventDetailFragmentArgs by navArgs()

    private lateinit var loadingSpinner: ProgressBar
    private lateinit var eventTitle: TextView
    private lateinit var sportName: TextView
    private lateinit var eventDate: TextView
    private lateinit var eventTime: TextView
    private lateinit var locationName: TextView
    private lateinit var locationAddress: TextView
    private lateinit var participantsCount: TextView
    private lateinit var participantsProgress: ProgressBar
    private lateinit var eventDescription: TextView
    private lateinit var cancelButton: Button
    private lateinit var backArrow: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booked_event_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = args.eventId

        initializeViews(view)
        setupClickListeners(eventId)
        setupObservers()

        viewModel.loadEventAndVenue(eventId)
    }

    private fun initializeViews(view: View) {
        loadingSpinner = view.findViewById(R.id.loading_spinner)
        eventTitle = view.findViewById(R.id.event_title)
        sportName = view.findViewById(R.id.sport_name)
        eventDate = view.findViewById(R.id.event_date)
        eventTime = view.findViewById(R.id.event_time)
        locationName = view.findViewById(R.id.location_name)
        locationAddress = view.findViewById(R.id.location_address)
        participantsCount = view.findViewById(R.id.participants_count)
        participantsProgress = view.findViewById(R.id.participants_progress)
        eventDescription = view.findViewById(R.id.event_description)
        cancelButton = view.findViewById(R.id.cancel_reservation_button)
        backArrow = view.findViewById(R.id.back_arrow_icon)
    }

    private fun setupClickListeners(eventId: String) {
        backArrow.setOnClickListener {
            findNavController().popBackStack()
        }

        cancelButton.setOnClickListener {
            viewModel.onCancelBookingClicked(eventId)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingSpinner.isVisible = isLoading

            cancelButton.isEnabled = !isLoading
        }

        viewModel.eventDetails.observe(viewLifecycleOwner) { details ->
            details?.let {

                val event = it.event
                val venue = it.venue

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                eventTitle.text = event.name
                sportName.text = event.sport
                eventDate.text = event.start_time?.let { date -> dateFormat.format(date) }
                eventTime.text = event.start_time?.let { date ->
                    val startTime = timeFormat.format(date)
                    "$startTime - 2 hours"
                }

                locationName.text = venue?.name ?: "Ubicación no disponible"

                participantsCount.text = "${event.booked} / ${event.max_capacity} participants"
                participantsProgress.max = event.max_capacity
                participantsProgress.progress = event.booked
                eventDescription.text = event.description
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }

        viewModel.cancellationSuccess.observe(viewLifecycleOwner) { hasSucceeded ->
            if (hasSucceeded) {
                Toast.makeText(context, "Reserva cancelada con éxito", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Volvemos a la pantalla anterior (perfil)
            }
        }
    }
}