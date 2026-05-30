# Kaliki Browser — Complete Installation & Development Guide

## Table of Contents
1. [Android Installation](#android)
2. [Desktop Installation (Windows/Linux/Mac)](#desktop)
3. [iOS (Future)](#ios)
4. [How to Fix Bugs](#fixing-bugs)
5. [How to Update the Browser](#updating)
6. [How to Add Features](#adding-features)
7. [Publishing to Stores](#publishing)
8. [Architecture Overview](#architecture)

---

## <a name="android"></a>1. Android Installation

### For Users (Install APK)
1. Download latest APK from [GitHub Releases](https://github.com/jayakrishnaa18/Kalikibrowser/releases)
2. Transfer to phone (or download directly on phone)
3. Enable "Install from Unknown Sources" (Settings → Security)
4. Tap APK → Install
5. Open "Kaliki Browser"

### For Developers (Build from Source)

**Prerequisites:**
- Android SDK (API 34)
- JDK 17
- Gradle 8.5+

```bash
git clone https://github.com/jayakrishnaa18/Kalikibrowser.git
cd Kalikibrowser

# Set SDK path
echo "sdk.dir=/path/to/your/Android/Sdk" > local.properties

# Build debug APK
./gradlew assembleDebug

# APK at: app/build/outputs/apk/debug/app-debug.apk
```

**For Play Store (Release APK):**
```bash
# Generate signing key (ONE TIME — keep this file safe forever)
keytool -genkey -v -keystore kaliki-release.keystore \
    -alias kaliki -keyalg RSA -keysize 2048 -validity 10000

# Add to app/build.gradle.kts:
# signingConfigs { create("release") { storeFile = file("kaliki-release.keystore") ... } }

# Build release
./gradlew assembleRelease
# or for Play Store bundle:
./gradlew bundleRelease
```

---

## <a name="desktop"></a>2. Desktop Installation

### For Users

#### Windows 10 / 11
1. Download `Kaliki-Browser-Setup-x.x.x.exe` from [Releases](https://github.com/jayakrishnaa18/Kalikibrowser/releases)
2. Double-click the `.exe`
3. If SmartScreen warning appears: click **"More info"** → **"Run anyway"**
4. Choose install location → click Install
5. Find in Start Menu → launch

**Uninstall:** Settings → Apps → Kaliki Browser → Uninstall

#### Linux (Ubuntu/Debian/Fedora)

**AppImage (works on all Linux — no install needed):**
```bash
wget https://github.com/jayakrishnaa18/Kalikibrowser/releases/download/vX.X/Kaliki-Browser.AppImage
chmod +x Kaliki-Browser.AppImage
./Kaliki-Browser.AppImage
```

**DEB package (Ubuntu/Debian):**
```bash
wget https://github.com/jayakrishnaa18/Kalikibrowser/releases/download/vX.X/kaliki-browser_amd64.deb
sudo dpkg -i kaliki-browser_amd64.deb
sudo apt-get install -f  # Fix dependencies if needed
```

**Run:** Search "Kaliki Browser" in app launcher or run `kaliki-browser`

#### macOS
1. Download `Kaliki-Browser.dmg`
2. Open DMG → Drag to Applications
3. First launch: Right-click → Open (to bypass Gatekeeper)

### For Developers (Build Desktop from Source)

```bash
git clone https://github.com/jayakrishnaa18/Kalikibrowser.git
cd Kalikibrowser
git checkout desktop

# Install Node.js dependencies
npm install

# Run in dev mode
npm start

# Build for current platform
npm run build-linux   # Linux: .AppImage + .deb
npm run build-win     # Windows: .exe installer
npm run build-mac     # macOS: .dmg
```

Build output → `dist/` folder

### Installing Chrome Extensions (Desktop)
1. Download extension source (unpacked folder with `manifest.json`)
2. Place folder inside `extensions/` directory in app
3. Restart browser
4. Extension loads automatically

---

## <a name="ios"></a>3. iOS (Future)

### Requirements
- Mac computer (macOS 13+)
- Xcode 15+
- Apple Developer Account ($99/year)
- iOS device or Simulator

### Build Steps (when iOS code is ready)
```bash
git clone https://github.com/jayakrishnaa18/Kalikibrowser.git
cd Kalikibrowser
git checkout ios

# Open in Xcode
open KalikiBrowser.xcodeproj

# Build: Product → Build (Cmd+B)
# Run: Product → Run (Cmd+R)
```

### Limitations on iOS
- Apple forces ALL browsers to use WebKit (WKWebView)
- Cannot use GeckoView or Chromium
- Ad blocking via Content Blocker rules (JSON format)
- Extension support limited compared to Android/Desktop

---

## <a name="fixing-bugs"></a>4. How to Fix Bugs

### Android Bugs

**Setup:**
```bash
cd Kalikibrowser  # (main branch)
```

**Key files:**
| File | What it controls |
|------|-----------------|
| `app/src/main/java/.../MainActivity.kt` | Main browser logic |
| `app/src/main/java/.../utils/AdBlocker.kt` | Ad blocking |
| `app/src/main/java/.../utils/TabManager.kt` | Tab state |
| `app/src/main/java/.../models/Models.kt` | Data models |
| `app/src/main/res/layout/activity_main.xml` | UI layout |
| `app/src/main/assets/extensions/adblocker/` | WebExtension |

**Common bug patterns and fixes:**

| Bug | Where to look | Common fix |
|-----|--------------|------------|
| Crash on tab switch | `switchToTab()` in MainActivity | Check `session.isOpen` before `setSession()` |
| URL bar shows wrong URL | `onLocationChange` delegate | Verify `tabManager.currentTab()` matches session |
| Page not loading | `navigateTo()` | Check session is attached to GeckoView |
| Ad blocker not working | `extensions/adblocker/manifest.json` | Verify `browser_specific_settings.gecko.id` |
| YouTube ads showing | `extensions/adblocker/youtube.js` | Update CSS selectors for new YouTube UI |
| Blank screen | `showGeckoSession()` | Ensure `geckoView.visibility = VISIBLE` |
| NTP showing old URL | `showNewTabPage()` | Clear `urlEditText` and `tab.url = null` |

**How to debug:**
```bash
# Build and install
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -i "kaliki"

# Filter by tag
adb logcat -s "Kaliki" "KalikiAds"
```

### Desktop Bugs

**Setup:**
```bash
cd Kalikibrowser
git checkout desktop
npm start  # Run with DevTools visible
```

**Key files:**
| File | What it controls |
|------|-----------------|
| `main.js` | Electron main process, windows, menus |
| `src/renderer.js` | Tab management, navigation, UI logic |
| `src/index.html` | Browser UI structure |
| `src/styles.css` | Styling |
| `preload.js` | API bridge (main ↔ renderer) |

**Debugging desktop:**
- Press `F12` to open DevTools
- Check Console for errors
- Network tab shows blocked requests
- `main.js` errors show in terminal where you ran `npm start`

---

## <a name="updating"></a>5. How to Update the Browser

### Update GeckoView (Android engine)
```bash
# Check latest version:
# https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-arm64-v8a/maven-metadata.xml

# Edit app/build.gradle.kts:
implementation("org.mozilla.geckoview:geckoview-arm64-v8a:NEW_VERSION")

# Rebuild
./gradlew assembleDebug
```

### Update Electron (Desktop engine)
```bash
# In kaliki-browser-desktop/
npm update electron
npm run build-linux  # or build-win
```

### Update Ad Block Rules
Edit `app/src/main/assets/extensions/adblocker/content.js` (Android)
or ad block list in `main.js` (Desktop)

### Bump Version for New Release
```bash
# Android: app/build.gradle.kts
versionCode = 9        # increment
versionName = "2.8.0"  # new version

# Desktop: package.json
"version": "2.8.0"
```

---

## <a name="adding-features"></a>6. How to Add Features

### Android — Add a new menu item
1. Edit `app/src/main/res/layout/bottom_sheet_menu.xml` — add LinearLayout
2. Edit `MainActivity.kt` → `showMainMenu()` — add click handler
3. Create the feature function

### Android — Add a new setting
1. Edit `app/src/main/res/layout/activity_settings.xml` — add switch
2. Edit `SettingsActivity.kt` — wire toggle
3. Edit `PrefsManager.kt` — add getter/setter

### Desktop — Add a keyboard shortcut
1. Edit `main.js` → add to `globalShortcut` or menu accelerator
2. Send IPC message to renderer
3. Handle in `renderer.js`

### Desktop — Add a new extension
1. Place unpacked extension folder in `extensions/`
2. It loads automatically via `session.loadExtension()`

---

## <a name="publishing"></a>7. Publishing to Stores

### Google Play Store
1. Create account: https://play.google.com/console ($25 one-time)
2. Generate signed AAB: `./gradlew bundleRelease`
3. Fill store listing (use PLAY_STORE_LISTING.md in repo)
4. Upload AAB + screenshots
5. Fill Data Safety form
6. Submit for review (1-7 days)

### Windows — Microsoft Store
1. Create dev account: https://partner.microsoft.com ($19 one-time)
2. Package as MSIX: Use `electron-builder --win msix`
3. Upload to Partner Center
4. Submit for review

### Linux — Snap Store
```bash
# Create snapcraft.yaml and build
snapcraft
snap install kaliki-browser_*.snap --dangerous
# Upload to snapcraft.io
snapcraft upload kaliki-browser_*.snap
```

### macOS — App Store
1. Apple Developer Account ($99/year)
2. Code sign with Apple certificate
3. Submit via Transporter app

---

## <a name="architecture"></a>8. Architecture Overview

### Android
```
Engine: Mozilla GeckoView (Firefox)
Language: Kotlin
UI: XML layouts + Material Design
Ad Blocking: WebExtension (content scripts) + Enhanced Tracking Protection
Tabs: GeckoSession per tab, one GeckoView
Storage: SharedPreferences + JSON files
Ads Revenue: Google AdMob (Native Advanced)
```

### Desktop
```
Engine: Electron (Chromium)
Language: JavaScript/HTML/CSS
UI: Custom HTML + CSS (dark theme)
Ad Blocking: session.webRequest.onBeforeRequest
Tabs: BrowserView per tab
Extensions: session.loadExtension() (Chrome extensions)
Storage: JSON files in app data folder
```

### iOS (planned)
```
Engine: WKWebView (WebKit — Apple required)
Language: Swift / SwiftUI
Ad Blocking: Content Blocker extension (JSON rules)
Tabs: WKWebView instances
```

---

## Quick Reference — Common Commands

```bash
# === ANDROID ===
./gradlew assembleDebug                    # Build debug APK
./gradlew assembleRelease                  # Build release APK
./gradlew bundleRelease                    # Build AAB for Play Store
adb install -r app/build/outputs/apk/debug/app-debug.apk  # Install on phone
adb logcat | grep Kaliki                   # View logs

# === DESKTOP ===
npm start                                  # Run dev mode
npm run build-win                          # Build Windows installer
npm run build-linux                        # Build Linux AppImage
npm run build-mac                          # Build macOS DMG

# === GIT ===
git checkout main                          # Android code
git checkout desktop                       # Desktop code
git tag v2.8.0 && git push --tags         # Create release tag
```

---

## Support

- **GitHub Issues:** https://github.com/jayakrishnaa18/Kalikibrowser/issues
- **Developed by:** Kaliki Labs
