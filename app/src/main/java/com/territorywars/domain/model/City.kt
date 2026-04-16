package com.territorywars.domain.model

data class City(
    val id: Int,
    val name: String,
    val region: String,
    val population: Int,
    val lat: Double,
    val lng: Double
)
