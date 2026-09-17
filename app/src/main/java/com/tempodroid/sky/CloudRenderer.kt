package com.tempodroid.sky

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.min
import kotlin.random.Random

object CloudRenderer {
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        cover: Int,
        lit: Int,
        shade: Int,
        seed: Int,
        nightTint: Boolean,
    ) {
        val amount = cover.coerceIn(0, 100)
        if (amount <= 4) return
        val random = Random(seed)
        val layers = when {
            amount < 25 -> 2
            amount < 50 -> 4
            amount < 75 -> 7
            else -> 10
        }
        val alphaScale = (0.22f + amount / 150f).coerceIn(0.22f, 0.84f)
        repeat(layers) { index ->
            val y = height * (0.08f + random.nextFloat() * 0.52f)
            val x = width * (random.nextFloat() * 1.24f - 0.12f)
            val scale = 0.60f + random.nextFloat() * 0.95f
            val cloudWidth = width * (0.24f + amount / 300f) * scale
            val cloudHeight = height * (0.10f + amount / 1000f) * scale
            val baseColor = if (index % 3 == 0) shade else lit
            val alpha = (Color.alpha(baseColor).coerceAtLeast(135) * alphaScale).toInt().coerceIn(42, 210)
            val tinted = if (nightTint) {
                Color.argb(
                    alpha,
                    Color.red(baseColor) / 2 + 20,
                    Color.green(baseColor) / 2 + 24,
                    Color.blue(baseColor) / 2 + 40,
                )
            } else {
                Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            }
            drawSoftCloud(canvas, x, y, cloudWidth, cloudHeight, tinted, shade)
        }
    }

    private fun drawSoftCloud(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Int,
        shade: Int,
    ) {
        val radius = min(width, height)
        val shadowAlpha = (Color.alpha(color) * 0.45f).toInt().coerceIn(20, 115)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                x,
                y - height * 0.2f,
                x,
                y + height * 0.7f,
                intArrayOf(
                    Color.argb(0, Color.red(shade), Color.green(shade), Color.blue(shade)),
                    Color.argb(shadowAlpha, Color.red(shade), Color.green(shade), Color.blue(shade)),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawOval(
            RectF(x - width * 0.08f, y, x + width * 0.92f, y + height * 0.55f),
            shadowPaint,
        )

        val puffs = arrayOf(
            floatArrayOf(0.00f, 0.10f, 0.50f),
            floatArrayOf(0.25f, -0.12f, 0.58f),
            floatArrayOf(0.52f, 0.00f, 0.66f),
            floatArrayOf(0.80f, 0.10f, 0.46f),
            floatArrayOf(0.38f, 0.18f, 0.54f),
        )
        puffs.forEach { puff ->
            val cx = x + width * puff[0]
            val cy = y + height * puff[1]
            val r = radius * puff[2]
            val alpha = Color.alpha(color)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    r,
                    intArrayOf(
                        color,
                        Color.argb(
                            (alpha * 0.68f).toInt(),
                            Color.red(color),
                            Color.green(color),
                            Color.blue(color),
                        ),
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.68f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(cx, cy, r, paint)
        }
    }
}
