package com.example.myapplication.sequential;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

/**
 * 绑定到单个 Activity 的顺序执行器：按 FIFO 顺序一次运行一个 {@link SequentialTask}，
 * 当前任务完成后才推进到下一个。所有方法都应在主线程调用。
 */
public final class SequentialExecutor {

    private final WeakReference<Activity> hostRef;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<SequentialTask> queue = new ArrayDeque<>();

    private SequentialTask running;
    private TokenCallback activeCallback;
    private boolean destroyed;

    SequentialExecutor(@NonNull Activity host) {
        this.hostRef = new WeakReference<>(host);
    }

    /** 入队一个任务；空闲时立即开始排空。销毁后或非主线程调用时静默丢弃。 */
    @MainThread
    public void enqueue(@NonNull SequentialTask task) {
        if (Looper.myLooper() != Looper.getMainLooper() || destroyed) {
            return;
        }
        queue.addLast(task);
        if (running == null) {
            drain();
        }
    }

    @MainThread
    private void drain() {
        if (destroyed || running != null) {
            return;
        }
        Activity host = hostRef.get();
        if (host == null || host.isFinishing() || host.isDestroyed()) {
            // 宿主已消失但 destroy() 尚未派发：自行清空
            clear();
            return;
        }
        SequentialTask task = queue.pollFirst();
        if (task == null) {
            return; // 空闲
        }
        running = task;
        TokenCallback cb = new TokenCallback();
        activeCallback = cb;
        // 可能同步完成（重入）——TokenCallback + post 推进已处理该情况。
        task.execute(host, cb);
    }

    @MainThread
    private void onTaskFinished(TokenCallback from) {
        if (destroyed || from != activeCallback) {
            return; // 陈旧回调：已清空或已被取代
        }
        running = null;
        activeCallback = null;
        mainHandler.post(this::drain); // post 而非递归，避免同步完成时栈溢出
    }

    /** 清空待执行任务并取消正在运行的任务。清空后仍可复用。 */
    @MainThread
    public void clear() {
        queue.clear();
        SequentialTask current = running;
        // 先失效再 cancel()，使 cancel() 内部触发的回调被忽略
        activeCallback = null;
        running = null;
        mainHandler.removeCallbacksAndMessages(null);
        if (current != null) {
            current.cancel();
        }
    }

    /** Activity 销毁时的永久拆除。 */
    @MainThread
    void destroy() {
        if (destroyed) {
            return;
        }
        clear();
        destroyed = true;
        hostRef.clear();
    }

    /** 一次性、幂等的完成回调；其自身身份即有效性令牌。 */
    private final class TokenCallback implements SequentialTask.Callback {
        private boolean used;

        @Override
        public void onComplete() {
            finishOnce();
        }

        @Override
        public void onCancel() {
            finishOnce();
        }

        private void finishOnce() {
            if (used) {
                return; // 防重复完成
            }
            used = true;
            onTaskFinished(this);
        }
    }
}
