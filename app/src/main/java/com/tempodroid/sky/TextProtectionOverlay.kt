package com.tempodroid.sky

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.tempodroid.ui.WidgetTheme

object TextProtectionOverlay {
    fun draw(canvas: Canvas, width: Int, height: Int, top: Int, bottom: Int) {
        val topPaint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height * 0.46f,
                intArrayOf(top, Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.46f, topPaint)
        val bottomPaint = Paint().apply {
            shader = LinearGradient(
                0f,
                height * 0.52f,
                0f,
                height.toFloat(),
                intArrayOf(Color.TRANSPARENT, bottom),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, height * 0.52f, width.toFloat(), height.toFloat(), bottomPaint)
        val left = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                width * 0.42f,
                0f,
                intArrayOf(0x59000000, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width * 0.42f, height * 0.62f, left)
    }

    fun glassPanel(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WidgetTheme.GLASS_FILL }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WidgetTheme.GLASS_STROKE
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, fill)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, stroke)
    }
}
