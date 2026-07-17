package com.example.myapplication.module.core;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;

import android.view.View;

/**
 * 懒加载模块在数据到达前的骨架占位。
 * 它占住列表中的一个位置,使该模块能被滚动检测到并触发加载;
 * 数据回来后 Manager 会用真实 item 替换它。
 */
public class SkeletonItem implements ModuleItem {

    /** 占位归属的模块 key,Manager 用它把占位映射回模块 */
    public final String ownerKey;
    /** 占位高度(dp),不同模块可给不同骨架高度 */
    public final int heightDp;

    public SkeletonItem(String ownerKey, int heightDp) {
        this.ownerKey = ownerKey;
        this.heightDp = heightDp;
    }

    /** 骨架占位的渲染 delegate,全局注册一次即可 */
    public static class Delegate implements ItemViewDelegate<SkeletonItem> {
        @Override
        public boolean isForViewType(ModuleItem item, int position) {
            return item instanceof SkeletonItem;
        }

        @Override
        public int getLayoutId() {
            return R.layout.item_module_skeleton;
        }

        @Override
        public void onBind(ViewHolder holder, SkeletonItem item, int position) {
            View card = holder.getView(R.id.skeleton_card);
            int h = (int) (item.heightDp
                    * card.getResources().getDisplayMetrics().density + 0.5f);
            if (card.getLayoutParams().height != h) {
                card.getLayoutParams().height = h;
                card.requestLayout();
            }
        }
    }
}
