const { app, BrowserWindow, session, ipcMain, dialog, Menu, MenuItem, globalShortcut, nativeTheme } = require('electron');
const path = require('path');
const fs = require('fs');

// Force dark mode
nativeTheme.themeSource = 'dark';

// Data paths
const userDataPath = app.getPath('userData');
const bookmarksPath = path.join(userDataPath, 'bookmarks.json');
const historyPath = path.join(userDataPath, 'history.json');
const settingsPath = path.join(userDataPath, 'settings.json');

// Ad blocker filter list
let adBlockFilters = [];

function loadAdBlockFilters() {
  const filtersFile = path.join(__dirname, 'adblock-filters.txt');
  if (fs.existsSync(filtersFile)) {
    const content = fs.readFileSync(filtersFile, 'utf-8');
    adBlockFilters = content
      .split('\n')
      .filter(line => line.trim() && !line.startsWith('!') && !line.startsWith('['))
      .map(line => line.trim());
  }
}

function setupAdBlocker(ses) {
  ses.webRequest.onBeforeRequest({ urls: ['*://*/*'] }, (details, callback) => {
    const url = details.url;
    const blocked = adBlockFilters.some(filter => {
      if (filter.startsWith('||')) {
        const domain = filter.slice(2).replace('^', '').replace('*', '');
        return url.includes(domain);
      }
      if (filter.startsWith('|')) {
        return url.startsWith(filter.slice(1));
      }
      return url.includes(filter);
    });
    callback({ cancel: blocked });
  });
}

// Load extensions from the extensions folder
async function loadExtensions(ses) {
  const extensionsDir = path.join(__dirname, 'extensions');
  if (!fs.existsSync(extensionsDir)) return;

  const entries = fs.readdirSync(extensionsDir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const extPath = path.join(extensionsDir, entry.name);
      const manifestPath = path.join(extPath, 'manifest.json');
      if (fs.existsSync(manifestPath)) {
        try {
          await ses.loadExtension(extPath);
          console.log(`Loaded extension: ${entry.name}`);
        } catch (e) {
          console.error(`Failed to load extension ${entry.name}:`, e.message);
        }
      }
    }
  }
}

// Data helpers
function loadJSON(filePath, defaultValue = []) {
  try {
    if (fs.existsSync(filePath)) {
      return JSON.parse(fs.readFileSync(filePath, 'utf-8'));
    }
  } catch (e) {
    console.error(`Error loading ${filePath}:`, e.message);
  }
  return defaultValue;
}

function saveJSON(filePath, data) {
  try {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2));
  } catch (e) {
    console.error(`Error saving ${filePath}:`, e.message);
  }
}

let mainWindow = null;
let adBlockEnabled = true;
let blockedCount = 0;

function createWindow(isPrivate = false) {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 800,
    minHeight: 600,
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#1a1a2e',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      webviewTag: true,
      sandbox: false
    },
    icon: path.join(__dirname, 'assets', 'icon.png')
  });

  win.loadFile(path.join(__dirname, 'src', 'index.html'));

  if (!mainWindow) mainWindow = win;

  win.on('closed', () => {
    if (win === mainWindow) mainWindow = null;
  });

  return win;
}

app.whenReady().then(async () => {
  loadAdBlockFilters();

  const ses = session.defaultSession;
  setupAdBlocker(ses);
  await loadExtensions(ses);

  createWindow();

  // Register global shortcuts
  app.on('browser-window-focus', () => {
    // Shortcuts are handled in renderer via preload
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow();
});

// IPC handlers
ipcMain.handle('get-bookmarks', () => loadJSON(bookmarksPath, []));
ipcMain.handle('save-bookmarks', (e, bookmarks) => {
  saveJSON(bookmarksPath, bookmarks);
  return true;
});

ipcMain.handle('get-history', () => loadJSON(historyPath, []));
ipcMain.handle('save-history', (e, history) => {
  saveJSON(historyPath, history);
  return true;
});

ipcMain.handle('get-settings', () => loadJSON(settingsPath, { homepage: 'kaliki://newtab', searchEngine: 'google' }));
ipcMain.handle('save-settings', (e, settings) => {
  saveJSON(settingsPath, settings);
  return true;
});

ipcMain.handle('toggle-adblock', () => {
  adBlockEnabled = !adBlockEnabled;
  return adBlockEnabled;
});

ipcMain.handle('get-adblock-status', () => ({ enabled: adBlockEnabled, blocked: blockedCount }));

ipcMain.handle('new-private-window', () => {
  const win = createWindow(true);
  return true;
});

ipcMain.handle('show-download-dialog', async (e, opts) => {
  const result = await dialog.showSaveDialog(mainWindow, {
    defaultPath: opts.filename || 'download',
    filters: [{ name: 'All Files', extensions: ['*'] }]
  });
  return result;
});

ipcMain.handle('minimize-window', (e) => {
  BrowserWindow.fromWebContents(e.sender)?.minimize();
});

ipcMain.handle('maximize-window', (e) => {
  const win = BrowserWindow.fromWebContents(e.sender);
  if (win?.isMaximized()) {
    win.unmaximize();
  } else {
    win?.maximize();
  }
});

ipcMain.handle('close-window', (e) => {
  BrowserWindow.fromWebContents(e.sender)?.close();
});

ipcMain.handle('toggle-fullscreen', (e) => {
  const win = BrowserWindow.fromWebContents(e.sender);
  win?.setFullScreen(!win.isFullScreen());
});
