package com.tempodroid.sky

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
            amount < 25 -> 3
            amount < 50 -> 6
            amount < 75 -> 9
            else -> 12
        }
        val alphaScale = (0.28f + amount / 140f).coerceIn(0.25f, 0.92f)
        repeat(layers) { index ->
            val y = height * (0.08f + random.nextFloat() * 0.55f)
            val x = width * (random.nextFloat() * 1.2f - 0.1f)
            val scale = 0.55f + random.nextFloat() * 0.9f
            val w = width * (0.22f + amount / 280f) * scale
            val h = height * (0.10f + amount / 900f) * scale
            val color = if (index % 3 == 0) shade else lit
            val a = (Color.alpha(color).coerceAtLeast(140) * alphaScale).toInt().coerceIn(50, 230)
            val tinted = if (nightTint) {
                Color.argb(a, Color.red(color) / 2 + 20, Color.green(color) / 2 + 24, Color.blue(color) / 2 + 40)
            } else {
                Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
            }
            drawCloud(canvas, x, y, w, h, tinted)
        }
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val path = Path()
        val r = min(w, h)
        path.addCircle(x, y, r * 0.55f, Path.Direction.CW)
        path.addCircle(x + w * 0.28f, y - h * 0.18f, r * 0.48f, Path.Direction.CW)
        path.addCircle(x + w * 0.55f, y, r * 0.62f, Path.Direction.CW)
        path.addCircle(x + w * 0.82f, y + h * 0.05f, r * 0.42f, Path.Direction.CW)
        path.addCircle(x + w * 0.38f, y + h * 0.16f, r * 0.50f, Path.Direction.CW)
        canvas.drawPath(path, paint)
    }
}
