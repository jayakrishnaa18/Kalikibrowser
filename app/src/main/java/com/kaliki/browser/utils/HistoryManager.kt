package com.kaliki.browser.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaliki.browser.models.HistoryEntry

class HistoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("kaliki_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<HistoryEntry> {
        val json = prefs.getString("history", "[]")
        val type = object : TypeToken<List<HistoryEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addEntry(title: String, url: String) {
        val list = getAll().toMutableList()
        list.add(0, HistoryEntry(
            id = System.currentTimeMillis(),
            title = title,
            url = url,
            visitedAt = System.currentTimeMillis()
        ))
        if (list.size > 10000) {
            save(list.take(10000))
        } else {
            save(list)
        }
    }

    fun remove(id: Long) {
        val list = getAll().toMutableList()
        list.removeAll { it.id == id }
        save(list)
    }

    fun clearAll() {
        save(emptyList())
    }

    fun search(query: String): List<HistoryEntry> {
        val q = query.lowercase()
        return getAll().filter {
            it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
        }.take(50)
    }

    private fun save(list: List<HistoryEntry>) {
        prefs.edit().putString("history", gson.toJson(list)).apply()
    }
}
