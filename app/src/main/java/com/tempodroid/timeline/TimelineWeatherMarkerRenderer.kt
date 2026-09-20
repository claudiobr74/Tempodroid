package com.tempodroid.timeline

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.tempodroid.sky.DayPeriod
import com.tempodroid.weather.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ícones coloridos do marcador "Agora" da seção 09 · Refined Widget Previews v2.
 * A geometria é vetorial/Canvas para permanecer nítida em todas as densidades.
 */
object TimelineWeatherMarkerRenderer {

    internal enum class Glyph {
        SUN,
        CLOUD_SUN,
        CLOUD,
        RAIN,
        SNOW,
        STORM,
        MOON,
        CLOUD_MOON,
    }

    internal data class GlyphPalette(
        val primary: Int,
        val secondary: Int,
        val accent: Int,
    )

    internal fun glyphFor(condition: WeatherCondition, period: DayPeriod): Glyph {
        val night = period == DayPeriod.NIGHT
        return when (condition) {
            WeatherCondition.CLEAR -> if (night) Glyph.MOON else Glyph.SUN
            WeatherCondition.PARTLY_CLOUDY -> if (night) Glyph.CLOUD_MOON else Glyph.CLOUD_SUN
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> Glyph.CLOUD
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Glyph.RAIN
            WeatherCondition.SNOW -> Glyph.SNOW
            WeatherCondition.STORM -> Glyph.STORM
        }
    }

    internal fun paletteFor(condition: WeatherCondition, period: DayPeriod): GlyphPalette =
        when (glyphFor(condition, period)) {
            Glyph.SUN -> GlyphPalette(SUN_GOLD, SUN_GOLD, SUN_GOLD)
            Glyph.CLOUD_SUN -> GlyphPalette(CLOUD_WHITE, SUN_GOLD, CLOUD_BLUE)
            Glyph.CLOUD -> GlyphPalette(CLOUD_WHITE, CLOUD_BLUE, CLOUD_SHADE)
            Glyph.RAIN -> GlyphPalette(RAIN_CLOUD_LIGHT, RAIN_CLOUD, RAIN_BLUE)
            Glyph.SNOW -> GlyphPalette(SNOW_CLOUD_LIGHT, SNOW_CLOUD, SNOW_FLAKE)
            Glyph.STORM -> GlyphPalette(STORM_LIGHT, STORM_CLOUD, LIGHTNING_YELLOW)
            Glyph.MOON -> GlyphPalette(MOON_GOLD, NIGHT_CUTOUT, MOON_GOLD)
            Glyph.CLOUD_MOON -> GlyphPalette(CLOUD_WHITE, MOON_GOLD, CLOUD_BLUE)
        }

    /** Compatibilidade com testes/consumidores anteriores: devolve a cor dominante. */
    internal fun colorFor(condition: WeatherCondition, period: DayPeriod): Int =
        paletteFor(condition, period).primary

    internal fun bubbleAccent(period: DayPeriod): Int =
        if (period == DayPeriod.NIGHT) 0xFF94A3B8.toInt() else NOW_LABEL_GOLD

