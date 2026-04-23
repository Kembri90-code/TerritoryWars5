package com.territorywars.presentation.map

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.territorywars.domain.model.Territory
import com.territorywars.presentation.theme.DarkPrimary
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.LinearRing
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polygon
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider

private class MapHolder {
    var territoriesCollection: MapObjectCollection? = null
    var routeCollection: MapObjectCollection? = null
    var userLocationView: UserLocationView? = null
    var lastMarker: PlayerMarker? = null
    var lastColor: String? = null
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

                mv.mapWindow.map.isNightModeEnabled = true

                val userLayer = MapKitFactory.getInstance().createUserLocationLayer(mv.mapWindow)
                userLayer.isVisible = true
                userLayer.setObjectListener(object : UserLocationObjectListener {
                    override fun onObjectAdded(view: UserLocationView) {
                        holder.userLocationView = view
                        applyUserMarker(view, holder.lastMarker ?: PlayerMarker.DOT, holder.lastColor ?: "#2979FF")
                    }
                    override fun onObjectRemoved(view: UserLocationView) {
                        holder.userLocationView = null
                    }
                    override fun onObjectUpdated(view: UserLocationView, event: ObjectEvent) {}
                })

                holder.territoriesCollection = mv.mapWindow.map.mapObjects.addCollection()
                holder.routeCollection = mv.mapWindow.map.mapObjects.addCollection()

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
            val newMarker = state.playerMarker
            val newColor = state.myColor
            if (newMarker != holder.lastMarker || newColor != holder.lastColor) {
                holder.lastMarker = newMarker
                holder.lastColor = newColor
                holder.userLocationView?.let { applyUserMarker(it, newMarker, newColor) }
            }

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

private fun applyUserMarker(view: UserLocationView, marker: PlayerMarker, colorHex: String) {
    val bitmap = createMarkerBitmap(marker, colorHex)
    val provider = ImageProvider.fromBitmap(bitmap)
    view.pin.setIcon(provider)
    view.arrow.setIcon(provider)
    val fillColor = try {
        val base = android.graphics.Color.parseColor(colorHex)
        (base and 0x00FFFFFF) or (0x33 shl 24)
    } catch (_: Exception) { 0x332979FF }
    view.accuracyCircle.fillColor = fillColor
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
            polyline.setStrokeColor(DarkPrimary.toArgb())
            polyline.strokeWidth = 5f
        }
    }
}
