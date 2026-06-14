package ru.gentslava.homeassistant.companion.ha

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A Home Assistant state object from GET /api/states (see docs/ha-integration-notes.md).
 * `attributes` is kept as a raw JsonObject — its shape depends on the domain.
 */
@Serializable
data class HaState(
    @SerialName("entity_id") val entityId: String,
    val state: String,
    val attributes: JsonObject = JsonObject(emptyMap()),
    @SerialName("last_changed") val lastChanged: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
) {
    val domain: String get() = entityId.substringBefore('.', "")
    val objectId: String get() = entityId.substringAfter('.', entityId)

    /** friendly_name, or a humanized object_id fallback. */
    val friendlyName: String
        get() = attributes["friendly_name"]?.jsonPrimitive?.contentOrNull
            ?: objectId.replace('_', ' ').replaceFirstChar { it.uppercase() }

    fun attr(key: String): String? = attributes[key]?.jsonPrimitive?.contentOrNull

    val isAvailable: Boolean get() = state != "unavailable" && state != "unknown"
}
