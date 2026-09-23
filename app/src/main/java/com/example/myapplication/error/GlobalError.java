package com.example.myapplication.error;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 全局错误弹窗（业务唯一入口）。
 *
 * <p>交易失败时调 {@link #show(Activity, CharSequence)} 弹出错误信息：
 * <pre>{@code
 * // 交易失败回调里：
 * GlobalLoading.hide(activity);        // 先关 loading
 * GlobalError.show(activity, "余额不足"); // 再弹错误
 * }</pre>
 *
 * <p>核心语义 —— <b>同一 Activity 上同时只显示一个错误弹窗</b>：当前已有错误弹窗显示时，
 * 后续的 {@link #show} 一律丢弃（不排队、不叠加），直到当前弹窗被关闭（点「确定」或返回键）后，
 * 新的错误才能再次弹出。用于「多笔交易同时报错」时避免弹窗糊屏。
 *
 * <p>其余特性对齐 {@code GlobalLoading}：按 Activity 隔离、{@code WeakHashMap} 防泄漏、
 * 页面销毁时自动关闭（见 {@code MyApplication}）。
 */
public final class GlobalError {

    private GlobalError() {
    }

    /** 弹出错误弹窗（默认标题）。已有错误弹窗显示时本次调用被丢弃。 */
    @MainThread
    public static void show(@NonNull Activity activity, @Nullable CharSequence message) {
        show(activity, null, message);
    }

    /** 弹出错误弹窗（自定义标题）。已有错误弹窗显示时本次调用被丢弃。 */
    @MainThread
    public static void show(@NonNull Activity activity,
                            @Nullable CharSequence title, @Nullable CharSequence message) {
        GlobalErrorManager.get().of(activity).show(title, message);
    }

    /** 强制关闭该 Activity 上的错误弹窗（兜底用），关闭后新的错误可再次弹出。 */
    @MainThread
    public static void dismiss(@Nullable Activity activity) {
        GlobalErrorManager.get().reset(activity);
    }
}
