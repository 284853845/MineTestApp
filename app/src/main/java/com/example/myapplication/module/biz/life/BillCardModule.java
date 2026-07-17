package com.example.myapplication.module.biz.life;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class BillCardModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "life_bill";

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
        return R.layout.item_life_bill;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_bill_month))
                .setText(String.valueOf(data.get("month")));
        ((TextView) holder.getView(R.id.tv_bill_total))
                .setText("¥ " + data.get("total"));
        ((TextView) holder.getView(R.id.tv_bill_items))
                .setText(String.valueOf(data.get("items")));
    }
}
