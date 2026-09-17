package com.tempodroid.sky

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.tempodroid.weather.WeatherCondition
import kotlin.random.Random

object RainRenderer {
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        condition: WeatherCondition,
        precipitation: Double?,
        rainColor: Int,
        seed: Int,
    ) {
        val density = when (condition) {
            WeatherCondition.RAIN -> 42
            WeatherCondition.HEAVY_RAIN -> 70
            WeatherCondition.STORM -> 90
            else -> return
        } + ((precipitation ?: 0.0) * 8).toInt()
        val random = Random(seed)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = if (condition == WeatherCondition.STORM) 2.2f else 1.4f
            strokeCap = Paint.Cap.ROUND
        }
        repeat(density.coerceAtMost(120)) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val len = height * (0.06f + random.nextFloat() * 0.10f)
            val alpha = 70 + random.nextInt(80)
            paint.shader = LinearGradient(
                x,
                y,
                x + len * 0.18f,
                y + len,
                intArrayOf(
                    (alpha shl 24) or (rainColor and 0x00FFFFFF),
                    0x00FFFFFF,
                ),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawLine(x, y, x + len * 0.18f, y + len, paint)
        }
        if (condition == WeatherCondition.STORM) {
            val flash = Paint().apply { color = 0x14FFFFFF }
            canvas.drawRect(0f, 0f, width.toFloat(), height * 0.45f, flash)
        }
    }
}
