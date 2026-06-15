package ru.gentslava.homeassistant.companion.ha

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Home Assistant REST client (see docs/ha-integration-notes.md).
 * Auth via `Authorization: Bearer <token>`. All calls run on Dispatchers.IO and return Result.
 */
class HaClient(private val config: HaConfig) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
        // Whole-call budget so a large /api/states stays within the watch's 8s P2P timeout.
        .callTimeout(CALL_TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    /** GET /api/ -> {"message":"API running."}. Validates host + token. */
    suspend fun checkApi(): Result<Unit> = call("GET", "/api/").map { }

    /**
     * GET /api/states -> all entities. Parsed element-by-element: a single malformed state
     * object (some custom integration) is skipped rather than failing the whole sync — the
     * protocol prefers fast partial data over an all-or-nothing reply.
     */
    suspend fun getStates(): Result<List<HaState>> =
        call("GET", "/api/states").mapCatching { raw ->
            json.parseToJsonElement(raw).jsonArray.mapNotNull { el ->
                runCatching { json.decodeFromJsonElement<HaState>(el) }.getOrNull()
            }
        }

    /** GET /api/states/<entity_id> -> one entity, or null on 404. */
    suspend fun getState(entityId: String): Result<HaState?> =
        call("GET", "/api/states/$entityId")
            .mapCatching { json.decodeFromString<HaState>(it) }
            .recoverCatching { e -> if (e is NotFound) null else throw e }

    /** POST /api/services/<domain>/<service> with `data` as the flat body (see notes). */
    suspend fun callService(domain: String, service: String, data: Map<String, String>): Result<Unit> {
        val body = json.encodeToString(data)
        return call("POST", "/api/services/$domain/$service", body).map { }
    }

    private suspend fun call(method: String, path: String, body: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = config.baseUrl
                require(base.isNotBlank()) { "HA base URL not configured" }
                val req = Request.Builder()
                    .url(base + path)
                    .header("Authorization", "Bearer ${config.token}")
                    .apply {
                        when (method) {
                            "GET" -> get()
                            "POST" -> post((body ?: "{}").toRequestBody(jsonMedia))
                            else -> error("Unsupported method $method")
                        }
                    }
                    .build()
                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    when {
                        resp.isSuccessful -> text
                        resp.code == 401 -> throw HaError("HA returned 401 (check token)")
                        resp.code == 404 -> throw NotFound()
                        else -> throw HaError("HA returned ${resp.code}: ${text.take(ERR_BODY_MAX)}")
                    }
                }
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_S = 5L
        const val READ_TIMEOUT_S = 5L
        const val CALL_TIMEOUT_S = 7L // stay inside the watch's 8s P2P budget
        const val ERR_BODY_MAX = 140
    }
}

class HaError(message: String) : Exception(message)
private class NotFound : Exception("not found")
