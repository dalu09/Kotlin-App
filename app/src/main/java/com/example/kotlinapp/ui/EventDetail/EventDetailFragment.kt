package com.example.kotlinapp.ui.eventdetail

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kotlinapp.R
import com.example.kotlinapp.data.repository.EventRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class EventDetailViewModelFactory(private val eventRepository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventDetailViewModel(eventRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class EventDetailFragment : BottomSheetDialogFragment() {

    private val viewModel: EventDetailViewModel by viewModels {
        EventDetailViewModelFactory(EventRepository(requireContext().applicationContext))
    }

    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var participantsText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var reserveButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val d = dialogInterface as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

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
        reserveButton = view.findViewById(R.id.reserveButton)

        setupObservers()
        setupClickListeners()

        val eventId = arguments?.getString("event_id")
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        } else {
            dismiss()
            Toast.makeText(requireContext(), "Error: Could not find event ID.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            titleText.text = event.name
            descriptionText.text = event.description
            val participantsString = "${event.booked} / ${event.max_capacity} participants"
            participantsText.text = participantsString
            progressBar.max = event.max_capacity
            progressBar.progress = event.booked
        }

        viewModel.bookingUiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookingUiState.AVAILABLE -> {
                    reserveButton.text = "Reserve"
                    reserveButton.isEnabled = true
                }
                is BookingUiState.BOOKED -> {
                    reserveButton.text = "Reserved"
                    reserveButton.isEnabled = false
                }
                is BookingUiState.LOADING -> {
                    reserveButton.text = "Reserving..."
                    reserveButton.isEnabled = false
                }
                is BookingUiState.OFFLINE -> {
                    reserveButton.text = "Reserve"
                    reserveButton.isEnabled = false
                    Toast.makeText(requireContext(), "No internet connection.", Toast.LENGTH_SHORT).show()
                }
                is BookingUiState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    reserveButton.text = "Reserve"
                    reserveButton.isEnabled = true
                }
                null -> { 
                    reserveButton.isEnabled = false 
                }
            }
        }
    }

    private fun setupClickListeners() {
        reserveButton.setOnClickListener {
            val eventId = arguments?.getString("event_id")
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (eventId != null && userId != null) {
                viewModel.createBooking(eventId, userId)
            } else if (userId == null) {
                Toast.makeText(requireContext(), "Error: You must be logged in to book an event.", Toast.LENGTH_LONG).show()
            }
        }
    }
}