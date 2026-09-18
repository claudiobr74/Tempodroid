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
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.LocalTime
import kotlin.math.max

object SolarLunarTimelineRenderer {

    internal data class NowMarkerLayout(
        val markerX: Float,
        val markerY: Float,
        val bubbleCenterX: Float,
        val bubbleTop: Float,
        val bubbleWidth: Float,
        val bubbleHeight: Float,
    )

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
        val pad = w * 0.04f

        TextProtectionOverlay.glassPanel(
            canvas = canvas,
            left = 0f,
            top = 0f,
            right = w.toFloat(),
            bottom = h.toFloat(),
            radius = h * 0.28f,
            darknessAlpha = panelDarknessAlpha,
        )

        val sunrise = state.sunrise ?: LocalTime.of(6, 10)
        val sunset = state.sunset ?: LocalTime.of(18, 5)
        val model = TimelineCalculator.build(
            now = now,
            sunrise = sunrise,
            sunset = sunset,
            solarNoon = state.solarNoon,
            moonrise = state.moonrise,
            moonset = state.moonset,
        )
        val period = DayPeriodCalculator.from(now, sunrise, sunset, state.solarNoon)

        val arcCy = h * 0.58f
        val arcRise = h * 0.46f
        drawArc(canvas, pad, w - pad, arcCy, arcRise)

