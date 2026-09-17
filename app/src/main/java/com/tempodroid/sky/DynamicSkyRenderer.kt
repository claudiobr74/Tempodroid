package com.tempodroid.sky

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.astronomy.MoonTerminator
import com.tempodroid.astronomy.SunPositionCalculator
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.weather.WeatherCondition
import kotlin.math.hypot
import kotlin.random.Random

object DynamicSkyRenderer {

    fun render(input: SkyRenderInput): Bitmap {
        val (w, h) = WidgetDimensions.clampBitmapSize(input.widthPx, input.heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val period = DayPeriodCalculator.from(input.localTime, input.sunrise, input.sunset, input.solarNoon)
        val palette = SkyPalette.resolve(
            period = period,
            weather = input.weatherCondition,
            moonPhase = input.moonPhase,
            moonIllumination = input.moonIllumination,
        )
        drawGradient(canvas, w, h, palette)
        drawStars(canvas, w, h, palette, input)

        val sun = input.sunPosition ?: SunPositionCalculator.position(
            input.localTime,
            input.sunrise,
            input.sunset,
            input.solarNoon,
        )
        if (sun.aboveHorizon) {
            val intensity = when (input.weatherCondition) {
                WeatherCondition.STORM -> 0.25f
                WeatherCondition.HEAVY_RAIN -> 0.4f
                WeatherCondition.RAIN, WeatherCondition.CLOUDY -> 0.55f
                WeatherCondition.FOG -> 0.45f
                else -> 1f
            }
            SunRenderer.draw(
                canvas = canvas,
                cx = sun.normalizedX * w,
                cy = sun.normalizedY * h * 0.72f,
                radius = h * 0.11f,
                glowColor = palette.glow,
                intensity = intensity,
            )
        }

        val moon = input.moonPosition
        val isDay = period == DayPeriod.DAY || period == DayPeriod.MORNING || period == DayPeriod.AFTERNOON
        val moonVis = MoonTerminator.skyVisibility(
            aboveHorizon = moon?.aboveHorizon == true,
            isDay = isDay && period != DayPeriod.SUNSET && period != DayPeriod.TWILIGHT,
            weather = input.weatherCondition,
        )
        if (moon != null && moonVis > 0f) {
            MoonRenderer.draw(
                canvas = canvas,
                cx = moon.normalizedX * w,
                cy = moon.normalizedY * h * 0.70f,
                radius = h * if (isDay) 0.07f else 0.10f,
                phase = input.moonPhase,
                illumination = input.moonIllumination,
                hemisphere = input.hemisphere,
                style = MoonRenderStyle.SKY,
                visibility = moonVis,
            )
        }

        drawAtmosphericHorizon(canvas, w, h, period, palette)

        val cover = effectiveCloudCover(input.cloudCover, input.weatherCondition)
        CloudRenderer.draw(
            canvas = canvas,
            width = w,
            height = h,
            cover = cover,
            lit = palette.cloudLit,
            shade = palette.cloudShade,
            seed = input.localTime.hour * 60 + (input.cloudCover ?: 0),
            nightTint = DayPeriodCalculator.isNightLike(period),
        )
        RainRenderer.draw(
            canvas = canvas,
            width = w,
            height = h,
            condition = input.weatherCondition,
            precipitation = input.precipitation,
            rainColor = palette.rain,
            seed = (input.precipitation?.times(10) ?: 0.0).toInt() + cover,
        )
        TextProtectionOverlay.draw(canvas, w, h, palette.scrimTop, palette.scrimBottom)
        return bitmap
    }

    private fun effectiveCloudCover(cloudCover: Int?, weather: WeatherCondition): Int {
        val mapped = when (weather) {
            WeatherCondition.CLEAR -> 8
            WeatherCondition.PARTLY_CLOUDY -> 40
            WeatherCondition.CLOUDY -> 78
            WeatherCondition.FOG -> 70
            WeatherCondition.RAIN -> 88
            WeatherCondition.HEAVY_RAIN -> 94
            WeatherCondition.STORM -> 98
        }
        return (cloudCover ?: mapped)
            .coerceIn(0, 100)
            .coerceAtLeast(if (weather == WeatherCondition.CLEAR) 0 else mapped / 2)
    }

    private fun drawGradient(canvas: Canvas, w: Int, h: Int, palette: SkyPalette) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                h.toFloat(),
                intArrayOf(palette.zenith, palette.mid, palette.horizon),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val glow = Paint().apply {
            shader = LinearGradient(
                w * 0.55f,
                h * 0.15f,
                w * 0.85f,
                h * 0.85f,
                intArrayOf(withAlpha(palette.glow, 84), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glow)
    }

    private fun drawStars(
        canvas: Canvas,
        w: Int,
        h: Int,
        palette: SkyPalette,
        input: SkyRenderInput,
    ) {
        if (palette.starAlpha < 8) return
        val random = Random(20260917)
        val count = palette.starAlpha / 3
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val moon = input.moonPosition
        val moonX = (moon?.normalizedX ?: -1f) * w
        val moonY = (moon?.normalizedY ?: -1f) * h * 0.7f
        val avoid = if (input.moonPhase == com.tempodroid.astronomy.MoonPhase.FULL_MOON) h * 0.22f else h * 0.08f
        repeat(count) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h * 0.62f
            if (moon?.aboveHorizon == true && hypot(x - moonX, y - moonY) < avoid) return@repeat
            val a = (12 + random.nextInt(palette.starAlpha)).coerceAtMost(255)
            paint.color = Color.argb(a, 230, 235, 255)
            canvas.drawCircle(x, y, if (random.nextFloat() > 0.85f) 1.6f else 0.9f, paint)
        }
    }

    private fun drawAtmosphericHorizon(
        canvas: Canvas,
        w: Int,
        h: Int,
        period: DayPeriod,
        palette: SkyPalette,
    ) {
        val haze = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                h * 0.58f,
                0f,
                h.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    withAlpha(palette.horizon, 44),
                    withAlpha(palette.horizon, 108),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, h * 0.58f, w.toFloat(), h.toFloat(), haze)

        val showSilhouette = period == DayPeriod.NIGHT ||
            period == DayPeriod.TWILIGHT ||
            period == DayPeriod.SUNSET ||
            period == DayPeriod.DAWN
        if (!showSilhouette) return

        val base = h * 0.92f
        val path = Path().apply {
            moveTo(0f, h.toFloat())
            lineTo(0f, base)
            quadTo(w * 0.18f, base - h * 0.025f, w * 0.34f, base - h * 0.012f)
            quadTo(w * 0.52f, base - h * 0.035f, w * 0.70f, base - h * 0.016f)
            quadTo(w * 0.86f, base - h * 0.026f, w.toFloat(), base - h * 0.010f)
            lineTo(w.toFloat(), h.toFloat())
            close()
        }
        val silhouette = when (period) {
            DayPeriod.NIGHT, DayPeriod.TWILIGHT -> Color.argb(150, 8, 16, 30)
            DayPeriod.SUNSET -> Color.argb(110, 40, 24, 44)
            else -> Color.argb(78, 20, 40, 48)
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = silhouette })

        if (period == DayPeriod.NIGHT || period == DayPeriod.TWILIGHT) {
            val lights = Paint(Paint.ANTI_ALIAS_FLAG)
            val random = Random(44)
            repeat(9) {
                lights.color = Color.argb(105, 255, 216, 152)
                canvas.drawCircle(
                    w * (0.24f + random.nextFloat() * 0.52f),
                    h * (0.945f + random.nextFloat() * 0.02f),
                    1.0f,
                    lights,
                )
            }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
}
