package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class VpnResellersPlanItem(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "durationDays") val durationDays: Int? = null,
    @Json(name = "durationMinutes") val durationMinutes: Long? = null,
    @Json(name = "quotaBytes") val quotaBytes: Long? = null,
    @Json(name = "quotaGb") val quotaGb: Int? = null,
    @Json(name = "retailPriceNgn") val retailPriceNgn: Double,
    @Json(name = "wholesaleCostNgn") val wholesaleCostNgn: Double,
    @Json(name = "profitMarginPercent") val profitMarginPercent: Int,
    @Json(name = "maxDevices") val maxDevices: Int? = null,
    @Json(name = "description") val description: String,
    @Json(name = "badge") val badge: String? = null,
    @Json(name = "isPopular") val isPopular: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class VpnResellersPlansResponse(
    @Json(name = "status") val status: String,
    @Json(name = "currency") val currency: String,
    @Json(name = "modelA_timeSubscriptions") val modelAPlans: List<VpnResellersPlanItem>,
    @Json(name = "modelB_meteredPackages") val modelBPlans: List<VpnResellersPlanItem>
)

@JsonClass(generateAdapter = true)
data class VpnResellersServerItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "countryCode") val countryCode: String,
    @Json(name = "flagEmoji") val flagEmoji: String,
    @Json(name = "cityName") val cityName: String,
    @Json(name = "datacenter") val datacenter: String,
    @Json(name = "ipAddress") val ipAddress: String,
    @Json(name = "pingMs") val pingMs: Int,
    @Json(name = "serverLoadPercent") val serverLoadPercent: Int,
    @Json(name = "protocols") val protocols: List<String>,
    @Json(name = "recommendedFor") val recommendedFor: String? = null,
    @Json(name = "isProOnly") val isProOnly: Boolean = false,
    @Json(name = "status") val status: String = "Active"
)

@JsonClass(generateAdapter = true)
data class VpnResellersServersResponse(
    @Json(name = "status") val status: String,
    @Json(name = "servers") val servers: List<VpnResellersServerItem>,
    @Json(name = "primaryProtocol") val primaryProtocol: String,
    @Json(name = "roamingOptimized") val roamingOptimized: Boolean,
    @Json(name = "source") val source: String? = null,
    @Json(name = "apiUrl") val apiUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class VpnResellersStatusResponse(
    @Json(name = "status") val status: String,
    @Json(name = "configured") val configured: Boolean,
    @Json(name = "apiKeyMasked") val apiKeyMasked: String? = null,
    @Json(name = "apiUrl") val apiUrl: String,
    @Json(name = "docsUrl") val docsUrl: String,
    @Json(name = "version") val version: String,
    @Json(name = "projectId") val projectId: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateVpnSessionRequest(
    @Json(name = "planId") val planId: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "userEmail") val userEmail: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "protocol") val protocol: String = "WireGuard"
)

@JsonClass(generateAdapter = true)
data class VpnSessionDetails(
    @Json(name = "token") val token: String,
    @Json(name = "planTitle") val planTitle: String,
    @Json(name = "model") val model: String,
    @Json(name = "expirationDate") val expirationDate: Long? = null,
    @Json(name = "expiresInSeconds") val expiresInSeconds: Long? = null,
    @Json(name = "quotaBytes") val quotaBytes: Long? = null,
    @Json(name = "protocol") val protocol: String,
    @Json(name = "assignedClientIp") val assignedClientIp: String
)

@JsonClass(generateAdapter = true)
data class CreateVpnSessionResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "session") val session: VpnSessionDetails? = null
)

@JsonClass(generateAdapter = true)
data class VpnSessionStatusResponse(
    @Json(name = "status") val status: String,
    @Json(name = "token") val token: String,
    @Json(name = "planTitle") val planTitle: String,
    @Json(name = "model") val model: String,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "expirationDate") val expirationDate: Long? = null,
    @Json(name = "remainingSeconds") val remainingSeconds: Long? = null,
    @Json(name = "quotaBytes") val quotaBytes: Long? = null,
    @Json(name = "usedBytes") val usedBytes: Long? = null,
    @Json(name = "remainingBytes") val remainingBytes: Long? = null
)

@JsonClass(generateAdapter = true)
data class ReportUsageRequest(
    @Json(name = "bytesUploaded") val bytesUploaded: Long,
    @Json(name = "bytesDownloaded") val bytesDownloaded: Long
)

@JsonClass(generateAdapter = true)
data class ReportUsageResponse(
    @Json(name = "status") val status: String,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "usedBytes") val usedBytes: Long,
    @Json(name = "remainingBytes") val remainingBytes: Long? = null,
    @Json(name = "shouldDisconnect") val shouldDisconnect: Boolean
)

interface VpnResellersApiService {

    // --- Official VPNresellers API v4.1 Endpoints (https://api.vpnresellers.com/docs/v4_1/) ---
    @GET("v4_1/servers")
    suspend fun getV41DirectServers(): List<VpnResellersServerItem>

    // --- Backend Secure Gateway & Moniepoint/Pairgate Sync Endpoints ---
    @GET("api/v1/vpnresellers/plans")
    suspend fun getPlans(): VpnResellersPlansResponse

    @GET("api/v1/vpnresellers/servers")
    suspend fun getServers(): VpnResellersServersResponse

    @GET("api/v1/vpnresellers/status")
    suspend fun getGatewayStatus(): VpnResellersStatusResponse

    @POST("api/v1/vpnresellers/subscriptions/create")
    suspend fun createSubscription(@Body request: CreateVpnSessionRequest): CreateVpnSessionResponse

    @GET("api/v1/vpnresellers/session/{token}")
    suspend fun getSessionStatus(@Path("token") token: String): VpnSessionStatusResponse

    @POST("api/v1/vpnresellers/session/{token}/usage")
    suspend fun reportUsage(
        @Path("token") token: String,
        @Body request: ReportUsageRequest
    ): ReportUsageResponse

    companion object {
        const val OFFICIAL_V4_1_BASE_URL = "https://api.vpnresellers.com/"
        const val DEFAULT_BASE_URL = "https://api.flowtest2026.com/"

        fun create(
            baseUrl: String = DEFAULT_BASE_URL,
            apiKey: String? = null
        ): VpnResellersApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)

            // Resolve secret API token
            val resolvedToken = apiKey?.takeIf { it.isNotBlank() }
                ?: try {
                    val key = com.example.BuildConfig.VPNRESELLERS_API_KEY
                    if (key.isNotBlank() && !key.contains("placeholder")) key else null
                } catch (e: Throwable) {
                    null
                }

            if (!resolvedToken.isNullOrBlank()) {
                clientBuilder.addInterceptor { chain ->
                    val original = chain.request()
                    val authHeader = if (resolvedToken.startsWith("Bearer ", ignoreCase = true)) {
                        resolvedToken
                    } else {
                        "Bearer $resolvedToken"
                    }
                    val request = original.newBuilder()
                        .header("Authorization", authHeader)
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
            }

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(VpnResellersApiService::class.java)
        }
    }
}
