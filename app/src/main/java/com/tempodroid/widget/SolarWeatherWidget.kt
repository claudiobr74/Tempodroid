package com.tempodroid.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import com.tempodroid.ui.WidgetConfigurationActivity
import com.tempodroid.ui.WidgetDimensions
import com.tempodroid.ui.WidgetLayoutMode
import com.tempodroid.ui.WidgetTheme
import com.tempodroid.weather.WeatherCodeMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId

/** Widget 4×2 SolarWeatherWidget. */
class SolarWeatherWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(WidgetDimensions.COMPACT_WIDTH_DP.dp, WidgetDimensions.COMPACT_HEIGHT_DP.dp),
            DpSize(WidgetDimensions.REFERENCE_WIDTH_DP.dp, WidgetDimensions.REFERENCE_HEIGHT_DP.dp),
            DpSize(WidgetDimensions.EXPANDED_WIDTH_DP.dp, WidgetDimensions.EXPANDED_HEIGHT_DP.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val state = AppContainer.get(context).cache.loadState(appWidgetId)
        provideContent {
            GlanceTheme {
                SolarWeatherContent(state, appWidgetId)
            }
        }
    }
}

@Composable
private fun SolarWeatherContent(state: SolarWeatherWidgetState, appWidgetId: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val widthPx = (size.width.value * density).toInt().coerceAtLeast(240)
    val heightPx = (size.height.value * density).toInt().coerceAtLeast(120)
    val bitmaps = WidgetRenderer.render(context, state, widthPx, heightPx)
    val compact = bitmaps.mode == WidgetLayoutMode.COMPACT
    val zone = runCatching { ZoneId.of(state.timezoneId) }.getOrDefault(ZoneId.systemDefault())
    val description = WidgetContentDescription.forState(state, LocalTime.now(zone))
    val configure = actionStartActivity(configurationIntent(context, appWidgetId))
    val openApp = actionStartActivity(appIntent(context, appWidgetId))
    val white = ColorProvider(Color.White)
    val secondary = ColorProvider(Color(WidgetTheme.TEXT_SECONDARY))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WidgetDimensions.CORNER_RADIUS_DP.dp)
            .clickable(configure),
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
                Column(modifier = GlanceModifier.clickable(openApp)) {
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
                        remoteViews = liveClockRemoteViews(
                            context = context,
                            layoutId = R.layout.widget_text_clock,
                            viewId = R.id.widget_clock,
                            timezoneId = state.timezoneId,
                        ),
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

            val timelineHeightDp = if (compact) 38f else WidgetDimensions.TIMELINE_HEIGHT_DP.toFloat()
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(timelineHeightDp.dp),
            ) {
                Image(
                    provider = ImageProvider(bitmaps.timeline),
                    contentDescription = "Linha do tempo solar e lunar",
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.fillMaxSize(),
                )

                // O texto HH:mm não pertence mais ao bitmap. TextClock atualiza
                // sozinho a cada minuto, exatamente como o relógio superior.
                bitmaps.nowClockPlacement?.let { placement ->
                    val innerWidthDp = (
                        size.width.value - WidgetDimensions.PADDING_DP * 2f
                    ).coerceAtLeast(120f)
                    val clockWidthDp = 56f
                    val startDp = (
                        placement.centerXFraction * innerWidthDp - clockWidthDp / 2f
                    ).coerceIn(0f, (innerWidthDp - clockWidthDp).coerceAtLeast(0f))
                    val topDp = (
                        placement.topFraction * timelineHeightDp - 1f
                    ).coerceIn(0f, (timelineHeightDp - 18f).coerceAtLeast(0f))

                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Spacer(GlanceModifier.height(topDp.dp))
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            Spacer(GlanceModifier.width(startDp.dp))
                            AndroidRemoteViews(
                                remoteViews = liveClockRemoteViews(
                                    context = context,
                                    layoutId = R.layout.widget_timeline_text_clock,
                                    viewId = R.id.widget_timeline_clock,
                                    timezoneId = state.timezoneId,
                                ),
                            )
                        }
                    }
                }
            }
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
                    .clickable(openApp),
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

private fun liveClockRemoteViews(
    context: Context,
    layoutId: Int,
    viewId: Int,
    timezoneId: String,
): RemoteViews = RemoteViews(context.packageName, layoutId).apply {
    // TextClock aceita setTimeZone(String) via RemoteViews. Isso mantém os dois
    // relógios sincronizados com o mesmo fuso usado pela astronomia do widget.
    setString(viewId, "setTimeZone", timezoneId)
}

private fun configurationIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, WidgetConfigurationActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(WidgetConfigurationActivity.EXTRA_EDIT_MODE, true)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun appIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

class SolarWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SolarWeatherWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateCoordinator.ensurePeriodic(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cache = AppContainer.get(context).cache
                appWidgetIds.forEach { cache.deleteWidgetState(it) }
                WidgetUpdateCoordinator.cancelIfNoWidgets(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateCoordinator.cancelIfNoWidgets(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateCoordinator.ensurePeriodic(context)
    }
}
