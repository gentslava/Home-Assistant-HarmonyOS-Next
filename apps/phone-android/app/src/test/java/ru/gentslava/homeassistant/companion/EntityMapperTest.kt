package ru.gentslava.homeassistant.companion

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gentslava.homeassistant.companion.bridge.EntityMapper
import ru.gentslava.homeassistant.companion.ha.HaState

class EntityMapperTest {

    private fun state(id: String, st: String, name: String? = null) = HaState(
        entityId = id,
        state = st,
        attributes = buildJsonObject { if (name != null) put("friendly_name", name) },
    )

    @Test fun lightMapsToTogglePrimary() {
        val card = EntityMapper.toCard(state("light.kitchen", "on", "Kitchen"))!!
        assertEquals("light", card.domain)
        assertEquals("Kitchen", card.name)
        assertEquals("on", card.state)
        assertEquals("toggle", card.primary.service)
        assertEquals(mapOf("entity_id" to "light.kitchen"), card.primary.data)
    }

    @Test fun switchMapsToToggle() {
        val card = EntityMapper.toCard(state("switch.router", "off"))!!
        assertEquals("toggle", card.primary.service)
        // friendly_name fallback humanizes object_id
        assertEquals("Router", card.name)
    }

    @Test fun lockLockedExposesUnlock() {
        val card = EntityMapper.toCard(state("lock.front_door", "locked"))!!
        assertEquals("unlock", card.primary.service)
        assertEquals("lock", card.primary.domain)
    }

    @Test fun lockUnlockedExposesLock() {
        val card = EntityMapper.toCard(state("lock.front_door", "unlocked"))!!
        assertEquals("lock", card.primary.service)
    }

    @Test fun unsupportedDomainIsFilteredOut() {
        assertNull(EntityMapper.toCard(state("sensor.temperature", "21.5")))
        assertNull(EntityMapper.toCard(state("climate.living", "heat")))
    }

    @Test fun cardsKeepsOnlySupportedDomains() {
        val cards = EntityMapper.cards(
            listOf(
                state("light.a", "on"),
                state("sensor.b", "5"),
                state("lock.c", "locked"),
                state("automation.d", "on"),
            ),
        )
        assertEquals(2, cards.size)
        assertTrue(cards.all { it.domain in EntityMapper.SUPPORTED_DOMAINS })
    }
}
