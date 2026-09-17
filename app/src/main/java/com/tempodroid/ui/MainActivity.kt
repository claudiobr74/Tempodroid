package com.tempodroid.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tempodroid.BuildConfig
import com.tempodroid.di.AppContainer
import com.tempodroid.widget.SolarWeatherWidgetReceiver
import com.tempodroid.widget.SolarWeatherWidgetState
import com.tempodroid.widget.WidgetUpdateCoordinator

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetUpdateCoordinator.refreshNow(this)

        setContent {
            val appWidgetId = intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
            val destination = when (intent?.data?.host) {
                "weather" -> "Clima deste widget"
                "forecast" -> "Resumo do dia"
                else -> "Tempodroid"
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var state by remember { mutableStateOf(SolarWeatherWidgetState.Empty) }
                    val container = remember { AppContainer.get(this@MainActivity) }
                    LaunchedEffect(appWidgetId) {
                        state = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            container.cache.loadState(appWidgetId)
                        } else {
                            container.cache.loadState()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(destination, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Relógio, clima e ciclo Sol/Lua em um widget Android 4×2.",
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            Text(
                                "${state.city} · ${state.temperature ?: "--"}° · ${state.condition}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Nascer ${state.sunrise} · Pôr ${state.sunset} · Lua ${(state.moonIllumination * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (state.hasWeatherError) {
                                Text(
                                    "Modo offline: exibindo o último clima válido.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Button(
                                onClick = {
                                    startActivity(
                                        Intent(this@MainActivity, WidgetConfigurationActivity::class.java)
                                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                            .putExtra(WidgetConfigurationActivity.EXTRA_EDIT_MODE, true),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Configurar este widget")
                            }
                        } else {
                            Button(
                                onClick = { requestPinWidget() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Adicionar widget à tela inicial")
                            }
                            Text(
                                "Se o launcher não oferecer fixação direta, mantenha pressionada a tela inicial, escolha Widgets e procure por Tempodroid.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text("Privacidade e dados", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A localização aproximada é opcional e só é solicitada quando você escolhe usá-la. Cidade, coordenadas e cache ficam armazenados localmente no aparelho. As coordenadas ou a cidade selecionada são enviadas ao provedor meteorológico somente para obter o clima.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(
                            onClick = { openUrl(PRIVACY_POLICY_URL) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Política de privacidade")
                        }

                        Text("Fontes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Dados meteorológicos: Open-Meteo (CC BY 4.0). Busca de cidades: Open-Meteo, com dados de localização baseados em GeoNames.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(
                            onClick = { openUrl(OPEN_METEO_URL) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Créditos e licença dos dados")
                        }
                        OutlinedButton(
                            onClick = { openUrl(SUPPORT_URL) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Suporte")
                        }

                        Text(
                            "Versão ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    private fun requestPinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, SolarWeatherWidgetReceiver::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
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

    companion object {
        private const val PRIVACY_POLICY_URL =
            "https://github.com/claudiobr74/Tempodroid/blob/main/docs/privacy-policy.md"
        private const val SUPPORT_URL =
            "https://github.com/claudiobr74/Tempodroid/issues"
        private const val OPEN_METEO_URL =
            "https://open-meteo.com/"
    }
}
