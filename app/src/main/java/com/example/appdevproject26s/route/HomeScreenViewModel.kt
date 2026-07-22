package com.example.appdevproject26s.route

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import javax.inject.Inject

const val PIXELS_PER_TILE = 256

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: MapSettingsRepository,
    private val navigate: Navigate,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {
    private val defaultPos = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()
    var cameraState = CameraState(firstPosition = defaultPos)



    val routePoints get() = navigate.routePoints
    val currentTrip get() = navigate.currentTrip
    val totalLengthKM get() = navigate.totalLengthKM
    val durationSeconds get() = navigate.durationSeconds
    val speedLimit get() = navigate.speedLimit
    val manoevertext get() = navigate.manoevertext
    val startAddress get() = navigate.startAddress
    val destinationAddress get() = navigate.destinationAddress
    val isCalculating get() = navigate.isCalculating
    val errorMessage get() = navigate.errorMessage

    private val _isPlanningMode = MutableStateFlow(false)
    val isPlanningMode = _isPlanningMode.asStateFlow()

    private val _isSelectingDestination = MutableStateFlow(false)
    val isSelectingDestination = _isSelectingDestination.asStateFlow()

    private val _isRoundTrip = MutableStateFlow(false)
    val isRoundTrip = _isRoundTrip.asStateFlow()

    private val _startAddressInput = MutableStateFlow("")
    val startAddressInput = _startAddressInput.asStateFlow()

    private val _destinations = MutableStateFlow(listOf(""))
    val destinations = _destinations.asStateFlow()

    private val _selectedVehicle = MutableStateFlow("driving-car")
    val selectedVehicle = _selectedVehicle.asStateFlow()

    private val _planningPoints = MutableStateFlow<List<Position>>(emptyList())
    val planningPoints = _planningPoints.asStateFlow()

    private var originalStartLoc: Location? = null
    private var originalDestLocs = mutableListOf<Location?>()

    val vehicleOptions = listOf(
        "Auto" to "driving-car",
        "Fahrrad" to "cycling-regular",
        "zu fuss" to "foot-walking"
    )

    private fun updatePlanningPoints() {
        val points = mutableListOf<Position>()
        originalStartLoc?.let { points.add(Position(it.lon, it.lat)) }
        originalDestLocs.forEach { loc ->
            loc?.let { points.add(Position(it.lon, it.lat)) }
        }
        _planningPoints.value = points
    }

    fun togglePlanningMode() {
        _isPlanningMode.value = !_isPlanningMode.value
        if (_isPlanningMode.value) {
            _startAddressInput.value = navigate.currentAddress
            originalStartLoc = navigate.currentPosition
            _destinations.value = listOf("")
            originalDestLocs = mutableListOf(null)
            _isSelectingDestination.value = false
            updatePlanningPoints()
        } else {
            _planningPoints.value = emptyList()
        }
    }

    fun openPlanningWithLocations(start: Location, stop: Location) {
        viewModelScope.launch {
            _isPlanningMode.value = true
            _isSelectingDestination.value = false
            originalStartLoc = start
            originalDestLocs = mutableListOf(stop)
            _startAddressInput.value = navigate.getAdresseOnce(start)
            _destinations.value = listOf(navigate.getAdresseOnce(stop))
            updatePlanningPoints()
        }
    }

    fun setRoundTrip(value: Boolean) {
        _isRoundTrip.value = value
    }

    fun setSelectedVehicle(vehicle: String) {
        _selectedVehicle.value = vehicle
    }

    fun setAvoidHighways(value: Boolean) {
        navigate.noHighway = value
    }

    fun setAvoidTolls(value: Boolean) {
        navigate.noMaut = value
    }

    fun updateStartAddress(value: String) {
        if (_startAddressInput.value != value) {
            _startAddressInput.value = value
            originalStartLoc = null
            updatePlanningPoints()
        }
    }

    fun updateDestinationAddress(index: Int, value: String) {
        val list = _destinations.value.toMutableList()
        if (index < list.size) {
            if (list[index] != value) {
                list[index] = value
                _destinations.value = list
                if (index < originalDestLocs.size) {
                    originalDestLocs[index] = null
                    updatePlanningPoints()
                }
            }
        }
    }

    fun startSelectingDestination() {
        _isSelectingDestination.value = true
        _isPlanningMode.value = false
    }

    private suspend fun awaitCalculation() {
        // Wir geben dem async-Aufruf kurz Zeit zu starten (isCalculating wird true)
        delay(200)
        // Wir warten solange isCalculating true ist
        while (navigate.isCalculating) {
            delay(100)
        }
    }

    fun calculateRouteFromPlanning() {
        viewModelScope.launch {
            val startLoc = originalStartLoc ?: navigate.getCoordinatesFromAddress(_startAddressInput.value)
            if (startLoc == null) return@launch

            val destTexts = _destinations.value
            val destLocs = originalDestLocs

            // First leg: Start to first destination
            val firstLegDest = destLocs.getOrNull(0) ?: navigate.getCoordinatesFromAddress(destTexts.getOrNull(0) ?: "")
            if (firstLegDest != null) {
                navigate.calcRoute(startLoc, firstLegDest, _selectedVehicle.value)
                awaitCalculation()

                // Additional legs (Zwischenziele)
                for (i in 0 until destLocs.size - 1) {
                    val from = destLocs[i] ?: navigate.getCoordinatesFromAddress(destTexts.getOrNull(i) ?: "")
                    val to = destLocs[i+1] ?: navigate.getCoordinatesFromAddress(destTexts.getOrNull(i+1) ?: "")

                    if (from != null && to != null) {
                        navigate.calcRoute(from, to, _selectedVehicle.value)
                        awaitCalculation()
                    }
                }

                // Final leg: back to start if it's a round trip
                if (_isRoundTrip.value) {
                    val lastLoc = destLocs.lastOrNull() ?: navigate.getCoordinatesFromAddress(destTexts.lastOrNull() ?: "")
                    if (lastLoc != null) {
                        navigate.calcRoute(lastLoc, startLoc, _selectedVehicle.value)
                        awaitCalculation()
                    }
                }

                _isPlanningMode.value = false
                _planningPoints.value = emptyList()
            }
        }
    }

    private val _userLocation = MutableStateFlow<Position?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _userLocation.value = Position(loc.longitude, loc.latitude)
                navigate.updatePosition(Location(loc.latitude, loc.longitude))
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.savedPosition.firstOrNull()?.let { restoredPos ->
                cameraState.position = restoredPos
            }

            snapshotFlow { cameraState.position }
                .collectLatest { position ->
                    delay(1000)
                    repository.saveCameraPosition(position)
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    fun onMapTap(point: Position, screePoint: DpOffset) {
        _fullscreen.value = !_fullscreen.value
    }

    private var lastClickTime: Long = 0
    fun onMapPress(point: Position, screenPoint: DpOffset, localDensity: Density) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) return
        lastClickTime = currentTime

        navigate.triggerVibration(500)

        val tappedLoc = Location(point.latitude, point.longitude)
        navigate.routeReset()
        if (_isSelectingDestination.value) {
            viewModelScope.launch {
                val address = navigate.getAdresseOnce(tappedLoc)

                val currentDests = _destinations.value.toMutableList()
                // If the last one is empty, replace it, otherwise add new
                if (currentDests.isNotEmpty() && currentDests.last().isEmpty()) {
                    currentDests[currentDests.lastIndex] = address
                    if (originalDestLocs.size > currentDests.lastIndex) {
                        originalDestLocs[currentDests.lastIndex] = tappedLoc
                    } else {
                        originalDestLocs.add(tappedLoc)
                    }
                } else {
                    currentDests.add(address)
                    originalDestLocs.add(tappedLoc)
                }

                _destinations.value = currentDests
                _isSelectingDestination.value = false
                _isPlanningMode.value = true
                updatePlanningPoints()
            }
        } else {
            val startLocation = _userLocation.value?.let { Location(it.latitude, it.longitude) } ?: return
            openPlanningWithLocations(startLocation, tappedLoc)
        }
    }
}
