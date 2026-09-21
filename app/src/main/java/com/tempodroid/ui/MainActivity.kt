package com.tempodroid.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.tempodroid.di.AppContainer
import com.tempodroid.weather.ForecastSnapshot
import com.tempodroid.widget.SolarWeatherWidgetReceiver
import com.tempodroid.widget.SolarWeatherWidgetState
import com.tempodroid.widget.WidgetUpdateCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var resumeGeneration by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // A tela lê exclusivamente o cache. O ciclo meteorológico pertence ao
        // WorkManager; assim abrir/reabrir o app não duplica chamadas à API.
        WidgetUpdateCoordinator.ensurePeriodic(this)

        setContent {
            val container = remember { AppContainer.get(this@MainActivity) }
            val scope = rememberCoroutineScope()
            var reloadKey by remember { mutableIntStateOf(0) }
            var installedWidgets by remember { mutableStateOf<List<InstalledWidgetUi>>(emptyList()) }
            var primaryState by remember { mutableStateOf(SolarWeatherWidgetState.Empty) }
            var primaryForecast by remember { mutableStateOf(ForecastSnapshot.Empty) }
            var primaryWidgetId by remember { mutableStateOf<Int?>(null) }
            var networkLabel by remember { mutableStateOf(currentNetworkLabel()) }
            var connectionMessage by remember { mutableStateOf<String?>(null) }
            val generation = resumeGeneration

            val requestedWidgetId = intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

            LaunchedEffect(reloadKey, requestedWidgetId, generation) {
                val manager = AppWidgetManager.getInstance(this@MainActivity)
                val provider = ComponentName(this@MainActivity, SolarWeatherWidgetReceiver::class.java)
                val ids = manager.getAppWidgetIds(provider).toList()
                val widgets = ids.map { id ->
                    InstalledWidgetUi(id, container.cache.loadState(id))
                }
                installedWidgets = widgets
                val selected = when {
                    requestedWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID ->
                        widgets.firstOrNull { it.appWidgetId == requestedWidgetId }
                    else -> widgets.firstOrNull()
                }
                primaryWidgetId = selected?.appWidgetId
                primaryState = selected?.state ?: container.cache.loadState()
                primaryForecast = container.forecastCache.loadForecast(selected?.appWidgetId)
                networkLabel = currentNetworkLabel()
            }

            TempodroidLightControlApp(
                primaryState = primaryState,
                primaryForecast = primaryForecast,
                primaryWidgetId = primaryWidgetId,
                installedWidgets = installedWidgets,
                networkLabel = networkLabel,
                connectionMessage = connectionMessage,
                onRefreshAll = {
                    WidgetUpdateCoordinator.refreshNow(this@MainActivity)
                    connectionMessage = "Sincronização solicitada."
                    scope.launch {
                        delay(1800)
                        reloadKey++
                    }
                },
                onRefreshWidget = { id ->
                    WidgetUpdateCoordinator.refreshWidget(this@MainActivity, id)
                    connectionMessage = "Atualização do widget solicitada."
                    scope.launch {
                        delay(1800)
                        reloadKey++
                    }
                },
                onConfigureWidget = { id -> openWidgetConfiguration(id) },
                onAddWidget = {
                    if (!requestPinWidget()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Este launcher não permite adicionar o widget por este botão. Adicione o Tempodroid pelo seletor de widgets da tela inicial.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onPrivacy = {
                    startActivity(Intent(this@MainActivity, PrivacyPolicyActivity::class.java))
                },
                onCredits = { openUrl(OPEN_METEO_URL) },
                onTestConnection = {
                    scope.launch {
                        connectionMessage = "Testando conexão…"
                        val result = runCatching {
                            container.weatherRepository.getWeather(primaryState.latitude, primaryState.longitude)
                        }
                        networkLabel = currentNetworkLabel()
                        connectionMessage = if (result.isSuccess) {
                            "Conexão com Open-Meteo funcionando."
                        } else {
                            "Não foi possível acessar o Open-Meteo agora."
                        }
                    }
                },
            )
        }
    }

    private fun openWidgetConfiguration(appWidgetId: Int) {
        startActivity(
            Intent(this, WidgetConfigurationActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                .putExtra(WidgetConfigurationActivity.EXTRA_EDIT_MODE, true),
        )
    }

    private fun requestPinWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, SolarWeatherWidgetReceiver::class.java)
        return if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            false
        }
    }

    private fun currentNetworkLabel(): String {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return "Indisponível"
        val network = manager.activeNetwork ?: return "Sem conexão"
        val capabilities = manager.getNetworkCapabilities(network) ?: return "Sem conexão"
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return "Sem internet"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi conectado"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Rede móvel conectada"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet conectada"
            else -> "Internet disponível"
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onResume() {
        super.onResume()
        resumeGeneration++
    }

    companion object {
        private const val OPEN_METEO_URL = "https://open-meteo.com/"
    }
}
