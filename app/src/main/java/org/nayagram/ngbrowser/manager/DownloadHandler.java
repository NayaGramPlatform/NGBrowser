package org.nayagram.ngbrowser.manager;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.URLUtil;
import android.widget.Toast;
import androidx.annotation.NonNull;
import org.mozilla.geckoview.WebResponse;
import org.nayagram.ngbrowser.db.NGBrowserDbHelper;
import java.util.Map;

public class DownloadHandler {

    public static void handleExternalResponse(@NonNull Context context, @NonNull WebResponse response, @NonNull NGBrowserDbHelper dbHelper) {
        String uriStr = response.uri;
        if (uriStr == null || (!uriStr.startsWith("http://") && !uriStr.startsWith("https://"))) {
            Toast.makeText(context, "Unsupported download URL", Toast.LENGTH_SHORT).show();
            return;
        }

        String contentDisposition = null;
        String contentType = null;
        for (Map.Entry<String, String> entry : response.headers.entrySet()) {
            if (entry.getKey() == null) continue;
            if ("content-disposition".equalsIgnoreCase(entry.getKey())) {
                contentDisposition = entry.getValue();
            } else if ("content-type".equalsIgnoreCase(entry.getKey())) {
                contentType = entry.getValue();
            }
        }

        String filename = URLUtil.guessFileName(uriStr, contentDisposition, contentType);
        if (filename == null || filename.trim().isEmpty()) {
            filename = "ngbrowser_download_" + System.currentTimeMillis();
        }
        // Sanitize filename
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(uriStr));
            if (contentType != null && !contentType.isEmpty()) {
                request.setMimeType(contentType);
            }
            request.setTitle(filename);
            request.setDescription("Downloading via NG Browser");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                dbHelper.addDownload(filename, uriStr, contentType);
                Toast.makeText(context, "Downloading: " + filename, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
