package com.kaliki.browser.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaliki.browser.models.Bookmark

class BookmarkManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("kaliki_bookmarks", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<Bookmark> {
        val json = prefs.getString("bookmarks", "[]")
        val type = object : TypeToken<List<Bookmark>>() {}.type
        return gson.fromJson(json, type)
    }

    fun add(title: String, url: String) {
        val list = getAll().toMutableList()
        if (list.none { it.url == url }) {
            list.add(Bookmark(
                id = System.currentTimeMillis(),
                title = title,
                url = url,
                createdAt = System.currentTimeMillis()
            ))
            save(list)
        }
    }

    fun remove(id: Long) {
        val list = getAll().toMutableList()
        list.removeAll { it.id == id }
        save(list)
    }

    fun isBookmarked(url: String): Boolean {
        return getAll().any { it.url == url }
    }

    private fun save(list: List<Bookmark>) {
        prefs.edit().putString("bookmarks", gson.toJson(list)).apply()
    }
}
