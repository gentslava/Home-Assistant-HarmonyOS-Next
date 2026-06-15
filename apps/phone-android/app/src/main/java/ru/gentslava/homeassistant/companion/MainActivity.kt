package ru.gentslava.homeassistant.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.gentslava.homeassistant.companion.bridge.EntityMapper
import ru.gentslava.homeassistant.companion.bridge.HaBridge
import ru.gentslava.homeassistant.companion.ha.HaClient
import ru.gentslava.homeassistant.companion.ha.HaConfig
import ru.gentslava.homeassistant.companion.p2p.EntityCard
import ru.gentslava.homeassistant.companion.p2p.WearEngineP2pService
import ru.gentslava.homeassistant.companion.ui.HaCard
import ru.gentslava.homeassistant.companion.ui.HaColorScheme
import ru.gentslava.homeassistant.companion.ui.HaSecondaryText
import ru.gentslava.homeassistant.companion.ui.entityAccent
import ru.gentslava.homeassistant.companion.ui.entityGlyph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val config = HaConfig(this)
        val client = HaClient(config)

        // Start the watch transport so a paired watch can reach HA through this companion.
        val p2p = WearEngineP2pService(this, HaBridge(client))
        if (config.isConfigured) p2p.start()

        setContent {
            MaterialTheme(colorScheme = HaColorScheme) {
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
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Home Assistant",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Companion", style = MaterialTheme.typography.bodyMedium, color = HaSecondaryText)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("HA URL") },
            placeholder = { Text("http://homeassistant.local:8123") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Long-lived access token") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

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
                                    status = "Connected — ${cards.size} entities"
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

        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium, color = HaSecondaryText)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(cards) { card -> EntityRow(card) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun EntityRow(card: EntityCard) {
    val accent = entityAccent(card.domain, card.state)
    Card(
        colors = CardDefaults.cardColors(containerColor = HaCard),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(entityGlyph(card.domain), fontSize = 18.sp)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(card.state, style = MaterialTheme.typography.bodySmall, color = accent)
            }
        }
    }
}
