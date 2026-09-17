package com.tempodroid.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tempodroid.di.AppContainer
import com.tempodroid.widget.SolarWeatherWidgetState
import com.tempodroid.widget.WidgetUpdateCoordinator
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) WidgetUpdateCoordinator.refreshNow(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetUpdateCoordinator.refreshNow(this)
        val destination = when (intent?.data?.host) {
            "weather" -> "Detalhes do clima"
            "forecast" -> "Previsão diária"
            else -> "Tempodroid"
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var state by remember { mutableStateOf(SolarWeatherWidgetState.Empty) }
                    var city by remember { mutableStateOf("") }
                    val scope = rememberCoroutineScope()
                    val container = remember { AppContainer.get(this@MainActivity) }
                    LaunchedEffect(Unit) {
                        state = container.cache.loadState()
                        city = state.city
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
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Cidade manual") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                container.preferences.setManualCity(city)
                                container.locationRepository.geocode(city)?.let {
                                    container.cache.saveLocation(it)
                                }
                                WidgetUpdateCoordinator.refreshNow(this@MainActivity)
                                state = container.cache.loadState()
                            }
                        }) { Text("Salvar cidade") }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            } else {
                                scope.launch {
                                    container.locationRepository.lastKnownCoarse()?.let {
                                        container.cache.saveLocation(it)
                                    }
                                    WidgetUpdateCoordinator.refreshNow(this@MainActivity)
                                }
                            }
                        }) { Text("Usar localização aproximada") }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
