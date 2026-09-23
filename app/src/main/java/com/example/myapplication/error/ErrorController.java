package com.example.myapplication.error;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import java.lang.ref.WeakReference;

/**
 * 绑定到单个 Activity 的错误弹窗控制器：同一时刻只维持一个错误弹窗。
 *
 * <p>核心是一个「显示中」闸门（{@link #dialog} 非空即视为显示中）：
 * <ul>
 *   <li>{@link #show(CharSequence, CharSequence)} 时若已有弹窗 → 直接丢弃本次调用；</li>
 *   <li>用户点「确定」或返回键关闭 → 通过 {@link DialogInterface.OnDismissListener}
 *       把 {@link #dialog} 置空，此后新的错误方可再次弹出；</li>
 *   <li>{@link #destroy()}（页面销毁/强制关闭）→ 先摘监听再 dismiss，避免关闭回调再触发逻辑。</li>
 * </ul>
 *
 * <p>仅在主线程操作弹窗；对外方法若不在主线程会自动 post 回主线程。
 * 用 {@link WeakReference} 持有 Activity，避免泄漏。
 */
final class ErrorController {

    private static final CharSequence DEFAULT_TITLE = "提示";
    private static final CharSequence DEFAULT_MESSAGE = "操作失败，请稍后重试";
    private static final CharSequence POSITIVE_TEXT = "确定";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @NonNull
    private final WeakReference<Activity> hostRef;

    /** 当前错误弹窗，显示中时非空（即「已有弹窗」闸门）。 */
    @Nullable
    private AlertDialog dialog;

    ErrorController(@NonNull Activity activity) {
        this.hostRef = new WeakReference<>(activity);
    }

    /**
     * 弹出错误弹窗。若当前已有错误弹窗显示，则本次调用被<b>丢弃</b>（不排队、不叠加）。
     */
    @MainThread
    void show(@Nullable CharSequence title, @Nullable CharSequence message) {
        runOnMain(() -> {
            // 闸门：已有错误弹窗显示时，后续一律丢弃。
            if (dialog != null) {
                return;
            }
            Activity activity = hostRef.get();
            // 关键守卫：Activity 正在结束/已销毁时绝不 show，否则会抛 BadTokenException。
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            AlertDialog d = new AlertDialog.Builder(activity)
                    .setTitle(TextUtils.isEmpty(title) ? DEFAULT_TITLE : title)
                    .setMessage(TextUtils.isEmpty(message) ? DEFAULT_MESSAGE : message)
                    .setPositiveButton(POSITIVE_TEXT, null)
                    .setCancelable(true)
                    .create();
            d.setCanceledOnTouchOutside(false);
            // 关闭（点确定/返回键/dismiss）后置空闸门，允许下一个错误再次弹出。
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface d) {
                    dialog = null;
                }
            });
            try {
                d.show();
                dialog = d;
            } catch (RuntimeException e) {
                // 守卫与 show 之间存在极小的竞态窗口，兜底防止崩溃。
                dialog = null;
            }
        });
    }

    /** Activity 销毁/强制关闭时拆除：关闭弹窗并置空闸门。 */
    @MainThread
    void destroy() {
        runOnMain(() -> {
            if (dialog == null) {
                return;
            }
            // 先摘监听，避免 dismiss 回调再把 dialog 置空（此处已手动处理）。
            dialog.setOnDismissListener(null);
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (RuntimeException ignored) {
                // Activity 已随窗口一起销毁时 dismiss 可能抛异常，忽略即可。
            }
            dialog = null;
        });
    }

    private void runOnMain(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }
}
