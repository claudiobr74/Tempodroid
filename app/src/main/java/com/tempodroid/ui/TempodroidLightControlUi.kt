package com.tempodroid.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tempodroid.BuildConfig
import com.tempodroid.astronomy.MoonPhase
import com.tempodroid.weather.WeatherCodeMapper
import com.tempodroid.weather.WeatherCondition
import com.tempodroid.widget.SolarWeatherWidgetState
import com.tempodroid.widget.WidgetBitmaps
import com.tempodroid.widget.WidgetRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val LightBackground = Color(0xFFF7F9FC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceAlt = Color(0xFFF1F5F9)
private val LightActive = Color(0xFFE8F3FB)
private val LightAccent = Color(0xFF0284C7)
private val LightAccentStrong = Color(0xFF0369A1)
private val LightText = Color(0xFF111827)
private val LightTextSecondary = Color(0xFF667085)
private val LightTextMuted = Color(0xFF98A2B3)
private val LightBorder = Color(0xFFE4E7EC)
private val LightGood = Color(0xFF16A34A)
private val LightDanger = Color(0xFFDC2626)

private val LightControlColors = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceAlt,
    primary = LightAccent,
    onPrimary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
)

private enum class LightDestination { HOME, WIDGETS, SETTINGS, STATUS, PREVIEW }

@Composable
fun TempodroidLightControlApp(
    primaryState: SolarWeatherWidgetState,
    primaryWidgetId: Int?,
    installedWidgets: List<InstalledWidgetUi>,
    networkLabel: String,
    connectionMessage: String?,
    onRefreshAll: () -> Unit,
    onRefreshWidget: (Int) -> Unit,
    onConfigureWidget: (Int) -> Unit,
    onAddWidget: () -> Unit,
    onPrivacy: () -> Unit,
    onCredits: () -> Unit,
    onTestConnection: () -> Unit,
) {
    var destination by remember { mutableStateOf(LightDestination.HOME) }
    var removalHelp by remember { mutableStateOf<InstalledWidgetUi?>(null) }

    MaterialTheme(colorScheme = LightControlColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LightBackground,
            contentColor = LightText,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (destination) {
                        LightDestination.HOME -> LightDashboardScreen(
                            state = primaryState,
                            primaryWidgetId = primaryWidgetId,
                            widgets = installedWidgets,
                            onRefresh = onRefreshAll,
                            onConfigure = { primaryWidgetId?.let(onConfigureWidget) ?: onAddWidget() },
                            onShowWidgets = { destination = LightDestination.WIDGETS },
                        )

                        LightDestination.WIDGETS -> LightWidgetsScreen(
                            widgets = installedWidgets,
                            onAddWidget = onAddWidget,
                            onRefreshWidget = onRefreshWidget,
                            onConfigureWidget = onConfigureWidget,
                            onRemovalHelp = { removalHelp = it },
                        )

                        LightDestination.SETTINGS -> LightSettingsScreen(
                            state = primaryState,
                            primaryWidgetId = primaryWidgetId,
                            onConfigureWidget = onConfigureWidget,
                            onAddWidget = onAddWidget,
                            onStatus = { destination = LightDestination.STATUS },
                            onPreview = { destination = LightDestination.PREVIEW },
                            onPrivacy = onPrivacy,
                            onCredits = onCredits,
                        )

                        LightDestination.STATUS -> LightStatusScreen(
                            state = primaryState,
                            networkLabel = networkLabel,
                            connectionMessage = connectionMessage,
                            onRefresh = onRefreshAll,
                            onTestConnection = onTestConnection,
                            onBack = { destination = LightDestination.SETTINGS },
                        )

                        LightDestination.PREVIEW -> LightPreviewScreen(
                            base = primaryState,
                            onBack = { destination = LightDestination.SETTINGS },
                        )
                    }
                }

                LightBottomNavigation(
                    active = when (destination) {
                        LightDestination.WIDGETS -> LightDestination.WIDGETS
                        LightDestination.SETTINGS,
                        LightDestination.STATUS,
                        LightDestination.PREVIEW,
                        -> LightDestination.SETTINGS
                        else -> LightDestination.HOME
                    },
                    onSelect = { destination = it },
                )
            }
        }

        removalHelp?.let { widget ->
            AlertDialog(
                onDismissRequest = { removalHelp = null },
                title = { Text("Como remover o widget", color = LightText) },
                text = {
                    Text(
                        "A remoção é controlada pelo Android. Na tela inicial, mantenha pressionado o widget de ${widget.state.city} e toque em Remover.",
                        color = LightTextSecondary,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { removalHelp = null }) { Text("Entendi") }
                },
            )
        }
    }
}

