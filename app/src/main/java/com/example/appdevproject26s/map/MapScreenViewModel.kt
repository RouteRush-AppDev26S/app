package com.example.appdevproject26s.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.map.route.Location
import com.example.appdevproject26s.map.route.MatheFile
import com.example.appdevproject26s.map.route.Navigate
import com.example.appdevproject26s.map.route.Schrittzahler
import com.example.appdevproject26s.map.route.Speed
import com.example.appdevproject26s.map.route.Timer
import com.example.appdevproject26s.social.friends.FriendRepository
import com.example.appdevproject26s.social.friends.FriendshipResponse
import com.example.appdevproject26s.social.sharing.MapPin
import com.example.appdevproject26s.social.sharing.PinRepository
import com.example.appdevproject26s.social.sharing.PinResponse
import com.example.appdevproject26s.social.sharing.SharingRepository
import com.example.appdevproject26s.steps.StepsRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import java.util.Locale
import javax.inject.Inject

const val PIXELS_PER_TILE = 256

@HiltViewModel
class MapScreenViewModel @Inject constructor(
    private val repository: MapSettingsRepository,
    private val navigate: Navigate,
    private val locationClient: FusedLocationProviderClient,
    private val zahler: Schrittzahler,
    private val speedkmh : Speed,
    private val mathe : MatheFile,
    private val timertr: Timer,
    private val authRepository: AuthRepository,
    private val stepsRepository: StepsRepository,
    private val friendRepository: FriendRepository,
    private val pinRepository: PinRepository,
    private val sharingRepository: SharingRepository,
    private val vibrator: Vibrator
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val stepsReadPermission = stepsRepository.stepsPermission

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


    // --- Pin Sharing values ---

    private val _isSharingPin = MutableStateFlow(false)
    val isSharingPin: StateFlow<Boolean> = _isSharingPin.asStateFlow()

    private val _showShareDialog = MutableStateFlow(false)
    val showSharePinDialog: StateFlow<Boolean> = _showShareDialog.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendshipResponse>>(emptyList())
    val friends: StateFlow<List<FriendshipResponse>> = _friends.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pinNote = MutableStateFlow("")
    val pinNote: StateFlow<String> = _pinNote.asStateFlow()

    private val _friendsToSharePinWith = MutableStateFlow<List<String>>(emptyList())
    val friendsToSharePinWith: StateFlow<List<String>> = _friendsToSharePinWith.asStateFlow()



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

    private val _selectedLocation = MutableStateFlow<Position?>(null)
    val selectedLocation = _selectedLocation.asStateFlow()

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet = _showBottomSheet.asStateFlow()

    private val _planningPoints = MutableStateFlow<List<Position>>(emptyList())
    val planningPoints = _planningPoints.asStateFlow()

    private val _mapPins = MutableStateFlow<List<MapPin>>(emptyList())
    val mapPins = _mapPins.asStateFlow()

    private val _selectedMapPin = MutableStateFlow<MapPin?>(null)
    val selectedMapPin = _selectedMapPin.asStateFlow()

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
                navigate.updatePosition(
                    Location(
                        lat = loc.latitude,
                        lon = loc.longitude,
                        time = loc.time
                    )
                )
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

        viewModelScope.launch {
            authRepository.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) {
                    fetchFriends()
                    fetchPins()
                } else {
                    _friends.value = emptyList()
                    _mapPins.value = emptyList()
                }
            }
        }
    }

    fun fetchPins() {
        viewModelScope.launch {
            val myPinsResult = pinRepository.getMyPins()
            val sharedPinsResult = sharingRepository.getPinsSharedWithMe()

            val combinedPins = mutableListOf<MapPin>()

            myPinsResult.onSuccess { pins ->
                Log.e("NUMBEROwnPins", pins.size.toString())
                combinedPins.addAll(pins.map { it.toMapPin(isMine = true) })
            }

            sharedPinsResult.onSuccess { shared ->
                Log.e("NUMBERSHAREPINS", shared.size.toString())
                Log.e("SharedContainsPin", shared.getOrNull(0)?.pin?.id.toString())
                Log.d("SharedResults", shared.toString())
                combinedPins.addAll(shared.mapNotNull { it.pin?.toMapPin(isMine = false) })
            }

            _mapPins.value = combinedPins
            Log.e("NUMBERCombinedPins", combinedPins.size.toString())
        }
    }

    private fun PinResponse.toMapPin(isMine: Boolean): MapPin {
        return MapPin(
            id = this.id,
            position = Position(this.lng, this.lat),
            note = this.note,
            isMine = isMine
        )
    }


    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    fun onMapTap(point: Position, screenPoint: DpOffset, localDensity: Density) {
        // Check if a pin was tapped
        val tappedPin = findTappedPin(point, cameraState.position.zoom, localDensity.density)
        if (tappedPin != null) {
            _selectedMapPin.value = tappedPin
            return
        }

        if (_selectedMapPin.value != null) {
            _selectedMapPin.value = null
            return
        }

        _fullscreen.value = !_fullscreen.value
        if (_showBottomSheet.value){
            dismissBottomSheet()
        }
    }

    private fun findTappedPin(tapPos: Position, zoom: Double, density: Float): MapPin? {
        val tapLoc = Location(tapPos.latitude, tapPos.longitude)
        val thresholdDp = 24.0 // Threshold in DP
        
        // Calculate meters per pixel at the current latitude
        // Formula: 156543.03392 * cos(lat) / 2^zoom
        val latRad = Math.toRadians(tapPos.latitude)
        val metersPerPixel = 156543.03392 * Math.cos(latRad) / Math.pow(2.0, zoom)
        val thresholdMeters = thresholdDp * density * metersPerPixel

        return _mapPins.value.find { pin ->
            val pinLoc = Location(pin.position.latitude, pin.position.longitude)
            val dist = mathe.haversineDistance(tapLoc, pinLoc)
            dist < thresholdMeters
        }
    }

    fun dismissPinPopup() {
        _selectedMapPin.value = null
    }

    fun deleteSelectedPin() {
        val pin = _selectedMapPin.value ?: return
        if (!pin.isMine) return

        viewModelScope.launch {
            pinRepository.deletePin(pin.id).onSuccess {
                _mapPins.value = _mapPins.value.filter { it.id != pin.id }
                dismissPinPopup()
            }.onFailure {
                Log.e("MapScreenViewModel", "Failed to delete pin: ${it.message}")
            }
        }
    }

    private var lastClickTime: Long = 0
    fun onMapPress(point: Position, screenPoint: DpOffset, localDensity: Density) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) return
        lastClickTime = currentTime

        triggerVibration(500)

        val tappedLoc = Location(point.latitude, point.longitude)

        if (_isSelectingDestination.value) {
            navigate.routeReset()
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
            _selectedLocation.value = point
            _showBottomSheet.value = true
        }
    }

    fun dismissBottomSheet() {
        _showBottomSheet.value = false
        _selectedLocation.value = null
    }

    fun navigateToSelected() {
        val dest = _selectedLocation.value ?: return
        val start = _userLocation.value?.let { Location(it.latitude, it.longitude) } ?: return
        navigate.routeReset()
        openPlanningWithLocations(start, Location(dest.latitude, dest.longitude))
        dismissBottomSheet()
    }

    fun navigateFromSelected() {
        val start = _selectedLocation.value ?: return
        navigate.routeReset()
        viewModelScope.launch {
            _isPlanningMode.value = true
            _isSelectingDestination.value = false
            val startLoc = Location(start.latitude, start.longitude)
            originalStartLoc = startLoc
            originalDestLocs = mutableListOf(null)
            _startAddressInput.value = navigate.getAdresseOnce(startLoc)
            _destinations.value = listOf("")
            updatePlanningPoints()
            dismissBottomSheet()
        }
    }

    fun openSharePinDialog() {
        if (_friends.value.isEmpty()) {
            fetchFriends()
        }
        _showShareDialog.value = true
    }

    fun fetchFriends() {
        viewModelScope.launch {
            friendRepository.getFriends().fold(
                onSuccess = { friendList ->
                    _friends.value = friendList
                },
                onFailure = {
                    _friends.value = emptyList()
                }
            )
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


    fun triggerVibration(duration: Long = 500) {
        val currentVibrator = vibrator
        Log.d(TAG, "Triggering vibration: $duration ms")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                currentVibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updatePinNote(note: String) {
        _pinNote.value = note
    }

    fun dismissShareDialog() {
        _searchQuery.value = ""
        _friendsToSharePinWith.value = emptyList()
        _showShareDialog.value = false
    }

    fun toggleSharePinWithUser(username: String) {
        val currentList = _friendsToSharePinWith.value
        _friendsToSharePinWith.value = if (currentList.contains(username)) {
            currentList - username
        } else {
            currentList + username
        }
    }

    fun sharePinWithFriends() {
        val selectedUsers = _friendsToSharePinWith.value
        val targetLocation = _selectedLocation.value
        val existingPin = _selectedMapPin.value
        val note = _pinNote.value.ifBlank { "Shared Location" }

        if (selectedUsers.isEmpty() || targetLocation == null) return

        viewModelScope.launch {
            _isSharingPin.value = true

            val pinResult = pinRepository.createPin(
                lat = targetLocation.latitude,
                lng = targetLocation.longitude,
                note = "Shared Location"
            )

            pinResult.fold(
                onSuccess = { createdPin ->
                    val pinId = createdPin.id

                    selectedUsers.forEach { username ->
                        sharingRepository.sharePin(
                            pinId = pinId,
                            username = username
                        ).fold(
                            onSuccess = {
                                Log.d("MapScreenViewModel", "Successfully shared pin $pinId with $username")
                            },
                            onFailure = { error ->
                                Log.e("MapScreenViewModel", "Failed to share pin $pinId with $username: ${error.message}")
                            }
                        )
                    }
                    fetchPins()
                },
                onFailure = { error ->
                    Log.e("MapScreenViewModel", "Failed to create pin prior to sharing: ${error.message}")
                }
            )

            _isSharingPin.value = false
            dismissShareDialog()
            dismissBottomSheet()
            dismissPinPopup()
        }
    }
}
