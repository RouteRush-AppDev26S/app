package com.example.appdevproject26s.route

import android.Manifest
import android.annotation.SuppressLint
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import java.util.Locale
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.steps.StepsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
const val PIXELS_PER_TILE = 256

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: MapSettingsRepository,
    private val navigate: Navigate,
    private val locationClient: FusedLocationProviderClient,
    private val zahler: Schrittzahler,
    private val speedkmh : Speed,
    private val mathe : MatheFile,
    private val timertr: Timer,
    private val authRepository: AuthRepository,
    private val stepsRepository: StepsRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val stepsReadPermission = stepsRepository.readStepsPermission

    fun isHealthConnectAvailable() = stepsRepository.isHealthConnectAvailable()

    suspend fun hasStepsPermission() = stepsRepository.hasStepsPermission()

    fun syncSteps() {
        viewModelScope.launch {
            try {
                stepsRepository.syncTodaySteps()
            } catch (_: Exception) {
                // Fehler ignoriert
            }
        }
    }

    private val defaultPos = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()
    var cameraState = CameraState(firstPosition = defaultPos)

    // 🟢 KORREKT: Compose State für Double
    var durationSeconds by mutableDoubleStateOf(0.0)
        private set // UI darf nur lesen, ViewModel schreibt

    // 🟢 KORREKT: Compose State für Float
    var averageSpeedKmH by mutableFloatStateOf(0.0f)
        private set
    var timerThread: Thread? = null

    fun formatDuration(totalSeconds: Double): String {
        // Nutzt explizite Methoden statt der Symbole / und %
        val hours = totalSeconds.div(3600).toInt()
        val minutes = totalSeconds.rem(3600).div(60).toInt()
        val seconds = totalSeconds.rem(60).toInt()

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    val routePoints get() = navigate.routePoints
    val trackPoints get() = navigate.trackPoints
    val currentPosition get() = navigate.currentPosition
    val currentAddress get() = navigate.currentAddress
    val currentTrip get() = navigate.currentTrip
    val totalLengthKM get() = navigate.totalLengthKM
    val routeDurationSeconds get() = navigate.durationSeconds
    val speedLimit get() = navigate.speedLimit
    val manoevertext get() = navigate.manoevertext
    val startAddress get() = navigate.startAddress
    val destinationAddress get() = navigate.destinationAddress
    val isCalculating get() = navigate.isCalculating
    val errorMessage get() = navigate.errorMessage

    var stepsPerMinute by mutableIntStateOf(0)
    val schritte get() = zahler.schritte
    val speed get() = speedkmh.speedKmH ?: 0f
    val distance get() = navigate.distance
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
    val trackingstart get() = navigate.trackingstart
    val vehicleOptions = listOf(
        "Auto" to "driving-car",
        "Fahrrad" to "cycling-regular",
        "zu fuss" to "foot-walking"
    )

    private fun updatePlanningPoints() {
        val points = mutableListOf<Position>()
        originalStartLoc?.let { points.add(Position(it.longitude, it.latitude)) }
        originalDestLocs.forEach { loc ->
            loc?.let { points.add(Position(it.longitude, it.latitude)) }
        }
        _planningPoints.value = points
    }


    @SuppressLint("MissingPermission")
    fun toggletracking(){
        navigate.trackingstart = !navigate.trackingstart
        if(navigate.trackingstart){
            zahler.start()
            speedkmh.startSpeedTracking {
                calculateAverageSpeed()
                calculateStepsPerMinute()
            }
            timertr.start { tick ->
                durationSeconds = tick.toDouble()
                calculateAverageSpeed()
                calculateStepsPerMinute()
            }
            originalStartLoc?.let { navigate.updatePosition(it) }
        } else {
            timertr.stop()
            speedkmh.stopSpeedTracking()
            durationSeconds=0.0
            navigate.distance = 0.0
            averageSpeedKmH = 0.0f
            stepsPerMinute = 0
            zahler.stop()
        }

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
                navigate.updatePosition(Location(lat = loc.latitude, lon = loc.longitude, time = loc.time))
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
    @RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION])
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
        zahler.stop()
        speedkmh.stopSpeedTracking()
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
    private fun calculateAverageSpeed() {
        if (durationSeconds > 0) {
            // distance ist in km, durationSeconds in s
            // km / (s / 3600) = (km / s) * 3600
            averageSpeedKmH = ((distance / durationSeconds) * 3600.0).toFloat()
        } else {
            averageSpeedKmH = 0.0f
        }
    }
    private fun calculateStepsPerMinute() {
        if (durationSeconds > 0 && schritte > 0) {
            // Schritte geteilt durch Sekunden ergibt Schritte pro Sekunde.
            // Mal 60 ergibt Schritte pro Minute.
            val spm = (schritte.toDouble() / durationSeconds) * 60.0
            stepsPerMinute = spm.toInt()
        } else {
            stepsPerMinute = 0
        }
    }
}
