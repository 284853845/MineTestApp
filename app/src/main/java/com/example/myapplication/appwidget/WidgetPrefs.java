package com.example.myapplication.appwidget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 2x4 小组件右侧 4 个菜单槽位的配置存储。
 *
 * 每个槽位保存一个 {@link WidgetFunctions} 的功能 id。
 */
public final class WidgetPrefs {

    private static final String PREF_NAME = "widget_menu_config";
    private static final String KEY_SLOT_PREFIX = "menu_slot_";

    /** 菜单槽位数量 */
    public static final int SLOT_COUNT = 4;

    /** 默认配置：首页 / 财富 / 贷款 / 生活 */
    private static final int[] DEFAULT_SLOTS = {0, 1, 2, 3};

    private WidgetPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 读取某槽位配置的功能 id */
    public static int getSlot(Context context, int slotIndex) {
        int def = (slotIndex >= 0 && slotIndex < DEFAULT_SLOTS.length)
                ? DEFAULT_SLOTS[slotIndex] : 0;
        return prefs(context).getInt(KEY_SLOT_PREFIX + slotIndex, def);
    }

    /** 写入某槽位配置的功能 id */
    public static void setSlot(Context context, int slotIndex, int funcId) {
        prefs(context).edit().putInt(KEY_SLOT_PREFIX + slotIndex, funcId).apply();
    }
}
