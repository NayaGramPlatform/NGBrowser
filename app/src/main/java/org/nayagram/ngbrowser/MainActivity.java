package org.nayagram.ngbrowser;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebRequestError;
import org.mozilla.geckoview.WebResponse;
import org.nayagram.ngbrowser.db.NGBrowserDbHelper;
import org.nayagram.ngbrowser.engine.NGEngineManager;
import org.nayagram.ngbrowser.manager.DownloadHandler;
import org.nayagram.ngbrowser.model.TabItem;
import org.nayagram.ngbrowser.security.ShieldsManager;
import org.nayagram.ngbrowser.util.SearchEngineHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NG Browser v2.0 MainActivity
 * Powered by standalone Mozilla GeckoView 140 Native C++/Rust Engine.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ANDROID_PERMISSIONS = 1001;

    private GeckoView geckoView;
    private GeckoRuntime geckoRuntime;

    private LinearLayout topToolbar;
    private LinearLayout bottomNavBar;
    private LinearLayout findInPageBar;

    private FrameLayout btnShieldsContainer;
    private ImageView ivShieldIcon;
    private TextView tvShieldBadge;
    private ImageView ivSecurityIcon;
    private EditText urlEditText;
    private ImageButton btnReloadStop;
    private FrameLayout btnTabsContainer;
    private TextView tvTabsCount;
    private ImageButton btnMenu;
    private ProgressBar progressBar;

    private EditText etFindQuery;
    private TextView tvFindMatches;
    private ImageButton btnFindPrev, btnFindNext, btnFindClose;

    private ImageButton btnBack, btnForward, btnHome, btnNewTab, btnBookmarkStar;

    private final List<TabItem> tabs = new ArrayList<>();
    private int currentTabIndex = -1;
    private boolean isFullScreen = false;

    private ShieldsManager shieldsManager;
    private NGBrowserDbHelper dbHelper;
    private GeckoSession.PermissionDelegate.Callback pendingPermissionCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shieldsManager = new ShieldsManager(this);
        dbHelper = new NGBrowserDbHelper(this);
        geckoRuntime = NGEngineManager.getRuntime(this);

        initViews();
        setupListeners();

        String initialUrl = extractUrlFromIntent(getIntent());
        createNewTab(initialUrl != null ? initialUrl : SearchEngineHelper.HOME_URL, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = extractUrlFromIntent(intent);
        if (url != null) {
            createNewTab(url, false);
        }
    }

    @Nullable
    private String extractUrlFromIntent(@Nullable Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                return data.toString();
            }
        }
        return null;
    }

    private void initViews() {
        geckoView = findViewById(R.id.geckoview);
        topToolbar = findViewById(R.id.top_toolbar);
        bottomNavBar = findViewById(R.id.bottom_nav_bar);
        findInPageBar = findViewById(R.id.find_in_page_bar);

        btnShieldsContainer = findViewById(R.id.btn_shields_container);
        ivShieldIcon = findViewById(R.id.iv_shield_icon);
        tvShieldBadge = findViewById(R.id.tv_shield_badge);
        ivSecurityIcon = findViewById(R.id.iv_security_icon);
        urlEditText = findViewById(R.id.url_edit_text);
        btnReloadStop = findViewById(R.id.btn_reload_stop);
        btnTabsContainer = findViewById(R.id.btn_tabs_container);
        tvTabsCount = findViewById(R.id.tv_tabs_count);
        btnMenu = findViewById(R.id.btn_menu);
        progressBar = findViewById(R.id.progress_bar);

        etFindQuery = findViewById(R.id.et_find_query);
        tvFindMatches = findViewById(R.id.tv_find_matches);
        btnFindPrev = findViewById(R.id.btn_find_prev);
        btnFindNext = findViewById(R.id.btn_find_next);
        btnFindClose = findViewById(R.id.btn_find_close);

        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnHome = findViewById(R.id.btn_home);
        btnNewTab = findViewById(R.id.btn_new_tab);
        btnBookmarkStar = findViewById(R.id.btn_bookmark_star);
    }

    private void setupListeners() {
        // Omni URL bar submission
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                navigateCurrentTab(urlEditText.getText().toString());
                hideKeyboard(urlEditText);
                urlEditText.clearFocus();
                return true;
            }
            return false;
        });

        // Reload / Stop button
        btnReloadStop.setOnClickListener(v -> {
            TabItem tab = getCurrentTab();
            if (tab == null) return;
            if (tab.isLoading()) {
                tab.getSession().stop();
            } else {
                tab.getSession().reload();
            }
        });

        // NG Shields Dashboard
        btnShieldsContainer.setOnClickListener(v -> showShieldsDialog());

        // Tabs Switcher
        btnTabsContainer.setOnClickListener(v -> showTabsDialog());

        // Overflow Menu
        btnMenu.setOnClickListener(this::showOverflowMenu);

        // Bottom Navigation
        btnBack.setOnClickListener(v -> {
            TabItem tab = getCurrentTab();
            if (tab != null && tab.canGoBack()) {
                tab.getSession().goBack();
            }
        });

        btnForward.setOnClickListener(v -> {
            TabItem tab = getCurrentTab();
            if (tab != null && tab.canGoForward()) {
                tab.getSession().goForward();
            }
        });

        btnHome.setOnClickListener(v -> navigateCurrentTab(SearchEngineHelper.HOME_URL));

        btnNewTab.setOnClickListener(v -> createNewTab(SearchEngineHelper.HOME_URL, false));

        btnBookmarkStar.setOnClickListener(v -> toggleCurrentBookmark());

        // Find in Page controls
        etFindQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                performFind(s.toString(), GeckoSession.FINDER_FIND_FORWARD);
            }
        });

        btnFindNext.setOnClickListener(v -> performFind(etFindQuery.getText().toString(), GeckoSession.FINDER_FIND_FORWARD));
        btnFindPrev.setOnClickListener(v -> performFind(etFindQuery.getText().toString(), GeckoSession.FINDER_FIND_BACKWARDS));
        btnFindClose.setOnClickListener(v -> closeFindBar());
    }

    @Nullable
    private TabItem getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }

    private void createNewTab(@NonNull String url, boolean isPrivate) {
        GeckoSession session = NGEngineManager.createSession(isPrivate, shieldsManager.isShieldEnabled(), false);
        TabItem tab = new TabItem(UUID.randomUUID().toString(), session, url, isPrivate);
        attachDelegates(tab);
        session.open(geckoRuntime);
        tab.setOpened(true);

        tabs.add(tab);
        switchToTab(tabs.size() - 1);
        session.loadUri(url);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        closeFindBar();
        currentTabIndex = index;
        TabItem tab = tabs.get(index);

        geckoView.releaseSession();
        geckoView.setSession(tab.getSession());
        refreshToolbarForTab(tab);
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        TabItem removed = tabs.remove(index);
        if (index == currentTabIndex) {
            geckoView.releaseSession();
        }
        removed.getSession().close();

        if (tabs.isEmpty()) {
            currentTabIndex = -1;
            createNewTab(SearchEngineHelper.HOME_URL, false);
        } else {
            int nextIndex = Math.min(index, tabs.size() - 1);
            switchToTab(nextIndex);
        }
    }

    private void navigateCurrentTab(@NonNull String input) {
        TabItem tab = getCurrentTab();
        if (tab == null) {
            createNewTab(SearchEngineHelper.resolveInputToUrl(this, input), false);
            return;
        }
        String resolved = SearchEngineHelper.resolveInputToUrl(this, input);
        tab.getSession().loadUri(resolved);
    }

    private void refreshToolbarForTab(@NonNull TabItem tab) {
        // Private mode toolbar tint vs Normal dark navy toolbar
        int barColor = ContextCompat.getColor(this, tab.isPrivate() ? R.color.ng_private_purple : R.color.ng_navy_dark);
        topToolbar.setBackgroundColor(barColor);

        if (!urlEditText.hasFocus()) {
            urlEditText.setText(SearchEngineHelper.formatDisplayUrl(tab.getUrl()));
        }

        // Security lock icon
        String u = tab.getUrl();
        if (u.startsWith("resource://") || u.startsWith("about:")) {
            ivSecurityIcon.setImageResource(R.drawable.ic_shield);
            ivSecurityIcon.setColorFilter(ContextCompat.getColor(this, R.color.ng_cyan_bright));
        } else if (tab.isSecure() || u.startsWith("https://")) {
            ivSecurityIcon.setImageResource(R.drawable.ic_lock);
            ivSecurityIcon.setColorFilter(ContextCompat.getColor(this, R.color.ng_shield_green));
        } else {
            ivSecurityIcon.setImageResource(R.drawable.ic_lock_open);
            ivSecurityIcon.setColorFilter(ContextCompat.getColor(this, R.color.ng_warn_amber));
        }

        // Reload vs Stop icon
        btnReloadStop.setImageResource(tab.isLoading() ? R.drawable.ic_close : R.drawable.ic_refresh);

        // Progress bar
        if (tab.isLoading() && tab.getProgress() > 0 && tab.getProgress() < 100) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(tab.getProgress());
        } else {
            progressBar.setVisibility(View.GONE);
        }

        // Shield badge
        ivShieldIcon.setColorFilter(ContextCompat.getColor(this,
                shieldsManager.isShieldEnabled() ? R.color.ng_cyan_bright : R.color.ng_text_hint));
        if (tab.getBlockedCount() > 0 && shieldsManager.isShieldEnabled()) {
            tvShieldBadge.setVisibility(View.VISIBLE);
            tvShieldBadge.setText(tab.getBlockedCount() > 99 ? "99+" : String.valueOf(tab.getBlockedCount()));
        } else {
            tvShieldBadge.setVisibility(View.GONE);
        }

        // Tab counter
        tvTabsCount.setText(String.valueOf(tabs.size()));

        // Back / Forward states
        btnBack.setAlpha(tab.canGoBack() ? 1.0f : 0.35f);
        btnForward.setAlpha(tab.canGoForward() ? 1.0f : 0.35f);

        // Bookmark star state
        boolean bookmarked = dbHelper.isBookmarked(tab.getUrl());
        btnBookmarkStar.setImageResource(bookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        btnBookmarkStar.setColorFilter(ContextCompat.getColor(this,
                bookmarked ? R.color.ng_cyan_bright : R.color.ng_white));
    }

    private void attachDelegates(@NonNull TabItem tab) {
        GeckoSession session = tab.getSession();

        // 1. Progress & Security Delegate
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(@NonNull GeckoSession s, @NonNull String url) {
                tab.setUrl(url);
                tab.setLoading(true);
                tab.setProgress(10);
                tab.resetBlockedCount();
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }

            @Override
            public void onPageStop(@NonNull GeckoSession s, boolean success) {
                tab.setLoading(false);
                tab.setProgress(100);
                if (success && !tab.isPrivate()) {
                    dbHelper.addHistory(tab.getTitle(), tab.getUrl());
                }
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }

            @Override
            public void onProgressChange(@NonNull GeckoSession s, int progress) {
                tab.setProgress(progress);
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }

            @Override
            public void onSecurityChange(@NonNull GeckoSession s, @NonNull SecurityInformation securityInfo) {
                tab.setSecure(securityInfo.isSecure);
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }
        });

        // 2. Navigation Delegate
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onLocationChange(@NonNull GeckoSession s, @Nullable String url,
                                         @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms,
                                         @NonNull Boolean hasUserGesture) {
                if (url != null) {
                    tab.setUrl(url);
                }
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }

            @Override
            public void onCanGoBack(@NonNull GeckoSession s, boolean canGoBack) {
                tab.setCanGoBack(canGoBack);
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }

            @Override
            public void onCanGoForward(@NonNull GeckoSession s, boolean canGoForward) {
                tab.setCanGoForward(canGoForward);
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }

            @Nullable
            @Override
            public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession s, @NonNull LoadRequest request) {
                String uri = request.uri;
                // Handle external app schemes (tel:, mailto:, whatsapp:, intent:, etc.)
                if (uri.startsWith("tel:") || uri.startsWith("mailto:") || uri.startsWith("sms:")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                    } catch (Exception ignored) {}
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                if (uri.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            String fallback = intent.getStringExtra("browser_fallback_url");
                            if (fallback != null) {
                                s.loadUri(fallback);
                            }
                        }
                    } catch (Exception ignored) {}
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }

            @Nullable
            @Override
            public GeckoResult<AllowOrDeny> onSubframeLoadRequest(@NonNull GeckoSession s, @NonNull LoadRequest request) {
                if (shieldsManager.shouldBlockSubframeUrl(request.uri)) {
                    tab.incrementBlockedCount();
                    if (tab == getCurrentTab()) {
                        runOnUiThread(() -> refreshToolbarForTab(tab));
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY);
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW);
            }

            @Nullable
            @Override
            public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession s, @NonNull String uri) {
                GeckoSession newSession = NGEngineManager.createSession(tab.isPrivate(), shieldsManager.isShieldEnabled(), tab.isDesktopMode());
                TabItem newTab = new TabItem(UUID.randomUUID().toString(), newSession, uri, tab.isPrivate());
                attachDelegates(newTab);
                newTab.setOpened(true);
                tabs.add(newTab);
                runOnUiThread(() -> switchToTab(tabs.size() - 1));
                return GeckoResult.fromValue(newSession);
            }

            @Nullable
            @Override
            public GeckoResult<String> onLoadError(@NonNull GeckoSession s, @Nullable String uri, @NonNull WebRequestError error) {
                String safeUri = uri != null ? uri.replace("<", "&lt;").replace(">", "&gt;") : "";
                String html = "data:text/html;charset=utf-8," + Uri.encode(
                        "<html><body style='background:#0d1b2a;color:#f0f6fc;font-family:sans-serif;padding:40px 24px;text-align:center;'>" +
                        "<h2 style='color:#00e5ff;'>Unable to load page</h2>" +
                        "<p style='color:#8ea4bd;word-break:break-all;'>" + safeUri + "</p>" +
                        "<p style='color:#ffb300;font-size:13px;'>Error code: " + error.code + " (Category: " + error.category + ")</p>" +
                        "<button onclick='window.location.reload()' style='margin-top:20px;padding:12px 24px;border:none;border-radius:20px;background:#00e5ff;color:#06131f;font-weight:bold;'>Try Again</button>" +
                        "</body></html>"
                );
                return GeckoResult.fromValue(html);
            }
        });

        // 3. Content Delegate (Title, Fullscreen Video, Downloads, Context Menu, Crash Recovery)
        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public void onTitleChange(@NonNull GeckoSession s, @Nullable String title) {
                tab.setTitle(title);
            }

            @Override
            public void onFullScreen(@NonNull GeckoSession s, boolean fullScreen) {
                runOnUiThread(() -> setFullScreenMode(fullScreen));
            }

            @Override
            public void onCloseRequest(@NonNull GeckoSession s) {
                runOnUiThread(() -> {
                    int idx = tabs.indexOf(tab);
                    if (idx >= 0) closeTab(idx);
                });
            }

            @Override
            public void onExternalResponse(@NonNull GeckoSession s, @NonNull WebResponse response) {
                runOnUiThread(() -> DownloadHandler.handleExternalResponse(MainActivity.this, response, dbHelper));
            }

            @Override
            public void onContextMenu(@NonNull GeckoSession s, int screenX, int screenY, @NonNull ContextElement element) {
                String targetUrl = element.linkUri != null ? element.linkUri : element.srcUri;
                if (targetUrl == null || targetUrl.isEmpty()) return;
                runOnUiThread(() -> showLinkContextDialog(targetUrl));
            }

            @Override
            public void onCrash(@NonNull GeckoSession s) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Tab process recovered", Toast.LENGTH_SHORT).show();
                    if (!s.isOpen()) s.open(geckoRuntime);
                    s.loadUri(tab.getUrl().isEmpty() ? SearchEngineHelper.HOME_URL : tab.getUrl());
                });
            }

            @Override
            public void onKill(@NonNull GeckoSession s) {
                runOnUiThread(() -> {
                    if (!s.isOpen()) s.open(geckoRuntime);
                    s.loadUri(tab.getUrl().isEmpty() ? SearchEngineHelper.HOME_URL : tab.getUrl());
                });
            }
        });

        // 4. Native C++/Rust Content Blocking Delegate (Real-time Tracker & Ad Counter)
        session.setContentBlockingDelegate(new ContentBlocking.Delegate() {
            @Override
            public void onContentBlocked(@NonNull GeckoSession s, @NonNull ContentBlocking.BlockEvent event) {
                tab.incrementBlockedCount();
                shieldsManager.recordBlocked();
                if (tab == getCurrentTab()) {
                    runOnUiThread(() -> refreshToolbarForTab(tab));
                }
            }
        });

        // 5. Permission Delegate (Android Runtime Permissions & Site Permissions)
        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public void onAndroidPermissionsRequest(@NonNull GeckoSession s, @Nullable String[] permissions, @NonNull Callback callback) {
                if (permissions == null || permissions.length == 0) {
                    callback.reject();
                    return;
                }
                pendingPermissionCallback = callback;
                ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_ANDROID_PERMISSIONS);
            }

            @Nullable
            @Override
            public GeckoResult<Integer> onContentPermissionRequest(@NonNull GeckoSession s, @NonNull ContentPermission perm) {
                GeckoResult<Integer> result = new GeckoResult<>();
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Site Permission Request")
                        .setMessage("Allow " + perm.uri + " to access this permission?")
                        .setPositiveButton("Allow", (d, w) -> result.complete(ContentPermission.VALUE_ALLOW))
                        .setNegativeButton("Deny", (d, w) -> result.complete(ContentPermission.VALUE_DENY))
                        .setOnCancelListener(d -> result.complete(ContentPermission.VALUE_DENY))
                        .show());
                return result;
            }
        });

        // 6. Prompt Delegate (JavaScript alert(), confirm(), prompt())
        session.setPromptDelegate(new GeckoSession.PromptDelegate() {
            @Nullable
            @Override
            public GeckoResult<PromptResponse> onAlertPrompt(@NonNull GeckoSession s, @NonNull AlertPrompt prompt) {
                GeckoResult<PromptResponse> result = new GeckoResult<>();
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle(prompt.title != null ? prompt.title : "Page Notice")
                        .setMessage(prompt.message != null ? prompt.message : "")
                        .setPositiveButton("OK", (d, w) -> result.complete(prompt.dismiss()))
                        .setOnCancelListener(d -> result.complete(prompt.dismiss()))
                        .show());
                return result;
            }

            @Nullable
            @Override
            public GeckoResult<PromptResponse> onButtonPrompt(@NonNull GeckoSession s, @NonNull ButtonPrompt prompt) {
                GeckoResult<PromptResponse> result = new GeckoResult<>();
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle(prompt.title != null ? prompt.title : "Confirm")
                        .setMessage(prompt.message != null ? prompt.message : "")
                        .setPositiveButton("OK", (d, w) -> result.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE)))
                        .setNegativeButton("Cancel", (d, w) -> result.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE)))
                        .setOnCancelListener(d -> result.complete(prompt.dismiss()))
                        .show());
                return result;
            }

            @Nullable
            @Override
            public GeckoResult<PromptResponse> onTextPrompt(@NonNull GeckoSession s, @NonNull TextPrompt prompt) {
                GeckoResult<PromptResponse> result = new GeckoResult<>();
                runOnUiThread(() -> {
                    EditText input = new EditText(MainActivity.this);
                    if (prompt.defaultValue != null) input.setText(prompt.defaultValue);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(prompt.title != null ? prompt.title : "Prompt")
                            .setMessage(prompt.message != null ? prompt.message : "")
                            .setView(input)
                            .setPositiveButton("OK", (d, w) -> result.complete(prompt.confirm(input.getText().toString())))
                            .setNegativeButton("Cancel", (d, w) -> result.complete(prompt.dismiss()))
                            .setOnCancelListener(d -> result.complete(prompt.dismiss()))
                            .show();
                });
                return result;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ANDROID_PERMISSIONS && pendingPermissionCallback != null) {
            boolean allGranted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                pendingPermissionCallback.grant();
            } else {
                pendingPermissionCallback.reject();
            }
            pendingPermissionCallback = null;
        }
    }

    private void setFullScreenMode(boolean fullScreen) {
        isFullScreen = fullScreen;
        topToolbar.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
        bottomNavBar.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
        if (fullScreen) {
            findInPageBar.setVisibility(View.GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void showLinkContextDialog(@NonNull String url) {
        String[] actions = {"Open in New Tab", "Open in Private Tab", "Copy Link", "Share Link"};
        new AlertDialog.Builder(this)
                .setTitle(url)
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        createNewTab(url, false);
                    } else if (which == 1) {
                        createNewTab(url, true);
                    } else if (which == 2) {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("URL", url));
                            Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 3) {
                        shareUrl(url, "");
                    }
                })
                .show();
    }

    private void showOverflowMenu(@NonNull View anchor) {
        TabItem tab = getCurrentTab();
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "New Tab");
        popup.getMenu().add(0, 2, 1, "New Private Tab");
        popup.getMenu().add(0, 3, 2, "Bookmarks");
        popup.getMenu().add(0, 4, 3, "History");
        popup.getMenu().add(0, 5, 4, "Downloads");
        popup.getMenu().add(0, 6, 5, "Find in Page");
        popup.getMenu().add(0, 7, 6, (tab != null && tab.isDesktopMode()) ? "Switch to Mobile Site" : "Request Desktop Site");
        popup.getMenu().add(0, 8, 7, "Share Page");
        popup.getMenu().add(0, 9, 8, "NG Shields & Privacy");
        popup.getMenu().add(0, 10, 9, "Settings & Engine Info");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: createNewTab(SearchEngineHelper.HOME_URL, false); return true;
                case 2: createNewTab(SearchEngineHelper.HOME_URL, true); return true;
                case 3: showBookmarksDialog(); return true;
                case 4: showHistoryDialog(); return true;
                case 5: showDownloadsDialog(); return true;
                case 6: openFindBar(); return true;
                case 7: toggleDesktopMode(); return true;
                case 8:
                    if (tab != null) shareUrl(tab.getUrl(), tab.getTitle());
                    return true;
                case 9: showShieldsDialog(); return true;
                case 10: showSettingsDialog(); return true;
                default: return false;
            }
        });
        popup.show();
    }

    private void toggleDesktopMode() {
        TabItem tab = getCurrentTab();
        if (tab == null) return;
        boolean next = !tab.isDesktopMode();
        tab.setDesktopMode(next);
        tab.getSession().getSettings().setUserAgentMode(
                next ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP : GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        );
        tab.getSession().getSettings().setViewportMode(
                next ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP : GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        );
        tab.getSession().reload();
        Toast.makeText(this, next ? "Desktop Site Enabled" : "Mobile Site Enabled", Toast.LENGTH_SHORT).show();
    }

    private void toggleCurrentBookmark() {
        TabItem tab = getCurrentTab();
        if (tab == null || tab.getUrl().isEmpty() || tab.getUrl().startsWith("resource://")) {
            Toast.makeText(this, "Cannot bookmark this page", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dbHelper.isBookmarked(tab.getUrl())) {
            dbHelper.removeBookmark(tab.getUrl());
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.addBookmark(tab.getTitle(), tab.getUrl());
            Toast.makeText(this, "Bookmarked!", Toast.LENGTH_SHORT).show();
        }
        refreshToolbarForTab(tab);
    }

    private void openFindBar() {
        findInPageBar.setVisibility(View.VISIBLE);
        etFindQuery.requestFocus();
    }

    private void closeFindBar() {
        findInPageBar.setVisibility(View.GONE);
        etFindQuery.setText("");
        tvFindMatches.setText("0/0");
        TabItem tab = getCurrentTab();
        if (tab != null) {
            tab.getSession().getFinder().clear();
        }
        hideKeyboard(etFindQuery);
    }

    private void performFind(@NonNull String query, int flags) {
        TabItem tab = getCurrentTab();
        if (tab == null) return;
        if (query.isEmpty()) {
            tab.getSession().getFinder().clear();
            tvFindMatches.setText("0/0");
            return;
        }
        tab.getSession().getFinder().find(query, flags).accept(result -> {
            if (result != null) {
                runOnUiThread(() -> tvFindMatches.setText(result.current + "/" + Math.max(result.total, 0)));
            }
        });
    }

    private void showTabsDialog() {
        String[] labels = new String[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            TabItem t = tabs.get(i);
            String prefix = (i == currentTabIndex ? "▶ " : "") + (t.isPrivate() ? "[Private] " : "");
            labels[i] = prefix + t.getTitle() + "
" + (t.getUrl().startsWith("resource://") ? "New Tab" : t.getUrl());
        }

        new AlertDialog.Builder(this)
                .setTitle("Open Tabs (" + tabs.size() + ")")
                .setItems(labels, (d, which) -> switchToTab(which))
                .setPositiveButton("+ New Tab", (d, w) -> createNewTab(SearchEngineHelper.HOME_URL, false))
                .setNeutralButton("+ Private Tab", (d, w) -> createNewTab(SearchEngineHelper.HOME_URL, true))
                .setNegativeButton("Close Current", (d, w) -> closeTab(currentTabIndex))
                .show();
    }

    private void showShieldsDialog() {
        TabItem tab = getCurrentTab();
        int tabBlocked = tab != null ? tab.getBlockedCount() : 0;
        long totalBlocked = shieldsManager.getTotalBlockedCount();

        String summary = "Engine: Gecko 140 Native Privacy Core
" +
                "Blocked on this tab: " + tabBlocked + "
" +
                "Total ads & trackers blocked: " + totalBlocked + "
" +
                "Protection Level: " + shieldsManager.getProtectionLabel() + "
" +
                "HTTPS-Only Mode: " + (shieldsManager.isHttpsOnly() ? "Enabled" : "Off");

        String[] options = {
                "Set Shield: Strict (Recommended)",
                "Set Shield: Standard",
                "Set Shield: Off",
                shieldsManager.isHttpsOnly() ? "Disable HTTPS-Only Mode" : "Enable HTTPS-Only Mode",
                "Clear All Browsing Data (Cookies, Cache, History)"
        };

        new AlertDialog.Builder(this)
                .setTitle("NG Shields Privacy Dashboard")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        updateProtectionLevel(ShieldsManager.LEVEL_STRICT);
                    } else if (which == 1) {
                        updateProtectionLevel(ShieldsManager.LEVEL_STANDARD);
                    } else if (which == 2) {
                        updateProtectionLevel(ShieldsManager.LEVEL_OFF);
                    } else if (which == 3) {
                        boolean next = !shieldsManager.isHttpsOnly();
                        shieldsManager.setHttpsOnly(next);
                        NGEngineManager.applyHttpsOnly(this, next);
                        Toast.makeText(this, "HTTPS-Only: " + (next ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                    } else if (which == 4) {
                        NGEngineManager.clearAllBrowsingData(this);
                        dbHelper.clearHistory();
                        Toast.makeText(this, "All cookies, cache & history cleared!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Stats Info", (d, w) ->
                        new AlertDialog.Builder(this).setTitle("Privacy Report").setMessage(summary).setPositiveButton("OK", null).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void updateProtectionLevel(int level) {
        shieldsManager.setProtectionLevel(level);
        NGEngineManager.applyProtectionLevel(this, level);
        boolean enabled = level != ShieldsManager.LEVEL_OFF;
        for (TabItem t : tabs) {
            t.getSession().getSettings().setUseTrackingProtection(enabled);
        }
        TabItem cur = getCurrentTab();
        if (cur != null) {
            cur.getSession().reload();
            refreshToolbarForTab(cur);
        }
        Toast.makeText(this, "Shields: " + shieldsManager.getProtectionLabel(), Toast.LENGTH_SHORT).show();
    }

    private void showBookmarksDialog() {
        List<NGBrowserDbHelper.Item> list = dbHelper.getBookmarks();
        if (list.isEmpty()) {
            Toast.makeText(this, "No bookmarks saved yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            labels[i] = list.get(i).title + "
" + list.get(i).url;
        }
        new AlertDialog.Builder(this)
                .setTitle("Bookmarks (" + list.size() + ")")
                .setItems(labels, (d, which) -> navigateCurrentTab(list.get(which).url))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showHistoryDialog() {
        List<NGBrowserDbHelper.Item> list = dbHelper.getHistory();
        if (list.isEmpty()) {
            Toast.makeText(this, "History is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            labels[i] = list.get(i).title + "
" + list.get(i).url;
        }
        new AlertDialog.Builder(this)
                .setTitle("History (" + list.size() + ")")
                .setItems(labels, (d, which) -> navigateCurrentTab(list.get(which).url))
                .setNeutralButton("Clear History", (d, w) -> {
                    dbHelper.clearHistory();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showDownloadsDialog() {
        List<NGBrowserDbHelper.Item> list = dbHelper.getDownloads();
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            labels[i] = list.get(i).title + "
" + list.get(i).url;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Downloads (" + list.size() + ")")
                .setPositiveButton("Open Downloads Folder", (d, w) -> {
                    try {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    } catch (Exception e) {
                        Toast.makeText(this, "Unable to open system Downloads", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null);

        if (list.isEmpty()) {
            builder.setMessage("No downloads recorded yet.");
        } else {
            builder.setItems(labels, (d, which) -> navigateCurrentTab(list.get(which).url));
            builder.setNeutralButton("Clear List", (d, w) -> dbHelper.clearDownloads());
        }
        builder.show();
    }

    private void showSettingsDialog() {
        String[] options = {
                "Default Search Engine: " + SearchEngineHelper.getSelectedEngineName(this),
                "Web Page Theme (System / Dark / Light)",
                "About NG Browser Engine"
        };
        new AlertDialog.Builder(this)
                .setTitle("NG Browser Settings")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showSearchEnginePicker();
                    } else if (which == 1) {
                        showThemePicker();
                    } else if (which == 2) {
                        new AlertDialog.Builder(this)
                                .setTitle("NG Browser v2.0.0")
                                .setMessage(
                                        "NayaGram Platform Official Browser

" +
                                        "• Standalone Native Engine: Mozilla GeckoView 140 ESR (C++/Rust)
" +
                                        "• Upstream Core Sync: Brave-Core 1.102.37 / Chromium 159
" +
                                        "• Built-in Enhanced Tracking Protection & Total Cookie Isolation
" +
                                        "• Package: org.nayagram.ngbrowser"
                                )
                                .setPositiveButton("OK", null)
                                .show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showSearchEnginePicker() {
        int current = SearchEngineHelper.getSelectedEngineIndex(this);
        new AlertDialog.Builder(this)
                .setTitle("Choose Default Search Engine")
                .setSingleChoiceItems(SearchEngineHelper.ENGINE_NAMES, current, (d, which) -> {
                    SearchEngineHelper.setSelectedEngineIndex(this, which);
                    Toast.makeText(this, "Search Engine: " + SearchEngineHelper.ENGINE_NAMES[which], Toast.LENGTH_SHORT).show();
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemePicker() {
        String[] themes = {"Follow System", "Dark Mode", "Light Mode"};
        new AlertDialog.Builder(this)
                .setTitle("Preferred Web Content Theme")
                .setItems(themes, (d, which) -> {
                    int scheme = GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM;
                    if (which == 1) scheme = GeckoRuntimeSettings.COLOR_SCHEME_DARK;
                    else if (which == 2) scheme = GeckoRuntimeSettings.COLOR_SCHEME_LIGHT;
                    shieldsManager.setPreferredColorScheme(scheme);
                    NGEngineManager.applyColorScheme(this, scheme);
                    Toast.makeText(this, "Theme set to " + themes[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void shareUrl(@NonNull String url, @NonNull String title) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        share.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(share, "Share via"));
    }

    private void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            TabItem tab = getCurrentTab();
            if (tab != null) tab.getSession().exitFullScreen();
            setFullScreenMode(false);
            return;
        }
        if (findInPageBar.getVisibility() == View.VISIBLE) {
            closeFindBar();
            return;
        }
        TabItem tab = getCurrentTab();
        if (tab != null && tab.canGoBack()) {
            tab.getSession().goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        geckoView.releaseSession();
        for (TabItem t : tabs) {
            t.getSession().close();
        }
        tabs.clear();
        super.onDestroy();
    }
}
