package com.tempodroid.sky

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Proteção de contraste para textos sobre os rasters fotográficos do Figma.
 *
 * A imagem original nunca é substituída nem recolorida. O renderer mede a
 * luminância real nas regiões de texto e aplica scrims neutros. A proteção do
 * topo é deliberadamente contínua em toda a largura para evitar costuras ou
 * mudanças artificiais de tom entre os blocos esquerdo e direito.
 */
object TextProtectionOverlay {

    fun draw(canvas: Canvas, bitmap: Bitmap, width: Int, height: Int) {
        val topScore = luminanceScore(bitmap, 0.00f, 0.00f, 1.00f, 0.48f)
        val bottomScore = luminanceScore(bitmap, 0.00f, 0.62f, 1.00f, 1.00f)

        val topAlpha = adaptiveAlpha(topScore, minAlpha = 0.05f, maxAlpha = 0.24f)
        val bottomAlpha = adaptiveAlpha(bottomScore, minAlpha = 0.04f, maxAlpha = 0.18f)

        drawContinuousTopScrim(
            canvas = canvas,
            width = width,
            height = height,
            alpha = topAlpha,
        )

        val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                height * 0.60f,
                0f,
                height.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((bottomAlpha * 255f).roundToInt(), 0, 0, 0),
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, height * 0.60f, width.toFloat(), height.toFloat(), bottomPaint)
    }

    /**
     * Opacidade do painel da timeline. Retorna alpha ARGB (0..255).
     * A região é a faixa visual onde a timeline é posicionada no widget 4x2.
     */
    fun timelineGlassAlpha(bitmap: Bitmap): Int {
        val score = luminanceScore(bitmap, 0.04f, 0.46f, 0.96f, 0.82f)
        return (adaptiveAlpha(score, minAlpha = 0.18f, maxAlpha = 0.34f) * 255f)
            .roundToInt()
            .coerceIn(0, 255)
    }

    fun glassPanel(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        darknessAlpha: Int,
    ) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(darknessAlpha.coerceIn(0, 255), 0, 0, 0)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, fill)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, stroke)
    }

    internal fun adaptiveAlpha(
        luminanceScore: Double,
        minAlpha: Float,
        maxAlpha: Float,
    ): Float {
        val normalized = ((luminanceScore - 0.08) / (0.74 - 0.08)).coerceIn(0.0, 1.0).toFloat()
        // Curva suave: evita saltos perceptíveis quando o céu muda de estado.
        val eased = normalized * normalized * (3f - 2f * normalized)
        return minAlpha + (maxAlpha - minAlpha) * eased
    }

    /**
     * Scrim superior único e contínuo. O shader varia apenas no eixo vertical;
     * portanto dois pixels na mesma altura recebem exatamente a mesma proteção,
     * eliminando a emenda vertical que aparecia entre os antigos scrims locais.
     */
    private fun drawContinuousTopScrim(
        canvas: Canvas,
        width: Int,
        height: Int,
        alpha: Float,
    ) {
        val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                height * 0.64f,
                intArrayOf(
                    Color.argb(a, 0, 0, 0),
                    Color.argb((a * 0.82f).roundToInt(), 0, 0, 0),
                    Color.argb((a * 0.42f).roundToInt(), 0, 0, 0),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.30f, 0.64f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.64f, paint)
    }

    /**
     * Combina luminância média e percentil 85. O percentil alto protege contra
     * pequenos trechos muito claros (sol/nuvem) que uma simples média ocultaria.
     */
    private fun luminanceScore(
        bitmap: Bitmap,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float,
    ): Double {
        if (bitmap.width <= 0 || bitmap.height <= 0) return 0.0
        val left = (bitmap.width * leftFraction).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (bitmap.height * topFraction).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (bitmap.width * rightFraction).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bitmap.height * bottomFraction).roundToInt().coerceIn(top + 1, bitmap.height)

        val columns = 14
        val rows = 8
        val samples = ArrayList<Double>(columns * rows)
        repeat(rows) { row ->
            val y = (top + ((row + 0.5f) / rows) * (bottom - top))
                .roundToInt()
                .coerceIn(top, bottom - 1)
            repeat(columns) { column ->
                val x = (left + ((column + 0.5f) / columns) * (right - left))
                    .roundToInt()
                    .coerceIn(left, right - 1)
                samples += relativeLuminance(bitmap.getPixel(x, y))
            }
        }
        if (samples.isEmpty()) return 0.0
        samples.sort()
        val average = samples.average()
        val p85 = samples[((samples.lastIndex * 0.85).roundToInt()).coerceIn(0, samples.lastIndex)]
        return average * 0.38 + p85 * 0.62
    }

    private fun relativeLuminance(color: Int): Double {
        fun linear(channel: Int): Double {
            val value = channel / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        val r = linear(Color.red(color))
        val g = linear(Color.green(color))
        val b = linear(Color.blue(color))
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
