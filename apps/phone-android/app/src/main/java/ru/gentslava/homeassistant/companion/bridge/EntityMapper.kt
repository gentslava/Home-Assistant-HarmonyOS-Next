package ru.gentslava.homeassistant.companion.bridge

import ru.gentslava.homeassistant.companion.ha.HaState
import ru.gentslava.homeassistant.companion.p2p.EntityAction
import ru.gentslava.homeassistant.companion.p2p.EntityCard

/**
 * Maps Home Assistant states to our P2P EntityCard/EntityAction (see docs/ha-integration-notes.md).
 * MVP domains: light, switch, lock. Labels are English for now — localization is a later task
 * (the watch renders labels as-is).
 */
object EntityMapper {

    val SUPPORTED_DOMAINS = setOf("light", "switch", "lock")

    fun cards(states: List<HaState>): List<EntityCard> = states.mapNotNull(::toCard)

    fun toCard(s: HaState): EntityCard? {
        if (s.domain !in SUPPORTED_DOMAINS) return null
        val primary = primaryAction(s)
        return EntityCard(
            entityId = s.entityId,
            domain = s.domain,
            name = s.friendlyName,
            state = s.state,
            icon = s.domain,
            primary = primary,
            secondary = secondaryActions(s),
        )
    }

    private fun primaryAction(s: HaState): EntityAction = when (s.domain) {
        "lock" -> if (s.state == "locked") {
            action("Unlock", "lock", "unlock", s.entityId)
        } else {
            action("Lock", "lock", "lock", s.entityId)
        }
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
