package com.kaliki.browser.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URI

data class SavedCredential(
    val domain: String,
    val username: String,
    val password: String,
    val savedAt: Long = System.currentTimeMillis()
)

class PasswordManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kaliki_passwords", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCredential(domain: String, username: String, password: String) {
        val credentials = getAllCredentials().toMutableList()
        // Remove existing entry for same domain+username
        credentials.removeAll { it.domain == domain && it.username == username }
        credentials.add(SavedCredential(domain, username, encode(password)))
        saveAll(credentials)
    }

    fun getCredentialsForDomain(url: String): List<SavedCredential> {
        val domain = extractDomain(url)
        return getAllCredentials()
            .filter { it.domain == domain }
            .map { it.copy(password = decode(it.password)) }
    }

    fun getAllCredentials(): List<SavedCredential> {
        val json = prefs.getString("credentials", "[]") ?: "[]"
        val type = object : TypeToken<List<SavedCredential>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun removeCredential(domain: String, username: String) {
        val credentials = getAllCredentials().toMutableList()
        credentials.removeAll { it.domain == domain && it.username == username }
        saveAll(credentials)
    }

    fun clearAll() {
        prefs.edit().putString("credentials", "[]").apply()
    }

    fun hasCredentialsForDomain(url: String): Boolean {
        return getCredentialsForDomain(url).isNotEmpty()
    }

    private fun saveAll(credentials: List<SavedCredential>) {
        val json = gson.toJson(credentials)
        prefs.edit().putString("credentials", json).apply()
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url
        }
    }

    private fun encode(text: String): String {
        return Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decode(encoded: String): String {
        return try {
            String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: Exception) {
            encoded
        }
    }
}
