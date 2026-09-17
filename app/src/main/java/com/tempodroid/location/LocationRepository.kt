package com.tempodroid.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.tempodroid.data.PreferencesRepository
import com.tempodroid.data.WidgetCacheRepository
import com.tempodroid.weather.HttpGetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.Locale

class LocationRepository(
    private val context: Context,
    private val cache: WidgetCacheRepository,
    private val preferences: PreferencesRepository,
    private val http: HttpGetter,
) {
    suspend fun resolve(
        appWidgetId: Int? = null,
        allowFreshDeviceLocation: Boolean = false,
    ): ResolvedLocation {
        cache.savedLocation(appWidgetId)?.let { return it }

        if (allowFreshDeviceLocation) {
            lastKnownCoarse()?.let { device ->
                cache.saveLocation(device, appWidgetId)
                return device
            }
        }

        if (appWidgetId == null) {
            preferences.manualCity()?.let { city ->
                geocode(city)?.let {
                    cache.saveLocation(it)
                    return it
                }
            }
        }

        val fallback = LocationFallback.goiania
        cache.saveLocation(fallback, appWidgetId)
        return fallback
    }

    @SuppressLint("MissingPermission")
    suspend fun lastKnownCoarse(): ResolvedLocation? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            ?: return null
        val city = reverseGeocodeCity(location.latitude, location.longitude) ?: "Localização atual"
        return ResolvedLocation(
            city = city,
            latitude = location.latitude,
            longitude = location.longitude,
            timezoneId = ZoneId.systemDefault().id,
            source = LocationSource.AUTHORIZED_COARSE,
        )
    }

    private suspend fun reverseGeocodeCity(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale("pt", "BR"))
                    .getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
                    ?.let { address ->
                        address.locality
                            ?: address.subAdminArea
                            ?: address.adminArea
                    }
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }

    suspend fun searchCities(query: String, limit: Int = 5): List<LocationSearchResult> {
        val normalized = query.trim()
        if (normalized.length < 2) return emptyList()
        val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8.toString())
        val count = limit.coerceIn(1, 10)
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$count&language=pt&format=json"
        val body = http.get(url)
        val root = Json.parseToJsonElement(body).jsonObject
        return root["results"]?.jsonArray.orEmpty().mapNotNull { element ->
            val result = element.jsonObject
            val name = result["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val latitude = result["latitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
            val longitude = result["longitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
            LocationSearchResult(
                name = name,
                admin1 = result["admin1"]?.jsonPrimitive?.content,
                country = result["country"]?.jsonPrimitive?.content,
                latitude = latitude,
                longitude = longitude,
                timezoneId = result["timezone"]?.jsonPrimitive?.content ?: LocationFallback.goiania.timezoneId,
            )
        }
    }

    suspend fun geocode(city: String): ResolvedLocation? = runCatching {
        searchCities(city, limit = 1).firstOrNull()?.toResolvedLocation()
    }.getOrNull()
}
