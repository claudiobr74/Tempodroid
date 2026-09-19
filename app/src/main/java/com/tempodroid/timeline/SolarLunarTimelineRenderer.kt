package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import com.tempodroid.sky.DayPeriod
import com.tempodroid.sky.DayPeriodCalculator
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Timeline canônica da seção 09 · Refined Widget Previews v2. */
object SolarLunarTimelineRenderer {

    internal data class NowMarkerLayout(
        val markerX: Float,
        val markerY: Float,
        val bubbleCenterX: Float,
        val bubbleTop: Float,
        val bubbleWidth: Float,
        val bubbleHeight: Float,
        val bubbleCenterY: Float = bubbleTop + bubbleHeight / 2f,
    )

    /**
     * Proporções lidas do viewport de 80 px da seção 09:
     * anel 22 px, disco 16 px, hora 13 px, legenda 9 px,
     * badge Agora 44 px, ícone 28 px, hora Agora 11 px e legenda 10 px.
     */
    internal data class TimelineVisualMetrics(
        val sunriseOuterDiameterFraction: Float = 22f / 80f,
        val sunriseInnerDiameterFraction: Float = 16f / 80f,
        val anchorTimeTextFraction: Float = 13f / 80f,
        val anchorLabelTextFraction: Float = 9f / 80f,
        val nowBadgeDiameterFraction: Float = 44f / 80f,
        val nowIconFractionOfBadge: Float = 28f / 44f,
        val nowTimeTextFraction: Float = 11f / 80f,
        val nowLabelTextFraction: Float = 10f / 80f,
    )

    internal val visualMetrics = TimelineVisualMetrics()

    fun render(
        widthPx: Int,
        heightPx: Int,
        state: SolarWeatherWidgetState,
        now: LocalTime,
        compact: Boolean,
        @Suppress("UNUSED_PARAMETER") moonSurface: Bitmap? = null,
        @Suppress("UNUSED_PARAMETER") panelDarknessAlpha: Int = 64,
    ): Bitmap {
        val w = widthPx.coerceAtLeast(120)
        val h = heightPx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val left = w * 0.035f
        val right = w * 0.965f

        val sunrise = state.sunrise ?: LocalTime.of(6, 10)
        val sunset = state.sunset ?: LocalTime.of(18, 5)
        val period = DayPeriodCalculator.from(now, sunrise, sunset, state.solarNoon)

        val sunrisePosition = visualPosition(sunrise)
        val sunsetPosition = visualPosition(sunset)
        val nowPosition = visualPosition(now)
        val sunriseX = xFor(sunrisePosition, left, right)
        val sunsetX = xFor(sunsetPosition, left, right)
        val nowX = xFor(nowPosition, left, right)

        drawSolarArc(
            canvas = canvas,
            left = left,
            right = right,
            h = h,
            sunrise = sunrise,
            sunset = sunset,
            now = now,
            condition = state.condition,
            period = period,
        )

        drawSunAnchor(
            canvas = canvas,
            x = sunriseX,
            y = yForTime(sunrise, sunrise, sunset, h),
            time = TimelineCalculator.formatClock(sunrise),
            label = "Nascer",
            sunrise = true,
            h = h,
            compact = compact,
        )
        drawSunAnchor(
            canvas = canvas,
            x = sunsetX,
            y = yForTime(sunset, sunrise, sunset, h),
            time = TimelineCalculator.formatClock(sunset),
            label = "Ocaso",
            sunrise = false,
            h = h,
            compact = compact,
        )

        drawNowMarker(
            canvas = canvas,
            left = left,
            right = right,
            h = h,
            position = nowPosition,
            markerX = nowX,
            markerY = yForTime(now, sunrise, sunset, h),
            label = TimelineCalculator.formatClock(now),
            condition = state.condition,
            period = period,
            compact = compact,
            sunriseX = sunriseX,
            sunsetX = sunsetX,
            now = now,
            sunrise = sunrise,
            sunset = sunset,
        )
        return bitmap
    }

