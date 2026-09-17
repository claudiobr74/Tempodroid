package com.tempodroid.sky

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.tempodroid.sky.SkyPalette.Companion.mix

object SunRenderer {
    fun draw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        glowColor: Int,
        intensity: Float = 1f,
    ) {
        if (radius < 1f || intensity <= 0f) return
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                radius * 3.6f,
                intArrayOf(
                    mix(glowColor, Color.WHITE, 0.35f).withAlpha((180 * intensity).toInt()),
                    mix(glowColor, Color.WHITE, 0.1f).withAlpha((70 * intensity).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius * 3.6f, glow)
        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((255 * intensity).toInt().coerceIn(0, 255), 255, 244, 180)
        }
        canvas.drawCircle(cx, cy, radius, core)
        val hot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((220 * intensity).toInt().coerceIn(0, 255), 255, 255, 245)
        }
        canvas.drawCircle(cx, cy, radius * 0.55f, hot)
    }

    fun drawCompact(canvas: Canvas, cx: Float, cy: Float, radius: Float, setting: Boolean) {
        val color = if (setting) Color.rgb(255, 168, 70) else Color.rgb(255, 214, 80)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(90, 255, 220, 120)
        }
        canvas.drawCircle(cx, cy, radius * 1.6f, glow)
        canvas.drawCircle(cx, cy, radius, paint)
    }
}

private fun Int.withAlpha(alpha: Int): Int =
    (alpha.coerceIn(0, 255) shl 24) or (this and 0x00FFFFFF)
