# Kaliki Browser — Keys, Config & Important Data

## AdMob IDs
- **App ID:** `ca-app-pub-6027286420304821~3031968964`
- **Native Ad Unit (Home Feed):** `ca-app-pub-6027286420304821/1561454716`

## Google Play Store
- **Package Name:** `com.kaliki.browser`
- **Version Code:** 8
- **Version Name:** 2.7.0
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Signing Key (Generate when publishing)
```bash
keytool -genkey -v -keystore kaliki-release.keystore \
    -alias kaliki -keyalg RSA -keysize 2048 -validity 10000
```
Store the .keystore file safely — you can NEVER replace it once published.

## GitHub Token (for CI/CD)
Update in GitHub Actions or local `.env` when needed.

## GeckoView Version
- **Current:** `org.mozilla.geckoview:geckoview-arm64-v8a:133.0.20241202233018`
- **Check latest:** https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-arm64-v8a/maven-metadata.xml

## Electron Version (Desktop)
- **Current:** `31.0.0`
- **Check latest:** https://github.com/electron/electron/releases

## Project Structure
```
Kalikibrowser/
├── main branch (Android)
│   ├── app/src/main/java/com/kaliki/browser/
│   │   ├── activities/MainActivity.kt (main browser)
│   │   ├── activities/SplashActivity.kt
│   │   ├── activities/InterestsActivity.kt
│   │   ├── activities/SettingsActivity.kt
│   │   ├── adapters/ (RecyclerView adapters)
│   │   ├── models/ (data classes)
│   │   └── utils/ (managers, helpers)
│   ├── app/src/main/res/ (layouts, drawables, values)
│   ├── app/src/main/assets/extensions/adblocker/ (WebExtension)
│   └── app/build.gradle.kts
│
└── desktop branch (Electron)
    ├── main.js (Electron main process)
    ├── preload.js (IPC bridge)
    ├── src/index.html (UI)
    ├── src/renderer.js (browser logic)
    ├── src/styles.css (dark theme)
    ├── adblock-filters.txt (113 rules)
    ├── extensions/ (Chrome extensions folder)
    └── package.json
```

## How to Resume Development
1. Clone: `git clone https://github.com/jayakrishnaa18/Kalikibrowser.git`
2. Android: `cd Kalikibrowser && ./gradlew assembleDebug`
3. Desktop: `git checkout desktop && npm install && npm start`

## Dependencies (Android)
- GeckoView (Mozilla Firefox engine)
- Google Material Design
- Google AdMob
- Gson (JSON)
- RecyclerView, SwipeRefreshLayout, Lifecycle

## Dependencies (Desktop)
- Electron 31
- electron-builder 24

## Privacy Policy URL (needed for Play Store)
Host PRIVACY_POLICY.md at a public URL (GitHub Pages works)
