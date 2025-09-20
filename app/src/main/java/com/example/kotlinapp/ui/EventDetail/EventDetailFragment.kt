package com.example.kotlinapp.ui.eventdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.kotlinapp.R

class EventDetailFragment : Fragment() {

    private lateinit var viewModel: EventDetailViewModel

    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var participantsText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_event_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(EventDetailViewModel::class.java)

        titleText = view.findViewById(R.id.eventTitle)
        descriptionText = view.findViewById(R.id.eventDescription)
        participantsText = view.findViewById(R.id.participants)
        progressBar = view.findViewById(R.id.progressParticipants)

        viewModel.title.observe(viewLifecycleOwner) { title ->
            titleText.text = title
        }
        viewModel.description.observe(viewLifecycleOwner) { description ->
            descriptionText.text = description
        }
        viewModel.participants.observe(viewLifecycleOwner) { participants ->
            participantsText.text = participants
        }
        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            progressBar.progress = progress
        }
    }
}
