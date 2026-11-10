package org.nayagram.ngbrowser.model;

import android.webkit.WebView;

public class TabItem {
    private String id;
    private String title;
    private String url;
    private WebView webView;
    private boolean isIncognito;

    public TabItem(String id, String title, String url, WebView webView, boolean isIncognito) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.webView = webView;
        this.isIncognito = isIncognito;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public WebView getWebView() { return webView; }
    public boolean isIncognito() { return isIncognito; }
}
