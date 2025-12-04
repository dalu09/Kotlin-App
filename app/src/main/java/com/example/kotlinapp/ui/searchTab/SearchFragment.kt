package com.example.kotlinapp.ui.searchTab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.data.service.AllEventsAdapter
import com.example.kotlinapp.data.service.RecommendedEventsAdapter
import com.example.kotlinapp.databinding.FragmentSearchBinding

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

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(EventRepository(requireContext().applicationContext))
    }

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var recommendedAdapter: RecommendedEventsAdapter
    private lateinit var allEventsAdapter: AllEventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupClickListeners()
        setupObservers()
    }

    private fun setupAdapters() {
        val onEventClicked: (Event) -> Unit = { event ->
            if (event.id.isNotEmpty()) {
                val action = SearchFragmentDirections.actionSearchFragmentToEventDetailFragment(event.id)
                findNavController().navigate(action)
            }
        }

        recommendedAdapter = RecommendedEventsAdapter(onEventClicked)
        binding.recommendedEventsRecycler.adapter = recommendedAdapter
        binding.recommendedEventsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        allEventsAdapter = AllEventsAdapter(onEventClicked)
        binding.allEventsRecycler.adapter = allEventsAdapter
        binding.allEventsRecycler.layoutManager = LinearLayoutManager(context)
        binding.allEventsRecycler.isNestedScrollingEnabled = false
    }

    private fun setupClickListeners() {

        binding.retryButton.setOnClickListener {
            viewModel.onRetry()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingSpinner.isVisible = isLoading && (viewModel.networkError.value == false)
        }

        viewModel.networkError.observe(viewLifecycleOwner) { hasError ->

            binding.connectionErrorLayout.isVisible = hasError
            binding.contentGroup.isVisible = !hasError
        }

        viewModel.recommendedEvents.observe(viewLifecycleOwner) { events ->
            recommendedAdapter.submitList(events)
        }

        viewModel.allEvents.observe(viewLifecycleOwner) { events ->
            allEventsAdapter.submitList(events)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorShown()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
