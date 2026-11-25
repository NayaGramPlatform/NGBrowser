package org.nayagram.ngbrowser.engine;

import android.content.Context;
import androidx.annotation.NonNull;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.StorageController;
import org.nayagram.ngbrowser.security.ShieldsManager;

/**
 * Manages the standalone Mozilla GeckoView C++/Rust rendering engine runtime,
 * privacy/tracking protection settings, HTTPS-Only enforcement, and session creation.
 */
public final class NGEngineManager {

    private static volatile GeckoRuntime sRuntime;

    private NGEngineManager() {}

    @NonNull
    public static synchronized GeckoRuntime getRuntime(@NonNull Context context) {
        if (sRuntime == null) {
            Context appCtx = context.getApplicationContext();
            ShieldsManager shields = new ShieldsManager(appCtx);

            ContentBlocking.Settings cbSettings = buildContentBlockingSettings(shields.getProtectionLevel());

            GeckoRuntimeSettings runtimeSettings = new GeckoRuntimeSettings.Builder()
                    .contentBlocking(cbSettings)
                    .allowInsecureConnections(
                            shields.isHttpsOnly()
                                    ? GeckoRuntimeSettings.HTTPS_ONLY
                                    : GeckoRuntimeSettings.HTTPS_ONLY_PRIVATE
                    )
                    .preferredColorScheme(shields.getPreferredColorScheme())
                    .build();

            sRuntime = GeckoRuntime.create(appCtx, runtimeSettings);
        }
        return sRuntime;
    }

    @NonNull
    public static ContentBlocking.Settings buildContentBlockingSettings(int level) {
        ContentBlocking.Settings.Builder builder = new ContentBlocking.Settings.Builder()
                .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                .cookiePurging(true)
                .cookieBannerHandlingMode(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
                .cookieBannerHandlingModePrivateBrowsing(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
                .queryParameterStrippingEnabled(true)
                .queryParameterStrippingPrivateBrowsingEnabled(true);

        if (level == ShieldsManager.LEVEL_STRICT) {
            builder.antiTracking(ContentBlocking.AntiTracking.STRICT)
                    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                    .strictSocialTrackingProtection(true)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                    .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS);
        } else if (level == ShieldsManager.LEVEL_STANDARD) {
            builder.antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.DEFAULT)
                    .strictSocialTrackingProtection(false)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                    .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS);
        } else {
            builder.antiTracking(ContentBlocking.AntiTracking.NONE)
                    .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.NONE)
                    .strictSocialTrackingProtection(false)
                    .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_ALL)
                    .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS);
        }

        return builder.build();
    }

    public static void applyProtectionLevel(@NonNull Context context, int level) {
        GeckoRuntime runtime = getRuntime(context);
        ContentBlocking.Settings current = runtime.getSettings().getContentBlocking();
        if (level == ShieldsManager.LEVEL_STRICT) {
            current.setAntiTracking(ContentBlocking.AntiTracking.STRICT);
            current.setEnhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT);
            current.setStrictSocialTrackingProtection(true);
            current.setCookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS);
        } else if (level == ShieldsManager.LEVEL_STANDARD) {
            current.setAntiTracking(ContentBlocking.AntiTracking.DEFAULT);
            current.setEnhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.DEFAULT);
            current.setStrictSocialTrackingProtection(false);
            current.setCookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS);
        } else {
            current.setAntiTracking(ContentBlocking.AntiTracking.NONE);
            current.setEnhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.NONE);
            current.setStrictSocialTrackingProtection(false);
            current.setCookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_ALL);
        }
    }

    public static void applyHttpsOnly(@NonNull Context context, boolean enabled) {
        GeckoRuntime runtime = getRuntime(context);
        runtime.getSettings().setAllowInsecureConnections(
                enabled ? GeckoRuntimeSettings.HTTPS_ONLY : GeckoRuntimeSettings.ALLOW_ALL
        );
    }

    public static void applyColorScheme(@NonNull Context context, int scheme) {
        GeckoRuntime runtime = getRuntime(context);
        runtime.getSettings().setPreferredColorScheme(scheme);
    }

    @NonNull
    public static GeckoSession createSession(boolean isPrivate, boolean useTrackingProtection, boolean isDesktopMode) {
        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(isPrivate)
                .useTrackingProtection(useTrackingProtection)
                .userAgentMode(isDesktopMode
                        ? GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                        : GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .viewportMode(isDesktopMode
                        ? GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                        : GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .allowJavascript(true)
                .suspendMediaWhenInactive(true)
                .build();
        return new GeckoSession(settings);
    }

    @NonNull
    public static GeckoResult<Void> clearAllBrowsingData(@NonNull Context context) {
        return getRuntime(context).getStorageController().clearData(StorageController.ClearFlags.ALL);
    }
}
