package org.nayagram.ngbrowser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlEditText;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnShields;
    private boolean isShieldActive = true;

    // Brave-Core / NayaGram AdBlock & Tracker Rules
    private static final String[] AD_DOMAINS = new String[]{
            "doubleclick.net", "googleadservices.com", "adservice.google.com",
            "facebook.com/tr", "analytics.google.com", "googletagmanager.com",
            "ad-delivery.net", "adnxs.com", "pagead2.googlesyndication.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        setupListeners();

        webView.loadUrl("https://duckduckgo.com");
    }

    private void initViews() {
        webView = findViewById(R.id.web_view);
        urlEditText = findViewById(R.id.url_edit_text);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        btnShields = findViewById(R.id.btn_shields);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Security & Privacy Enhancements
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (isShieldActive && request.getUrl() != null) {
                    String url = request.getUrl().toString().toLowerCase();
                    for (String adDomain : AD_DOMAINS) {
                        if (url.contains(adDomain)) {
                            return new WebResourceResponse("text/plain", "UTF-8",
                                    new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!urlEditText.hasFocus()) {
                    urlEditText.setText(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!urlEditText.hasFocus()) {
                    urlEditText.setText(url);
                }
            }
        });
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        btnShields.setOnClickListener(v -> {
            isShieldActive = !isShieldActive;
            btnShields.setAlpha(isShieldActive ? 1.0f : 0.4f);
            String status = isShieldActive ? "NG Shields Active (AdBlock ON)" : "NG Shields Paused";
            Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
            webView.reload();
        });

        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                loadEnteredUrl();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        findViewById(R.id.btn_home).setOnClickListener(v -> webView.loadUrl("https://duckduckgo.com"));
    }

    private void loadEnteredUrl() {
        String input = urlEditText.getText().toString().trim();
        if (input.isEmpty()) return;

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            if (input.contains(".") && !input.contains(" ")) {
                input = "https://" + input;
            } else {
                try {
                    input = "https://duckduckgo.com/?q=" + URLEncoder.encode(input, "UTF-8");
                } catch (Exception e) {
                    input = "https://duckduckgo.com";
                }
            }
        }
        webView.loadUrl(input);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
        }
        urlEditText.clearFocus();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
