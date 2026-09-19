package com.tempodroid.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import com.tempodroid.astronomy.AstronomyCalculator
import com.tempodroid.astronomy.CelestialPositionCalculator
import com.tempodroid.astronomy.SunPositionCalculator
import com.tempodroid.sky.DynamicSkyRenderer
import com.tempodroid.sky.FigmaVisualAssets
import com.tempodroid.sky.SkyRenderInput
import com.tempodroid.sky.TextProtectionOverlay
import com.tempodroid.timeline.SolarLunarTimelineRenderer
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.ui.WidgetLayout
import com.tempodroid.ui.WidgetLayoutMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class WidgetNowClockPlacement(
    val centerXFraction: Float,
    val topFraction: Float,
)

data class WidgetBitmaps(
    val sky: Bitmap,
    val timeline: Bitmap,
    val mode: WidgetLayoutMode,
    val nowClockPlacement: WidgetNowClockPlacement? = null,
)

object WidgetRenderer {
    private val cache = LruCache<String, WidgetBitmaps>(6)
    private val astronomyCalculator = AstronomyCalculator()

    fun render(
        context: Context,
        state: SolarWeatherWidgetState,
        widthPx: Int,
        heightPx: Int,
        useLiveAstronomy: Boolean = true,
    ): WidgetBitmaps {
        val density = context.resources.displayMetrics.density
        val widthDp = widthPx / density
        val heightDp = heightPx / density
        val mode = WidgetLayout.mode(widthDp, heightDp)
        val (w, h) = WidgetDimensions.clampBitmapSize(widthPx, heightPx)
        val zone = runCatching { ZoneId.of(state.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val liveNow = LocalTime.now(zone)
        val now = resolveRenderTime(state, useLiveAstronomy, liveNow)
        val localDate = LocalDate.now(zone)
        val key = listOf(
            w, h, mode, localDate, now.hour, now.minute, useLiveAstronomy,
            state.condition, state.moonPhase, state.moonIllumination,
            state.cloudCover, state.weatherCode, state.precipitation,
            state.sunrise, state.sunset, state.city, state.isStale, state.hasWeatherError,
        ).joinToString("|")
        cache.get(key)?.let { return it }

        val liveAstronomy = if (useLiveAstronomy) {
            runCatching {
                astronomyCalculator.compute(
                    latitude = state.latitude,
                    longitude = state.longitude,
                    date = localDate,
                    timezone = zone,
                    at = now,
                )
            }.getOrNull()
        } else {
            null
        }

        val renderState = liveAstronomy?.let { astronomy ->
            state.copy(
                sunrise = astronomy.sunrise,
                sunset = astronomy.sunset,
                solarNoon = astronomy.solarNoon,
                moonrise = astronomy.moonrise,
                moonset = astronomy.moonset,
                moonPhase = astronomy.moonPhase,
                moonIllumination = astronomy.moonIllumination,
            )
        } ?: state

        val sun = liveAstronomy?.sunPosition ?: SunPositionCalculator.position(
            now,
            renderState.sunrise ?: LocalTime.of(6, 10),
            renderState.sunset ?: LocalTime.of(18, 5),
            renderState.solarNoon,
        )
        val moon = liveAstronomy?.moonPosition ?: run {
            val rise = renderState.moonrise
            val set = renderState.moonset
            if (rise != null && set != null) {
                CelestialPositionCalculator.alongArc(now = now, rise = rise, set = set)
            } else {
                null
            }
        }
        val moonSurface = FigmaVisualAssets.moonSurface(context)

        val sky = DynamicSkyRenderer.render(
            context = context,
            input = SkyRenderInput(
                widthPx = w,
                heightPx = h,
                localTime = now,
                sunrise = renderState.sunrise ?: LocalTime.of(6, 10),
                sunset = renderState.sunset ?: LocalTime.of(18, 5),
                weatherCondition = renderState.condition,
                cloudCover = renderState.cloudCover,
                precipitation = renderState.precipitation,
                sunPosition = sun,
                moonPosition = moon,
                moonPhase = renderState.moonPhase,
                moonIllumination = renderState.moonIllumination,
                hemisphere = renderState.hemisphere,
                solarNoon = renderState.solarNoon,
            ),
            moonSurface = moonSurface,
        )
        val timelineGlassAlpha = TextProtectionOverlay.timelineGlassAlpha(sky)
        val timelineWidthPx = w - (32 * density).toInt()
        val timelineHeightPx = (WidgetDimensions.TIMELINE_HEIGHT_DP * density).toInt().coerceAtLeast(48)
        val compact = mode == WidgetLayoutMode.COMPACT
        val timeline = SolarLunarTimelineRenderer.render(
            widthPx = timelineWidthPx,
            heightPx = timelineHeightPx,
            state = renderState,
            now = now,
            compact = compact,
            moonSurface = moonSurface,
            panelDarknessAlpha = timelineGlassAlpha,
            // No widget real, o texto da hora é um TextClock nativo e nunca fica stale.
            drawNowTime = !useLiveAstronomy,
        )
        val nowClockPlacement = if (useLiveAstronomy) {
            SolarLunarTimelineRenderer.nowClockOverlayPlacement(
                widthPx = timelineWidthPx,
                heightPx = timelineHeightPx,
                state = renderState,
                now = now,
                compact = compact,
            ).let {
                WidgetNowClockPlacement(
                    centerXFraction = it.centerXFraction,
                    topFraction = it.topFraction,
                )
            }
        } else {
            null
        }
        val result = WidgetBitmaps(
            sky = sky,
            timeline = timeline,
            mode = mode,
            nowClockPlacement = nowClockPlacement,
        )
        cache.put(key, result)
        return result
    }

    /**
     * O widget real deve seguir o relógio do aparelho/fuso configurado. displayTime
     * é uma ferramenta de simulação usada apenas quando a astronomia ao vivo está
     * desativada (prévia do App Control e testes visuais).
     */
    internal fun resolveRenderTime(
        state: SolarWeatherWidgetState,
        useLiveAstronomy: Boolean,
        liveNow: LocalTime,
    ): LocalTime = if (useLiveAstronomy) liveNow else state.displayTime ?: liveNow

    fun placeholder(widthPx: Int, heightPx: Int): Bitmap {
        val (w, h) = WidgetDimensions.clampBitmapSize(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { color = 0xFF1E4E8C.toInt() })
        return bitmap
    }
}
