package com.dj.insulink.shared.feature.librelink.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibreLinkLoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class LibreLinkLoginResponse(
    @SerialName("status") val status: Int = 0,
    @SerialName("data") val data: LibreLinkLoginData? = null
)

@Serializable
data class LibreLinkLoginData(
    @SerialName("authTicket") val authTicket: LibreLinkAuthTicket? = null,
    @SerialName("user") val user: LibreLinkUserDto? = null,
    @SerialName("redirect") val redirect: Boolean? = null,
    @SerialName("region") val region: String? = null
)

@Serializable
data class LibreLinkAuthTicket(
    @SerialName("token") val token: String = ""
)

@Serializable
data class LibreLinkUserDto(
    @SerialName("id") val id: String = ""
)

@Serializable
data class LibreLinkConnectionsResponse(
    @SerialName("data") val data: List<LibreLinkConnectionDto> = emptyList()
)

@Serializable
data class LibreLinkConnectionDto(
    @SerialName("patientId") val patientId: String = "",
    @SerialName("firstName") val firstName: String = "",
    @SerialName("lastName") val lastName: String = ""
)

@Serializable
data class LibreLinkGraphResponse(
    @SerialName("data") val data: LibreLinkGraphData? = null
)

@Serializable
data class LibreLinkGraphData(
    @SerialName("connection") val connection: LibreLinkConnectionDetailDto? = null,
    @SerialName("graphData") val graphData: List<LibreLinkGlucoseMeasurementDto> = emptyList()
)

@Serializable
data class LibreLinkConnectionDetailDto(
    @SerialName("glucoseMeasurement") val glucoseMeasurement: LibreLinkGlucoseMeasurementDto? = null
)

@Serializable
data class LibreLinkGlucoseMeasurementDto(
    @SerialName("FactoryTimestamp") val factoryTimestamp: String = "",
    @SerialName("ValueInMgPerDl") val valueInMgPerDl: Int = 0
)
