package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import com.tempodroid.sky.DayPeriod
import com.tempodroid.sky.DayPeriodCalculator
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

    internal data class TimelineVisualMetrics(
        val sunriseOuterDiameterFraction: Float = 0.28f,
        val sunriseInnerDiameterFraction: Float = 0.20f,
        val anchorTimeTextFraction: Float = 0.26f,
        val anchorLabelTextFraction: Float = 0.16f,
        val nowBadgeDiameterFraction: Float = 0.48f,
        val nowIconFractionOfBadge: Float = 0.64f,
        val nowTimeTextFraction: Float = 0.20f,
        val nowLabelTextFraction: Float = 0.14f,
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

        val sunriseX = xFor(visualPosition(sunrise), left, right)
        val sunsetX = xFor(visualPosition(sunset), left, right)
        val nowPosition = visualPosition(now)
        val nowX = xFor(nowPosition, left, right)

        drawSolarArc(
            canvas = canvas,
            left = left,
            right = right,
            h = h,
            sunrise = sunrise,
            sunset = sunset,
            now = now,
        )

        drawSunAnchor(
            canvas = canvas,
            x = sunriseX,
            y = yForTime(sunrise, sunrise, sunset, h),
            time = TimelineCalculator.formatClock(sunrise),
            label = "NASCER",
            sunrise = true,
            h = h,
            compact = compact,
        )
        drawSunAnchor(
            canvas = canvas,
            x = sunsetX,
            y = yForTime(sunset, sunrise, sunset, h),
            time = TimelineCalculator.formatClock(sunset),
            label = "OCASO",
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

    private fun drawSolarArc(
        canvas: Canvas,
        left: Float,
        right: Float,
        h: Int,
        sunrise: LocalTime,
        sunset: LocalTime,
        now: LocalTime,
    ) {
        val startMinutes = WINDOW_START_MINUTES
        val sunriseMinutes = minuteOf(sunrise).coerceIn(startMinutes, WINDOW_END_MINUTES)
        val sunsetMinutes = minuteOf(sunset).coerceIn(sunriseMinutes + 1, WINDOW_END_MINUTES)
        val nowMinutes = minuteOf(now)

        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1FFFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(2f, h * 0.025f)
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(h * 0.07f, h * 0.055f), 0f)
        }
        drawClockSegment(canvas, left, right, h, startMinutes, sunriseMinutes, sunrise, sunset, dashPaint)
        drawClockSegment(canvas, left, right, h, sunsetMinutes, WINDOW_END_MINUTES, sunrise, sunset, dashPaint)

        val sunriseX = xForMinute(sunriseMinutes, left, right)
        val sunsetX = xForMinute(sunsetMinutes, left, right)
        val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(2.5f, h * 0.032f)
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                sunriseX,
                0f,
                sunsetX,
                0f,
                intArrayOf(
                    TimelineWeatherMarkerRenderer.SUN_GOLD,
                    0xFFFFC857.toInt(),
                    TimelineWeatherMarkerRenderer.SUNSET_ORANGE,
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        val futurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x3DFFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(2.5f, h * 0.032f)
            strokeCap = Paint.Cap.ROUND
        }

        when {
            nowMinutes < sunriseMinutes -> {
                drawClockSegment(canvas, left, right, h, sunriseMinutes, sunsetMinutes, sunrise, sunset, futurePaint)
            }
            nowMinutes in sunriseMinutes..sunsetMinutes -> {
                drawClockSegment(canvas, left, right, h, sunriseMinutes, nowMinutes, sunrise, sunset, gradientPaint)
                drawClockSegment(canvas, left, right, h, nowMinutes, sunsetMinutes, sunrise, sunset, futurePaint)
            }
            else -> {
                drawClockSegment(canvas, left, right, h, sunriseMinutes, sunsetMinutes, sunrise, sunset, gradientPaint)
            }
        }
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
        val steps = max(8, ((endMinutes - startMinutes) / 20))
        for (step in 0..steps) {
            val fraction = step / steps.toFloat()
            val minute = (startMinutes + (endMinutes - startMinutes) * fraction).toInt()
            val time = LocalTime.of((minute / 60).coerceIn(0, 23), minute % 60)
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
        val color = if (sunrise) {
            TimelineWeatherMarkerRenderer.SUN_GOLD
        } else {
            TimelineWeatherMarkerRenderer.SUNSET_ORANGE
        }
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = (color and 0x00FFFFFF) or 0x40000000
            style = Paint.Style.FILL
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            setShadowLayer(h * 0.025f, 0f, h * 0.01f, (color and 0x00FFFFFF) or 0x66000000)
        }
        canvas.drawCircle(x, y, outerRadius, haloPaint)
        canvas.drawCircle(x, y, innerRadius, dotPaint)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.235f else metrics.anchorTimeTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.018f, 0f, h * 0.012f, 0x99000000.toInt())
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            textSize = h * if (compact) 0.14f else metrics.anchorLabelTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.014f, 0f, h * 0.01f, 0x88000000.toInt())
        }
        val timeBaseline = y + outerRadius + timePaint.textSize * 1.05f
        val labelBaseline = min(h - h * 0.025f, timeBaseline + labelPaint.textSize * 1.15f)
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
        condition: com.tempodroid.weather.WeatherCondition,
        period: DayPeriod,
        compact: Boolean,
        sunriseX: Float,
        sunsetX: Float,
        now: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
    ) {
        val metrics = visualMetrics
        val diameter = h * if (compact) 0.44f else metrics.nowBadgeDiameterFraction
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
        val centerY = layoutBase.bubbleCenterY
        val radius = diameter / 2f

        if (abs(adjustedX - markerX) > radius * 0.45f) {
            val link = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x38FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = max(1f, h * 0.012f)
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(markerX, markerY, adjustedX, centerY, link)
            canvas.drawCircle(markerX, markerY, max(1.8f, h * 0.018f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x99FFFFFF.toInt()
            })
        }

        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1AFFFFFF
            style = Paint.Style.FILL
            setShadowLayer(h * 0.035f, 0f, h * 0.018f, 0x55000000)
        }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x4DFFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.012f)
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
            textSize = h * if (compact) 0.125f else metrics.nowLabelTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.012f, 0f, h * 0.008f, 0x99000000.toInt())
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.18f else metrics.nowTimeTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.014f, 0f, h * 0.01f, 0x99000000.toInt())
        }
        val labelBaseline = max(nowLabelPaint.textSize, centerY - radius - h * 0.045f)
        val timeBaseline = min(h - h * 0.02f, centerY + radius + timePaint.textSize * 1.05f)
        canvas.drawText("AGORA", adjustedX, labelBaseline, nowLabelPaint)
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
        val bubbleCenterY = markerY.coerceIn(h * 0.31f, h - radius - h * 0.22f)
        val bubbleTop = bubbleCenterY - radius

        return NowMarkerLayout(
            markerX = markerX,
            markerY = markerY,
            bubbleCenterX = bubbleCenterX,
            bubbleTop = bubbleTop,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            bubbleCenterY = bubbleCenterY,
        )
    }

    private fun collisionAdjustedCenter(
        desiredCenterX: Float,
        markerX: Float,
        sunriseX: Float,
        sunsetX: Float,
        diameter: Float,
        left: Float,
        right: Float,
        now: LocalTime,
        sunrise: LocalTime,
        sunset: LocalTime,
    ): Float {
        val radius = diameter / 2f
        val minDistance = diameter * 0.95f
        var result = desiredCenterX

        if (abs(result - sunriseX) < minDistance) {
            result = if (minuteOf(now) >= minuteOf(sunrise)) sunriseX + minDistance else sunriseX - minDistance
        }
        if (abs(result - sunsetX) < minDistance) {
            val minutesUntilSunset = minuteOf(sunset) - minuteOf(now)
            result = if (minutesUntilSunset in 1..10 || minuteOf(now) >= minuteOf(sunset)) {
                sunsetX + minDistance
            } else {
                sunsetX - minDistance
            }
        }

        // markerX remains the exact chronological point; only the visual badge may move.
        val minCenter = left + radius + diameter * 0.08f
        val maxCenter = right - radius - diameter * 0.08f
        return result.coerceIn(minCenter, maxCenter).also { _ -> markerX }
    }

    /**
     * The v2 Figma timeline intentionally omits midnight/noon endpoints and uses
     * a visual window from 03:45 to 22:15. Times remain chronological inside
     * that window; values beyond it clamp to the safe edges.
     */
    internal fun visualPosition(time: LocalTime): Float {
        val minute = minuteOf(time)
        return ((minute - WINDOW_START_MINUTES) / (WINDOW_END_MINUTES - WINDOW_START_MINUTES).toFloat())
            .coerceIn(0f, 1f)
    }

    private fun xForMinute(minute: Int, left: Float, right: Float): Float {
        val p = ((minute - WINDOW_START_MINUTES) / (WINDOW_END_MINUTES - WINDOW_START_MINUTES).toFloat())
            .coerceIn(0f, 1f)
        return xFor(p, left, right)
    }

    private fun xFor(position: Float, left: Float, right: Float): Float =
        left + position.coerceIn(0f, 1f) * (right - left)

    private fun yForTime(time: LocalTime, sunrise: LocalTime, sunset: LocalTime, h: Int): Float {
        val minute = minuteOf(time)
        val sunriseMinute = minuteOf(sunrise)
        val sunsetMinute = minuteOf(sunset)
        val anchor = h * 0.27f
        val rise = h * 0.22f
        val nightDrop = h * 0.53f

        return when {
            minute in sunriseMinute..sunsetMinute -> {
                val duration = max(1, sunsetMinute - sunriseMinute)
                val phase = ((minute - sunriseMinute) / duration.toFloat()).coerceIn(0f, 1f)
                anchor - rise * sin(Math.PI * phase).toFloat()
            }
            minute > sunsetMinute -> {
                val phase = ((minute - sunsetMinute) / 300f).coerceIn(0f, 1f)
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

    private const val WINDOW_START_MINUTES = 3 * 60 + 45
    private const val WINDOW_END_MINUTES = 22 * 60 + 15
}
