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
import com.example.kotlinapp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class EventDetailFragment : BottomSheetDialogFragment() {

    private val viewModel: EventDetailViewModel by viewModels()

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
            Toast.makeText(requireContext(), "Error: No se pudo encontrar el ID del evento.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            titleText.text = event.name
            descriptionText.text = event.description
            val participantsString = "${event.booked} / ${event.max_capacity} participantes"
            participantsText.text = participantsString
            progressBar.progress = if (event.max_capacity > 0) (100 * event.booked) / event.max_capacity else 0
            reserveButton.isEnabled = event.booked < event.max_capacity
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                arguments?.getString("event_id")?.let { eventId -> viewModel.loadEvent(eventId) }
            }
        }
    }

    private fun setupClickListeners() {
        reserveButton.setOnClickListener {
            val eventId = arguments?.getString("event_id")
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (eventId != null && userId != null) {
                reserveButton.isEnabled = false
                viewModel.createBooking(eventId, userId)
            } else {
                Toast.makeText(requireContext(), "Error: Debes iniciar sesión para reservar.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
