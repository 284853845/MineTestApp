package com.example.myapplication.module.biz.home;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.List;
import java.util.Map;

public class QuickEntryModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "home_quick_entry";
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
        return R.layout.item_home_quick_entry;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        GridLayout grid = holder.getView(R.id.grid_entry);
        grid.removeAllViews();
        grid.setColumnCount(COLUMNS);

        final View demo = holder.getView(R.id.iv_demo_rounded);
        if (demo != null) {
            demo.setVisibility(View.VISIBLE);
            demo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(
                            v.getContext(),
                            "圆角图片示例被点击",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) data.get("entries");
        if (entries == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(grid.getContext());
        for (Map<String, Object> entry : entries) {
            View cell = inflater.inflate(R.layout.item_home_quick_entry_cell, grid, false);
            ImageView icon = cell.findViewById(R.id.iv_entry_icon);
            TextView name = cell.findViewById(R.id.tv_entry_name);
            bindIcon(icon, String.valueOf(entry.get("icon")));
            name.setText(String.valueOf(entry.get("name")));
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
