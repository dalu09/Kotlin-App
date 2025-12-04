package com.example.kotlinapp.ui.bookedEventDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.databinding.FragmentBookedEventDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class BookedEventDetailFragment : Fragment() {

    private val viewModel: BookedEventDetailViewModel by viewModels {
        BookedEventDetailViewModelFactory(EventRepository(requireContext().applicationContext))
    }
    private val args: BookedEventDetailFragmentArgs by navArgs()

    private var _binding: FragmentBookedEventDetailBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookedEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = args.eventId

        setupClickListeners(eventId)
        setupObservers()

        viewModel.loadEventAndVenue(eventId)
    }

    private fun setupClickListeners(eventId: String) {
        binding.backArrowIcon.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.cancelReservationButton.setOnClickListener {
            viewModel.onCancelBookingClicked(eventId)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingSpinner.isVisible = isLoading
            binding.cancelReservationButton.isEnabled = !isLoading
        }

        viewModel.eventDetails.observe(viewLifecycleOwner) { details ->
            details?.let {
                val event = it.event
                val venue = it.venue

                binding.eventTitle.text = event.name
                binding.sportName.text = event.sport
                binding.eventDate.text = event.start_time?.let { date -> dateFormat.format(date) }
                binding.eventTime.text = event.start_time?.let { date ->
                    val startTime = timeFormat.format(date)
                    "$startTime - 2 hours"
                }
                binding.locationName.text = venue?.name ?: "Ubicación no disponible"
                binding.participantsCount.text = "${event.booked} / ${event.max_capacity} participants"
                binding.participantsProgress.max = event.max_capacity
                binding.participantsProgress.progress = event.booked
                binding.eventDescription.text = event.description
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
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
