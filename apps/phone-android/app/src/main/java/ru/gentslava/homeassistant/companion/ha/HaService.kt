package ru.gentslava.homeassistant.companion.ha

/**
 * Home Assistant data source used by [ru.gentslava.homeassistant.companion.bridge.HaBridge].
 * An interface so the bridge can be unit-tested with a fake (no network), and so a future
 * WebSocket-backed implementation can replace [HaClient] without touching the bridge.
 */
interface HaService {
    suspend fun checkApi(): Result<Unit>
    suspend fun getStates(): Result<List<HaState>>
    suspend fun getState(entityId: String): Result<HaState?>
    suspend fun callService(domain: String, service: String, data: Map<String, String>): Result<Unit>
}
