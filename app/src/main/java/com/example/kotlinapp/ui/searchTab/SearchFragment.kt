package com.example.kotlinapp.ui.searchTab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository

// 1. Factory para poder crear el ViewModel con su dependencia (EventRepository)
class SearchViewModelFactory(private val eventRepository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(eventRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SearchFragment : Fragment() {

    // 2. Se actualiza la inicialización del ViewModel para que use la Factory
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(EventRepository(requireContext().applicationContext))
    }

    private lateinit var recommendedAdapter: RecommendedEventsAdapter
    private lateinit var allEventsAdapter: AllEventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onEventClicked: (Event) -> Unit = { event ->
            // Asegurarse de que el evento tenga un ID antes de navegar
            if (event.id.isNotEmpty()) {
                val action = SearchFragmentDirections.actionSearchFragmentToEventDetailFragment(event.id)
                findNavController().navigate(action)
            }
        }

        recommendedAdapter = RecommendedEventsAdapter(onEventClicked)
        allEventsAdapter = AllEventsAdapter(onEventClicked)

        val recommendedRecycler: RecyclerView = view.findViewById(R.id.recommended_events_recycler)
        recommendedRecycler.adapter = recommendedAdapter

        val allEventsRecycler: RecyclerView = view.findViewById(R.id.all_events_recycler)
        allEventsRecycler.adapter = allEventsAdapter

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.recommendedEvents.observe(viewLifecycleOwner) { events ->
            recommendedAdapter.submitList(events)
        }

        viewModel.allEvents.observe(viewLifecycleOwner) { events ->
            allEventsAdapter.submitList(events)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }
}