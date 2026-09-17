package com.tempodroid.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import com.tempodroid.astronomy.SunPositionCalculator
import com.tempodroid.sky.DynamicSkyRenderer
import com.tempodroid.sky.SkyRenderInput
import com.tempodroid.timeline.SolarLunarTimelineRenderer
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.ui.WidgetLayout
import com.tempodroid.ui.WidgetLayoutMode
import java.time.LocalTime
import java.time.ZoneId

data class WidgetBitmaps(
    val sky: Bitmap,
    val timeline: Bitmap,
    val mode: WidgetLayoutMode,
)

object WidgetRenderer {
    private val cache = LruCache<String, WidgetBitmaps>(4)

    fun render(context: Context, state: SolarWeatherWidgetState, widthPx: Int, heightPx: Int): WidgetBitmaps {
        val density = context.resources.displayMetrics.density
        val widthDp = widthPx / density
        val heightDp = heightPx / density
        val mode = WidgetLayout.mode(widthDp, heightDp)
        val (w, h) = WidgetDimensions.clampBitmapSize(widthPx, heightPx)
        val zone = runCatching { ZoneId.of(state.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val now = state.displayTime ?: LocalTime.now(zone)
        val key = listOf(
            w, h, mode, now.hour, now.minute / 10,
            state.condition, state.moonPhase, state.cloudCover, state.weatherCode,
            state.sunrise, state.sunset, state.city, state.isStale, state.hasWeatherError,
        ).joinToString("|")
        cache.get(key)?.let { return it }
        val sun = SunPositionCalculator.position(now, state.sunrise ?: LocalTime.of(6, 10), state.sunset ?: LocalTime.of(18, 5), state.solarNoon)
        val moonAbove = com.tempodroid.astronomy.CelestialPositionCalculator.alongArc(
            now = now,
            rise = state.moonrise ?: LocalTime.of(20, 0),
            set = state.moonset ?: LocalTime.of(8, 30),
        )
        val sky = DynamicSkyRenderer.render(
            SkyRenderInput(
                widthPx = w,
                heightPx = h,
                localTime = now,
                sunrise = state.sunrise ?: LocalTime.of(6, 10),
                sunset = state.sunset ?: LocalTime.of(18, 5),
                weatherCondition = state.condition,
                cloudCover = state.cloudCover,
                precipitation = state.precipitation,
                sunPosition = sun,
                moonPosition = moonAbove,
                moonPhase = state.moonPhase,
                moonIllumination = state.moonIllumination,
                hemisphere = state.hemisphere,
                solarNoon = state.solarNoon,
            ),
        )
        val timeline = SolarLunarTimelineRenderer.render(
            widthPx = w - (32 * density).toInt(),
            heightPx = (WidgetDimensions.TIMELINE_HEIGHT_DP * density).toInt().coerceAtLeast(48),
            state = state,
            now = now,
            compact = mode == WidgetLayoutMode.COMPACT,
        )
        val result = WidgetBitmaps(sky = sky, timeline = timeline, mode = mode)
        cache.put(key, result)
        return result
    }

    fun placeholder(widthPx: Int, heightPx: Int): Bitmap {
        val (w, h) = WidgetDimensions.clampBitmapSize(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { color = 0xFF1E4E8C.toInt() })
        return bitmap
    }
}
