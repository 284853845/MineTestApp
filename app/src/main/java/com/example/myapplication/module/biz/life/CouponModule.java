package com.example.myapplication.module.biz.life;

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

public class CouponModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "life_coupon";
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
        Log.d(TAG, "优惠券模块曝光 -> 上报埋点");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parse(Object raw) {
        return (Map<String, Object>) raw;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.item_life_coupon;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        ((TextView) holder.getView(R.id.tv_coupon_title))
                .setText(String.valueOf(data.get("title")));
        LinearLayout container = holder.getView(R.id.ll_coupon_rows);
        container.removeAllViews();
        List<Map<String, Object>> coupons =
                (List<Map<String, Object>>) data.get("coupons");
        if (coupons == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (Map<String, Object> coupon : coupons) {
            View row = inflater.inflate(R.layout.item_life_coupon_row, container, false);
            ((TextView) row.findViewById(R.id.tv_coupon_row_title))
                    .setText(String.valueOf(coupon.get("title")));
            ((TextView) row.findViewById(R.id.tv_coupon_row_expire))
                    .setText(String.valueOf(coupon.get("expire")));
            container.addView(row);
        }
    }
}
