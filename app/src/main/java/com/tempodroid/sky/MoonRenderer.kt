package com.tempodroid.sky

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.tempodroid.astronomy.Hemisphere
import com.tempodroid.astronomy.MoonOrientation
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.astronomy.MoonPhaseCalculator
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.astronomy.MoonTerminator
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Disco lunar com máscara de terminador esférica.
 *
 * A superfície iluminada usa a textura fotográfica do MoonVisual. A fase,
 * orientação e hemisfério permanecem astronômicos.
 *
 * No céu, a parte iluminada recebe reforço perceptual próprio, enquanto a
 * parte não iluminada (earthshine) continua muito sutil. Isso evita o antigo
 * "disco azul" sem voltar a deixar a textura lunar apagada durante o dia.
 */
object MoonRenderer {

    fun render(
        sizePx: Int,
        phase: MoonPhase,
        illumination: Double,
        hemisphere: Hemisphere,
        style: MoonRenderStyle,
        surfaceTexture: Bitmap? = null,
    ): Bitmap {
        val size = sizePx.coerceIn(8, 256)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(
            canvas = canvas,
            cx = size / 2f,
            cy = size / 2f,
            radius = size * if (style == MoonRenderStyle.SKY) 0.34f else 0.38f,
            phase = phase,
            illumination = illumination,
            hemisphere = hemisphere,
            style = style,
            visibility = 1f,
            surfaceTexture = surfaceTexture,
        )
        return bitmap
    }

