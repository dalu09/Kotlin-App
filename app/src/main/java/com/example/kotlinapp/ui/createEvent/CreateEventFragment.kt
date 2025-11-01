package com.example.kotlinapp.ui.createEvent

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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

// La Factory para crear el ViewModel no cambia
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

    // 1. Calendarios para guardar la fecha y hora seleccionadas
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

        // 2. Configurar los click listeners para los campos de fecha/hora
        val startTimeEditText = view.findViewById<TextInputEditText>(R.id.start_time_edit_text)
        val endTimeEditText = view.findViewById<TextInputEditText>(R.id.end_time_edit_text)

        startTimeEditText.setOnClickListener {
            showDateTimePickerDialog(startTimeEditText, startCalendar)
        }

        endTimeEditText.setOnClickListener {
            showDateTimePickerDialog(endTimeEditText, endCalendar)
        }
    }

    private fun setupObservers(view: View) {
        viewModel.formState.observe(viewLifecycleOwner) { state ->
            state.error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }

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

    // 3. Función para mostrar los diálogos de fecha y hora en secuencia
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
                    false // Usar formato de 12 o 24 horas según la configuración del dispositivo
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // 4. Función para formatear y mostrar la fecha en el EditText
    private fun updateDateTimeInView(editText: TextInputEditText, calendar: Calendar) {
        val format = "dd/MM/yyyy HH:mm" // Formato deseado
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        editText.setText(sdf.format(calendar.time))
    }
}