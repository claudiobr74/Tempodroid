package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.tempodroid.sky.DayPeriod
import com.tempodroid.sky.DayPeriodCalculator
import com.tempodroid.sky.TextProtectionOverlay
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Timeline híbrida premium.
 *
 * Recupera a profundidade visual da timeline anterior (glass + arco multicolorido),
 * mantendo a informação simplificada aprovada: apenas Nascer, Agora e Ocaso.
 */
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
        val sunriseOuterDiameterFraction: Float = 28f / 80f,
        val sunriseInnerDiameterFraction: Float = 16f / 80f,
        val anchorTimeTextFraction: Float = 15f / 80f,
        val anchorLabelTextFraction: Float = 8f / 80f,
        val nowBadgeDiameterFraction: Float = 42f / 80f,
        val nowIconFractionOfBadge: Float = 26f / 42f,
        val nowTimeTextFraction: Float = 11f / 80f,
        val nowLabelTextFraction: Float = 9f / 80f,
    )

    internal val visualMetrics = TimelineVisualMetrics()

    fun render(
        widthPx: Int,
        heightPx: Int,
        state: SolarWeatherWidgetState,
        now: LocalTime,
        compact: Boolean,
        @Suppress("UNUSED_PARAMETER") moonSurface: Bitmap? = null,
        panelDarknessAlpha: Int = 64,
    ): Bitmap {
        val w = widthPx.coerceAtLeast(120)
        val h = heightPx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val left = w * 0.045f
        val right = w * 0.955f

        drawGlassPanel(canvas, w, h, panelDarknessAlpha)

        val sunrise = state.sunrise ?: LocalTime.of(6, 10)
        val sunset = state.sunset ?: LocalTime.of(18, 5)
        val period = DayPeriodCalculator.from(now, sunrise, sunset, state.solarNoon)

        val sunrisePosition = visualPosition(sunrise)
        val sunsetPosition = visualPosition(sunset)
        val nowPosition = visualPosition(now)
        val sunriseX = xFor(sunrisePosition, left, right)
        val sunsetX = xFor(sunsetPosition, left, right)
        val nowX = xFor(nowPosition, left, right)

        val arcCy = h * 0.58f
        val arcRise = h * 0.42f
        drawPremiumArc(
            canvas = canvas,
            left = left,
            right = right,
            cy = arcCy,
            rise = arcRise,
            h = h,
            period = period,
            condition = state.condition,
        )

        drawSunAnchor(
            canvas = canvas,
            x = sunriseX,
            y = yOnArc(sunrisePosition, arcCy, arcRise),
            time = TimelineCalculator.formatClock(sunrise),
            label = "Nascer",
            sunrise = true,
            h = h,
            compact = compact,
        )
        drawSunAnchor(
            canvas = canvas,
            x = sunsetX,
            y = yOnArc(sunsetPosition, arcCy, arcRise),
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
            markerY = yOnArc(nowPosition, arcCy, arcRise),
            label = TimelineCalculator.formatClock(now),
            condition = state.condition,
            period = period,
            compact = compact,
            sunriseX = sunriseX,
            sunsetX = sunsetX,
            now = now,
            sunrise = sunrise,
            sunset = sunset,
            arcCy = arcCy,
            arcRise = arcRise,
        )
        return bitmap
    }

    private fun drawGlassPanel(canvas: Canvas, w: Int, h: Int, panelDarknessAlpha: Int) {
        val darkness = panelDarknessAlpha.coerceIn(50, 88)
        TextProtectionOverlay.glassPanel(
            canvas = canvas,
            left = 0f,
            top = 0f,
            right = w.toFloat(),
            bottom = h.toFloat(),
            radius = h * 0.27f,
            darknessAlpha = darkness,
        )

        val radius = h * 0.27f
        val inset = max(1f, h * 0.012f)
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x32FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.010f)
        }
        canvas.drawRoundRect(
            RectF(inset, inset, w - inset, h - inset),
            radius,
            radius,
            border,
        )

        val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x20FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.008f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(w * 0.10f, h * 0.075f, w * 0.90f, h * 0.075f, highlight)
    }

    private fun drawPremiumArc(
        canvas: Canvas,
        left: Float,
        right: Float,
        cy: Float,
        rise: Float,
        h: Int,
        period: DayPeriod,
        condition: WeatherCondition,
    ) {
        val path = Path().apply {
            moveTo(left, cy)
            quadTo((left + right) / 2f, cy - rise * 1.6f, right, cy)
        }

        val adverse = condition == WeatherCondition.RAIN ||
            condition == WeatherCondition.HEAVY_RAIN ||
            condition == WeatherCondition.STORM
        val alpha = when {
            period == DayPeriod.NIGHT -> 205
            adverse -> 220
            else -> 255
        }

        val gradientColors = intArrayOf(
            withAlpha(0xFF5668D8.toInt(), (alpha * 0.78f).toInt()),
            withAlpha(0xFFFFC84D.toInt(), alpha),
            withAlpha(0xFFFFE66A.toInt(), alpha),
            withAlpha(0xFFFF6B3D.toInt(), alpha),
            withAlpha(0xFF7A56B6.toInt(), (alpha * 0.82f).toInt()),
        )
        val stops = floatArrayOf(0f, 0.18f, 0.50f, 0.79f, 1f)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(5f, h * 0.080f)
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                left,
                cy,
                right,
                cy,
                gradientColors.map { withAlpha(it, 48) }.toIntArray(),
                stops,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(path, glow)

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(2.4f, h * 0.032f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            shader = LinearGradient(
                left,
                cy,
                right,
                cy,
                gradientColors,
                stops,
                Shader.TileMode.CLAMP,
            )
            setShadowLayer(h * 0.018f, 0f, h * 0.012f, 0x66000000)
        }
        canvas.drawPath(path, line)
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
        val iconRadius = h * metrics.sunriseOuterDiameterFraction / 2f
        val coreRadius = h * metrics.sunriseInnerDiameterFraction / 2f
        val color = if (sunrise) TimelineWeatherMarkerRenderer.SUN_GOLD else TimelineWeatherMarkerRenderer.SUNSET_ORANGE

        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = withAlpha(color, 54)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, iconRadius * 0.82f, halo)

        val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = max(1.3f, h * 0.014f)
            strokeCap = Paint.Cap.ROUND
            setShadowLayer(h * 0.014f, 0f, h * 0.008f, withAlpha(color, 100))
        }
        val rayInner = coreRadius * 1.30f
        val rayOuter = iconRadius * 0.92f
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0 - 90.0)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            canvas.drawLine(
                x + dx * rayInner,
                y + dy * rayInner,
                x + dx * rayOuter,
                y + dy * rayOuter,
                rayPaint,
            )
        }

        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            setShadowLayer(h * 0.018f, 0f, h * 0.010f, withAlpha(color, 120))
        }
        canvas.drawCircle(x, y, coreRadius, core)

        val centerHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (sunrise) 0x66FFFFFF else 0x44FFD9B3
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x - coreRadius * 0.25f, y - coreRadius * 0.28f, coreRadius * 0.24f, centerHighlight)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.17f else metrics.anchorTimeTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.014f, 0f, h * 0.010f, 0xA0000000.toInt())
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (sunrise) 0xFFE8C867.toInt() else 0xFFFFA06B.toInt()
            textSize = h * if (compact) 0.095f else metrics.anchorLabelTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.010f, 0f, h * 0.006f, 0x99000000.toInt())
        }

        val timeBaseline = min(h * 0.82f, y + iconRadius * 0.86f + timePaint.textSize)
        val labelBaseline = min(h * 0.965f, timeBaseline + labelPaint.textSize * 1.18f)
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
        arcCy: Float,
        arcRise: Float,
    ) {
        val metrics = visualMetrics
        val diameter = h * if (compact) 0.47f else metrics.nowBadgeDiameterFraction
        val layoutBase = calculateNowMarkerLayout(
            left = left,
            right = right,
            h = h,
            position = position,
            bubbleWidth = diameter,
            bubbleHeight = diameter,
            arcCy = arcCy,
            arcRise = arcRise,
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
        val radius = diameter / 2f
        val desiredCenterY = markerY - radius * 0.28f
        val centerY = desiredCenterY.coerceIn(
            radius + h * 0.13f,
            h - radius - h * 0.17f,
        )
        val accent = TimelineWeatherMarkerRenderer.bubbleAccent(period)

        val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(accent, 190)
            style = Paint.Style.STROKE
            strokeWidth = max(1.2f, h * 0.013f)
            strokeCap = Paint.Cap.ROUND
        }
        val deltaX = markerX - adjustedX
        val deltaY = markerY - centerY
        if (abs(deltaX) > radius * 0.15f || abs(deltaY) > radius * 0.75f) {
            val startY = centerY + radius * 0.70f
            canvas.drawLine(adjustedX, startY, markerX, markerY, connectorPaint)
        }
        canvas.drawCircle(markerX, markerY, max(2f, h * 0.022f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(accent, 235)
            style = Paint.Style.FILL
        })

        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x4D000000
            style = Paint.Style.FILL
            setShadowLayer(h * 0.045f, 0f, h * 0.020f, 0x99000000.toInt())
        }
        canvas.drawCircle(adjustedX, centerY + h * 0.010f, radius * 1.02f, shadow)

        val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (period == DayPeriod.NIGHT) 0xB0182740.toInt() else 0xA8192B42.toInt()
            style = Paint.Style.FILL
        }
        val badgeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (period == DayPeriod.NIGHT) 0x78B8CAEA else 0x70FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = max(1.2f, h * 0.013f)
        }
        val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(accent, 88)
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.007f)
        }
        canvas.drawCircle(adjustedX, centerY, radius, badgeFill)
        canvas.drawCircle(adjustedX, centerY, radius, badgeStroke)
        canvas.drawCircle(adjustedX, centerY, radius * 0.87f, innerStroke)

        TimelineWeatherMarkerRenderer.draw(
            canvas = canvas,
            cx = adjustedX,
            cy = centerY,
            sizePx = diameter * metrics.nowIconFractionOfBadge,
            condition = condition,
            period = period,
        )

        val nowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (period == DayPeriod.NIGHT) 0xFFD3E1F7.toInt() else TimelineWeatherMarkerRenderer.NOW_LABEL_GOLD
            textSize = h * if (compact) 0.105f else metrics.nowLabelTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.010f, 0f, h * 0.007f, 0xA0000000.toInt())
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.13f else metrics.nowTimeTextFraction
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.012f, 0f, h * 0.008f, 0xA0000000.toInt())
        }
        val labelBaseline = max(nowLabelPaint.textSize, centerY - radius - h * 0.040f)
        val timeBaseline = min(h - h * 0.020f, centerY + radius + timePaint.textSize * 1.05f)
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
        val bubbleCenterY = markerY.coerceIn(radius + h * 0.10f, h - radius - h * 0.12f)
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
     * O ponto cronológico nunca se move. Apenas o centro visual do badge pode
     * deslocar-se quando o Agora fica muito próximo de Nascer ou Ocaso.
     */
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
        var result = desiredCenterX
        val nowMinutes = minuteOf(now)
        val riseMinutes = minuteOf(sunrise)
        val setMinutes = minuteOf(sunset)
        val minutesFromSunrise = nowMinutes - riseMinutes
        val minutesUntilSunset = setMinutes - nowMinutes
        val threshold = diameter * 1.12f

        if (minutesFromSunrise in -18..18 && abs(markerX - sunriseX) < threshold) {
            result = sunriseX + diameter * 1.02f
        }
        if (minutesUntilSunset in 10..28 && abs(markerX - sunsetX) < threshold * 1.4f) {
            result = sunsetX - diameter * 1.05f
        } else if (minutesUntilSunset in -4..9 && abs(markerX - sunsetX) < threshold * 1.4f) {
            result = sunsetX + diameter * 0.88f
        }

        val radius = diameter / 2f
        val minCenter = left + radius + diameter * 0.10f
        val maxCenter = right - radius - diameter * 0.10f
        return result.coerceIn(minCenter, maxCenter)
    }

    /**
     * Mantém a projeção temporal já validada nos previews v2; o redesign desta
     * etapa é apenas visual, não altera a cronologia horizontal.
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

    private fun xFor(position: Float, left: Float, right: Float): Float =
        left + position.coerceIn(0f, 1f) * (right - left)

    private fun yOnArc(position: Float, cy: Float, rise: Float): Float {
        val t = position.coerceIn(0f, 1f)
        return cy - 3.2f * rise * t * (1f - t)
    }

    private fun minuteOf(time: LocalTime): Int = time.hour * 60 + time.minute

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

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
}