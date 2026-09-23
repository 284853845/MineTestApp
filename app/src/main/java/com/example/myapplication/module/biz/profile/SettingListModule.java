package com.example.myapplication.module.biz.profile;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.AudioCompressionActivity;
import com.example.myapplication.DemoActivity;
import com.example.myapplication.ItemComponentDemoActivity;
import com.example.myapplication.R;
import com.example.myapplication.appwidget.WidgetSettingsActivity;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.List;
import java.util.Map;

public class SettingListModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "profile_setting";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parse(Object raw) {
        return (Map<String, Object>) raw;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.item_profile_setting;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        LinearLayout container = holder.getView(R.id.ll_setting_rows);
        container.removeAllViews();
        List<Map<String, Object>> settings =
                (List<Map<String, Object>>) data.get("settings");
        if (settings == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (Map<String, Object> setting : settings) {
            View row = inflater.inflate(R.layout.item_profile_setting_row, container, false);
            ImageView icon = row.findViewById(R.id.iv_setting_icon);
            int resId = icon.getResources().getIdentifier(
                    String.valueOf(setting.get("icon")),
                    "drawable",
                    icon.getContext().getPackageName());
            if (resId != 0) {
                icon.setImageResource(resId);
            } else {
                icon.setImageDrawable(null);
            }
            ((TextView) row.findViewById(R.id.tv_setting_name))
                    .setText(String.valueOf(setting.get("name")));

            final String action = setting.get("action") == null
                    ? null : String.valueOf(setting.get("action"));
            if (action != null) {
                row.setClickable(true);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        handleAction(view.getContext(), action);
                    }
                });
            }
            container.addView(row);
        }
    }

    private void handleAction(Context ctx, String action) {
        if ("widget_settings".equals(action)) {
            ctx.startActivity(new Intent(ctx, WidgetSettingsActivity.class));
        } else if ("audio_compression".equals(action)) {
            ctx.startActivity(new Intent(ctx, AudioCompressionActivity.class));
        } else if ("item_component_demo".equals(action)) {
            ctx.startActivity(new Intent(ctx, ItemComponentDemoActivity.class));
        } else if ("function_demo".equals(action)) {
            ctx.startActivity(new Intent(ctx, DemoActivity.class));
        }
    }
}
