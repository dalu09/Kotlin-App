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
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.kotlinapp.R
import com.example.kotlinapp.data.models.Event
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
import com.google.firebase.firestore.GeoPoint

class MainFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val TAG = "MainFragment"
    }

    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: MainViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableMyLocation(true) // Cargar eventos cercanos después de obtener el permiso
        } else {
            Toast.makeText(requireContext(), "El permiso de ubicación es necesario para mostrar eventos cercanos", Toast.LENGTH_LONG).show()
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
        Log.d(TAG, "onViewCreated: Inicializando... ")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupObservers()
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        val allEventsButton = view.findViewById<Button>(R.id.allEventsButton)
        allEventsButton.setOnClickListener {
            viewModel.loadAllEvents()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: El mapa está listo.")
        gMap = googleMap
        gMap?.setOnMarkerClickListener(this)
        // Cargar eventos cercanos por defecto al iniciar
        enableMyLocation(true)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val eventId = marker.tag as? String
        if (eventId != null) {
            Log.i(TAG, "Marcador clickeado! Navegando al detalle del evento con ID: $eventId")
            
            val bundle = Bundle().apply {
                putString("event_id", eventId)
            }
            findNavController().navigate(R.id.action_mainFragment_to_eventDetailFragment, bundle)

        } else {
            Log.w(TAG, "Marcador clickeado, pero no tiene un ID de evento en su tag.")
        }
        return false
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Configurando observadores del ViewModel.")
        viewModel.events.observe(viewLifecycleOwner) { events ->
            Log.d(TAG, "Observador de EVENTOS activado. Número de eventos recibidos: ${events.size}")
            addEventsToMap(events)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Log.e(TAG, "Observador de ERRORES activado: $error")
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }
    }

    private fun addEventsToMap(events: List<Event>) {
        if (gMap == null) {
            Log.w(TAG, "addEventsToMap llamado pero el mapa (gMap) es nulo.")
            return
        }
        Log.d(TAG, "Añadiendo ${events.size} eventos al mapa.")
        gMap?.clear()

        val customMarkerIcon = bitmapDescriptorFromVector(R.drawable.icon_sl, 130, 130)

        events.forEach { event ->
            event.location?.let { geoPoint ->
                val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title(event.name)

                customMarkerIcon?.let { markerOptions.icon(it) }
                
                val marker = gMap?.addMarker(markerOptions)
                marker?.tag = event.id

            } ?: run {
                Log.w(TAG, "El evento '${event.name}' (ID: ${event.id}) fue recibido pero NO tiene ubicación (location es null).")
            }
        }
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int, width: Int, height: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(requireContext(), vectorResId)?.let {
            it.setBounds(0, 0, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            it.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun enableMyLocation(loadEvents: Boolean) {
        if (gMap == null) return
        Log.d(TAG, "enableMyLocation: Verificando permisos de ubicación.")
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                gMap?.isMyLocationEnabled = true
                
                if (loadEvents) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                        if (location != null) {
                            Log.i(TAG, "¡Ubicación OBTENIDA!: Lat=${location.latitude}, Lon=${location.longitude}")
                            val userLatLng = LatLng(location.latitude, location.longitude)
                            gMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f))

                            val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                            Log.d(TAG, "Pidiendo al ViewModel que cargue eventos cercanos...")
                            viewModel.loadNearbyEvents(userGeoPoint, 10000.0) // 10km radius

                        } else {
                            Log.e(TAG, "¡ERROR CRÍTICO! La ubicación obtenida es NULL.")
                            Toast.makeText(requireContext(), "No se pudo obtener la ubicación. Asegúrate de tenerla activada.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}