    /**
     * A seção 09 usa quatro segmentos visuais, não uma barra de progresso:
     * tracejado pré-nascer, metade ascendente dourada, metade descendente e
     * tracejado pós-ocaso. A metade direita muda conforme o cenário canônico.
     */
    private fun drawSolarArc(
        canvas: Canvas,
        left: Float,
        right: Float,
        h: Int,
        sunrise: LocalTime,
        sunset: LocalTime,
        now: LocalTime,
        condition: WeatherCondition,
        period: DayPeriod,
    ) {
        val sunriseMinutes = minuteOf(sunrise)
        val sunsetMinutes = minuteOf(sunset)
        val midpointMinutes = ((sunriseMinutes + sunsetMinutes) / 2).coerceIn(sunriseMinutes, sunsetMinutes)
        val adverse = condition == WeatherCondition.RAIN ||
            condition == WeatherCondition.HEAVY_RAIN ||
            condition == WeatherCondition.STORM
        val nightOrAdverse = period == DayPeriod.NIGHT || adverse
        val dashAlpha = if (nightOrAdverse) 0x33 else 0x1F

        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (0x00FFFFFF or (dashAlpha shl 24))
            style = Paint.Style.STROKE
            strokeWidth = max(2f, h * 0.025f)
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(
                if (nightOrAdverse) floatArrayOf(h * 0.042f, h * 0.056f)
                else floatArrayOf(h * 0.038f, h * 0.038f),
                0f,
            )
        }
        drawClockSegment(
            canvas,
            left,
            right,
            h,
            WINDOW_START_MINUTES,
            sunriseMinutes,
            sunrise,
            sunset,
            dashPaint,
        )
        drawClockSegment(
            canvas,
            left,
            right,
            h,
            sunsetMinutes,
            WINDOW_END_MINUTES,
            sunrise,
            sunset,
            dashPaint,
        )

        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TimelineWeatherMarkerRenderer.SUN_GOLD
            style = Paint.Style.STROKE
            strokeWidth = max(2.5f, h * 0.03125f)
            strokeCap = Paint.Cap.ROUND
        }
        val minutesUntilSunset = sunsetMinutes - minuteOf(now)
        val rightColor = when {
            nightOrAdverse -> TimelineWeatherMarkerRenderer.SUN_GOLD
            condition == WeatherCondition.CLEAR && minutesUntilSunset in 0..12 ->
                (TimelineWeatherMarkerRenderer.SUNSET_ORANGE and 0x00FFFFFF) or 0x66000000
            else -> 0x3DFFFFFF
        }
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rightColor
            style = Paint.Style.STROKE
            strokeWidth = max(2.5f, h * 0.03125f)
            strokeCap = Paint.Cap.ROUND
        }
        drawClockSegment(canvas, left, right, h, sunriseMinutes, midpointMinutes, sunrise, sunset, leftPaint)
        drawClockSegment(canvas, left, right, h, midpointMinutes, sunsetMinutes, sunrise, sunset, rightPaint)
    }

    private fun drawClockSegment(
        canvas: Canvas,
        left: Float,
        right: Float,
        h: Int,
        startMinutes: Int,
        endMinutes: Int,
        sunrise: LocalTime,
        sunset: LocalTime,
        paint: Paint,
    ) {
        if (endMinutes <= startMinutes) return
        val path = Path()
        val steps = max(8, (endMinutes - startMinutes) / 20)
        for (step in 0..steps) {
            val fraction = step / steps.toFloat()
            val minute = (startMinutes + (endMinutes - startMinutes) * fraction).toInt().coerceIn(0, 1439)
            val time = LocalTime.of(minute / 60, minute % 60)
            val x = xForMinute(minute, left, right)
            val y = yForTime(time, sunrise, sunset, h)
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSunAnchor(
        canvas: Canvas,
        x: Float,
        y: Float,
        time: String,
        label: String,
        sunrise: Boolean,
        h: Int,
        compact: Boolean,
    ) {
        val metrics = visualMetrics
        val outerRadius = h * metrics.sunriseOuterDiameterFraction / 2f
        val innerRadius = h * metrics.sunriseInnerDiameterFraction / 2f
        val color = if (sunrise) TimelineWeatherMarkerRenderer.SUN_GOLD else TimelineWeatherMarkerRenderer.SUNSET_ORANGE
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = (color and 0x00FFFFFF) or 0x40000000
            style = Paint.Style.FILL
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, outerRadius, haloPaint)
        canvas.drawCircle(x, y, innerRadius, dotPaint)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.18f else metrics.anchorTimeTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.012f, 0f, h * 0.008f, 0x99000000.toInt())
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFF94A3B8.toInt()
            textSize = h * if (compact) 0.12f else metrics.anchorLabelTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.01f, 0f, h * 0.006f, 0x88000000.toInt())
        }
        val timeBaseline = y + outerRadius + timePaint.textSize * 1.05f
        val labelBaseline = min(h - h * 0.02f, timeBaseline + labelPaint.textSize * 1.18f)
        canvas.drawText(time, x, timeBaseline, timePaint)
        canvas.drawText(label, x, labelBaseline, labelPaint)
    }

    private fun drawNowMarker(
        canvas: Canvas,
        left: Float,
        right: Float,
        h: Int,
        position: Float,
        markerX: Float,
        markerY: Float,
        label: String,
        condition: WeatherCondition,
        period: DayPeriod,
        compact: Boolean,
        sunriseX: Float,
        sunsetX: Float,
        now: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
    ) {
        val metrics = visualMetrics
        val diameter = h * if (compact) 0.50f else metrics.nowBadgeDiameterFraction
        val layoutBase = calculateNowMarkerLayout(
            left = left,
            right = right,
            h = h,
            position = position,
            bubbleWidth = diameter,
            bubbleHeight = diameter,
            arcCy = h * 0.91f,
            arcRise = h * 1.06f,
            markerYOverride = markerY,
        )
        val adjustedX = collisionAdjustedCenter(
            desiredCenterX = layoutBase.bubbleCenterX,
            markerX = markerX,
            sunriseX = sunriseX,
            sunsetX = sunsetX,
            diameter = diameter,
            left = left,
            right = right,
            now = now,
            sunrise = sunrise,
            sunset = sunset,
        )
        val centerY = canonicalNowCenterY(now, sunrise, sunset, h)
        val radius = diameter / 2f

        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1AFFFFFF
            style = Paint.Style.FILL
        }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x4DFFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.0125f)
        }
        canvas.drawCircle(adjustedX, centerY, radius, badgeFill)
        canvas.drawCircle(adjustedX, centerY, radius, badgeStroke)

        TimelineWeatherMarkerRenderer.draw(
            canvas = canvas,
            cx = adjustedX,
            cy = centerY,
            sizePx = diameter * metrics.nowIconFractionOfBadge,
            condition = condition,
            period = period,
        )

        val nowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (period == DayPeriod.NIGHT) 0xFF94A3B8.toInt() else TimelineWeatherMarkerRenderer.NOW_LABEL_GOLD
            textSize = h * if (compact) 0.12f else metrics.nowLabelTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.01f, 0f, h * 0.006f, 0x99000000.toInt())
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.14f else metrics.nowTimeTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.01f, 0f, h * 0.006f, 0x99000000.toInt())
        }
        val labelBaseline = max(nowLabelPaint.textSize, centerY - radius - h * 0.05f)
        val timeBaseline = min(h - h * 0.015f, centerY + radius + timePaint.textSize * 1.05f)
        canvas.drawText("Agora", adjustedX, labelBaseline, nowLabelPaint)
        canvas.drawText(label, adjustedX, timeBaseline, timePaint)
    }

    internal fun calculateNowMarkerLayout(
        left: Float,
        right: Float,
        h: Int,
        position: Float,
        bubbleWidth: Float,
        bubbleHeight: Float,
        arcCy: Float,
        arcRise: Float,
        markerYOverride: Float? = null,
    ): NowMarkerLayout {
        val p = position.coerceIn(0f, 1f)
        val markerX = xFor(p, left, right)
        val markerY = markerYOverride ?: yOnArc(p, arcCy, arcRise)
        val safeMargin = h * 0.03f
        val minCenter = left + bubbleWidth / 2f + safeMargin
        val maxCenter = right - bubbleWidth / 2f - safeMargin
        val bubbleCenterX = if (minCenter <= maxCenter) markerX.coerceIn(minCenter, maxCenter) else (left + right) / 2f
        val radius = bubbleHeight / 2f
        val bubbleCenterY = markerY.coerceIn(h * 0.31f, h - radius - h * 0.05f)
        return NowMarkerLayout(
            markerX = markerX,
            markerY = markerY,
            bubbleCenterX = bubbleCenterX,
            bubbleTop = bubbleCenterY - radius,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            bubbleCenterY = bubbleCenterY,
        )
    }

    /**
     * Ajustes de colisão observados no Figma:
     * 17:56 desloca o badge para a esquerda do Ocaso; 18:05, para a direita.
     * Depois do pôr do sol a separação vertical já resolve a colisão e o X
     * volta a representar a posição temporal.
     */
    private fun collisionAdjustedCenter(
        desiredCenterX: Float,
        @Suppress("UNUSED_PARAMETER") markerX: Float,
        sunriseX: Float,
        sunsetX: Float,
        diameter: Float,
        left: Float,
        right: Float,
        now: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
    ): Float {
        var result = desiredCenterX
        val nowMinutes = minuteOf(now)
        val riseMinutes = minuteOf(sunrise)
        val setMinutes = minuteOf(sunset)
        val minutesUntilSunset = setMinutes - nowMinutes
        val minutesFromSunrise = nowMinutes - riseMinutes

        if (minutesFromSunrise in -10..10 && abs(result - sunriseX) < diameter * 1.2f) {
            result = sunriseX + diameter * 0.9f
        }
        if (minutesUntilSunset in 11..25) {
            result = sunsetX - diameter * 1.24f
        } else if (minutesUntilSunset in 1..10) {
            result = sunsetX + diameter * 0.71f
        }

        val radius = diameter / 2f
        val minCenter = left + radius + diameter * 0.08f
        val maxCenter = right - radius - diameter * 0.08f
        return result.coerceIn(minCenter, maxCenter)
    }

    private fun canonicalNowCenterY(now: LocalTime, sunrise: LocalTime, sunset: LocalTime, h: Int): Float {
        val nowMinutes = minuteOf(now)
        val riseMinutes = minuteOf(sunrise)
        val setMinutes = minuteOf(sunset)
        if (nowMinutes < riseMinutes) return h * 0.31f

        val untilSunset = setMinutes - nowMinutes
        if (untilSunset in 11..25) return h * 0.36f
        if (untilSunset in 1..10) return h * 0.60f
        if (nowMinutes < setMinutes) return h * 0.31f

        val afterSunset = (nowMinutes - setMinutes).coerceAtLeast(0)
        val nightFraction = (afterSunset / 257f).coerceIn(0f, 1f)
        return h * (0.56f + 0.27f * nightFraction)
    }

    /**
     * Projeção temporal art-directed da seção 09. Ela continua monotônica e
     * baseada no relógio, mas usa os checkpoints visuais dos previews para
     * reservar espaço para os três marcadores em um widget 4×2.
     */
    internal fun visualPosition(time: LocalTime): Float {
        val minute = minuteOf(time)
        val anchors = POSITION_ANCHORS
        if (minute <= anchors.first().first) return anchors.first().second
        if (minute >= anchors.last().first) return anchors.last().second
        for (index in 0 until anchors.lastIndex) {
            val (m0, p0) = anchors[index]
            val (m1, p1) = anchors[index + 1]
            if (minute in m0..m1) {
                val t = (minute - m0) / (m1 - m0).toFloat()
                return p0 + (p1 - p0) * t
            }
        }
        return 0f
    }

    private fun xForMinute(minute: Int, left: Float, right: Float): Float {
        val safeMinute = minute.coerceIn(0, 1439)
        return xFor(visualPosition(LocalTime.of(safeMinute / 60, safeMinute % 60)), left, right)
    }

    private fun xFor(position: Float, left: Float, right: Float): Float =
        left + position.coerceIn(0f, 1f) * (right - left)

    private fun yForTime(time: LocalTime, sunrise: LocalTime, sunset: LocalTime, h: Int): Float {
        val minute = minuteOf(time)
        val sunriseMinute = minuteOf(sunrise)
        val sunsetMinute = minuteOf(sunset)
        val anchor = h * 0.27f
        val rise = h * 0.22f
        val nightDrop = h * 0.56f

        return when {
            minute in sunriseMinute..sunsetMinute -> {
                val duration = max(1, sunsetMinute - sunriseMinute)
                val phase = ((minute - sunriseMinute) / duration.toFloat()).coerceIn(0f, 1f)
                anchor - rise * sin(Math.PI * phase).toFloat()
            }
            minute > sunsetMinute -> {
                val phase = ((minute - sunsetMinute) / 257f).coerceIn(0f, 1f)
                anchor + nightDrop * phase
            }
            else -> {
                val phase = ((sunriseMinute - minute) / 270f).coerceIn(0f, 1f)
                anchor + nightDrop * phase
            }
        }
    }

    private fun yOnArc(position: Float, cy: Float, rise: Float): Float {
        val t = position.coerceIn(0f, 1f)
        return cy - 3.2f * rise * t * (1f - t)
    }

    private fun minuteOf(time: LocalTime): Int = time.hour * 60 + time.minute

    fun measureText(paint: Paint, value: String): Int {
        val bounds = Rect()
        paint.getTextBounds(value, 0, value.length, bounds)
        return bounds.width()
    }

    private val POSITION_ANCHORS = listOf(
        3 * 60 + 45 to 0.00f,
        6 * 60 + 11 to 0.133f,
        10 * 60 + 30 to 0.351f,
        15 * 60 + 30 to 0.581f,
        18 * 60 + 13 to 0.788f,
        19 * 60 to 0.819f,
        22 * 60 + 30 to 0.905f,
        23 * 60 + 59 to 0.965f,
    )

    private const val WINDOW_START_MINUTES = 3 * 60 + 45
    private const val WINDOW_END_MINUTES = 23 * 60 + 59
}
