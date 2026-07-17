package com.example.myapplication.security;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Compatibility screenshot detector based on MediaStore image insert/update events.
 */
public class ScreenshotObserver {

    public interface Listener {
        void onScreenshotDetected();
    }

    private static final long QUERY_DELAY_MS = 500L;
    private static final long START_TOLERANCE_MS = 10_000L;
    private static final long FUTURE_TOLERANCE_MS = 5_000L;
    private static final int MAX_QUERY_COUNT = 5;
    private static final String[] SCREENSHOT_KEYWORDS = {
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screen shot",
            "screencapture",
            "screen_capture",
            "screenshots",
            "截屏",
            "截图",
            "屏幕截图"
    };

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final ContentObserver observer;
    private final Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            queryLatestImage();
        }
    };

    private boolean registered;
    private long listenStartAt;
    private long lastHandledId = -1L;

    public ScreenshotObserver(Context context, Listener listener) {
        appContext = context.getApplicationContext();
        this.listener = listener;
        observer = new ContentObserver(mainHandler) {
            @Override
            public void onChange(boolean selfChange) {
                scheduleQuery();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                scheduleQuery();
            }
        };
    }

    public void start() {
        if (registered) {
            return;
        }
        listenStartAt = System.currentTimeMillis();
        appContext.getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer);
        registered = true;
    }

    public void stop() {
        if (!registered) {
            return;
        }
        mainHandler.removeCallbacks(queryRunnable);
        appContext.getContentResolver().unregisterContentObserver(observer);
        registered = false;
    }

    private void scheduleQuery() {
        if (!registered) {
            return;
        }
        mainHandler.removeCallbacks(queryRunnable);
        mainHandler.postDelayed(queryRunnable, QUERY_DELAY_MS);
    }

    private void queryLatestImage() {
        Cursor cursor = null;
        try {
            cursor = appContext.getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    buildProjection(),
                    null,
                    null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            int checkedCount = 0;
            do {
                long id = getLong(cursor, MediaStore.Images.Media._ID);
                if (id == lastHandledId) {
                    return;
                }
                String displayName = getString(cursor, MediaStore.Images.Media.DISPLAY_NAME);
                String data = Build.VERSION.SDK_INT >= 29
                        ? "" : getString(cursor, MediaStore.Images.Media.DATA);
                String relativePath = Build.VERSION.SDK_INT >= 29
                        ? getString(cursor, MediaStore.Images.Media.RELATIVE_PATH) : "";
                String bucketName = getString(cursor, MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                long dateAdded = getLong(cursor, MediaStore.Images.Media.DATE_ADDED) * 1000L;
                long dateTaken = getLong(cursor, MediaStore.Images.Media.DATE_TAKEN);

                if (isLikelyScreenshot(displayName, data, relativePath, bucketName,
                        dateAdded, dateTaken)) {
                    lastHandledId = id;
                    listener.onScreenshotDetected();
                    return;
                }
                checkedCount++;
            } while (checkedCount < MAX_QUERY_COUNT && cursor.moveToNext());
        } catch (SecurityException ignored) {
            // Permission was revoked while foreground. The Activity handles downgrade messaging.
        } catch (RuntimeException ignored) {
            // Some vendors expose partial MediaStore columns. Ignore this event safely.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String[] buildProjection() {
        if (Build.VERSION.SDK_INT >= 29) {
            return new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            };
        }
        return new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };
    }

    private boolean isLikelyScreenshot(String displayName, String data, String relativePath,
                                       String bucketName, long dateAdded, long dateTaken) {
        String text = join(displayName, data, relativePath, bucketName).toLowerCase(Locale.US);
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        for (String keyword : SCREENSHOT_KEYWORDS) {
            if (text.contains(keyword)) {
                return isInListenWindow(dateAdded) || isInListenWindow(dateTaken)
                        || (dateAdded <= 0 && dateTaken <= 0);
            }
        }
        return false;
    }

    private boolean isInListenWindow(long imageTime) {
        if (imageTime <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        return imageTime >= listenStartAt - START_TOLERANCE_MS
                && imageTime <= now + FUTURE_TOLERANCE_MS;
    }

    private String join(String displayName, String data, String relativePath, String bucketName) {
        return safe(displayName) + " " + safe(data) + " "
                + safe(relativePath) + " " + safe(bucketName);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 && !cursor.isNull(index) ? cursor.getString(index) : "";
    }

    private long getLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        return index >= 0 && !cursor.isNull(index) ? cursor.getLong(index) : 0L;
    }
}