    fun draw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        sizePx: Float,
        condition: WeatherCondition,
        period: DayPeriod,
    ) {
        if (sizePx <= 0f) return
        val scale = sizePx / 28f
        canvas.save()
        canvas.translate(cx - sizePx / 2f, cy - sizePx / 2f)
        canvas.scale(scale, scale)
        when (glyphFor(condition, period)) {
            Glyph.SUN -> drawSun(canvas)
            Glyph.CLOUD_SUN -> drawCloudSun(canvas)
            Glyph.CLOUD -> drawCloud(canvas, CLOUD_WHITE, CLOUD_BLUE, CLOUD_SHADE)
            Glyph.RAIN -> drawRain(canvas)
            Glyph.SNOW -> drawSnow(canvas)
            Glyph.STORM -> drawStorm(canvas)
            Glyph.MOON -> drawMoon(canvas)
            Glyph.CLOUD_MOON -> drawCloudMoon(canvas)
        }
        canvas.restore()
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun drawSun(canvas: Canvas) {
        val sun = fill(SUN_GOLD)
        canvas.drawCircle(14f, 14f, 7f, sun)
        val ray = stroke(SUN_GOLD, 2f)
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45.0) - 90.0)
            canvas.drawLine(
                14f + cos(a).toFloat() * 10f,
                14f + sin(a).toFloat() * 10f,
                14f + cos(a).toFloat() * 13f,
                14f + sin(a).toFloat() * 13f,
                ray,
            )
        }
    }

    private fun drawCloudSun(canvas: Canvas) {
        canvas.drawCircle(9f, 9f, 6f, fill(SUN_GOLD))
        val ray = stroke(SUN_GOLD, 1.5f)
        val rays = arrayOf(
            floatArrayOf(9f, 0f, 9f, 2f),
            floatArrayOf(1f, 9f, 3f, 9f),
            floatArrayOf(3f, 3f, 4.5f, 4.5f),
            floatArrayOf(14f, 3f, 15.5f, 1.5f),
        )
        rays.forEach { canvas.drawLine(it[0], it[1], it[2], it[3], ray) }
        drawCloud(canvas, CLOUD_WHITE, CLOUD_WHITE, CLOUD_WHITE, xOffset = 1f, yOffset = 2f)
    }

    private fun drawCloud(
        canvas: Canvas,
        light: Int,
        main: Int,
        shade: Int,
        xOffset: Float = 0f,
        yOffset: Float = 0f,
    ) {
        val base = fill(main)
        val left = fill(light)
        val right = fill(shade)
        canvas.drawRoundRect(RectF(5f + xOffset, 14f + yOffset, 25f + xOffset, 23f + yOffset), 4.5f, 4.5f, base)
        canvas.drawCircle(10f + xOffset, 14f + yOffset, 5f, left)
        canvas.drawCircle(18f + xOffset, 12.5f + yOffset, 5.2f, base)
        canvas.drawCircle(23f + xOffset, 16f + yOffset, 3.5f, right)
    }

    private fun drawRain(canvas: Canvas) {
        canvas.drawRoundRect(RectF(4f, 9f, 26f, 17f), 4f, 4f, fill(RAIN_CLOUD))
        canvas.drawCircle(9f, 9f, 6f, fill(RAIN_CLOUD_LIGHT))
        canvas.drawCircle(18f, 8f, 4.7f, fill(RAIN_CLOUD))
        val x = floatArrayOf(5f, 12f, 19f, 26f)
        val len = floatArrayOf(7f, 9f, 7f, 9f)
        x.forEachIndexed { index, xx ->
            canvas.drawLine(
                xx,
                19f,
                xx - 1.5f,
                19f + len[index],
                stroke(if (index % 2 == 0) RAIN_BLUE_LIGHT else RAIN_BLUE, 2f),
            )
        }
    }

    private fun drawSnow(canvas: Canvas) {
        canvas.drawRoundRect(RectF(4f, 8f, 26f, 16f), 4f, 4f, fill(SNOW_CLOUD))
        canvas.drawCircle(9f, 8f, 6f, fill(SNOW_CLOUD_LIGHT))
        canvas.drawCircle(18f, 7.5f, 4.8f, fill(SNOW_CLOUD))
        val flake = stroke(SNOW_FLAKE, 1.4f)
        val centers = arrayOf(8f to 21f, 15f to 24f, 22f to 20.5f)
        centers.forEach { (x, y) ->
            canvas.drawLine(x - 2f, y, x + 2f, y, flake)
            canvas.drawLine(x, y - 2f, x, y + 2f, flake)
            canvas.drawLine(x - 1.4f, y - 1.4f, x + 1.4f, y + 1.4f, flake)
            canvas.drawLine(x - 1.4f, y + 1.4f, x + 1.4f, y - 1.4f, flake)
        }
    }

    private fun drawStorm(canvas: Canvas) {
        canvas.drawRoundRect(RectF(4f, 8f, 26f, 16f), 4f, 4f, fill(STORM_CLOUD))
        canvas.drawCircle(9f, 8f, 6.5f, fill(STORM_LIGHT))
        canvas.drawCircle(17f, 7f, 5f, fill(STORM_CLOUD))
        canvas.drawCircle(23f, 10f, 4f, fill(STORM_DARK))
        val bolt = Path().apply {
            moveTo(15f, 14f)
            lineTo(10f, 21f)
            lineTo(14f, 21f)
            lineTo(11.5f, 28f)
            lineTo(20f, 18.5f)
            lineTo(15.5f, 18.5f)
            close()
        }
        canvas.drawPath(bolt, fill(LIGHTNING_YELLOW))
    }

    private fun drawMoon(canvas: Canvas) {
        canvas.drawCircle(14f, 14f, 11f, fill(MOON_GOLD))
        canvas.drawCircle(17f, 9f, 8f, fill(NIGHT_CUTOUT))
        canvas.drawCircle(21f, 5f, 2f, fill(MOON_GOLD))
    }

    private fun drawCloudMoon(canvas: Canvas) {
        canvas.drawCircle(9f, 8f, 6f, fill(MOON_GOLD))
        canvas.drawCircle(11f, 6f, 4.5f, fill(NIGHT_CUTOUT))
        drawCloud(canvas, CLOUD_WHITE, CLOUD_BLUE, CLOUD_SHADE, xOffset = 0f, yOffset = 1f)
    }

    internal const val SUN_GOLD: Int = 0xFFFFD700.toInt()
    internal const val SUNSET_ORANGE: Int = 0xFFFF6B35.toInt()
    internal const val NOW_LABEL_GOLD: Int = 0xFFFFB020.toInt()
    internal const val RAIN_BLUE_LIGHT: Int = 0xFF5BC8FF.toInt()
    internal const val RAIN_BLUE: Int = 0xFF3AADFF.toInt()
    internal const val LIGHTNING_YELLOW: Int = 0xFFFFE000.toInt()

    private const val CLOUD_WHITE: Int = 0xFFFFFFFF.toInt()
    private const val CLOUD_BLUE: Int = 0xFFD0E8FF.toInt()
    private const val CLOUD_SHADE: Int = 0xFFB7CADF.toInt()
    private const val RAIN_CLOUD_LIGHT: Int = 0xFFE8F4FF.toInt()
    private const val RAIN_CLOUD: Int = 0xFFD0E8FF.toInt()
    private const val SNOW_CLOUD_LIGHT: Int = 0xFFF4F8FC.toInt()
    private const val SNOW_CLOUD: Int = 0xFFDCE8F2.toInt()
    private const val SNOW_FLAKE: Int = 0xFFFFFFFF.toInt()
    private const val STORM_LIGHT: Int = 0xFF8FA0B8.toInt()
    private const val STORM_CLOUD: Int = 0xFF7B8EA8.toInt()
    private const val STORM_DARK: Int = 0xFF6B7E98.toInt()
    private const val MOON_GOLD: Int = 0xFFFFE87A.toInt()
    private const val NIGHT_CUTOUT: Int = 0xFF1A2240.toInt()
}
