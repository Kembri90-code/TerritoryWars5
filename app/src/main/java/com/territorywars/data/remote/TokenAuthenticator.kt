package com.territorywars.data.remote

import com.territorywars.BuildConfig
import com.territorywars.data.local.TokenDataStore
import com.territorywars.data.remote.api.AuthApi
import com.territorywars.data.remote.dto.RefreshRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenDataStore: TokenDataStore,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenDataStore.refreshToken.firstOrNull() } ?: return null

        val newAccessToken = runBlocking {
            try {
                val authApi = Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(OkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthApi::class.java)

                val refreshResponse = authApi.refresh(RefreshRequest(refreshToken = refreshToken))
                if (refreshResponse.isSuccessful) {
                    val newToken = refreshResponse.body()!!.accessToken
                    tokenDataStore.updateAccessToken(newToken)
                    newToken
                } else {
                    // Refresh token is invalid — force re-login
                    tokenDataStore.clearTokens()
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        return newAccessToken?.let {
            response.request.newBuilder()
                .header("Authorization", "Bearer $it")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var r = response.priorResponse
        while (r != null) { count++; r = r.priorResponse }
        return count
    }
}
