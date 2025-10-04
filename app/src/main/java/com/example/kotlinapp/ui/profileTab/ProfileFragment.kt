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

        // Referencias a las vistas
        val usernameTextView: TextView = view.findViewById(R.id.profile_name)
        val descriptionTextView: TextView = view.findViewById(R.id.profile_bio)
        val ratingTextView: TextView = view.findViewById(R.id.profile_rating)
        // --- NUEVO: Referencia al ChipGroup ---
        val sportTagsGroup: ChipGroup = view.findViewById(R.id.sport_tags_group)
        // --- FIN DE LO NUEVO ---

        // Configurar los observadores
        setupObservers(usernameTextView, descriptionTextView, ratingTextView, sportTagsGroup)

        // Iniciar la carga de los datos
        viewModel.loadUserProfile()
    }

    private fun setupObservers(
        usernameTextView: TextView,
        descriptionTextView: TextView,
        ratingTextView: TextView,
        sportTagsGroup: ChipGroup // --- NUEVO: Parámetro añadido ---
    ) {
        // ... observadores de username, description, avgRating, isLoading, error (sin cambios) ...
        viewModel.username.observe(viewLifecycleOwner) { /* ... */ }
        viewModel.description.observe(viewLifecycleOwner) { /* ... */ }
        viewModel.avgRating.observe(viewLifecycleOwner) { /* ... */ }

        // --- NUEVO: Observador para la lista de deportes ---
        viewModel.sportList.observe(viewLifecycleOwner) { sports ->
            // Limpia los chips anteriores antes de añadir nuevos
            sportTagsGroup.removeAllViews()

            // Crea un Chip para cada deporte en la lista
            sports.forEach { sportName ->
                val chip = Chip(context) // Crea una nueva instancia de Chip
                chip.text = sportName   // Asigna el nombre del deporte
                chip.setChipBackgroundColorResource(R.color.gris) // Puedes personalizar el color
                chip.isClickable = false // Para que no parezcan botones
                chip.isCheckable = false
                sportTagsGroup.addView(chip) // Añade el Chip al grupo
            }
        }
        // --- FIN DE LO NUEVO ---
    }
}
