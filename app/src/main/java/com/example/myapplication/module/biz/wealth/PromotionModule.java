package com.example.myapplication.module.biz.wealth;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class PromotionModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "wealth_promotion";

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
        return R.layout.item_wealth_promotion;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_promotion_title))
                .setText(String.valueOf(data.get("title")));
        ((TextView) holder.getView(R.id.tv_promotion_desc))
                .setText(String.valueOf(data.get("desc")));
    }
}
