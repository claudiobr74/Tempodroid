package com.tempodroid.sky

import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCondition

/**
 * Paletas ARGB (0xAARRGGBB) para o céu dinâmico.
 */
data class SkyPalette(
    val zenith: Int,
    val mid: Int,
    val horizon: Int,
    val glow: Int,
    val starAlpha: Int,
    val cloudLit: Int,
    val cloudShade: Int,
    val rain: Int,
    val scrimTop: Int,
    val scrimBottom: Int,
) {
    companion object {
        fun resolve(
            period: DayPeriod,
            weather: WeatherCondition,
            moonPhase: MoonPhase,
            moonIllumination: Double,
        ): SkyPalette {
            val base = when (period) {
                DayPeriod.DAWN -> DAWN
                DayPeriod.MORNING -> MORNING
                DayPeriod.DAY -> DAY
                DayPeriod.AFTERNOON -> AFTERNOON
                DayPeriod.SUNSET -> SUNSET
                DayPeriod.TWILIGHT -> TWILIGHT
                DayPeriod.NIGHT -> nightPalette(moonPhase, moonIllumination)
            }
            return applyWeather(base, weather, period)
        }

        val DAWN = SkyPalette(
            zenith = 0xFF1B2A4A.toInt(),
            mid = 0xFF5B6FA8.toInt(),
            horizon = 0xFFF3B48A.toInt(),
            glow = 0xFFFFD4A3.toInt(),
            starAlpha = 80,
            cloudLit = 0xFFFFE4C8.toInt(),
            cloudShade = 0xFF8A7A92.toInt(),
            rain = 0x66B0C4DE.toInt(),
            scrimTop = 0x66081420.toInt(),
            scrimBottom = 0x730A1220.toInt(),
        )
        val MORNING = SkyPalette(
            zenith = 0xFF3A8FD4.toInt(),
            mid = 0xFF6EC6F0.toInt(),
            horizon = 0xFFFFE0B0.toInt(),
            glow = 0xFFFFF3C4.toInt(),
            starAlpha = 0,
            cloudLit = 0xFFFFF8F0.toInt(),
            cloudShade = 0xFFB9C9DC.toInt(),
            rain = 0x66A0B8CC.toInt(),
            scrimTop = 0x40081A2C.toInt(),
            scrimBottom = 0x590A2030.toInt(),
        )
        val DAY = SkyPalette(
            zenith = 0xFF1E7AD6.toInt(),
            mid = 0xFF4EB7EA.toInt(),
            horizon = 0xFFFFC978.toInt(),
            glow = 0xFFFFF1A8.toInt(),
            starAlpha = 0,
            cloudLit = 0xFFFFFFFF.toInt(),
            cloudShade = 0xFFC5D6EA.toInt(),
            rain = 0x6690A8C0.toInt(),
            scrimTop = 0x33051A30.toInt(),
            scrimBottom = 0x4D082030.toInt(),
        )
        val AFTERNOON = SkyPalette(
            zenith = 0xFF2B6CB0.toInt(),
            mid = 0xFF5AA8D8.toInt(),
            horizon = 0xFFFFB35C.toInt(),
            glow = 0xFFFFE08A.toInt(),
            starAlpha = 0,
            cloudLit = 0xFFFFF0D8.toInt(),
            cloudShade = 0xFFA8B8C8.toInt(),
            rain = 0x6698A8B8.toInt(),
            scrimTop = 0x40081624.toInt(),
            scrimBottom = 0x590A1C28.toInt(),
        )
        val SUNSET = SkyPalette(
            zenith = 0xFF3A1D5C.toInt(),
            mid = 0xFFD45A6A.toInt(),
            horizon = 0xFFFF9A4A.toInt(),
            glow = 0xFFFFC56A.toInt(),
            starAlpha = 30,
            cloudLit = 0xFFFFC8A0.toInt(),
            cloudShade = 0xFF6A3A58.toInt(),
            rain = 0x66807090.toInt(),
            scrimTop = 0x59081220.toInt(),
            scrimBottom = 0x730A1020.toInt(),
        )
        val TWILIGHT = SkyPalette(
            zenith = 0xFF121433.toInt(),
            mid = 0xFF3A2A68.toInt(),
            horizon = 0xFF6A4A8A.toInt(),
            glow = 0xFF8A6AB0.toInt(),
            starAlpha = 140,
            cloudLit = 0xFFC8B8E0.toInt(),
            cloudShade = 0xFF2A2448.toInt(),
            rain = 0x66506080.toInt(),
            scrimTop = 0x73080C1C.toInt(),
            scrimBottom = 0x8C060A18.toInt(),
        )
        val NIGHT = SkyPalette(
            zenith = 0xFF071022.toInt(),
            mid = 0xFF152048.toInt(),
            horizon = 0xFF1E2A58.toInt(),
            glow = 0xFF8AA0D8.toInt(),
            starAlpha = 200,
            cloudLit = 0xFF6A7AA0.toInt(),
            cloudShade = 0xFF12182C.toInt(),
            rain = 0x66405070.toInt(),
            scrimTop = 0x8C040814.toInt(),
            scrimBottom = 0xA6000814.toInt(),
        )

        private fun nightPalette(phase: MoonPhase, illumination: Double): SkyPalette {
            val lift = (illumination.coerceIn(0.0, 1.0) * 28).toInt()
            val stars = when (phase) {
                MoonPhase.NEW_MOON -> 230
                MoonPhase.WAXING_CRESCENT, MoonPhase.WANING_CRESCENT -> 190
                MoonPhase.FIRST_QUARTER, MoonPhase.LAST_QUARTER -> 150
                MoonPhase.WAXING_GIBBOUS, MoonPhase.WANING_GIBBOUS -> 110
                MoonPhase.FULL_MOON -> 80
            }
            val horizonGlow = if (phase == MoonPhase.FULL_MOON) 0xFF4A5A88.toInt() else NIGHT.horizon
            return NIGHT.copy(
                mid = liftChannel(NIGHT.mid, lift),
                horizon = liftChannel(horizonGlow, lift / 2),
                glow = if (phase == MoonPhase.FULL_MOON) 0xFFB8C8F0.toInt() else NIGHT.glow,
                starAlpha = stars,
                cloudLit = if (illumination > 0.6) 0xFF8A9AB8.toInt() else NIGHT.cloudLit,
            )
        }

        private fun applyWeather(
            base: SkyPalette,
            weather: WeatherCondition,
            period: DayPeriod,
        ): SkyPalette = when (weather) {
            WeatherCondition.CLEAR -> base
            WeatherCondition.PARTLY_CLOUDY -> base.copy(
                mid = mix(base.mid, 0xFF8AA8C0.toInt(), 0.18f),
                starAlpha = (base.starAlpha * 0.7).toInt(),
            )
            WeatherCondition.CLOUDY, WeatherCondition.SNOW -> desaturate(base, 0.35f).copy(
                zenith = mix(base.zenith, 0xFF6A7A8C.toInt(), 0.45f),
                mid = mix(base.mid, 0xFF8A96A4.toInt(), 0.40f),
                horizon = mix(base.horizon, 0xFF9AA4B0.toInt(), 0.35f),
                starAlpha = (base.starAlpha * 0.25).toInt(),
            )
            WeatherCondition.FOG -> desaturate(base, 0.5f).copy(
                zenith = mix(base.zenith, 0xFF9AA8B4.toInt(), 0.5f),
                horizon = 0xFFC8D0D6.toInt(),
                starAlpha = 0,
            )
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> desaturate(base, 0.45f).copy(
                zenith = mix(base.zenith, 0xFF4A5A68.toInt(), 0.55f),
                mid = mix(base.mid, 0xFF5A6A78.toInt(), 0.50f),
                horizon = mix(base.horizon, 0xFF6A7884.toInt(), 0.40f),
                glow = mix(base.glow, 0xFF8899A8.toInt(), 0.5f),
                starAlpha = (base.starAlpha * 0.1).toInt(),
            )
            WeatherCondition.STORM -> desaturate(base, 0.6f).copy(
                zenith = 0xFF1A2430.toInt(),
                mid = 0xFF2A3540.toInt(),
                horizon = 0xFF3A4450.toInt(),
                glow = 0xFF4A5560.toInt(),
                starAlpha = 0,
                scrimTop = 0x99000000.toInt(),
            )
        }.let { pal ->
            if (period == DayPeriod.NIGHT && weather == WeatherCondition.STORM) {
                pal.copy(zenith = 0xFF0A1018.toInt(), mid = 0xFF141C28.toInt())
            } else pal
        }

        private fun desaturate(p: SkyPalette, amount: Float) = p.copy(
            zenith = mix(p.zenith, grayOf(p.zenith), amount),
            mid = mix(p.mid, grayOf(p.mid), amount),
            horizon = mix(p.horizon, grayOf(p.horizon), amount),
        )

        fun mix(a: Int, b: Int, t: Float): Int {
            val clamped = t.coerceIn(0f, 1f)
            fun ch(shift: Int): Int {
                val ca = (a shr shift) and 0xFF
                val cb = (b shr shift) and 0xFF
                return (ca + (cb - ca) * clamped).toInt()
            }
            return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }

        private fun grayOf(color: Int): Int {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val y = (r * 30 + g * 59 + b * 11) / 100
            val a = color ushr 24
            return (a shl 24) or (y shl 16) or (y shl 8) or y
        }

        private fun liftChannel(color: Int, lift: Int): Int {
            fun ch(shift: Int): Int = (((color shr shift) and 0xFF) + lift).coerceIn(0, 255)
            return (color and 0xFF000000.toInt()) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}
