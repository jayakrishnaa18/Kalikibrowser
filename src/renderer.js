// Kaliki Browser - Renderer Process
// Tab management, navigation, bookmarks, history, keyboard shortcuts

const tabs = [];
let activeTabId = null;
let bookmarks = [];
let history = [];
let zoomLevels = {};

const DEFAULT_SHORTCUTS = [
  { label: 'Google', url: 'https://www.google.com', icon: 'G' },
  { label: 'YouTube', url: 'https://www.youtube.com', icon: '▶' },
  { label: 'GitHub', url: 'https://www.github.com', icon: '⌥' },
  { label: 'Reddit', url: 'https://www.reddit.com', icon: 'R' },
  { label: 'Twitter', url: 'https://www.x.com', icon: '𝕏' },
  { label: 'Wikipedia', url: 'https://www.wikipedia.org', icon: 'W' },
  { label: 'Stack Overflow', url: 'https://stackoverflow.com', icon: '⚡' },
  { label: 'Gmail', url: 'https://mail.google.com', icon: '✉' }
];

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
  bookmarks = await window.kaliki.getBookmarks();
  history = await window.kaliki.getHistory();

  createNewTab();
  setupEventListeners();
  setupKeyboardShortcuts();
});

// --- Tab Management ---
function generateId() {
  return 'tab-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
}

function createNewTab(url = null) {
  const tabId = generateId();
  const tab = {
    id: tabId,
    title: 'New Tab',
    url: url || 'kaliki://newtab',
    isNewTab: !url
  };
  tabs.push(tab);

  // Create tab element
  const tabEl = document.createElement('div');
  tabEl.className = 'tab';
  tabEl.dataset.id = tabId;
  tabEl.innerHTML = `
    <span class="tab-title">New Tab</span>
    <button class="tab-close" title="Close tab">✕</button>
  `;
  tabEl.addEventListener('click', (e) => {
    if (!e.target.classList.contains('tab-close')) {
      switchTab(tabId);
    }
  });
  tabEl.querySelector('.tab-close').addEventListener('click', (e) => {
    e.stopPropagation();
    closeTab(tabId);
  });
  document.getElementById('tabs-container').appendChild(tabEl);

  // Create webview or newtab page
  const container = document.getElementById('webview-container');
  if (!url) {
    // New tab page
    const newtabDiv = document.createElement('div');
    newtabDiv.className = 'newtab-page';
    newtabDiv.id = `page-${tabId}`;
    newtabDiv.innerHTML = `
      <div class="newtab-logo">⚡</div>
      <div class="newtab-title">Kaliki</div>
      <div class="newtab-subtitle">Developed by Kaliki Labs</div>
      <input class="newtab-search" type="text" placeholder="Search or enter URL" autofocus>
      <div class="newtab-shortcuts">
        ${DEFAULT_SHORTCUTS.map(s => `
          <div class="shortcut-item" data-url="${s.url}">
            <div class="shortcut-icon">${s.icon}</div>
            <span class="shortcut-label">${s.label}</span>
          </div>
        `).join('')}
      </div>
    `;
    container.appendChild(newtabDiv);

    // Setup newtab search
    const searchInput = newtabDiv.querySelector('.newtab-search');
    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        navigateTo(searchInput.value);
      }
    });

    // Setup shortcut clicks
    newtabDiv.querySelectorAll('.shortcut-item').forEach(item => {
      item.addEventListener('click', () => {
        navigateTo(item.dataset.url);
      });
    });
  } else {
    createWebview(tabId, url);
  }

  switchTab(tabId);
  return tabId;
}

function createWebview(tabId, url) {
  const container = document.getElementById('webview-container');

  // Remove newtab page if exists
  const newtab = document.getElementById(`page-${tabId}`);
  if (newtab) newtab.remove();

  const webview = document.createElement('webview');
  webview.id = `webview-${tabId}`;
  webview.src = url;
  webview.setAttribute('allowpopups', '');
  webview.setAttribute('webpreferences', 'contextIsolation=yes');
  webview.style.width = '100%';
  webview.style.height = '100%';
  webview.style.position = 'absolute';
  webview.style.top = '0';
  webview.style.left = '0';

  // Event listeners
  webview.addEventListener('page-title-updated', (e) => {
    updateTabTitle(tabId, e.title);
  });

  webview.addEventListener('did-navigate', (e) => {
    updateTabUrl(tabId, e.url);
    addToHistory(e.url, getTabTitle(tabId));
  });

  webview.addEventListener('did-navigate-in-page', (e) => {
    if (e.isMainFrame) {
      updateTabUrl(tabId, e.url);
    }
  });

  webview.addEventListener('did-start-loading', () => {
    if (tabId === activeTabId) {
      document.getElementById('btn-refresh').textContent = '✕';
    }
  });

  webview.addEventListener('did-stop-loading', () => {
    if (tabId === activeTabId) {
      document.getElementById('btn-refresh').textContent = '↻';
      updateNavButtons();
    }
  });

  webview.addEventListener('new-window', (e) => {
    createNewTab(e.url);
  });

  container.appendChild(webview);

  // Update tab data
  const tab = tabs.find(t => t.id === tabId);
  if (tab) {
    tab.url = url;
    tab.isNewTab = false;
  }

  return webview;
}

