package com.kaliki.browser.activities

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.kaliki.browser.R
import com.kaliki.browser.utils.PasswordManager
import com.kaliki.browser.utils.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var passwordManager: PasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefsManager = PrefsManager(this)
        passwordManager = PasswordManager(this)

        findViewById<ImageButton>(R.id.btn_back_settings).setOnClickListener { finish() }

        setupToggles()
        setupSearchEngine()
        setupPasswords()
        setupExtensions()
        setupSync()
        setupClearData()
        setupAbout()
    }

    private fun setupToggles() {
        val adBlock = findViewById<SwitchMaterial>(R.id.switch_adblock)
        val tracker = findViewById<SwitchMaterial>(R.id.switch_tracker)
        val darkMode = findViewById<SwitchMaterial>(R.id.switch_dark_mode)
        val cookies = findViewById<SwitchMaterial>(R.id.switch_cookies)
        val dnt = findViewById<SwitchMaterial>(R.id.switch_dnt)
        val js = findViewById<SwitchMaterial>(R.id.switch_javascript)
        val images = findViewById<SwitchMaterial>(R.id.switch_images)
        val saveData = findViewById<SwitchMaterial>(R.id.switch_save_data)

        adBlock.isChecked = prefsManager.isAdBlockEnabled()
        tracker.isChecked = prefsManager.isTrackerBlockEnabled()
        darkMode.isChecked = prefsManager.forceDarkMode()
        cookies.isChecked = prefsManager.blockThirdPartyCookies()
        dnt.isChecked = prefsManager.doNotTrack()
        js.isChecked = prefsManager.isJavaScriptEnabled()
        images.isChecked = prefsManager.loadImages()
        saveData.isChecked = prefsManager.saveData()

        adBlock.setOnCheckedChangeListener { _, checked -> prefsManager.setAdBlock(checked) }
        tracker.setOnCheckedChangeListener { _, checked -> prefsManager.setTrackerBlock(checked) }
        darkMode.setOnCheckedChangeListener { _, checked -> prefsManager.setForceDarkMode(checked) }
        cookies.setOnCheckedChangeListener { _, checked -> prefsManager.setBlockThirdPartyCookies(checked) }
        dnt.setOnCheckedChangeListener { _, checked -> prefsManager.setDoNotTrack(checked) }
        js.setOnCheckedChangeListener { _, checked -> prefsManager.setJavaScript(checked) }
        images.setOnCheckedChangeListener { _, checked -> prefsManager.setLoadImages(checked) }
        saveData.setOnCheckedChangeListener { _, checked -> prefsManager.setSaveData(checked) }
    }

    private fun setupSearchEngine() {
        val spinner = findViewById<Spinner>(R.id.spinner_search_engine)
        val engines = arrayOf("Google", "DuckDuckGo", "Bing", "Brave Search", "Ecosia", "Yandex")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, engines)

        val current = prefsManager.getSearchEngine()
        val idx = when {
            current.contains("google") -> 0
            current.contains("duckduckgo") -> 1
            current.contains("bing") -> 2
            current.contains("brave") -> 3
            current.contains("ecosia") -> 4
            current.contains("yandex") -> 5
            else -> 0
        }
        spinner.setSelection(idx)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val url = when (position) {
                    0 -> "https://www.google.com/search?q="
                    1 -> "https://duckduckgo.com/?q="
                    2 -> "https://www.bing.com/search?q="
                    3 -> "https://search.brave.com/search?q="
                    4 -> "https://www.ecosia.org/search?q="
                    5 -> "https://yandex.com/search/?text="
                    else -> "https://www.google.com/search?q="
                }
                prefsManager.setSearchEngine(url)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPasswords() {
        findViewById<LinearLayout>(R.id.section_passwords).setOnClickListener {
            showSavedPasswords()
        }
        val passwordCount = passwordManager.getAllCredentials().size
        findViewById<TextView>(R.id.passwords_count).text = "$passwordCount saved"
    }

    private fun showSavedPasswords() {
        val credentials = passwordManager.getAllCredentials()
        if (credentials.isEmpty()) {
            Toast.makeText(this, "No saved passwords", Toast.LENGTH_SHORT).show()
            return
        }

        val items = credentials.map { "${it.domain} - ${it.username}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Saved Passwords")
            .setItems(items) { _, which ->
                val cred = credentials[which]
                AlertDialog.Builder(this)
                    .setTitle(cred.domain)
                    .setMessage("Username: ${cred.username}\n\nDelete this password?")
                    .setPositiveButton("Delete") { _, _ ->
                        passwordManager.removeCredential(cred.domain, cred.username)
                        Toast.makeText(this, "Password deleted", Toast.LENGTH_SHORT).show()
                        setupPasswords() // Refresh count
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .setNeutralButton("Clear All") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Clear All Passwords?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Clear") { _, _ ->
                        passwordManager.clearAll()
                        Toast.makeText(this, "All passwords cleared", Toast.LENGTH_SHORT).show()
                        setupPasswords()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .show()
    }

    private fun setupExtensions() {
        val extensionsList = findViewById<TextView>(R.id.extensions_list)
        extensionsList.text = "Ad Blocker (Built-in) - Active"
    }

    private fun setupSync() {
        findViewById<LinearLayout>(R.id.section_sync).setOnClickListener {
            Toast.makeText(this, "Sync coming soon! Stay tuned.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClearData() {
        findViewById<Button>(R.id.btn_clear_cache).setOnClickListener {
            getSharedPreferences("kaliki_prefs", MODE_PRIVATE).edit()
                .putBoolean("clear_cache_pending", true).apply()
            Toast.makeText(this, "Cache will be cleared on next launch", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_clear_cookies).setOnClickListener {
            getSharedPreferences("kaliki_prefs", MODE_PRIVATE).edit()
                .putBoolean("clear_cookies_pending", true).apply()
            Toast.makeText(this, "Cookies will be cleared on next launch", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btn_clear_all).setOnClickListener {
            getSharedPreferences("kaliki_prefs", MODE_PRIVATE).edit()
                .putBoolean("clear_cache_pending", true)
                .putBoolean("clear_cookies_pending", true).apply()
            prefsManager.clearHistory()
            Toast.makeText(this, "All data will be cleared on next launch", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAbout() {
        val versionText = findViewById<TextView>(R.id.version_text)
        val version = packageManager.getPackageInfo(packageName, 0).versionName
        versionText.text = "Kaliki Browser v$version (GeckoView)"
    }
}
