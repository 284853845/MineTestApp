package com.example.myapplication.loading;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.myapplication.R;

import java.lang.ref.WeakReference;

/**
 * 绑定到单个 Activity 的 loading 控制器：用引用计数聚合该页面上的并发交易，
 * 首笔交易开始时（0 -&gt; 1）弹出全屏 loading 弹窗，末笔交易结束时（-&gt; 0）关闭弹窗。
 *
 * <p>用 {@link Dialog} 实现，相比往 content 里加 View 的方式：弹窗是独立 Window，
 * {@code setCancelable(false)} 天然吞掉返回键与外部点击，实现真正的阻塞式 loading。
 * 但也带来 Dialog 特有的坑，本类逐一处理：
 * <ul>
 *   <li>Activity 正在结束/已销毁时 {@code show} 会抛 {@code BadTokenException}
 *       —— 用 {@link Activity#isFinishing()}/{@link Activity#isDestroyed()} 守卫 + try/catch 兜底；</li>
 *   <li>Activity 销毁前未 dismiss 会「leaked window」崩溃
 *       —— {@link #destroy()} 由 {@code MyApplication} 在 onActivityDestroyed 时调用，强制关闭；</li>
 *   <li>dismiss 发生在窗口已随 Activity 销毁之后可能抛异常 —— 统一 try/catch 忽略。</li>
 * </ul>
 *
 * <p>仅在主线程操作弹窗；对外方法若不在主线程会自动 post 回主线程。
 * 用 {@link WeakReference} 持有 Activity，避免泄漏。
 *
 * <p><b>延时关闭（防闪烁）</b>：计数归零时不立即关闭，而是延时 {@link #DISMISS_DELAY_MS} 毫秒再关；
 * 若延时窗口内又有新交易 {@link #show(CharSequence)} 进来，则取消这次关闭、直接复用当前弹窗。
 * 用于「上一笔关闭后立刻发起下一笔」时保持弹窗不间断，避免一关一开的闪烁。
 */
final class LoadingController {

    /** 弹窗背后遮罩的变暗程度，0f 全透明、1f 全黑；loading 场景取一个轻微值即可。 */
    private static final float DIM_AMOUNT = 0.2f;

    /**
     * 计数归零后延时关闭的毫秒数（防闪烁）。取值原则：够短，让「交易真正结束」时多出的
     * loading 时长用户无感；又够长，能桥接「上一笔关闭后立刻发起下一笔」之间的极短间隙。
     * 若连续交易之间偶有几十~上百毫秒间隙导致仍闪，可上调到 150~200。
     */
    private static final long DISMISS_DELAY_MS = 100L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @NonNull
    private final WeakReference<Activity> hostRef;

    /** 当前未结束的交易数量。 */
    private int refCount;

    /** 复用的 loading 弹窗，展示时非空。 */
    @Nullable
    private Dialog dialog;

    /** 计数归零后的延时关闭动作；被新交易 show 取消（防闪烁）。 */
    private final Runnable delayedDismiss = new Runnable() {
        @Override
        public void run() {
            // 延时到点时若仍无进行中的交易，才真正关闭。
            if (refCount <= 0) {
                dismissDialog();
            }
        }
    };

    LoadingController(@NonNull Activity activity) {
        this.hostRef = new WeakReference<>(activity);
    }

    /**
     * 开始一笔交易：计数加一，首笔时（0 -&gt; 1）弹出弹窗。
     */
    @MainThread
    void show(@Nullable CharSequence message) {
        runOnMain(() -> {
            // 有新交易进来，先取消可能待执行的延时关闭，保持弹窗不间断。
            mainHandler.removeCallbacks(delayedDismiss);
            refCount++;
            if (refCount == 1) {
                showDialog(message);
            } else if (!TextUtils.isEmpty(message)) {
                updateMessage(message);
            }
        });
    }

    /** 结束一笔交易：计数减一，归零时延时关闭弹窗。多减（计数已为 0）时安全忽略。 */
    @MainThread
    void hide() {
        runOnMain(() -> {
            if (refCount <= 0) {
                return;
            }
            refCount--;
            if (refCount <= 0) {
                refCount = 0;
                scheduleDismiss();
            }
        });
    }

    /** Activity 销毁时强制拆除：立即关闭弹窗、计数清零（销毁不能再延时，否则会 leaked window）。 */
    @MainThread
    void destroy() {
        runOnMain(() -> {
            mainHandler.removeCallbacks(delayedDismiss);
            refCount = 0;
            dismissDialog();
        });
    }

    @MainThread
    private void showDialog(@Nullable CharSequence message) {
        Activity activity = hostRef.get();
        // 关键守卫：Activity 正在结束/已销毁时绝不 show，否则 Dialog 会抛 BadTokenException。
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            refCount = 0;
            return;
        }
        if (dialog != null) {
            // 弹窗还在（上一笔的延时关闭尚未到点），直接复用，必要时更新文案，避免一关一开的闪烁。
            if (!TextUtils.isEmpty(message)) {
                updateMessage(message);
            }
            return;
        }
        Dialog d = new Dialog(activity);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.view_global_loading);
        // 不可取消：吞掉返回键与外部点击，实现真正的阻塞式 loading。
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        configureWindow(d.getWindow());
        if (!TextUtils.isEmpty(message)) {
            TextView text = d.findViewById(R.id.global_loading_text);
            if (text != null) {
                text.setText(message);
            }
        }
        try {
            d.show();
            dialog = d;
        } catch (RuntimeException e) {
            // 守卫与 show 之间存在极小的竞态窗口，兜底防止崩溃。
            dialog = null;
            refCount = 0;
        }
    }

    /** 弹窗窗口：铺满全屏、背景透明（只露出中间胶囊），背后加一层轻微变暗遮罩。 */
    private static void configureWindow(@Nullable Window window) {
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = DIM_AMOUNT;
        window.setAttributes(lp);
    }

    @MainThread
    private void updateMessage(@NonNull CharSequence message) {
        if (dialog == null) {
            return;
        }
        TextView text = dialog.findViewById(R.id.global_loading_text);
        if (text != null) {
            text.setText(message);
        }
    }

    /** 计数归零时调用：延时关闭弹窗，给「立刻发起的下一笔交易」留出复用窗口，防闪烁。 */
    @MainThread
    private void scheduleDismiss() {
        if (dialog == null) {
            return;
        }
        mainHandler.removeCallbacks(delayedDismiss);
        mainHandler.postDelayed(delayedDismiss, DISMISS_DELAY_MS);
    }

    @MainThread
    private void dismissDialog() {
        if (dialog == null) {
            return;
        }
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (RuntimeException ignored) {
            // Activity 已随窗口一起销毁时 dismiss 可能抛异常，忽略即可。
        }
        dialog = null;
    }

    private void runOnMain(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }
}
