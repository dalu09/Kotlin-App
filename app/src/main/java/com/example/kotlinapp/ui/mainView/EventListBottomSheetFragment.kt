package com.example.kotlinapp.ui.mainView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EventListBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var eventsRecyclerView: RecyclerView
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_event_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventsRecyclerView = view.findViewById(R.id.events_recycler_view)
        eventsRecyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.selectedEvents.observe(viewLifecycleOwner) { events ->
            eventsRecyclerView.adapter = EventListAdapter(events) { eventId ->

                setFragmentResult(REQUEST_KEY, bundleOf(KEY_EVENT_ID to eventId))

            }
        }

        arguments?.getStringArrayList(ARG_EVENT_IDS)?.let { ids ->
            viewModel.loadEventsByIds(ids)
        }
    }

    companion object {

        const val REQUEST_KEY = "event_list_request"
        const val KEY_EVENT_ID = "event_id"

        private const val ARG_EVENT_IDS = "event_ids"

        fun newInstance(eventIds: List<String>): EventListBottomSheetFragment {
            val fragment = EventListBottomSheetFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_EVENT_IDS, ArrayList(eventIds))
            fragment.arguments = args
            return fragment
        }
    }
}