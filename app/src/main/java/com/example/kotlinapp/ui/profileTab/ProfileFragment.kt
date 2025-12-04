package com.example.kotlinapp.ui.profileTab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinapp.R
import com.example.kotlinapp.databinding.FragmentProfileBinding // 1. Importar View Binding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var upcomingEventsAdapter: UpcomingEventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserEvents()
    }

    private fun setupRecyclerView() {
        upcomingEventsAdapter = UpcomingEventsAdapter(emptyList())
        binding.upcomingEventsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.upcomingEventsRecyclerView.adapter = upcomingEventsAdapter
    }

    private fun setupClickListeners() {
        binding.btnSignOut.setOnClickListener {
            viewModel.onSignOutClicked()
        }
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
        binding.retryButton.setOnClickListener {
            viewModel.onRetry()
        }

        binding.backArrow.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.profileName.text = user.username ?: "Nombre no disponible"
                binding.profileBio.text = user.description ?: "Sin descripción."
                updateRatingUI(binding.profileRating, user.avgRating, user.numRating)
                updateSportsChips(user.sportList, binding.sportTagsGroup)
            } else {
                binding.profileName.text = "Cargando perfil..."
                binding.profileBio.text = ""
                binding.profileRating.text = ""
                binding.sportTagsGroup.removeAllViews()
            }
        }

        viewModel.profileImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.profileImage.setImageBitmap(bitmap)
            } else {
                binding.profileImage.setImageResource(R.drawable.profle_default)
            }
        }

        viewModel.upcomingEvents.observe(viewLifecycleOwner) { events ->
            upcomingEventsAdapter.updateEvents(events)
        }

        viewModel.postedEvents.observe(viewLifecycleOwner) { events ->
            binding.postedEventsContainer.removeAllViews()

            if (events.isNullOrEmpty()) {
                val emptyView = TextView(requireContext()).apply {
                    text = "No events posted yet."
                    setPadding(0, 16, 0, 16)
                }
                binding.postedEventsContainer.addView(emptyView)
            } else {
                val inflater = LayoutInflater.from(requireContext())
                events.forEach { event ->
                    val cardView = inflater.inflate(R.layout.item_posted_event, binding.postedEventsContainer, false)

                    val nameText = cardView.findViewById<TextView>(R.id.item_event_name)
                    val sportText = cardView.findViewById<TextView>(R.id.item_event_sport)

                    nameText.text = event.name
                    sportText.text = event.sport

                    cardView.setOnClickListener {
                        val bundle = Bundle().apply { putString("event_id", event.id) }
                        findNavController().navigate(R.id.action_profileFragment_to_editEventFragment, bundle)
                    }
                    binding.postedEventsContainer.addView(cardView)
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.navigateToLogin.observe(viewLifecycleOwner) { navigate ->
            if (navigate == true) {
                findNavController().navigate(R.id.action_global_to_loginFragment)
                viewModel.onNavigationComplete()
            }
        }

        viewModel.networkError.observe(viewLifecycleOwner) { hasError ->
            binding.networkErrorLayout.isVisible = hasError
            binding.upcomingEventsTitle.isVisible = !hasError
            binding.upcomingEventsRecyclerView.isVisible = !hasError
            binding.postedEventsTitle.isVisible = !hasError
            binding.postedEventsContainer.isVisible = !hasError
        }
    }

    private fun updateRatingUI(ratingTextView: TextView, avg: Double, count: Long) {
        ratingTextView.text = if (count > 0) "★ %.1f (%d)".format(avg, count) else "Sin calificaciones"
    }

    private fun updateSportsChips(sports: List<String>, sportTagsGroup: ChipGroup) {
        sportTagsGroup.removeAllViews()
        sports.forEach { sportName ->
            val chip = Chip(sportTagsGroup.context).apply {
                text = sportName
                isClickable = false
            }
            sportTagsGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
