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
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Venue
import com.example.kotlinapp.data.repository.EventRepository
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()
    private lateinit var createButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createButton = view.findViewById(R.id.create_event_button)

        setupDateTimePickers(view)
        setupObservers(view)
        setupClickListeners(view)
        
        viewModel.loadInitialData()
    }

    private fun setupObservers(view: View) {
        viewModel.formState.observe(viewLifecycleOwner) { state ->
            val sportsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.sports)
            view.findViewById<AutoCompleteTextView>(R.id.sport_auto_complete).setAdapter(sportsAdapter)

            val skillLevelsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.skillLevels)
            view.findViewById<AutoCompleteTextView>(R.id.skill_level_auto_complete).setAdapter(skillLevelsAdapter)

            val venueNames = state.venues.map { it.name }
            val venuesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, venueNames)
            view.findViewById<AutoCompleteTextView>(R.id.venue_auto_complete).setAdapter(venuesAdapter)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateEventUiState.SUCCESS -> {
                    createButton.isEnabled = false
                    Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    viewModel.onDone()
                }
                is CreateEventUiState.ERROR -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    // Re-enable the button if the error is not fatal (e.g., a validation error)
                    createButton.isEnabled = !state.isFatal
                }
                is CreateEventUiState.IDLE -> {
                    createButton.isEnabled = true
                }
            }
        }
    }

    private fun setupClickListeners(view: View) {
        createButton.setOnClickListener {
            createButton.isEnabled = false

            val name = view.findViewById<TextInputEditText>(R.id.event_name_edit_text).text.toString()
            val description = view.findViewById<TextInputEditText>(R.id.description_edit_text).text.toString()
            val sport = view.findViewById<AutoCompleteTextView>(R.id.sport_auto_complete).text.toString()
            val skillLevel = view.findViewById<AutoCompleteTextView>(R.id.skill_level_auto_complete).text.toString()
            val venue = view.findViewById<AutoCompleteTextView>(R.id.venue_auto_complete).text.toString()
            val maxParticipants = view.findViewById<TextInputEditText>(R.id.max_participants_edit_text).text.toString()

            viewModel.createEvent(
                name = name, description = description, sport = sport, skillLevel = skillLevel,
                venueName = venue, maxParticipants = maxParticipants, 
                startTime = if (isDateSet(startCalendar)) startCalendar.time else null,
                endTime = if (isDateSet(endCalendar)) endCalendar.time else null
            )
        }
    }

    private fun setupDateTimePickers(view: View) {
        val startTimeEditText = view.findViewById<TextInputEditText>(R.id.start_time_edit_text)
        val endTimeEditText = view.findViewById<TextInputEditText>(R.id.end_time_edit_text)

        startTimeEditText.setOnClickListener { showDateTimePickerDialog(startTimeEditText, startCalendar) }
        endTimeEditText.setOnClickListener { showDateTimePickerDialog(endTimeEditText, endCalendar) }
    }

    private fun showDateTimePickerDialog(editText: TextInputEditText, calendar: Calendar) {
        val now = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                updateDateTimeInView(editText, calendar)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateTimeInView(editText: TextInputEditText, calendar: Calendar) {
        val format = "dd/MM/yyyy HH:mm"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        editText.setText(sdf.format(calendar.time))
    }

    private fun isDateSet(calendar: Calendar): Boolean {
        return calendar.timeInMillis != Calendar.getInstance().apply { clear() }.timeInMillis
    }
}
