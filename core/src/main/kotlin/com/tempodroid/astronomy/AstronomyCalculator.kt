package com.tempodroid.astronomy

import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
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
 * Sol: algoritmo NOAA Solar Calculator.
 * Lua: posição e nascer/ocaso topocêntricos via commons-suncalc 3.x,
 * biblioteca Apache-2.0 compatível com Android API 26+.
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
        val sunHorizontal = sunHorizontal(latitude, longitude, jd)
        val sunPos = CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = sunHorizontal.altitude,
            azimuthDegrees = sunHorizontal.azimuth,
            aboveHorizon = sunHorizontal.altitude > 0.0,
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
        val sunrise: LocalTime?,
        val sunset: LocalTime?,
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
        val solarNoon = julianToLocalTime(jTransit, timezone)

        if (cosHa >= 1.0 || cosHa <= -1.0) {
            return SolarTimes(
                sunrise = null,
                sunset = null,
                solarNoon = solarNoon,
            )
        }

        val ha = acos(cosHa).toDegrees() / 15.0
        val sunriseJd = jTransit - ha / 24.0
        val sunsetJd = jTransit + ha / 24.0
        return SolarTimes(
            sunrise = julianToLocalTime(sunriseJd, timezone),
            sunset = julianToLocalTime(sunsetJd, timezone),
            solarNoon = solarNoon,
        )
    }

    data class MoonEvents(val rise: LocalTime?, val set: LocalTime?)

    /**
     * Nascer e ocaso lunares topocêntricos. Diferente da implementação antiga,
     * não estima os eventos por apenas 48 amostras da posição simplificada.
     */
    fun moonRiseSet(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        timezone: ZoneId,
    ): MoonEvents {
        val times = MoonTimes.compute()
            .on(date.atStartOfDay(timezone))
            .at(latitude, longitude)
            .execute()

        return MoonEvents(
            rise = times.rise
                ?.withZoneSameInstant(timezone)
                ?.toLocalTime()
                ?.withSecond(0)
                ?.withNano(0),
            set = times.set
                ?.withZoneSameInstant(timezone)
                ?.toLocalTime()
                ?.withSecond(0)
                ?.withNano(0),
        )
    }

    /**
     * A posição visual da Lua sempre parte da altitude e do azimute reais.
     * moonrise/moonset permanecem nos parâmetros apenas por compatibilidade
     * binária/fonte com chamadas anteriores; não dirigem mais a trajetória.
     */
    @Suppress("UNUSED_PARAMETER")
    fun moonPosition(
        latitude: Double,
        longitude: Double,
        dateTime: ZonedDateTime,
        moonrise: LocalTime?,
        moonset: LocalTime?,
    ): CelestialPosition {
        val horiz = moonHorizontal(latitude, longitude, julianDate(dateTime))
        return CelestialPositionCalculator.fromHorizontalCoordinates(
            altitudeDegrees = horiz.altitude,
            azimuthDegrees = horiz.azimuth,
            aboveHorizon = horiz.altitude > 0.0,
        )
    }

    data class Horizontal(val altitude: Double, val azimuth: Double)

    /**
     * Posição lunar topocêntrica/aparente. getAltitude() já considera a
     * refração próxima ao horizonte; o azimute é medido a partir do norte.
     */
    fun moonHorizontal(latitude: Double, longitude: Double, jd: Double): Horizontal {
        val instant = julianToInstant(jd)
        val position = MoonPosition.compute()
            .on(instant)
            .at(latitude, longitude)
            .execute()
        return Horizontal(
            altitude = position.altitude,
            azimuth = position.azimuth.normalizeDegrees(),
        )
    }

    data class Equatorial(val ra: Double, val dec: Double)

    /**
     * Modelo lunar simplificado legado, mantido para compatibilidade de API e
     * testes históricos. A trajetória do widget não usa mais este resultado.
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

    private fun julianToLocalTime(jd: Double, zone: ZoneId): LocalTime =
        julianToInstant(jd).atZone(zone).toLocalTime().withSecond(0).withNano(0)

    private fun julianToInstant(jd: Double): Instant {
        val epochDay = jd - 2440587.5
        val millis = (epochDay * 86_400_000.0).toLong()
        return Instant.ofEpochMilli(millis)
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
