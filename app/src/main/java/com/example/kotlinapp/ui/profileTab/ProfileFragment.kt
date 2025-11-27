package com.example.kotlinapp.ui.profileTab

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.os.Bundle

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var signOutButton: Button
    private lateinit var editProfileButton: Button
    private lateinit var profileImageView: ImageView
    private lateinit var upcomingEventsRecyclerView: RecyclerView
    private lateinit var eventsProgressBar: ProgressBar
    private lateinit var upcomingEventsAdapter: UpcomingEventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signOutButton = view.findViewById(R.id.btnSignOut)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        profileImageView = view.findViewById(R.id.profile_image)

        upcomingEventsRecyclerView = view.findViewById(R.id.upcoming_events_recycler_view)

        setupRecyclerView()
        setupClickListeners()
        setupObservers(view)
    }

    private fun setupRecyclerView() {

        upcomingEventsAdapter = UpcomingEventsAdapter(emptyList())
        upcomingEventsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        upcomingEventsRecyclerView.adapter = upcomingEventsAdapter
    }

    private fun setupClickListeners() {
        signOutButton.setOnClickListener {
            viewModel.onSignOutClicked()
        }
        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
    }

    private fun setupObservers(rootView: View) {
        val usernameTextView: TextView = rootView.findViewById(R.id.profile_name)
        val descriptionTextView: TextView = rootView.findViewById(R.id.profile_bio)
        val ratingTextView: TextView = rootView.findViewById(R.id.profile_rating)
        val sportTagsGroup: ChipGroup = rootView.findViewById(R.id.sport_tags_group)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                usernameTextView.text = user.username ?: "Nombre no disponible"
                descriptionTextView.text = user.description ?: "Sin descripción."
                updateRatingUI(ratingTextView, user.avgRating, user.numRating)
                updateSportsChips(user.sportList, sportTagsGroup)
            } else {

                usernameTextView.text = "Cargando perfil..."
                descriptionTextView.text = ""
                ratingTextView.text = ""
                sportTagsGroup.removeAllViews()
            }
        }

        viewModel.profileImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap)
            } else {
                profileImageView.setImageResource(R.drawable.profle_default)
            }
        }

        viewModel.upcomingEvents.observe(viewLifecycleOwner) { events ->
            upcomingEventsAdapter.updateEvents(events)

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
}