    fun draw(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        phase: MoonPhase,
        illumination: Double,
        hemisphere: Hemisphere,
        style: MoonRenderStyle,
        visibility: Float,
        surfaceTexture: Bitmap? = null,
    ) {
        if (visibility <= 0.02f || radius < 1f) return
        val illum = illumination.coerceIn(0.0, 1.0)
        val orientation = MoonPhaseCalculator.getMoonOrientation(phase, hemisphere)
        val lightFromRight = orientation == MoonOrientation.LIGHT_FROM_RIGHT
        val haloBoost = if (phase == MoonPhase.FULL_MOON) 1f else illum.toFloat()
        val isSky = style == MoonRenderStyle.SKY
        val discBounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Lua nova no céu não deve aparecer como um disco escuro artificial.
        if (isSky && illum <= 0.03 && phase == MoonPhase.NEW_MOON) return

        // Halo só aparece quando a Lua já tem presença suficiente no ambiente;
        // em pleno dia ele desaparece, evitando aspecto de adesivo brilhante.
        if (isSky && haloBoost > 0.2f && visibility > 0.55f) {
            val haloRadius = radius * (1.70f + 0.45f * haloBoost)
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    haloRadius,
                    intArrayOf(
                        Color.argb((30 * visibility * haloBoost).toInt(), 218, 228, 255),
                        Color.argb((10 * visibility * haloBoost).toInt(), 180, 204, 255),
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.48f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(cx, cy, haloRadius, haloPaint)
        }

        drawEarthshine(
            canvas = canvas,
            bounds = discBounds,
            cx = cx,
            cy = cy,
            radius = radius,
            illumination = illum,
            visibility = visibility,
            isSky = isSky,
            surfaceTexture = surfaceTexture,
        )

        if (illum <= 0.03 && phase == MoonPhase.NEW_MOON) return

        val litPath = illuminatedPath(cx, cy, radius, illum, lightFromRight)
        val litAlpha = photographicSurfaceAlpha(visibility, isSky)
        canvas.save()
        canvas.clipPath(litPath)
        if (surfaceTexture != null && !surfaceTexture.isRecycled) {
            val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = litAlpha
            }
            canvas.drawBitmap(surfaceTexture, null, discBounds, texturePaint)
        } else {
            val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(litAlpha, 232, 236, 242)
            }
            canvas.drawPath(litPath, lit)
            val crater = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((40 * visibility).toInt().coerceIn(0, 40), 160, 165, 180)
            }
            canvas.drawCircle(cx - radius * 0.22f, cy - radius * 0.18f, radius * 0.16f, crater)
            canvas.drawCircle(cx + radius * 0.18f, cy + radius * 0.12f, radius * 0.10f, crater)
            canvas.drawCircle(cx - radius * 0.05f, cy + radius * 0.28f, radius * 0.08f, crater)
        }
        canvas.restore()
    }

    /**
     * Reforça somente a superfície iluminada da Lua no céu. O estado
     * meteorológico continua controlando [visibility], mas o ganho não vaza
     * para o earthshine nem para o halo.
     */
    internal fun photographicSurfaceAlpha(visibility: Float, isSky: Boolean): Int {
        val v = visibility.coerceIn(0f, 1f)
        val perceived = if (isSky) sqrt(v) else v
        return (255f * perceived).toInt().coerceIn(0, 255)
    }

    private fun drawEarthshine(
        canvas: Canvas,
        bounds: RectF,
        cx: Float,
        cy: Float,
        radius: Float,
        illumination: Double,
        visibility: Float,
        isSky: Boolean,
        surfaceTexture: Bitmap?,
    ) {
        val disc = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(disc)

        if (surfaceTexture != null && !surfaceTexture.isRecycled) {
            val textureAlpha = if (isSky) {
                // Quadrático: praticamente invisível de dia, sutil à noite.
                (16f * visibility.coerceIn(0f, 1f) * visibility.coerceIn(0f, 1f)).toInt()
            } else {
                (46f * visibility.coerceIn(0f, 1f)).toInt()
            }.coerceIn(0, 64)
            if (textureAlpha > 0) {
                val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = textureAlpha
                }
                canvas.drawBitmap(surfaceTexture, null, bounds, texturePaint)
            }
        }

        val shadeAlpha = earthshineShadeAlpha(
            illumination = illumination,
            visibility = visibility,
            isSky = isSky,
        )
        if (shadeAlpha > 0) {
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isSky) {
                    Color.argb(shadeAlpha, 46, 54, 66)
                } else {
                    Color.argb(shadeAlpha, 58, 68, 92)
                }
            }
            canvas.drawCircle(cx, cy, radius, shade)
        }
        canvas.restore()
    }

    internal fun earthshineShadeAlpha(
        illumination: Double,
        visibility: Float,
        isSky: Boolean,
    ): Int {
        val v = visibility.coerceIn(0f, 1f)
        val phaseFactor = 1f - illumination.coerceIn(0.0, 1.0).toFloat() * 0.20f
        return if (isSky) {
            (24f * v * v * phaseFactor).toInt().coerceIn(0, 24)
        } else {
            (60f * v * phaseFactor).toInt().coerceIn(0, 68)
        }
    }

    internal fun illuminatedPath(
        cx: Float,
        cy: Float,
        radius: Float,
        illumination: Double,
        lightFromRight: Boolean,
    ): Path {
        val disc = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        if (illumination >= 0.98) return disc
        if (illumination <= 0.02) return Path()

        val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val semi = Path()
        val start = if (lightFromRight) -90f else 90f
        semi.addArc(bounds, start, 180f)
        semi.close()

        if (abs(illumination - 0.5) < 0.03) return semi

        val rx = MoonTerminator.ellipseHalfWidth(illumination, radius).coerceAtLeast(0.6f)
        val terminator = Path().apply {
            addOval(RectF(cx - rx, cy - radius, cx + rx, cy + radius), Path.Direction.CW)
        }
        val result = Path()
        val ok = if (MoonTerminator.isGibbous(illumination)) {
            result.op(semi, terminator, Path.Op.UNION) && result.op(disc, Path.Op.INTERSECT)
        } else {
            result.op(semi, terminator, Path.Op.DIFFERENCE)
        }
        return if (!ok || result.isEmpty) semi else result
    }
}
