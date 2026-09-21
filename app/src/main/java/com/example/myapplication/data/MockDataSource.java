package com.example.myapplication.data;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mock 数据源:用线程池模拟网络请求耗时,结果切回主线程回调。
 * 写法与真实网络一致 —— 模块调用 {@link #request} 后在回调里拿数据,
 * 后续换成 Retrofit/OkHttp 实现只需替换本类。
 *
 * 约定:
 *  - getPageConfig 返回的 key 顺序即模块在页面中的展示顺序(后端驱动排序)
 *  - request 回调 data==null 表示该模块无数据(用于演示"无数据不展示")
 */
public class MockDataSource implements DataSource {

    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    public void getPageConfig(final String pageId, final ConfigCallback callback) {
        io.execute(new Runnable() {
            @Override
            public void run() {
                sleep(180);
                final List<String> keys = configFor(pageId);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(keys);
                    }
                });
            }
        });
    }

    @Override
    public void request(final String moduleKey, final boolean isRefresh,
                        final DataCallback callback) {
        io.execute(new Runnable() {
            @Override
            public void run() {
                sleep(300 + (long) (Math.random() * 500));
                final Object data = dataFor(moduleKey, isRefresh);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(data);
                    }
                });
            }
        });
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // ===== 页面配置:模块顺序由这里(模拟后端)决定 =====

    private List<String> configFor(String pageId) {
        switch (pageId) {
            case "home":
                return Arrays.asList(
                        "home_asset_overview", "home_latest_sms", "home_banner", "home_loop_banner",
                        "home_quick_entry",
                        "home_badge_grid", "home_transaction");
            case "wealth":
                return Arrays.asList(
                        "wealth_holding", "wealth_fund", "wealth_promotion");
            case "loan":
                return Arrays.asList(
                        "loan_credit", "loan_product", "loan_repay");
            case "life":
                return Arrays.asList(
                        "life_service", "life_bill", "life_coupon");
            case "profile":
                return Arrays.asList(
                        "profile_header", "profile_asset", "profile_setting");
            default:
                return new ArrayList<>();
        }
    }

    // ===== 各模块数据。约定返回 null 表示"无数据,模块隐藏" =====
    @SuppressWarnings("unchecked")
    private Object dataFor(String key, boolean isRefresh) {
        // 刷新时给个后缀,便于直观看到数据变化
        String tag = isRefresh ? "(已刷新)" : "";
        switch (key) {
            // ---------- 首页 ----------
            case "home_asset_overview":
                return map(
                        "total", isRefresh ? "1,286,540.88" : "1,280,326.50",
                        "profit", isRefresh ? "+2,140.30" : "+1,860.20",
                        "profitRate", isRefresh ? "+0.17%" : "+0.15%");
            case "home_banner":
                return map("banners", Arrays.asList(
                        map("title", "新人专享" + tag, "subtitle", "首投立减88元", "icon", "ic_tab_wealth"),
                        map("title", "限时优惠", "subtitle", "理财季精选好货", "icon", "ic_tab_home"),
                        map("title", "贷款福利", "subtitle", "年化低至3.85%", "icon", "ic_tab_loan"),
                        map("title", "生活缴费", "subtitle", "水电燃气一键搞定", "icon", "ic_tab_life")));
            case "home_loop_banner":
                return map("banners", Arrays.asList(
                        map("title", "Loop Banner A" + tag,
                                "subtitle", "Custom layout injected from XML.",
                                "icon", "ic_tab_home"),
                        map("title", "Loop Banner B",
                                "subtitle", "Built-in indicator and infinite scroll.",
                                "icon", "ic_tab_wealth"),
                        map("title", "Loop Banner C",
                                "subtitle", "Keeps running only when the module is visible.",
                                "icon", "ic_tab_loan"),
                        map("title", "Loop Banner D",
                                "subtitle", "Independent implementation without touching the old view.",
                                "icon", "ic_tab_life")));
            case "home_quick_entry":
                return map("entries", Arrays.asList(
                        map("name", "转账", "icon", "ic_tab_wealth"),
                        map("name", "信用卡", "icon", "ic_tab_loan"),
                        map("name", "理财", "icon", "ic_tab_home"),
                        map("name", "缴费", "icon", "ic_tab_life"),
                        map("name", "贷款", "icon", "ic_tab_loan"),
                        map("name", "基金", "icon", "ic_tab_wealth"),
                        map("name", "外汇", "icon", "ic_tab_setting"),
                        map("name", "更多", "icon", "ic_tab_discover")));
            case "home_badge_grid":
                return map("entries", Arrays.asList(
                        map("name", "转账", "icon", "ic_tab_wealth", "badge", "HOT"),
                        map("name", "信用卡", "icon", "ic_tab_loan", "badge", "限时5折"),
                        map("name", "理财", "icon", "ic_tab_home", "badge", "新人专享立减88"),
                        map("name", "缴费", "icon", "ic_tab_life"),
                        map("name", "贷款", "icon", "ic_tab_loan", "badge", "低至3.85%"),
                        map("name", "基金", "icon", "ic_tab_wealth"),
                        map("name", "外汇", "icon", "ic_tab_setting", "badge", "NEW"),
                        map("name", "更多", "icon", "ic_tab_discover")));
            case "home_transaction": {
                List<Object> rows = new ArrayList<>();
                String[] names = {"工资入账", "超市消费", "转账给张三", "话费充值",
                        "基金申购", "信用卡还款", "餐饮美食", "网购退款"};
                for (int i = 0; i < names.length; i++) {
                    boolean income = i == 0 || i == 7;
                    rows.add(map(
                            "title", names[i] + tag,
                            "time", "06-1" + (i % 6) + " 1" + i + ":30",
                            "amount", (income ? "+" : "-") + (100 + i * 137) + ".00",
                            "income", income));
                }
                return map("title", "交易明细", "rows", rows);
            }

            // ---------- 财富 ----------
            case "wealth_holding":
                return map(
                        "marketValue", isRefresh ? "568,920.00" : "562,310.00",
                        "dayProfit", isRefresh ? "+1,260.00" : "+980.00",
                        "positions", "12");
            case "wealth_fund":
                return map("funds", Arrays.asList(
                        map("name", "稳健增利债券A", "rate", "+5.62%", "tag", "低风险"),
                        map("name", "科技创新混合", "rate", "+18.30%", "tag", "中高风险"),
                        map("name", "沪深300指数", "rate", "+9.14%", "tag", "中风险"),
                        map("name", "黄金主题ETF", "rate", "+12.07%", "tag", "中风险")));
            case "wealth_promotion":
                // 故意返回 null:演示"无数据 -> 该模块整体不展示"
                return null;

            // ---------- 贷款 ----------
            case "loan_credit":
                return map(
                        "available", isRefresh ? "298,000" : "300,000",
                        "total", "300,000",
                        "rate", "年化 3.85% 起");
            case "loan_product":
                return map("products", Arrays.asList(
                        map("name", "工薪贷", "desc", "凭工资流水最高可借 50 万", "rate", "3.85%"),
                        map("name", "房抵贷", "desc", "房产抵押,额度高利率低", "rate", "3.45%"),
                        map("name", "经营贷", "desc", "企业主专享周转资金", "rate", "3.65%")));
            case "loan_repay":
                return map(
                        "nextDate", "2026-07-10",
                        "amount", "8,650.00",
                        "left", "23 期");

            // ---------- 生活 ----------
            case "life_service":
                return map("services", Arrays.asList(
                        map("name", "水费", "icon", "ic_tab_life"),
                        map("name", "电费", "icon", "ic_tab_life"),
                        map("name", "燃气", "icon", "ic_tab_life"),
                        map("name", "宽带", "icon", "ic_tab_discover"),
                        map("name", "电影", "icon", "ic_tab_home"),
                        map("name", "加油", "icon", "ic_tab_setting")));
            case "life_bill":
                return map(
                        "month", "6 月账单",
                        "total", isRefresh ? "4,236.50" : "4,180.00",
                        "items", "本月共 36 笔支出");
            case "life_coupon": {
                List<Object> coupons = new ArrayList<>();
                String[] titles = {"超市满100减20", "加油立减15元", "电影票9.9元起", "话费充值95折"};
                for (String t : titles) {
                    coupons.add(map("title", t + tag, "expire", "有效期至 06-30"));
                }
                return map("title", "我的优惠券", "coupons", coupons);
            }

            // ---------- 我的 ----------
            case "profile_header":
                return map(
                        "name", "尊享白金客户",
                        "level", isRefresh ? "钻石卡 VIP7" : "白金卡 VIP6",
                        "id", "6225 **** **** 8888");
            case "profile_asset":
                return map(
                        "deposit", "862,000",
                        "fund", "418,200",
                        "point", "26,800");
            case "profile_setting":
                return map("settings", Arrays.asList(
                        map("name", "桌面小组件设置", "icon", "ic_tab_setting", "action", "widget_settings"),
                        map("name", "音频压缩示例", "icon", "ic_tab_message", "action", "audio_compression"),
                        map("name", "账户安全", "icon", "ic_tab_setting"),
                        map("name", "我的银行卡", "icon", "ic_tab_loan"),
                        map("name", "消息通知", "icon", "ic_tab_message"),
                        map("name", "帮助中心", "icon", "ic_tab_discover"),
                        map("name", "关于我们", "icon", "ic_tab_profile"),
                        map("name", "自定义 Item 组件示例", "icon", "ic_tab_discover",
                                "action", "item_component_demo")));

            default:
                return null;
        }
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