function switchTab(tabId) {
  activeTabId = tabId;

  // Update tab styles
  document.querySelectorAll('.tab').forEach(el => {
    el.classList.toggle('active', el.dataset.id === tabId);
  });

  // Show/hide webviews and newtab pages
  document.querySelectorAll('#webview-container > *').forEach(el => {
    const id = el.id.replace('webview-', '').replace('page-', '');
    if (el.tagName === 'WEBVIEW') {
      el.classList.toggle('active', id === tabId);
    } else {
      el.style.display = id === tabId ? 'flex' : 'none';
    }
  });

  // Update URL bar
  const tab = tabs.find(t => t.id === tabId);
  if (tab) {
    const urlBar = document.getElementById('url-bar');
    urlBar.value = tab.isNewTab ? '' : tab.url;
    updateSecurityIcon(tab.url);
    updateBookmarkButton(tab.url);
  }

  updateNavButtons();
}

function closeTab(tabId) {
  const idx = tabs.findIndex(t => t.id === tabId);
  if (idx === -1) return;

  // Remove tab
  tabs.splice(idx, 1);

  // Remove DOM elements
  const tabEl = document.querySelector(`.tab[data-id="${tabId}"]`);
  if (tabEl) tabEl.remove();

  const webview = document.getElementById(`webview-${tabId}`);
  if (webview) webview.remove();

  const newtab = document.getElementById(`page-${tabId}`);
  if (newtab) newtab.remove();

  // If no tabs left, create a new one
  if (tabs.length === 0) {
    createNewTab();
    return;
  }

  // Switch to nearest tab
  if (activeTabId === tabId) {
    const newIdx = Math.min(idx, tabs.length - 1);
    switchTab(tabs[newIdx].id);
  }
}

function updateTabTitle(tabId, title) {
  const tab = tabs.find(t => t.id === tabId);
  if (tab) tab.title = title;

  const tabEl = document.querySelector(`.tab[data-id="${tabId}"] .tab-title`);
  if (tabEl) tabEl.textContent = title;
}

function updateTabUrl(tabId, url) {
  const tab = tabs.find(t => t.id === tabId);
  if (tab) tab.url = url;

  if (tabId === activeTabId) {
    document.getElementById('url-bar').value = url;
    updateSecurityIcon(url);
    updateBookmarkButton(url);
    updateNavButtons();
  }
}

function getTabTitle(tabId) {
  const tab = tabs.find(t => t.id === tabId);
  return tab ? tab.title : '';
}

// --- Navigation ---
function navigateTo(input) {
  if (!input.trim()) return;

  let url = input.trim();

  // Check if it's a URL
  if (url.match(/^https?:\/\//)) {
    // Already a URL
  } else if (url.match(/^[a-zA-Z0-9][-a-zA-Z0-9]*\.[a-zA-Z]{2,}/)) {
    url = 'https://' + url;
  } else {
    // Search query
    url = `https://www.google.com/search?q=${encodeURIComponent(url)}`;
  }

  const tab = tabs.find(t => t.id === activeTabId);
  if (!tab) return;

  if (tab.isNewTab) {
    createWebview(activeTabId, url);
  } else {
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview) {
      webview.src = url;
    }
  }

  tab.url = url;
  tab.isNewTab = false;
}

function goBack() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (webview && webview.canGoBack()) webview.goBack();
}

function goForward() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (webview && webview.canGoForward()) webview.goForward();
}

function refresh() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (webview) {
    if (webview.isLoading()) {
      webview.stop();
    } else {
      webview.reload();
    }
  }
}

