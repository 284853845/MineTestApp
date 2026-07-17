package com.example.myapplication.list.core;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

/**
 * 通用 ViewHolder,内部用 SparseArray 缓存控件,避免每个模块重复写 findViewById。
 * delegate 中通过 holder.getView(id) 拿到控件即可。
 */
public class ViewHolder extends RecyclerView.ViewHolder {

    private final SparseArray<View> viewCache = new SparseArray<>();

    public ViewHolder(View itemView) {
        super(itemView);
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int viewId) {
        View view = viewCache.get(viewId);
        if (view == null) {
            view = itemView.findViewById(viewId);
            viewCache.put(viewId, view);
        }
        return (T) view;
    }

    public View itemView() {
        return itemView;
    }
}
