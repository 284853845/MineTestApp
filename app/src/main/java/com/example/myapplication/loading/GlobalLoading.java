package com.example.myapplication.loading;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * 全局 loading 工具类（业务唯一入口）。
 *
 * <p>显示与关闭完全隔离：交易开始处调 {@link #show(Activity)}，交易结束处（成功/失败/取消）
 * 调 {@link #hide(Activity)}，两端无需传递任何句柄：
 * <pre>{@code
 * GlobalLoading.show(activity);   // 交易开始
 * // ...发起异步交易...
 * // 回调里统一：
 * GlobalLoading.hide(activity);   // 交易结束
 * }</pre>
 *
 * <p>特性：
 * <ul>
 *   <li>按 Activity 隔离，引用计数聚合并发交易：多笔交易时每笔 show 需对应一次 hide，
 *       等最后一笔 hide 后才关闭弹窗；</li>
 *   <li>页面（Activity）销毁时其 loading 自动关闭（见 {@code MyApplication}）；</li>
 *   <li>{@code hide()} 在计数已为 0 时安全忽略，不会把计数减为负。</li>
 * </ul>
 *
 * <p>注意：{@link #show(Activity)} 与 {@link #hide(Activity)} 必须成对调用。若担心异常路径
 * 漏调 hide，请在 {@code finally} 或统一回调里调用；页面关闭时也会自动兜底清理。
 */
public final class GlobalLoading {

    private GlobalLoading() {
    }

    /** 在该 Activity 上开始一笔交易的 loading（计数 +1）。 */
    @MainThread
    public static void show(@NonNull Activity activity) {
        show(activity, null);
    }

    /** 在该 Activity 上开始一笔交易的 loading（计数 +1），并展示自定义文案。 */
    @MainThread
    public static void show(@NonNull Activity activity, @Nullable CharSequence message) {
        GlobalLoadingManager.get().of(activity).show(message);
    }

    /** 结束该 Activity 上的一笔交易的 loading（计数 -1，归零时关闭弹窗）。 */
    @MainThread
    public static void hide(@NonNull Activity activity) {
        GlobalLoadingManager.get().of(activity).hide();
    }

    /** 强制关闭该 Activity 上的 loading（清零计数）。兜底用，正常应通过 show/hide 成对调用。 */
    @MainThread
    public static void dismissAll(@Nullable Activity activity) {
        GlobalLoadingManager.get().reset(activity);
    }
}
