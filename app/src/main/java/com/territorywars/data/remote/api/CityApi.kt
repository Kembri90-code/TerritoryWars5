package com.territorywars.data.remote.api

import com.territorywars.data.remote.dto.CityDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CityApi {

    @GET("cities")
    suspend fun getCities(
        @Query("search") search: String? = null
    ): Response<List<CityDto>>
}
