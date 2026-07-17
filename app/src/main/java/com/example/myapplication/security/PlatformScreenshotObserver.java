package com.example.myapplication.security;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

/**
 * Android 14+ screenshot callback wrapper.
 *
 * <p>The project still compiles with SDK 30, so this class uses reflection to call
 * Activity.registerScreenCaptureCallback when the runtime system supports it.</p>
 */
public class PlatformScreenshotObserver {

    public interface Listener {
        void onScreenshotDetected();
    }

    private static final int SDK_ANDROID_14 = 34;
    private static final String SCREEN_CAPTURE_CALLBACK_CLASS =
            "android.app.Activity$ScreenCaptureCallback";

    private final Activity activity;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor mainExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            mainHandler.post(command);
        }
    };

    private Object callback;
    private boolean registered;

    public PlatformScreenshotObserver(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void start() {
        if (registered || Build.VERSION.SDK_INT < SDK_ANDROID_14) {
            return;
        }
        try {
            Class<?> callbackClass = Class.forName(SCREEN_CAPTURE_CALLBACK_CLASS);
            callback = Proxy.newProxyInstance(
                    callbackClass.getClassLoader(),
                    new Class[]{callbackClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            if ("onScreenCaptured".equals(method.getName())) {
                                listener.onScreenshotDetected();
                            }
                            return null;
                        }
                    });
            Method registerMethod = Activity.class.getMethod(
                    "registerScreenCaptureCallback",
                    Executor.class,
                    callbackClass);
            registerMethod.invoke(activity, mainExecutor, callback);
            registered = true;
        } catch (ReflectiveOperationException ignored) {
            callback = null;
        } catch (RuntimeException ignored) {
            callback = null;
        }
    }

    public void stop() {
        if (!registered || callback == null || Build.VERSION.SDK_INT < SDK_ANDROID_14) {
            return;
        }
        try {
            Class<?> callbackClass = Class.forName(SCREEN_CAPTURE_CALLBACK_CLASS);
            Method unregisterMethod = Activity.class.getMethod(
                    "unregisterScreenCaptureCallback",
                    callbackClass);
            unregisterMethod.invoke(activity, callback);
        } catch (ReflectiveOperationException ignored) {
        } catch (RuntimeException ignored) {
        } finally {
            callback = null;
            registered = false;
        }
    }
}
