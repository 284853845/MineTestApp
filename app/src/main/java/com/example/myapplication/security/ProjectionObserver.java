package com.example.myapplication.security;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

/**
 * Best-effort projection/recording detector based on video routes and displays.
 */
public class ProjectionObserver {

    public interface Listener {
        void onProjectionDetected();
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final MediaRouter mediaRouter;
    private final DisplayManager displayManager;
    private final Runnable projectionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkProjectionState();
            if (registered) {
                mainHandler.postDelayed(this, PROJECTION_CHECK_INTERVAL_MS);
            }
        }
    };

    private final MediaRouter.Callback routeCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            checkProjectionState();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            checkProjectionState();
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            checkProjectionState();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            checkProjectionState();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            checkProjectionState();
        }

        @Override
        public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info,
                                   MediaRouter.RouteGroup group, int index) {
            checkProjectionState();
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info,
                                     MediaRouter.RouteGroup group) {
            checkProjectionState();
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo info) {
        }

        @Override
        public void onRoutePresentationDisplayChanged(MediaRouter router,
                                                      MediaRouter.RouteInfo info) {
            checkProjectionState();
        }
    };

    private final DisplayManager.DisplayListener displayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                    checkProjectionState();
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    checkProjectionState();
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    checkProjectionState();
                }
            };

    private boolean registered;
    private boolean projectionActive;

    private static final long PROJECTION_CHECK_INTERVAL_MS = 1000L;

    public ProjectionObserver(Context context, Listener listener) {
        appContext = context.getApplicationContext();
        this.listener = listener;
        mediaRouter = (MediaRouter) appContext.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        displayManager = (DisplayManager) appContext.getSystemService(Context.DISPLAY_SERVICE);
    }

    public void start() {
        if (registered) {
            return;
        }
        if (mediaRouter != null) {
            mediaRouter.addCallback(
                    MediaRouter.ROUTE_TYPE_LIVE_VIDEO,
                    routeCallback,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
                            | MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        }
        if (displayManager != null) {
            displayManager.registerDisplayListener(displayListener, mainHandler);
        }
        registered = true;
        checkProjectionState();
        mainHandler.postDelayed(projectionCheckRunnable, PROJECTION_CHECK_INTERVAL_MS);
    }

    public void stop() {
        if (!registered) {
            return;
        }
        if (mediaRouter != null) {
            mediaRouter.removeCallback(routeCallback);
        }
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
        mainHandler.removeCallbacks(projectionCheckRunnable);
        projectionActive = false;
        registered = false;
    }

    private void checkProjectionState() {
        if (!registered) {
            return;
        }
        boolean active = hasLiveVideoRoute() || hasExternalDisplay();
        if (active && !projectionActive) {
            listener.onProjectionDetected();
        }
        projectionActive = active;
    }

    private boolean hasLiveVideoRoute() {
        if (mediaRouter == null) {
            return false;
        }
        try {
            MediaRouter.RouteInfo route =
                    mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
            MediaRouter.RouteInfo defaultRoute = mediaRouter.getDefaultRoute();
            if (isProjectionRoute(route, defaultRoute, true)) {
                return true;
            }
            int routeCount = mediaRouter.getRouteCount();
            for (int i = 0; i < routeCount; i++) {
                if (isProjectionRoute(mediaRouter.getRouteAt(i), defaultRoute, false)) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isProjectionRoute(MediaRouter.RouteInfo route,
                                      MediaRouter.RouteInfo defaultRoute,
                                      boolean selectedRoute) {
        if (route == null || route == defaultRoute) {
            return false;
        }
        if ((route.getSupportedTypes() & MediaRouter.ROUTE_TYPE_LIVE_VIDEO) == 0) {
            return false;
        }
        if (route.getPresentationDisplay() != null) {
            return true;
        }
        return selectedRoute
                && (route.getPlaybackType() == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
                || route.getDeviceType() == MediaRouter.RouteInfo.DEVICE_TYPE_TV
                || route.isEnabled());
    }

    private boolean hasExternalDisplay() {
        if (displayManager == null) {
            return false;
        }
        Display[] displays = displayManager.getDisplays(
                DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (hasValidExternalDisplay(displays)) {
            return true;
        }
        displays = displayManager.getDisplays();
        return hasValidExternalDisplay(displays);
    }

    private boolean hasValidExternalDisplay(Display[] displays) {
        if (displays == null) {
            return false;
        }
        for (Display display : displays) {
            if (display != null
                    && display.isValid()
                    && display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                return true;
            }
        }
        return false;
    }
}
