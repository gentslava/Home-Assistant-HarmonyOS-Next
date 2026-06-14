package ru.gentslava.homeassistant.companion.ha

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for the HA connection: base URL + long-lived token.
 * Stored in EncryptedSharedPreferences — the token never lands in plaintext or in code.
 */
class HaConfig(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ha_config",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** e.g. http://homeassistant.local:8123 — trailing slash trimmed. */
    var baseUrl: String
        get() = prefs.getString(KEY_URL, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_URL, value.trim().trimEnd('/')).apply() }

    /** Long-lived access token. */
    var token: String
        get() = prefs.getString(KEY_TOKEN, "").orEmpty()
        set(value) { prefs.edit().putString(KEY_TOKEN, value.trim()).apply() }

    val isConfigured: Boolean get() = baseUrl.isNotBlank() && token.isNotBlank()

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_TOKEN = "token"
    }
}
