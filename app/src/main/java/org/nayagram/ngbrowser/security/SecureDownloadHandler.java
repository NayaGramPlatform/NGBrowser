package org.nayagram.ngbrowser.security;

import android.content.Context;
import androidx.annotation.NonNull;
import org.mozilla.geckoview.WebResponse;
import org.nayagram.ngbrowser.db.NGBrowserDbHelper;
import org.nayagram.ngbrowser.manager.DownloadHandler;

public class SecureDownloadHandler {
    public static void enqueueDownload(@NonNull Context context, @NonNull WebResponse response, @NonNull NGBrowserDbHelper dbHelper) {
        DownloadHandler.handleExternalResponse(context, response, dbHelper);
    }
}
