package com.kaliki.browser.utils

import android.content.Context
import android.content.SharedPreferences

class AdBlocker(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kaliki_adblock", Context.MODE_PRIVATE)
    private val statsPrefs: SharedPreferences = context.getSharedPreferences("kaliki_stats", Context.MODE_PRIVATE)

    fun getBlockedTotal(): Int = statsPrefs.getInt("total_blocked", 0)
    fun incrementBlocked() { statsPrefs.edit().putInt("total_blocked", getBlockedTotal() + 1).apply() }

    fun isWhitelisted(domain: String): Boolean = prefs.getBoolean("whitelist_$domain", false)
    fun toggleWhitelist(domain: String): Boolean {
        val newState = !isWhitelisted(domain)
        prefs.edit().putBoolean("whitelist_$domain", newState).apply()
        return newState
    }

    private val blockedDomains = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "pagead2.googlesyndication.com", "adservice.google.com",
        "googleads.g.doubleclick.net", "static.doubleclick.net",
        "ad.doubleclick.net", "ads.google.com", "www.googletagservices.com",
        "google-analytics.com", "ssl.google-analytics.com",
        "analytics.google.com", "an.facebook.com", "pixel.facebook.com",
        "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net",
        "rubiconproject.com", "pubmatic.com", "openx.net",
        "casalemedia.com", "bidswitch.net", "smartadserver.com",
        "sharethrough.com", "triplelift.com", "sovrn.com",
        "admob.com", "applovin.com", "inmobi.com",
        "vungle.com", "smaato.com", "chartboost.com",
        "popads.net", "popcash.net", "propellerads.com",
        "adcash.com", "revcontent.com", "mgid.com",
        "outbrain.com", "taboola.com",
        "scorecardresearch.com", "quantserve.com", "hotjar.com",
        "mixpanel.com", "segment.io", "amplitude.com",
        "fullstory.com", "clarity.ms", "heapanalytics.com",
        "appsflyer.com", "adjust.com", "branch.io",
        "tealium.com", "demdex.net", "krxd.net",
        "moatads.com", "doubleverify.com",
        "s0.2mdn.net", "vid.springserve.com", "teads.tv",
        "amazon-adsystem.com", "ads-twitter.com",
        "bat.bing.com", "pixel.wp.com", "stats.wp.com",
        "advertising.com", "adform.net", "serving-sys.com",
        "nr-data.net", "newrelic.com", "optimizely.com"
    )

    fun shouldBlock(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // NEVER block anything YouTube/Google video related
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") ||
            lowerUrl.contains("googlevideo.com") || lowerUrl.contains("ytimg.com") ||
            lowerUrl.contains("yt3.ggpht.com") || lowerUrl.contains("youtubei.googleapis.com") ||
            lowerUrl.contains("jnn-pa.googleapis.com") || lowerUrl.contains("play.google.com") ||
            lowerUrl.contains("imasdk.googleapis.com") || lowerUrl.contains("youtube-nocookie.com") ||
            lowerUrl.contains("gstatic.com") || lowerUrl.contains("googleapis.com/youtubei")) {
            return false
        }

        for (domain in blockedDomains) {
            if (lowerUrl.contains(domain)) return true
        }
        return false
    }

    // YouTube background playback — trick YouTube into thinking page is visible
    fun getYouTubeBackgroundScript(): String = """
        (function() {
            'use strict';
            if (window.__kalikiBg) return;
            window.__kalikiBg = true;

            // Override document.hidden — always return false
            Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });

            // Block visibilitychange event from firing
            var origAdd = EventTarget.prototype.addEventListener;
            EventTarget.prototype.addEventListener = function(type, fn, opts) {
                if (type === 'visibilitychange') return;
                return origAdd.call(this, type, fn, opts);
            };

            // Also block any existing listeners from receiving the event
            document.addEventListener('visibilitychange', function(e) {
                e.stopImmediatePropagation();
            }, true);
        })();
    """.trimIndent()

    // YouTube ad bypass - fast-forward approach (like Brave)
    // Wait for ad to START playing, then speed through it
    fun getYouTubeAdBlockScript(): String = """
        (function() {
            'use strict';
            if (window.__kalikiYt) return;
            window.__kalikiYt = true;

            // CSS: Hide overlay ads and promoted content immediately
            var s = document.createElement('style');
            s.textContent = `
                .ytp-ad-overlay-container, .ytp-ad-module,
                .ytp-ad-image-overlay, .ytp-ad-text-overlay,
                .ytp-ad-message-container, #player-ads, #masthead-ad,
                ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer,
                ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer,
                ytd-banner-promo-renderer, ytd-companion-slot-renderer,
                ytd-action-companion-ad-renderer, .ytd-mealbar-promo-renderer,
                ytd-statement-banner-renderer
                { display:none!important; height:0!important; }
            `;
            document.head.appendChild(s);

            function bypassAd() {
                var video = document.querySelector('video');
                var player = document.querySelector('#movie_player, .html5-video-player');
                if (!player || !video) return;

                var adShowing = player.classList.contains('ad-showing') ||
                               player.classList.contains('ad-interrupting');

                if (adShowing) {
                    // Step 1: Click skip button if available
                    var skip = document.querySelector(
                        '.ytp-skip-ad-button, .ytp-ad-skip-button-modern, ' +
                        'button.ytp-ad-skip-button, [class*="skip-button"]'
                    );
                    if (skip) { skip.click(); return; }

                    // Step 2: Only fast-forward AFTER video has started playing
                    // readyState >= 3 means enough data to play
                    // currentTime > 0.1 means video actually started
                    if (video.readyState >= 3 && video.currentTime > 0.1 && isFinite(video.duration)) {
                        video.currentTime = video.duration - 0.01;
                        video.playbackRate = 16;
                    }
                } else {
                    // Real content playing — reset speed
                    if (video.playbackRate !== 1) video.playbackRate = 1;
                }

                // Remove promoted feed items
                document.querySelectorAll(
                    'ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, ' +
                    'ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, #masthead-ad'
                ).forEach(function(el) { el.remove(); });
            }

            // Check every 300ms
            setInterval(bypassAd, 300);

            // Also use MutationObserver for instant detection
            var target = document.querySelector('#movie_player');
            if (target) {
                new MutationObserver(bypassAd).observe(target, { attributes: true, attributeFilter: ['class'] });
            }
        })();
    """.trimIndent()

    fun getGeneralAdBlockScript(): String = """
        (function() {
            'use strict';
            if (document.getElementById('kaliki-adblock')) return;
            var s = document.createElement('style'); s.id = 'kaliki-adblock';
            s.textContent = `
                [id*="google_ads"], [id*="ad-slot"], [class*="ad-container"],
                [class*="ad-wrapper"], [class*="ad-banner"], ins.adsbygoogle,
                [data-ad], [data-ad-slot], [data-google-query-id],
                [id*="div-gpt-ad"], iframe[src*="doubleclick"],
                iframe[src*="googlesyndication"], iframe[src*="ad"],
                [id*="sponsor"], [class*="sponsor"], [class*="promoted-"],
                [aria-label="Ads"], [aria-label="Advertisement"],
                [class*="outbrain"], [class*="taboola"], [id*="taboola"],
                [class*="mgid"], [id*="mgid"]
                { display:none!important; height:0!important; min-height:0!important; }
            `;
            document.head.appendChild(s);
        })();
    """.trimIndent()
}
