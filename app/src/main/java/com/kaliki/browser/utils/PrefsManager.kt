package com.kaliki.browser.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kaliki_prefs", Context.MODE_PRIVATE)

    fun isAdBlockEnabled(): Boolean = prefs.getBoolean("ad_block", true)
    fun setAdBlock(enabled: Boolean) = prefs.edit().putBoolean("ad_block", enabled).apply()

    fun isTrackerBlockEnabled(): Boolean = prefs.getBoolean("tracker_block", true)
    fun setTrackerBlock(enabled: Boolean) = prefs.edit().putBoolean("tracker_block", enabled).apply()

    fun forceDarkMode(): Boolean = prefs.getBoolean("force_dark", false)
    fun setForceDarkMode(enabled: Boolean) = prefs.edit().putBoolean("force_dark", enabled).apply()

    fun blockThirdPartyCookies(): Boolean = prefs.getBoolean("block_cookies", true)
    fun setBlockThirdPartyCookies(enabled: Boolean) = prefs.edit().putBoolean("block_cookies", enabled).apply()

    fun doNotTrack(): Boolean = prefs.getBoolean("dnt", true)
    fun setDoNotTrack(enabled: Boolean) = prefs.edit().putBoolean("dnt", enabled).apply()

    fun isJavaScriptEnabled(): Boolean = prefs.getBoolean("javascript", true)
    fun setJavaScript(enabled: Boolean) = prefs.edit().putBoolean("javascript", enabled).apply()

    fun loadImages(): Boolean = prefs.getBoolean("load_images", true)
    fun setLoadImages(enabled: Boolean) = prefs.edit().putBoolean("load_images", enabled).apply()

    fun saveData(): Boolean = prefs.getBoolean("save_data", false)
    fun setSaveData(enabled: Boolean) = prefs.edit().putBoolean("save_data", enabled).apply()

    fun getSearchEngine(): String = prefs.getString("search_engine", "https://www.google.com/search?q=")!!
    fun setSearchEngine(url: String) = prefs.edit().putString("search_engine", url).apply()

    fun getUserAgent(defaultUA: String): String {
        val custom = prefs.getString("user_agent", null)
        return custom ?: defaultUA
    }

    fun clearHistory() = prefs.edit().putBoolean("history_cleared", true).apply()
}
