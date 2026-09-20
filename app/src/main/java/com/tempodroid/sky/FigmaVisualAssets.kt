package com.tempodroid.sky

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import com.tempodroid.R

/**
 * Carregador dos 15 fundos raster atuais do DynamicSkyBackground no Figma.
 * Os arquivos ficam em drawable-nodpi; o renderer controla o center-crop para
 * as dimensões reais do widget sem reamostragem automática por densidade.
 */
internal object FigmaVisualAssets {
    private val skyCache = LruCache<Int, Bitmap>(4)

    @Volatile
    private var moonSurfaceCache: Bitmap? = null

    fun sky(context: Context, state: FigmaSkyState): Bitmap {
        val resourceId = skyResource(state)
        skyCache.get(resourceId)?.let { return it }
        val decoded = requireNotNull(BitmapFactory.decodeResource(context.resources, resourceId)) {
            "Falha ao carregar background do Figma: $state"
        }
        skyCache.put(resourceId, decoded)
        return decoded
    }

    fun moonSurface(context: Context): Bitmap {
        moonSurfaceCache?.let { return it }
        return synchronized(this) {
            moonSurfaceCache ?: requireNotNull(
                BitmapFactory.decodeResource(context.resources, R.drawable.figma_moon_surface),
            ) { "Falha ao carregar textura lunar do Figma" }
                .also { moonSurfaceCache = it }
        }
    }

    @DrawableRes
    private fun skyResource(state: FigmaSkyState): Int = when (state) {
        FigmaSkyState.DAY_CLEAR -> R.drawable.figma_sky_day_clear
        FigmaSkyState.DAY_PARTLY_CLOUDY -> R.drawable.figma_sky_day_partly_cloudy
        FigmaSkyState.DAY_CLOUDY -> R.drawable.figma_sky_day_cloudy
        FigmaSkyState.DAY_RAIN -> R.drawable.figma_sky_day_rain
        FigmaSkyState.DAY_STORM -> R.drawable.figma_sky_day_storm
        FigmaSkyState.SUNSET_CLEAR -> R.drawable.figma_sky_sunset_clear
        FigmaSkyState.SUNSET_PARTLY_CLOUDY -> R.drawable.figma_sky_sunset_partly_cloudy
        FigmaSkyState.SUNSET_CLOUDY -> R.drawable.figma_sky_sunset_cloudy
        FigmaSkyState.SUNSET_RAIN -> R.drawable.figma_sky_sunset_rain
        FigmaSkyState.SUNSET_STORM -> R.drawable.figma_sky_sunset_storm
        FigmaSkyState.NIGHT_CLEAR -> R.drawable.figma_sky_night_clear
        FigmaSkyState.NIGHT_PARTLY_CLOUDY -> R.drawable.figma_sky_night_partly_cloudy
        FigmaSkyState.NIGHT_CLOUDY -> R.drawable.figma_sky_night_cloudy
        FigmaSkyState.NIGHT_RAIN -> R.drawable.figma_sky_night_rain
        FigmaSkyState.NIGHT_STORM -> R.drawable.figma_sky_night_storm
    }
}
