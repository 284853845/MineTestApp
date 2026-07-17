package com.example.myapplication.list.delegate;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.list.model.ActionModuleItem;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** 带按钮的操作 delegate */
public class ActionDelegate implements ItemViewDelegate<ActionModuleItem> {

    @Override
    public boolean isForViewType(ModuleItem item, int position) {
        return item instanceof ActionModuleItem;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_action;
    }

    @Override
    public void onBind(ViewHolder holder, final ActionModuleItem item, int position) {
        ((TextView) holder.getView(R.id.tv_action_title)).setText(item.title);
        Button button = holder.getView(R.id.btn_action);
        button.setText(item.buttonText);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (item.callback != null) {
                    item.callback.onClick(item);
                }
            }
        });
    }
}
