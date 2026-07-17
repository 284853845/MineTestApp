package com.example.myapplication.data;

import android.content.Context;

/**
 * 全局共享的磁盘缓存持有者。整个 app 复用同一个 DiskCache 实例。
 */
public final class CacheProvider {

    private static DiskCache instance;

    private CacheProvider() {
    }

    public static synchronized DiskCache get(Context context) {
        if (instance == null) {
            instance = new DiskCache(context.getApplicationContext());
        }
        return instance;
    }
}
