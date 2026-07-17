package com.example.myapplication.module.core;

import android.content.Context;

import com.example.myapplication.data.DataSource;
import com.example.myapplication.data.DiskCache;

/**
 * 模块与所在页面之间的桥梁。模块通过它拿到 Context / 数据源,
 * 并在自己的数据变化后通知宿主重建列表。
 *
 * 由 ModuleManager 实现,模块不直接依赖具体页面。
 */
public interface ModuleHost {

    /** 提供给模块 inflate 布局、读资源等 */
    Context getContext();

    /** 模块通过它发起网络请求 */
    DataSource getDataSource();

    /** 模块通过它读写磁盘缓存 */
    DiskCache getCache();

    /**
     * 模块数据/状态发生变化后调用,请求宿主重新计算并刷新列表。
     * 重建会跳过无数据(EMPTY)模块,从而实现"无数据不展示"。
     */
    void requestRebuild();
}
