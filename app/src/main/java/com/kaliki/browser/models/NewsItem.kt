package com.kaliki.browser.models

data class NewsItem(
    val title: String,
    val source: String,
    val url: String,
    val category: String,
    val color: Int = 0xFF1A73E8.toInt(),
    val imageUrl: String = ""
)
