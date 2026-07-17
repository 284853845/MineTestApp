package com.example.myapplication.module.biz.loan;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class CreditLimitModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "loan_credit";

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
        return R.layout.item_loan_credit;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_credit_available))
                .setText(String.valueOf(data.get("available")));
        ((TextView) holder.getView(R.id.tv_credit_total))
                .setText("总额度 " + data.get("total"));
        ((TextView) holder.getView(R.id.tv_credit_rate))
                .setText(String.valueOf(data.get("rate")));
    }
}
