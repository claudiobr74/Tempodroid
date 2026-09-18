package com.tempodroid.sky

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import com.tempodroid.R

/**
 * Carregador dos assets raster originais do arquivo final do Figma.
 * Os backgrounds ficam em drawable-nodpi para evitar reamostragem automática
 * por densidade; o renderer decide o crop para o tamanho real do widget.
 */
internal object FigmaVisualAssets {
    private val skyCache = LruCache<Int, Bitmap>(3)

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
        FigmaSkyState.CLEAR_DAY -> R.drawable.figma_sky_clear_day
        FigmaSkyState.PARTLY_CLOUDY -> R.drawable.figma_sky_partly_cloudy
        FigmaSkyState.CLOUDY -> R.drawable.figma_sky_cloudy
        FigmaSkyState.RAIN -> R.drawable.figma_sky_rain
        FigmaSkyState.STORM -> R.drawable.figma_sky_storm
        FigmaSkyState.SUNRISE -> R.drawable.figma_sky_sunrise
        FigmaSkyState.SUNSET -> R.drawable.figma_sky_sunset
        FigmaSkyState.CLEAR_NIGHT -> R.drawable.figma_sky_clear_night
        FigmaSkyState.CLOUDY_NIGHT -> R.drawable.figma_sky_cloudy_night
        FigmaSkyState.RAIN_NIGHT -> R.drawable.figma_sky_rain_night
    }
}
