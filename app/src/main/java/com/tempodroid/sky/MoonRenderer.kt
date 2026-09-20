package com.tempodroid.sky

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
 * Disco lunar fotográfico com terminador esférico contínuo.
 *
 * A textura é preservada apenas onde existe superfície lunar; fase, orientação
 * e hemisfério continuam astronômicos. A iluminação ambiente controla halo e
 * earthshine de forma independente da face iluminada, evitando o antigo disco
 * azul/cinza em céu claro.
 */
object MoonRenderer {

    fun render(
        sizePx: Int,
        phase: MoonPhase,
        illumination: Double,
        hemisphere: Hemisphere,
        style: MoonRenderStyle,
        surfaceTexture: Bitmap? = null,
        ambientDarkness: Float = 0f,
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
            ambientDarkness = ambientDarkness,
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
        ambientDarkness: Float = 0f,
    ) {
        if (visibility <= 0.02f || radius < 1f) return
        val illum = illumination.coerceIn(0.0, 1.0)
        val orientation = MoonPhaseCalculator.getMoonOrientation(phase, hemisphere)
        val lightFromRight = orientation == MoonOrientation.LIGHT_FROM_RIGHT
        val haloBoost = if (phase == MoonPhase.FULL_MOON) 1f else illum.toFloat()
        val isSky = style == MoonRenderStyle.SKY
        val darkness = ambientDarkness.coerceIn(0f, 1f)
        val discBounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Lua nova no céu não vira um disco escuro artificial.
        if (isSky && illum <= 0.03 && phase == MoonPhase.NEW_MOON) return

        // Halo cinematográfico apenas quando o ambiente já está escuro.
        if (isSky && darkness > 0.16f && haloBoost > 0.18f && visibility > 0.20f) {
            val haloRadius = radius * (1.62f + 0.42f * haloBoost)
            val strength = visibility * haloBoost * darkness
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    haloRadius,
                    intArrayOf(
                        Color.argb((34 * strength).toInt().coerceIn(0, 34), 232, 238, 248),
                        Color.argb((13 * strength).toInt().coerceIn(0, 13), 190, 207, 235),
                        Color.TRANSPARENT,
                    ),
                    floatArrayOf(0f, 0.46f, 1f),
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
            ambientDarkness = darkness,
            surfaceTexture = surfaceTexture,
        )

        if (illum <= 0.03 && phase == MoonPhase.NEW_MOON) return

        val litPath = illuminatedPath(cx, cy, radius, illum, lightFromRight)
        val litAlpha = photographicSurfaceAlpha(visibility, isSky)
        canvas.save()
        canvas.clipPath(litPath)
        if (surfaceTexture != null && !surfaceTexture.isRecycled) {
            canvas.drawBitmap(
                surfaceTexture,
                null,
                discBounds,
                cinematicSurfacePaint(litAlpha, darkness, isSky),
            )
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

    /** Reforço perceptual aplicado somente à face iluminada. */
    internal fun photographicSurfaceAlpha(visibility: Float, isSky: Boolean): Int {
        val v = visibility.coerceIn(0f, 1f)
        val perceived = if (isSky) sqrt(v) else v
        return (255f * perceived).toInt().coerceIn(0, 255)
    }

    private fun cinematicSurfacePaint(alpha: Int, darkness: Float, isSky: Boolean): Paint {
        val contrast = if (isSky) 1.07f + 0.06f * darkness else 1.04f
        val red = contrast * 1.018f
        val green = contrast
        val blue = contrast * 0.975f
        val offset = if (isSky) 2.5f + 4.5f * darkness else 2f
        val matrix = ColorMatrix(
            floatArrayOf(
                red, 0f, 0f, 0f, offset,
                0f, green, 0f, 0f, offset,
                0f, 0f, blue, 0f, offset * 0.6f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = alpha
            colorFilter = ColorMatrixColorFilter(matrix)
        }
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
        ambientDarkness: Float,
        surfaceTexture: Bitmap?,
    ) {
        val disc = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(disc)

        val v = visibility.coerceIn(0f, 1f)
        val darkness = ambientDarkness.coerceIn(0f, 1f)
        val darkFaceFactor = (1f - illumination.coerceIn(0.0, 1.0).toFloat() * 0.35f).coerceIn(0.65f, 1f)

        if (surfaceTexture != null && !surfaceTexture.isRecycled) {
            val textureAlpha = if (isSky) {
                ((3f + 29f * darkness) * v * v * darkFaceFactor).toInt()
            } else {
                (46f * v * darkFaceFactor).toInt()
            }.coerceIn(0, 52)
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
            ambientDarkness = darkness,
        )
        if (shadeAlpha > 0) {
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(shadeAlpha, 34, 39, 48)
            }
            canvas.drawCircle(cx, cy, radius, shade)
        }
        canvas.restore()
    }

    internal fun earthshineShadeAlpha(
        illumination: Double,
        visibility: Float,
        isSky: Boolean,
        ambientDarkness: Float = 0f,
    ): Int {
        val v = visibility.coerceIn(0f, 1f)
        val darkness = ambientDarkness.coerceIn(0f, 1f)
        val phaseFactor = 1f - illumination.coerceIn(0.0, 1.0).toFloat() * 0.22f
        return if (isSky) {
            ((4f + 22f * darkness) * v * v * phaseFactor).toInt().coerceIn(0, 26)
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
