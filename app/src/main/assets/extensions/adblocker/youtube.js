(function() {
    'use strict';
    if (window.__kalikiYtExt) return;
    window.__kalikiYtExt = true;

    // Override visibility API for background playback
    Object.defineProperty(document, 'hidden', {get: () => false, configurable: true});
    Object.defineProperty(document, 'visibilityState', {get: () => 'visible', configurable: true});
    document.addEventListener('visibilitychange', function(e) {
        e.stopImmediatePropagation();
        e.stopPropagation();
    }, true);

    // Block visibilitychange from being added
    var origAdd = EventTarget.prototype.addEventListener;
    EventTarget.prototype.addEventListener = function(type, fn, opts) {
        if (type === 'visibilitychange') return;
        return origAdd.call(this, type, fn, opts);
    };

    // Ad skipper - runs every 300ms
    setInterval(function() {
        var player = document.querySelector('#movie_player, .html5-video-player');
        var video = document.querySelector('video');
        if (!player || !video) return;

        var adShowing = player.classList.contains('ad-showing') ||
                       player.classList.contains('ad-interrupting');

        if (adShowing) {
            // Try to click skip button first
            var skip = document.querySelector(
                '.ytp-skip-ad-button, .ytp-ad-skip-button-modern, ' +
                'button.ytp-ad-skip-button, [class*="skip-button"], ' +
                '.ytp-ad-skip-button-container button, .ytp-skip-ad-button__text'
            );
            if (skip) { skip.click(); return; }

            // Jump to end of ad (no playbackRate change — avoids blank screen)
            if (video.readyState >= 2 && isFinite(video.duration) && video.duration > 0.5) {
                video.currentTime = video.duration - 0.01;
            }
            // Mute ad audio
            video.muted = true;
        } else {
            // Unmute for real content
            if (video.muted) video.muted = false;
        }

        // Remove overlay ads and promoted content
        var adSelectors = [
            '.ytp-ad-overlay-container',
            '.ytp-ad-module',
            '.ytp-ad-image-overlay',
            '.ytp-ad-text-overlay',
            '.ytp-ad-message-container',
            '#player-ads',
            '#masthead-ad',
            'ytd-promoted-sparkles-web-renderer',
            'ytd-display-ad-renderer',
            'ytd-ad-slot-renderer',
            'ytd-in-feed-ad-layout-renderer',
            'ytd-banner-promo-renderer',
            'ytd-companion-slot-renderer',
            'ytd-action-companion-ad-renderer',
            '.ytd-mealbar-promo-renderer',
            'ytd-statement-banner-renderer',
            'ytd-promoted-video-renderer',
            '.ytd-rich-item-renderer:has(ytd-ad-slot-renderer)'
        ];
        document.querySelectorAll(adSelectors.join(',')).forEach(function(el) { el.remove(); });
    }, 300);

    // MutationObserver for instant ad detection
    var observer = new MutationObserver(function(mutations) {
        for (var m of mutations) {
            if (m.type === 'attributes' && m.attributeName === 'class') {
                var target = m.target;
                if (target.classList && target.classList.contains('ad-showing')) {
                    var video = document.querySelector('video');
                    var skip = document.querySelector('.ytp-skip-ad-button, .ytp-ad-skip-button-modern');
                    if (skip) { skip.click(); return; }
                    if (video && video.readyState >= 2 && isFinite(video.duration) && video.duration > 0.5) {
                        video.currentTime = video.duration - 0.01;
                        video.muted = true;
                    }
                }
            }
        }
    });

    // Wait for player to appear then observe
    function observePlayer() {
        var player = document.querySelector('#movie_player');
        if (player) {
            observer.observe(player, { attributes: true, attributeFilter: ['class'] });
        } else {
            setTimeout(observePlayer, 500);
        }
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', observePlayer);
    } else {
        observePlayer();
    }

    // CSS injection to hide ad elements
    var style = document.createElement('style');
    style.textContent = [
        '.ytp-ad-overlay-container, .ytp-ad-module,',
        '.ytp-ad-image-overlay, .ytp-ad-text-overlay,',
        '.ytp-ad-message-container, #player-ads, #masthead-ad,',
        'ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer,',
        'ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer,',
        'ytd-banner-promo-renderer, ytd-companion-slot-renderer,',
        'ytd-action-companion-ad-renderer, .ytd-mealbar-promo-renderer,',
        'ytd-statement-banner-renderer, ytd-promoted-video-renderer',
        '{ display:none!important; height:0!important; }'
    ].join('\n');
    (document.head || document.documentElement).appendChild(style);
})();
