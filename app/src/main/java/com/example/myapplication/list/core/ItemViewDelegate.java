package com.example.myapplication.list.core;

import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * UI delegate for one module item type.
 */
public interface ItemViewDelegate<T extends ModuleItem> {

    /**
     * Delegate identity used as RecyclerView viewType registration key.
     * Default keeps old behavior: one delegate class maps to one viewType.
     */
    default String getViewTypeKey() {
        return getClass().getName();
    }

    /** Whether this delegate can bind the current item. */
    boolean isForViewType(ModuleItem item, int position);

    /** Layout resource used by this delegate. */
    int getLayoutId();

    /** Bind data into the holder. */
    void onBind(ViewHolder holder, T item, int position);

    /** View is attached to window. Modules can start view-bound work here. */
    default void onViewAttached(ViewHolder holder) {
    }

    /** View is detached from window. Modules can pause view-bound work here. */
    default void onViewDetached(ViewHolder holder) {
    }

    /** ViewHolder is recycled. Modules should release view references here. */
    default void onViewRecycled(ViewHolder holder) {
    }

    /** Default ViewHolder creation; most modules only need getLayoutId(). */
    default ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(getLayoutId(), parent, false));
    }
}