function goHome() {
  const tab = tabs.find(t => t.id === activeTabId);
  if (tab) {
    // Convert current tab to newtab
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview) webview.remove();

    tab.isNewTab = true;
    tab.url = 'kaliki://newtab';
    tab.title = 'New Tab';

    const container = document.getElementById('webview-container');
    const newtabDiv = document.createElement('div');
    newtabDiv.className = 'newtab-page active';
    newtabDiv.id = `page-${activeTabId}`;
    newtabDiv.style.display = 'flex';
    newtabDiv.innerHTML = `
      <div class="newtab-logo">⚡</div>
      <div class="newtab-title">Kaliki</div>
      <div class="newtab-subtitle">Developed by Kaliki Labs</div>
      <input class="newtab-search" type="text" placeholder="Search or enter URL" autofocus>
      <div class="newtab-shortcuts">
        ${DEFAULT_SHORTCUTS.map(s => `
          <div class="shortcut-item" data-url="${s.url}">
            <div class="shortcut-icon">${s.icon}</div>
            <span class="shortcut-label">${s.label}</span>
          </div>
        `).join('')}
      </div>
    `;
    container.appendChild(newtabDiv);

    const searchInput = newtabDiv.querySelector('.newtab-search');
    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') navigateTo(searchInput.value);
    });
    newtabDiv.querySelectorAll('.shortcut-item').forEach(item => {
      item.addEventListener('click', () => navigateTo(item.dataset.url));
    });

    updateTabTitle(activeTabId, 'New Tab');
    document.getElementById('url-bar').value = '';
  }
}

function updateNavButtons() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  document.getElementById('btn-back').disabled = !webview || !webview.canGoBack();
  document.getElementById('btn-forward').disabled = !webview || !webview.canGoForward();
}

function updateSecurityIcon(url) {
  const icon = document.getElementById('security-icon');
  if (!url || url.startsWith('kaliki://')) {
    icon.textContent = '⚡';
  } else if (url.startsWith('https://')) {
    icon.textContent = '🔒';
  } else {
    icon.textContent = '⚠️';
  }
}

// --- Bookmarks ---
function updateBookmarkButton(url) {
  const btn = document.getElementById('btn-bookmark');
  const isBookmarked = bookmarks.some(b => b.url === url);
  btn.textContent = isBookmarked ? '★' : '☆';
  btn.classList.toggle('bookmarked', isBookmarked);
}

function toggleBookmark() {
  const tab = tabs.find(t => t.id === activeTabId);
  if (!tab || tab.isNewTab) return;

  const idx = bookmarks.findIndex(b => b.url === tab.url);
  if (idx >= 0) {
    bookmarks.splice(idx, 1);
  } else {
    bookmarks.push({ title: tab.title, url: tab.url, date: Date.now() });
  }

  window.kaliki.saveBookmarks(bookmarks);
  updateBookmarkButton(tab.url);
  renderBookmarks();
}

function renderBookmarks() {
  const container = document.getElementById('sidebar-content');
  container.innerHTML = bookmarks.map((b, i) => `
    <div class="bookmark-item" data-url="${b.url}">
      <span>📄</span>
      <span class="bookmark-title">${b.title || b.url}</span>
      <span class="bookmark-delete" data-index="${i}">✕</span>
    </div>
  `).join('');

  container.querySelectorAll('.bookmark-item').forEach(item => {
    item.addEventListener('click', (e) => {
      if (!e.target.classList.contains('bookmark-delete')) {
        navigateTo(item.dataset.url);
      }
    });
  });

  container.querySelectorAll('.bookmark-delete').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      bookmarks.splice(parseInt(btn.dataset.index), 1);
      window.kaliki.saveBookmarks(bookmarks);
      renderBookmarks();
    });
  });
}

// --- History ---
function addToHistory(url, title) {
  if (!url || url.startsWith('kaliki://')) return;

  history.unshift({
    url, title,
    timestamp: Date.now()
  });

  // Keep last 1000 entries
  if (history.length > 1000) history = history.slice(0, 1000);
  window.kaliki.saveHistory(history);
}

// --- Find in Page ---
let findOpen = false;

function toggleFind() {
  const findbar = document.getElementById('findbar');
  findOpen = !findOpen;

  if (findOpen) {
    findbar.classList.remove('hidden');
    document.getElementById('find-input').focus();
  } else {
    findbar.classList.add('hidden');
    const webview = document.getElementById(`webview-${activeTabId}`);
    if (webview) webview.stopFindInPage('clearSelection');
  }
}

