package com.kaliki.browser.utils

import com.kaliki.browser.models.BrowserTab

class TabManager {

    val tabs = mutableListOf<BrowserTab>()
    private var currentIndex = 0

    fun addTab(tab: BrowserTab) {
        tabs.add(tab)
        currentIndex = tabs.size - 1
    }

    fun removeTab(tab: BrowserTab) {
        val idx = tabs.indexOf(tab)
        tabs.remove(tab)
        if (currentIndex >= tabs.size) currentIndex = tabs.size - 1
        if (currentIndex < 0) currentIndex = 0
    }

    fun switchTo(index: Int) {
        if (index in tabs.indices) currentIndex = index
    }

    fun currentTab(): BrowserTab? {
        return if (tabs.isNotEmpty() && currentIndex in tabs.indices) tabs[currentIndex] else null
    }

    fun currentIndex(): Int = currentIndex
}
