package com.territorywars.data.remote

import com.territorywars.BuildConfig
import com.territorywars.data.local.TokenDataStore
import com.territorywars.domain.model.GeoPoint
import com.territorywars.domain.model.Territory
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class TerritoryEvent {
    data class Updated(val territory: Territory) : TerritoryEvent()
    data class Deleted(val id: String) : TerritoryEvent()
}

@Singleton
class SocketManager @Inject constructor(
    private val tokenDataStore: TokenDataStore
) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _territoryEvents = MutableSharedFlow<TerritoryEvent>(extraBufferCapacity = 64)
    val territoryEvents: SharedFlow<TerritoryEvent> = _territoryEvents

    fun connect() {
        if (socket?.connected() == true) return
        scope.launch {
            try {
                val token = tokenDataStore.accessToken.first() ?: return@launch
                val opts = IO.Options()
                opts.auth = mapOf("token" to token)
                // Socket.IO uses HTTP URL — it handles WebSocket upgrade internally
                val url = BuildConfig.BASE_URL.removeSuffix("api/").trimEnd('/')
                socket = IO.socket(url, opts).apply {
                    on(Socket.EVENT_CONNECT) {
                        Timber.d("[Socket] Connected")
                    }
                    on(Socket.EVENT_DISCONNECT) {
                        Timber.d("[Socket] Disconnected")
                    }
                    on(Socket.EVENT_CONNECT_ERROR) { args ->
                        Timber.e("[Socket] Connect error: ${args.firstOrNull()}")
                    }
                    on("territory_updated") { args ->
                        try {
                            val json = args.firstOrNull() as? JSONObject ?: return@on
                            val territory = parseTerritoryJson(json)
                            scope.launch { _territoryEvents.emit(TerritoryEvent.Updated(territory)) }
                        } catch (e: Exception) {
                            Timber.e(e, "[Socket] territory_updated parse error")
                        }
                    }
                    on("territory_deleted") { args ->
                        try {
                            val json = args.firstOrNull() as? JSONObject ?: return@on
                            val id = json.getString("id")
                            scope.launch { _territoryEvents.emit(TerritoryEvent.Deleted(id)) }
                        } catch (e: Exception) {
                            Timber.e(e, "[Socket] territory_deleted parse error")
                        }
                    }
                    connect()
                }
            } catch (e: Exception) {
                Timber.e(e, "[Socket] Connection error")
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    private fun parseTerritoryJson(json: JSONObject): Territory {
        val polygonArray = json.getJSONArray("polygon")
        val polygon = (0 until polygonArray.length()).map { i ->
            val pair = polygonArray.getJSONArray(i)
            GeoPoint(lat = pair.getDouble(1), lng = pair.getDouble(0))
        }
        return Territory(
            id = json.getString("id"),
            ownerId = json.getString("owner_id"),
            ownerUsername = json.getString("owner_username"),
            ownerColor = json.getString("owner_color"),
            clanId = if (json.isNull("clan_id")) null else json.getString("clan_id"),
            clanColor = if (json.isNull("clan_color")) null else json.getString("clan_color"),
            clanTag = if (json.isNull("clan_tag")) null else json.optString("clan_tag").ifEmpty { null },
            polygon = polygon,
            areaM2 = json.getDouble("area_m2"),
            perimeterM = json.getDouble("perimeter_m"),
            capturedAt = json.getString("captured_at"),
            updatedAt = json.getString("updated_at")
        )
    }
}
