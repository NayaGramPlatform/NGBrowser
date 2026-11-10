package org.nayagram.ngbrowser;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.nayagram.ngbrowser.db.NGBrowserDbHelper;
import org.nayagram.ngbrowser.model.TabItem;
import org.nayagram.ngbrowser.security.SecureDownloadHandler;
import org.nayagram.ngbrowser.security.ShieldsManager;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FrameLayout webContainer;
    private EditText urlEditText;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnShields, btnMenu, btnTabsCount;
    private TextView tvTabsCount;

    private final List<TabItem> tabsList = new ArrayList<>();
    private int currentTabIndex = -1;

    private final ShieldsManager shieldsManager = new ShieldsManager();
    private boolean isDesktopMode = false;
    private boolean isDarkMode = false;
    private boolean isHttpsOnlyMode = true;

    private NGBrowserDbHelper dbHelper;
    private SharedPreferences prefs;

    private static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NGBrowserDbHelper(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        initViews();
        setupListeners();

        // Create default initial tab with Brave/DuckDuckGo clean UI
        createNewTab("https://duckduckgo.com", false);
    }

    private void initViews() {
        webContainer = findViewById(R.id.web_container);
        urlEditText = findViewById(R.id.url_edit_text);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        btnShields = findViewById(R.id.btn_shields);
        btnMenu = findViewById(R.id.btn_menu);
        btnTabsCount = findViewById(R.id.btn_tabs_count);
        tvTabsCount = findViewById(R.id.tv_tabs_count);
    }

    private void updateTabsCount() {
        if (tvTabsCount != null) {
            tvTabsCount.setText(String.valueOf(tabsList.size()));
        }
    }

    private WebView getCurrentWebView() {
        if (currentTabIndex >= 0 && currentTabIndex < tabsList.size()) {
            return tabsList.get(currentTabIndex).getWebView();
        }
        return null;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebViewInstance(boolean isIncognito) {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(!isIncognito);
        settings.setDatabaseEnabled(!isIncognito);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        if (isIncognito) {
            settings.setSaveFormData(false);
            settings.setSavePassword(false);
            CookieManager.getInstance().setAcceptCookie(false);
        } else {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        }

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) ->
                SecureDownloadHandler.processDownload(MainActivity.this, url, userAgent, contentDisposition, mimetype));

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == getCurrentWebView()) {
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(newProgress);
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                for (TabItem tab : tabsList) {
                    if (tab.getWebView() == view) {
                        tab.setTitle(title);
                        break;
                    }
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (isHttpsOnlyMode && request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (url.startsWith("http://")) {
                        view.loadUrl(url.replace("http://", "https://"));
                        return true;
                    }
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request.getUrl() != null) {
                    String url = request.getUrl().toString();
                    if (shieldsManager.shouldBlockUrl(url)) {
                        return new WebResourceResponse("text/plain", "UTF-8",
                                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view == getCurrentWebView()) {
                    if (!urlEditText.hasFocus()) urlEditText.setText(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view == getCurrentWebView()) {
                    if (!urlEditText.hasFocus()) urlEditText.setText(url);
                }
                if (!isIncognito && url != null && !url.startsWith("about:")) {
                    dbHelper.addHistory(view.getTitle(), url);
                }
            }
        });

        return webView;
    }

    private void createNewTab(String url, boolean isIncognito) {
        WebView webView = createWebViewInstance(isIncognito);
        TabItem tab = new TabItem(UUID.randomUUID().toString(), isIncognito ? "Private Tab" : "New Tab", url, webView, isIncognito);
        tabsList.add(tab);
        switchToTab(tabsList.size() - 1);
        webView.loadUrl(url);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabsList.size()) return;
        currentTabIndex = index;

        webContainer.removeAllViews();
        TabItem activeTab = tabsList.get(index);
        webContainer.addView(activeTab.getWebView());

        urlEditText.setText(activeTab.getUrl());
        updateTabsCount();
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            WebView current = getCurrentWebView();
            if (current != null) current.reload();
        });

        btnShields.setOnClickListener(v -> showShieldsStatusDialog());
        btnTabsCount.setOnClickListener(v -> showTabsManagerDialog());
        btnMenu.setOnClickListener(this::showPopupMenu);

        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                loadEnteredUrl();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            WebView current = getCurrentWebView();
            if (current != null && current.canGoBack()) current.goBack();
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            WebView current = getCurrentWebView();
            if (current != null && current.canGoForward()) current.goForward();
        });

        findViewById(R.id.btn_home).setOnClickListener(v -> {
            WebView current = getCurrentWebView();
            if (current != null) current.loadUrl("https://duckduckgo.com");
        });
    }

    private void showShieldsStatusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🛡️ NG Shields & Privacy");
        String message = "Status: " + (shieldsManager.isShieldEnabled() ? "ACTIVE (AdBlock ON)" : "PAUSED") +
                "\nTrackers & Ads Blocked: " + shieldsManager.getBlockedCount() +
                "\nHTTPS-Only Mode: " + (isHttpsOnlyMode ? "ACTIVE" : "OFF");
        builder.setMessage(message);
        builder.setPositiveButton(shieldsManager.isShieldEnabled() ? "Pause Shields" : "Enable Shields", (dialog, which) -> {
            shieldsManager.setShieldEnabled(!shieldsManager.isShieldEnabled());
            btnShields.setAlpha(shieldsManager.isShieldEnabled() ? 1.0f : 0.4f);
            WebView current = getCurrentWebView();
            if (current != null) current.reload();
        });
        builder.setNeutralButton("Reset Counters", (dialog, which) -> shieldsManager.resetBlockedCount());
        builder.show();
    }

    private void loadEnteredUrl() {
        String input = urlEditText.getText().toString().trim();
        if (input.isEmpty()) return;

        String searchEngine = prefs.getString("search_engine", "DuckDuckGo");
        String searchUrlPrefix = "https://duckduckgo.com/?q=";
        if (searchEngine.equals("Google")) searchUrlPrefix = "https://www.google.com/search?q=";
        else if (searchEngine.equals("Brave")) searchUrlPrefix = "https://search.brave.com/search?q=";
        else if (searchEngine.equals("Bing")) searchUrlPrefix = "https://www.bing.com/search?q=";

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            if (input.contains(".") && !input.contains(" ")) {
                input = "https://" + input;
            } else {
                try {
                    input = searchUrlPrefix + URLEncoder.encode(input, "UTF-8");
                } catch (Exception e) {
                    input = "https://duckduckgo.com";
                }
            }
        }

        WebView current = getCurrentWebView();
        if (current != null) current.loadUrl(input);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
        urlEditText.clearFocus();
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("➕ New Tab");
        popup.getMenu().add("🕶️ Private Tab");
        popup.getMenu().add("⭐ Add Bookmark");
        popup.getMenu().add("📚 Bookmarks & History");
        popup.getMenu().add("🔒 HTTPS-Only Mode (" + (isHttpsOnlyMode ? "ON" : "OFF") + ")");
        popup.getMenu().add("🖥️ Desktop Mode (" + (isDesktopMode ? "ON" : "OFF") + ")");
        popup.getMenu().add("🌙 Dark Mode (" + (isDarkMode ? "ON" : "OFF") + ")");
        popup.getMenu().add("🗑️ Clear History & Cookies");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("New Tab")) {
                createNewTab("https://duckduckgo.com", false);
            } else if (title.contains("Private Tab")) {
                createNewTab("https://duckduckgo.com", true);
            } else if (title.contains("Add Bookmark")) {
                WebView current = getCurrentWebView();
                if (current != null) {
                    dbHelper.addBookmark(current.getTitle(), current.getUrl());
                    Toast.makeText(this, "Bookmark Saved ⭐", Toast.LENGTH_SHORT).show();
                }
            } else if (title.contains("Bookmarks & History")) {
                showBookmarksHistoryDialog();
            } else if (title.contains("HTTPS-Only Mode")) {
                isHttpsOnlyMode = !isHttpsOnlyMode;
                Toast.makeText(this, "HTTPS-Only: " + (isHttpsOnlyMode ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            } else if (title.contains("Desktop Mode")) {
                isDesktopMode = !isDesktopMode;
                WebView current = getCurrentWebView();
                if (current != null) {
                    current.getSettings().setUserAgentString(isDesktopMode ? DESKTOP_USER_AGENT : null);
                    current.reload();
                }
            } else if (title.contains("Dark Mode")) {
                isDarkMode = !isDarkMode;
                WebView current = getCurrentWebView();
                if (current != null) {
                    if (isDarkMode) {
                        current.evaluateJavascript("document.body.style.backgroundColor='#121212'; document.body.style.color='#FFFFFF';", null);
                    } else {
                        current.reload();
                    }
                }
            } else if (title.contains("Clear History")) {
                dbHelper.clearHistory();
                CookieManager.getInstance().removeAllCookies(null);
                Toast.makeText(this, "History & Cookies Cleared! 🗑️", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }

    private void showTabsManagerDialog() {
        List<String> tabTitles = new ArrayList<>();
        for (int i = 0; i < tabsList.size(); i++) {
            TabItem tab = tabsList.get(i);
            tabTitles.add((i == currentTabIndex ? "▶ " : "") + (tab.isIncognito() ? "🕶️ " : "") + tab.getTitle());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tabs (" + tabsList.size() + ")");
        builder.setItems(tabTitles.toArray(new String[0]), (dialog, which) -> switchToTab(which));
        builder.setPositiveButton("➕ New Tab", (dialog, which) -> createNewTab("https://duckduckgo.com", false));
        if (tabsList.size() > 1) {
            builder.setNegativeButton("❌ Close Tab", (dialog, which) -> {
                tabsList.remove(currentTabIndex);
                switchToTab(Math.max(0, currentTabIndex - 1));
            });
        }
        builder.show();
    }

    private void showBookmarksHistoryDialog() {
        CharSequence[] options = new CharSequence[]{"⭐ Bookmarks", "📚 History"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Saved Data");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                List<NGBrowserDbHelper.Item> bks = dbHelper.getBookmarks();
                List<String> titles = new ArrayList<>();
                for (NGBrowserDbHelper.Item b : bks) titles.add(b.title + "\n" + b.url);
                if (titles.isEmpty()) titles.add("No bookmarks.");
                AlertDialog.Builder b2 = new AlertDialog.Builder(this);
                b2.setTitle("Bookmarks");
                b2.setItems(titles.toArray(new String[0]), (d2, w2) -> {
                    if (!bks.isEmpty() && w2 < bks.size()) {
                        WebView current = getCurrentWebView();
                        if (current != null) current.loadUrl(bks.get(w2).url);
                    }
                });
                b2.show();
            } else {
                List<NGBrowserDbHelper.Item> hist = dbHelper.getHistory();
                List<String> titles = new ArrayList<>();
                for (NGBrowserDbHelper.Item h : hist) titles.add(h.title + "\n" + h.url);
                if (titles.isEmpty()) titles.add("No history.");
                AlertDialog.Builder b2 = new AlertDialog.Builder(this);
                b2.setTitle("History");
                b2.setItems(titles.toArray(new String[0]), (d2, w2) -> {
                    if (!hist.isEmpty() && w2 < hist.size()) {
                        WebView current = getCurrentWebView();
                        if (current != null) current.loadUrl(hist.get(w2).url);
                    }
                });
                b2.show();
            }
        });
        builder.show();
    }

    @Override
    public void onBackPressed() {
        WebView current = getCurrentWebView();
        if (current != null && current.canGoBack()) {
            current.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
