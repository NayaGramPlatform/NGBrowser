package org.nayagram.ngbrowser.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class SearchEngineHelper {

    public static final String HOME_URL = "resource://android/assets/home.html";

    private static final String PREFS_NAME = "ng_search_prefs";
    private static final String KEY_ENGINE = "default_search_engine";

    public static final String[] ENGINE_NAMES = {
            "DuckDuckGo (Private)",
            "Brave Search",
            "Google",
            "Startpage",
            "Bing"
    };

    private static final String[] ENGINE_URLS = {
            "https://duckduckgo.com/?q=",
            "https://search.brave.com/search?q=",
            "https://www.google.com/search?q=",
            "https://www.startpage.com/sp/search?query=",
            "https://www.bing.com/search?q="
    };

    private SearchEngineHelper() {}

    public static int getSelectedEngineIndex(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int idx = prefs.getInt(KEY_ENGINE, 0);
        return (idx >= 0 && idx < ENGINE_URLS.length) ? idx : 0;
    }

    public static void setSelectedEngineIndex(@NonNull Context context, int index) {
        if (index < 0 || index >= ENGINE_URLS.length) return;
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_ENGINE, index)
                .apply();
    }

    @NonNull
    public static String getSelectedEngineName(@NonNull Context context) {
        return ENGINE_NAMES[getSelectedEngineIndex(context)];
    }

    @NonNull
    public static String resolveInputToUrl(@NonNull Context context, @NonNull String rawInput) {
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return HOME_URL;
        }
        String lower = trimmed.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("about:") || lower.startsWith("resource://")
                || lower.startsWith("data:")) {
            return trimmed;
        }
        // Check if input looks like a domain or localhost without spaces
        if (!trimmed.contains(" ") && (trimmed.contains(".") || lower.startsWith("localhost"))) {
            return "https://" + trimmed;
        }
        // Otherwise perform a web search
        String base = ENGINE_URLS[getSelectedEngineIndex(context)];
        try {
            return base + URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return base + Uri.encode(trimmed);
        }
    }

    @NonNull
    public static String formatDisplayUrl(String url) {
        if (url == null || url.isEmpty() || HOME_URL.equals(url) || url.startsWith("resource://android/assets/")) {
            return "";
        }
        return url;
    }
}
