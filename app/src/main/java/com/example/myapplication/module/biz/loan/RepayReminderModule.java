package com.example.myapplication.module.biz.loan;

import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.Map;

public class RepayReminderModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "loan_repay";

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
        return R.layout.item_loan_repay;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_repay_amount)).setText("¥ " + data.get("amount"));
        ((TextView) holder.getView(R.id.tv_repay_date))
                .setText("下期还款日 " + data.get("nextDate"));
        ((TextView) holder.getView(R.id.tv_repay_left))
                .setText("剩余 " + data.get("left"));
    }
}
