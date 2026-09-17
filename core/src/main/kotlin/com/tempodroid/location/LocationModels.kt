package com.tempodroid.location

data class ResolvedLocation(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val source: LocationSource,
)

enum class LocationSource {
    SAVED_COORDINATES,
    AUTHORIZED_COARSE,
    MANUAL_CITY,
    FALLBACK,
}

object LocationFallback {
    val goiania = ResolvedLocation(
        city = "Goiânia",
        latitude = -16.6869,
        longitude = -49.2648,
        timezoneId = "America/Sao_Paulo",
        source = LocationSource.FALLBACK,
    )
}
