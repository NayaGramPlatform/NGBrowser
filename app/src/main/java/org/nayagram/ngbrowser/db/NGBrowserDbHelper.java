package org.nayagram.ngbrowser.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NGBrowserDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ngbrowser.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_BOOKMARKS = "bookmarks";
    public static final String TABLE_HISTORY = "history";
    public static final String TABLE_DOWNLOADS = "downloads";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_MIME = "mime";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public static class Item {
        public final long id;
        public final String title;
        public final String url;
        public final long timestamp;

        public Item(long id, String title, String url, long timestamp) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    public NGBrowserDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_BOOKMARKS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_URL + " TEXT UNIQUE, " +
                COLUMN_TIMESTAMP + " INTEGER);");

        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER);");

        db.execSQL("CREATE TABLE " + TABLE_DOWNLOADS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_URL + " TEXT, " +
                COLUMN_MIME + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DOWNLOADS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_URL + " TEXT, " +
                    COLUMN_MIME + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER);");
        }
    }

    // --- Bookmarks ---
    public void addBookmark(String title, String url) {
        if (url == null || url.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, (title == null || title.trim().isEmpty()) ? url : title.trim());
        cv.put(COLUMN_URL, url);
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_BOOKMARKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void removeBookmark(String url) {
        if (url == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOKMARKS, COLUMN_URL + "=?", new String[]{url});
    }

    public boolean isBookmarked(String url) {
        if (url == null || url.isEmpty()) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_BOOKMARKS, new String[]{COLUMN_ID}, COLUMN_URL + "=?", new String[]{url}, null, null, null, "1");
        boolean exists = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return exists;
    }

    public List<Item> getBookmarks() {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_BOOKMARKS, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
        while (c != null && c.moveToNext()) {
            list.add(new Item(
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_URL)),
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            ));
        }
        if (c != null) c.close();
        return list;
    }

    // --- History (Deduplicated, Searchable, Time-Range Clearable) ---
    public void addHistory(String title, String url) {
        if (url == null || url.isEmpty() || url.startsWith("about:") || url.startsWith("resource://")) return;
        SQLiteDatabase db = getWritableDatabase();
        // Remove previous duplicate entry for the same URL so history stays clean and shows latest visit first
        db.delete(TABLE_HISTORY, COLUMN_URL + "=?", new String[]{url});
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, (title == null || title.trim().isEmpty()) ? url : title.trim());
        cv.put(COLUMN_URL, url);
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_HISTORY, null, cv);
    }

    public List<Item> getHistory() {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC", "300");
        while (c != null && c.moveToNext()) {
            list.add(new Item(
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_URL)),
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            ));
        }
        if (c != null) c.close();
        return list;
    }

    public List<Item> searchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getHistory();
        }
        List<Item> list = new ArrayList<>();
        String pattern = "%" + query.trim() + "%";
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_HISTORY,
                null,
                COLUMN_TITLE + " LIKE ? OR " + COLUMN_URL + " LIKE ?",
                new String[]{pattern, pattern},
                null,
                null,
                COLUMN_TIMESTAMP + " DESC",
                "150"
        );
        while (c != null && c.moveToNext()) {
            list.add(new Item(
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_URL)),
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            ));
        }
        if (c != null) c.close();
        return list;
    }

    public void deleteHistoryItem(long id) {
        getWritableDatabase().delete(TABLE_HISTORY, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearHistorySince(long sinceTimestampMillis) {
        getWritableDatabase().delete(TABLE_HISTORY, COLUMN_TIMESTAMP + ">=?", new String[]{String.valueOf(sinceTimestampMillis)});
    }

    public void clearHistory() {
        getWritableDatabase().delete(TABLE_HISTORY, null, null);
    }

    // --- Downloads ---
    public void addDownload(String filename, String url, String mimeType) {
        if (url == null || url.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, (filename == null || filename.isEmpty()) ? "download" : filename);
        cv.put(COLUMN_URL, url);
        cv.put(COLUMN_MIME, mimeType != null ? mimeType : "*/*");
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_DOWNLOADS, null, cv);
    }

    public List<Item> getDownloads() {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_DOWNLOADS, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC", "100");
        while (c != null && c.moveToNext()) {
            list.add(new Item(
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)),
                    c.getString(c.getColumnIndexOrThrow(COLUMN_URL)),
                    c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            ));
        }
        if (c != null) c.close();
        return list;
    }

    public void clearDownloads() {
        getWritableDatabase().delete(TABLE_DOWNLOADS, null, null);
    }
}
