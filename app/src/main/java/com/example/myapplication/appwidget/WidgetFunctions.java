package com.example.myapplication.appwidget;

import com.example.myapplication.R;

/**
 * 桌面小组件菜单可指向的「功能」定义。
 *
 * 目前对应 App 的 5 个主 Tab（首页/财富/贷款/生活/我的）。
 * 后续要新增功能：在 {@link #ALL} 数组里加一项即可，
 * tabIndex 为 {@link com.example.myapplication.MainActivity} 中的 Tab 下标。
 */
public final class WidgetFunctions {

    /** 单个功能项 */
    public static final class Function {
        public final int id;          // 唯一 id，存储用，请勿随意改动已有值
        public final String name;     // 显示名
        public final int iconRes;     // 图标资源
        public final int tabIndex;    // 打开 App 后切到的 Tab 下标

        Function(int id, String name, int iconRes, int tabIndex) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
            this.tabIndex = tabIndex;
        }
    }

    /** 所有可选功能。id 与数组下标一致，方便存取。 */
    public static final Function[] ALL = new Function[]{
            new Function(0, "首页", R.drawable.ic_tab_home, 0),
            new Function(1, "财富", R.drawable.ic_tab_wealth, 1),
            new Function(2, "贷款", R.drawable.ic_tab_loan, 2),
            new Function(3, "生活", R.drawable.ic_tab_life, 3),
            new Function(4, "我的", R.drawable.ic_tab_profile, 4),
    };

    private WidgetFunctions() {
    }

    public static int count() {
        return ALL.length;
    }

    /** 按 id 取功能；越界则回退到第一个，避免空指针。 */
    public static Function getById(int id) {
        if (id < 0 || id >= ALL.length) {
            return ALL[0];
        }
        return ALL[id];
    }

    /** 所有功能显示名，供设置页单选框使用。 */
    public static String[] names() {
        String[] names = new String[ALL.length];
        for (int i = 0; i < ALL.length; i++) {
            names[i] = ALL[i].name;
        }
        return names;
    }
}
