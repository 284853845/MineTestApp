package com.example.myapplication.module.biz.loan;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.List;
import java.util.Map;

public class LoanProductModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "loan_product";

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
        return R.layout.item_loan_product;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        LinearLayout container = holder.getView(R.id.ll_product_rows);
        container.removeAllViews();
        List<Map<String, Object>> products =
                (List<Map<String, Object>>) data.get("products");
        if (products == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (Map<String, Object> product : products) {
            View row = inflater.inflate(R.layout.item_loan_product_row, container, false);
            ((TextView) row.findViewById(R.id.tv_product_name))
                    .setText(String.valueOf(product.get("name")));
            ((TextView) row.findViewById(R.id.tv_product_desc))
                    .setText(String.valueOf(product.get("desc")));
            ((TextView) row.findViewById(R.id.tv_product_rate))
                    .setText(String.valueOf(product.get("rate")));
            container.addView(row);
        }
    }
}
