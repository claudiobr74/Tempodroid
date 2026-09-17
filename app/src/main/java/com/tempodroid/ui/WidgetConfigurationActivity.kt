package com.tempodroid.ui

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.tempodroid.R
import com.tempodroid.di.AppContainer
import com.tempodroid.location.LocationFallback
import com.tempodroid.location.LocationSearchResult
import com.tempodroid.location.ResolvedLocation
import com.tempodroid.widget.SolarWeatherWidget
import com.tempodroid.widget.SolarWeatherWidgetReceiver
import com.tempodroid.widget.WidgetUpdateCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (!isValidTempodroidWidget(appWidgetId)) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigurationScreen()
                }
            }
        }
    }

    private fun isValidTempodroidWidget(id: Int): Boolean {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id) ?: return false
        val expectedProvider = ComponentName(this, SolarWeatherWidgetReceiver::class.java)
        return info.provider == expectedProvider
    }

    @androidx.compose.runtime.Composable
    private fun ConfigurationScreen() {
        val container = remember { AppContainer.get(this@WidgetConfigurationActivity) }
        val scope = rememberCoroutineScope()
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<LocationSearchResult>>(emptyList()) }
        var selected by remember { mutableStateOf<ResolvedLocation?>(null) }
        var saved by remember { mutableStateOf<ResolvedLocation?>(null) }
        var isEdit by remember { mutableStateOf(intent.getBooleanExtra(EXTRA_EDIT_MODE, false)) }
        var searching by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(appWidgetId) {
            saved = container.cache.savedLocationExact(appWidgetId)
            selected = saved
            if (container.cache.isConfigured(appWidgetId)) isEdit = true
        }

        LaunchedEffect(query) {
            if (query.trim().length < 2) {
                results = emptyList()
                searching = false
                return@LaunchedEffect
            }
            delay(400)
            searching = true
            message = null
            runCatching { container.locationRepository.searchCities(query) }
                .onSuccess {
                    results = it
                    if (it.isEmpty()) message = "Nenhuma cidade encontrada."
                }
                .onFailure {
                    results = emptyList()
                    message = "Sem internet. Você pode usar a última cidade ou o fallback."
                }
            searching = false
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                scope.launch {
                    val location = container.locationRepository.lastKnownCoarse()
                    if (location != null) {
                        selected = location
                        query = location.city
                        results = emptyList()
                        message = null
                    } else {
                        message = "Não foi possível obter uma localização aproximada."
                    }
                }
            } else {
                message = "Permissão de localização não concedida. Você pode buscar a cidade manualmente."
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Configurar widget", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Widget 4×2 · relógio, clima e Sol/Lua",
                style = MaterialTheme.typography.bodyMedium,
            )

            AndroidView(
                factory = { context ->
                    LayoutInflater.from(context).inflate(R.layout.widget_solar_weather_preview, null, false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(24.dp)),
            )

            saved?.let {
                Text(
                    "Cidade atual: ${it.city}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it != selected?.city) selected = null
                },
                label = { Text("Buscar cidade") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Buscar cidade" },
            )

            if (searching) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Text("Buscando cidades…")
                }
            }

            results.forEach { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = result.toResolvedLocation()
                            query = result.name
                            results = emptyList()
                            message = null
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Text(result.name, style = MaterialTheme.typography.titleMedium)
                    if (result.secondaryLabel.isNotBlank()) {
                        Text(result.secondaryLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text(
                "Localização aproximada é opcional. Se você escolher usá-la, o Tempodroid acessa apenas a última localização aproximada disponível para selecionar o clima local; não há rastreamento contínuo nem localização em segundo plano.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            this@WidgetConfigurationActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        scope.launch {
                            val location = container.locationRepository.lastKnownCoarse()
                            if (location != null) {
                                selected = location
                                query = location.city
                                results = emptyList()
                                message = null
                            } else {
                                message = "Não foi possível obter uma localização aproximada."
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Usar localização aproximada" },
            ) {
                Text("Usar localização aproximada")
            }

            val effective = selected ?: saved
            if (effective != null) {
                Text(
                    "Selecionado: ${effective.city}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    "Nenhuma cidade selecionada. Ao continuar, usaremos Goiânia como fallback.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "O launcher pode redimensionar o widget. O formato oficial é 4×2.",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val chosen = selected ?: saved ?: LocationFallback.goiania
                    scope.launch { completeConfiguration(chosen) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isEdit) "Salvar configuração do widget" else "Adicionar widget"
                    },
            ) {
                Text(if (isEdit) "Salvar" else "Adicionar widget")
            }

            OutlinedButton(
                onClick = { finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Cancelar configuração" },
            ) {
                Text("Cancelar")
            }
        }
    }

    private suspend fun completeConfiguration(location: ResolvedLocation) {
        val container = AppContainer.get(this)
        val previous = container.cache.savedLocationExact(appWidgetId)
        val changedLocation = previous == null ||
            previous.latitude != location.latitude || previous.longitude != location.longitude
        if (changedLocation) container.cache.clearWeather(appWidgetId)
        container.cache.saveLocation(location, appWidgetId)

        val zone = runCatching { ZoneId.of(location.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val now = Instant.now().atZone(zone)
        val astronomy = container.astronomyRepository.getAstronomy(
            latitude = location.latitude,
            longitude = location.longitude,
            date = LocalDate.from(now),
            timezone = zone,
            at = now.toLocalTime(),
        )
        container.cache.saveAstronomy(astronomy, appWidgetId)
        val dateLabel = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR"))
            .format(now)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        container.cache.saveDateLabel(dateLabel, appWidgetId)

        runCatching {
            val manager = GlanceAppWidgetManager(this)
            val glanceId = manager.getGlanceIdBy(appWidgetId)
            SolarWeatherWidget().update(this, glanceId)
        }
        WidgetUpdateCoordinator.refreshWidget(this, appWidgetId)

        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        const val EXTRA_EDIT_MODE = "com.tempodroid.extra.EDIT_WIDGET"
    }
}
