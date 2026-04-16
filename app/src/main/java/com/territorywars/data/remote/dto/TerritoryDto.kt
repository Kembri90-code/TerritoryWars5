package com.territorywars.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.territorywars.domain.model.GeoPoint
import com.territorywars.domain.model.RoutePoint
import com.territorywars.domain.model.Territory

data class TerritoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("owner_username") val ownerUsername: String,
    @SerializedName("owner_color") val ownerColor: String,
    @SerializedName("clan_id") val clanId: String?,
    @SerializedName("clan_color") val clanColor: String?,
    @SerializedName("polygon") val polygon: List<List<Double>>, // [[lng, lat], ...]
    @SerializedName("area_m2") val areaM2: Double,
    @SerializedName("perimeter_m") val perimeterM: Double,
    @SerializedName("captured_at") val capturedAt: String,
    @SerializedName("updated_at") val updatedAt: String
) {
    fun toDomain() = Territory(
        id = id,
        ownerId = ownerId,
        ownerUsername = ownerUsername,
        ownerColor = ownerColor,
        clanId = clanId,
        clanColor = clanColor,
        polygon = polygon.map { GeoPoint(lat = it[1], lng = it[0]) },
        areaM2 = areaM2,
        perimeterM = perimeterM,
        capturedAt = capturedAt,
        updatedAt = updatedAt
    )
}

data class CaptureRequest(
    @SerializedName("route_points") val routePoints: List<RoutePointDto>
)

data class RoutePointDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("accuracy") val accuracy: Float
)

fun RoutePoint.toDto() = RoutePointDto(
    lat = lat, lng = lng, timestamp = timestamp, accuracy = accuracy
)

data class CaptureResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("territory") val territory: TerritoryDto?,
    @SerializedName("merged") val merged: Boolean,
    @SerializedName("error") val error: String?
)
