package com.tempodroid.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tempodroid.di.AppContainer
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
                "weather" -> "Detalhes do clima"
                "forecast" -> "Previsão diária"
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
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Text(destination, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("${state.city} · ${state.temperature ?: "--"}° · ${state.condition}")
                        Text("Nascer ${state.sunrise} · Pôr ${state.sunset}")
                        Text("Lua ${state.moonPhase} · ${(state.moonIllumination * 100).toInt()}%")
                        if (state.hasWeatherError) Text("Modo offline: último clima válido.")
                        Spacer(Modifier.height(16.dp))

                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            Text("A cidade e a localização são configuradas por instância do widget.")
                            Spacer(Modifier.height(8.dp))
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
                            Text("Adicione o widget 4×2 à tela inicial para escolher a cidade e configurar o clima.")
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }
}
