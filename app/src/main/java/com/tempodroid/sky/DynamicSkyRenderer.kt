package com.tempodroid.sky

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.tempodroid.astronomy.CelestialPositionCalculator
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.ui.WidgetDimensions

/**
 * Renderer visual alinhado ao arquivo final do Figma.
 *
 * A camada base usa os rasters de céu. A Lua permanece astronômica/dinâmica
 * e é composta por cima do fundo com textura fotográfica e fase real.
 *
 * Fundos raster não têm profundidade real. Portanto não tentamos "recortar"
 * a Lua redesenhando faixas do background sobre ela; a presença da Lua em
 * condições com nuvens é controlada somente pela opacidade meteorológica.
 */
object DynamicSkyRenderer {

    fun render(
        context: Context,
        input: SkyRenderInput,
        moonSurface: Bitmap? = null,
    ): Bitmap {
        val (w, h) = WidgetDimensions.clampBitmapSize(input.widthPx, input.heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val period = DayPeriodCalculator.from(input.localTime, input.sunrise, input.sunset, input.solarNoon)
        val backgroundState = FigmaVisualStateResolver.background(period, input.weatherCondition)
        val background = FigmaVisualAssets.sky(context, backgroundState)
        drawCenterCrop(canvas, background, w, h)

        val moon = input.moonPosition
        val moonVisibility = if (moon?.aboveHorizon == true) {
            FigmaVisualStateResolver.moonOpacity(period, input.weatherCondition)
        } else {
            0f
        }

        if (moon != null && moonVisibility > 0f) {
            val altitudeDegrees = moon.altitudeDegrees
            val azimuthDegrees = moon.azimuthDegrees
            val projectedMoon = if (altitudeDegrees != null && azimuthDegrees != null) {
                CelestialPositionCalculator.fromHorizontalCoordinates(
                    altitudeDegrees = altitudeDegrees,
                    azimuthDegrees = azimuthDegrees,
                    aboveHorizon = moon.aboveHorizon,
                )
            } else {
                moon
            }

            val moonX = projectedMoon.normalizedX * w
            val moonY = projectedMoon.normalizedY * h
            val moonComponentSize = h * FigmaVisualStateResolver.moonComponentSizeFraction(period)
            val radius = moonComponentSize * 0.34f
            MoonRenderer.draw(
                canvas = canvas,
                cx = moonX,
                cy = moonY,
                radius = radius,
                phase = input.moonPhase,
                illumination = input.moonIllumination,
                hemisphere = input.hemisphere,
                style = MoonRenderStyle.SKY,
                visibility = moonVisibility,
                surfaceTexture = moonSurface ?: FigmaVisualAssets.moonSurface(context),
                ambientDarkness = ambientDarkness(period),
            )
        }

        TextProtectionOverlay.draw(canvas, bitmap, w, h)
        return bitmap
    }

    private fun ambientDarkness(period: DayPeriod): Float = when (period) {
        DayPeriod.NIGHT -> 1f
        DayPeriod.TWILIGHT -> 0.72f
        DayPeriod.DAWN -> 0.48f
        DayPeriod.SUNSET -> 0.42f
        DayPeriod.MORNING -> 0.14f
        DayPeriod.AFTERNOON -> 0.08f
        DayPeriod.DAY -> 0f
    }

    private fun drawCenterCrop(canvas: Canvas, source: Bitmap, width: Int, height: Int) {
        val src = centerCropRect(source.width, source.height, width, height)
        val dst = Rect(0, 0, width, height)
        canvas.drawBitmap(
            source,
            src,
            dst,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    }

    private fun centerCropRect(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Rect {
        val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        return if (sourceRatio > targetRatio) {
            val cropWidth = (sourceHeight * targetRatio).toInt().coerceAtMost(sourceWidth)
            val left = ((sourceWidth - cropWidth) / 2).coerceAtLeast(0)
            Rect(left, 0, left + cropWidth, sourceHeight)
        } else {
            val cropHeight = (sourceWidth / targetRatio).toInt().coerceAtMost(sourceHeight)
            val top = ((sourceHeight - cropHeight) / 2).coerceAtLeast(0)
            Rect(0, top, sourceWidth, top + cropHeight)
        }
    }
}
