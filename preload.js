const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('kaliki', {
  // Window controls
  minimize: () => ipcRenderer.invoke('minimize-window'),
  maximize: () => ipcRenderer.invoke('maximize-window'),
  close: () => ipcRenderer.invoke('close-window'),
  toggleFullscreen: () => ipcRenderer.invoke('toggle-fullscreen'),

  // Bookmarks
  getBookmarks: () => ipcRenderer.invoke('get-bookmarks'),
  saveBookmarks: (bookmarks) => ipcRenderer.invoke('save-bookmarks', bookmarks),

  // History
  getHistory: () => ipcRenderer.invoke('get-history'),
  saveHistory: (history) => ipcRenderer.invoke('save-history', history),

  // Settings
  getSettings: () => ipcRenderer.invoke('get-settings'),
  saveSettings: (settings) => ipcRenderer.invoke('save-settings', settings),

  // Ad blocker
  toggleAdBlock: () => ipcRenderer.invoke('toggle-adblock'),
  getAdBlockStatus: () => ipcRenderer.invoke('get-adblock-status'),

  // Private window
  newPrivateWindow: () => ipcRenderer.invoke('new-private-window'),

  // Downloads
  showDownloadDialog: (opts) => ipcRenderer.invoke('show-download-dialog', opts)
});