function findInPage(text, forward = true) {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (!webview || !text) return;

  webview.findInPage(text, { forward });
}

// --- Zoom ---
function zoomIn() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (!webview) return;
  const current = zoomLevels[activeTabId] || 1.0;
  const newZoom = Math.min(current + 0.1, 3.0);
  zoomLevels[activeTabId] = newZoom;
  webview.setZoomFactor(newZoom);
}

function zoomOut() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (!webview) return;
  const current = zoomLevels[activeTabId] || 1.0;
  const newZoom = Math.max(current - 0.1, 0.3);
  zoomLevels[activeTabId] = newZoom;
  webview.setZoomFactor(newZoom);
}

function zoomReset() {
  const webview = document.getElementById(`webview-${activeTabId}`);
  if (!webview) return;
  zoomLevels[activeTabId] = 1.0;
  webview.setZoomFactor(1.0);
}

// --- Event Listeners ---
function setupEventListeners() {
  // Window controls
  document.getElementById('btn-minimize').addEventListener('click', () => window.kaliki.minimize());
  document.getElementById('btn-maximize').addEventListener('click', () => window.kaliki.maximize());
  document.getElementById('btn-close').addEventListener('click', () => window.kaliki.close());

  // Tab bar
  document.getElementById('btn-new-tab').addEventListener('click', () => createNewTab());

  // Navigation
  document.getElementById('btn-back').addEventListener('click', goBack);
  document.getElementById('btn-forward').addEventListener('click', goForward);
  document.getElementById('btn-refresh').addEventListener('click', refresh);
  document.getElementById('btn-home').addEventListener('click', goHome);

  // URL bar
  const urlBar = document.getElementById('url-bar');
  urlBar.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      navigateTo(urlBar.value);
      urlBar.blur();
    }
    if (e.key === 'Escape') {
      urlBar.blur();
    }
  });
  urlBar.addEventListener('focus', () => {
    urlBar.select();
  });

  // Bookmark
  document.getElementById('btn-bookmark').addEventListener('click', toggleBookmark);

  // Shield / Ad blocker
  document.getElementById('btn-shield').addEventListener('click', toggleShieldPopup);
  document.getElementById('adblock-toggle').addEventListener('change', async (e) => {
    await window.kaliki.toggleAdBlock();
    updateShieldStatus();
  });

  // Menu button
  document.getElementById('btn-menu').addEventListener('click', toggleDropdownMenu);

  // Find bar
  document.getElementById('find-input').addEventListener('input', (e) => {
    findInPage(e.target.value);
  });
  document.getElementById('find-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      findInPage(e.target.value, !e.shiftKey);
    }
    if (e.key === 'Escape') {
      toggleFind();
    }
  });
  document.getElementById('find-next').addEventListener('click', () => {
    findInPage(document.getElementById('find-input').value, true);
  });
  document.getElementById('find-prev').addEventListener('click', () => {
    findInPage(document.getElementById('find-input').value, false);
  });
  document.getElementById('find-close').addEventListener('click', toggleFind);

  // Sidebar
  document.getElementById('sidebar-close').addEventListener('click', () => {
    document.getElementById('sidebar').classList.add('hidden');
  });

  // Context menu - hide on click elsewhere
  document.addEventListener('click', (e) => {
    if (!e.target.closest('#context-menu')) {
      document.getElementById('context-menu').classList.add('hidden');
    }
    if (!e.target.closest('#dropdown-menu') && !e.target.closest('#btn-menu')) {
      document.getElementById('dropdown-menu').classList.add('hidden');
    }
    if (!e.target.closest('#shield-popup') && !e.target.closest('#btn-shield')) {
      document.getElementById('shield-popup').classList.add('hidden');
    }
  });

  // Right-click context menu
  document.addEventListener('contextmenu', (e) => {
    if (e.target.closest('#webview-container')) {
      e.preventDefault();
      showContextMenu(e.clientX, e.clientY);
    }
  });

  // Context menu actions
  document.getElementById('context-menu').addEventListener('click', (e) => {
    const action = e.target.closest('.menu-item')?.dataset.action;
    if (!action) return;
    handleContextAction(action);
    document.getElementById('context-menu').classList.add('hidden');
  });

  // Dropdown menu actions
  document.getElementById('dropdown-menu').addEventListener('click', (e) => {
    const action = e.target.closest('.menu-item')?.dataset.action;
    if (!action) return;
    handleMenuAction(action);
    document.getElementById('dropdown-menu').classList.add('hidden');
  });
}

