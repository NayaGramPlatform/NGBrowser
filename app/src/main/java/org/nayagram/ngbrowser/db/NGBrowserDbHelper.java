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
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_BOOKMARKS = "bookmarks";
    public static final String TABLE_HISTORY = "history";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public static class Item {
        public long id;
        public String title;
        public String url;
        public long timestamp;

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public void addBookmark(String title, String url) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, (title == null || title.isEmpty()) ? url : title);
        cv.put(COLUMN_URL, url);
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_BOOKMARKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addHistory(String title, String url) {
        if (url == null || url.startsWith("about:")) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, (title == null || title.isEmpty()) ? url : title);
        cv.put(COLUMN_URL, url);
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_HISTORY, null, cv);
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

    public List<Item> getHistory() {
        List<Item> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_HISTORY, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC", "100");
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

    public void clearHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
    }
}
