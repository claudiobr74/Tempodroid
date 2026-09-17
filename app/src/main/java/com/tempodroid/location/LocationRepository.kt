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
import java.time.ZoneId
import java.util.Locale

class LocationRepository(
    private val context: Context,
    private val cache: WidgetCacheRepository,
    private val preferences: PreferencesRepository,
    private val http: HttpGetter,
) {
    suspend fun resolve(allowFreshDeviceLocation: Boolean = false): ResolvedLocation {
        cache.savedLocation()?.let { saved ->
            if (saved.source != LocationSource.FALLBACK) return saved.copy(source = LocationSource.SAVED_COORDINATES)
        }
        if (allowFreshDeviceLocation) {
            lastKnownCoarse()?.let { device ->
                cache.saveLocation(device)
                return device
            }
        }
        preferences.manualCity()?.let { city ->
            geocode(city)?.let {
                cache.saveLocation(it)
                return it
            }
        }
        cache.savedLocation()?.let { return it }
        val fallback = LocationFallback.goiania
        cache.saveLocation(fallback)
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

    suspend fun geocode(city: String): ResolvedLocation? {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${city.trim()}&count=1&language=pt&format=json"
        return runCatching {
            val body = http.get(url)
            val root = Json.parseToJsonElement(body).jsonObject
            val result = root["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
            ResolvedLocation(
                city = result["name"]?.jsonPrimitive?.content ?: city,
                latitude = result["latitude"]?.jsonPrimitive?.content?.toDouble() ?: return null,
                longitude = result["longitude"]?.jsonPrimitive?.content?.toDouble() ?: return null,
                timezoneId = result["timezone"]?.jsonPrimitive?.content ?: LocationFallback.goiania.timezoneId,
                source = LocationSource.MANUAL_CITY,
            )
        }.getOrNull()
    }
}
