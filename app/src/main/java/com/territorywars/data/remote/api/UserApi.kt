package com.territorywars.data.remote.api

import com.territorywars.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApi {

    @GET("users/me")
    suspend fun getMe(): Response<UserDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<UserDto>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserDto>
}
