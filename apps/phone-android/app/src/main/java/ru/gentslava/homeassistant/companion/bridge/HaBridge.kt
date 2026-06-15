package ru.gentslava.homeassistant.companion.bridge

import kotlinx.serialization.encodeToString
import ru.gentslava.homeassistant.companion.ha.HaClient
import ru.gentslava.homeassistant.companion.p2p.Ack
import ru.gentslava.homeassistant.companion.p2p.CallServiceRequest
import ru.gentslava.homeassistant.companion.p2p.P2pJson
import ru.gentslava.homeassistant.companion.p2p.SyncEntityRequest
import ru.gentslava.homeassistant.companion.p2p.SyncEntityResponse
import ru.gentslava.homeassistant.companion.p2p.SyncRequest
import ru.gentslava.homeassistant.companion.p2p.SyncResponse
import ru.gentslava.homeassistant.companion.p2p.parseIncoming

/**
 * Translates one inbound P2P request (from the watch) into HA REST calls and produces the JSON
 * reply to send back. Transport-agnostic: in/out are JSON strings, so it is unit-testable without
 * Wear Engine. Reply `id` always echoes the request `id` (P2P correlation, see docs/p2p-protocol.md).
 */
class HaBridge(private val client: HaClient) {

    /** Handle a raw inbound message; returns the JSON reply, or null if unrecognized. */
    suspend fun handle(rawJson: String): String? = when (val msg = parseIncoming(rawJson)) {
        is SyncRequest -> client.getStates().fold(
            onSuccess = { encode(SyncResponse(id = msg.id, cards = EntityMapper.cards(it))) },
            onFailure = { ackError(msg.id, it) },
        )

        is SyncEntityRequest -> client.getState(msg.entityId).fold(
            onSuccess = { state ->
                encode(SyncEntityResponse(id = msg.id, card = state?.let(EntityMapper::toCard)))
            },
            onFailure = { ackError(msg.id, it) },
        )

        is CallServiceRequest -> client.callService(msg.domain, msg.service, msg.data).fold(
            onSuccess = { encode(Ack(id = msg.id, ok = true)) },
            onFailure = { ackError(msg.id, it) },
        )

        null -> null
    }

    private fun ackError(id: String, e: Throwable): String =
        encode(Ack(id = id, ok = false, error = e.message ?: "error"))

    private inline fun <reified T> encode(value: T): String = P2pJson.encodeToString(value)
}
