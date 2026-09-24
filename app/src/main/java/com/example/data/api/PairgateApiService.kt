package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface PairgateApiService {

    @GET("user/balance")
    suspend fun getBalance(
        @Header("Authorization") bearerToken: String,
        @Header("api-key") apiKey: String = bearerToken.replace("Bearer ", "")
    ): Response<PairgateBalanceResponse>

    @GET("balance")
    suspend fun getDirectBalance(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateBalanceResponse>

    @GET("wallet/balance")
    suspend fun getWalletBalance(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateBalanceResponse>

    @GET("user/profile")
    suspend fun getAdminProfile(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateAdminProfileResponse>

    @GET("services/list")
    suspend fun getServices(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateApiResponse>

    @GET("data-plans")
    suspend fun getDataPlans(
        @Header("Authorization") bearerToken: String,
        @Query("provider_id") providerId: String? = null,
        @Query("plan_type") planType: String? = null,
        @Query("network_id") networkId: Int? = null,
        @Query("plan_category") planCategory: String? = null
    ): Response<PairgateDataPlansResponse>

    @GET("data/plans")
    suspend fun getDataPlansAlt(
        @Header("Authorization") bearerToken: String,
        @Query("provider_id") providerId: String? = null,
        @Query("plan_type") planType: String? = null,
        @Query("network_id") networkId: Int? = null
    ): Response<PairgateDataPlansResponse>

    @GET("plans")
    suspend fun getPlansGeneric(
        @Header("Authorization") bearerToken: String,
        @Query("type") type: String? = "data",
        @Query("network") network: String? = null
    ): Response<PairgateDataPlansResponse>

    @GET("data-plans/categories")
    suspend fun getDataPlanCategories(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateApiResponse>

    @GET("services")
    suspend fun getServicesGeneral(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateDataPlansResponse>

    @GET("providers/{type}")
    suspend fun getProvidersByType(
        @Header("Authorization") bearerToken: String,
        @Path("type") type: String
    ): Response<PairgateApiResponse>

    @POST("https://pairgate.com")
    suspend fun createClientVirtualAccountRoot(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateVirtualAccountRequest
    ): Response<PairgateVirtualAccountResponse>

    @POST("https://pairgate.com/api/virtual-account")
    suspend fun createClientVirtualAccountDirect(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateVirtualAccountRequest
    ): Response<PairgateVirtualAccountResponse>

    @POST("virtual-account/create")
    suspend fun createClientVirtualAccount(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateVirtualAccountRequest
    ): Response<PairgateVirtualAccountResponse>

    @GET("admin/virtual-accounts")
    suspend fun getClientVirtualAccounts(
        @Header("Authorization") bearerToken: String
    ): Response<PairgateAdminAccountsListResponse>

    @POST("airtime/purchase")
    suspend fun purchaseAirtime(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateAirtimeRequest
    ): Response<PairgateApiResponse>

    @POST("data/purchase")
    suspend fun purchaseData(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateDataRequest
    ): Response<PairgateApiResponse>

    @POST("electricity/purchase")
    suspend fun purchaseElectricity(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateElectricityPurchaseRequest
    ): Response<PairgateApiResponse>

    @POST("cable/purchase")
    suspend fun purchaseCable(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateCablePurchaseRequest
    ): Response<PairgateApiResponse>

    @POST("bill/pay")
    suspend fun payBill(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateBillRequest
    ): Response<PairgateApiResponse>

    @POST("electricity/verify")
    suspend fun verifyElectricityMeter(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateElectricityVerifyRequest
    ): Response<PairgateElectricityVerifyResponse>

    @POST("cable/verify")
    suspend fun verifyCableSmartcard(
        @Header("Authorization") bearerToken: String,
        @Body body: PairgateCableVerifyRequest
    ): Response<PairgateElectricityVerifyResponse>

    @GET("transaction/query/{reference}")
    suspend fun queryTransaction(
        @Header("Authorization") bearerToken: String,
        @Path("reference") reference: String
    ): Response<PairgateApiResponse>

    @GET("transaction/status")
    suspend fun queryTransactionStatus(
        @Header("Authorization") bearerToken: String,
        @Query("reference_code") referenceCode: String
    ): Response<PairgateApiResponse>

    @GET("query")
    suspend fun queryTransactionQuery(
        @Header("Authorization") bearerToken: String,
        @Query("reference") reference: String
    ): Response<PairgateApiResponse>

    companion object {
        private const val BASE_URL = "https://pairgate.com/api/v1/"

        fun create(): PairgateApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val reqBuilder = original.newBuilder()
                        .header("Accept", "application/json")
                        .header("Connection", "keep-alive")
                        .header("Content-Type", "application/json")

                    val existingAuth = original.header("Authorization")
                    val existingApiKey = original.header("api-key")
                    val existingXApiKey = original.header("X-API-KEY")

                    // Extract raw API token without "Bearer "
                    var rawToken = when {
                        !existingAuth.isNullOrBlank() -> existingAuth.replace(Regex("(?i)^bearer\\s+"), "").trim()
                        !existingApiKey.isNullOrBlank() -> existingApiKey.replace(Regex("(?i)^bearer\\s+"), "").trim()
                        !existingXApiKey.isNullOrBlank() -> existingXApiKey.replace(Regex("(?i)^bearer\\s+"), "").trim()
                        else -> ""
                    }

                    // Fallback to BuildConfig if token in request is empty
                    if (rawToken.isBlank()) {
                        try {
                            rawToken = com.example.BuildConfig.PAIRGATE_API_KEY.replace(Regex("(?i)^bearer\\s+"), "").trim()
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    if (rawToken.isNotBlank() && !rawToken.contains("PLACEHOLDER", ignoreCase = true)) {
                        reqBuilder.header("Authorization", "Bearer $rawToken")
                        reqBuilder.header("api-key", rawToken)
                        reqBuilder.header("X-API-KEY", rawToken)
                    }

                    chain.proceed(reqBuilder.build())
                }
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            return retrofit.create(PairgateApiService::class.java)
        }
    }
}
