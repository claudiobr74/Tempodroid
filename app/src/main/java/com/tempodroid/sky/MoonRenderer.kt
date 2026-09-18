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

/**
 * Disco lunar com máscara de terminador esférica.
 *
 * Quando [surfaceTexture] é fornecida, a superfície iluminada usa exatamente
 * a textura raster do componente MoonVisual do Figma. A mesma máscara
 * astronômica continua determinando fase, orientação e hemisfério.
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

        if (isSky && haloBoost > 0.2f && visibility > 0.4f) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    radius * (2.4f + haloBoost),
                    intArrayOf(
                        Color.argb((70 * visibility * haloBoost).toInt(), 210, 220, 255),
                        Color.argb((28 * visibility * haloBoost).toInt(), 180, 200, 255),
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(cx, cy, radius * (2.4f + haloBoost), haloPaint)
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
        canvas.save()
        canvas.clipPath(litPath)
        if (surfaceTexture != null && !surfaceTexture.isRecycled) {
            val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (255 * visibility).toInt().coerceIn(0, 255)
            }
            canvas.drawBitmap(surfaceTexture, null, discBounds, texturePaint)
        } else {
            val lit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((255 * visibility).toInt().coerceIn(0, 255), 232, 236, 242)
            }
            canvas.drawPath(litPath, lit)
            val crater = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((40 * visibility).toInt(), 160, 165, 180)
            }
            canvas.drawCircle(cx - radius * 0.22f, cy - radius * 0.18f, radius * 0.16f, crater)
            canvas.drawCircle(cx + radius * 0.18f, cy + radius * 0.12f, radius * 0.10f, crater)
            canvas.drawCircle(cx - radius * 0.05f, cy + radius * 0.28f, radius * 0.08f, crater)
        }
        canvas.restore()
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
        if (surfaceTexture != null && !surfaceTexture.isRecycled) {
            val disc = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(disc)
            val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = ((if (isSky) 82 else 96) * visibility).toInt().coerceIn(0, 120)
            }
            canvas.drawBitmap(surfaceTexture, null, bounds, texturePaint)
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(
                    (190 - illumination.coerceIn(0.0, 1.0) * 70.0).toInt().coerceIn(80, 210),
                    18,
                    34,
                    58,
                )
            }
            canvas.drawCircle(cx, cy, radius, shade)
            canvas.restore()
            return
        }

        val earthshineAlpha = ((if (isSky) 38 else 50) * visibility * (1f - illumination.toFloat() * 0.4f)).toInt()
        val earthshine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(earthshineAlpha.coerceIn(0, 90), 90, 100, 130)
        }
        canvas.drawCircle(cx, cy, radius, earthshine)
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