        val anchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xB3FFFFFF.toInt()
            textSize = h * if (compact) 0.105f else 0.115f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(h * 0.012f, 0f, h * 0.008f, 0x66000000)
        }

        drawSunAnchor(
            canvas = canvas,
            x = xFor(model.sunrisePosition, pad, w - pad),
            y = yOnArc(model.sunrisePosition, arcCy, arcRise),
            time = TimelineCalculator.formatClock(sunrise),
            sunrise = true,
            h = h,
            paint = anchorPaint,
        )
        drawSunAnchor(
            canvas = canvas,
            x = xFor(model.sunsetPosition, pad, w - pad),
            y = yOnArc(model.sunsetPosition, arcCy, arcRise),
            time = TimelineCalculator.formatClock(sunset),
            sunrise = false,
            h = h,
            paint = anchorPaint,
        )

        drawNowMarker(
            canvas = canvas,
            left = pad,
            right = w - pad,
            h = h,
            position = model.currentPosition,
            label = model.nowLabel,
            condition = state.condition,
            period = period,
            arcCy = arcCy,
            arcRise = arcRise,
            compact = compact,
        )
        return bitmap
    }

    private fun drawArc(canvas: Canvas, left: Float, right: Float, cy: Float, rise: Float) {
        val path = Path()
        path.moveTo(left, cy)
        path.quadTo((left + right) / 2f, cy - rise * 1.6f, right, cy)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(2f, (right - left) * 0.004f)
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                left,
                cy,
                right,
                cy,
                intArrayOf(
                    0xFF1B2A6A.toInt(),
                    0xFFFFC36A.toInt(),
                    0xFFFFE66A.toInt(),
                    0xFFFF8A3A.toInt(),
                    0xFF6A3A8A.toInt(),
                ),
                floatArrayOf(0f, 0.28f, 0.5f, 0.72f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSunAnchor(
        canvas: Canvas,
        x: Float,
        y: Float,
        time: String,
        sunrise: Boolean,
        h: Int,
        paint: Paint,
    ) {
        val radius = max(3f, h * 0.055f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (sunrise) 0xFFFFD45A.toInt() else 0xFFFF9256.toInt()
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.012f)
        }
        canvas.drawCircle(x, y, radius, fill)
        canvas.drawCircle(x, y, radius, stroke)

        val gap = h * 0.065f
        val baseline = y - (paint.ascent() + paint.descent()) / 2f
        paint.textAlign = if (sunrise) Paint.Align.LEFT else Paint.Align.RIGHT
        canvas.drawText(
            time,
            if (sunrise) x + radius + gap else x - radius - gap,
            baseline,
            paint,
        )
    }

    private fun drawNowMarker(
        canvas: Canvas,
        left: Float,
        right: Float,
        h: Int,
        position: Float,
        label: String,
        condition: com.tempodroid.weather.WeatherCondition,
        period: DayPeriod,
        arcCy: Float,
        arcRise: Float,
        compact: Boolean,
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * if (compact) 0.12f else 0.13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val iconSize = h * if (compact) 0.20f else 0.22f
        val horizontalPadding = h * 0.10f
        val gap = h * 0.06f
        val bubbleHeight = h * 0.34f
        val bubbleWidth = horizontalPadding * 2f + iconSize + gap + textPaint.measureText(label)
        val layout = calculateNowMarkerLayout(
            left = left,
            right = right,
            h = h,
            position = position,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            arcCy = arcCy,
            arcRise = arcRise,
        )

        val accent = TimelineWeatherMarkerRenderer.bubbleAccent(period)
        val rect = RectF(
            layout.bubbleCenterX - layout.bubbleWidth / 2f,
            layout.bubbleTop,
            layout.bubbleCenterX + layout.bubbleWidth / 2f,
            layout.bubbleTop + layout.bubbleHeight,
        )
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x66172338
            style = Paint.Style.FILL
            setShadowLayer(h * 0.025f, 0f, h * 0.016f, 0x45000000)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.012f)
        }
        val radius = layout.bubbleHeight / 2f
        canvas.drawRoundRect(rect, radius, radius, bubblePaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        val iconCx = rect.left + horizontalPadding + iconSize / 2f
        val bubbleCy = rect.centerY()
        TimelineWeatherMarkerRenderer.draw(
            canvas = canvas,
            cx = iconCx,
            cy = bubbleCy,
            sizePx = iconSize,
            condition = condition,
            period = period,
        )

        textPaint.textAlign = Paint.Align.LEFT
        val textX = iconCx + iconSize / 2f + gap
        val textY = bubbleCy - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, textX, textY, textPaint)

        val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.012f)
            strokeCap = Paint.Cap.ROUND
        }
        val connectorStartY = rect.bottom + h * 0.025f
        val connectorEndY = layout.markerY - h * 0.065f
        if (connectorEndY > connectorStartY) {
            canvas.drawLine(
                layout.bubbleCenterX,
                connectorStartY,
                layout.markerX,
                connectorEndY,
                connectorPaint,
            )
        }

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accent and 0x00FFFFFF) or 0x44000000
            style = Paint.Style.FILL
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.FILL
        }
        val dotOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xF2FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = max(1f, h * 0.012f)
        }
        val dotRadius = max(2.5f, h * 0.045f)
        canvas.drawCircle(layout.markerX, layout.markerY, dotRadius * 1.75f, glow)
        canvas.drawCircle(layout.markerX, layout.markerY, dotRadius, dot)
        canvas.drawCircle(layout.markerX, layout.markerY, dotRadius, dotOutline)
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
    ): NowMarkerLayout {
        val p = position.coerceIn(0f, 1f)
        val markerX = xFor(p, left, right)
        val markerY = yOnArc(p, arcCy, arcRise)
        val safeMargin = h * 0.03f
        val minCenter = left + bubbleWidth / 2f + safeMargin
        val maxCenter = right - bubbleWidth / 2f - safeMargin
        val bubbleCenterX = if (minCenter <= maxCenter) {
            markerX.coerceIn(minCenter, maxCenter)
        } else {
            (left + right) / 2f
        }
        val connectorRoom = h * 0.08f
        val desiredTop = markerY - bubbleHeight - connectorRoom
        val bubbleTop = max(h * 0.02f, desiredTop)

        return NowMarkerLayout(
            markerX = markerX,
            markerY = markerY,
            bubbleCenterX = bubbleCenterX,
            bubbleTop = bubbleTop,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
        )
    }

    private fun xFor(position: Float, left: Float, right: Float): Float =
        left + position.coerceIn(0f, 1f) * (right - left)

    private fun yOnArc(position: Float, cy: Float, rise: Float): Float {
        val t = position.coerceIn(0f, 1f)
        return cy - 3.2f * rise * t * (1f - t)
    }

    fun measureText(paint: Paint, value: String): Int {
        val bounds = Rect()
        paint.getTextBounds(value, 0, value.length, bounds)
        return bounds.width()
    }
}
