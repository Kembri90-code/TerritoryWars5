package com.territorywars.presentation.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.territorywars.data.local.TokenDataStore
import com.territorywars.data.remote.SocketManager
import com.territorywars.data.remote.TerritoryEvent
import com.territorywars.data.remote.api.ClanApi
import com.territorywars.data.remote.api.TerritoryApi
import com.territorywars.data.remote.api.UserApi
import com.territorywars.data.remote.dto.CaptureRequest
import com.territorywars.data.remote.dto.toDto
import com.territorywars.domain.model.GeoPoint
import com.territorywars.domain.model.RoutePoint
import com.territorywars.domain.model.Territory
import com.territorywars.service.GpsTrackingService
import com.yandex.mapkit.Animation
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.geometry.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.*

data class MapState(
    val territories: List<Territory> = emptyList(),
    val myUserId: String? = null,
    val playerMarker: PlayerMarker = PlayerMarker.DOT,
    val myColor: String = "#2979FF",
    val isCapturing: Boolean = false,
    val routePoints: List<RoutePoint> = emptyList(),
    val routeDistanceM: Double = 0.0,
    val captureDurationSec: Long = 0L,
    val distanceToStartM: Double = Double.MAX_VALUE,
    val canFinishCapture: Boolean = false,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val notification: String? = null,
    val isLoading: Boolean = false,
    val lastBbox: String? = null,
    val clanRequestsBadge: Int = 0
)

