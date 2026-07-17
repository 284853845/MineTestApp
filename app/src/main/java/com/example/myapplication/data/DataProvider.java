package com.example.myapplication.data;

/**
 * 全局共享的数据源持有者。整个 app 复用同一个 DataSource 实例。
 * 后续切换真实网络实现时,只需改这里的 new 即可。
 */
public final class DataProvider {

    private static DataSource instance;

    private DataProvider() {
    }

    public static synchronized DataSource get() {
        if (instance == null) {
            instance = new MockDataSource();
        }
        return instance;
    }
}
