package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.astronomy.hemisphereFromLatitude
import com.tempodroid.sky.MoonRenderer
import com.tempodroid.sky.SunRenderer
import com.tempodroid.sky.TextProtectionOverlay
import com.tempodroid.widget.SolarWeatherWidgetState
import java.time.LocalTime

object SolarLunarTimelineRenderer {

    fun render(
        widthPx: Int,
        heightPx: Int,
        state: SolarWeatherWidgetState,
        now: LocalTime,
        compact: Boolean,
        moonSurface: Bitmap? = null,
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
        val model = TimelineCalculator.build(now, sunrise, sunset, state.solarNoon, state.moonrise, state.moonset)
        drawArc(canvas, pad, w - pad, h * 0.38f, h * 0.22f)
        val markers = TimelineLabels.defaultMarkers(sunrise, sunset)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xF2FFFFFF.toInt()
            textSize = h * if (compact) 0.13f else 0.16f
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.018f, 0f, h * 0.012f, 0x80000000.toInt())
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt()
            textSize = h * if (compact) 0.12f else 0.14f
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.016f, 0f, h * 0.010f, 0x73000000)
        }
        markers.forEach { marker ->
            val x = pad + marker.position * (w - 2 * pad)
            val iconY = h * 0.28f
            when {
                marker.position == 0f || marker.position == 1f -> {
                    val moon = MoonRenderer.render(
                        sizePx = (h * 0.42f).toInt().coerceAtLeast(16),
                        phase = state.moonPhase,
                        illumination = state.moonIllumination,
                        hemisphere = hemisphereFromLatitude(state.latitude),
                        style = MoonRenderStyle.TIMELINE,
                        surfaceTexture = moonSurface,
                    )
                    canvas.drawBitmap(moon, x - moon.width / 2f, iconY - moon.height / 2f, null)
                    moon.recycle()
                }
                marker.title.contains("Nascer") -> SunRenderer.drawCompact(canvas, x, iconY, h * 0.09f, setting = false)
                marker.title == "Dia" -> SunRenderer.drawCompact(canvas, x, iconY, h * 0.10f, setting = false)
                else -> SunRenderer.drawCompact(canvas, x, iconY, h * 0.09f, setting = true)
            }
            if (!compact) {
                canvas.drawText(marker.title, x, h * 0.68f, textPaint)
                canvas.drawText(marker.subtitle, x, h * 0.88f, subPaint)
            } else {
                canvas.drawText(marker.subtitle, x, h * 0.78f, subPaint)
            }
        }
        drawNowMarker(canvas, pad, w - pad, h, model.currentPosition, model.nowLabel, compact)
        return bitmap
    }

    private fun drawArc(canvas: Canvas, left: Float, right: Float, cy: Float, rise: Float) {
        val path = Path()
        path.moveTo(left, cy)
        path.quadTo((left + right) / 2f, cy - rise * 1.6f, right, cy)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
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

    private fun drawNowMarker(
        canvas: Canvas,
        left: Float,
        right: Float,
        h: Int,
        position: Float,
        label: String,
        compact: Boolean,
    ) {
        val x = left + position.coerceIn(0f, 1f) * (right - left)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            setShadowLayer(h * 0.014f, 0f, h * 0.01f, 0x80000000.toInt())
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * 0.14f
            textAlign = Paint.Align.CENTER
            setShadowLayer(h * 0.018f, 0f, h * 0.012f, 0x8A000000.toInt())
        }
        canvas.drawText("agora", x, h * 0.16f, text)
        canvas.drawCircle(x, h * 0.28f, 4.5f, paint)
        canvas.drawLine(x, h * 0.32f, x, h * 0.52f, paint.apply { strokeWidth = 1.6f })
        canvas.drawText(label, x, h * 0.55f + if (compact) 0f else 2f, text)
    }

    fun measureText(paint: Paint, value: String): Int {
        val bounds = Rect()
        paint.getTextBounds(value, 0, value.length, bounds)
        return bounds.width()
    }
}