@Composable
private fun LightDashboardScreen(
    state: SolarWeatherWidgetState,
    primaryWidgetId: Int?,
    widgets: List<InstalledWidgetUi>,
    onRefresh: () -> Unit,
    onConfigure: () -> Unit,
    onShowWidgets: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Tempodroid", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = LightText)
            Text("Clima, Sol e Lua", color = LightTextSecondary, fontSize = 14.sp)
        }

        RealWidgetPreview(state = state, height = 184.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (state.hasWeatherError) LightDanger else LightGood, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(lightUpdatedAgo(state.updatedAt), color = LightTextSecondary, fontSize = 13.sp)
            }
            if (primaryWidgetId == null) {
                Text("Nenhum widget instalado", color = LightTextMuted, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LightActionButton(
                label = "Atualizar",
                icon = Icons.Outlined.Refresh,
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                filled = false,
            )
            LightActionButton(
                label = if (primaryWidgetId == null) "Adicionar widget" else "Configurar widget",
                icon = Icons.Outlined.Settings,
                onClick = onConfigure,
                modifier = Modifier.weight(1f),
                filled = true,
            )
        }

        LightWeatherCard(state)
        LightSolarLunarCard(state)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Widgets ativos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LightText)
                Text(
                    "Gerenciar",
                    color = LightAccentStrong,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onShowWidgets),
                )
            }
            val active = primaryWidgetId?.let { id -> widgets.firstOrNull { it.appWidgetId == id } }
                ?: widgets.firstOrNull()
            if (active != null) {
                LightWidgetSummaryRow(active.state, onShowWidgets)
            } else {
                EmptyInlineCard("Adicione um widget 4×2 para começar.", onShowWidgets)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LightWeatherCard(state: SolarWeatherWidgetState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(20.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${state.temperature ?: "--"}°", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = LightText)
                Text(
                    "Sensação ${state.apparentTemperature?.let { "$it°" } ?: "--"}",
                    color = LightTextSecondary,
                    fontSize = 13.sp,
                )
            }
            Column(
                modifier = Modifier
                    .background(LightActive, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    WeatherCodeMapper.labelPt(state.condition, state.isDay),
                    color = LightAccentStrong,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                )
                Text(state.city, color = LightTextSecondary, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LightDetailCell(Icons.Outlined.WaterDrop, "Umidade", state.relativeHumidity?.let { "$it%" } ?: "--", Modifier.weight(1f))
            LightDetailCell(Icons.Outlined.Air, "Vento", state.windSpeedKmh?.let { "$it km/h" } ?: "--", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LightDetailCell(Icons.Outlined.Cloud, "Chuva", state.rainChance?.let { "$it%" } ?: "--", Modifier.weight(1f))
            LightDetailCell(Icons.Outlined.Speed, "Pressão", state.surfacePressureHpa?.let { "${it.roundToInt()} hPa" } ?: "--", Modifier.weight(1f))
        }
    }
}

@Composable
private fun LightDetailCell(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(LightSurfaceAlt, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = LightAccent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text(label, color = LightTextSecondary, fontSize = 11.sp)
            Text(value, color = LightText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LightSolarLunarCard(state: SolarWeatherWidgetState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(20.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Sol e Lua", color = LightText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Nascer ${lightFormatTime(state.sunrise)}  •  Ocaso ${lightFormatTime(state.sunset)}",
                color = LightTextSecondary,
                fontSize = 13.sp,
            )
            Text(
                "Lua ${lightMoonPhasePt(state.moonPhase)} · ${(state.moonIllumination * 100).roundToInt()}% iluminada",
                color = LightTextSecondary,
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier.size(52.dp).background(LightActive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(lightMoonGlyph(state.moonPhase), fontSize = 29.sp)
        }
    }
}

@Composable
private fun LightWidgetSummaryRow(state: SolarWeatherWidgetState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(16.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = LightAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(state.city, color = LightText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                state.temperature?.let { "$it° · ${WeatherCodeMapper.labelPt(state.condition, state.isDay)}" } ?: "Sem dados meteorológicos",
                color = LightTextSecondary,
                fontSize = 12.sp,
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = LightTextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyInlineCard(message: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(16.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = LightTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = LightTextMuted)
    }
}

@Composable
private fun LightWidgetsScreen(
    widgets: List<InstalledWidgetUi>,
    onAddWidget: () -> Unit,
    onRefreshWidget: (Int) -> Unit,
    onConfigureWidget: (Int) -> Unit,
    onRemovalHelp: (InstalledWidgetUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Widgets", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = LightText)
                Text("Gerencie os widgets 4×2 instalados", color = LightTextSecondary, fontSize = 14.sp)
            }
            CompactPrimaryAction("+ Novo", onAddWidget)
        }

        if (widgets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightSurface, RoundedCornerShape(20.dp))
                    .border(1.dp, LightBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Cloud, null, tint = LightAccent, modifier = Modifier.size(42.dp))
                Text("Nenhum widget instalado", color = LightText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Adicione o Tempodroid 4×2 à tela inicial para começar.",
                    color = LightTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                CompactPrimaryAction("Adicionar widget", onAddWidget)
            }
        } else {
            widgets.forEach { widget ->
                LightWidgetCard(widget, onRefreshWidget, onConfigureWidget, onRemovalHelp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LightWidgetCard(
    widget: InstalledWidgetUi,
    onRefreshWidget: (Int) -> Unit,
    onConfigureWidget: (Int) -> Unit,
    onRemovalHelp: (InstalledWidgetUi) -> Unit,
) {
    val state = widget.state
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(20.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(state.city, color = LightText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Atualização ${lightUpdatedAgoShort(state.updatedAt)}",
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(state.temperature?.let { "$it°C" } ?: "--°C", color = LightAccentStrong, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    WeatherCodeMapper.labelPt(state.condition, state.isDay),
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LightActionButton(
                label = "Atualizar",
                icon = Icons.Outlined.Refresh,
                onClick = { onRefreshWidget(widget.appWidgetId) },
                modifier = Modifier.weight(1f),
                filled = false,
            )
            LightActionButton(
                label = "Configurar",
                icon = Icons.Outlined.Settings,
                onClick = { onConfigureWidget(widget.appWidgetId) },
                modifier = Modifier.weight(1f),
                filled = true,
            )
        }

        Row(
            modifier = Modifier
                .clickable { onRemovalHelp(widget) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Delete, null, tint = LightTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Como remover da tela inicial", color = LightTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LightSettingsScreen(
    state: SolarWeatherWidgetState,
    primaryWidgetId: Int?,
    onConfigureWidget: (Int) -> Unit,
    onAddWidget: () -> Unit,
    onStatus: () -> Unit,
    onPreview: () -> Unit,
    onPrivacy: () -> Unit,
    onCredits: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column {
            Text("Ajustes", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = LightText)
            Text("Apenas opções que realmente funcionam no aplicativo", color = LightTextSecondary, fontSize = 14.sp)
        }

        LightSettingsSection("Widget") {
            LightSettingRow(
                label = "Cidade e localização",
                detail = state.city.ifBlank { "Escolher cidade" },
                icon = Icons.Outlined.LocationOn,
                onClick = { primaryWidgetId?.let(onConfigureWidget) ?: onAddWidget() },
            )
        }

        LightSettingsSection("Ferramentas") {
            LightSettingRow("Status do sistema", "Rede, cache e atualização", Icons.Outlined.Info, onStatus)
            LightDivider()
            LightSettingRow("Prévia do widget", "Testar clima, período e fase da Lua", Icons.Outlined.Science, onPreview)
        }

        LightSettingsSection("Privacidade e dados") {
            LightSettingRow("Política de privacidade", "Como o Tempodroid usa seus dados", Icons.Outlined.Info, onPrivacy)
            LightDivider()
            LightSettingRow("Fonte dos dados", "Open-Meteo", Icons.Outlined.Info, onCredits)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface, RoundedCornerShape(16.dp))
                .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text("Versão", color = LightTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text("Tempodroid v${BuildConfig.VERSION_NAME}", color = LightText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LightSettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, color = LightAccentStrong, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface, RoundedCornerShape(18.dp))
                .border(1.dp, LightBorder, RoundedCornerShape(18.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun LightSettingRow(label: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(LightActive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = LightAccentStrong, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = LightText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(detail, color = LightTextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = LightTextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun LightDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LightBorder))
}

@Composable
private fun LightStatusScreen(
    state: SolarWeatherWidgetState,
    networkLabel: String,
    connectionMessage: String?,
    onRefresh: () -> Unit,
    onTestConnection: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val permission = if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) "Concedida" else "Não concedida"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LightBackHeader("Status do sistema", "Diagnóstico e integridade do Tempodroid", onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (state.hasWeatherError) Color(0xFFFFF1F2) else Color(0xFFF0FDF4), RoundedCornerShape(18.dp))
                .border(1.dp, if (state.hasWeatherError) Color(0xFFFECACA) else Color(0xFFBBF7D0), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(11.dp).background(if (state.hasWeatherError) LightDanger else LightGood, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (state.hasWeatherError) "Usando dados em cache" else "Funcionando normalmente",
                    color = LightText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (state.hasWeatherError) "A última atualização do clima falhou." else "Dados locais e atualização estão ativos.",
                    color = LightTextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightSurface, RoundedCornerShape(18.dp))
                .border(1.dp, LightBorder, RoundedCornerShape(18.dp)),
        ) {
            LightStatusRow("Última atualização", lightUpdatedAgoShort(state.updatedAt))
            LightStatusRow("Atualização automática", "A cada ~30 min")
            LightStatusRow("Provedor", "Open-Meteo")
            LightStatusRow("Cidade", state.city)
            LightStatusRow("Cache meteorológico", if (state.updatedAt == null) "Sem dados" else "Ativo")
            LightStatusRow("Rede", networkLabel)
            LightStatusRow("Localização aproximada", permission, last = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LightActionButton("Atualizar agora", Icons.Outlined.Refresh, onRefresh, Modifier.weight(1f), filled = true)
            LightActionButton("Testar conexão", Icons.Outlined.NetworkCheck, onTestConnection, Modifier.weight(1f), filled = false)
        }

        connectionMessage?.let {
            Text(it, color = LightAccentStrong, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LightStatusRow(label: String, value: String, last: Boolean = false) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = LightTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                color = LightText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (!last) LightDivider()
    }
}

@Composable
private fun LightPreviewScreen(base: SolarWeatherWidgetState, onBack: () -> Unit) {
    var condition by remember { mutableStateOf(WeatherCondition.CLEAR) }
    var period by remember { mutableStateOf("Entardecer") }
    var moonPhase by remember { mutableStateOf(MoonPhase.WAXING_CRESCENT) }

    val previewTime = when (period) {
        "Dia" -> LocalTime.of(12, 0)
        "Noite" -> LocalTime.of(22, 0)
        else -> LocalTime.of(18, 0)
    }
    val preview = base.copy(
        condition = condition,
        cloudCover = when (condition) {
            WeatherCondition.CLEAR -> 4
            WeatherCondition.PARTLY_CLOUDY -> 42
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> 86
            else -> 95
        },
        precipitation = when (condition) {
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> 2.5
            WeatherCondition.STORM -> 5.0
            else -> 0.0
        },
        rainChance = when (condition) {
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> 85
            WeatherCondition.STORM -> 95
            else -> 10
        },
        moonPhase = moonPhase,
        moonIllumination = lightMoonIlluminationFor(moonPhase),
        sunrise = LocalTime.of(6, 10),
        solarNoon = LocalTime.of(12, 10),
        sunset = LocalTime.of(18, 13),
        moonrise = LocalTime.of(8, 0),
        moonset = LocalTime.of(23, 30),
        isDay = period != "Noite",
        displayTime = previewTime,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LightBackHeader("Prévia do widget", "A prévia usa o renderer real do Tempodroid", onBack)

        RealWidgetPreview(
            state = preview,
            height = 188.dp,
            useLiveAstronomy = false,
            renderToken = "${condition.name}|$period|${moonPhase.name}",
        )

        LightChoiceSection("Condição do tempo") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LightChoiceChip("Limpo", condition == WeatherCondition.CLEAR, Modifier.weight(1f)) { condition = WeatherCondition.CLEAR }
                LightChoiceChip("Parcial", condition == WeatherCondition.PARTLY_CLOUDY, Modifier.weight(1f)) { condition = WeatherCondition.PARTLY_CLOUDY }
                LightChoiceChip("Nublado", condition == WeatherCondition.CLOUDY, Modifier.weight(1f)) { condition = WeatherCondition.CLOUDY }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LightChoiceChip("Chuva", condition == WeatherCondition.RAIN, Modifier.weight(1f)) { condition = WeatherCondition.RAIN }
                LightChoiceChip("Tempestade", condition == WeatherCondition.STORM, Modifier.weight(1f)) { condition = WeatherCondition.STORM }
            }
        }

        LightChoiceSection("Período") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Dia", "Entardecer", "Noite").forEach { item ->
                    LightChoiceChip(item, period == item, Modifier.weight(1f)) { period = item }
                }
            }
        }

        LightChoiceSection("Fase da Lua") {
            val rows = listOf(
                listOf(
                    "Nova" to MoonPhase.NEW_MOON,
                    "Crescente" to MoonPhase.WAXING_CRESCENT,
                    "1/4 Cresc." to MoonPhase.FIRST_QUARTER,
                    "Gibosa C." to MoonPhase.WAXING_GIBBOUS,
                ),
                listOf(
                    "Cheia" to MoonPhase.FULL_MOON,
                    "Gibosa M." to MoonPhase.WANING_GIBBOUS,
                    "1/4 Ming." to MoonPhase.LAST_QUARTER,
                    "Minguante" to MoonPhase.WANING_CRESCENT,
                ),
            )
            rows.forEachIndexed { index, row ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (label, phase) ->
                        LightChoiceChip(label, moonPhase == phase, Modifier.weight(1f), compact = true) { moonPhase = phase }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LightChoiceSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface, RoundedCornerShape(18.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(title, color = LightText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun LightChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(if (selected) LightAccent else LightSurfaceAlt, RoundedCornerShape(12.dp))
            .border(1.dp, if (selected) LightAccent else LightBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else LightText,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class LightPreviewResult(val bitmaps: WidgetBitmaps? = null, val error: String? = null)

@Composable
private fun RealWidgetPreview(
    state: SolarWeatherWidgetState,
    height: Dp,
    useLiveAstronomy: Boolean = true,
    renderToken: String = "live",
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFCBD5E1)),
    ) {
        val widthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val heightPx = with(density) { height.roundToPx() }.coerceAtLeast(1)
        val result by produceState(
            initialValue = LightPreviewResult(),
            state,
            widthPx,
            heightPx,
            useLiveAstronomy,
            renderToken,
        ) {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    WidgetRenderer.render(context, state, widthPx, heightPx, useLiveAstronomy)
                }.fold(
                    onSuccess = { LightPreviewResult(bitmaps = it) },
                    onFailure = { LightPreviewResult(error = it.message ?: "Falha ao gerar a prévia") },
                )
            }
        }

        val rendered = result.bitmaps
        if (rendered == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    result.error ?: "Gerando prévia…",
                    color = if (result.error == null) LightTextSecondary else LightDanger,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@BoxWithConstraints
        }

        Image(
            rendered.sky.asImageBitmap(),
            contentDescription = "Prévia real do widget",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        WeatherCodeMapper.labelPt(state.condition, state.isDay),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("${state.temperature ?: "--"}°", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(lightFormatTime(state.displayTime ?: LocalTime.now()), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(state.displayDate, color = Color.White.copy(alpha = 0.86f), fontSize = 10.sp)
                    Text(state.city, color = Color.White.copy(alpha = 0.86f), fontSize = 10.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Image(
                rendered.timeline.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                contentScale = ContentScale.FillBounds,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    "Máx ${state.high?.let { "$it°" } ?: "--"} · Mín ${state.low?.let { "$it°" } ?: "--"} · ${state.rainChance?.let { "$it% chuva" } ?: "--"}",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun LightBackHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ArrowBack, null, tint = LightAccentStrong, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Ajustes", color = LightAccentStrong, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(title, color = LightText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = LightTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun LightActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    filled: Boolean,
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .then(
                if (filled) Modifier.background(LightAccent, RoundedCornerShape(14.dp))
                else Modifier.background(LightSurface, RoundedCornerShape(14.dp)).border(1.dp, LightBorder, RoundedCornerShape(14.dp)),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (filled) Color.White else LightAccentStrong, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (filled) Color.White else LightText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactPrimaryAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(LightAccent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LightBottomNavigation(active: LightDestination, onSelect: (LightDestination) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(LightSurface)
            .border(1.dp, LightBorder)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LightBottomNavItem("Início", Icons.Outlined.Home, active == LightDestination.HOME) { onSelect(LightDestination.HOME) }
        LightBottomNavItem("Widgets", Icons.Outlined.Apps, active == LightDestination.WIDGETS) { onSelect(LightDestination.WIDGETS) }
        LightBottomNavItem("Ajustes", Icons.Outlined.Settings, active == LightDestination.SETTINGS) { onSelect(LightDestination.SETTINGS) }
    }
}

@Composable
private fun LightBottomNavItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(82.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(30.dp)
                .then(if (active) Modifier.background(LightActive, RoundedCornerShape(18.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (active) LightAccentStrong else LightTextMuted, modifier = Modifier.size(21.dp))
        }
        Text(
            label,
            color = if (active) LightText else LightTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun lightFormatTime(time: LocalTime?): String =
    time?.format(DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))) ?: "--:--"

private fun lightUpdatedAgo(updatedAt: Instant?): String = "Atualizado ${lightUpdatedAgoShort(updatedAt)}"

private fun lightUpdatedAgoShort(updatedAt: Instant?): String {
    if (updatedAt == null) return "sem dados"
    val minutes = Duration.between(updatedAt, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há $minutes min"
        else -> "há ${minutes / 60}h"
    }
}

private fun lightMoonPhasePt(phase: MoonPhase): String = when (phase) {
    MoonPhase.NEW_MOON -> "Nova"
    MoonPhase.WAXING_CRESCENT -> "Crescente"
    MoonPhase.FIRST_QUARTER -> "Quarto Crescente"
    MoonPhase.WAXING_GIBBOUS -> "Gibosa Crescente"
    MoonPhase.FULL_MOON -> "Cheia"
    MoonPhase.WANING_GIBBOUS -> "Gibosa Minguante"
    MoonPhase.LAST_QUARTER -> "Quarto Minguante"
    MoonPhase.WANING_CRESCENT -> "Minguante"
}

private fun lightMoonGlyph(phase: MoonPhase): String = when (phase) {
    MoonPhase.NEW_MOON -> "🌑"
    MoonPhase.WAXING_CRESCENT -> "🌒"
    MoonPhase.FIRST_QUARTER -> "🌓"
    MoonPhase.WAXING_GIBBOUS -> "🌔"
    MoonPhase.FULL_MOON -> "🌕"
    MoonPhase.WANING_GIBBOUS -> "🌖"
    MoonPhase.LAST_QUARTER -> "🌗"
    MoonPhase.WANING_CRESCENT -> "🌘"
}

private fun lightMoonIlluminationFor(phase: MoonPhase): Double = when (phase) {
    MoonPhase.NEW_MOON -> 0.01
    MoonPhase.WAXING_CRESCENT, MoonPhase.WANING_CRESCENT -> 0.25
    MoonPhase.FIRST_QUARTER, MoonPhase.LAST_QUARTER -> 0.5
    MoonPhase.WAXING_GIBBOUS, MoonPhase.WANING_GIBBOUS -> 0.75
    MoonPhase.FULL_MOON -> 1.0
}
