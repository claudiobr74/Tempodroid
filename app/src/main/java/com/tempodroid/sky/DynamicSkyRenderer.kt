package com.tempodroid.sky

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.weather.WeatherCondition

/**
 * Renderer visual alinhado ao arquivo final do Figma.
 *
 * A camada base usa os dez rasters originais de DynamicSkyBackground. Sol,
 * nuvens, chuva, atmosfera e estrelas já fazem parte desses assets. A Lua
 * permanece astronômica/dinâmica, usando a textura, opacidades e escalas
 * extraídas das variantes MoonWeatherSky.
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
            val moonX = moon.normalizedX * w
            val moonY = moon.normalizedY * h * 0.70f
            // MoonVisual ocupa 48/58/66px no frame Figma de 160px. O disco
            // texturizado ocupa 68% do componente (radius = 34%).
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
            )
            redrawPhysicalCloudOcclusion(
                canvas = canvas,
                background = background,
                targetWidth = w,
                targetHeight = h,
                moonX = moonX,
                moonY = moonY,
                moonRadius = radius,
                condition = input.weatherCondition,
                cloudCover = input.cloudCover,
            )
        }

        // Mede o bitmap já composto (incluindo Lua/oclusão) e protege apenas
        // as regiões reais de texto. As imagens-fonte do Figma permanecem
        // intactas e o scrim varia continuamente conforme a luminância local.
        TextProtectionOverlay.draw(canvas, bitmap, w, h)
        return bitmap
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

    /**
     * As variantes finais WeatherWidget4x2 acrescentam nuvens físicas sobre a
     * Lua em estados com cobertura. Reutilizamos a própria imagem Figma na
     * região de oclusão para manter a textura atmosférica coerente.
     */
    private fun redrawPhysicalCloudOcclusion(
        canvas: Canvas,
        background: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        moonX: Float,
        moonY: Float,
        moonRadius: Float,
        condition: WeatherCondition,
        cloudCover: Int?,
    ) {
        val base = when (condition) {
            WeatherCondition.CLEAR -> 0f
            WeatherCondition.PARTLY_CLOUDY -> 0.28f
            WeatherCondition.CLOUDY -> 0.58f
            WeatherCondition.FOG -> 0.68f
            WeatherCondition.RAIN -> 0.72f
            WeatherCondition.HEAVY_RAIN -> 0.82f
            WeatherCondition.STORM -> 1f
        }
        if (base <= 0f) return
        val cover = ((cloudCover ?: (base * 100).toInt()).coerceIn(0, 100) / 100f)
        val fraction = (base * 0.65f + cover * 0.35f).coerceIn(0f, 1f)
        if (fraction >= 0.98f) return

        val left = moonX - moonRadius * 1.15f
        val right = moonX + moonRadius * 1.15f
        val top = moonY - moonRadius * 0.10f
        val bottom = moonY + moonRadius * (0.25f + 1.45f * fraction)
        canvas.save()
        canvas.clipRect(RectF(left, top, right, bottom))
        drawCenterCrop(canvas, background, targetWidth, targetHeight)
        canvas.restore()
    }
}
