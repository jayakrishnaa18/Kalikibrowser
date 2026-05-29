(function() {
    'use strict';
    if (document.getElementById('kaliki-ext-adblock')) return;
    var s = document.createElement('style');
    s.id = 'kaliki-ext-adblock';
    s.textContent = [
        '[id*="google_ads"], ins.adsbygoogle, [data-ad], [data-ad-slot],',
        'iframe[src*="doubleclick"], iframe[src*="googlesyndication"],',
        '[class*="ad-container"], [class*="ad-wrapper"], [class*="ad-banner"],',
        '[id*="div-gpt-ad"], [aria-label="Advertisement"],',
        '[class*="outbrain"], [class*="taboola"], [id*="taboola"],',
        '[class*="mgid"], [id*="mgid"], [id*="sponsor"], [class*="sponsor"],',
        '[data-google-query-id], [class*="promoted-"]',
        '{ display:none!important; height:0!important; min-height:0!important; }'
    ].join('\n');
    (document.head || document.documentElement).appendChild(s);
})();
