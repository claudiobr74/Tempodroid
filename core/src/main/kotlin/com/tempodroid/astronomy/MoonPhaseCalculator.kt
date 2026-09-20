package com.tempodroid.astronomy

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Fase e iluminação lunar a partir de data juliana.
 *
 * Algoritmo: idade sinódica a partir da Lua nova de 6 jan 2000 18:14 UTC
 * (Meeus / NASA lunar phases). Sem biblioteca externa.
 */
object MoonPhaseCalculator {
    const val SYNODIC_MONTH_DAYS = 29.530588853

    /** Julian Date da Lua nova de 2000-01-06 18:14 UTC. */
    const val KNOWN_NEW_MOON_JD = 2451550.259722

    private const val MAJOR_PHASE_WINDOW_DAYS = 0.65

    fun lunarAgeDays(julianDate: Double): Double {
        val delta = julianDate - KNOWN_NEW_MOON_JD
        val age = delta % SYNODIC_MONTH_DAYS
        return if (age < 0) age + SYNODIC_MONTH_DAYS else age
    }

    fun normalizedCycle(julianDate: Double): Double =
        lunarAgeDays(julianDate) / SYNODIC_MONTH_DAYS

    /**
     * Fração iluminada 0.0..1.0 usando (1 - cos(2π · age / período)) / 2.
     */
    fun illumination(julianDate: Double): Double {
        val age = lunarAgeDays(julianDate)
        val phaseAngle = 2.0 * PI * (age / SYNODIC_MONTH_DAYS)
        val value = (1.0 - cos(phaseAngle)) / 2.0
        return value.coerceIn(0.0, 1.0)
    }

    fun isWaxing(julianDate: Double): Boolean =
        lunarAgeDays(julianDate) < SYNODIC_MONTH_DAYS / 2.0

    /**
     * Classificação nominal da fase baseada na idade sinódica real.
     *
     * As quatro fases principais recebem uma janela curta ao redor do instante
     * astronômico correspondente. Fora dessa janela, a Lua é crescente/gibosa
     * ou minguante/gibosa de acordo com sua posição no ciclo. Isso evita que
     * uma Lua já claramente gibosa permaneça rotulada como "quarto".
     */
    fun phase(julianDate: Double): MoonPhase {
        val age = lunarAgeDays(julianDate)
        val quarter = SYNODIC_MONTH_DAYS / 4.0
        val half = SYNODIC_MONTH_DAYS / 2.0
        val threeQuarter = quarter * 3.0
        val distanceToNew = min(age, SYNODIC_MONTH_DAYS - age)

        return when {
            distanceToNew <= MAJOR_PHASE_WINDOW_DAYS -> MoonPhase.NEW_MOON
            kotlin.math.abs(age - quarter) <= MAJOR_PHASE_WINDOW_DAYS -> MoonPhase.FIRST_QUARTER
            kotlin.math.abs(age - half) <= MAJOR_PHASE_WINDOW_DAYS -> MoonPhase.FULL_MOON
            kotlin.math.abs(age - threeQuarter) <= MAJOR_PHASE_WINDOW_DAYS -> MoonPhase.LAST_QUARTER
            age < quarter -> MoonPhase.WAXING_CRESCENT
            age < half -> MoonPhase.WAXING_GIBBOUS
            age < threeQuarter -> MoonPhase.WANING_GIBBOUS
            else -> MoonPhase.WANING_CRESCENT
        }
    }

    fun phaseFromIllumination(illumination: Double, waxing: Boolean): MoonPhase {
        val illum = illumination.coerceIn(0.0, 1.0)
        return when {
            illum < 0.03 -> MoonPhase.NEW_MOON
            illum > 0.97 -> MoonPhase.FULL_MOON
            illum in 0.46..0.54 -> if (waxing) MoonPhase.FIRST_QUARTER else MoonPhase.LAST_QUARTER
            illum < 0.5 -> if (waxing) MoonPhase.WAXING_CRESCENT else MoonPhase.WANING_CRESCENT
            else -> if (waxing) MoonPhase.WAXING_GIBBOUS else MoonPhase.WANING_GIBBOUS
        }
    }

    fun getMoonOrientation(phase: MoonPhase, hemisphere: Hemisphere): MoonOrientation {
        val waxing = when (phase) {
            MoonPhase.WAXING_CRESCENT, MoonPhase.FIRST_QUARTER, MoonPhase.WAXING_GIBBOUS -> true
            MoonPhase.WANING_GIBBOUS, MoonPhase.LAST_QUARTER, MoonPhase.WANING_CRESCENT -> false
            MoonPhase.NEW_MOON, MoonPhase.FULL_MOON -> true
        }
        return when (hemisphere) {
            Hemisphere.NORTH -> if (waxing) MoonOrientation.LIGHT_FROM_RIGHT else MoonOrientation.LIGHT_FROM_LEFT
            Hemisphere.SOUTH -> if (waxing) MoonOrientation.LIGHT_FROM_LEFT else MoonOrientation.LIGHT_FROM_RIGHT
        }
    }

    /**
     * Semi-eixo do terminador esférico: 0 em quarto, |r| em nova/cheia.
     * Valor positivo = gibbous (mais da metade), negativo = crescente.
     */
    fun terminatorScale(illumination: Double): Double {
        val k = (illumination.coerceIn(0.0, 1.0) * 2.0) - 1.0
        return k
    }

    fun typicalIllumination(phase: MoonPhase): Double = when (phase) {
        MoonPhase.NEW_MOON -> 0.02
        MoonPhase.WAXING_CRESCENT, MoonPhase.WANING_CRESCENT -> 0.18
        MoonPhase.FIRST_QUARTER, MoonPhase.LAST_QUARTER -> 0.50
        MoonPhase.WAXING_GIBBOUS, MoonPhase.WANING_GIBBOUS -> 0.82
        MoonPhase.FULL_MOON -> 1.0
    }

    fun labelPt(phase: MoonPhase): String = when (phase) {
        MoonPhase.NEW_MOON -> "Lua nova"
        MoonPhase.WAXING_CRESCENT -> "Lua crescente"
        MoonPhase.FIRST_QUARTER -> "Quarto crescente"
        MoonPhase.WAXING_GIBBOUS -> "Gibosa crescente"
        MoonPhase.FULL_MOON -> "Lua cheia"
        MoonPhase.WANING_GIBBOUS -> "Gibosa minguante"
        MoonPhase.LAST_QUARTER -> "Quarto minguante"
        MoonPhase.WANING_CRESCENT -> "Lua minguante"
    }
}

internal fun Double.normalizeDegrees(): Double {
    var value = this % 360.0
    if (value < 0) value += 360.0
    return value
}

internal fun Double.toRadians(): Double = this * PI / 180.0
internal fun Double.toDegrees(): Double = this * 180.0 / PI
internal fun floorDivPositive(value: Double, divisor: Double): Double = floor(value / divisor)
