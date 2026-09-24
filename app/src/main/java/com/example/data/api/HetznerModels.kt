package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HetznerServerType(
    val id: Long,
    val name: String,
    val description: String,
    val cores: Int,
    val memory: Float,
    val disk: Int,
    @Json(name = "prices") val prices: List<HetznerPrice>? = null
)

@JsonClass(generateAdapter = true)
data class HetznerPrice(
    val location: String,
    @Json(name = "price_monthly") val priceMonthly: HetznerAmount? = null
)

@JsonClass(generateAdapter = true)
data class HetznerAmount(
    val net: String,
    val gross: String
)

@JsonClass(generateAdapter = true)
data class HetznerLocation(
    val id: Long,
    val name: String,
    val description: String,
    val country: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "network_zone") val networkZone: String
)

@JsonClass(generateAdapter = true)
data class HetznerDatacenter(
    val id: Long,
    val name: String,
    val description: String,
    val location: HetznerLocation
)

@JsonClass(generateAdapter = true)
data class HetznerDatacentersResponse(
    val datacenters: List<HetznerDatacenter>
)

@JsonClass(generateAdapter = true)
data class HetznerServerPublicNetIp(
    val ip: String
)

@JsonClass(generateAdapter = true)
data class HetznerServerPublicNet(
    val ipv4: HetznerServerPublicNetIp?,
    val ipv6: HetznerServerPublicNetIp?
)

@JsonClass(generateAdapter = true)
data class HetznerServer(
    val id: Long,
    val name: String,
    val status: String,
    val created: String,
    @Json(name = "public_net") val publicNet: HetznerServerPublicNet?,
    @Json(name = "server_type") val serverType: HetznerServerType?,
    val datacenter: HetznerDatacenter?
)

@JsonClass(generateAdapter = true)
data class HetznerServersResponse(
    val servers: List<HetznerServer>
)

@JsonClass(generateAdapter = true)
data class CreateHetznerServerRequest(
    val name: String,
    @Json(name = "server_type") val serverType: String,
    val location: String? = null,
    val datacenter: String? = null,
    val image: String = "ubuntu-24.04",
    @Json(name = "user_data") val userData: String? = null,
    @Json(name = "start_after_create") val startAfterCreate: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CreateHetznerServerResponse(
    val server: HetznerServer,
    val action: HetznerAction? = null,
    @Json(name = "root_password") val rootPassword: String? = null
)

@JsonClass(generateAdapter = true)
data class HetznerAction(
    val id: Long,
    val command: String,
    val status: String,
    val progress: Int
)
