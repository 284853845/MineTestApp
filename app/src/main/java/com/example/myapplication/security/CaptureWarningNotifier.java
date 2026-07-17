package com.example.myapplication.security;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Shows capture warnings with throttling so one system event does not spam Toasts.
 */
public class CaptureWarningNotifier {

    private static final long WARNING_INTERVAL_MS = 3000L;

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private long lastScreenshotWarningAt;
    private long lastProjectionWarningAt;
    private long lastPermissionWarningAt;

    public CaptureWarningNotifier(Context context) {
        appContext = context.getApplicationContext();
    }

    public void notifyScreenshotDetected() {
        showThrottled(
                "检测到截屏操作，请注意保护账户与资产信息",
                WarningType.SCREENSHOT);
    }

    public void notifyProjectionDetected() {
        showThrottled(
                "检测到投屏或录屏环境，请注意保护账户与资产信息",
                WarningType.PROJECTION);
    }

    public void notifyScreenshotPermissionDenied() {
        showThrottled(
                "未授予图片读取权限，无法准确监听截屏",
                WarningType.PERMISSION);
    }

    private void showThrottled(final String message, WarningType type) {
        long now = System.currentTimeMillis();
        if (!shouldShow(now, type)) {
            return;
        }
        markShown(now, type);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean shouldShow(long now, WarningType type) {
        switch (type) {
            case SCREENSHOT:
                return now - lastScreenshotWarningAt >= WARNING_INTERVAL_MS;
            case PROJECTION:
                return now - lastProjectionWarningAt >= WARNING_INTERVAL_MS;
            case PERMISSION:
            default:
                return now - lastPermissionWarningAt >= WARNING_INTERVAL_MS;
        }
    }

    private void markShown(long now, WarningType type) {
        switch (type) {
            case SCREENSHOT:
                lastScreenshotWarningAt = now;
                break;
            case PROJECTION:
                lastProjectionWarningAt = now;
                break;
            case PERMISSION:
            default:
                lastPermissionWarningAt = now;
                break;
        }
    }

    private enum WarningType {
        SCREENSHOT,
        PROJECTION,
        PERMISSION
    }
}
