package com.example.myapplication.module.core;

import android.util.Log;

import com.example.myapplication.data.DataSource;
import com.example.myapplication.data.DiskCache;
import com.example.myapplication.list.core.ModuleItem;

import java.util.Collections;
import java.util.List;

/**
 * 最常见的模块形态:整个模块就是列表里的一张卡片(一个 item)。
 * 卡片内部的复杂内容(多行、宫格、横滑等)在各自 delegate 的 onBind 里填充。
 *
 * 子类只需:
 *  - 解析数据(重写 {@link #parse(Object)} 缓存到字段)
 *  - 提供卡片 item(重写 {@link #createCardItem()})
 *  - 提供 delegate(重写 {@link #registerDelegates})
 *
 * 取数策略(缓存优先 + 网络刷新,cache-then-network):
 *  1. 首次进入(非刷新):先异步读磁盘缓存,命中则立即渲染;同时发网络请求。
 *  2. 网络成功:写回磁盘缓存,再次渲染(最新覆盖旧缓存)。
 *  3. 防竞态:网络若先于缓存返回,则丢弃随后到达的旧缓存,不回退。
 *  4. 下拉刷新:跳过读缓存(刷新即要最新),成功后仍写回缓存。
 *
 * @param <T> 解析后的业务数据类型
 */
public abstract class SingleCardModule<T> extends Module {

    private static final String TAG = "ModuleCache";

    private T data;

    /** 标记本轮加载网络是否已返回,用于缓存/网络的竞态裁决 */
    private boolean networkReturned;
    /** 本轮是否已用缓存渲染过(影响网络失败时是否回退到错误态) */
    private boolean renderedFromCache;

    /**
     * 是否启用磁盘缓存。默认 true。
     * 对实时性极强、缓存无意义的模块可重写为 false。
     */
    protected boolean isCacheEnabled() {
        return true;
    }

    @Override
    protected void loadData(final boolean isRefresh) {
        networkReturned = false;
        renderedFromCache = false;

        // 1) 非刷新且开启缓存:先读缓存,命中立即渲染
        if (!isRefresh && isCacheEnabled()) {
            getCache().read(getKey(), new DiskCache.ReadCallback() {
                @Override
                public void onRead(Object cached) {
                    // 网络已先回 / 无缓存 -> 不处理,避免旧数据覆盖新数据
                    if (networkReturned || cached == null) {
                        return;
                    }
                    data = parse(cached);
                    if (data != null) {
                        renderedFromCache = true;
                        Log.d(TAG, getKey() + " 命中缓存,先行渲染");
                        // 缓存命中先展示;状态置为 LOADED 并触发重建
                        onDataLoaded(data);
                    }
                }
            });
        }

        // 2) 始终发起网络请求拿最新数据
        getDataSource().request(getKey(), isRefresh, new DataSource.DataCallback() {
            @Override
            public void onResult(Object raw) {
                networkReturned = true;
                data = (raw == null) ? null : parse(raw);
                // 3) 写回缓存(有数据才写)
                if (raw != null && isCacheEnabled()) {
                    getCache().write(getKey(), raw);
                    Log.d(TAG, getKey() + " 网络成功,刷新并写回缓存");
                }
                onDataLoaded(data);
            }

            @Override
            public void onError(Throwable error) {
                networkReturned = true;
                // 已用缓存渲染过则保留缓存内容,不退化为错误态
                if (renderedFromCache && data != null) {
                    Log.d(TAG, getKey() + " 网络失败,保留已展示的缓存数据");
                } else {
                    onLoadError(error);
                }
            }
        });
    }

    /** 当前已解析的数据,buildItems / delegate 可用 */
    protected T getData() {
        return data;
    }

    protected final void dispatchDataLoaded(T loadedData) {
        data = loadedData;
        onDataLoaded(data);
    }

    /** 把数据源返回的原始对象解析成业务类型 */
    protected abstract T parse(Object raw);

    /** 产出本模块那一张卡片对应的 item;返回 null 表示无内容 */
    protected abstract ModuleItem createCardItem();

    @Override
    public List<ModuleItem> buildItems() {
        if (data == null) {
            return Collections.emptyList();
        }
        ModuleItem card = createCardItem();
        if (card == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(card);
    }
}