private const val CLOSE_RADIUS_M = 15.0
private const val MIN_POINTS = 20
private const val BBOX_DEBOUNCE_MS = 500L
private const val GPS_INTERVAL_CAPTURING_MS = 2000L
private const val GPS_INTERVAL_IDLE_MS = 5000L

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val territoryApi: TerritoryApi,
    private val userApi: UserApi,
    private val clanApi: ClanApi,
    private val tokenDataStore: TokenDataStore,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var mapView: MapView? = null
    private var captureStartTime: Long = 0L
    private var timerJob: Job? = null
    private var bboxDebounceJob: Job? = null
    private var currentLocationRequest = buildLocationRequest(GPS_INTERVAL_IDLE_MS)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                onNewLocation(loc.latitude, loc.longitude, loc.accuracy)
            }
        }
    }

    init {
        loadCurrentUser()
        connectSocket()
    }

    private fun connectSocket() {
        socketManager.connect()
        viewModelScope.launch {
            socketManager.territoryEvents.collect { event ->
                when (event) {
                    is TerritoryEvent.Updated -> _state.update { s ->
                        val existing = s.territories.indexOfFirst { it.id == event.territory.id }
                        if (existing >= 0) {
                            s.copy(territories = s.territories.toMutableList().also { it[existing] = event.territory })
                        } else {
                            s.copy(territories = s.territories + event.territory)
                        }
                    }
                    is TerritoryEvent.Deleted -> _state.update { s ->
                        s.copy(territories = s.territories.filter { it.id != event.id })
                    }
                }
            }
        }
    }

    private fun buildLocationRequest(intervalMs: Long): LocationRequest {
        return LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        )
            .setMinUpdateIntervalMillis((intervalMs / 2).toLong())
            .setMaxUpdateDelayMillis(intervalMs * 2)
            .build()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val response = userApi.getMe()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _state.update { it.copy(myUserId = body.id, myColor = body.color) }
                    loadClanRequestsBadge(body.clanId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load current user")
            }
        }
        viewModelScope.launch {
            tokenDataStore.playerMarker.collect { id ->
                _state.update { it.copy(playerMarker = playerMarkerById(id)) }
            }
        }
    }

    private fun loadClanRequestsBadge(clanId: String?) {
        if (clanId == null) return
        viewModelScope.launch {
            try {
                val resp = clanApi.getClanRequests(clanId)
                if (resp.isSuccessful) {
                    _state.update { it.copy(clanRequestsBadge = resp.body()?.size ?: 0) }
                }
            } catch (_: Exception) {}
        }
    }

    fun onMapReady(mv: MapView) {
        mapView = mv
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                _state.update { it.copy(currentLat = location.latitude, currentLng = location.longitude) }
                centerOnLocation(location.latitude, location.longitude)
                viewModelScope.launch {
                    loadTerritoriesInBbox(calculateBbox(location.latitude, location.longitude))
                }
            } else {
                loadTerritoriesInView()
            }
        }
        fusedLocationClient.requestLocationUpdates(currentLocationRequest, locationCallback, null)
    }

    private fun onNewLocation(lat: Double, lng: Double, accuracy: Float) {
        _state.update { it.copy(currentLat = lat, currentLng = lng) }

        if (_state.value.routePoints.isEmpty() && !_state.value.isCapturing) {
            centerOnLocation(lat, lng)
        }

        onMapMoved(lat, lng)

        if (_state.value.isCapturing) {
            if (accuracy > 30f) return

            val point = RoutePoint(lat, lng, System.currentTimeMillis(), accuracy)
            val newPoints = _state.value.routePoints + point
            val newDistance = calcTotalDistance(newPoints)
            val distToStart = if (newPoints.isNotEmpty()) {
                haversineM(lat, lng, newPoints.first().lat, newPoints.first().lng)
            } else Double.MAX_VALUE

            val canFinish = newPoints.size >= MIN_POINTS

            _state.update {
                it.copy(
                    routePoints = newPoints,
                    routeDistanceM = newDistance,
                    distanceToStartM = distToStart,
                    canFinishCapture = canFinish
                )
            }
        }
    }

    private fun onMapMoved(lat: Double, lng: Double) {
        val bbox = calculateBbox(lat, lng)
        val lastBbox = _state.value.lastBbox

        if (lastBbox != bbox) {
            bboxDebounceJob?.cancel()
            bboxDebounceJob = viewModelScope.launch {
                delay(BBOX_DEBOUNCE_MS)
                loadTerritoriesInBbox(bbox)
            }
        }
    }

    private fun calculateBbox(centerLat: Double, centerLng: Double): String {
        val offset = 0.02
        val lat1 = centerLat - offset
        val lat2 = centerLat + offset
        val lng1 = centerLng - offset
        val lng2 = centerLng + offset
        return "$lat1,$lng1,$lat2,$lng2"
    }

    fun centerOnMyLocation() {
        val s = _state.value
        if (s.currentLat != null && s.currentLng != null) {
            centerOnLocation(s.currentLat, s.currentLng)
        }
    }

    private fun centerOnLocation(lat: Double, lng: Double) {
        mapView?.mapWindow?.map?.move(
            CameraPosition(Point(lat, lng), 16.0f, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    fun startCapture() {
        _state.update {
            it.copy(isCapturing = true, routePoints = emptyList(), routeDistanceM = 0.0, captureDurationSec = 0L)
        }
        captureStartTime = System.currentTimeMillis()
        startTimer()
        switchToCapturingMode()
        context.startService(Intent(context, GpsTrackingService::class.java).apply {
            action = GpsTrackingService.ACTION_START
        })
    }

    @SuppressLint("MissingPermission")
    private fun switchToCapturingMode() {
        currentLocationRequest = buildLocationRequest(GPS_INTERVAL_CAPTURING_MS)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(currentLocationRequest, locationCallback, null)
    }

    @SuppressLint("MissingPermission")
    private fun switchToIdleMode() {
        currentLocationRequest = buildLocationRequest(GPS_INTERVAL_IDLE_MS)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        fusedLocationClient.requestLocationUpdates(currentLocationRequest, locationCallback, null)
    }

    fun cancelCapture() {
        stopCapture()
        _state.update { it.copy(isCapturing = false, routePoints = emptyList(), canFinishCapture = false) }
    }

    fun finishCapture() {
        val points = _state.value.routePoints
        if (points.size < MIN_POINTS) {
            _state.update { it.copy(notification = "Недостаточно точек маршрута. Продолжайте идти.") }
            return
        }
        stopCapture()
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = territoryApi.captureTerritory(
                    CaptureRequest(routePoints = points.map { it.toDto() })
                )
                if (response.isSuccessful) {
                    val result = response.body()!!
                    if (result.success && result.territory != null) {
                        val newTerritory = result.territory.toDomain()
                        val notification = when {
                            result.conquered > 0 && result.merged -> "✓ Территория расширена и захвачена у противника!"
                            result.conquered > 1 -> "✓ Захвачено ${result.conquered} территорий противника!"
                            result.conquered == 1 -> "✓ Захвачена территория противника!"
                            result.merged -> "✓ Территория расширена!"
                            else -> "✓ Территория захвачена!"
                        }
                        _state.update { s ->
                            s.copy(
                                isCapturing = false,
                                routePoints = emptyList(),
                                canFinishCapture = false,
                                territories = s.territories + newTerritory,
                                isLoading = false,
                                notification = notification
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(isCapturing = false, routePoints = emptyList(), isLoading = false,
                                notification = result.error ?: "Ошибка захвата территории")
                        }
                    }
                } else {
                    _state.update { it.copy(isCapturing = false, routePoints = emptyList(), isLoading = false, notification = "Ошибка сервера") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCapturing = false, routePoints = emptyList(), isLoading = false, notification = "Нет подключения") }
            }
        }
    }

    private fun stopCapture() {
        timerJob?.cancel()
        switchToIdleMode()
        context.startService(Intent(context, GpsTrackingService::class.java).apply {
            action = GpsTrackingService.ACTION_STOP
        })
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - captureStartTime) / 1000
                _state.update { it.copy(captureDurationSec = elapsed) }
            }
        }
    }

    fun onMapTapped(lat: Double, lng: Double) {
        val territory = _state.value.territories.firstOrNull { t ->
            isPointInPolygon(GeoPoint(lat, lng), t.polygon)
        }
        if (territory != null && territory.ownerId != _state.value.myUserId) {
            val area = if (territory.areaM2 >= 1_000_000) {
                "${"%.2f".format(territory.areaM2 / 1_000_000)} км²"
            } else {
                "${territory.areaM2.toInt()} м²"
            }
            _state.update {
                it.copy(notification = "${territory.ownerUsername} · $area")
            }
        }
    }

    private fun loadTerritoriesInView() {
        viewModelScope.launch {
            try {
                val bbox = calculateBbox(55.7558, 37.6173)
                loadTerritoriesInBbox(bbox)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load territories")
            }
        }
    }

    private suspend fun loadTerritoriesInBbox(bbox: String) {
        try {
            val response = territoryApi.getTerritoriesInBbox(bbox)
            if (response.isSuccessful) {
                _state.update {
                    it.copy(
                        territories = response.body()!!.map { dto -> dto.toDomain() },
                        lastBbox = bbox
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load territories in bbox: $bbox")
        }
    }

    fun dismissNotification() {
        _state.update { it.copy(notification = null) }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        bboxDebounceJob?.cancel()
        socketManager.disconnect()
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun calcTotalDistance(points: List<RoutePoint>): Double {
        if (points.size < 2) return 0.0
        return points.zipWithNext().sumOf { (a, b) -> haversineM(a.lat, a.lng, b.lat, b.lng) }
    }

    private fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].lng; val yi = polygon[i].lat
            val xj = polygon[j].lng; val yj = polygon[j].lat
            if ((yi > point.lat) != (yj > point.lat) &&
                point.lng < (xj - xi) * (point.lat - yi) / (yj - yi) + xi) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
