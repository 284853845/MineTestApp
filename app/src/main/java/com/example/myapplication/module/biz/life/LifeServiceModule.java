package com.example.myapplication.module.biz.life;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.List;
import java.util.Map;

public class LifeServiceModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "life_service";
    private static final int COLUMNS = 4;

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
        return R.layout.item_life_service;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        GridLayout grid = holder.getView(R.id.grid_service);
        grid.removeAllViews();
        grid.setColumnCount(COLUMNS);
        List<Map<String, Object>> services =
                (List<Map<String, Object>>) data.get("services");
        if (services == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(grid.getContext());
        for (Map<String, Object> service : services) {
            View cell = inflater.inflate(R.layout.item_life_service_cell, grid, false);
            ImageView icon = cell.findViewById(R.id.iv_service_icon);
            TextView name = cell.findViewById(R.id.tv_service_name);
            bindIcon(icon, String.valueOf(service.get("icon")));
            name.setText(String.valueOf(service.get("name")));
            grid.addView(cell);
        }
    }

    private void bindIcon(ImageView icon, String iconName) {
        int resId = icon.getResources().getIdentifier(
                iconName, "drawable", icon.getContext().getPackageName());
        if (resId != 0) {
            icon.setImageResource(resId);
        } else {
            icon.setImageDrawable(null);
        }
    }
}
