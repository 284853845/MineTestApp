package com.example.myapplication.security;

import android.app.Activity;
import android.content.Context;

/**
 * Coordinates screenshot and projection monitoring while an Activity is foreground.
 */
public class CaptureSecurityMonitor {

    private final CaptureWarningNotifier notifier;
    private final PlatformScreenshotObserver platformScreenshotObserver;
    private final ScreenshotObserver screenshotObserver;
    private final ProjectionObserver projectionObserver;

    private boolean started;
    private boolean platformScreenshotStarted;
    private boolean screenshotStarted;

    public CaptureSecurityMonitor(Activity activity) {
        Context appContext = activity.getApplicationContext();
        notifier = new CaptureWarningNotifier(appContext);
        platformScreenshotObserver = new PlatformScreenshotObserver(activity,
                new PlatformScreenshotObserver.Listener() {
                    @Override
                    public void onScreenshotDetected() {
                        notifier.notifyScreenshotDetected();
                    }
                });
        screenshotObserver = new ScreenshotObserver(appContext, new ScreenshotObserver.Listener() {
            @Override
            public void onScreenshotDetected() {
                notifier.notifyScreenshotDetected();
            }
        });
        projectionObserver = new ProjectionObserver(appContext, new ProjectionObserver.Listener() {
            @Override
            public void onProjectionDetected() {
                notifier.notifyProjectionDetected();
            }
        });
    }

    public void start(boolean screenshotPermissionGranted) {
        if (!started) {
            startPlatformScreenshotObserver();
            projectionObserver.start();
            started = true;
        }
        if (screenshotPermissionGranted) {
            startScreenshotObserver();
        } else {
            stopScreenshotObserver();
        }
    }

    public void stop() {
        stopScreenshotObserver();
        if (started) {
            projectionObserver.stop();
            stopPlatformScreenshotObserver();
            started = false;
        }
    }

    public void onScreenshotPermissionChanged(boolean granted) {
        if (!started) {
            return;
        }
        if (granted) {
            startScreenshotObserver();
        } else {
            stopScreenshotObserver();
            notifier.notifyScreenshotPermissionDenied();
        }
    }

    public void notifyScreenshotPermissionDenied() {
        notifier.notifyScreenshotPermissionDenied();
    }

    private void startPlatformScreenshotObserver() {
        if (platformScreenshotStarted) {
            return;
        }
        platformScreenshotObserver.start();
        platformScreenshotStarted = true;
    }

    private void stopPlatformScreenshotObserver() {
        if (!platformScreenshotStarted) {
            return;
        }
        platformScreenshotObserver.stop();
        platformScreenshotStarted = false;
    }

    private void startScreenshotObserver() {
        if (screenshotStarted) {
            return;
        }
        screenshotObserver.start();
        screenshotStarted = true;
    }

    private void stopScreenshotObserver() {
        if (!screenshotStarted) {
            return;
        }
        screenshotObserver.stop();
        screenshotStarted = false;
    }
}
