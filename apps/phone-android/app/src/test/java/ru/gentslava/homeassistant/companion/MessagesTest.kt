package ru.gentslava.homeassistant.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.gentslava.homeassistant.companion.p2p.Ack
import ru.gentslava.homeassistant.companion.p2p.CallServiceRequest
import ru.gentslava.homeassistant.companion.p2p.P2pJson
import ru.gentslava.homeassistant.companion.p2p.SyncEntityRequest
import ru.gentslava.homeassistant.companion.p2p.SyncRequest
import ru.gentslava.homeassistant.companion.p2p.parseIncoming

class MessagesTest {

    @Test fun parsesSyncRequest() {
        val msg = parseIncoming("""{"v":1,"id":"sync-1","type":"SYNC_REQUEST"}""")
        assertTrue(msg is SyncRequest)
        assertEquals("sync-1", msg!!.id)
    }

    @Test fun parsesSyncEntityRequest() {
        val msg = parseIncoming(
            """{"v":1,"id":"e-2","type":"SYNC_ENTITY_REQUEST","entity_id":"light.kitchen"}""",
        )
        assertTrue(msg is SyncEntityRequest)
        assertEquals("light.kitchen", (msg as SyncEntityRequest).entityId)
    }

    @Test fun parsesCallService() {
        val msg = parseIncoming(
            """{"v":1,"id":"svc-3","type":"CALL_SERVICE","domain":"light","service":"toggle","data":{"entity_id":"light.kitchen"}}""",
        )
        assertTrue(msg is CallServiceRequest)
        msg as CallServiceRequest
        assertEquals("light", msg.domain)
        assertEquals("toggle", msg.service)
        assertEquals("light.kitchen", msg.data["entity_id"])
    }

    @Test fun unknownTypeReturnsNull() {
        assertNull(parseIncoming("""{"v":1,"id":"x","type":"NONSENSE"}"""))
        assertNull(parseIncoming("not json"))
    }

    @Test fun ackSerializesWithProtocolFields() {
        val json = P2pJson.encodeToString(Ack(id = "svc-3", ok = false, error = "HA returned 401"))
        assertTrue(json.contains("\"type\":\"ACK\""))
        assertTrue(json.contains("\"v\":1"))
        assertTrue(json.contains("\"ok\":false"))
        assertTrue(json.contains("HA returned 401"))
    }
}
