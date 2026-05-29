# Kaliki Browser

A fast, private, and secure Android browser built on **Mozilla GeckoView** (Firefox engine).

## Features

| Feature | Description |
|---------|-------------|
| Ad & Tracker Blocking | Firefox Enhanced Tracking Protection (STRICT mode) |
| YouTube Ad-Free | Ads blocked at engine level |
| YouTube Background Play | Audio continues when app is minimized or phone locked |
| Multi-Tab Browsing | Tab screenshots, restore on relaunch |
| Incognito Mode | No history, cookies cleared on exit |
| Dark Mode | Always-on dark theme |
| Discover Feed | News feed with native ads on home page |
| Bookmarks & History | Full management with search |
| Downloads | Built-in download manager |
| Desktop Mode | Switch to desktop user agent |
| Find in Page | Search text within page |
| Reader Mode | Distraction-free reading |
| Share & Print | Share URLs, print pages |
| Add to Home Screen | Create shortcuts |
| QR Code Generator | Generate QR for current URL |
| Screenshot | Capture current page |
| Translate | Translate page via Google |
| Long Press Menu | Save images, copy URLs, share |
| AdMob Integration | Native ads in Discover feed |

## Tech Stack

| Component | Technology |
|-----------|------------|
| Engine | Mozilla GeckoView (Firefox) |
| Language | Kotlin |
| UI | Material Design 3 |
| Ad Blocking | Firefox Enhanced Tracking Protection |
| Monetization | Google AdMob (Native Advanced) |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 14 (API 34) |

## Build

```bash
# Prerequisites
# - Android SDK (API 34)
# - JDK 17
# - Gradle 8.5

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing key)
./gradlew assembleRelease

# APK output
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release.apk
```

## Project Structure

```
app/src/main/
├── java/com/kaliki/browser/
│   ├── activities/
│   │   ├── MainActivity.kt      # Main browser activity
│   │   └── SettingsActivity.kt   # Settings screen
│   ├── adapters/
│   │   ├── TabAdapter.kt         # Tab switcher adapter
│   │   ├── SimpleListAdapter.kt  # Bookmarks/history list
│   │   └── NewsFeedAdapter.kt    # Discover feed
│   ├── models/
│   │   ├── Models.kt             # BrowserTab, Bookmark, History
│   │   └── NewsItem.kt           # News feed item
│   ├── services/
│   │   └── DownloadService.kt    # Download service
│   └── utils/
│       ├── AdBlocker.kt          # Ad blocking logic
│       ├── BookmarkManager.kt    # Bookmark storage
│       ├── DownloadHelper.kt     # Download manager
│       ├── HistoryManager.kt     # History storage
│       ├── PrefsManager.kt       # Preferences
│       └── TabManager.kt         # Tab state management
├── res/
│   ├── layout/                   # All XML layouts
│   ├── drawable/                 # Vector icons & shapes
│   ├── values/                   # Colors, strings, themes
│   └── values-night/             # Dark mode colors
└── AndroidManifest.xml
```

## AdMob Configuration

- **App ID:** `ca-app-pub-6027286420304821~3031968964`
- **Native Ad Unit:** `ca-app-pub-6027286420304821/1561454716`

To use your own AdMob account, update these in:
- `AndroidManifest.xml` (App ID)
- `MainActivity.kt` (Ad Unit ID in `loadNativeAds()`)

## Download APK

Check [Releases](https://github.com/jayakrishnaa18/Kalikibrowser/releases) for the latest APK.

## License

MIT
