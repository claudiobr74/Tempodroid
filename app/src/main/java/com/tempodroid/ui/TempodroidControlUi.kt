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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
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

private val AppBackground = Color(0xFF0A0D16)
private val SurfacePrimary = Color(0xFF111622)
private val SurfaceSecondary = Color(0xFF161D2F)
private val ActiveSurface = Color(0xFF1F293D)
private val Accent = Color(0xFF38BDF8)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF94A3B8)
private val TextMuted = Color(0xFF64748B)
private val Good = Color(0xFF10B981)
private val Danger = Color(0xFFEF4444)
private val Hairline = Color.White.copy(alpha = 0.05f)

private val ControlColors = darkColorScheme(
    background = AppBackground,
    surface = SurfacePrimary,
    primary = Accent,
    onPrimary = AppBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

data class InstalledWidgetUi(
    val appWidgetId: Int,
    val state: SolarWeatherWidgetState,
)

enum class ControlDestination { HOME, WIDGETS, SETTINGS, STATUS, VISUAL_TEST }

@Composable
fun TempodroidControlApp(
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
    var destination by remember { mutableStateOf(ControlDestination.HOME) }
    var removalHelp by remember { mutableStateOf<InstalledWidgetUi?>(null) }

    MaterialTheme(colorScheme = ControlColors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .systemBarsPadding(),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (destination) {
                    ControlDestination.HOME -> DashboardScreen(
                        state = primaryState,
                        primaryWidgetId = primaryWidgetId,
                        widgets = installedWidgets,
                        onRefresh = onRefreshAll,
                        onCity = { primaryWidgetId?.let(onConfigureWidget) ?: onAddWidget() },
                        onLocation = { primaryWidgetId?.let(onConfigureWidget) ?: onAddWidget() },
                        onConfigure = { primaryWidgetId?.let(onConfigureWidget) ?: onAddWidget() },
                        onShowWidgets = { destination = ControlDestination.WIDGETS },
                    )

                    ControlDestination.WIDGETS -> WidgetsScreen(
                        widgets = installedWidgets,
                        onAddWidget = onAddWidget,
                        onRefreshWidget = onRefreshWidget,
                        onConfigureWidget = onConfigureWidget,
                        onRemovalHelp = { removalHelp = it },
                    )

                    ControlDestination.SETTINGS -> SettingsScreen(
                        state = primaryState,
                        primaryWidgetId = primaryWidgetId,
                        onConfigureWidget = onConfigureWidget,
                        onAddWidget = onAddWidget,
                        onStatus = { destination = ControlDestination.STATUS },
                        onVisualTest = { destination = ControlDestination.VISUAL_TEST },
                        onPrivacy = onPrivacy,
                        onCredits = onCredits,
                    )

                    ControlDestination.STATUS -> StatusScreen(
                        state = primaryState,
                        networkLabel = networkLabel,
                        connectionMessage = connectionMessage,
                        onRefresh = onRefreshAll,
                        onTestConnection = onTestConnection,
                    )

                    ControlDestination.VISUAL_TEST -> VisualTestScreen(primaryState)
                }
            }

            BottomNavigation(
                active = when (destination) {
                    ControlDestination.WIDGETS -> ControlDestination.WIDGETS
                    ControlDestination.SETTINGS,
                    ControlDestination.STATUS,
                    ControlDestination.VISUAL_TEST,
                    -> ControlDestination.SETTINGS
                    else -> ControlDestination.HOME
                },
                onSelect = { destination = it },
            )
        }

        removalHelp?.let { widget ->
            AlertDialog(
                onDismissRequest = { removalHelp = null },
                title = { Text("Como remover o widget") },
                text = {
                    Text(
                        "O Android controla a remoção dos widgets da tela inicial. " +
                            "Mantenha pressionado o widget ${widget.state.city} e escolha Remover.",
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
private fun DashboardScreen(
    state: SolarWeatherWidgetState,
    primaryWidgetId: Int?,
    widgets: List<InstalledWidgetUi>,
    onRefresh: () -> Unit,
    onCity: () -> Unit,
    onLocation: () -> Unit,
    onConfigure: () -> Unit,
    onShowWidgets: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.height(42.dp), verticalArrangement = Arrangement.Center) {
            Text("Tempodroid", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Clima, Sol e Lua", color = TextSecondary, fontSize = 12.sp)
        }

        WidgetPreview(state = state, height = 170.dp)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(if (state.hasWeatherError) Danger else Good, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(updatedAgo(state.updatedAt), color = TextSecondary, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction("Atualizar", Icons.Outlined.Refresh, onRefresh, Modifier.weight(1f))
            QuickAction("Cidade", Icons.Outlined.Search, onCity, Modifier.weight(1f))
            QuickAction("Localizar", Icons.Outlined.LocationOn, onLocation, Modifier.weight(1f))
            QuickAction("Configurar", Icons.Outlined.Settings, onConfigure, Modifier.weight(1f))
        }

        CompactWeatherCard(state)
        CompactSolarLunarCard(state)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("MEUS WIDGETS ATIVOS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Ver todos",
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onShowWidgets),
                )
            }
            val active = primaryWidgetId?.let { id -> widgets.firstOrNull { it.appWidgetId == id } }
                ?: widgets.firstOrNull()
            if (active != null) {
                WidgetInstanceRow(active.state, onShowWidgets)
            } else {
                WidgetInstanceRow(state.copy(city = "Nenhum widget instalado"), onShowWidgets)
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .background(SurfacePrimary, RoundedCornerShape(12.dp))
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun CompactWeatherCard(state: SolarWeatherWidgetState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(234.dp)
            .background(SurfacePrimary, RoundedCornerShape(16.dp))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.temperature ?: "--"}°", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sensação ${state.apparentTemperature?.let { "$it°" } ?: "--"}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(SurfaceSecondary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .fillMaxHeight()
                    .background(Color(0xBF172338), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Text(
                    WeatherCodeMapper.labelPt(state.condition, state.isDay),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DetailCell(Icons.Outlined.WaterDrop, "Umidade", state.relativeHumidity?.let { "$it%" } ?: "--", Modifier.weight(1f))
                DetailCell(Icons.Outlined.Air, "Vento", state.windSpeedKmh?.let { "$it km/h" } ?: "--", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DetailCell(Icons.Outlined.Cloud, "Chuva", state.rainChance?.let { "$it%" } ?: "--", Modifier.weight(1f))
                DetailCell(Icons.Outlined.Speed, "Pressão", state.surfacePressureHpa?.let { "$it hPa" } ?: "--", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailCell(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(
        modifier = modifier
            .height(43.dp)
            .background(SurfaceSecondary, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = TextMuted, fontSize = 10.sp)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompactSolarLunarCard(state: SolarWeatherWidgetState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(SurfacePrimary, RoundedCornerShape(16.dp))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SOL E LUA", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Sol: ${formatTime(state.sunrise)} nascer / ${formatTime(state.sunset)} pôr", color = TextSecondary, fontSize = 11.sp)
            Text(
                "Lua ${moonPhasePt(state.moonPhase)} (${(state.moonIllumination * 100).roundToInt()}% iluminada)",
                color = TextSecondary,
                fontSize = 11.sp,
            )
        }
        Box(
            Modifier.size(48.dp).background(ActiveSurface.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(moonGlyph(state.moonPhase), fontSize = 27.sp)
        }
    }
}

@Composable
private fun WidgetInstanceRow(state: SolarWeatherWidgetState, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .background(SurfaceSecondary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = Accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(state.city, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (state.temperature != null) {
            Text(
                "  ·  ${state.temperature}°  ·  ${WeatherCodeMapper.labelPt(state.condition, state.isDay)}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun WidgetsScreen(
    widgets: List<InstalledWidgetUi>,
    onAddWidget: () -> Unit,
    onRefreshWidget: (Int) -> Unit,
    onConfigureWidget: (Int) -> Unit,
    onRemovalHelp: (InstalledWidgetUi) -> Unit,
) {
    if (widgets.isEmpty()) {
        EmptyWidgetsScreen(onAddWidget)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Widgets Ativos", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Gerencie seus 4×2 instalados", color = TextSecondary, fontSize = 12.sp)
            }
            ActionPill("+ Novo", onAddWidget)
        }

        widgets.forEach { widget ->
            WidgetCard(widget, onRefreshWidget, onConfigureWidget, onRemovalHelp)
        }
    }
}

@Composable
private fun WidgetCard(
    widget: InstalledWidgetUi,
    onRefreshWidget: (Int) -> Unit,
    onConfigureWidget: (Int) -> Unit,
    onRemovalHelp: (InstalledWidgetUi) -> Unit,
) {
    val state = widget.state
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfacePrimary, RoundedCornerShape(16.dp))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(state.city, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Tempodroid 4×2 • ${updatedAgoShort(state.updatedAt)}", color = TextSecondary, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.temperature?.let { "$it°C" } ?: "--°C", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(WeatherCodeMapper.labelPt(state.condition, state.isDay), color = TextSecondary, fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(SurfaceSecondary, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("✦  Tema Adaptativo M3 Ativo", fontSize = 11.sp)
            Text(if (state.latitude < 0) "Hemisfério Sul" else "Hemisfério Norte", color = TextMuted, fontSize = 10.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TinyAction("Atualizar", Icons.Outlined.Refresh, { onRefreshWidget(widget.appWidgetId) }, Modifier.weight(1f))
            TinyAction("Trocar Cidade", Icons.Outlined.Search, { onConfigureWidget(widget.appWidgetId) }, Modifier.weight(1f))
            TinyAction("Configurar", Icons.Outlined.Settings, { onConfigureWidget(widget.appWidgetId) }, Modifier.weight(1f))
            TinyAction("Como remover", Icons.Outlined.Delete, { onRemovalHelp(widget) }, Modifier.weight(1f), danger = true)
        }
    }
}

@Composable
private fun TinyAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    danger: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(29.dp)
            .background(if (danger) Danger.copy(alpha = 0.12f) else SurfaceSecondary, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (danger) Danger else TextPrimary, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = if (danger) Danger else TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun EmptyWidgetsScreen(onAddWidget: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(140.dp).background(SurfaceSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Cloud, null, tint = Accent, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Nenhum widget configurado", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Adicione o widget Tempodroid 4×2 à tela inicial para monitorar clima, Sol e Lua.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        ActionPill("Configurar Novo Widget", onAddWidget)
    }
}

@Composable
private fun SettingsScreen(
    state: SolarWeatherWidgetState,
    primaryWidgetId: Int?,
    onConfigureWidget: (Int) -> Unit,
    onAddWidget: () -> Unit,
    onStatus: () -> Unit,
    onVisualTest: () -> Unit,
    onPrivacy: () -> Unit,
    onCredits: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Configurações", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Somente ações disponíveis no Tempodroid", color = TextSecondary, fontSize = 12.sp)
        }

        SettingsSection("WIDGET") {
            SettingActionRow("Cidade e localização", state.city, Icons.Outlined.LocationOn) {
                primaryWidgetId?.let(onConfigureWidget) ?: onAddWidget()
            }
        }

        SettingsSection("DIAGNÓSTICO") {
            SettingActionRow("Status do sistema", "Rede, cache e localização", Icons.Outlined.Info, onStatus)
            SettingDivider()
            SettingActionRow("Laboratório visual", "Simular clima, período e Lua", Icons.Outlined.Science, onVisualTest)
        }

        SettingsSection("PRIVACIDADE E DADOS") {
            SettingActionRow("Política de privacidade", "Como o app usa seus dados", Icons.Outlined.Info, onPrivacy)
            SettingDivider()
            SettingActionRow("Créditos dos dados", "Open-Meteo", Icons.Outlined.Info, onCredits)
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("VERSÃO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Tempodroid v${BuildConfig.VERSION_NAME}", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Column(
            Modifier
                .fillMaxWidth()
                .background(SurfacePrimary, RoundedCornerShape(12.dp))
                .border(1.dp, Hairline, RoundedCornerShape(12.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingActionRow(label: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp)
            Text(detail, color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SettingDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

@Composable
private fun StatusScreen(
    state: SolarWeatherWidgetState,
    networkLabel: String,
    connectionMessage: String?,
    onRefresh: () -> Unit,
    onTestConnection: () -> Unit,
) {
    val context = LocalContext.current
    val permission = if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) {
        "Concedida"
    } else {
        "Não concedida"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Status do Sistema", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Diagnóstico e integridade do Tempodroid", color = TextSecondary, fontSize = 12.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Good.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .border(1.dp, Good.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).background(if (state.hasWeatherError) Danger else Good, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (state.hasWeatherError) "Usando dados em cache" else "Funcionando normalmente",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (state.hasWeatherError) "A última atualização de clima falhou." else "Dados locais e atualização estão ativos.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(SurfacePrimary, RoundedCornerShape(16.dp))
                .border(1.dp, Hairline, RoundedCornerShape(16.dp)),
        ) {
            StatusRow("Última atualização de dados", updatedAgoShort(state.updatedAt))
            StatusRow("Atualização em segundo plano", "A cada ~30 min")
            StatusRow("Provedor ativo", "Open-Meteo API")
            StatusRow(
                "Cidade / Coordenadas",
                "${state.city} (${formatCoordinate(state.latitude)}, ${formatCoordinate(state.longitude)})",
            )
            StatusRow("Cache meteorológico", if (state.updatedAt == null) "Sem dados" else "Ativo")
            StatusRow("Conexão de rede", networkLabel)
            StatusRow("Permissão de localização", permission, last = true)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatusButton("Forçar Sincronia", Icons.Outlined.Refresh, onRefresh, Modifier.weight(1f), filled = true)
            StatusButton("Testar Conexão", Icons.Outlined.NetworkCheck, onTestConnection, Modifier.weight(1f), filled = false)
        }

        connectionMessage?.let { Text(it, color = Accent, fontSize = 11.sp) }
    }
}

@Composable
private fun StatusRow(label: String, value: String, last: Boolean = false) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(39.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text(
                value,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
    }
}

@Composable
private fun StatusButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    filled: Boolean,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .then(
                if (filled) {
                    Modifier.background(ActiveSurface, RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                },
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (filled) TextPrimary else Accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (filled) TextPrimary else Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VisualTestScreen(base: SolarWeatherWidgetState) {
    var condition by remember { mutableStateOf(WeatherCondition.CLEAR) }
    var period by remember { mutableStateOf("Twilight") }
    var moonPhase by remember { mutableStateOf(MoonPhase.WAXING_CRESCENT) }

    val preview = base.copy(
        condition = condition,
        cloudCover = when (condition) {
            WeatherCondition.CLEAR -> 5
            WeatherCondition.PARTLY_CLOUDY -> 45
            WeatherCondition.CLOUDY, WeatherCondition.FOG -> 85
            else -> 95
        },
        precipitation = when (condition) {
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> 2.0
            WeatherCondition.STORM -> 5.0
            else -> 0.0
        },
        moonPhase = moonPhase,
        moonIllumination = moonIlluminationFor(moonPhase),
        isDay = period != "Night",
        displayTime = when (period) {
            "Day" -> LocalTime.of(12, 0)
            "Night" -> LocalTime.of(22, 0)
            else -> LocalTime.of(18, 0)
        },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("Laboratório Visual", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Teste simulações e estados da atmosfera", color = TextSecondary, fontSize = 12.sp)
        }

        WidgetPreview(state = preview, height = 190.dp, useLiveAstronomy = false)

        ChoiceSection("CONDIÇÃO CLIMÁTICA") {
            ChoiceRow(
                labels = listOf("Clear", "Partly Cloudy", "Cloudy", "Rain", "Storm"),
                selected = when (condition) {
                    WeatherCondition.PARTLY_CLOUDY -> "Partly Cloudy"
                    WeatherCondition.CLOUDY, WeatherCondition.FOG -> "Cloudy"
                    WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> "Rain"
                    WeatherCondition.STORM -> "Storm"
                    else -> "Clear"
                },
            ) { label ->
                condition = when (label) {
                    "Partly Cloudy" -> WeatherCondition.PARTLY_CLOUDY
                    "Cloudy" -> WeatherCondition.CLOUDY
                    "Rain" -> WeatherCondition.RAIN
                    "Storm" -> WeatherCondition.STORM
                    else -> WeatherCondition.CLEAR
                }
            }
        }

        ChoiceSection("PERÍODO DO DIA") {
            ChoiceRow(listOf("Day", "Twilight", "Night"), period) { period = it }
        }

        ChoiceSection("FASE LUNAR") {
            MoonChoiceRows(moonPhase) { moonPhase = it }
        }
    }
}

@Composable
private fun ChoiceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun ChoiceRow(labels: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { label ->
            ChoiceChip(label, label == selected) { onSelect(label) }
        }
    }
}

@Composable
private fun MoonChoiceRows(selected: MoonPhase, onSelect: (MoonPhase) -> Unit) {
    val rows = listOf(
        listOf(MoonPhase.NEW_MOON, MoonPhase.WAXING_CRESCENT, MoonPhase.FIRST_QUARTER, MoonPhase.WAXING_GIBBOUS),
        listOf(MoonPhase.FULL_MOON, MoonPhase.WANING_GIBBOUS, MoonPhase.LAST_QUARTER, MoonPhase.WANING_CRESCENT),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { phases ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                phases.forEach { phase ->
                    ChoiceChip(moonPhaseEnglish(phase), phase == selected, compact = true) { onSelect(phase) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(if (selected) Accent else SurfacePrimary, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 9.dp else 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) AppBackground else TextPrimary,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun WidgetPreview(
    state: SolarWeatherWidgetState,
    height: Dp,
    useLiveAstronomy: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceSecondary),
    ) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { height.roundToPx() }
        val bitmaps by produceState<WidgetBitmaps?>(null, state, widthPx, heightPx, useLiveAstronomy) {
            value = withContext(Dispatchers.Default) {
                WidgetRenderer.render(context, state, widthPx, heightPx, useLiveAstronomy)
            }
        }

        bitmaps?.let { rendered ->
            Image(
                rendered.sky.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            WeatherCodeMapper.labelPt(state.condition, state.isDay),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("${state.temperature ?: "--"}°", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatTime(state.displayTime ?: LocalTime.now()),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(state.displayDate, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                        Text(state.city, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Image(
                    rendered.timeline.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    contentScale = ContentScale.FillBounds,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        "Máx ${state.high?.let { "$it°" } ?: "--"}  ·  " +
                            "Mín ${state.low?.let { "$it°" } ?: "--"}  ·  " +
                            (state.rainChance?.let { "$it% chuva" } ?: "--"),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.24f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(active: ControlDestination, onSelect: (ControlDestination) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(SurfacePrimary)
            .border(1.dp, Hairline)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BottomNavItem("Início", Icons.Outlined.Home, active == ControlDestination.HOME) {
            onSelect(ControlDestination.HOME)
        }
        BottomNavItem("Widgets", Icons.Outlined.Apps, active == ControlDestination.WIDGETS) {
            onSelect(ControlDestination.WIDGETS)
        }
        BottomNavItem("Ajustes", Icons.Outlined.Settings, active == ControlDestination.SETTINGS) {
            onSelect(ControlDestination.SETTINGS)
        }
    }
}

@Composable
private fun BottomNavItem(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(72.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(28.dp)
                .then(
                    if (active) Modifier.background(ActiveSurface, RoundedCornerShape(16.dp)) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (active) Accent else TextSecondary, modifier = Modifier.size(20.dp))
        }
        Text(
            label,
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ActionPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Accent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = AppBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatTime(time: LocalTime?): String =
    time?.format(DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))) ?: "--:--"

private fun updatedAgo(updatedAt: Instant?): String = "Atualizado · ${updatedAgoShort(updatedAt)}"

private fun updatedAgoShort(updatedAt: Instant?): String {
    if (updatedAt == null) return "sem dados"
    val minutes = Duration.between(updatedAt, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há $minutes min"
        else -> "há ${minutes / 60}h"
    }
}

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun moonPhasePt(phase: MoonPhase): String = when (phase) {
    MoonPhase.NEW_MOON -> "Nova"
    MoonPhase.WAXING_CRESCENT -> "Crescente"
    MoonPhase.FIRST_QUARTER -> "Quarto Crescente"
    MoonPhase.WAXING_GIBBOUS -> "Gibosa Crescente"
    MoonPhase.FULL_MOON -> "Cheia"
    MoonPhase.WANING_GIBBOUS -> "Gibosa Minguante"
    MoonPhase.LAST_QUARTER -> "Quarto Minguante"
    MoonPhase.WANING_CRESCENT -> "Minguante"
}

private fun moonPhaseEnglish(phase: MoonPhase): String = when (phase) {
    MoonPhase.NEW_MOON -> "New"
    MoonPhase.WAXING_CRESCENT -> "Waxing Crescent"
    MoonPhase.FIRST_QUARTER -> "First Quarter"
    MoonPhase.WAXING_GIBBOUS -> "Waxing Gibbous"
    MoonPhase.FULL_MOON -> "Full"
    MoonPhase.WANING_GIBBOUS -> "Waning Gibbous"
    MoonPhase.LAST_QUARTER -> "Last Quarter"
    MoonPhase.WANING_CRESCENT -> "Waning Crescent"
}

private fun moonGlyph(phase: MoonPhase): String = when (phase) {
    MoonPhase.NEW_MOON -> "🌑"
    MoonPhase.WAXING_CRESCENT -> "🌒"
    MoonPhase.FIRST_QUARTER -> "🌓"
    MoonPhase.WAXING_GIBBOUS -> "🌔"
    MoonPhase.FULL_MOON -> "🌕"
    MoonPhase.WANING_GIBBOUS -> "🌖"
    MoonPhase.LAST_QUARTER -> "🌗"
    MoonPhase.WANING_CRESCENT -> "🌘"
}

private fun moonIlluminationFor(phase: MoonPhase): Double = when (phase) {
    MoonPhase.NEW_MOON -> 0.01
    MoonPhase.WAXING_CRESCENT,
    MoonPhase.WANING_CRESCENT,
    -> 0.25
    MoonPhase.FIRST_QUARTER,
    MoonPhase.LAST_QUARTER,
    -> 0.5
    MoonPhase.WAXING_GIBBOUS,
    MoonPhase.WANING_GIBBOUS,
    -> 0.75
    MoonPhase.FULL_MOON -> 1.0
}
