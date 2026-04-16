package com.territorywars.presentation.map

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.Territory
import com.territorywars.presentation.theme.Primary
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView

// Держит ссылки на sub-collections, чтобы между factory и update не использовать traverse
private class MapHolder {
    var territoriesCollection: MapObjectCollection? = null
    var routeCollection: MapObjectCollection? = null
}

@Composable
fun YandexMapView(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val holder = remember { MapHolder() }

    AndroidView(
        factory = { context ->
            MapView(context).also { mv ->
                MapKitFactory.getInstance().onStart()
                mv.onStart()

                // Тёмная тема карты
                mv.mapWindow.map.isNightModeEnabled = true

                // Слой местоположения пользователя
                val userLayer = MapKitFactory.getInstance().createUserLocationLayer(mv.mapWindow)
                userLayer.isVisible = true

                // Отдельные коллекции для территорий и маршрута (очищаем через .clear())
                holder.territoriesCollection = mv.mapWindow.map.mapObjects.addCollection()
                holder.routeCollection = mv.mapWindow.map.mapObjects.addCollection()

                // Обработка тапов (MapKit 4.x: InputListener)
                mv.mapWindow.map.addInputListener(object : InputListener {
                    override fun onMapTap(map: Map, point: Point) {
                        viewModel.onMapTapped(point.latitude, point.longitude)
                    }
                    override fun onMapLongTap(map: Map, point: Point) {}
                })

                viewModel.onMapReady(mv)
            }
        },
        modifier = modifier,
        update = {
            holder.territoriesCollection?.let { col ->
                updateTerritoryPolygons(col, state.territories, state.myUserId)
            }
            holder.routeCollection?.let { col ->
                updateCaptureRoute(col, state)
            }
        },
        onRelease = { mv ->
            mv.onStop()
            MapKitFactory.getInstance().onStop()
        }
    )
}

private fun updateTerritoryPolygons(
    collection: MapObjectCollection,
    territories: List<Territory>,
    myUserId: String?
) {
    collection.clear()

    territories.forEach { territory ->
        val points = territory.polygon.map { Point(it.lat, it.lng) }
        if (points.size < 3) return@forEach

        val polygon = Polygon(LinearRing(points), emptyList())
        val isOwn = territory.ownerId == myUserId
        val hexColor = if (isOwn) territory.ownerColor
                       else territory.clanColor ?: territory.ownerColor

        val fillColor = try {
            val base = android.graphics.Color.parseColor(hexColor)
            val alpha = if (isOwn) 0x80 else 0x55
            (base and 0x00FFFFFF) or (alpha shl 24)
        } catch (_: Exception) {
            0x5500C853.toInt()
        }

        val strokeColor = try {
            val base = android.graphics.Color.parseColor(hexColor)
            val alpha = if (isOwn) 0xFF else 0xBB
            (base and 0x00FFFFFF) or (alpha shl 24)
        } catch (_: Exception) {
            0xFF00C853.toInt()
        }

        collection.addPolygon(polygon).also { obj ->
            obj.fillColor = fillColor
            obj.strokeColor = strokeColor
            obj.strokeWidth = if (isOwn) 3f else 2f
            obj.zIndex = if (isOwn) 1f else 0f
        }
    }
}

private fun updateCaptureRoute(
    collection: MapObjectCollection,
    state: MapState
) {
    collection.clear()

    if (state.isCapturing && state.routePoints.size >= 2) {
        val points = state.routePoints.map { Point(it.lat, it.lng) }
        collection.addPolyline(Polyline(points)).also { polyline ->
            polyline.setStrokeColor(Primary.toArgb())
            polyline.strokeWidth = 5f
        }
    }
}
