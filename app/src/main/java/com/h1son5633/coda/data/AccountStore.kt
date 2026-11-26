package com.h1son5633.coda.data

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/** Persists the anonymous device id and, after QR login, the session cookies. */
class AccountStore(context: Context) {

    private val prefs = context.getSharedPreferences("account", Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString(KEY_DEVICE, null) ?: run {
        val id = UUID.randomUUID().toString().replace("-", "").take(24).uppercase()
        prefs.edit().putString(KEY_DEVICE, id).apply()
        id
    }

    fun cookies(): Map<String, String> {
        val raw = prefs.getString(KEY_COOKIES, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveCookies(cookies: Map<String, String>) {
        val merged = cookies().toMutableMap()
        merged.putAll(cookies)
        val obj = JSONObject()
        for ((k, v) in merged) obj.put(k, v)
        prefs.edit().putString(KEY_COOKIES, obj.toString()).apply()
    }

    fun clearCookies() {
        prefs.edit().remove(KEY_COOKIES).apply()
    }

    val isLoggedIn: Boolean get() = !cookies()["MUSIC_U"].isNullOrBlank()

    private companion object {
        const val KEY_DEVICE = "device_id"
        const val KEY_COOKIES = "cookies"
    }
}
