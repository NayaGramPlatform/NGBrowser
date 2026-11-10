package org.nayagram.ngbrowser.security;

import android.net.Uri;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

public class ShieldsManager {

    private boolean isShieldEnabled = true;
    private int blockedCount = 0;

    private static final HashSet<String> AD_AND_TRACKER_DOMAINS = new HashSet<>(Arrays.asList(
            "doubleclick.net", "googleadservices.com", "adservice.google.com",
            "facebook.com/tr", "analytics.google.com", "googletagmanager.com",
            "ad-delivery.net", "adnxs.com", "pagead2.googlesyndication.com",
            "outbrain.com", "taboola.com", "scorecardresearch.com",
            "hotjar.com", "criteo.com", "amazon-adsystem.com",
            "clarity.ms", "mixpanel.com", "segment.io"
    ));

    public ShieldsManager() {}

    public boolean isShieldEnabled() { return isShieldEnabled; }
    public void setShieldEnabled(boolean enabled) { this.isShieldEnabled = enabled; }
    public int getBlockedCount() { return blockedCount; }
    public void resetBlockedCount() { blockedCount = 0; }

    public boolean shouldBlockUrl(String urlString) {
        if (!isShieldEnabled || urlString == null) return false;
        try {
            Uri uri = Uri.parse(urlString.toLowerCase());
            String host = uri.getHost();
            if (host != null) {
                for (String domain : AD_AND_TRACKER_DOMAINS) {
                    if (host.equals(domain) || host.endsWith("." + domain)) {
                        blockedCount++;
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
