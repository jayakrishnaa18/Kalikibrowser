package com.kaliki.browser.models

import android.graphics.Bitmap
import org.mozilla.geckoview.GeckoSession

data class BrowserTab(
    val id: String,
    var session: GeckoSession,
    var title: String = "New Tab",
    var url: String? = null,
    var isIncognito: Boolean = false,
    var isOnNtp: Boolean = true,
    var thumbnail: Bitmap? = null
)

data class Bookmark(
    val id: Long,
    val title: String,
    val url: String,
    val createdAt: Long
)

data class HistoryEntry(
    val id: Long,
    val title: String,
    val url: String,
    val visitedAt: Long
)

data class ListItem(
    val title: String,
    val subtitle: String,
    val id: String
)
