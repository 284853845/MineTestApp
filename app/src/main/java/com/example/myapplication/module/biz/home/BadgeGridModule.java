package com.example.myapplication.module.biz.home;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;
import com.example.myapplication.widget.BadgeRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BadgeGridModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "home_badge_grid";
    private static final int SPAN = 5;

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
        return R.layout.item_home_badge_grid;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        BadgeRecyclerView rv = holder.getView(R.id.rv_badge_grid);
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) data.get("entries");
        final List<Map<String, Object>> entries =
                (list == null) ? new ArrayList<Map<String, Object>>() : list;

        if (!(rv.getLayoutManager() instanceof GridLayoutManager)) {
            rv.setLayoutManager(new GridLayoutManager(rv.getContext(), SPAN));
            rv.setNestedScrollingEnabled(false);
        }
        rv.setAdapter(new GridAdapter(entries));
        rv.setBadgeAnchorId(R.id.iv_icon);
        rv.setBadgeProvider(new BadgeRecyclerView.BadgeProvider() {
            @Override
            public View onCreateBadge(int position, BadgeRecyclerView parent) {
                Object badge = entries.get(position).get("badge");
                if (badge == null || String.valueOf(badge).isEmpty()) {
                    return null;
                }
                TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_badge_text, parent, false);
                textView.setText(String.valueOf(badge));
                return textView;
            }
        });
    }

    static class GridAdapter extends RecyclerView.Adapter<GridAdapter.VH> {

        private final List<Map<String, Object>> data;

        GridAdapter(List<Map<String, Object>> data) {
            this.data = data;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_badge_grid_cell, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            Map<String, Object> entry = data.get(position);
            Context ctx = h.itemView.getContext();
            int resId = ctx.getResources().getIdentifier(
                    String.valueOf(entry.get("icon")), "drawable", ctx.getPackageName());
            if (resId != 0) {
                h.icon.setImageResource(resId);
            } else {
                h.icon.setImageDrawable(null);
            }
            h.name.setText(String.valueOf(entry.get("name")));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;

            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.iv_icon);
                name = v.findViewById(R.id.tv_name);
            }
        }
    }
}
