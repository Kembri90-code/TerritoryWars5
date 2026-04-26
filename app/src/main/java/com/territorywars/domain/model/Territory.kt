package com.territorywars.domain.model

data class Territory(
    val id: String,
    val ownerId: String,
    val ownerUsername: String,
    val ownerColor: String,      // #RRGGBB
    val clanId: String?,
    val clanColor: String?,
    val clanTag: String?,
    val polygon: List<GeoPoint>, // список вершин полигона
    val areaM2: Double,
    val perimeterM: Double,
    val capturedAt: String,
    val updatedAt: String
)

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class RoutePoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val accuracy: Float
)
