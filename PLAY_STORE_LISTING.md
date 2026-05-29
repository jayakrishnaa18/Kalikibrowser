# Kaliki Browser - Google Play Store Listing

## App Title
Kaliki Browser - Fast & Private

## Short Description (80 chars)
Fast, private browser with built-in ad blocker. Block ads & trackers for free.

## Full Description (4000 chars max)
Kaliki Browser is a fast, lightweight, and privacy-focused web browser that puts you in control. Built with powerful ad blocking and tracker protection to give you a cleaner, faster, and more secure browsing experience.

🛡️ BUILT-IN AD & TRACKER BLOCKER
• Blocks 60+ ad networks automatically
• Stops 45+ trackers from following you
• Faster page loads with no ads
• Saves mobile data by blocking unwanted content

🔒 PRIVACY FIRST
• Incognito mode - no history saved
• Block third-party cookies
• Do Not Track (DNT) signal
• No data collection - your data stays on YOUR device

⚡ FAST & LIGHTWEIGHT
• Optimized for speed and low memory
• Swipe to refresh
• Smooth scrolling
• Hardware accelerated rendering

📱 FULL FEATURED BROWSER
• Multiple tabs with easy switching
• Bookmarks & history management
• Built-in download manager
• Find in page
• Desktop mode toggle
• Share pages easily
• Print web pages

🌙 DARK MODE
• Force dark mode on any website
• Eye-friendly dark theme
• Reduces eye strain at night

📖 READER MODE
• Distraction-free reading
• Clean, formatted content
• Perfect for articles and blog posts

🔍 MULTIPLE SEARCH ENGINES
• Google
• DuckDuckGo
• Bing
• Brave Search
• Ecosia
• Yandex

💾 DATA SAVER
• Data saver mode for slow connections
• Block images to save data
• Lightweight browsing experience

Kaliki Browser - Browse freely, privately, and securely.

## Category
Communication > Web Browsers

## Tags
browser, ad blocker, private browser, fast browser, web browser, ad block, tracker blocker, privacy, incognito, dark mode

## Content Rating
Everyone

## Privacy Policy URL
[Required - you'll need to create one]

## Screenshots needed (minimum 2)
1. New tab page with shortcuts
2. Website with ad blocker active
3. Settings page
4. Tab switcher
5. Menu showing features
6. Reader mode
7. Dark mode on website
8. Incognito mode

## Feature Graphic (1024x500)
- Kaliki Browser logo
- Tagline: "Fast. Private. Secure."
- Dark gradient background

## Release Notes (v1.0.0)
• Initial release
• Built-in ad & tracker blocker (60+ networks)
• Incognito mode
• Multi-tab browsing
• Bookmarks & history
• Download manager
• Reader mode
• Force dark mode
• Desktop mode
• Find in page
• Multiple search engines
• Data saver mode
• Swipe to refresh

---

## Steps to Publish on Google Play Store

### 1. Google Play Developer Account
- Sign up at https://play.google.com/console
- One-time fee: $25

### 2. Generate Signed APK/AAB
```bash
cd kaliki-browser-android
# Generate keystore (keep this safe - you can never replace it!)
keytool -genkey -v -keystore kaliki-release.keystore -alias kaliki -keyalg RSA -keysize 2048 -validity 10000

# Build release AAB (preferred for Play Store)
./gradlew bundleRelease

# Or build APK
./gradlew assembleRelease
```

### 3. Create Privacy Policy
- Required for all browsers
- Host on a public URL (GitHub Pages works)
- Must describe data handling practices

### 4. Upload to Play Console
1. Create new app
2. Fill in store listing
3. Upload screenshots
4. Set content rating
5. Upload AAB/APK
6. Set pricing (Free)
7. Submit for review

### 5. Review Process
- Usually takes 1-3 days
- First submission may take longer
- Browser apps may require additional review
