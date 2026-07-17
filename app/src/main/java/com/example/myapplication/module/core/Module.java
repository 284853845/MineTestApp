package com.example.myapplication.module.core;

import android.content.Context;

import com.example.myapplication.data.DataSource;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;

import java.util.List;

/**
 * 首页/各 Tab 列表里的"模块"抽象。每个业务模块继承它,做到:
 *  1. 自己发请求({@link #loadData})—— 数据请求在模块内发起
 *  2. 自己渲染({@link #buildItems} + {@link #registerDelegates})—— UI 解耦在各自文件
 *  3. 自己决定显隐 —— buildItems 返回空 / 状态为 EMPTY 时该模块整体不展示
 *  4. 感知页面生命周期 —— 重新可见 / 下拉刷新 / 滚动曝光(按需重写回调)
 *
 * 模块只关心"我的数据"和"我的 UI",顺序、拼装、事件分发由 {@link ModuleManager} 负责。
 */
public abstract class Module {

    /** 模块加载状态,决定 Manager 如何渲染它 */
    public enum State {
        /** 尚未加载(懒加载模块滚动到附近前停在此态) */
        NOT_LOADED,
        /** 请求进行中,展示骨架占位 */
        LOADING,
        /** 加载成功且有数据,展示真实内容 */
        LOADED,
        /** 加载成功但无数据,整体隐藏 */
        EMPTY,
        /** 加载失败,默认隐藏(也可重写 buildItems 展示错误态) */
        ERROR
    }

    private ModuleHost host;
    private State state = State.NOT_LOADED;
    /** 数据版本号:每次数据落定自增,供列表做增量 diff 的内容比对依据 */
    private long dataVersion = 0;

    /** 数据回来后通知 Manager 的回调,由 Manager 注入 */
    public interface LoadCallback {
        void onLoadFinished(Module module);
    }

    private LoadCallback loadCallback;

    void attach(ModuleHost host, LoadCallback callback) {
        this.host = host;
        this.loadCallback = callback;
    }

    protected Context getContext() {
        return host.getContext();
    }

    protected DataSource getDataSource() {
        return host.getDataSource();
    }

    protected com.example.myapplication.data.DiskCache getCache() {
        return host.getCache();
    }

    public State getState() {
        return state;
    }

    /** 当前数据版本号,内容每变化一次即不同 */
    public long getDataVersion() {
        return dataVersion;
    }

    /** 与后端配置对应的唯一 key */
    public abstract String getKey();

    /**
     * 是否懒加载:返回 true 时,Manager 先放骨架占位,
     * 等该模块滚动到接近可视区才调用 {@link #startLoad(boolean)}。
     * 默认 false,即进入页面立即加载。
     */
    public boolean isLazyLoad() {
        return false;
    }

    /** 向 adapter 注册本模块所需的渲染 delegate(可注册多个) */
    public abstract void registerDelegates(DelegateRegistry registry);

    /**
     * 根据当前已加载的数据产出列表项。
     * 返回空集合表示"无数据,不展示";有数据则按需返回 1 到多个 item。
     * 仅在 state==LOADED 时被 Manager 调用。
     */
    public abstract List<ModuleItem> buildItems();

    /**
     * 真正的取数逻辑,子类实现:调用 {@link #getDataSource()} 发请求,
     * 拿到数据后调用 {@link #onDataLoaded(Object)} 或 {@link #onLoadError(Throwable)}。
     *
     * @param isRefresh 是否由下拉刷新触发
     */
    protected abstract void loadData(boolean isRefresh);

    // ----- 状态流转:由 Manager 或子类回调驱动 -----

    /** Manager 触发加载入口(立即加载或懒加载就绪时) */
    public final void startLoad(boolean isRefresh) {
        state = State.LOADING;
        loadData(isRefresh);
    }

    /** 子类在数据返回后调用。data==null 视为无数据 -> EMPTY */
    protected final void onDataLoaded(Object data) {
        state = (data == null) ? State.EMPTY : State.LOADED;
        afterStateSettled();
        // LOADED 后还要再看 buildItems 是否为空,空也按隐藏处理
        if (state == State.LOADED && buildItems().isEmpty()) {
            state = State.EMPTY;
        }
        dataVersion++;
        notifyLoadFinished();
    }

    /** 子类在请求失败时调用 */
    protected final void onLoadError(Throwable error) {
        state = State.ERROR;
        dataVersion++;
        notifyLoadFinished();
    }

    /** 子类把原始数据解析、缓存到自己字段的钩子,onDataLoaded 内回调 */
    protected void afterStateSettled() {
    }

    private void notifyLoadFinished() {
        if (loadCallback != null) {
            loadCallback.onLoadFinished(this);
        }
    }

    // ----- 生命周期回调:默认空实现,模块按需重写 -----

    /** 页面(Tab)重新可见 */
    /** Module is attached to its host and delegate is registered. */
    public void onModuleCreated() {
    }

    /** Page Fragment onStart. */
    public void onPageStarted() {
    }

    /** Page Fragment onResume. */
    public void onPageResumed() {
    }

    /** Page Fragment onPause. */
    public void onPagePaused() {
    }

    /** Page Fragment onStop. */
    public void onPageStopped() {
    }

    /** Page view is destroyed; release long-lived module resources here. */
    public void onModuleDestroyed() {
    }

    public void onPageVisible() {
    }

    /** 页面(Tab)不可见 */
    public void onPageInvisible() {
    }

    /**
     * 下拉刷新。默认行为:重新发起一次加载。
     * 懒加载且尚未加载过的模块不会被刷新(由 Manager 过滤)。
     */
    public void onRefresh() {
        startLoad(true);
    }

    /** 本模块滚入可视区(曝光埋点入口) */
    public void onModuleExposed() {
    }

    /** 本模块滚出可视区 */
    public void onModuleHidden() {
    }

    /** 列表滚动中的实时回调 */
    public void onPageScroll(int dx, int dy) {
    }

    /** 注册 delegate 的轻量入口,避免模块直接持有 adapter */
    public interface DelegateRegistry {
        void register(ItemViewDelegate<?> delegate);
    }
}
