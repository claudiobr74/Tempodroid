package com.tempodroid.astronomy

import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.sin

/**
 * Posição do Sol no céu do widget a partir de nascer, meio-dia e ocaso reais.
 * Não usa 06h/18h como regra.
 */
object SunPositionCalculator {

    fun position(
        localTime: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
        solarNoon: LocalTime?,
    ): CelestialPosition {
        val now = minutesOf(localTime)
        val rise = minutesOf(sunrise)
        val set = minutesOf(sunset)
        if (now < rise || now > set) {
            val x = if (now < rise) {
                ((now + 24 * 60 - set).toFloat() / ((rise + 24 * 60 - set).coerceAtLeast(1)))
                    .coerceIn(0f, 1f) * 0.08f
            } else {
                0.92f + ((now - set).toFloat() / ((rise + 24 * 60 - set).coerceAtLeast(1))).coerceIn(0f, 1f) * 0.08f
            }
            return CelestialPosition(
                normalizedX = x.coerceIn(0f, 1f),
                normalizedY = 1.08f,
                aboveHorizon = false,
            )
        }
        val noon = minutesOf(solarNoon ?: LocalTime.of(12, 0))
        val x: Float
        val altitudeFactor: Float
        if (now <= noon) {
            val span = (noon - rise).coerceAtLeast(1)
            val t = (now - rise).toFloat() / span
            x = 0.08f + t * 0.42f
            altitudeFactor = sin((t.coerceIn(0f, 1f)) * (PI / 2.0)).toFloat()
        } else {
            val span = (set - noon).coerceAtLeast(1)
            val t = (now - noon).toFloat() / span
            x = 0.50f + t.coerceIn(0f, 1f) * 0.42f
            altitudeFactor = sin((1f - t.coerceIn(0f, 1f)) * (PI / 2.0)).toFloat()
        }
        val y = 0.88f - altitudeFactor * 0.62f
        return CelestialPosition(
            normalizedX = x.coerceIn(0f, 1f),
            normalizedY = y.coerceIn(0.12f, 1f),
            aboveHorizon = true,
        )
    }
}

object CelestialPositionCalculator {
    /**
     * Interpola arco nascer→ocaso. Lida com astros que cruzam a meia-noite.
     */
    fun alongArc(now: LocalTime, rise: LocalTime, set: LocalTime): CelestialPosition {
        val nowM = minutesOf(now)
        val riseM = minutesOf(rise)
        val setM = minutesOf(set)
        val above = isAboveHorizon(nowM, riseM, setM)
        val progress = arcProgress(nowM, riseM, setM)
        val altitudeFactor = sin(progress.coerceIn(0f, 1f) * PI).toFloat()
        val y = if (above) 0.88f - altitudeFactor * 0.55f else 1.08f
        return CelestialPosition(
            normalizedX = (0.08f + progress.coerceIn(0f, 1f) * 0.84f).coerceIn(0f, 1f),
            normalizedY = y,
            aboveHorizon = above,
        )
    }

    fun isAboveHorizon(nowMinutes: Int, riseMinutes: Int, setMinutes: Int): Boolean {
        return if (riseMinutes <= setMinutes) {
            nowMinutes in riseMinutes until setMinutes
        } else {
            nowMinutes >= riseMinutes || nowMinutes < setMinutes
        }
    }

    fun arcProgress(nowMinutes: Int, riseMinutes: Int, setMinutes: Int): Float {
        val duration = if (setMinutes > riseMinutes) {
            setMinutes - riseMinutes
        } else {
            setMinutes + 24 * 60 - riseMinutes
        }.coerceAtLeast(1)
        val elapsed = if (setMinutes > riseMinutes) {
            nowMinutes - riseMinutes
        } else {
            if (nowMinutes >= riseMinutes) nowMinutes - riseMinutes
            else nowMinutes + 24 * 60 - riseMinutes
        }
        return (elapsed.toFloat() / duration).coerceIn(0f, 1f)
    }
}

fun minutesOf(time: LocalTime): Int = time.hour * 60 + time.minute
