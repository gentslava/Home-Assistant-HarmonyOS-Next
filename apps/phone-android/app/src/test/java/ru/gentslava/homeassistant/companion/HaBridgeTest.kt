package ru.gentslava.homeassistant.companion

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gentslava.homeassistant.companion.bridge.HaBridge
import ru.gentslava.homeassistant.companion.ha.HaService
import ru.gentslava.homeassistant.companion.ha.HaState

/** In-memory HaService so the bridge is tested without Wear Engine or HTTP. */
private class FakeHaService(
    private val states: Result<List<HaState>> = Result.success(emptyList()),
    private val state: Result<HaState?> = Result.success(null),
    private val call: Result<Unit> = Result.success(Unit),
) : HaService {
    var lastCall: Triple<String, String, Map<String, String>>? = null
    override suspend fun checkApi() = Result.success(Unit)
    override suspend fun getStates() = states
    override suspend fun getState(entityId: String) = state
    override suspend fun callService(domain: String, service: String, data: Map<String, String>): Result<Unit> {
        lastCall = Triple(domain, service, data)
        return call
    }
}

class HaBridgeTest {
    private fun st(id: String, s: String) =
        HaState(entityId = id, state = s, attributes = buildJsonObject {})

    @Test fun syncRequestReturnsCardsEchoingId() = runBlocking {
        val bridge = HaBridge(FakeHaService(states = Result.success(listOf(st("light.k", "on")))))
        val reply = bridge.handle("""{"v":1,"id":"s1","type":"SYNC_REQUEST"}""")!!
        assertTrue(reply.contains("\"type\":\"SYNC_RESPONSE\""))
        assertTrue(reply.contains("\"id\":\"s1\""))
        assertTrue(reply.contains("light.k"))
    }

    @Test fun syncRequestFailureReturnsErrorAck() = runBlocking {
        val bridge = HaBridge(FakeHaService(states = Result.failure(RuntimeException("boom"))))
        val reply = bridge.handle("""{"v":1,"id":"s2","type":"SYNC_REQUEST"}""")!!
        assertTrue(reply.contains("\"type\":\"ACK\""))
        assertTrue(reply.contains("\"ok\":false"))
        assertTrue(reply.contains("\"id\":\"s2\""))
    }

    @Test fun callServicePassesThroughAndAcks() = runBlocking {
        val fake = FakeHaService()
        val reply = HaBridge(fake).handle(
            """{"v":1,"id":"c1","type":"CALL_SERVICE","domain":"light","service":"toggle","data":{"entity_id":"light.k"}}""",
        )!!
        assertTrue(reply.contains("\"ok\":true"))
        assertEquals(Triple("light", "toggle", mapOf("entity_id" to "light.k")), fake.lastCall)
    }

    @Test fun callServiceFailureSurfacesError() = runBlocking {
        val bridge = HaBridge(FakeHaService(call = Result.failure(RuntimeException("HA returned 401"))))
        val reply = bridge.handle(
            """{"v":1,"id":"c2","type":"CALL_SERVICE","domain":"light","service":"toggle","data":{}}""",
        )!!
        assertTrue(reply.contains("\"ok\":false"))
        assertTrue(reply.contains("HA returned 401"))
    }

    @Test fun unsupportedVersionReturnsErrorAck() = runBlocking {
        val reply = HaBridge(FakeHaService()).handle("""{"v":99,"id":"v1","type":"SYNC_REQUEST"}""")!!
        assertTrue(reply.contains("\"ok\":false"))
        assertTrue(reply.contains("unsupported protocol v99"))
    }

    @Test fun unrecognizedMessageReturnsNull() = runBlocking {
        assertNull(HaBridge(FakeHaService()).handle("not json"))
    }
}
