package com.example.myapplication.loading;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.WeakHashMap;

/**
 * 全局单例：维护 Activity -&gt; {@link LoadingController} 的映射。
 *
 * <p>镜像项目现有的 {@code SequentialExecutorManager} 模式：{@link WeakHashMap} 防泄漏，
 * 主线程操作，{@link MyApplication} 在 Activity 销毁时调用 {@link #remove(Activity)} 拆除。
 */
public final class GlobalLoadingManager {

    private static GlobalLoadingManager instance;

    // WeakHashMap：即使漏调 remove()，Activity 被 GC 后条目也会自动消失。
    private final WeakHashMap<Activity, LoadingController> controllers = new WeakHashMap<>();

    private GlobalLoadingManager() {
    }

    public static synchronized GlobalLoadingManager get() {
        if (instance == null) {
            instance = new GlobalLoadingManager();
        }
        return instance;
    }

    /** 取得（必要时创建）绑定到该 Activity 的控制器。 */
    @MainThread
    LoadingController of(@NonNull Activity activity) {
        LoadingController controller = controllers.get(activity);
        if (controller == null) {
            controller = new LoadingController(activity);
            controllers.put(activity, controller);
        }
        return controller;
    }

    /** 由 {@code MyApplication.onActivityDestroyed} 调用：拆除并解绑该 Activity 的 loading。 */
    @MainThread
    public void remove(@NonNull Activity activity) {
        LoadingController controller = controllers.remove(activity);
        if (controller != null) {
            controller.destroy();
        }
    }

    /** 强制清零并关闭某个 Activity 的 loading（兜底用），控制器本身保留可复用。 */
    @MainThread
    void reset(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        LoadingController controller = controllers.get(activity);
        if (controller != null) {
            controller.destroy();
        }
    }
}
