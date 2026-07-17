package com.example.myapplication.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * 2x4 桌面小组件。
 *
 * 左侧展示标题/数值/刷新；右侧 2x2 共 4 个菜单按钮，
 * 每个按钮指向哪个功能由 {@link WidgetPrefs} 配置，点击后打开 App 并切到对应 Tab。
 * 业务数据请替换 {@link #updateAppWidget} 中的占位逻辑。
 */
public class WideWidgetProvider extends AppWidgetProvider {

    /** 点击刷新按钮的自定义 action */
    private static final String ACTION_REFRESH =
            "com.example.myapplication.appwidget.ACTION_REFRESH_WIDE";

    /** 4 个菜单容器的 view id */
    private static final int[] MENU_CONTAINER_IDS = {
            R.id.widget_menu_1, R.id.widget_menu_2, R.id.widget_menu_3, R.id.widget_menu_4
    };
    /** 4 个菜单图标的 view id */
    private static final int[] MENU_ICON_IDS = {
            R.id.widget_menu_icon_1, R.id.widget_menu_icon_2,
            R.id.widget_menu_icon_3, R.id.widget_menu_icon_4
    };
    /** 4 个菜单文字的 view id */
    private static final int[] MENU_LABEL_IDS = {
            R.id.widget_menu_label_1, R.id.widget_menu_label_2,
            R.id.widget_menu_label_3, R.id.widget_menu_label_4
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            refreshAll(context);
        }
    }

    /** 刷新全部 2x4 组件实例，设置页保存后也会调用。 */
    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName name = new ComponentName(context, WideWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(name);
        WideWidgetProvider provider = new WideWidgetProvider();
        for (int id : ids) {
            provider.updateAppWidget(context, manager, id);
        }
    }

    /**
     * 渲染单个组件实例。后续自定义内容主要改这里。
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wide);

        // === 左侧占位数据，请替换为真实业务数据 ===
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        int value = 3000 + new Random().nextInt(9000);
        views.setTextViewText(R.id.widget_wide_value, String.format(Locale.getDefault(), "%,d", value));
        views.setTextViewText(R.id.widget_wide_subtitle, "更新于 " + time);
        // ==========================================

        // 左侧主体与刷新按钮
        views.setOnClickPendingIntent(R.id.widget_wide_root, buildOpenTabIntent(context, 100, 0));
        views.setOnClickPendingIntent(R.id.widget_wide_refresh, buildRefreshIntent(context));

        // 右侧 4 个菜单：按配置渲染图标/文字，并绑定跳转
        for (int slot = 0; slot < WidgetPrefs.SLOT_COUNT; slot++) {
            int funcId = WidgetPrefs.getSlot(context, slot);
            WidgetFunctions.Function func = WidgetFunctions.getById(funcId);
            views.setImageViewResource(MENU_ICON_IDS[slot], func.iconRes);
            views.setTextViewText(MENU_LABEL_IDS[slot], func.name);
            // requestCode 用 slot+1 区分，避免 PendingIntent 复用导致 extra 串掉
            views.setOnClickPendingIntent(
                    MENU_CONTAINER_IDS[slot],
                    buildOpenTabIntent(context, slot + 1, func.tabIndex));
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /** 打开 App 并切到指定 Tab 的 PendingIntent */
    private PendingIntent buildOpenTabIntent(Context context, int requestCode, int tabIndex) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tabIndex);
        return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** 刷新数据的 PendingIntent（广播给自己） */
    private PendingIntent buildRefreshIntent(Context context) {
        Intent intent = new Intent(context, WideWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
