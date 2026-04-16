package com.territorywars.presentation.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.territorywars.data.local.TokenDataStore
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
    val isCapturing: Boolean = false,
    val routePoints: List<RoutePoint> = emptyList(),
    val routeDistanceM: Double = 0.0,
    val captureDurationSec: Long = 0L,
    val distanceToStartM: Double = Double.MAX_VALUE,
    val canFinishCapture: Boolean = false,   // расстояние до старта < 15 м
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val notification: String? = null,
    val isLoading: Boolean = false
)

private const val CLOSE_RADIUS_M = 15.0
private const val MIN_POINTS = 20

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val territoryApi: TerritoryApi,
    private val userApi: UserApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var mapView: MapView? = null
    private var captureStartTime: Long = 0L
    private var timerJob: Job? = null
    private var locationJob: Job? = null

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000L
    ).setMinUpdateIntervalMillis(1000L).build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                onNewLocation(loc.latitude, loc.longitude, loc.accuracy)
            }
        }
    }

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val response = userApi.getMe()
                if (response.isSuccessful) {
                    _state.update { it.copy(myUserId = response.body()!!.id) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load current user")
            }
        }
    }

    fun onMapReady(mv: MapView) {
        mapView = mv
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        // Загрузить территории при старте
        loadTerritoriesInView()
    }

    private fun onNewLocation(lat: Double, lng: Double, accuracy: Float) {
        _state.update { it.copy(currentLat = lat, currentLng = lng) }

        // Первая точка → центрируем карту
        if (_state.value.routePoints.isEmpty() && !_state.value.isCapturing) {
            centerOnLocation(lat, lng)
        }

        if (_state.value.isCapturing) {
            if (accuracy > 30f) return // Отбрасываем неточные точки

            val point = RoutePoint(lat, lng, System.currentTimeMillis(), accuracy)
            val newPoints = _state.value.routePoints + point
            val newDistance = calcTotalDistance(newPoints)
            val distToStart = if (newPoints.isNotEmpty()) {
                haversineM(lat, lng, newPoints.first().lat, newPoints.first().lng)
            } else Double.MAX_VALUE

            val canFinish = distToStart <= CLOSE_RADIUS_M && newPoints.size >= MIN_POINTS

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
        _state.update { it.copy(isCapturing = true, routePoints = emptyList(), routeDistanceM = 0.0, captureDurationSec = 0L) }
        captureStartTime = System.currentTimeMillis()
        startTimer()
        // Запустить Foreground Service
        context.startService(Intent(context, GpsTrackingService::class.java).apply {
            action = GpsTrackingService.ACTION_START
        })
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
                        _state.update { s ->
                            s.copy(
                                isCapturing = false,
                                routePoints = emptyList(),
                                canFinishCapture = false,
                                territories = s.territories + newTerritory,
                                isLoading = false,
                                notification = if (result.merged) "Территория расширена!" else "Территория захвачена!"
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
        // Найти территорию в радиусе тапа (логика упрощена — можно расширить через PostGIS)
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
                // Загружаем в bbox текущей области — упрощённый bbox
                val response = territoryApi.getTerritoriesInBbox("55.5,37.3,55.9,37.9")
                if (response.isSuccessful) {
                    _state.update { it.copy(territories = response.body()!!.map { dto -> dto.toDomain() }) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load territories")
            }
        }
    }

    fun dismissNotification() {
        _state.update { it.copy(notification = null) }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // --- Гео-утилиты ---

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

    // Ray-casting алгоритм для проверки точки в полигоне
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
