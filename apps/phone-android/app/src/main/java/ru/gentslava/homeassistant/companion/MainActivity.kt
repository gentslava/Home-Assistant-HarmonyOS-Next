package ru.gentslava.homeassistant.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.gentslava.homeassistant.companion.bridge.EntityMapper
import ru.gentslava.homeassistant.companion.bridge.HaBridge
import ru.gentslava.homeassistant.companion.ha.HaClient
import ru.gentslava.homeassistant.companion.ha.HaConfig
import ru.gentslava.homeassistant.companion.p2p.EntityCard
import ru.gentslava.homeassistant.companion.p2p.WearEngineP2pService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = HaConfig(this)
        val client = HaClient(config)

        // Start the watch transport so a paired watch can reach HA through this companion.
        // (Wear Engine asks for the DEVICE_MANAGER permission on first run.)
        val p2p = WearEngineP2pService(this, HaBridge(client))
        if (config.isConfigured) p2p.start()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { CompanionScreen(config, client) }
            }
        }
    }
}

@Composable
private fun CompanionScreen(config: HaConfig, client: HaClient) {
    var url by remember { mutableStateOf(config.baseUrl) }
    var token by remember { mutableStateOf(config.token) }
    var status by remember { mutableStateOf(if (config.isConfigured) "Configured" else "Not configured") }
    var cards by remember { mutableStateOf<List<EntityCard>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Home Assistant Companion", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("HA URL (http://homeassistant.local:8123)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Long-lived access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                config.baseUrl = url
                config.token = token
                scope.launch {
                    status = "Checking…"
                    client.checkApi().fold(
                        onSuccess = {
                            client.getStates().fold(
                                onSuccess = { states ->
                                    cards = EntityMapper.cards(states)
                                    status = "Connected — ${cards.size} entities (light/switch/lock)"
                                },
                                onFailure = { status = "States error: ${it.message}" },
                            )
                        },
                        onFailure = { status = "Connection error: ${it.message}" },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Connect & test") }

        Text(status, style = MaterialTheme.typography.bodyMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cards) { card ->
                Text("${card.name}  ·  ${card.state}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
