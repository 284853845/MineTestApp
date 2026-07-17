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
 * 2x2 桌面小组件。
 *
 * 展示一个标题、一个主数值和更新时间，底部有刷新按钮。
 * 点击组件主体会打开 App，点击刷新按钮会刷新数据。
 * 业务数据请替换 {@link #updateAppWidget} 中的占位逻辑。
 */
public class SmallWidgetProvider extends AppWidgetProvider {

    /** 点击刷新按钮的自定义 action */
    private static final String ACTION_REFRESH =
            "com.example.myapplication.appwidget.ACTION_REFRESH_SMALL";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 系统按周期或添加组件时回调，刷新所有实例
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // 处理刷新按钮点击
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName name = new ComponentName(context, SmallWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(name);
            for (int id : ids) {
                updateAppWidget(context, manager, id);
            }
        }
    }

    /**
     * 渲染单个组件实例。后续自定义内容主要改这里。
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_small);

        // === 占位数据，请替换为真实业务数据 ===
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Random random = new Random();
        int steps = 3000 + random.nextInt(9000);          // 模拟今日步数
        int percent = random.nextInt(30) - 5;             // 模拟同比涨跌
        String trend = (percent >= 0 ? "↑ 比昨日 +" : "↓ 比昨日 ") + percent + "%";
        views.setTextViewText(R.id.widget_small_value, String.format(Locale.getDefault(), "%,d", steps));
        views.setTextViewText(R.id.widget_small_trend, trend);
        views.setTextViewText(R.id.widget_small_subtitle, "更新于 " + time);
        // ====================================

        // 点击主体打开 App
        views.setOnClickPendingIntent(R.id.widget_small_root, buildOpenAppIntent(context));
        // 点击刷新按钮刷新
        views.setOnClickPendingIntent(R.id.widget_small_refresh, buildRefreshIntent(context));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /** 打开 App 主界面的 PendingIntent */
    private PendingIntent buildOpenAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** 刷新数据的 PendingIntent（广播给自己） */
    private PendingIntent buildRefreshIntent(Context context) {
        Intent intent = new Intent(context, SmallWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
