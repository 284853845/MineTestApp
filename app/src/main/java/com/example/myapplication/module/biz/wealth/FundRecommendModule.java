package com.example.myapplication.module.biz.wealth;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.List;
import java.util.Map;

public class FundRecommendModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "wealth_fund";

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
        return R.layout.item_wealth_fund;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        LinearLayout row = holder.getView(R.id.ll_fund_row);
        row.removeAllViews();
        List<Map<String, Object>> funds =
                (List<Map<String, Object>>) data.get("funds");
        if (funds == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(row.getContext());
        for (int i = 0; i < funds.size(); i++) {
            View card = inflater.inflate(R.layout.item_wealth_fund_card, row, false);
            Map<String, Object> fund = funds.get(i);
            ((TextView) card.findViewById(R.id.tv_fund_rate))
                    .setText(String.valueOf(fund.get("rate")));
            ((TextView) card.findViewById(R.id.tv_fund_name))
                    .setText(String.valueOf(fund.get("name")));
            ((TextView) card.findViewById(R.id.tv_fund_tag))
                    .setText(String.valueOf(fund.get("tag")));

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            lp.leftMargin = i == 0 ? 0 : dp(row, 10);
            card.setLayoutParams(lp);
            row.addView(card);
        }
    }

    private int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }
}
