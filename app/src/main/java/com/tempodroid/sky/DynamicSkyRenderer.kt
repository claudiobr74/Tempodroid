package com.tempodroid.sky

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.tempodroid.astronomy.MoonRenderStyle
import com.tempodroid.astronomy.MoonTerminator
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.weather.WeatherCondition

/**
 * Renderer visual alinhado ao arquivo final do Figma.
 *
 * O céu não é mais sintetizado por gradientes/nuvens procedurais. A camada
 * base usa os dez rasters originais de DynamicSkyBackground. Sol, nuvens,
 * chuva, atmosfera e estrelas já fazem parte desses assets. A Lua permanece
 * astronômica/dinâmica e usa a textura original de MoonVisual.
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

        val palette = SkyPalette.resolve(
            period = period,
            weather = input.weatherCondition,
            moonPhase = input.moonPhase,
            moonIllumination = input.moonIllumination,
        )

        val moon = input.moonPosition
        val isDay = FigmaVisualStateResolver.matrixPeriod(period) == FigmaMatrixPeriod.DAY
        val moonVisibility = MoonTerminator.skyVisibility(
            aboveHorizon = moon?.aboveHorizon == true,
            isDay = isDay,
            weather = input.weatherCondition,
        )

        if (moon != null && moonVisibility > 0f) {
            val moonX = moon.normalizedX * w
            val moonY = moon.normalizedY * h * 0.70f
            val radius = h * if (isDay) 0.07f else 0.10f
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

        // No Figma final esta é uma camada independente, acima do céu/Lua e
        // abaixo dos textos do widget.
        TextProtectionOverlay.draw(canvas, w, h, palette.scrimTop, palette.scrimBottom)
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
     * MoonWeatherSky no Figma possui uma camada "Physical cloud occlusion".
     * Como os próprios clouds já estão no raster base, redesenhamos localmente
     * a mesma imagem acima da Lua para recuperar a sensação de nuvem na frente,
     * sem inventar uma segunda textura.
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
        if (fraction >= 0.98f) return // Lua já está invisível em tempestade.

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
