package org.nayagram.ngbrowser.model;

import androidx.annotation.NonNull;
import org.mozilla.geckoview.GeckoSession;

public class TabItem {
    private final String id;
    private final GeckoSession session;
    private final boolean isPrivate;

    private String title;
    private String url;
    private boolean isLoading;
    private int progress;
    private boolean canGoBack;
    private boolean canGoForward;
    private boolean isSecure;
    private int blockedCount;
    private boolean isDesktopMode;
    private boolean isOpened;

    public TabItem(@NonNull String id, @NonNull GeckoSession session, String initialUrl, boolean isPrivate) {
        this.id = id;
        this.session = session;
        this.url = initialUrl != null ? initialUrl : "";
        this.title = isPrivate ? "Private Tab" : "New Tab";
        this.isPrivate = isPrivate;
        this.isLoading = false;
        this.progress = 0;
        this.canGoBack = false;
        this.canGoForward = false;
        this.isSecure = true;
        this.blockedCount = 0;
        this.isDesktopMode = false;
        this.isOpened = false;
    }

    @NonNull
    public String getId() { return id; }

    @NonNull
    public GeckoSession getSession() { return session; }

    public boolean isPrivate() { return isPrivate; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url != null ? url : ""; }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public boolean canGoBack() { return canGoBack; }
    public void setCanGoBack(boolean canGoBack) { this.canGoBack = canGoBack; }

    public boolean canGoForward() { return canGoForward; }
    public void setCanGoForward(boolean canGoForward) { this.canGoForward = canGoForward; }

    public boolean isSecure() { return isSecure; }
    public void setSecure(boolean secure) { isSecure = secure; }

    public int getBlockedCount() { return blockedCount; }
    public void incrementBlockedCount() { this.blockedCount++; }
    public void resetBlockedCount() { this.blockedCount = 0; }

    public boolean isDesktopMode() { return isDesktopMode; }
    public void setDesktopMode(boolean desktopMode) { isDesktopMode = desktopMode; }

    public boolean isOpened() { return isOpened; }
    public void setOpened(boolean opened) { isOpened = opened; }
}
