package com.example.myapplication.sms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LatestSmsRepository {

    private static final String TAG = "LatestSmsRepository";
    private static final String LOG_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    public interface Callback {
        void onResult(LatestSmsState state);
    }

    private static final String[] PROJECTION = {
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT,
            Telephony.Sms.TYPE
    };
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final int MAX_MATCH_COUNT = 5;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public LatestSmsRepository(Context context) {
        appContext = context.getApplicationContext();
    }

    public void loadLatest(final Callback callback) {
        IO.execute(new Runnable() {
            @Override
            public void run() {
                final LatestSmsState state = queryLatestInboxSms();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(state);
                    }
                });
            }
        });
    }

    private LatestSmsState queryLatestInboxSms() {
        Cursor cursor = null;
        try {
            ContentResolver resolver = appContext.getContentResolver();
            cursor = resolver.query(Telephony.Sms.Inbox.CONTENT_URI, PROJECTION,
                    null, null, null);
            if (cursor == null) {
                Log.d(TAG, "inbox sms cursor is null");
                return LatestSmsState.empty();
            }

            List<LatestSms> allMessages = new ArrayList<>();
            while (cursor.moveToNext()) {
                LatestSms sms = new LatestSms(
                        "sms_inbox",
                        cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)),
                        safeString(cursor, Telephony.Sms.ADDRESS),
                        safeString(cursor, Telephony.Sms.BODY),
                        cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                logMessage(sms);
                allMessages.add(sms);
            }

            Collections.sort(allMessages, new Comparator<LatestSms>() {
                @Override
                public int compare(LatestSms left, LatestSms right) {
                    long rightTime = effectiveTime(right);
                    long leftTime = effectiveTime(left);
                    if (rightTime > leftTime) {
                        return 1;
                    }
                    if (rightTime < leftTime) {
                        return -1;
                    }
                    return Long.compare(right.id, left.id);
                }
            });

            List<LatestSms> messages = new ArrayList<>();
            int count = Math.min(MAX_MATCH_COUNT, allMessages.size());
            for (int i = 0; i < count; i++) {
                messages.add(allMessages.get(i));
            }
            if (messages.isEmpty()) {
                return LatestSmsState.empty();
            }
            return LatestSmsState.loaded(messages);
        } catch (Exception e) {
            Log.d(TAG, "query inbox sms failed", e);
            return LatestSmsState.error();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String safeString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return "";
        }
        return cursor.getString(index);
    }

    private static long effectiveTime(LatestSms sms) {
        return Math.max(sms.date, sms.dateSent);
    }

    private static void logMessage(LatestSms message) {
        Log.d(TAG, message.source + " id=" + message.id
                + ", address=" + message.address
                + ", date=" + formatLogTime(message.date)
                + ", dateSent=" + formatLogTime(message.dateSent)
                + ", type=" + message.type);
        Log.d(TAG, message.source + " body=" + message.body);
    }

    private static String formatLogTime(long timeMillis) {
        if (timeMillis <= 0L) {
            return "";
        }
        return new SimpleDateFormat(LOG_TIME_PATTERN, Locale.getDefault())
                .format(new Date(timeMillis));
    }
}
