package com.example.kotlinapp.ui.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class CreateAccountFragment : Fragment() {

    private lateinit var viewModel: CreateAccountViewModel
    private var userUid: String? = null


    private val sportsOptions = arrayOf("Football", "Basketball", "Volleyball")

    private val selectedSports = mutableSetOf<String>()


    private lateinit var usernameEdit: EditText
    private lateinit var descriptionEdit: EditText
    private lateinit var createAccountButton: Button
    private lateinit var progressBar: ProgressBar
    // AÑADIDO: Nuevas vistas para la selección de deportes
    private lateinit var sportsSelector: TextView
    private lateinit var sportsChipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userUid = it.getString("user_uid")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateAccountViewModel::class.java)


        usernameEdit = view.findViewById(R.id.editUsername)
        descriptionEdit = view.findViewById(R.id.editDescription)
        createAccountButton = view.findViewById(R.id.btnCreateAccount)
        progressBar = view.findViewById(R.id.progressBar)

        sportsSelector = view.findViewById(R.id.sportsSelector)
        sportsChipGroup = view.findViewById(R.id.sportsChipGroup)


        sportsSelector.setOnClickListener {
            showSportsSelectionDialog()
        }

        createAccountButton.setOnClickListener {
            val uid = userUid
            if (uid == null) {
                Toast.makeText(context, "Error: No se encontró el ID de usuario.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val username = usernameEdit.text.toString()
            val description = descriptionEdit.text.toString()

            // MODIFICADO: Pasamos la lista de deportes seleccionados al ViewModel
            viewModel.createAccount(uid, username, selectedSports.toList(), description)
        }

        observeProfileCreationState()
    }


    private fun showSportsSelectionDialog() {
        val checkedItems = sportsOptions.map { selectedSports.contains(it) }.toBooleanArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Sports")
            .setMultiChoiceItems(sportsOptions, checkedItems) { _, which, isChecked ->
                val sport = sportsOptions[which]
                if (isChecked) {
                    selectedSports.add(sport)
                } else {
                    selectedSports.remove(sport)
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                updateSportsChips()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun updateSportsChips() {
        sportsChipGroup.removeAllViews()
        selectedSports.forEach { sport ->
            val chip = Chip(context)
            chip.text = sport
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                selectedSports.remove(sport)
                updateSportsChips()
            }
            sportsChipGroup.addView(chip)
        }
    }

    private fun observeProfileCreationState() {
        viewModel.profileCreationState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateAccountViewModel.ProfileCreationState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    createAccountButton.isEnabled = false
                }
                is CreateAccountViewModel.ProfileCreationState.Success -> {
                    progressBar.visibility = View.GONE
                    createAccountButton.isEnabled = true
                    Toast.makeText(context, "¡Perfil creado con éxito!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_createAccountFragment_to_loginFragment)
                }
                is CreateAccountViewModel.ProfileCreationState.Error -> {
                    progressBar.visibility = View.GONE
                    createAccountButton.isEnabled = true
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is CreateAccountViewModel.ProfileCreationState.Idle -> {
                    progressBar.visibility = View.GONE
                    createAccountButton.isEnabled = true
                }
            }
        }
    }
}