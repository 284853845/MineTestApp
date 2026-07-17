package com.example.myapplication.list.delegate;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.list.model.HeaderItem;
import com.example.myapplication.widget.ExpandableContainer;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

/** 头部 delegate */
public class HeaderDelegate implements ItemViewDelegate<HeaderItem> {

    @Override
    public boolean isForViewType(ModuleItem item, int position) {
        return item instanceof HeaderItem;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_header;
    }

    @Override
    public void onBind(ViewHolder holder, HeaderItem item, int position) {
        RecyclerView rvMoney = holder.getView(R.id.rv_money);
        rvMoney.setNestedScrollingEnabled(false);
        rvMoney.setLayoutManager(new GridLayoutManager(rvMoney.getContext(), 2));
        rvMoney.setAdapter(new MoneyAdapter(buildMoneyItems()));
        final TextView tvOnlineMoney = holder.getView(R.id.tv_online_money);

        final ExpandableContainer expandable = holder.getView(R.id.expandable);
        tvOnlineMoney.setVisibility(expandable.isExpanded() ? View.VISIBLE : View.GONE);
        tvOnlineMoney.setAlpha(expandable.isExpanded() ? 1f : 0f);
        expandable.setOnExpandProgressChangeListener(new ExpandableContainer.OnExpandProgressChangeListener() {
            @Override
            public void onExpandProgressChanged(float progress) {
                if (progress > 0f && tvOnlineMoney.getVisibility() != View.VISIBLE) {
                    tvOnlineMoney.setVisibility(View.VISIBLE);
                }
                tvOnlineMoney.setAlpha(progress);
                if (progress <= 0f) {
                    tvOnlineMoney.setVisibility(View.GONE);
                }
            }
        });
        holder.getView(R.id.btn_expand).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandable.expand();
            }
        });
        holder.getView(R.id.btn_collapse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expandable.collapse();
            }
        });
    }

    private List<MoneyItem> buildMoneyItems() {
        return Arrays.asList(
                new MoneyItem("总资产", "200,000.00"),
                new MoneyItem("可用余额", "80,000.00"),
                new MoneyItem("在途资金", "100,000.00"),
                new MoneyItem("累计收益", "12,345.67"),
                new MoneyItem("昨日收益", "88.88"),
                new MoneyItem("待结算", "6,666.00"),
                new MoneyItem("冻结金额", "1,000.00"),
                new MoneyItem("本月支出", "9,999.00"),
                new MoneyItem("本月收入", "18,888.00"),
                new MoneyItem("体验金", "500.00"));
    }

    private static class MoneyItem {
        final String name;
        final String amount;

        MoneyItem(String name, String amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    private static class MoneyAdapter extends RecyclerView.Adapter<MoneyAdapter.MoneyViewHolder> {
        private final List<MoneyItem> items;

        MoneyAdapter(List<MoneyItem> items) {
            this.items = items;
        }

        @Override
        public MoneyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header_money, parent, false);
            return new MoneyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MoneyViewHolder holder, int position) {
            MoneyItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvAmount.setText(item.amount);
            holder.middleDivider.setVisibility(position % 2 == 0 ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class MoneyViewHolder extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvAmount;
            final View middleDivider;

            MoneyViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_money_name);
                tvAmount = itemView.findViewById(R.id.tv_money_amount);
                middleDivider = itemView.findViewById(R.id.view_middle_divider);
            }
        }
    }
}
