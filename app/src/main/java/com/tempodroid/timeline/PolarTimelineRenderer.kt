package com.tempodroid.timeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import com.tempodroid.sky.TextProtectionOverlay
import kotlin.math.max

/** Timeline sem âncoras falsas para locais/data em dia ou noite polar. */
object PolarTimelineRenderer {
    fun render(
        widthPx: Int,
        heightPx: Int,
        isDay: Boolean,
        panelDarknessAlpha: Int,
    ): Bitmap {
        val w = widthPx.coerceAtLeast(120)
        val h = heightPx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
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

        val left = w * 0.08f
        val right = w * 0.92f
        val cy = h * 0.56f
        val rise = h * 0.24f
        val path = Path().apply {
            moveTo(left, cy)
            quadTo((left + right) / 2f, cy - rise, right, cy)
        }
        val colors = if (isDay) {
            intArrayOf(0xFFFFB020.toInt(), 0xFFFFE66A.toInt(), 0xFFFF8A3D.toInt())
        } else {
            intArrayOf(0xFF5368C7.toInt(), 0xFF8DA2D8.toInt(), 0xFF5368C7.toInt())
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(2.2f, h * 0.03f)
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                left,
                cy,
                right,
                cy,
                colors,
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(path, line)

        val title = if (isDay) "Dia polar" else "Noite polar"
        val subtitle = if (isDay) "Sol acima do horizonte" else "Sol abaixo do horizonte"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = h * 0.20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xD9FFFFFF.toInt()
            textSize = h * 0.12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, w / 2f, h * 0.46f, titlePaint)
        canvas.drawText(subtitle, w / 2f, h * 0.78f, subtitlePaint)
        return bitmap
    }
}
