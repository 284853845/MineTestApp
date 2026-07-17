package com.example.myapplication.security;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

/**
 * Base Activity that monitors screenshots and projection while foreground.
 */
public abstract class CaptureAwareActivity extends AppCompatActivity {

    private static final int REQUEST_CAPTURE_IMAGE_PERMISSION = 4107;
    private static final String READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES";
    private static final String PREFS_NAME = "capture_security";
    private static final int PERMISSION_FLOW_VERSION = 2;
    private static final String KEY_PERMISSION_FLOW_VERSION = "permission_flow_version";
    private static final String KEY_PERMISSION_REQUESTED = "permission_requested";
    private static final String KEY_PERMISSION_WARNING_SHOWN = "permission_warning_shown";

    private CaptureSecurityMonitor captureSecurityMonitor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        captureSecurityMonitor = new CaptureSecurityMonitor(this);
        ensurePermissionPrefsVersion();
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean granted = hasScreenshotPermission();
        captureSecurityMonitor.start(granted);
        if (!granted) {
            requestScreenshotPermissionIfNeeded();
        }
    }

    @Override
    protected void onStop() {
        if (captureSecurityMonitor != null) {
            captureSecurityMonitor.stop();
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAPTURE_IMAGE_PERMISSION) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (captureSecurityMonitor != null) {
            captureSecurityMonitor.onScreenshotPermissionChanged(granted);
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_PERMISSION_WARNING_SHOWN, !granted)
                .apply();
    }

    private void requestScreenshotPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)) {
            notifyScreenshotPermissionDeniedOnce(prefs);
            return;
        }
        prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply();
        ActivityCompat.requestPermissions(
                this,
                new String[]{getScreenshotPermission()},
                REQUEST_CAPTURE_IMAGE_PERMISSION);
    }

    private void ensurePermissionPrefsVersion() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getInt(KEY_PERMISSION_FLOW_VERSION, 0) == PERMISSION_FLOW_VERSION) {
            return;
        }
        prefs.edit()
                .putInt(KEY_PERMISSION_FLOW_VERSION, PERMISSION_FLOW_VERSION)
                .putBoolean(KEY_PERMISSION_REQUESTED, false)
                .putBoolean(KEY_PERMISSION_WARNING_SHOWN, false)
                .apply();
    }

    private boolean hasScreenshotPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, getScreenshotPermission())
                == PackageManager.PERMISSION_GRANTED;
    }

    private String getScreenshotPermission() {
        return Build.VERSION.SDK_INT >= 33
                ? READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void notifyScreenshotPermissionDeniedOnce(SharedPreferences prefs) {
        if (prefs.getBoolean(KEY_PERMISSION_WARNING_SHOWN, false)) {
            return;
        }
        prefs.edit().putBoolean(KEY_PERMISSION_WARNING_SHOWN, true).apply();
        if (captureSecurityMonitor != null) {
            captureSecurityMonitor.notifyScreenshotPermissionDenied();
        }
    }
}
