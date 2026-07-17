package com.example.myapplication.list.delegate;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.list.model.ImageTextModuleItem;

import android.widget.ImageView;
import android.widget.TextView;

/** 左图右文 delegate */
public class ImageTextDelegate implements ItemViewDelegate<ImageTextModuleItem> {

    @Override
    public boolean isForViewType(ModuleItem item, int position) {
        return item instanceof ImageTextModuleItem;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_image_text;
    }

    @Override
    public void onBind(ViewHolder holder, ImageTextModuleItem item, int position) {
        ((ImageView) holder.getView(R.id.iv_image)).setImageResource(item.imageResId);
        ((TextView) holder.getView(R.id.tv_image_title)).setText(item.title);
        ((TextView) holder.getView(R.id.tv_image_desc)).setText(item.desc);
    }
}
