package com.example.kotlinapp.ui.mainView

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Event
import com.example.kotlinapp.data.repository.EventRepository
import com.example.kotlinapp.util.MessageWrapper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint

class MainViewModelFactory(private val eventRepository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(eventRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val TAG = "MainFragment"
    }

    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: GeoPoint? = null

    private var isShowingNearby = true

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(EventRepository(requireContext().applicationContext))
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(requireContext(), "El permiso de ubicación es necesario", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupResultListener(view) // Pasamos la vista aquí
        setupObservers()
        setupClickListeners(view)
    }

    // Recibimos la vista como parámetro
    private fun setupResultListener(view: View) {
        childFragmentManager.setFragmentResultListener(
            EventListBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val eventId = bundle.getString(EventListBottomSheetFragment.KEY_EVENT_ID)
            eventId?.let {
                // --- CAMBIO EMPIEZA AQUÍ: Solución a la condición de carrera ---
                // Postponemos la navegación al siguiente ciclo de la UI para dar tiempo
                // a que el BottomSheet se cierre por completo.
                view.post {
                    // Comprobamos que seguimos en el destino correcto antes de navegar
                    if (findNavController().currentDestination?.id == R.id.mainFragment) {
                        val navBundle = Bundle().apply { putString("event_id", it) }
                        findNavController().navigate(R.id.action_mainFragment_to_eventDetailFragment, navBundle)
                    }
                }
                // --- CAMBIO TERMINA AQUÍ ---
            }
        }
    }

    private fun setupClickListeners(view: View) {
        val toggleButton = view.findViewById<MaterialButton>(R.id.toggleEventsButton)
        toggleButton.setOnClickListener {
            isShowingNearby = !isShowingNearby
            if (isShowingNearby) {
                toggleButton.text = "Eventos Cercanos"
                toggleButton.setIconResource(R.drawable.ic_location_nearby)
                lastKnownLocation?.let {
                    viewModel.loadNearbyEvents(it, 10000.0)
                } ?: enableMyLocation()
            } else {
                toggleButton.text = "Ver Todos"
                toggleButton.setIconResource(R.drawable.ic_view_grid)
                viewModel.loadAllEvents()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap?.setOnMarkerClickListener(this)
        enableMyLocation()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val eventIds = marker.tag as? List<*>
        if (eventIds.isNullOrEmpty()) return true

        if (eventIds.size == 1) {
            val singleEventId = eventIds.first() as? String
            singleEventId?.let {
                val bundle = Bundle().apply { putString("event_id", it) }
                findNavController().navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle)
            }
        } else {
            val stringEventIds = eventIds.mapNotNull { it as? String }
            if (stringEventIds.isNotEmpty()) {
                val bottomSheet = EventListBottomSheetFragment.newInstance(stringEventIds)
                bottomSheet.show(childFragmentManager, bottomSheet.tag)
            }
        }

        return true
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MainUiState.Loading -> Log.d(TAG, "Cargando eventos...")
                is MainUiState.Success -> {
                    Log.d(TAG, "Eventos cargados desde la red.")
                    addEventsToMap(state.events)
                }
                is MainUiState.Stale -> {
                    Log.d(TAG, "Mostrando eventos desde la caché.")
                    addEventsToMap(state.events)
                    state.message.getContentIfNotHandled()?.let { message ->
                        showOfflineSnackbar(message)
                    }
                }
                is MainUiState.Error -> {
                    state.message.getContentIfNotHandled()?.let { errorMessage ->
                        Log.e(TAG, "Error: $errorMessage")
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showOfflineSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("REINTENTAR") {
                    if (isShowingNearby) {
                        lastKnownLocation?.let { loc -> viewModel.loadNearbyEvents(loc, 10000.0) }
                            ?: enableMyLocation()
                    } else {
                        viewModel.loadAllEvents()
                    }
                }
                .show()
        }
    }

    private fun addEventsToMap(events: List<Event>) {
        gMap?.clear()
        val customMarkerIcon = bitmapDescriptorFromVector(R.drawable.icon_sl, 130, 130)

        val eventsByLocation = events.groupBy { it.location }

        eventsByLocation.forEach { (geoPoint, eventsAtLocation) ->
            geoPoint?.let {
                val latLng = LatLng(it.latitude, it.longitude)

                val markerOptions = MarkerOptions().position(latLng).title(eventsAtLocation.first().name)
                customMarkerIcon?.let { markerOptions.icon(it) }

                val marker = gMap?.addMarker(markerOptions)
                marker?.tag = eventsAtLocation.map { event -> event.id }
            }
        }
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int, width: Int, height: Int): BitmapDescriptor? {
        return context?.let {
            ContextCompat.getDrawable(it, vectorResId)?.let { drawable ->
                drawable.setBounds(0, 0, width, height)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.draw(canvas)
                BitmapDescriptorFactory.fromBitmap(bitmap)
            }
        }
    }

    private fun enableMyLocation() {
        if (gMap == null || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        gMap?.isMyLocationEnabled = true

        if (viewModel.uiState.value == null) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                if (location != null) {
                    val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                    lastKnownLocation = userGeoPoint
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    gMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
                    viewModel.loadNearbyEvents(userGeoPoint, 10000.0)
                } else {
                    Toast.makeText(requireContext(), "No se pudo obtener la ubicación.", Toast.LENGTH_LONG).show()
                    viewModel.loadAllEvents()
                }
            }
        } else {
            lastKnownLocation?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                gMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
            }
        }
    }
}