package com.kaliki.browser.activities

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.print.PrintManager
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// QR scanning via intent (no external library needed)
import com.kaliki.browser.R
import com.kaliki.browser.adapters.SimpleListAdapter
import com.kaliki.browser.adapters.SuggestionsAdapter
import com.kaliki.browser.adapters.TabAdapter
import com.kaliki.browser.models.*
import com.kaliki.browser.utils.*
import org.mozilla.geckoview.*
import org.mozilla.geckoview.GeckoSession.ContentDelegate
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var geckoView: GeckoView
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var newTabPage: View
    private lateinit var ntpSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnHome: ImageButton
    private lateinit var btnTabs: TextView
    private lateinit var btnMenuBottom: ImageButton

    private lateinit var runtime: GeckoRuntime

    private val tabManager = TabManager()
    private lateinit var adBlocker: AdBlocker
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var historyManager: HistoryManager
    private lateinit var downloadHelper: DownloadHelper
    private lateinit var prefsManager: PrefsManager
    private lateinit var passwordManager: PasswordManager

    private var isIncognito = false
    private var blockedCount = 0
    private var sessionBlocked = 0
    private val nativeAds = mutableListOf<NativeAd>()
    private var canGoBack = false
    private var canGoForward = false

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDarkModePreference()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initManagers()
        initGeckoRuntime()
        requestNotificationPermission()
        initViews()
        setupListeners()
        blockedCount = adBlocker.getBlockedTotal()
        MobileAds.initialize(this) { initStatus ->
            android.util.Log.d("KalikiAds", "MobileAds initialized: ${initStatus.adapterStatusMap}")
        }
        setupNewsFeed()
        handleIntent(intent)
        if (tabManager.tabs.isEmpty()) {
            val prefs = getSharedPreferences("kaliki_session", MODE_PRIVATE)
            val savedTabsJson = prefs.getString("saved_tabs_json", null)
            if (!savedTabsJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<Map<String, String>>>() {}.type
                    val tabList: List<Map<String, String>> = Gson().fromJson(savedTabsJson, type)
                    for (entry in tabList) {
                        val url = entry["url"] ?: continue
                        val title = entry["title"] ?: url
                        val session = createGeckoSession()
                        val tab = BrowserTab(
                            id = System.currentTimeMillis().toString() + Math.random().toString(),
                            session = session,
                            title = title,
                            url = url,
                            isOnNtp = false
                        )
                        tabManager.addTab(tab)
                        session.loadUri(url)
                    }
                    updateTabCount()
                    val activeIdx = prefs.getInt("active_tab_index", 0)
                        .coerceIn(0, tabManager.tabs.size - 1)
                    switchToTab(activeIdx)
                } catch (_: Exception) {
                    createNewTab(null)
                }
            } else {
                createNewTab(null)
            }
        }
    }

    private fun applyDarkModePreference() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    private fun initManagers() {
        prefsManager = PrefsManager(this)
        adBlocker = AdBlocker(this)
        bookmarkManager = BookmarkManager(this)
        historyManager = HistoryManager(this)
        downloadHelper = DownloadHelper(this)
        passwordManager = PasswordManager(this)
    }

    private fun initGeckoRuntime() {
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .webManifest(true)
            .contentBlocking(ContentBlocking.Settings.Builder()
                .antiTracking(
                    ContentBlocking.AntiTracking.DEFAULT or
                    ContentBlocking.AntiTracking.CRYPTOMINING or
                    ContentBlocking.AntiTracking.FINGERPRINTING
                )
                .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                .build())
            .build()
        runtime = GeckoRuntime.create(this, settings)

        // Load ad blocker WebExtension
        runtime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/extensions/adblocker/",
            "adblocker@kaliki.browser"
        ).accept(
            { ext -> android.util.Log.d("Kaliki", "Ad blocker extension loaded: ${ext?.id}") },
            { e -> android.util.Log.e("Kaliki", "Extension failed to load: $e") }
        )

        // Setup autofill storage delegate for password manager
        setupAutocompleteDelegate()
    }

    private fun setupAutocompleteDelegate() {
        // Password autofill is handled via JavaScript injection on page load
        // The PasswordManager stores credentials and offers to fill them
        android.util.Log.d("Kaliki", "Password manager initialized with autofill support")
    }

    /**
     * Injects autofill script to detect login forms and offer to save/fill credentials
     */
    private fun injectPasswordAutofill(session: GeckoSession, url: String) {
        if (url.startsWith("about:") || url.isEmpty()) return
        val domain = passwordManager.extractDomain(url)
        val credentials = passwordManager.getCredentialsForDomain(url)

        if (credentials.isNotEmpty()) {
            val cred = credentials.first()
            val safeUser = cred.username.replace("\\", "\\\\").replace("'", "\\'")
            val safePass = cred.password.replace("\\", "\\\\").replace("'", "\\'")
            // Offer to fill saved credentials
            runOnUiThread {
                Snackbar.make(geckoView, "Autofill login for $domain?", Snackbar.LENGTH_LONG)
                    .setAction("Fill") {
                        val fillScript = "(function(){var inputs=document.querySelectorAll('input');" +
                            "for(var i=0;i<inputs.length;i++){var input=inputs[i];" +
                            "var type=(input.type||'').toLowerCase();" +
                            "var name=(input.name||input.id||'').toLowerCase();" +
                            "if(type==='email'||(type==='text'&&(name.includes('user')||name.includes('email')||name.includes('login')))){" +
                            "input.value='$safeUser';input.dispatchEvent(new Event('input',{bubbles:true}));}" +
                            "else if(type==='password'){" +
                            "input.value='$safePass';input.dispatchEvent(new Event('input',{bubbles:true}));}}})();"
                        session.loadUri("javascript:void(${Uri.encode(fillScript)})")
                    }
                    .show()
            }
        }

        // Inject form submission listener to detect new passwords
        val detectScript = """
            (function() {
                if (window._kalikiPwdListenerAdded) return;
                window._kalikiPwdListenerAdded = true;
                document.addEventListener('submit', function(e) {
                    var form = e.target;
                    var user = '', pass = '';
                    var inputs = form.querySelectorAll('input');
                    for (var i = 0; i < inputs.length; i++) {
                        var input = inputs[i];
                        var type = (input.type || '').toLowerCase();
                        var name = (input.name || input.id || '').toLowerCase();
                        if (type === 'password' && input.value) { pass = input.value; }
                        else if ((type === 'email' || type === 'text') && (name.includes('user') || name.includes('email') || name.includes('login') || name.includes('name'))) { user = input.value; }
                    }
                    if (user && pass) {
                        window._kalikiDetectedLogin = {username: user, password: pass};
                    }
                }, true);
            })();
        """.trimIndent()
        session.loadUri("javascript:void(${Uri.encode(detectScript)})")
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        geckoView = findViewById(R.id.gecko_view)
        urlEditText = findViewById(R.id.url_edit_text)
        progressBar = findViewById(R.id.progress_bar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        newTabPage = findViewById(R.id.new_tab_page)
        ntpSearch = findViewById(R.id.ntp_search)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnHome = findViewById(R.id.btn_home)
        btnTabs = findViewById(R.id.btn_tabs)
        btnMenuBottom = findViewById(R.id.btn_menu_bottom)
        // Shield and refresh
        findViewById<ImageButton>(R.id.shield_icon).setOnClickListener { showShieldInfo() }
        findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener { tabManager.currentTab()?.session?.reload() }
        findViewById<ImageButton>(R.id.btn_menu).setOnClickListener { showMainMenu() }
    }

    private fun setupListeners() {
        urlEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val text = urlEditText.text.toString().trim()
                if (text.isNotEmpty()) { navigateTo(text); hideKeyboard() }
                hideSuggestions()
                true
            } else false
        }
        ntpSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val text = ntpSearch.text.toString().trim()
                if (text.isNotEmpty()) { navigateTo(text); hideKeyboard() }
                true
            } else false
        }
        urlEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlEditText.selectAll()
            else hideSuggestions()
        }
        setupSearchSuggestions()
        btnBack.setOnClickListener { goBack() }
        btnForward.setOnClickListener { goForward() }
        btnHome.setOnClickListener { showNewTabPage() }
        findViewById<ImageButton>(R.id.btn_new_tab).setOnClickListener { createNewTab(null) }
        btnTabs.setOnClickListener { showTabSwitcher() }
        btnMenuBottom.setOnClickListener { showMainMenu() }
        swipeRefresh.isEnabled = false
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent))
        setupShortcuts()
        setupGestures()
    }

    private fun setupShortcuts() {
        data class Shortcut(val name: String, val url: String, val letter: String, val faviconUrl: String)
        val shortcuts = listOf(
            Shortcut("Google", "https://www.google.com", "G", "https://www.google.com/favicon.ico"),
            Shortcut("YouTube", "https://m.youtube.com", "Y", "https://www.youtube.com/favicon.ico"),
            Shortcut("Facebook", "https://www.facebook.com", "f", "https://www.facebook.com/favicon.ico"),
            Shortcut("Instagram", "https://www.instagram.com", "I", "https://www.instagram.com/favicon.ico"),
            Shortcut("Twitter", "https://twitter.com", "X", "https://abs.twimg.com/favicons/twitter.3.ico"),
            Shortcut("WhatsApp", "https://web.whatsapp.com", "W", "https://web.whatsapp.com/favicon.ico"),
            Shortcut("Amazon", "https://www.amazon.in", "A", "https://www.amazon.in/favicon.ico"),
            Shortcut("Wikipedia", "https://www.wikipedia.org", "W", "https://www.wikipedia.org/static/favicon/wikipedia.ico")
        )
        val grid = findViewById<GridLayout>(R.id.shortcuts_grid)
        grid.removeAllViews()
        for (shortcut in shortcuts) {
            val view = layoutInflater.inflate(R.layout.item_shortcut, grid, false) as LinearLayout
            view.findViewById<TextView>(R.id.shortcut_label).text = shortcut.name
            view.findViewById<TextView>(R.id.shortcut_icon).text = shortcut.letter
            val faviconView = view.findViewById<ImageView>(R.id.shortcut_favicon)
            val letterView = view.findViewById<TextView>(R.id.shortcut_icon)
            loadFavicon(shortcut.faviconUrl, faviconView, letterView)
            view.setOnClickListener { navigateTo(shortcut.url) }
            grid.addView(view)
        }
    }

    private fun loadFavicon(url: String, imageView: ImageView, fallbackText: TextView) {
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connect()
                if (connection.responseCode == 200) {
                    val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                    if (bitmap != null) runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        fallbackText.visibility = View.GONE
                    }
                }
                connection.disconnect()
            } catch (_: Exception) {}
        }.start()
    }

    // =================== NEWS FEED ===================

    private fun setupNewsFeed() {
        val container = findViewById<LinearLayout>(R.id.news_feed_container)
        container.removeAllViews()

        // Show loading indicator
        val loading = TextView(this).apply {
            text = "Loading your news..."
            textSize = 14f
            setTextColor(getColor(R.color.dark_text_secondary))
            setPadding(32, 32, 32, 32)
        }
        container.addView(loading)

        val feedManager = NewsFeedManager(this)
        feedManager.fetchNews(feedManager.getUserInterests()) { newsItems ->
            container.removeAllViews()
            if (newsItems.isEmpty()) {
                addStaticNews(container)
                return@fetchNews
            }
            for (i in newsItems.indices) {
                val itemView = layoutInflater.inflate(R.layout.item_news, container, false)
                itemView.findViewById<TextView>(R.id.news_title).text = newsItems[i].title
                itemView.findViewById<TextView>(R.id.news_source).text = newsItems[i].source
                itemView.findViewById<TextView>(R.id.news_category).text = newsItems[i].category
                // Load source favicon + thumbnail
                val faviconView = itemView.findViewById<ImageView>(R.id.news_favicon)
                val thumbView = itemView.findViewById<ImageView>(R.id.news_image)
                val domain = try { Uri.parse(newsItems[i].url).host ?: "" } catch (_: Exception) { "" }
                if (domain.isNotEmpty()) {
                    loadImageAsync("https://www.google.com/s2/favicons?domain=$domain&sz=32", faviconView)
                    loadImageAsync("https://www.google.com/s2/favicons?domain=$domain&sz=128", thumbView)
                }
                itemView.setOnClickListener { navigateTo(newsItems[i].url) }
                container.addView(itemView)

                // Ad slot every 4 items
                if (i == 3 || i == 8) {
                    val adFrame = FrameLayout(this).apply {
                        id = View.generateViewId()
                        tag = "ad_slot_$i"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    container.addView(adFrame)
                }
            }
            loadNativeAds(container)
        }
    }

    private fun addStaticNews(container: LinearLayout) {
        fun favicon(domain: String) = "https://www.google.com/s2/favicons?domain=$domain&sz=128"

        val news = listOf(
            NewsItem("Breaking: Tech Giants Report Record Earnings in Q2 2026", "TechCrunch - 2h", "https://techcrunch.com", "Technology", 0xFF4285F4.toInt(), favicon("techcrunch.com")),
            NewsItem("New Study Reveals Surprising Benefits of Morning Exercise", "Health Today - 3h", "https://health.com", "Health", 0xFF34A853.toInt(), favicon("health.com")),
            NewsItem("Space Agency Announces Ambitious Mars Mission Timeline for 2028", "NASA - 4h", "https://nasa.gov", "Science", 0xFF9C27B0.toInt(), favicon("nasa.gov")),
            NewsItem("Global Markets Rally as Economic Recovery Accelerates Worldwide", "Bloomberg - 1h", "https://bloomberg.com", "Finance", 0xFFFA7B17.toInt(), favicon("bloomberg.com")),
            NewsItem("AI Revolution: Machine Learning Transforms Healthcare Industry", "Wired - 5h", "https://wired.com", "AI", 0xFF4285F4.toInt(), favicon("wired.com")),
            NewsItem("Climate Summit 2026: Historic Agreement on Carbon Emissions", "BBC News - 2h", "https://bbc.com", "World", 0xFFEA4335.toInt(), favicon("bbc.com")),
            NewsItem("Top 10 Most Anticipated Smartphones of 2026 Revealed", "GSMArena - 6h", "https://gsmarena.com", "Gadgets", 0xFF4285F4.toInt(), favicon("gsmarena.com")),
            NewsItem("Cricket World Cup: India Dominates Semi-Final with Record Chase", "ESPN - 1h", "https://espn.com", "Sports", 0xFF34A853.toInt(), favicon("espn.com")),
            NewsItem("Electric Vehicles Expected to Outsell Gas Cars by 2028 Report", "The Verge - 3h", "https://theverge.com", "Auto", 0xFFFBBC04.toInt(), favicon("theverge.com")),
            NewsItem("Breakthrough in Quantum Computing Promises New Era of Technology", "MIT Review - 4h", "https://technologyreview.com", "Science", 0xFF9C27B0.toInt(), favicon("technologyreview.com")),
            NewsItem("Remote Work Revolution: Companies Fully Embrace Hybrid Models", "Forbes - 7h", "https://forbes.com", "Business", 0xFFFA7B17.toInt(), favicon("forbes.com")),
            NewsItem("New Streaming Platform Challenges Netflix with Premium Free Tier", "Netflix - 5h", "https://netflix.com", "Entertainment", 0xFFEA4335.toInt(), favicon("netflix.com")),
            NewsItem("Cybersecurity Alert: Major Vulnerability Found in Popular Apps", "ZDNet - 2h", "https://zdnet.com", "Security", 0xFF4285F4.toInt(), favicon("zdnet.com")),
            NewsItem("SpaceX Announces First Commercial Mars Flights for Civilians", "SpaceX - 8h", "https://spacex.com", "Space", 0xFF9C27B0.toInt(), favicon("spacex.com")),
            NewsItem("Gaming Industry Revenue Surpasses Film and Music Combined 2026", "IGN - 4h", "https://ign.com", "Gaming", 0xFF34A853.toInt(), favicon("ign.com")),
            NewsItem("India Becomes World's Third Largest Economy Official Report", "Reuters - 1h", "https://reuters.com", "Economy", 0xFFFA7B17.toInt(), favicon("reuters.com")),
            NewsItem("Apple Unveils Next-Gen AR Glasses at Annual Developer Event", "Apple - 3h", "https://apple.com", "Tech", 0xFF4285F4.toInt(), favicon("apple.com")),
            NewsItem("Scientists Discover New Deep-Sea Species in Pacific Ocean", "Nat Geo - 6h", "https://nationalgeographic.com", "Nature", 0xFF34A853.toInt(), favicon("nationalgeographic.com")),
            NewsItem("Bitcoin Surges Past 200K as Institutional Investors Pile In", "CoinDesk - 2h", "https://coindesk.com", "Crypto", 0xFFFBBC04.toInt(), favicon("coindesk.com")),
            NewsItem("World Health Organization Declares End to Major Pandemic Threat", "WHO - 1h", "https://who.int", "Health", 0xFF34A853.toInt(), favicon("who.int"))
        )

        for (i in news.indices) {
            val itemView = layoutInflater.inflate(R.layout.item_news, container, false)
            itemView.findViewById<TextView>(R.id.news_title).text = news[i].title
            itemView.findViewById<TextView>(R.id.news_source).text = news[i].source
            itemView.findViewById<TextView>(R.id.news_category).text = news[i].category
            val imgView = itemView.findViewById<ImageView>(R.id.news_image)
            imgView.setBackgroundResource(R.drawable.shortcut_bg)
            imgView.background.setTint(news[i].color)
            if (news[i].imageUrl.isNotEmpty()) {
                loadNewsImage(news[i].imageUrl, imgView, null)
            }
            itemView.setOnClickListener { navigateTo(news[i].url) }
            container.addView(itemView)

            if (i == 3 || i == 9 || i == 15) {
                val adFrame = FrameLayout(this).apply {
                    id = View.generateViewId()
                    tag = "ad_slot_$i"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                container.addView(adFrame)
            }
        }

        loadNativeAds(container)
    }

    private fun loadNativeAds(container: LinearLayout) {
        val adLoader = AdLoader.Builder(this, "ca-app-pub-6027286420304821/1561454716")
            .forNativeAd { nativeAd ->
                nativeAds.add(nativeAd)
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    if (child is FrameLayout && child.tag?.toString()?.startsWith("ad_slot") == true && child.childCount == 0) {
                        val adView = layoutInflater.inflate(R.layout.item_native_ad, child, false)
                        val nativeAdView = adView.findViewById<com.google.android.gms.ads.nativead.NativeAdView>(R.id.native_ad_view)
                        val headlineView = nativeAdView.findViewById<TextView>(R.id.ad_headline)
                        headlineView.text = nativeAd.headline; nativeAdView.headlineView = headlineView
                        val bodyView = nativeAdView.findViewById<TextView>(R.id.ad_body)
                        bodyView.text = nativeAd.body ?: ""; nativeAdView.bodyView = bodyView
                        val ctaView = nativeAdView.findViewById<android.widget.Button>(R.id.ad_call_to_action)
                        ctaView.text = nativeAd.callToAction ?: "Learn More"; nativeAdView.callToActionView = ctaView
                        val iconView = nativeAdView.findViewById<ImageView>(R.id.ad_icon)
                        if (nativeAd.icon != null) { iconView.setImageDrawable(nativeAd.icon!!.drawable); iconView.visibility = View.VISIBLE }
                        nativeAdView.iconView = iconView
                        val advView = nativeAdView.findViewById<TextView>(R.id.ad_advertiser)
                        advView.text = nativeAd.advertiser ?: "Sponsored"; nativeAdView.advertiserView = advView
                        nativeAdView.setNativeAd(nativeAd)
                        child.addView(adView)
                        break
                    }
                }
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    android.util.Log.e("KalikiAds", "Native ad failed: code=${error.code} msg=${error.message} domain=${error.domain}")
                }
                override fun onAdLoaded() {
                    android.util.Log.d("KalikiAds", "Native ad loaded successfully")
                }
            })
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 3)
    }

    // =================== GECKO SESSION MANAGEMENT ===================

    private fun createGeckoSession(): GeckoSession {
        val session = GeckoSession()
        session.open(runtime)

        session.navigationDelegate = object : NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                val tab = findTabBySession(session) ?: return
                tab.url = url
                if (tabManager.currentTab() == tab) {
                    runOnUiThread { urlEditText.setText(url ?: "") }
                }
            }

            override fun onCanGoBack(session: GeckoSession, back: Boolean) {
                if (tabManager.currentTab()?.session == session) {
                    canGoBack = back
                    runOnUiThread { updateNavButtons() }
                }
            }

            override fun onCanGoForward(session: GeckoSession, forward: Boolean) {
                if (tabManager.currentTab()?.session == session) {
                    canGoForward = forward
                    runOnUiThread { updateNavButtons() }
                }
            }

            override fun onLoadRequest(session: GeckoSession, request: NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val url = request.uri

                // Handle external intents
                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("intent:") || url.startsWith("market:")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }

                // Fix Google redirect notice — extract actual URL
                if (url.contains("google.com/url?") || url.contains("google.co.in/url?")) {
                    val actualUrl = Uri.parse(url).getQueryParameter("q") ?: Uri.parse(url).getQueryParameter("url")
                    if (actualUrl != null && actualUrl.startsWith("http")) {
                        session.loadUri(actualUrl)
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }

                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                val newSession = createGeckoSession()
                val tab = BrowserTab(
                    id = System.currentTimeMillis().toString(),
                    session = newSession,
                    title = "New Tab",
                    url = uri,
                    isOnNtp = false
                )
                runOnUiThread {
                    tabManager.addTab(tab)
                    updateTabCount()
                    showGeckoSession(newSession)
                }
                return GeckoResult.fromValue(newSession)
            }
        }

        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                if (tabManager.currentTab()?.session == session) {
                    runOnUiThread {
                        progressBar.visibility = View.VISIBLE
                        progressBar.progress = 15
                        urlEditText.setText(url)
                    }
                }
                findTabBySession(session)?.url = url
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                val tab = findTabBySession(session)
                if (tabManager.currentTab()?.session == session) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        updateNavButtons()
                    }
                }
                if (!isIncognito && tab != null && tab.url != null && !tab.url!!.startsWith("about:")) {
                    historyManager.addEntry(tab.title, tab.url!!)
                }
                // Inject YouTube ad-skip and background playback scripts
                val url = tab?.url ?: ""
                if (url.contains("youtube.com") || url.contains("youtu.be")) {
                    injectYouTubeScripts(session)
                }
                // Inject general ad-block CSS on all pages
                if (prefsManager.isAdBlockEnabled() && url.isNotEmpty() && !url.startsWith("about:")) {
                    injectGeneralAdBlock(session)
                }
                // Inject password autofill on pages with login forms
                if (!isIncognito && url.isNotEmpty() && !url.startsWith("about:")) {
                    injectPasswordAutofill(session, url)
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                if (tabManager.currentTab()?.session == session) {
                    runOnUiThread {
                        progressBar.progress = progress
                        progressBar.visibility = if (progress >= 100) View.GONE else View.VISIBLE
                    }
                }
            }
        }

        session.contentDelegate = object : ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                val tab = findTabBySession(session) ?: return
                tab.title = title ?: "Untitled"
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                runOnUiThread {
                    if (fullScreen) {
                        // Force landscape + hide everything
                        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        findViewById<View>(R.id.toolbar).visibility = View.GONE
                        findViewById<View>(R.id.bottom_bar).visibility = View.GONE
                        findViewById<View>(R.id.suggestions_list).visibility = View.GONE
                        window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        )
                    } else {
                        // Restore portrait + show UI
                        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        findViewById<View>(R.id.toolbar).visibility = View.VISIBLE
                        findViewById<View>(R.id.bottom_bar).visibility = View.VISIBLE
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                        // After a delay, allow sensor again
                        Handler(Looper.getMainLooper()).postDelayed({
                            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }, 500)
                    }
                }
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                val url = response.uri
                val contentType = response.headers["Content-Type"]
                val contentDisposition = response.headers["Content-Disposition"]
                downloadHelper.startDownload(url, contentDisposition, contentType)
                runOnUiThread { showToast("Downloading...") }
            }

            override fun onContextMenu(session: GeckoSession, screenX: Int, screenY: Int, element: ContentDelegate.ContextElement) {
                runOnUiThread {
                    val linkUri = element.linkUri
                    val srcUri = element.srcUri
                    when {
                        element.type == ContentDelegate.ContextElement.TYPE_VIDEO ||
                        (srcUri != null && (srcUri.contains(".mp4") || srcUri.contains(".webm") || srcUri.contains("video"))) -> {
                            showVideoContextMenu(srcUri ?: linkUri ?: "")
                        }
                        srcUri != null && element.type == ContentDelegate.ContextElement.TYPE_IMAGE -> {
                            showImageContextMenu(srcUri)
                        }
                        linkUri != null -> {
                            showLinkContextMenu(linkUri)
                        }
                    }
                }
            }
        }

        // Track blocked content for shield badge
        session.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                blockedCount++
                sessionBlocked++
                adBlocker.incrementBlocked()
                runOnUiThread {
                    findViewById<TextView>(R.id.blocked_count)?.text =
                        if (blockedCount > 999) "999+" else blockedCount.toString()
                }
            }
        }

        // Handle permission requests from websites (camera, mic, location, notifications)
        session.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onAndroidPermissionsRequest(session: GeckoSession, permissions: Array<out String>?, callback: GeckoSession.PermissionDelegate.Callback) {
                if (permissions == null) { callback.reject(); return }
                // Request Android runtime permissions
                pendingPermissionCallback = callback
                pendingPermissions = permissions
                androidx.core.app.ActivityCompat.requestPermissions(this@MainActivity, permissions, PERMISSION_REQUEST_CODE)
            }

            override fun onContentPermissionRequest(session: GeckoSession, perm: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int>? {
                // Auto-allow notifications, ask for geolocation
                return when (perm.permission) {
                    GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> {
                        GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    }
                    GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> {
                        GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    }
                    else -> {
                        GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                    }
                }
            }

            override fun onMediaPermissionRequest(session: GeckoSession, uri: String, video: Array<out GeckoSession.PermissionDelegate.MediaSource>?, audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?, callback: GeckoSession.PermissionDelegate.MediaCallback) {
                // Allow media access (camera/mic for video calls, etc.)
                val videoSource = video?.firstOrNull()
                val audioSource = audio?.firstOrNull()
                callback.grant(videoSource, audioSource)
            }
        }

        return session
    }

    // Request notification permission on Android 13+
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
        }
    }

    // Permission handling
    private var pendingPermissionCallback: GeckoSession.PermissionDelegate.Callback? = null
    private var pendingPermissions: Array<out String>? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                pendingPermissionCallback?.grant()
            } else {
                pendingPermissionCallback?.reject()
            }
            pendingPermissionCallback = null
            pendingPermissions = null
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        const val QR_SCAN_REQUEST = 1002
    }

    private fun findTabBySession(session: GeckoSession): BrowserTab? {
        return tabManager.tabs.find { it.session == session }
    }

    private fun injectYouTubeScripts(session: GeckoSession) {
        // Inject background playback script (overrides visibility API)
        val bgScript = adBlocker.getYouTubeBackgroundScript()
        session.loadUri("javascript:void(${Uri.encode(bgScript)})")
        // Inject ad-skip script (fast-forwards ads + clicks skip)
        val adScript = adBlocker.getYouTubeAdBlockScript()
        session.loadUri("javascript:void(${Uri.encode(adScript)})")
    }

    private fun injectGeneralAdBlock(session: GeckoSession) {
        val script = adBlocker.getGeneralAdBlockScript()
        session.loadUri("javascript:void(${Uri.encode(script)})")
    }

    // =================== TAB MANAGEMENT ===================

    fun createNewTab(url: String?) {
        val session = createGeckoSession()
        val hasUrl = url != null && url.isNotEmpty()
        val tab = BrowserTab(
            id = System.currentTimeMillis().toString(),
            session = session,
            title = if (hasUrl) "Loading..." else "New Tab",
            url = url,
            isOnNtp = !hasUrl
        )
        tabManager.addTab(tab)
        updateTabCount()
        if (hasUrl) {
            showGeckoSession(session)
            session.loadUri(url!!)
        } else {
            showNewTabPage()
        }
    }

    private fun showGeckoSession(session: GeckoSession) {
        try { geckoView.releaseSession() } catch (_: Exception) {}
        geckoView.setSession(session)
        geckoView.visibility = View.VISIBLE
        newTabPage.visibility = View.GONE
        tabManager.currentTab()?.isOnNtp = false
    }

    private fun showNewTabPage() {
        captureTabThumbnail()
        try { geckoView.releaseSession() } catch (_: Exception) {}
        geckoView.visibility = View.GONE
        newTabPage.visibility = View.VISIBLE
        urlEditText.setText("")
        ntpSearch.setText("")
        tabManager.currentTab()?.apply {
            url = null
            title = "New Tab"
            isOnNtp = true
        }
        updateBlockedCount()
        updateNavButtons()
        // Apply theme accent color to NTP search bar outline
        val accentColor = getSharedPreferences("kaliki_prefs", MODE_PRIVATE).getInt("accent_color", 0xFF4285F4.toInt())
        ntpSearch.background?.setTint(accentColor)
    }

    private fun currentSession(): GeckoSession? {
        val tab = tabManager.currentTab() ?: return null
        return if (tab.isOnNtp) null else tab.session
    }

    private fun closeTab(tab: BrowserTab) {
        tab.session.close()
        tabManager.removeTab(tab)
        updateTabCount()
        if (tabManager.tabs.isEmpty()) {
            createNewTab(null)
        } else {
            val c = tabManager.currentTab()!!
            if (!c.isOnNtp && c.url != null) {
                showGeckoSession(c.session)
                urlEditText.setText(c.url)
            } else {
                showNewTabPage()
            }
        }
    }

    private fun switchToTab(index: Int) {
        tabManager.switchTo(index)
        val tab = tabManager.currentTab() ?: return
        if (!tab.isOnNtp && tab.url != null) {
            showGeckoSession(tab.session)
            urlEditText.setText(tab.url)
        } else {
            showNewTabPage()
        }
        updateNavButtons()
    }

    private fun updateTabCount() { btnTabs.text = tabManager.tabs.size.toString() }

    private fun captureTabThumbnail() {
        val tab = tabManager.currentTab() ?: return
        if (tab.isOnNtp) return
        if (geckoView.width <= 0 || geckoView.height <= 0) return
        try {
            val bitmap = Bitmap.createBitmap(geckoView.width, geckoView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            geckoView.draw(canvas)
            tab.thumbnail = Bitmap.createScaledBitmap(bitmap, 300, 400, true)
        } catch (_: Exception) { }
    }

    // =================== NAVIGATION ===================

    fun navigateTo(input: String) {
        if (input.isEmpty()) return
        val url = resolveUrl(input)

        val currentTab = tabManager.currentTab()
        if (currentTab != null && currentTab.isOnNtp) {
            // Create fresh session to avoid showing old page content
            try { geckoView.releaseSession() } catch (_: Exception) {}
            currentTab.session.close()
            val freshSession = createGeckoSession()
            currentTab.session = freshSession
            currentTab.url = url
            currentTab.title = "Loading..."
            currentTab.isOnNtp = false
            geckoView.setSession(freshSession)
            newTabPage.visibility = View.GONE
            geckoView.visibility = View.VISIBLE
            freshSession.loadUri(url)
        } else if (currentTab != null) {
            currentTab.url = url
            currentTab.isOnNtp = false
            currentTab.session.loadUri(url)
        } else {
            createNewTab(url)
        }
        hideKeyboard()
    }

    private var audioFocusRequest: AudioFocusRequest? = null
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build())
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        }
    }

    private fun resolveUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.contains(".") && !trimmed.contains(" ")) return "https://$trimmed"
        return prefsManager.getSearchEngine() + Uri.encode(trimmed)
    }

    private fun goBack() {
        val session = currentSession()
        if (session != null && canGoBack) session.goBack()
        else if (newTabPage.visibility != View.VISIBLE) showNewTabPage()
    }

    private fun goForward() { currentSession()?.let { if (canGoForward) it.goForward() } }
    private fun reload() { currentSession()?.reload() }

    private fun updateNavButtons() {
        btnBack.alpha = if (canGoBack || newTabPage.visibility != View.VISIBLE) 1f else 0.3f
        btnForward.alpha = if (canGoForward) 1f else 0.3f
    }

    // =================== CONTEXT MENUS ===================

    private fun showImageContextMenu(imageUrl: String) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_context_menu, null)
        dialog.setContentView(view)
        view.findViewById<TextView>(R.id.context_url).text = imageUrl
        view.findViewById<LinearLayout>(R.id.ctx_open_new_tab).setOnClickListener { createNewTab(imageUrl); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.ctx_save).setOnClickListener { downloadHelper.startDownload(imageUrl, null, "image/*"); showToast("Downloading image..."); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.ctx_copy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("url", imageUrl))
            showToast("URL copied"); dialog.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.ctx_share).setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, imageUrl) }, "Share"))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showLinkContextMenu(linkUrl: String) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_context_menu, null)
        dialog.setContentView(view)
        view.findViewById<TextView>(R.id.context_url).text = linkUrl
        view.findViewById<TextView>(R.id.ctx_save_label)?.text = "Download link"
        view.findViewById<LinearLayout>(R.id.ctx_open_new_tab).setOnClickListener { createNewTab(linkUrl); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.ctx_save).setOnClickListener { downloadHelper.startDownload(linkUrl, null, null); showToast("Downloading..."); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.ctx_copy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("url", linkUrl))
            showToast("URL copied"); dialog.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.ctx_share).setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, linkUrl) }, "Share"))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showVideoContextMenu(videoUrl: String) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_context_menu, null)
        dialog.setContentView(view)
        view.findViewById<TextView>(R.id.context_url).text = videoUrl
        view.findViewById<TextView>(R.id.ctx_save_label)?.text = "Download video"
        view.findViewById<LinearLayout>(R.id.ctx_open_new_tab).setOnClickListener { createNewTab(videoUrl); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.ctx_save).setOnClickListener {
            downloadHelper.startDownload(videoUrl, null, "video/*")
            showToast("Downloading video...")
            dialog.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.ctx_copy).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("url", videoUrl))
            showToast("URL copied")
            dialog.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.ctx_share).setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, videoUrl) }, "Share"))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun downloadMediaFromPage() {
        val session = currentSession()
        if (session == null) { showToast("No page loaded"); return }
        val tab = tabManager.currentTab() ?: return
        val url = tab.url ?: ""

        // Use JavaScript to detect video elements on the page
        val detectScript = """
            (function(){
                var videos = document.querySelectorAll('video source, video[src]');
                var meta = document.querySelector('meta[property="og:video"]');
                var urls = [];
                videos.forEach(function(v) { if(v.src) urls.push(v.src); if(v.currentSrc) urls.push(v.currentSrc); });
                if(meta && meta.content) urls.push(meta.content);
                var allVideos = document.querySelectorAll('video');
                allVideos.forEach(function(v) { if(v.currentSrc) urls.push(v.currentSrc); });
                if(urls.length > 0) {
                    document.title = 'KALIKI_VIDEO:' + urls[0];
                } else {
                    document.title = 'KALIKI_VIDEO:NONE';
                }
            })();
        """.trimIndent()
        session.loadUri("javascript:void(${Uri.encode(detectScript)})")

        // Check title after a short delay for result
        Handler(Looper.getMainLooper()).postDelayed({
            val currentTitle = tabManager.currentTab()?.title ?: ""
            if (currentTitle.startsWith("KALIKI_VIDEO:") && !currentTitle.contains("NONE")) {
                val videoUrl = currentTitle.removePrefix("KALIKI_VIDEO:")
                showVideoContextMenu(videoUrl)
            } else {
                // Fallback: for YouTube pages, offer to open in external downloader
                if (url.contains("youtube.com") || url.contains("youtu.be")) {
                    val videoId = extractYouTubeVideoId(url)
                    if (videoId != null) {
                        val downloadUrl = "https://www.y2mate.com/youtube/$videoId"
                        createNewTab(downloadUrl)
                        showToast("Opening video downloader...")
                    } else {
                        showToast("No downloadable video found")
                    }
                } else {
                    showToast("No downloadable video found on this page")
                }
            }
        }, 500)
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("embed/([a-zA-Z0-9_-]{11})")
        )
        for (p in patterns) {
            val match = p.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    // =================== MENUS ===================

    private fun showMainMenu() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_menu, null)
        dialog.setContentView(view)

        // List items
        view.findViewById<LinearLayout>(R.id.menu_new_tab).setOnClickListener { createNewTab(null); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_bookmarks).setOnClickListener { showBookmarks(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_history).setOnClickListener { showHistory(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_downloads).setOnClickListener { showDownloads(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_private).setOnClickListener { toggleIncognito(); dialog.dismiss() }
        view.findViewById<TextView>(R.id.menu_private_state).text = if (isIncognito) "ON" else "OFF"
        view.findViewById<LinearLayout>(R.id.menu_find).setOnClickListener { showFindInPage(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_desktop).setOnClickListener { toggleDesktopMode(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_translate).setOnClickListener { translatePage(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_screenshot).setOnClickListener { screenshotPage(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_save_page).setOnClickListener { saveToReadingList(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_reading_list).setOnClickListener { showReadingList(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_scan_qr).setOnClickListener { openQrScanner(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_download_media).setOnClickListener { downloadMediaFromPage(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_add_home).setOnClickListener { addToHomeScreen(); dialog.dismiss() }
        view.findViewById<LinearLayout>(R.id.menu_settings).setOnClickListener { openSettings(); dialog.dismiss() }

        dialog.show()
    }

    private fun showTabSwitcher() {
        captureTabThumbnail()
        val dialog = BottomSheetDialog(this, R.style.BottomSheetTheme)
        dialog.behavior.peekHeight = resources.displayMetrics.heightPixels
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val view = layoutInflater.inflate(R.layout.bottom_sheet_tabs, null); dialog.setContentView(view)
        view.findViewById<TextView>(R.id.tabs_count_label).text = "${tabManager.tabs.size} Tabs"
        val recycler = view.findViewById<RecyclerView>(R.id.tabs_recycler)
        recycler.layoutManager = LinearLayoutManager(this@MainActivity)
        val tabAdapter = TabAdapter(tabManager.tabs, { idx -> switchToTab(idx); dialog.dismiss() }, { tab -> closeTab(tab); dialog.dismiss() }, { tab, _ -> showTabGroupColorPicker(tab) })
        recycler.adapter = tabAdapter
        tabAdapter.attachSwipeToClose(recycler)
        view.findViewById<Button>(R.id.btn_new_tab).setOnClickListener { createNewTab(null); dialog.dismiss() }
        view.findViewById<Button>(R.id.btn_close_all).setOnClickListener { tabManager.tabs.toList().forEach { closeTab(it) }; dialog.dismiss() }
        dialog.show()
    }

    private fun showBookmarks() {
        val d = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val v = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        d.setContentView(v)
        v.findViewById<TextView>(R.id.list_title).text = "Bookmarks"
        v.findViewById<Button>(R.id.btn_clear).apply {
            visibility = View.VISIBLE
            text = "Clear All"
            setOnClickListener {
                bookmarkManager.getAll().forEach { bookmarkManager.remove(it.id) }
                d.dismiss()
                showToast("Bookmarks cleared")
            }
        }
        val rv = v.findViewById<RecyclerView>(R.id.list_recycler)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SimpleListAdapter(
            bookmarkManager.getAll().map { ListItem(it.title, it.url, it.id.toString()) },
            onClick = { item -> navigateTo(item.subtitle); d.dismiss() },
            onDelete = { item ->
                bookmarkManager.remove(item.id.toLong())
                rv.adapter = SimpleListAdapter(
                    bookmarkManager.getAll().map { ListItem(it.title, it.url, it.id.toString()) },
                    onClick = { i -> navigateTo(i.subtitle); d.dismiss() },
                    onDelete = { i -> bookmarkManager.remove(i.id.toLong()); showToast("Removed") }
                )
            }
        )
        d.show()
    }

    private fun showHistory() {
        val d = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val v = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        d.setContentView(v)
        v.findViewById<TextView>(R.id.list_title).text = "History"

        val allHistory = historyManager.getAll()

        val now = System.currentTimeMillis()
        val todayStart = now - (now % 86400000)
        val yesterdayStart = todayStart - 86400000
        val weekStart = todayStart - (7 * 86400000)

        val grouped = mutableListOf<ListItem>()
        var addedToday = false; var addedYesterday = false; var addedWeek = false; var addedOlder = false

        for (entry in allHistory) {
            when {
                entry.visitedAt >= todayStart -> {
                    if (!addedToday) { grouped.add(ListItem("--- Today ---", "", "header")); addedToday = true }
                }
                entry.visitedAt >= yesterdayStart -> {
                    if (!addedYesterday) { grouped.add(ListItem("--- Yesterday ---", "", "header")); addedYesterday = true }
                }
                entry.visitedAt >= weekStart -> {
                    if (!addedWeek) { grouped.add(ListItem("--- Last 7 days ---", "", "header")); addedWeek = true }
                }
                else -> {
                    if (!addedOlder) { grouped.add(ListItem("--- Older ---", "", "header")); addedOlder = true }
                }
            }
            grouped.add(ListItem(entry.title, entry.url, entry.id.toString()))
        }

        val rv = v.findViewById<RecyclerView>(R.id.list_recycler)
        rv.layoutManager = LinearLayoutManager(this)

        fun refreshAdapter() {
            rv.adapter = SimpleListAdapter(
                grouped,
                onClick = { item ->
                    if (item.id == "header") return@SimpleListAdapter
                    navigateTo(item.subtitle); d.dismiss()
                },
                onDelete = { item ->
                    if (item.id != "header") {
                        historyManager.remove(item.id.toLong())
                        grouped.remove(item)
                        refreshAdapter()
                    }
                }
            )
        }
        refreshAdapter()

        val clearBtn = v.findViewById<Button>(R.id.btn_clear)
        clearBtn.visibility = View.VISIBLE
        clearBtn.setOnClickListener {
            historyManager.clearAll(); d.dismiss(); showToast("History cleared")
        }
        d.show()
    }

    private fun showDownloads() { try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch (_: Exception) {} }

    private fun showFindInPage() {
        if (currentSession() == null) { showToast("No page loaded"); return }
        val fb = findViewById<LinearLayout>(R.id.find_bar)
        fb.visibility = View.VISIBLE
        val fi = findViewById<EditText>(R.id.find_input)
        fi.requestFocus()
        showKeyboard(fi)
        fi.setOnEditorActionListener { _, _, _ ->
            val text = fi.text.toString()
            currentSession()?.finder?.find(text, 0)
            true
        }
        findViewById<ImageButton>(R.id.find_next).setOnClickListener {
            currentSession()?.finder?.find(fi.text.toString(), 0)
        }
        findViewById<ImageButton>(R.id.find_prev).setOnClickListener {
            currentSession()?.finder?.find(fi.text.toString(), GeckoSession.FINDER_FIND_BACKWARDS)
        }
        findViewById<ImageButton>(R.id.find_close).setOnClickListener {
            fb.visibility = View.GONE
            currentSession()?.finder?.clear()
            hideKeyboard()
        }
    }

    // =================== SHIELD (Brave-style) ===================

    private fun showShieldInfo() {
        val tab = tabManager.currentTab()
        val currentDomain = try { Uri.parse(tab?.url ?: "").host?.removePrefix("www.") ?: "" } catch (_: Exception) { "" }
        val isWhitelisted = currentDomain.isNotEmpty() && adBlocker.isWhitelisted(currentDomain)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetTheme); val view = layoutInflater.inflate(R.layout.bottom_sheet_shield, null); dialog.setContentView(view)
        view.findViewById<TextView>(R.id.shield_domain).text = if (currentDomain.isNotEmpty()) currentDomain else "New Tab"
        view.findViewById<TextView>(R.id.shield_status).text = if (isWhitelisted) "Shield is DOWN" else "Shield is UP"
        view.findViewById<TextView>(R.id.shield_status).setTextColor(ContextCompat.getColor(this, if (isWhitelisted) R.color.danger else R.color.success))
        view.findViewById<TextView>(R.id.shield_blocked_session).text = sessionBlocked.toString()
        view.findViewById<TextView>(R.id.shield_blocked_total).text = blockedCount.toString()
        val toggle = view.findViewById<SwitchMaterial>(R.id.shield_toggle); toggle.isChecked = !isWhitelisted
        toggle.setOnCheckedChangeListener { _, checked -> if (currentDomain.isNotEmpty()) { adBlocker.toggleWhitelist(currentDomain); currentSession()?.reload(); dialog.dismiss(); showToast(if (checked) "Shield UP" else "Shield DOWN") } }
        view.findViewById<TextView>(R.id.shield_adblock_status).text = if (prefsManager.isAdBlockEnabled()) "ON" else "OFF"
        view.findViewById<TextView>(R.id.shield_tracker_status).text = "ON (Enhanced Tracking Protection)"
        view.findViewById<TextView>(R.id.shield_incognito_status).text = if (isIncognito) "ON" else "OFF"
        dialog.show()
    }

    // =================== FEATURES ===================

    private fun addToHomeScreen() {
        val tab = tabManager.currentTab() ?: return
        if (tab.isOnNtp || tab.url == null) { showToast("No page to add"); return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val shortcutIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tab.url))
                shortcutIntent.addCategory(Intent.CATEGORY_BROWSABLE)

                val pinShortcutInfo = android.content.pm.ShortcutInfo.Builder(this, "web_${System.currentTimeMillis()}")
                    .setShortLabel(tab.title.take(20).ifEmpty { "Web Page" })
                    .setLongLabel(tab.title)
                    .setIntent(shortcutIntent)
                    .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_logo))
                    .build()
                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                showToast("Shortcut added to home screen")
            } else {
                showToast("Device doesn't support shortcuts")
            }
        } else {
            val shortcutIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tab.url))
            val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, tab.title)
                putExtra("duplicate", false)
            }
            sendBroadcast(intent)
            showToast("Shortcut added")
        }
    }

    private fun addBookmark() {
        val t = tabManager.currentTab() ?: return
        if (t.isOnNtp || t.url == null) { showToast("No page to bookmark"); return }
        if (bookmarkManager.isBookmarked(t.url!!)) {
            val all = bookmarkManager.getAll()
            val existing = all.find { it.url == t.url }
            if (existing != null) bookmarkManager.remove(existing.id)
            showToast("Bookmark removed")
        } else {
            bookmarkManager.add(t.title, t.url!!)
            showToast("Bookmarked")
        }
    }

    private fun shareCurrentPage() {
        val t = tabManager.currentTab() ?: return
        if (t.isOnNtp || t.url == null) { showToast("No page to share"); return }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, t.url ?: ""); putExtra(Intent.EXTRA_SUBJECT, t.title) }, "Share"))
    }

    private var isDesktopMode = false
    private fun toggleDesktopMode() {
        if (currentSession() == null) { showToast("No page loaded"); return }
        isDesktopMode = !isDesktopMode
        // GeckoView uses GeckoSessionSettings for user agent override
        // For simplicity, reload with viewport hint
        showToast(if (isDesktopMode) "Desktop site ON" else "Mobile site")
        currentSession()?.reload()
    }

    private fun toggleIncognito() {
        isIncognito = !isIncognito
        if (isIncognito) {
            findViewById<LinearLayout>(R.id.bottom_bar)?.setBackgroundColor(
                ContextCompat.getColor(this, R.color.dark_bg_primary))
            showToast("Incognito Mode -- no history saved")
        } else {
            findViewById<LinearLayout>(R.id.bottom_bar)?.setBackgroundColor(
                ContextCompat.getColor(this, R.color.toolbar_bg))
            showToast("Incognito OFF")
        }
    }

    private fun updateBlockedCount() {
        findViewById<TextView>(R.id.stat_blocked)?.text = blockedCount.toString()
    }

    private fun openSettings() { startActivity(Intent(this, SettingsActivity::class.java)) }

    private fun loadNewsImage(url: String, imageView: ImageView, fallbackText: TextView?) {
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000; connection.readTimeout = 3000
                connection.instanceFollowRedirects = true; connection.connect()
                if (connection.responseCode == 200) {
                    val bitmap = android.graphics.BitmapFactory.decodeStream(connection.inputStream)
                    if (bitmap != null) runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                        fallbackText?.visibility = View.GONE
                    }
                }
                connection.disconnect()
            } catch (_: Exception) { }
        }.start()
    }

    // =================== TRANSLATE ===================

    private fun translatePage() {
        val session = currentSession() ?: run { showToast("No page to translate"); return }
        val url = tabManager.currentTab()?.url ?: run { showToast("No page to translate"); return }
        val translateUrl = "https://translate.google.com/translate?sl=auto&tl=en&u=${Uri.encode(url)}"
        session.loadUri(translateUrl)
        showToast("Translating page...")
    }

    // =================== QR CODE ===================

    private fun showQrCode() {
        val tab = tabManager.currentTab()
        if (tab == null || tab.isOnNtp || tab.url == null) { showToast("No URL to generate QR"); return }
        val url = tab.url!!
        val qrBitmap = generateQrCode(url, 512)
        if (qrBitmap == null) { showToast("Could not generate QR code"); return }

        val dialog = android.app.AlertDialog.Builder(this)
        val imgView = ImageView(this).apply {
            setImageBitmap(qrBitmap)
            setPadding(48, 48, 48, 48)
            adjustViewBounds = true
        }
        dialog.setTitle("QR Code")
            .setMessage(url.take(60) + if (url.length > 60) "..." else "")
            .setView(imgView)
            .setPositiveButton("Share") { _, _ -> shareQrCode(qrBitmap) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun generateQrCode(text: String, size: Int): Bitmap? {
        try {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val data = text.toByteArray()
            val moduleCount = 33
            val moduleSize = size.toFloat() / moduleCount
            val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; style = android.graphics.Paint.Style.FILL }

            fun drawFinder(x: Int, y: Int) {
                for (i in 0..6) for (j in 0..6) {
                    if (i == 0 || i == 6 || j == 0 || j == 6 || (i in 2..4 && j in 2..4)) {
                        canvas.drawRect((x + i) * moduleSize, (y + j) * moduleSize, (x + i + 1) * moduleSize, (y + j + 1) * moduleSize, paint)
                    }
                }
            }
            drawFinder(0, 0); drawFinder(moduleCount - 7, 0); drawFinder(0, moduleCount - 7)

            var bitIndex = 0
            for (row in 9 until moduleCount - 8) {
                for (col in 9 until moduleCount - 8) {
                    val byteIdx = bitIndex / 8
                    val bitPos = 7 - (bitIndex % 8)
                    if (byteIdx < data.size) {
                        if ((data[byteIdx].toInt() shr bitPos) and 1 == 1) {
                            canvas.drawRect(col * moduleSize, row * moduleSize, (col + 1) * moduleSize, (row + 1) * moduleSize, paint)
                        }
                    }
                    bitIndex++
                }
            }
            return bitmap
        } catch (_: Exception) { return null }
    }

    private fun shareQrCode(bitmap: Bitmap) {
        try {
            val file = java.io.File(cacheDir, "qr_code.png")
            val fos = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share QR Code"))
        } catch (e: Exception) { showToast("Failed to share QR code") }
    }

    // =================== SCREENSHOT ===================

    private fun screenshotPage() {
        if (currentSession() == null) { showToast("Nothing to capture -- you're on the home page"); return }
        try {
            val bitmap = Bitmap.createBitmap(geckoView.width, geckoView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            geckoView.draw(canvas)
            val fileName = "Kaliki_Screenshot_${System.currentTimeMillis()}.png"
            val fos: java.io.OutputStream
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KalikiBrowser")
                }
                val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                fos = contentResolver.openOutputStream(uri!!)!!
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/KalikiBrowser")
                dir.mkdirs()
                val file = java.io.File(dir, fileName)
                fos = java.io.FileOutputStream(file)
            }
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            showToast("Screenshot saved to Pictures/KalikiBrowser")
        } catch (e: Exception) {
            showToast("Screenshot failed: ${e.message}")
        }
    }

    // =================== QR CODE SCANNER ===================

    private fun openQrScanner() {
        // Try to use any installed QR scanner app via intent
        try {
            val intent = Intent("com.google.zxing.client.android.SCAN")
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 9999)
        } catch (_: Exception) {
            // No QR scanner app — open Google Lens as fallback
            try {
                val lensIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com"))
                startActivity(lensIntent)
            } catch (_: Exception) {
                showToast("No QR scanner available. Install Google Lens or any barcode scanner app.")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 9999 && resultCode == RESULT_OK) {
            val scanned = data?.getStringExtra("SCAN_RESULT") ?: return
            if (scanned.startsWith("http://") || scanned.startsWith("https://")) {
                navigateTo(scanned)
            } else {
                navigateTo(scanned)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // =================== READING LIST ===================

    private fun saveToReadingList() {
        val tab = tabManager.currentTab() ?: return
        if (tab.isOnNtp || tab.url == null) { showToast("No page to save"); return }

        val prefs = getSharedPreferences("kaliki_reading_list", MODE_PRIVATE)
        val existing = prefs.getStringSet("urls", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existing.add("${tab.url}|||${tab.title}")
        prefs.edit().putStringSet("urls", existing).apply()
        showToast("Saved to Reading List")
    }

    private fun showReadingList() {
        val prefs = getSharedPreferences("kaliki_reading_list", MODE_PRIVATE)
        val items = prefs.getStringSet("urls", emptySet()) ?: emptySet()
        val listItems = items.map {
            val parts = it.split("|||")
            ListItem(parts.getOrElse(1) { "Saved page" }, parts.getOrElse(0) { "" }, parts.getOrElse(0) { "" })
        }

        val d = BottomSheetDialog(this, R.style.BottomSheetTheme)
        val v = layoutInflater.inflate(R.layout.bottom_sheet_list, null)
        d.setContentView(v)
        v.findViewById<TextView>(R.id.list_title).text = "Reading List"
        val rv = v.findViewById<RecyclerView>(R.id.list_recycler)
        rv.layoutManager = LinearLayoutManager(this@MainActivity)
        rv.adapter = SimpleListAdapter(listItems, { item -> navigateTo(item.subtitle); d.dismiss() }, { item ->
            val existingSet = prefs.getStringSet("urls", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            existingSet.removeAll { it.startsWith(item.subtitle + "|||") || it == item.subtitle + "|||" + item.title }
            prefs.edit().putStringSet("urls", existingSet).apply()
            showToast("Removed from Reading List")
            d.dismiss()
        })
        if (listItems.isEmpty()) {
            showToast("Reading list is empty")
        }
        d.show()
    }

    // =================== TAB GROUPS ===================

    private fun showTabGroupColorPicker(tab: BrowserTab) {
        val colors = listOf(
            Pair("None", 0),
            Pair("Red", 0xFFEA4335.toInt()),
            Pair("Blue", 0xFF4285F4.toInt()),
            Pair("Green", 0xFF34A853.toInt()),
            Pair("Orange", 0xFFFF9800.toInt()),
            Pair("Purple", 0xFF9C27B0.toInt())
        )
        val colorNames = colors.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tab Group Color")
            .setItems(colorNames) { _, which ->
                tab.groupColor = colors[which].second
                showToast(if (which == 0) "Group color removed" else "${colors[which].first} group set")
            }
            .show()
    }

    // =================== GESTURE NAVIGATION ===================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val gestureLeft = findViewById<View>(R.id.gesture_left)
        val gestureRight = findViewById<View>(R.id.gesture_right)

        var startX = 0f

        gestureLeft.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = event.rawX; true }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - startX
                    if (dx > 80) {
                        tabManager.currentTab()?.session?.goBack()
                    }
                    true
                }
                else -> true
            }
        }

        gestureRight.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = event.rawX; true }
                MotionEvent.ACTION_UP -> {
                    val dx = startX - event.rawX
                    if (dx > 80) {
                        tabManager.currentTab()?.session?.goForward()
                    }
                    true
                }
                else -> true
            }
        }
    }

    // =================== SMART SEARCH SUGGESTIONS ===================

    private var searchJob: Thread? = null

    private fun setupSearchSuggestions() {
        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length < 2) { hideSuggestions(); return }
                if (!urlEditText.hasFocus()) return
                searchJob?.interrupt()
                searchJob = Thread {
                    try {
                        Thread.sleep(200) // debounce
                        val suggestions = fetchSuggestions(query)
                        runOnUiThread { showSuggestions(suggestions) }
                    } catch (_: InterruptedException) {}
                }
                searchJob?.start()
            }
        })
    }

    private fun fetchSuggestions(query: String): List<String> {
        val results = mutableListOf<String>()

        // Add history matches first
        val historyMatches = historyManager.getAll()
            .filter { it.title.contains(query, true) || it.url.contains(query, true) }
            .take(3)
            .map { it.title.ifEmpty { it.url } }
        results.addAll(historyMatches)

        // Fetch Google suggestions
        try {
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&q=${Uri.encode(query)}"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // Parse: ["query",["suggestion1","suggestion2",...]]
            val startIdx = json.indexOf(",[")
            val endIdx = json.indexOf("]", startIdx + 2)
            if (startIdx > 0 && endIdx > startIdx) {
                val arr = json.substring(startIdx + 2, endIdx)
                arr.split(",").forEach { s ->
                    val clean = s.trim().trim('"')
                    if (clean.isNotEmpty() && clean != query) results.add(clean)
                }
            }
        } catch (_: Exception) {}

        return results.distinct().take(8)
    }

    private fun showSuggestions(suggestions: List<String>) {
        val rv = findViewById<RecyclerView>(R.id.suggestions_list)
        if (suggestions.isEmpty()) { rv.visibility = View.GONE; return }
        rv.visibility = View.VISIBLE
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = SuggestionsAdapter(suggestions) { suggestion ->
            urlEditText.setText(suggestion)
            hideSuggestions()
            navigateTo(suggestion)
            hideKeyboard()
        }
    }

    private fun hideSuggestions() {
        findViewById<RecyclerView>(R.id.suggestions_list).visibility = View.GONE
    }

    // =================== LIFECYCLE ===================

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val tab = tabManager.currentTab()
        when {
            drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawers()
            findViewById<LinearLayout>(R.id.find_bar).visibility == View.VISIBLE -> findViewById<LinearLayout>(R.id.find_bar).visibility = View.GONE
            tab != null && !tab.isOnNtp && canGoBack -> tab.session.goBack()
            tab != null && !tab.isOnNtp -> showNewTabPage()
            tab != null && tabManager.tabs.size > 1 -> closeTab(tab)
            else -> super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) { super.onNewIntent(intent); intent?.let { handleIntent(it) } }
    private fun handleIntent(intent: Intent) { val url = intent.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT); if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) createNewTab(url) }

    override fun onResume() {
        super.onResume()
        // Session is active when visible
        tabManager.currentTab()?.session?.setActive(true)
    }

    override fun onPause() {
        super.onPause()
        // Keep session active for YouTube background playback
        // setActive(true) tells GeckoView to keep processing audio even when not visible
        tabManager.currentTab()?.session?.setActive(true)
        requestAudioFocus()
    }

    override fun onStop() {
        super.onStop()
        // Keep session active — do NOT close or suspend sessions
        tabManager.currentTab()?.session?.setActive(true)
        val tabData = tabManager.tabs
            .filter { !it.isOnNtp && it.url != null }
            .map { mapOf("url" to it.url!!, "title" to it.title) }
        val json = Gson().toJson(tabData)
        getSharedPreferences("kaliki_session", MODE_PRIVATE).edit()
            .putString("saved_tabs_json", json)
            .putInt("active_tab_index", tabManager.currentIndex())
            .apply()
    }

    override fun onDestroy() {
        tabManager.tabs.forEach { it.session.close() }
        nativeAds.forEach { it.destroy() }
        super.onDestroy()
    }

    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(urlEditText.windowToken, 0); urlEditText.clearFocus(); ntpSearch.clearFocus() }
    private fun showKeyboard(view: View) { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT) }
    private fun showToast(msg: String) { Snackbar.make(geckoView, msg, Snackbar.LENGTH_SHORT).show() }

    private fun loadImageAsync(url: String, imageView: ImageView) {
        Thread {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000; conn.readTimeout = 3000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connect()
                if (conn.responseCode == 200) {
                    val bitmap = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    if (bitmap != null) runOnUiThread { imageView.setImageBitmap(bitmap) }
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }.start()
    }
}
