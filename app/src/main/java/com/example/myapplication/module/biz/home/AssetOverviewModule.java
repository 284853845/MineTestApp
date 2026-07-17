package com.example.myapplication.module.biz.home;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.menu.MenuItem;
import com.example.myapplication.menu.MenuPopupWindow;
import com.example.myapplication.module.core.IndependentCardModule;
import com.example.myapplication.widget.CornerBadgeHelper;
import com.example.myapplication.widget.MoneyTextView;

import java.util.Arrays;
import java.util.Map;

/**
 * Home asset overview. Layout and binding are owned by this module.
 */
public class AssetOverviewModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "home_asset_overview";
    private boolean isShow = true;

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
        return R.layout.item_home_asset_overview;
    }

    @Override
    protected void onBindView(ViewHolder holder, Map<String, Object> data, int position) {
        TextView tvTotal = holder.getView(R.id.tv_total);
        tvTotal.setText(String.valueOf(data.get("total")));
        bindTotalBadge(tvTotal);
        ((TextView) holder.getView(R.id.tv_profit)).setText("今日收益 " + data.get("profit"));
        ((TextView) holder.getView(R.id.tv_profit_rate))
                .setText(String.valueOf(data.get("profitRate")));

        MoneyTextView tvMoney = holder.getView(R.id.tv_money);
        tvMoney.setMoney("100");
        tvMoney.setHidden(false);
        tvMoney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                MenuPopupWindow popupWindow = new MenuPopupWindow(
                        view.getContext(),
                        Arrays.asList(
                                new MenuItem(R.drawable.ic_tab_home, "优惠券"),
                                new MenuItem(R.drawable.ic_tab_wealth, "版本切换"),
                                new MenuItem(R.drawable.ic_tab_setting, "语音助手")),
                        new MenuPopupWindow.OnMenuClickListener() {
                            @Override
                            public void onMenuClick(MenuItem item, int position) {
                                Toast.makeText(
                                        view.getContext(),
                                        "点击了" + item.text,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                popupWindow.showUnderAnchor(view);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void bindTotalBadge(TextView tvTotal) {
        Object tag = tvTotal.getTag(R.id.tag_corner_badge_handle);
        if (tag instanceof CornerBadgeHelper.BadgeHandle) {
            ((CornerBadgeHelper.BadgeHandle<String>) tag).setData("新功能");
            return;
        }
        tvTotal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object tag = tvTotal.getTag(R.id.tag_corner_badge_handle);
                if (tag instanceof CornerBadgeHelper.BadgeHandle) {
                    isShow = !isShow;
                    ((CornerBadgeHelper.BadgeHandle<String>) tag).setVisible(isShow);
                    ((CornerBadgeHelper.BadgeHandle<String>) tag).setData("121321");
                }
            }
        });
        CornerBadgeHelper.BadgeHandle<String> handle = CornerBadgeHelper.addBadge(
                tvTotal,
                R.layout.layout_corner_badge_demo,
                "新功能",
                new CornerBadgeHelper.BadgeBinder<String>() {
                    @Override
                    public void onBind(View badgeView, String data) {
                        TextView textView = badgeView.findViewById(R.id.tv_corner_badge_text);
                        if (textView != null) {
                            textView.setText(data);
                        }
                    }
                });
        tvTotal.setTag(R.id.tag_corner_badge_handle, handle);
    }
}
