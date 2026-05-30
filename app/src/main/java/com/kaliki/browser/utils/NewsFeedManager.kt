package com.kaliki.browser.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kaliki.browser.models.NewsItem

class NewsFeedManager(private val context: Context) {

    fun fetchNews(interests: Set<String>, callback: (List<NewsItem>) -> Unit) {
        val langPrefs = getLanguageParams()
        Thread {
            val allNews = mutableListOf<NewsItem>()
            for (interest in interests.take(5)) {
                try {
                    val url = "https://news.google.com/rss/search?q=${interest}&hl=${langPrefs.first}&gl=${langPrefs.second}&ceid=${langPrefs.third}"
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    val xml = connection.inputStream.bufferedReader().readText()
                    connection.disconnect()

                    val items = parseRss(xml, interest)
                    allNews.addAll(items.take(3))
                } catch (_: Exception) {}
            }
            allNews.shuffle()
            Handler(Looper.getMainLooper()).post { callback(allNews) }
        }.start()
    }

    private fun parseRss(xml: String, category: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        val pattern = Regex(
            "<item>.*?<title>(.*?)</title>.*?<link>(.*?)</link>.*?<source.*?>(.*?)</source>.*?</item>",
            RegexOption.DOT_MATCHES_ALL
        )
        for (match in pattern.findAll(xml).take(5)) {
            val title = match.groupValues[1]
                .replace("<![CDATA[", "").replace("]]>", "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'")
                .trim()
            val link = match.groupValues[2]
                .replace("<![CDATA[", "").replace("]]>", "")
                .trim()
            val source = match.groupValues[3].trim()
            items.add(
                NewsItem(
                    title = title,
                    source = source,
                    url = link,
                    category = category.replaceFirstChar { it.uppercase() }
                )
            )
        }
        return items
    }

    fun getUserInterests(): Set<String> {
        val prefs = context.getSharedPreferences("kaliki_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("user_interests", setOf("Technology", "Sports", "World")) ?: setOf("Technology")
    }

    fun saveInterests(interests: Set<String>) {
        context.getSharedPreferences("kaliki_prefs", Context.MODE_PRIVATE)
            .edit()
            .putStringSet("user_interests", interests)
            .putBoolean("interests_selected", true)
            .apply()
    }

    fun isInterestsSelected(): Boolean {
        return context.getSharedPreferences("kaliki_prefs", Context.MODE_PRIVATE)
            .getBoolean("interests_selected", false)
    }

    fun saveLanguage(langCode: String) {
        context.getSharedPreferences("kaliki_prefs", Context.MODE_PRIVATE)
            .edit().putString("news_language", langCode).apply()
    }

    fun getLanguageParams(): Triple<String, String, String> {
        val prefs = context.getSharedPreferences("kaliki_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("news_language", "en") ?: "en"
        return when (lang) {
            "hi" -> Triple("hi", "IN", "IN:hi")
            "te" -> Triple("te", "IN", "IN:te")
            "ta" -> Triple("ta", "IN", "IN:ta")
            "kn" -> Triple("kn", "IN", "IN:kn")
            "ml" -> Triple("ml", "IN", "IN:ml")
            "mr" -> Triple("mr", "IN", "IN:mr")
            "bn" -> Triple("bn", "IN", "IN:bn")
            "gu" -> Triple("gu", "IN", "IN:gu")
            "es" -> Triple("es", "US", "US:es")
            "fr" -> Triple("fr", "FR", "FR:fr")
            "ja" -> Triple("ja", "JP", "JP:ja")
            "de" -> Triple("de", "DE", "DE:de")
            "pt" -> Triple("pt", "BR", "BR:pt")
            else -> Triple("en", "US", "US:en")
        }
    }
}
