package com.territorywars.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.territorywars.domain.model.City

data class CityDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("region") val region: String,
    @SerializedName("population") val population: Int,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
) {
    fun toDomain() = City(
        id = id, name = name, region = region,
        population = population, lat = lat, lng = lng
    )
}
