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

        // Nuevo método de observadores.
        setupObservers(usernameTextView, descriptionTextView, ratingTextView, sportTagsGroup)

    }

    private fun setupObservers(
        usernameTextView: TextView,
        descriptionTextView: TextView,
        ratingTextView: TextView,
        sportTagsGroup: ChipGroup
    ) {

        viewModel.user.observe(viewLifecycleOwner) { user ->
            // Si el usuario es nulo mostrar un estado por defecto.
            if (user == null) {
                usernameTextView.text = "Cargando perfil..."
                descriptionTextView.text = ""
                ratingTextView.text = ""
                sportTagsGroup.removeAllViews()
            } else {
                usernameTextView.text = user.username ?: "Nombre no disponible"
                descriptionTextView.text = user.description ?: "Sin descripción."
                updateRatingUI(ratingTextView, user.avgRating, user.numRating)
                updateSportsChips(user.sportList, sportTagsGroup)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateRatingUI(ratingTextView: TextView, avg: Double, count: Long) {
        if (count > 0) {
            ratingTextView.text = "★ %.1f (%d)".format(avg, count)
        } else {
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