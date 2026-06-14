package ru.gentslava.homeassistant.companion.p2p

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Wire types for the watch <-> phone P2P protocol. Mirror of docs/p2p-protocol.md (v1).
 * The watch is always the initiator; the companion only sends replies.
 */
const val PROTOCOL_VERSION = 1

val P2pJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

// ---- Payload types (shared) ----

@Serializable
data class EntityAction(
    val kind: String = "SERVICE",
    val label: String,
    val domain: String,
    val service: String,
    val data: Map<String, String>,
)

@Serializable
data class EntityCard(
    @SerialName("entity_id") val entityId: String,
    val domain: String,
    val name: String,
    val state: String,
    val icon: String? = null,
    val primary: EntityAction,
    val secondary: List<EntityAction> = emptyList(),
)

// ---- Outgoing (companion -> watch): serialized to JSON ----

@Serializable
data class SyncResponse(
    val id: String,
    val cards: List<EntityCard>,
    val v: Int = PROTOCOL_VERSION,
    val type: String = "SYNC_RESPONSE",
)

@Serializable
data class SyncEntityResponse(
    val id: String,
    val card: EntityCard? = null,
    val v: Int = PROTOCOL_VERSION,
    val type: String = "SYNC_ENTITY_RESPONSE",
)

@Serializable
data class Ack(
    val id: String,
    val ok: Boolean,
    val error: String? = null,
    val v: Int = PROTOCOL_VERSION,
    val type: String = "ACK",
)

// ---- Incoming (watch -> companion): parsed by `type` ----

sealed interface IncomingMsg {
    val id: String
}

data class SyncRequest(override val id: String) : IncomingMsg

data class SyncEntityRequest(override val id: String, val entityId: String) : IncomingMsg

data class CallServiceRequest(
    override val id: String,
    val domain: String,
    val service: String,
    val data: Map<String, String>,
) : IncomingMsg

/** Parse one inbound JSON message. Returns null if it's not a recognized request. */
fun parseIncoming(json: String): IncomingMsg? = runCatching {
    val obj = P2pJson.parseToJsonElement(json).jsonObject
    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
    when (obj["type"]?.jsonPrimitive?.contentOrNull) {
        "SYNC_REQUEST" -> SyncRequest(id)
        "SYNC_ENTITY_REQUEST" -> {
            val entityId = obj["entity_id"]?.jsonPrimitive?.contentOrNull ?: return null
            SyncEntityRequest(id, entityId)
        }
        "CALL_SERVICE" -> {
            val domain = obj["domain"]?.jsonPrimitive?.contentOrNull ?: return null
            val service = obj["service"]?.jsonPrimitive?.contentOrNull ?: return null
            val data = obj["data"]?.jsonObject
                ?.mapValues { it.value.jsonPrimitive.contentOrNull ?: "" }
                ?: emptyMap()
            CallServiceRequest(id, domain, service, data)
        }
        else -> null
    }
}.getOrNull()
