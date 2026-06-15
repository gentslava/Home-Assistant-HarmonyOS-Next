package ru.gentslava.homeassistant.companion.ha

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    /** GET /api/ -> {"message":"API running."}. Validates host + token. */
    suspend fun checkApi(): Result<Unit> = call("GET", "/api/").map { }

    /** GET /api/states -> all entities. */
    suspend fun getStates(): Result<List<HaState>> =
        call("GET", "/api/states").mapCatching { json.decodeFromString<List<HaState>>(it) }

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
                        else -> throw HaError("HA returned ${resp.code}: ${text.take(140)}")
                    }
                }
            }
        }
}

class HaError(message: String) : Exception(message)
private class NotFound : Exception("not found")
