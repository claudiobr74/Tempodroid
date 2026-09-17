package com.tempodroid.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tempodroid.R
import com.tempodroid.accessibility.WidgetContentDescription
import com.tempodroid.di.AppContainer
import com.tempodroid.fixtures.DailySummaryFormatter
import com.tempodroid.ui.MainActivity
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.ui.WidgetLayoutMode
import com.tempodroid.ui.WidgetTheme
import com.tempodroid.weather.WeatherCodeMapper

/**
 * Widget 4×2 SolarWeatherWidget.
 *
 * Relógio: [WidgetTheme.CLOCK_STRATEGY]
 */
class SolarWeatherWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(WidgetDimensions.COMPACT_WIDTH_DP.dp, WidgetDimensions.COMPACT_HEIGHT_DP.dp),
            DpSize(WidgetDimensions.REFERENCE_WIDTH_DP.dp, WidgetDimensions.REFERENCE_HEIGHT_DP.dp),
            DpSize(WidgetDimensions.EXPANDED_WIDTH_DP.dp, WidgetDimensions.EXPANDED_HEIGHT_DP.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = AppContainer.get(context).cache.loadState()
        provideContent {
            GlanceTheme {
                SolarWeatherContent(state)
            }
        }
    }
}

@Composable
private fun SolarWeatherContent(state: SolarWeatherWidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceAtLeast(240)
    val heightPx = (size.height.value * density).toInt().coerceAtLeast(120)
    val bitmaps = WidgetRenderer.render(context, state, widthPx, heightPx)
    val compact = bitmaps.mode == WidgetLayoutMode.COMPACT
    val description = WidgetContentDescription.forState(state)
    val home = actionStartActivity(deepLink(context, "tempodroid://home"))
    val weather = actionStartActivity(deepLink(context, "tempodroid://weather"))
    val forecast = actionStartActivity(deepLink(context, "tempodroid://forecast"))
    val white = ColorProvider(Color.White)
    val secondary = ColorProvider(Color(0xE6FFFFFF))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WidgetDimensions.CORNER_RADIUS_DP.dp)
            .clickable(home),
    ) {
        Image(
            provider = ImageProvider(bitmaps.sky),
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(WidgetDimensions.PADDING_DP.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(modifier = GlanceModifier.clickable(weather)) {
                    Text(
                        text = WeatherCodeMapper.labelPt(state.condition, state.isDay),
                        style = TextStyle(color = secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = state.temperature?.let { "$it°" } ?: "--°",
                        style = TextStyle(
                            color = white,
                            fontSize = if (compact) 28.sp else 34.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                Column {
                    AndroidRemoteViews(
                        remoteViews = RemoteViews(context.packageName, R.layout.widget_text_clock),
                    )
                    if (!compact) {
                        Text(
                            text = state.displayDate,
                            style = TextStyle(color = secondary, fontSize = 11.sp),
                        )
                        Text(
                            text = "◉  ${state.city}",
                            style = TextStyle(color = secondary, fontSize = 11.sp),
                        )
                    }
                }
            }

            Spacer(GlanceModifier.defaultWeight())

            Image(
                provider = ImageProvider(bitmaps.timeline),
                contentDescription = "Linha do tempo solar e lunar",
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(if (compact) 38.dp else WidgetDimensions.TIMELINE_HEIGHT_DP.dp),
            )
            Spacer(GlanceModifier.height(4.dp))

            val summary = if (compact) {
                DailySummaryFormatter.compact(state.high, state.low, state.rainChance)
            } else {
                DailySummaryFormatter.normal(state.high, state.low, state.rainChance)
            }
            val statusSuffix = when {
                state.hasWeatherError -> "  ·  offline"
                state.isStale -> "  ·  desatualizado"
                else -> ""
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(forecast),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = summary + statusSuffix,
                    style = TextStyle(
                        color = white,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(WidgetTheme.SUMMARY_FILL)))
                        .cornerRadius(50.dp)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

private fun deepLink(context: Context, uri: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        data = Uri.parse(uri)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

class SolarWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SolarWeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateCoordinator.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateCoordinator.cancelIfNoWidgets(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateCoordinator.ensurePeriodic(context)
    }
}
