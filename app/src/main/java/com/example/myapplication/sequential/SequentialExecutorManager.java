package com.example.myapplication.sequential;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * 全局单例：维护 Activity -> {@link SequentialExecutor} 的映射，
 * 提供便捷入队、按 Activity 移除，以及一次性清除所有执行器的能力。
 */
public final class SequentialExecutorManager {

    private static SequentialExecutorManager instance;

    // WeakHashMap：即使没有显式 remove()，Activity 被 GC 后条目也会自动消失，避免泄漏。
    private final WeakHashMap<Activity, SequentialExecutor> executors = new WeakHashMap<>();

    private SequentialExecutorManager() {
    }

    public static synchronized SequentialExecutorManager get() {
        if (instance == null) {
            instance = new SequentialExecutorManager();
        }
        return instance;
    }

    /** 取得（必要时创建）绑定到该 Activity 的执行器。主线程调用。 */
    @MainThread
    public SequentialExecutor of(@NonNull Activity activity) {
        SequentialExecutor exec = executors.get(activity);
        if (exec == null) {
            exec = new SequentialExecutor(activity);
            executors.put(activity, exec);
        }
        return exec;
    }

    /** 便捷方法：直接向该 Activity 的执行器入队一个任务。 */
    @MainThread
    public void enqueue(@NonNull Activity activity, @NonNull SequentialTask task) {
        of(activity).enqueue(task);
    }

    /** 由 CaptureAwareActivity.onDestroy() 调用：拆除并解绑该 Activity 的执行器。 */
    @MainThread
    public void remove(@NonNull Activity activity) {
        SequentialExecutor exec = executors.remove(activity);
        if (exec != null) {
            exec.destroy();
        }
    }

    /** 清空每个 Activity 的队列并取消其正在运行的任务；执行器本身保留可复用。 */
    @MainThread
    public void clearAll() {
        List<SequentialExecutor> snapshot = new ArrayList<>(executors.values());
        for (SequentialExecutor exec : snapshot) {
            exec.clear();
        }
    }
}
