package com.example.myapplication.menu;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/** 菜单弹窗列表的 adapter */
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(MenuItem item, int position);
    }

    private final List<MenuItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public MenuAdapter(List<MenuItem> data) {
        if (data != null) {
            items.addAll(data);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public MenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu_popup, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MenuViewHolder holder, int position) {
        final MenuItem item = items.get(position);
        final int pos = position;
        holder.icon.setImageResource(item.iconResId);
        holder.text.setText(item.text);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(item, pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MenuViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView text;

        MenuViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_menu_icon);
            text = itemView.findViewById(R.id.tv_menu_text);
        }
    }
}
