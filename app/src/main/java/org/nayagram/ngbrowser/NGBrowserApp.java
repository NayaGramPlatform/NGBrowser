package org.nayagram.ngbrowser;

import android.app.Application;
import org.nayagram.ngbrowser.engine.NGEngineManager;

public class NGBrowserApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Pre-warm standalone GeckoView C++/Rust runtime on app startup
        NGEngineManager.getRuntime(this);
    }
}