// --- Keyboard Shortcuts ---
function setupKeyboardShortcuts() {
  document.addEventListener('keydown', (e) => {
    const ctrl = e.ctrlKey || e.metaKey;
    const shift = e.shiftKey;

    // Ctrl+T: New tab
    if (ctrl && !shift && e.key === 't') {
      e.preventDefault();
      createNewTab();
    }
    // Ctrl+W: Close tab
    else if (ctrl && !shift && e.key === 'w') {
      e.preventDefault();
      closeTab(activeTabId);
    }
    // Ctrl+Tab: Next tab
    else if (ctrl && e.key === 'Tab') {
      e.preventDefault();
      const idx = tabs.findIndex(t => t.id === activeTabId);
      const nextIdx = shift ? (idx - 1 + tabs.length) % tabs.length : (idx + 1) % tabs.length;
      switchTab(tabs[nextIdx].id);
    }
    // Ctrl+L or F6: Focus URL bar
    else if ((ctrl && e.key === 'l') || e.key === 'F6') {
      e.preventDefault();
      document.getElementById('url-bar').focus();
    }
    // Ctrl+R or F5: Refresh
    else if ((ctrl && e.key === 'r') || e.key === 'F5') {
      e.preventDefault();
      refresh();
    }
    // Ctrl+F: Find
    else if (ctrl && !shift && e.key === 'f') {
      e.preventDefault();
      if (!findOpen) toggleFind();
      else document.getElementById('find-input').focus();
    }
    // Escape: Close find
    else if (e.key === 'Escape') {
      if (findOpen) toggleFind();
    }
    // Ctrl+Shift+N: Private window
    else if (ctrl && shift && e.key === 'N') {
      e.preventDefault();
      window.kaliki.newPrivateWindow();
    }
    // F11: Fullscreen
    else if (e.key === 'F11') {
      e.preventDefault();
      window.kaliki.toggleFullscreen();
    }
    // F12: DevTools
    else if (e.key === 'F12') {
      e.preventDefault();
      const webview = document.getElementById(`webview-${activeTabId}`);
      if (webview) {
        if (webview.isDevToolsOpened()) {
          webview.closeDevTools();
        } else {
          webview.openDevTools();
        }
      }
    }
    // Ctrl++: Zoom in
    else if (ctrl && (e.key === '=' || e.key === '+')) {
      e.preventDefault();
      zoomIn();
    }
    // Ctrl+-: Zoom out
    else if (ctrl && e.key === '-') {
      e.preventDefault();
      zoomOut();
    }
    // Ctrl+0: Reset zoom
    else if (ctrl && e.key === '0') {
      e.preventDefault();
      zoomReset();
    }
    // Ctrl+P: Print
    else if (ctrl && e.key === 'p') {
      e.preventDefault();
      const webview = document.getElementById(`webview-${activeTabId}`);
      if (webview) webview.print();
    }
    // Ctrl+U: View source
    else if (ctrl && e.key === 'u') {
      e.preventDefault();
      viewSource();
    }
    // Ctrl+H: History
    else if (ctrl && e.key === 'h') {
      e.preventDefault();
      showHistory();
    }
    // Ctrl+B: Bookmarks sidebar
    else if (ctrl && e.key === 'b') {
      e.preventDefault();
      toggleSidebar();
    }
    // Alt+Left: Back
    else if (e.altKey && e.key === 'ArrowLeft') {
      e.preventDefault();
      goBack();
    }
    // Alt+Right: Forward
    else if (e.altKey && e.key === 'ArrowRight') {
      e.preventDefault();
      goForward();
    }
  });
}

// --- Menus ---
function showContextMenu(x, y) {
  const menu = document.getElementById('context-menu');
  menu.classList.remove('hidden');
  menu.style.left = x + 'px';
  menu.style.top = y + 'px';

  // Adjust if off-screen
  const rect = menu.getBoundingClientRect();
  if (rect.right > window.innerWidth) {
    menu.style.left = (window.innerWidth - rect.width - 5) + 'px';
  }
  if (rect.bottom > window.innerHeight) {
    menu.style.top = (window.innerHeight - rect.height - 5) + 'px';
  }
}

