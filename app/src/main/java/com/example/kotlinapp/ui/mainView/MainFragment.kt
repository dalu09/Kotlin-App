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
import androidx.fragment.app.viewModels
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

    private val viewModel: MainViewModel by viewModels {
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
        setupObservers()
        setupClickListeners(view)
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
        val eventId = marker.tag as? String
        if (eventId != null) {
            val bundle = Bundle().apply { putString("event_id", eventId) }
            findNavController().navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle)
        }
        return false
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
        events.forEach { event ->
            event.location?.let { geoPoint ->
                val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                val markerOptions = MarkerOptions().position(latLng).title(event.name)
                customMarkerIcon?.let { markerOptions.icon(it) }
                val marker = gMap?.addMarker(markerOptions)
                marker?.tag = event.id
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

        // **LA SOLUCIÓN**: Solo cargar datos si el ViewModel no tiene ya un estado.
        // Esto previene que se vuelvan a cargar los datos en cada rotación de pantalla.
        if (viewModel.uiState.value == null) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                if (location != null) {
                    val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                    lastKnownLocation = userGeoPoint
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    gMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
                    // La carga inicial siempre será de los eventos cercanos
                    viewModel.loadNearbyEvents(userGeoPoint, 10000.0)
                } else {
                    Toast.makeText(requireContext(), "No se pudo obtener la ubicación.", Toast.LENGTH_LONG).show()
                    // Si no hay ubicación, cargamos todos los eventos como fallback
                    viewModel.loadAllEvents()
                }
            }
        } else {
            // Si ya hay datos, solo centramos la cámara si tenemos la ubicación
            lastKnownLocation?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                gMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))
            }
        }
    }
}