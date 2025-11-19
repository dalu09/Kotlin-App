package com.example.kotlinapp.ui.createEvent

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.repository.EventRepository
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Factory para crear el ViewModel
class CreateEventViewModelFactory(private val eventRepository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateEventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateEventViewModel(eventRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CreateEventFragment : Fragment() {

    private val viewModel: CreateEventViewModel by viewModels {
        CreateEventViewModelFactory(EventRepository(requireContext().applicationContext))
    }

    private var venuesList: List<Venue> = emptyList()

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(view)
        viewModel.loadInitialData()

        val startTimeEditText = view.findViewById<TextInputEditText>(R.id.start_time_edit_text)
        val endTimeEditText = view.findViewById<TextInputEditText>(R.id.end_time_edit_text)

        startTimeEditText.setOnClickListener {
            showDateTimePickerDialog(startTimeEditText, startCalendar)
        }

        endTimeEditText.setOnClickListener {
            showDateTimePickerDialog(endTimeEditText, endCalendar)
        }

        // Configurar el listener del botón de creación
        val createButton = view.findViewById<Button>(R.id.create_event_button)
        createButton.setOnClickListener {
            val name = view.findViewById<TextInputEditText>(R.id.event_name_edit_text).text.toString()
            val description = view.findViewById<TextInputEditText>(R.id.description_edit_text).text.toString()
            val sport = view.findViewById<AutoCompleteTextView>(R.id.sport_auto_complete).text.toString()
            val skillLevel = view.findViewById<AutoCompleteTextView>(R.id.skill_level_auto_complete).text.toString()
            val venue = view.findViewById<AutoCompleteTextView>(R.id.venue_auto_complete).text.toString()
            val maxParticipants = view.findViewById<TextInputEditText>(R.id.max_participants_edit_text).text.toString()

            viewModel.createEvent(
                name = name,
                description = description,
                sport = sport,
                skillLevel = skillLevel,
                venueName = venue,
                maxParticipants = maxParticipants,
                startTime = startCalendar.time,
                endTime = endCalendar.time
            )
        }
    }

    private fun setupObservers(view: View) {
        viewModel.formState.observe(viewLifecycleOwner) { state ->

            // Manejar errores de validación o de la API
            state.error?.let { errorMessage ->
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.onEventCreationNotified() // Limpiar el estado del error para no mostrarlo de nuevo
            }

            // Manejar evento de creación exitosa
            if (state.isEventCreated) {
                Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show()
                clearForm(view)
                viewModel.onEventCreationNotified() // Limpiar el estado de éxito
            }

            // Poblar los desplegables
            val sportsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.sports)
            view.findViewById<AutoCompleteTextView>(R.id.sport_auto_complete).setAdapter(sportsAdapter)

            val skillLevelsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.skillLevels)
            view.findViewById<AutoCompleteTextView>(R.id.skill_level_auto_complete).setAdapter(skillLevelsAdapter)

            venuesList = state.venues
            val venueNames = state.venues.map { it.name }
            val venuesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, venueNames)
            view.findViewById<AutoCompleteTextView>(R.id.venue_auto_complete).setAdapter(venuesAdapter)
        }
    }

    private fun showDateTimePickerDialog(editText: TextInputEditText, calendar: Calendar) {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                        updateDateTimeInView(editText, calendar)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateTimeInView(editText: TextInputEditText, calendar: Calendar) {
        val format = "dd/MM/yyyy HH:mm"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        editText.setText(sdf.format(calendar.time))
    }

    private fun clearForm(view: View) {
        view.findViewById<TextInputEditText>(R.id.event_name_edit_text).text?.clear()
        view.findViewById<TextInputEditText>(R.id.description_edit_text).text?.clear()
        view.findViewById<AutoCompleteTextView>(R.id.sport_auto_complete).setText("", false)
        view.findViewById<AutoCompleteTextView>(R.id.skill_level_auto_complete).setText("", false)
        view.findViewById<AutoCompleteTextView>(R.id.venue_auto_complete).setText("", false)
        view.findViewById<TextInputEditText>(R.id.max_participants_edit_text).text?.clear()
        view.findViewById<TextInputEditText>(R.id.start_time_edit_text).text?.clear()
        view.findViewById<TextInputEditText>(R.id.end_time_edit_text).text?.clear()
    }
}
