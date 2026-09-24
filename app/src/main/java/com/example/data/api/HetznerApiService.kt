package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface HetznerApiService {

    @GET("v1/datacenters")
    suspend fun getDatacenters(
        @Header("Authorization") token: String
    ): Response<HetznerDatacentersResponse>

    @GET("v1/servers")
    suspend fun getServers(
        @Header("Authorization") token: String
    ): Response<HetznerServersResponse>

    @POST("v1/servers")
    suspend fun createServer(
        @Header("Authorization") token: String,
        @Body request: CreateHetznerServerRequest
    ): Response<CreateHetznerServerResponse>

    @DELETE("v1/servers/{id}")
    suspend fun deleteServer(
        @Header("Authorization") token: String,
        @Path("id") serverId: Long
    ): Response<Unit>

    @POST("v1/servers/{id}/actions/reboot")
    suspend fun rebootServer(
        @Header("Authorization") token: String,
        @Path("id") serverId: Long
    ): Response<Unit>

    companion object {
        private const val BASE_URL = "https://api.hetzner.cloud/"

        fun create(): HetznerApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(HetznerApiService::class.java)
        }
    }
}