function toggleDropdownMenu() {
  const menu = document.getElementById('dropdown-menu');
  const btn = document.getElementById('btn-menu');
  const rect = btn.getBoundingClientRect();

  if (menu.classList.contains('hidden')) {
    menu.classList.remove('hidden');
    menu.style.top = (rect.bottom + 4) + 'px';
    menu.style.right = (window.innerWidth - rect.right) + 'px';
    menu.style.left = 'auto';
  } else {
    menu.classList.add('hidden');
  }
}

function toggleShieldPopup() {
  const popup = document.getElementById('shield-popup');
  popup.classList.toggle('hidden');
  if (!popup.classList.contains('hidden')) {
    updateShieldStatus();
  }
}

async function updateShieldStatus() {
  const status = await window.kaliki.getAdBlockStatus();
  document.getElementById('adblock-toggle').checked = status.enabled;
  document.getElementById('blocked-count').textContent = `Ads blocked: ${status.blocked}`;
}

function handleContextAction(action) {
  switch (action) {
    case 'back': goBack(); break;
    case 'forward': goForward(); break;
    case 'refresh': refresh(); break;
    case 'view-source': viewSource(); break;
    case 'devtools':
      const wv = document.getElementById(`webview-${activeTabId}`);
      if (wv) wv.openDevTools();
      break;
    case 'print':
      const wvp = document.getElementById(`webview-${activeTabId}`);
      if (wvp) wvp.print();
      break;
  }
}

function handleMenuAction(action) {
  switch (action) {
    case 'new-tab': createNewTab(); break;
    case 'new-private': window.kaliki.newPrivateWindow(); break;
    case 'history': showHistory(); break;
    case 'bookmarks': toggleSidebar(); break;
    case 'downloads-page': break;
    case 'zoom-in': zoomIn(); break;
    case 'zoom-out': zoomOut(); break;
    case 'zoom-reset': zoomReset(); break;
    case 'fullscreen': window.kaliki.toggleFullscreen(); break;
    case 'find': toggleFind(); break;
    case 'print-menu':
      const wv = document.getElementById(`webview-${activeTabId}`);
      if (wv) wv.print();
      break;
    case 'view-source-menu': viewSource(); break;
    case 'devtools-menu':
      const wvd = document.getElementById(`webview-${activeTabId}`);
      if (wvd) wvd.openDevTools();
      break;
    case 'settings': break;
    case 'about': showAbout(); break;
  }
}

// --- Utility functions ---
function viewSource() {
  const tab = tabs.find(t => t.id === activeTabId);
  if (tab && !tab.isNewTab) {
    createNewTab('view-source:' + tab.url);
  }
}

function showHistory() {
  // Open history in a simple newtab-like view
  const tabId = createNewTab();
  const tab = tabs.find(t => t.id === tabId);
  if (tab) {
    updateTabTitle(tabId, 'History');
    const page = document.getElementById(`page-${tabId}`);
    if (page) {
      page.innerHTML = `
        <div class="internal-page" style="width:100%;max-width:800px;overflow-y:auto;max-height:100%;">
          <h2>History</h2>
          ${history.slice(0, 100).map(h => `
            <div class="history-item" data-url="${h.url}">
              <span class="history-time">${new Date(h.timestamp).toLocaleTimeString()}</span>
              <span class="history-title">${h.title || h.url}</span>
            </div>
          `).join('') || '<p style="color:var(--text-muted)">No history yet</p>'}
        </div>
      `;
      page.querySelectorAll('.history-item').forEach(item => {
        item.addEventListener('click', () => navigateTo(item.dataset.url));
      });
    }
  }
}

function toggleSidebar() {
  const sidebar = document.getElementById('sidebar');
  sidebar.classList.toggle('hidden');
  if (!sidebar.classList.contains('hidden')) {
    renderBookmarks();
  }
}

function showAbout() {
  const tabId = createNewTab();
  const page = document.getElementById(`page-${tabId}`);
  if (page) {
    updateTabTitle(tabId, 'About');
    page.innerHTML = `
      <div style="text-align:center;padding:60px;">
        <div style="font-size:64px;margin-bottom:16px;">⚡</div>
        <h1 style="font-size:28px;margin-bottom:8px;">Kaliki Browser</h1>
        <p style="color:var(--text-secondary);margin-bottom:4px;">Version 2.7.0</p>
        <p style="color:var(--text-muted);font-size:12px;">Developed by Kaliki Labs</p>
        <p style="color:var(--text-muted);font-size:12px;margin-top:24px;">Fast. Private. Secure.</p>
      </div>
    `;
  }
}
