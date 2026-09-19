package com.tempodroid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background = Color(0xFFF7F9FC),
                    surface = Color.White,
                    primary = Color(0xFF0284C7),
                    onBackground = Color(0xFF111827),
                    onSurface = Color(0xFF111827),
                ),
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Política de privacidade", style = MaterialTheme.typography.headlineSmall)
                        Text("Última atualização: 17 de setembro de 2026", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "O Tempodroid usa apenas os dados necessários para fornecer o widget de relógio, clima e ciclo Sol/Lua.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text("Localização aproximada", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A localização aproximada é opcional e só é solicitada quando você toca em “Usar localização aproximada”. O aplicativo não solicita localização precisa, não usa localização em segundo plano e não mantém rastreamento contínuo.",
                        )
                        Text("Dados armazenados no aparelho", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Para cada widget, o Tempodroid pode armazenar localmente cidade, coordenadas, fuso horário, último clima válido e dados astronômicos. O backup automático desses dados está desativado.",
                        )
                        Text("Serviços externos", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "As coordenadas da cidade selecionada são enviadas à Open-Meteo para obter o clima. A busca manual de cidades também usa a geocodificação da Open-Meteo, baseada em dados do GeoNames. Esses serviços podem receber informações técnicas inerentes à conexão, como o endereço IP.",
                        )
                        Text("Publicidade e analytics", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "O Tempodroid não possui anúncios, login, analytics, perfil comportamental nem venda de dados pessoais.",
                        )
                        Text("Exclusão", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Ao remover uma instância do widget, o estado daquela instância é apagado. Você também pode limpar os dados do aplicativo nas configurações do Android ou desinstalá-lo.",
                        )
                        Text("Segurança", style = MaterialTheme.typography.titleMedium)
                        Text("As comunicações externas usam HTTPS e o aplicativo bloqueia tráfego HTTP em texto claro.")
                        Text("Fontes", style = MaterialTheme.typography.titleMedium)
                        Text("Dados meteorológicos: Open-Meteo (CC BY 4.0). Busca de localidades: Open-Meteo/GeoNames.")
                    }
                }
            }
        }
    }
}
