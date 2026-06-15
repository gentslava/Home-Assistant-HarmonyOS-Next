package ru.gentslava.homeassistant.companion.bridge

import ru.gentslava.homeassistant.companion.ha.HaState
import ru.gentslava.homeassistant.companion.p2p.EntityAction
import ru.gentslava.homeassistant.companion.p2p.EntityCard

/**
 * Maps Home Assistant states to our P2P EntityCard/EntityAction (see docs/ha-integration-notes.md).
 * Domains: light, switch, lock, cover, scene, sensor. Labels are English for now — localization
 * is a later task (the watch renders labels as-is).
 *
 * sensor is read-only: no primary action (the unit is folded into [state]). scene is stateless: a
 * single "Activate" action with a stable display state (its HA state is a timestamp we don't show).
 */
object EntityMapper {

    val SUPPORTED_DOMAINS = setOf("light", "switch", "lock", "cover", "scene", "sensor")

    fun cards(states: List<HaState>): List<EntityCard> = states.mapNotNull(::toCard)

    fun toCard(s: HaState): EntityCard? {
        if (s.domain !in SUPPORTED_DOMAINS) return null
        return EntityCard(
            entityId = s.entityId,
            domain = s.domain,
            name = s.friendlyName,
            state = displayState(s),
            icon = s.domain,
            primary = primaryAction(s),
            secondary = secondaryActions(s),
        )
    }

    /** What the watch shows as the entity's state line. */
    private fun displayState(s: HaState): String = when (s.domain) {
        // A scene's HA state is a last-activated timestamp — show a stable token instead.
        "scene" -> "scene"
        // Fold the unit into the value so the watch needs no extra field: "21.5" + "°C".
        "sensor" -> s.attr("unit_of_measurement")?.let { "${s.state} $it" } ?: s.state
        else -> s.state
    }

    private fun primaryAction(s: HaState): EntityAction? = when (s.domain) {
        "lock" -> if (s.state == "locked") {
            action("Unlock", "lock", "unlock", s.entityId)
        } else {
            action("Lock", "lock", "lock", s.entityId)
        }
        "cover" -> when (s.state) {
            "open" -> action("Close", "cover", "close_cover", s.entityId)
            "closed" -> action("Open", "cover", "open_cover", s.entityId)
            "opening", "closing" -> action("Stop", "cover", "stop_cover", s.entityId)
            else -> action("Open", "cover", "open_cover", s.entityId)
        }
        "scene" -> action("Activate", "scene", "turn_on", s.entityId)
        // Read-only: no primary tile on the watch.
        "sensor" -> null
        // light / switch
        else -> action("Toggle", s.domain, "toggle", s.entityId)
    }

    private fun secondaryActions(s: HaState): List<EntityAction> = when (s.domain) {
        "light" -> listOf(
            action("On", "light", "turn_on", s.entityId),
            action("Off", "light", "turn_off", s.entityId),
        )
        "switch" -> listOf(
            action("On", "switch", "turn_on", s.entityId),
            action("Off", "switch", "turn_off", s.entityId),
        )
        "cover" -> listOf(
            action("Open", "cover", "open_cover", s.entityId),
            action("Close", "cover", "close_cover", s.entityId),
            action("Stop", "cover", "stop_cover", s.entityId),
        )
        else -> emptyList()
    }

    private fun action(label: String, domain: String, service: String, entityId: String) =
        EntityAction(
            kind = "SERVICE",
            label = label,
            domain = domain,
            service = service,
            data = mapOf("entity_id" to entityId),
        )
}
