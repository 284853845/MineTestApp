package com.example.myapplication.module.biz.wealth;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class HoldingSummaryModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "wealth_holding";

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
        return R.layout.item_wealth_holding;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_market_value))
                .setText(String.valueOf(data.get("marketValue")));
        ((TextView) holder.getView(R.id.tv_day_profit))
                .setText(String.valueOf(data.get("dayProfit")));
        ((TextView) holder.getView(R.id.tv_positions))
                .setText(String.valueOf(data.get("positions")));
    }
}
