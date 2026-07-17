package com.example.myapplication.appwidget;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.security.CaptureAwareActivity;

/**
 * 桌面小组件设置页。
 *
 * 4 行分别对应 2x4 组件右侧 4 个菜单槽位，点击某行弹出单选框选择功能，
 * 选中后保存到 {@link WidgetPrefs} 并立即刷新桌面组件。
 */
public class WidgetSettingsActivity extends CaptureAwareActivity {

    private LinearLayout rowsContainer;
    // 每行的「当前功能名」TextView，便于选择后即时刷新文案
    private final TextView[] valueViews = new TextView[WidgetPrefs.SLOT_COUNT];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_settings);
        rowsContainer = findViewById(R.id.widget_settings_rows);
        buildRows();
    }

    private void buildRows() {
        rowsContainer.removeAllViews();
        for (int slot = 0; slot < WidgetPrefs.SLOT_COUNT; slot++) {
            rowsContainer.addView(buildRow(slot, slot == WidgetPrefs.SLOT_COUNT - 1));
        }
    }

    private View buildRow(final int slot, boolean last) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int v = dp(16);
        row.setPadding(0, v, 0, v);
        row.setClickable(true);

        // 左侧：菜单N
        TextView title = new TextView(this);
        title.setText(getString(R.string.widget_settings_menu_label, slot + 1));
        title.setTextSize(15);
        title.setTextColor(0xFF222222);
        title.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(title);

        // 右侧：当前功能名
        TextView value = new TextView(this);
        value.setTextSize(15);
        value.setTextColor(0xFF666666);
        value.setText(currentFunctionName(slot));
        row.addView(value);
        valueViews[slot] = value;

        // 箭头
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(20);
        arrow.setTextColor(0xFFCCCCCC);
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        arrowLp.leftMargin = dp(6);
        arrow.setLayoutParams(arrowLp);
        row.addView(arrow);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFunctionPicker(slot);
            }
        });
        return row;
    }

    /** 弹出单选框选择该槽位的功能 */
    private void showFunctionPicker(final int slot) {
        final String[] names = WidgetFunctions.names();
        int checked = WidgetPrefs.getSlot(this, slot);
        if (checked < 0 || checked >= names.length) {
            checked = 0;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.widget_settings_menu_label, slot + 1))
                .setSingleChoiceItems(names, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WidgetPrefs.setSlot(WidgetSettingsActivity.this, slot, which);
                        valueViews[slot].setText(names[which]);
                        // 立即刷新桌面上的 2x4 组件
                        WideWidgetProvider.refreshAll(getApplicationContext());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String currentFunctionName(int slot) {
        return WidgetFunctions.getById(WidgetPrefs.getSlot(this, slot)).name;
    }

    private int dp(int x) {
        return (int) (x * getResources().getDisplayMetrics().density + 0.5f);
    }
}
