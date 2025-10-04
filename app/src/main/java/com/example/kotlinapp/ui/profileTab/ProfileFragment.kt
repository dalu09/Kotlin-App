// ProfileFragment.kt
package com.example.kotlinapp.ui.profileTab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.example.kotlinapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usernameTextView: TextView = view.findViewById(R.id.profile_name)
        val descriptionTextView: TextView = view.findViewById(R.id.profile_bio)
        val ratingTextView: TextView = view.findViewById(R.id.profile_rating)
        val sportTagsGroup: ChipGroup = view.findViewById(R.id.sport_tags_group)

        setupObservers(usernameTextView, descriptionTextView, ratingTextView, sportTagsGroup)

        viewModel.startListeningForUserProfile()
    }

    private fun setupObservers(
        usernameTextView: TextView,
        descriptionTextView: TextView,
        ratingTextView: TextView,
        sportTagsGroup: ChipGroup
    ) {
        viewModel.username.observe(viewLifecycleOwner) { username ->
            usernameTextView.text = username ?: "Nombre no disponible"
        }

        viewModel.description.observe(viewLifecycleOwner) { description ->
            descriptionTextView.text = description ?: "Sin descripción."
        }

        viewModel.numRating.observe(viewLifecycleOwner) { count ->
            val avg = viewModel.avgRating.value ?: 0.0
            updateRatingUI(ratingTextView, avg, count)
        }

        viewModel.avgRating.observe(viewLifecycleOwner) { avg ->
            val count = viewModel.numRating.value ?: 0L
            updateRatingUI(ratingTextView, avg, count)
        }

        viewModel.sportList.observe(viewLifecycleOwner) { sports ->
            updateSportsChips(sports, sportTagsGroup)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateRatingUI(ratingTextView: TextView, avg: Double, count: Long) {
        if (count > 0) {
            // Si hay 1 o más reseñas, mostramos las estrellas y el contador
            ratingTextView.text = "★ %.1f (%d)".format(avg, count)
        } else {
            // Si el contador es 0, mostramos el mensaje "Sin calificaciones"
            ratingTextView.text = "Sin calificaciones"
        }
    }
    private fun updateSportsChips(sports: List<String>, sportTagsGroup: ChipGroup) {
        sportTagsGroup.removeAllViews()

        sports.forEach { sportName ->
            val chip = Chip(context).apply {
                text = sportName
                isClickable = false
            }
            sportTagsGroup.addView(chip)
        }
    }
}
