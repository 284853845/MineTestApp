package com.example.myapplication.module.biz.profile;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class AssetMiniModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "profile_asset";

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
        return R.layout.item_profile_asset;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_asset_deposit))
                .setText(String.valueOf(data.get("deposit")));
        ((TextView) holder.getView(R.id.tv_asset_fund))
                .setText(String.valueOf(data.get("fund")));
        ((TextView) holder.getView(R.id.tv_asset_point))
                .setText(String.valueOf(data.get("point")));
    }
}
