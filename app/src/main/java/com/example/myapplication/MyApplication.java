package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.myapplication.error.GlobalErrorManager;
import com.example.myapplication.loading.GlobalLoadingManager;
import com.example.myapplication.sequential.SequentialExecutorManager;

/**
 * 应用入口。通过 {@link Application.ActivityLifecycleCallbacks} 统一在 Activity 销毁时
 * 清理其顺序执行器、全局 loading 与全局错误弹窗，无需任何 Activity 继承特定基类。
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new SequentialLifecycleCallbacks());
    }

    /**
     * 只关心 onActivityDestroyed：页面销毁即移除并拆除该 Activity 的执行器、全局 loading 与错误弹窗，
     * 清空其待执行任务、取消当前正在展示的弹窗、关闭残留的 loading / 错误弹窗。其余回调无操作。
     */
    private static final class SequentialLifecycleCallbacks
            implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            SequentialExecutorManager.get().remove(activity);
            GlobalLoadingManager.get().remove(activity);
            GlobalErrorManager.get().remove(activity);
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }
    }
}
