package com.example.myapplication.module.biz.home;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;

import java.util.List;
import java.util.Map;

public class TransactionListModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "home_transaction";
    private static final String TAG = "ModuleExpose";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public boolean isLazyLoad() {
        return true;
    }

    @Override
    public void onModuleExposed() {
        Log.d(TAG, "交易明细模块曝光 -> 上报埋点");
    }

    @Override
    public void onModuleHidden() {
        Log.d(TAG, "交易明细模块滚出可视区");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parse(Object raw) {
        return (Map<String, Object>) raw;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.item_home_transaction;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_tx_title))
                .setText(String.valueOf(data.get("title")));
        LinearLayout container = holder.getView(R.id.ll_tx_rows);
        container.removeAllViews();
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) data.get("rows");
        if (rows == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (Map<String, Object> row : rows) {
            View rowView = inflater.inflate(R.layout.item_home_transaction_row, container, false);
            ((TextView) rowView.findViewById(R.id.tv_tx_row_title))
                    .setText(String.valueOf(row.get("title")));
            ((TextView) rowView.findViewById(R.id.tv_tx_row_time))
                    .setText(String.valueOf(row.get("time")));
            TextView amount = rowView.findViewById(R.id.tv_tx_row_amount);
            boolean income = Boolean.TRUE.equals(row.get("income"));
            amount.setText(String.valueOf(row.get("amount")));
            amount.setTextColor(income ? 0xFFE53935 : 0xFF222222);
            container.addView(rowView);
        }
    }
}
