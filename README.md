# Kaliki Browser — Desktop

A fast, private, and secure desktop browser built on Electron (Chromium engine) with built-in ad blocking and extension support.

## Supported Platforms

| Platform | Versions | Installer |
|----------|----------|-----------|
| **Windows** | Windows 10, 11 (64-bit) | `.exe` (NSIS installer) |
| **Linux** | Ubuntu 20.04+, Debian 11+, Fedora 36+ | `.AppImage` or `.deb` |
| **macOS** | macOS 11 (Big Sur) and later | `.dmg` |

## Installation

### Windows 10 / 11

1. Download `Kaliki-Browser-Setup-x.x.x.exe` from [Releases](https://github.com/jayakrishnaa18/Kalikibrowser/releases)
2. Double-click the `.exe` file
3. If Windows SmartScreen appears, click **"More info"** → **"Run anyway"** (this is because the app isn't code-signed yet)
4. Follow the installer — choose install location
5. Kaliki Browser will appear in Start Menu and Desktop

**To uninstall:** Settings → Apps → Kaliki Browser → Uninstall

### Linux (Ubuntu / Debian)

**Option A: AppImage (recommended — no install needed)**
```bash
# Download the .AppImage file
chmod +x Kaliki-Browser-x.x.x.AppImage
./Kaliki-Browser-x.x.x.AppImage
```

**Option B: .deb package (Debian/Ubuntu)**
```bash
sudo dpkg -i kaliki-browser_x.x.x_amd64.deb
# If dependencies missing:
sudo apt-get install -f
```

Then launch from Applications menu or run `kaliki-browser` in terminal.

### macOS

1. Download `Kaliki-Browser-x.x.x.dmg`
2. Open the `.dmg` file
3. Drag "Kaliki Browser" to the Applications folder
4. First launch: Right-click → Open (to bypass Gatekeeper)

## Building from Source

### Prerequisites
- Node.js 18+ (https://nodejs.org)
- npm or yarn
- Git

### Steps

```bash
# Clone
git clone https://github.com/jayakrishnaa18/Kalikibrowser.git
cd Kalikibrowser
git checkout desktop

# Install dependencies
npm install

# Run in development mode
npm start

# Build for your platform
npm run build-win     # Windows .exe installer
npm run build-linux   # Linux .AppImage + .deb
npm run build-mac     # macOS .dmg
```

Build output will be in the `dist/` folder.

## Features

| Feature | Shortcut |
|---------|----------|
| New Tab | `Ctrl+T` |
| Close Tab | `Ctrl+W` |
| Reopen Closed Tab | `Ctrl+Shift+T` |
| Next Tab | `Ctrl+Tab` |
| Previous Tab | `Ctrl+Shift+Tab` |
| Address Bar | `Ctrl+L` |
| Find in Page | `Ctrl+F` |
| Zoom In | `Ctrl++` |
| Zoom Out | `Ctrl+-` |
| Reset Zoom | `Ctrl+0` |
| Fullscreen | `F11` |
| DevTools | `F12` |
| Private Window | `Ctrl+Shift+N` |
| Print | `Ctrl+P` |
| View Source | `Ctrl+U` |
| Refresh | `Ctrl+R` or `F5` |
| Hard Refresh | `Ctrl+Shift+R` |
| Bookmark Page | `Ctrl+D` |
| History | `Ctrl+H` |

## Extensions

Kaliki Browser supports Chrome extensions. To install:

1. Download extension source (unpacked folder)
2. Place in `extensions/` folder inside the app directory
3. Restart browser — extension will load automatically

**Pre-installed:**
- Ad Blocker (built-in, blocks 100+ ad networks)

## Ad Blocker

Built-in ad and tracker blocking — no extension needed:
- Blocks 100+ ad networks (DoubleClick, Google Ads, Facebook Ads, etc.)
- Blocks trackers (Google Analytics, Hotjar, Mixpanel, etc.)
- Blocks cryptocurrency miners
- Per-site toggle (disable for specific sites)

## Privacy

- No telemetry or data collection
- No account required
- All data stored locally
- Private window leaves no traces
- Built-in tracker blocking

## System Requirements

### Windows
- Windows 10 or 11 (64-bit)
- 4 GB RAM minimum
- 500 MB disk space
- Internet connection

### Linux
- 64-bit Linux distribution
- glibc 2.28+ (Ubuntu 20.04+, Debian 11+)
- 4 GB RAM minimum
- 500 MB disk space

### macOS
- macOS 11 Big Sur or later
- Apple Silicon (M1/M2/M3) or Intel
- 4 GB RAM minimum

## License

MIT

## Developed by

**Kaliki Labs**
