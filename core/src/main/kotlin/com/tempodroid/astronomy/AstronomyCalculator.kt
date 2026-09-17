package com.tempodroid.astronomy

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Cálculo local de Sol e Lua.
 *
 * Sol: algoritmo NOAA Solar Calculator (equação do tempo + declinação).
 * Lua: posição simplificada de Meeus (cap. 47, termos principais) e
 * moonrise/moonset por amostragem de altitude ao longo do dia.
 *
 * Nenhuma biblioteca externa. Licença do código: do próprio app.
 */
class AstronomyCalculator {

    fun julianDate(dateTime: ZonedDateTime): Double {
        val utc = dateTime.withZoneSameInstant(ZoneId.of("UTC"))
        return julianDateUtc(
            utc.year,
            utc.monthValue,
            utc.dayOfMonth,
            utc.hour,
            utc.minute,
            utc.second + utc.nano / 1_000_000_000.0,
        )
    }

    fun julianDateUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val dayFraction = (hour + minute / 60.0 + second / 3600.0) / 24.0
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5 + dayFraction
    }

    fun compute(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timezone: ZoneId,
        at: LocalTime = LocalTime.now(timezone),
    ): AstronomyData {
        val solar = solarTimes(latitude, longitude, date, timezone)
        val moonEvents = moonRiseSet(latitude, longitude, date, timezone)
        val now = ZonedDateTime.of(date, at, timezone)
        val jd = julianDate(now)
        val phase = MoonPhaseCalculator.phase(jd)
        val illumination = MoonPhaseCalculator.illumination(jd)
        val sunPos = SunPositionCalculator.position(
            localTime = at,
            sunrise = solar.sunrise,
            sunset = solar.sunset,
            solarNoon = solar.solarNoon,
        )
        val moonPos = moonPosition(
            latitude = latitude,
            longitude = longitude,
            dateTime = now,
            moonrise = moonEvents.rise,
            moonset = moonEvents.set,
        )
        return AstronomyData(
            sunrise = solar.sunrise,
            sunset = solar.sunset,
            solarNoon = solar.solarNoon,
            moonrise = moonEvents.rise,
            moonset = moonEvents.set,
            moonPhase = phase,
            moonIllumination = illumination,
            sunPosition = sunPos,
            moonPosition = moonPos,
            hemisphere = hemisphereFromLatitude(latitude),
        )
    }

    data class SolarTimes(
        val sunrise: LocalTime,
        val sunset: LocalTime,
        val solarNoon: LocalTime,
    )

    fun solarTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timezone: ZoneId,
    ): SolarTimes {
        val n = date.toJulianDay() - 2451545.0 + 0.0008
        val jStar = n - longitude / 360.0
        val m = (357.5291 + 0.98560028 * jStar).normalizeDegrees()
        val mRad = m.toRadians()
        val c = 1.9148 * sin(mRad) + 0.0200 * sin(2 * mRad) + 0.0003 * sin(3 * mRad)
        val lambda = (m + c + 180.0 + 102.9372).normalizeDegrees()
        val jTransit = 2451545.0 + jStar + 0.0053 * sin(mRad) - 0.0069 * sin(2 * lambda.toRadians())
        val sinDec = sin(lambda.toRadians()) * sin(23.4397.toRadians())
        val dec = asin(sinDec)
        val latRad = latitude.toRadians()
        val cosHa = (sin((-0.833).toRadians()) - sin(latRad) * sinDec) / (cos(latRad) * cos(dec))
        val ha = when {
            cosHa >= 1.0 -> 0.0
            cosHa <= -1.0 -> 12.0
            else -> acos(cosHa).toDegrees() / 15.0
        }
        val solarNoonJd = jTransit
        val sunriseJd = jTransit - ha / 24.0
        val sunsetJd = jTransit + ha / 24.0
        return SolarTimes(
            sunrise = julianToLocalTime(sunriseJd, timezone),
            sunset = julianToLocalTime(sunsetJd, timezone),
            solarNoon = julianToLocalTime(solarNoonJd, timezone),
        )
    }

    data class MoonEvents(val rise: LocalTime?, val set: LocalTime?)

    fun moonRiseSet(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timezone: ZoneId,
    ): MoonEvents {
        val samples = 48
        val altitudes = Array(samples + 1) { i ->
            val minutes = (i * (24 * 60) / samples)
            val time = LocalTime.of(minutes / 60 % 24, minutes % 60)
            val zdt = ZonedDateTime.of(date, time, timezone)
            moonHorizontal(latitude, longitude, julianDate(zdt)).altitude
        }
        var riseMinutes: Int? = null
        var setMinutes: Int? = null
        for (i in 0 until samples) {
            val a0 = altitudes[i]
            val a1 = altitudes[i + 1]
            if (a0 < 0 && a1 >= 0 && riseMinutes == null) {
                val t = a0 / (a0 - a1)
                riseMinutes = ((i + t) * (24 * 60) / samples).roundToInt().coerceIn(0, 24 * 60 - 1)
            }
            if (a0 >= 0 && a1 < 0 && setMinutes == null) {
                val t = a0 / (a0 - a1)
                setMinutes = ((i + t) * (24 * 60) / samples).roundToInt().coerceIn(0, 24 * 60 - 1)
            }
        }
        return MoonEvents(
            rise = riseMinutes?.let { minutesToLocalTime(it) },
            set = setMinutes?.let { minutesToLocalTime(it) },
        )
    }

    fun moonPosition(
        latitude: Double,
        longitude: Double,
        dateTime: ZonedDateTime,
        moonrise: LocalTime?,
        moonset: LocalTime?,
    ): CelestialPosition {
        val horiz = moonHorizontal(latitude, longitude, julianDate(dateTime))
        val above = horiz.altitude > 0.0
        if (moonrise != null && moonset != null) {
            val approx = CelestialPositionCalculator.alongArc(
                now = dateTime.toLocalTime(),
                rise = moonrise,
                set = moonset,
            )
            return approx.copy(
                aboveHorizon = above,
                altitudeDegrees = horiz.altitude,
                azimuthDegrees = horiz.azimuth,
            )
        }
        val x = ((horiz.azimuth + 90.0) / 180.0).toFloat().coerceIn(0f, 1f)
        val y = (1.0 - (horiz.altitude / 90.0).coerceIn(0.0, 1.0)).toFloat()
        return CelestialPosition(
            normalizedX = x,
            normalizedY = y,
            aboveHorizon = above,
            altitudeDegrees = horiz.altitude,
            azimuthDegrees = horiz.azimuth,
        )
    }

    data class Horizontal(val altitude: Double, val azimuth: Double)

    fun moonHorizontal(latitude: Double, longitude: Double, jd: Double): Horizontal {
        val equatorial = moonEquatorial(jd)
        val gst = greenwichSiderealTime(jd)
        val lst = (gst + longitude).normalizeDegrees()
        val ha = (lst - equatorial.ra).normalizeDegrees().toRadians()
        val dec = equatorial.dec.toRadians()
        val lat = latitude.toRadians()
        val sinAlt = sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(ha)
        val alt = asin(sinAlt.coerceIn(-1.0, 1.0)).toDegrees()
        val y = -cos(dec) * sin(ha)
        val x = sin(dec) * cos(lat) - cos(dec) * sin(lat) * cos(ha)
        var az = atan2(y, x).toDegrees().normalizeDegrees()
        return Horizontal(altitude = alt, azimuth = az)
    }

    data class Equatorial(val ra: Double, val dec: Double)

    /**
     * Longitude/latitude eclíptica lunar (termos principais de Meeus) → equatorial.
     */
    fun moonEquatorial(jd: Double): Equatorial {
        val t = (jd - 2451545.0) / 36525.0
        val lp = (218.3164477 + 481267.88123421 * t).normalizeDegrees()
        val d = (297.8501921 + 445267.1114034 * t).normalizeDegrees()
        val m = (357.5291092 + 35999.0502909 * t).normalizeDegrees()
        val mp = (134.9633964 + 477198.8675055 * t).normalizeDegrees()
        val f = (93.2720950 + 483202.0175233 * t).normalizeDegrees()
        val dRad = d.toRadians()
        val mRad = m.toRadians()
        val mpRad = mp.toRadians()
        val fRad = f.toRadians()
        val lon = lp +
            6.289 * sin(mpRad) +
            1.274 * sin(2 * dRad - mpRad) +
            0.658 * sin(2 * dRad) +
            0.214 * sin(2 * mpRad) +
            0.110 * sin(dRad)
        val lat = 5.128 * sin(fRad) +
            0.281 * sin(mpRad + fRad) +
            0.278 * sin(mpRad - fRad)
        val lambda = lon.normalizeDegrees().toRadians()
        val beta = lat.toRadians()
        val eps = (23.439291 - 0.0130042 * t).toRadians()
        val ra = atan2(
            sin(lambda) * cos(eps) - tan(beta) * sin(eps),
            cos(lambda),
        ).toDegrees().normalizeDegrees()
        val dec = asin(
            (sin(beta) * cos(eps) + cos(beta) * sin(eps) * sin(lambda)).coerceIn(-1.0, 1.0),
        ).toDegrees()
        return Equatorial(ra = ra, dec = dec)
    }

    fun sunHorizontal(latitude: Double, longitude: Double, jd: Double): Horizontal {
        val n = jd - 2451545.0
        val L = (280.460 + 0.9856474 * n).normalizeDegrees()
        val g = (357.528 + 0.9856003 * n).normalizeDegrees().toRadians()
        val lambda = (L + 1.915 * sin(g) + 0.020 * sin(2 * g)).normalizeDegrees().toRadians()
        val eps = 23.439.toRadians()
        val ra = atan2(cos(eps) * sin(lambda), cos(lambda)).toDegrees().normalizeDegrees()
        val dec = asin(sin(eps) * sin(lambda)).toDegrees()
        val gst = greenwichSiderealTime(jd)
        val lst = (gst + longitude).normalizeDegrees()
        val ha = (lst - ra).normalizeDegrees().toRadians()
        val decRad = dec.toRadians()
        val lat = latitude.toRadians()
        val sinAlt = sin(lat) * sin(decRad) + cos(lat) * cos(decRad) * cos(ha)
        val alt = asin(sinAlt.coerceIn(-1.0, 1.0)).toDegrees()
        val y = -cos(decRad) * sin(ha)
        val x = sin(decRad) * cos(lat) - cos(decRad) * sin(lat) * cos(ha)
        val az = atan2(y, x).toDegrees().normalizeDegrees()
        return Horizontal(altitude = alt, azimuth = az)
    }

    private fun greenwichSiderealTime(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * t.pow(2)
        return gmst.normalizeDegrees()
    }

    private fun julianToLocalTime(jd: Double, zone: ZoneId): LocalTime {
        val epochDay = jd - 2440587.5
        val millis = (epochDay * 86400000.0).toLong()
        val instant = Instant.ofEpochMilli(millis)
        return instant.atZone(zone).toLocalTime().withSecond(0).withNano(0)
    }

    private fun minutesToLocalTime(total: Int): LocalTime {
        val clamped = total.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(clamped / 60, clamped % 60)
    }

    private fun LocalDate.toJulianDay(): Double {
        return julianDateUtc(year, monthValue, dayOfMonth, 12, 0, 0.0)
    }
}

interface AstronomyRepository {
    fun getAstronomy(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timezone: ZoneId,
        at: LocalTime = LocalTime.now(timezone),
    ): AstronomyData
}

class AstronomyRepositoryImpl(
    private val calculator: AstronomyCalculator = AstronomyCalculator(),
) : AstronomyRepository {
    override fun getAstronomy(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timezone: ZoneId,
        at: LocalTime,
    ): AstronomyData = calculator.compute(latitude, longitude, date, timezone, at)
}
