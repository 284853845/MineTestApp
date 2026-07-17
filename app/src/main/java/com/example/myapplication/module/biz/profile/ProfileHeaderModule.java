package com.example.myapplication.module.biz.profile;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class ProfileHeaderModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "profile_header";

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
        return R.layout.item_profile_header;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_profile_name))
                .setText(String.valueOf(data.get("name")));
        ((TextView) holder.getView(R.id.tv_profile_level))
                .setText(String.valueOf(data.get("level")));
        ((TextView) holder.getView(R.id.tv_profile_id))
                .setText(String.valueOf(data.get("id")));
    }
}
