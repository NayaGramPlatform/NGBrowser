package org.nayagram.ngbrowser.security;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;
import android.widget.Toast;

public class SecureDownloadHandler {

    public static void processDownload(Context context, String url, String userAgent, String contentDisposition, String mimeType) {
        if (url == null) return;

        // Verify URL Scheme Security (Block file:// or dangerous schemes)
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(context, "Download blocked: Unsafe URL scheme", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("NG Browser Secure Download");
            request.setTitle(fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(context, "Secure Download Started: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Download Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
