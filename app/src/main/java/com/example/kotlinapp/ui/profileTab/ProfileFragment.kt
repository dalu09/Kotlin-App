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
        val ratingTextView: TextView = view.findViewById(R.id.profile_rating) // Asumiendo que tienes un TextView para el rating

        setupObservers(usernameTextView, descriptionTextView, ratingTextView)

        viewModel.loadUserProfile()
    }

    private fun setupObservers(
        usernameTextView: TextView,
        descriptionTextView: TextView,
        ratingTextView: TextView
    ) {
        viewModel.username.observe(viewLifecycleOwner) { username ->
            usernameTextView.text = username ?: "Nombre no disponible"
        }

        viewModel.description.observe(viewLifecycleOwner) { description ->
            descriptionTextView.text = description ?: "Sin descripción."
        }

        viewModel.avgRating.observe(viewLifecycleOwner) { avgRating ->

            val ratingText = avgRating?.let { "★ %.1f".format(it) } ?: "Sin calificación"
            ratingTextView.text = ratingText
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {

                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }
}
