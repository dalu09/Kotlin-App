package com.example.kotlinapp.ui.editevent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.google.android.material.textfield.TextInputEditText

class EditEventFragment : Fragment() {

    private val viewModel: EditEventViewModel by viewModels()

    private lateinit var nameEdit: TextInputEditText
    private lateinit var descriptionEdit: TextInputEditText
    private lateinit var sportDropdown: AutoCompleteTextView
    private lateinit var updateButton: Button
    private lateinit var cancelButton: Button

    private val sportsOptions = listOf("Football", "Basketball", "Volleyball")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        nameEdit = view.findViewById(R.id.event_name_edit_text)
        descriptionEdit = view.findViewById(R.id.description_edit_text)
        sportDropdown = view.findViewById(R.id.sport_auto_complete)
        updateButton = view.findViewById(R.id.update_event_button)
        cancelButton = view.findViewById(R.id.cancel_button)


        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sportsOptions)
        sportDropdown.setAdapter(adapter)


        val eventId = arguments?.getString("event_id")
        if (eventId != null) {
            viewModel.loadEvent(eventId)
        } else {
            Toast.makeText(context, "Error: No se encontró el ID del evento", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }


        updateButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val description = descriptionEdit.text.toString()
            val sport = sportDropdown.text.toString()

            viewModel.updateEvent(eventId, name, description, sport)
        }

        cancelButton.setOnClickListener {

            findNavController().popBackStack()
        }


        setupObservers()
    }

    private fun setupObservers() {

        viewModel.event.observe(viewLifecycleOwner) { event ->
            if (event != null) {

                if (nameEdit.text.isNullOrEmpty()) nameEdit.setText(event.name)
                if (descriptionEdit.text.isNullOrEmpty()) descriptionEdit.setText(event.description)
                if (sportDropdown.text.isNullOrEmpty()) {
                    sportDropdown.setText(event.sport, false)
                }
            }
        }


        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Evento actualizado correctamente", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure { e ->
                Toast.makeText(context, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }


        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }
}