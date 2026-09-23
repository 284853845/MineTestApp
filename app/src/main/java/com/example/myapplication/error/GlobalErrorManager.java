package com.example.myapplication.error;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.WeakHashMap;

/**
 * 全局单例：维护 Activity -&gt; {@link ErrorController} 的映射。
 *
 * <p>镜像 {@code GlobalLoadingManager}：{@link WeakHashMap} 防泄漏，主线程操作，
 * {@code MyApplication} 在 Activity 销毁时调用 {@link #remove(Activity)} 拆除。
 */
public final class GlobalErrorManager {

    private static GlobalErrorManager instance;

    // WeakHashMap：即使漏调 remove()，Activity 被 GC 后条目也会自动消失。
    private final WeakHashMap<Activity, ErrorController> controllers = new WeakHashMap<>();

    private GlobalErrorManager() {
    }

    public static synchronized GlobalErrorManager get() {
        if (instance == null) {
            instance = new GlobalErrorManager();
        }
        return instance;
    }

    /** 取得（必要时创建）绑定到该 Activity 的控制器。 */
    @MainThread
    ErrorController of(@NonNull Activity activity) {
        ErrorController controller = controllers.get(activity);
        if (controller == null) {
            controller = new ErrorController(activity);
            controllers.put(activity, controller);
        }
        return controller;
    }

    /** 由 {@code MyApplication.onActivityDestroyed} 调用：拆除并解绑该 Activity 的错误弹窗。 */
    @MainThread
    public void remove(@NonNull Activity activity) {
        ErrorController controller = controllers.remove(activity);
        if (controller != null) {
            controller.destroy();
        }
    }

    /** 强制关闭某个 Activity 的错误弹窗（兜底用），控制器本身保留可复用。 */
    @MainThread
    void reset(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        ErrorController controller = controllers.get(activity);
        if (controller != null) {
            controller.destroy();
        }
    }
}
