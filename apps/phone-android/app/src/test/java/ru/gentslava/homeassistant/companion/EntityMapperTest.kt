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

    private fun stateWithUnit(id: String, st: String, unit: String) = HaState(
        entityId = id,
        state = st,
        attributes = buildJsonObject { put("unit_of_measurement", unit) },
    )

    @Test fun lightMapsToTogglePrimary() {
        val card = EntityMapper.toCard(state("light.kitchen", "on", "Kitchen"))!!
        assertEquals("light", card.domain)
        assertEquals("Kitchen", card.name)
        assertEquals("on", card.state)
        assertEquals("toggle", card.primary!!.service)
        assertEquals(mapOf("entity_id" to "light.kitchen"), card.primary!!.data)
    }

    @Test fun switchMapsToToggle() {
        val card = EntityMapper.toCard(state("switch.router", "off"))!!
        assertEquals("toggle", card.primary!!.service)
        // friendly_name fallback humanizes object_id
        assertEquals("Router", card.name)
    }

    @Test fun lockLockedExposesUnlock() {
        val card = EntityMapper.toCard(state("lock.front_door", "locked"))!!
        assertEquals("unlock", card.primary!!.service)
        assertEquals("lock", card.primary!!.domain)
    }

    @Test fun lockUnlockedExposesLock() {
        val card = EntityMapper.toCard(state("lock.front_door", "unlocked"))!!
        assertEquals("lock", card.primary!!.service)
    }

    @Test fun coverClosedExposesOpen() {
        val card = EntityMapper.toCard(state("cover.garage", "closed"))!!
        assertEquals("open_cover", card.primary!!.service)
        assertEquals("cover", card.primary!!.domain)
    }

    @Test fun coverOpenExposesClose() {
        val card = EntityMapper.toCard(state("cover.garage", "open"))!!
        assertEquals("close_cover", card.primary!!.service)
    }

    @Test fun coverTransitionalExposesStop() {
        assertEquals("stop_cover", EntityMapper.toCard(state("cover.g", "opening"))!!.primary!!.service)
        assertEquals("stop_cover", EntityMapper.toCard(state("cover.g", "closing"))!!.primary!!.service)
    }

    @Test fun coverHasThreeSecondaries() {
        val card = EntityMapper.toCard(state("cover.garage", "open"))!!
        assertEquals(
            setOf("open_cover", "close_cover", "stop_cover"),
            card.secondary.map { it.service }.toSet(),
        )
    }

    @Test fun sceneExposesTurnOn() {
        val card = EntityMapper.toCard(state("scene.movie", "2026-06-15T10:00:00"))!!
        assertEquals("turn_on", card.primary!!.service)
        assertEquals("scene", card.primary!!.domain)
        assertTrue(card.secondary.isEmpty())
        // The raw timestamp state is masked with a stable token.
        assertEquals("scene", card.state)
    }

    @Test fun sensorHasNoPrimary() {
        val card = EntityMapper.toCard(state("sensor.temperature", "21.5"))!!
        assertNull(card.primary)
        assertTrue(card.secondary.isEmpty())
    }

    @Test fun sensorFoldsUnitIntoState() {
        val card = EntityMapper.toCard(stateWithUnit("sensor.temperature", "21.5", "°C"))!!
        assertEquals("21.5 °C", card.state)
    }

    @Test fun sensorWithoutUnitKeepsRawState() {
        val card = EntityMapper.toCard(state("sensor.humidity", "60"))!!
        assertEquals("60", card.state)
    }

    @Test fun unsupportedDomainIsFilteredOut() {
        assertNull(EntityMapper.toCard(state("climate.living", "heat")))
        assertNull(EntityMapper.toCard(state("media_player.tv", "playing")))
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
        // light, sensor, lock are supported; automation is not.
        assertEquals(3, cards.size)
        assertTrue(cards.all { it.domain in EntityMapper.SUPPORTED_DOMAINS })
    }
}
