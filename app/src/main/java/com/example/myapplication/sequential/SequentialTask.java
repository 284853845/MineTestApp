package com.example.myapplication.sequential;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

/**
 * 顺序执行器中的一个任务单元。同一时刻只有一个任务在运行，
 * 只有当 {@link Callback#onComplete()}（或 {@link Callback#onCancel()}）被调用后，
 * 执行器才会推进到下一个任务。所有方法都在主线程调用。
 */
public interface SequentialTask {

    /**
     * 启动任务（展示弹窗、启动页面等）。任务结束时必须且只能调用一次
     * {@code callback.onComplete()}，除非它先被 {@link #cancel()} 取消。
     *
     * @param host     宿主 Activity（非空，调用时保证未销毁）
     * @param callback 用于推进队列的完成信号
     */
    @MainThread
    void execute(@NonNull Activity host, @NonNull Callback callback);

    /**
     * 当执行器被清空（宿主 Activity 销毁，或调用了 clear()/clearAll()）
     * 且本任务正在运行时被调用。应在此关闭弹窗 / 释放资源。
     * cancel() 之后，本任务再发出的回调都会被执行器忽略。
     */
    @MainThread
    default void cancel() {
        // 默认无操作
    }

    /** 交给任务的一次性完成信号；执行器保证其幂等。 */
    interface Callback {

        /** 任务正常结束，推进队列。 */
        @MainThread
        void onComplete();

        /** 用户取消 / 退出，与 onComplete() 一样推进队列。 */
        @MainThread
        default void onCancel() {
            onComplete();
        }
    }
}
