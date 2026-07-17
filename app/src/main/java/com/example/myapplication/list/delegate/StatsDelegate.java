package com.example.myapplication.list.delegate;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.list.model.StatsModuleItem;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 一行多个统计数字的 delegate,子项数量不固定,故动态生成 */
public class StatsDelegate implements ItemViewDelegate<StatsModuleItem> {

    @Override
    public boolean isForViewType(ModuleItem item, int position) {
        return item instanceof StatsModuleItem;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_stats;
    }

    @Override
    public void onBind(ViewHolder holder, StatsModuleItem item, int position) {
        LinearLayout container = holder.getView(R.id.ll_stats_container);
        container.removeAllViews();
        Context ctx = container.getContext();
        for (StatsModuleItem.Stat stat : item.stats) {
            container.addView(buildStatCell(ctx, stat));
        }
    }

    private View buildStatCell(Context ctx, StatsModuleItem.Stat stat) {
        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(lp);

        TextView value = new TextView(ctx);
        value.setText(stat.value);
        value.setTextSize(20);
        value.setTextColor(Color.parseColor("#1976D2"));
        value.setGravity(Gravity.CENTER);

        TextView label = new TextView(ctx);
        label.setText(stat.label);
        label.setTextSize(12);
        label.setTextColor(Color.parseColor("#888888"));
        label.setGravity(Gravity.CENTER);

        cell.addView(value);
        cell.addView(label);
        return cell;
    }
}
