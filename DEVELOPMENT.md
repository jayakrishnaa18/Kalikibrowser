# Development Guide — Kaliki Browser

## How to Update This Browser

### Prerequisites
- Android Studio (or command line with Android SDK)
- JDK 17
- Gradle 8.5+

### Setup
```bash
git clone https://github.com/jayakrishnaa18/Kalikibrowser.git
cd Kalikibrowser

# Set local SDK path
echo "sdk.dir=/path/to/your/android-sdk" > local.properties

# Build
./gradlew assembleDebug
```

---

## Common Updates

### 1. Update GeckoView Engine
Edit `app/build.gradle.kts`:
```kotlin
// Check latest version at: https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-arm64-v8a/maven-metadata.xml
implementation("org.mozilla.geckoview:geckoview-arm64-v8a:NEW_VERSION_HERE")
```

### 2. Add New Home Page Shortcuts
Edit `MainActivity.kt` → `setupShortcuts()`:
```kotlin
Shortcut("SiteName", "https://example.com", "E", "https://www.google.com/s2/favicons?domain=example.com&sz=128")
```

### 3. Add New News Feed Items
Edit `MainActivity.kt` → `setupNewsFeed()`:
```kotlin
NewsItem("Title", "Source • Time", "https://url.com", "Category", 0xFF4285F4.toInt(), favicon("domain.com"))
```

### 4. Change Search Engine Default
Edit `PrefsManager.kt`:
```kotlin
fun getSearchEngine(): String = prefs.getString("search_engine", "https://www.google.com/search?q=")!!
```

### 5. Update AdMob IDs
- App ID: `AndroidManifest.xml` → `com.google.android.gms.ads.APPLICATION_ID`
- Ad Unit: `MainActivity.kt` → search for `ca-app-pub-` in `loadNativeAds()`

### 6. Add New Menu Items
1. Add layout in `res/layout/bottom_sheet_menu.xml`
2. Add click handler in `MainActivity.kt` → `showMainMenu()`
3. Add the feature function

### 7. Change App Colors/Theme
- Light colors: `res/values/colors.xml`
- Dark colors: `res/values-night/colors.xml`
- Theme: `res/values/themes.xml`

### 8. Update App Version for Play Store
Edit `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2          // Increment for each release
    versionName = "1.1.0"    // User-visible version
}
```

---

## Building for Play Store

### Generate Signing Key (one time only)
```bash
keytool -genkey -v -keystore kaliki-release.keystore \
    -alias kaliki -keyalg RSA -keysize 2048 -validity 10000
```
**KEEP THIS FILE SAFE — you can never replace it.**

### Configure Signing in build.gradle.kts
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("kaliki-release.keystore")
            storePassword = "YOUR_PASSWORD"
            keyAlias = "kaliki"
            keyPassword = "YOUR_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Build Release AAB (for Play Store)
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Build Release APK (for direct install)
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Architecture

### GeckoView Basics
```
GeckoRuntime (singleton) — one per app
    └── GeckoSession (one per tab)
            ├── NavigationDelegate  → URL changes, back/forward
            ├── ProgressDelegate    → page load progress
            ├── ContentDelegate     → title, downloads, context menu
            └── ContentBlocking     → ad/tracker blocking
```

### Tab Lifecycle
```
Create tab → new GeckoSession → session.open(runtime)
Show tab   → geckoView.setSession(session)
Hide tab   → geckoView.releaseSession()
Close tab  → session.close()
```

### Content Blocking (Ad Blocker)
GeckoView uses Firefox's Enhanced Tracking Protection:
```kotlin
ContentBlocking.Settings.Builder()
    .antiTracking(ContentBlocking.AntiTracking.DEFAULT or AntiTracking.CRYPTOMINING)
    .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
```
This blocks: ads, trackers, cryptominers, fingerprinters — automatically.

---

## Adding uBlock Origin (Future)

GeckoView supports WebExtensions. To add uBlock Origin:

1. Download uBlock Origin XPI from Firefox Add-ons
2. Extract to `app/src/main/assets/extensions/ublock_origin/`
3. Load in MainActivity:
```kotlin
runtime.webExtensionController
    .ensureBuiltIn("resource://android/assets/extensions/ublock_origin/", "uBlock0@nickel-labs.org")
```

---

## Multi-Architecture APK (Reduce Size)

Current APK is ~174MB (arm64 only). For Play Store, use AAB which auto-splits:
```bash
./gradlew bundleRelease
# Google Play delivers only the correct architecture to each user (~90MB)
```

For multi-arch APK (supports all devices):
```kotlin
// build.gradle.kts
implementation("org.mozilla.geckoview:geckoview-arm64-v8a:VERSION")  // 64-bit ARM
implementation("org.mozilla.geckoview:geckoview-armeabi-v7a:VERSION") // 32-bit ARM
implementation("org.mozilla.geckoview:geckoview-x86_64:VERSION")     // x86 emulators
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Build fails "Could not find geckoview" | Check Mozilla Maven repo in settings.gradle.kts |
| APK too large | Use AAB for Play Store, or single architecture |
| Ads not showing | Wait 24h after creating AdMob unit |
| YouTube still shows ads | GeckoView ETP handles most, some may slip through |
| App crashes on start | Check GeckoRuntime is created only once |
