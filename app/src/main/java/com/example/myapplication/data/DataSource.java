package com.example.myapplication.data;

import java.util.List;

/**
 * 数据源抽象。模块/页面只依赖这个接口,不关心底层是 Mock 还是真实网络。
 * 后续接真实接口时,只需提供一个新的实现类(如 RetrofitDataSource),
 * 上层代码不用改。
 *
 * 所有回调都保证回到主线程,调用方可直接刷新 UI。
 */
public interface DataSource {

    /** 页面配置回调:返回该页面按后端顺序排列的模块 key 列表 */
    interface ConfigCallback {
        void onResult(List<String> orderedModuleKeys);

        void onError(Throwable error);
    }

    /** 单个模块数据回调 */
    interface DataCallback {
        /** data 为该模块的数据;约定 data==null 表示"无数据",模块应隐藏 */
        void onResult(Object data);

        void onError(Throwable error);
    }

    /**
     * 拉取页面配置(模块顺序由后端决定)。
     *
     * @param pageId   页面标识,如 "home" / "wealth"
     * @param callback 结果回调(主线程)
     */
    void getPageConfig(String pageId, ConfigCallback callback);

    /**
     * 拉取单个模块的数据。每个模块在自己内部调用它发起请求。
     *
     * @param moduleKey 模块 key
     * @param isRefresh 是否由下拉刷新触发(实现可据此决定是否走缓存)
     * @param callback  结果回调(主线程)
     */
    void request(String moduleKey, boolean isRefresh, DataCallback callback);
}
