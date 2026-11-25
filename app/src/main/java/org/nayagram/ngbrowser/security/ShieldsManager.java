package org.nayagram.ngbrowser.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

/**
 * NG Shields Manager: coordinates native GeckoView C++/Rust Enhanced Tracking Protection
 * together with fast domain-level ad/tracker filtering and persistent privacy statistics.
 */
public class ShieldsManager {
    public static final int LEVEL_OFF = 0;
    public static final int LEVEL_STANDARD = 1;
    public static final int LEVEL_STRICT = 2;

    private static final String PREFS_NAME = "ng_shields_prefs";
    private static final String KEY_PROTECTION_LEVEL = "protection_level";
    private static final String KEY_HTTPS_ONLY = "https_only";
    private static final String KEY_TOTAL_BLOCKED = "total_blocked_count";
    private static final String KEY_COLOR_SCHEME = "color_scheme";

    private final SharedPreferences prefs;

    private static final HashSet<String> BLOCKED_AD_DOMAINS = new HashSet<>(Arrays.asList(
            "doubleclick.net", "googleadservices.com", "adservice.google.com",
            "pagead2.googlesyndication.com", "googlesyndication.com", "googletagmanager.com",
            "google-analytics.com", "analytics.google.com", "ad-delivery.net",
            "adnxs.com", "adsrvr.org", "advertising.com", "adform.net", "adroll.com",
            "outbrain.com", "taboola.com", "scorecardresearch.com", "quantserve.com",
            "hotjar.com", "criteo.com", "criteo.net", "amazon-adsystem.com",
            "clarity.ms", "mixpanel.com", "segment.io", "segment.com",
            "amplitude.com", "appsflyer.com", "adjust.com", "branch.io",
            "pubmatic.com", "rubiconproject.com", "openx.net", "casalemedia.com",
            "indexww.com", "sharethrough.com", "mgid.com", "revcontent.com",
            "popads.net", "popcash.net", "propellerads.com", "exoclick.com",
            "juicyads.com", "trafficjunky.net", "zedo.com", "bidswitch.net",
            "moatads.com", "iasds01.com", "doubleverify.com", "krxd.net",
            "bluekai.com", "demdex.net", "omtrdc.net", "newrelic.com",
            "nr-data.net", "bugsnag.com", "sentry.io", "fullstory.com",
            "mouseflow.com", "crazyegg.com", "luckyorange.com", "yandex.ru/metrika"
    ));

    public ShieldsManager(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getProtectionLevel() {
        return prefs.getInt(KEY_PROTECTION_LEVEL, LEVEL_STRICT);
    }

    public void setProtectionLevel(int level) {
        prefs.edit().putInt(KEY_PROTECTION_LEVEL, level).apply();
    }

    public boolean isShieldEnabled() {
        return getProtectionLevel() != LEVEL_OFF;
    }

    public boolean isHttpsOnly() {
        return prefs.getBoolean(KEY_HTTPS_ONLY, true);
    }

    public void setHttpsOnly(boolean enabled) {
        prefs.edit().putBoolean(KEY_HTTPS_ONLY, enabled).apply();
    }

    public int getPreferredColorScheme() {
        return prefs.getInt(KEY_COLOR_SCHEME, GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM);
    }

    public void setPreferredColorScheme(int scheme) {
        prefs.edit().putInt(KEY_COLOR_SCHEME, scheme).apply();
    }

    public long getTotalBlockedCount() {
        return prefs.getLong(KEY_TOTAL_BLOCKED, 0L);
    }

    public void recordBlocked() {
        long current = getTotalBlockedCount();
        prefs.edit().putLong(KEY_TOTAL_BLOCKED, current + 1L).apply();
    }

    public void resetTotalBlockedCount() {
        prefs.edit().putLong(KEY_TOTAL_BLOCKED, 0L).apply();
    }

    public boolean shouldBlockSubframeUrl(String urlString) {
        if (!isShieldEnabled() || urlString == null) return false;
        try {
            Uri uri = Uri.parse(urlString.toLowerCase(Locale.US));
            String host = uri.getHost();
            if (host == null) return false;
            for (String domain : BLOCKED_AD_DOMAINS) {
                if (host.equals(domain) || host.endsWith("." + domain)) {
                    recordBlocked();
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @NonNull
    public String getProtectionLabel() {
        int level = getProtectionLevel();
        if (level == LEVEL_STRICT) return "Strict (Ads, Trackers, Fingerprinting)";
        if (level == LEVEL_STANDARD) return "Standard (Balanced Privacy)";
        return "Off (Disabled)";
    }
}
