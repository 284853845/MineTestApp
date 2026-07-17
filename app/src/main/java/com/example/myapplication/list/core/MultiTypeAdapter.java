package com.example.myapplication.list.core;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Multi-type RecyclerView adapter. It only maps items to their delegates.
 */
public class MultiTypeAdapter extends RecyclerView.Adapter<ViewHolder> {

    private final List<ModuleItem> items = new ArrayList<>();
    private final List<CellKey> keys = new ArrayList<>();
    private final SparseArray<ItemViewDelegate> delegates = new SparseArray<>();
    private final Set<String> registeredKeys = new HashSet<>();

    /**
     * Stable item identity used by DiffUtil.
     */
    public static class CellKey {
        public final String id;
        public final long version;

        public CellKey(String id, long version) {
            this.id = id;
            this.version = version;
        }
    }

    public MultiTypeAdapter register(ItemViewDelegate<?> delegate) {
        if (registeredKeys.add(delegate.getViewTypeKey())) {
            delegates.put(delegates.size(), delegate);
        }
        return this;
    }

    public void setItems(List<? extends ModuleItem> data) {
        items.clear();
        keys.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void setItems(final List<? extends ModuleItem> newItems,
                         final List<CellKey> newKeys) {
        if (newItems == null || newKeys == null || newItems.size() != newKeys.size()) {
            setItems(newItems);
            return;
        }
        final List<CellKey> oldKeys = new ArrayList<>(keys);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldKeys.size();
            }

            @Override
            public int getNewListSize() {
                return newKeys.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldKeys.get(oldPos).id.equals(newKeys.get(newPos).id);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return oldKeys.get(oldPos).version == newKeys.get(newPos).version;
            }
        });
        items.clear();
        items.addAll(newItems);
        keys.clear();
        keys.addAll(newKeys);
        result.dispatchUpdatesTo(this);
    }

    public List<ModuleItem> getItems() {
        return items;
    }

    @Override
    public int getItemViewType(int position) {
        ModuleItem item = items.get(position);
        for (int i = 0; i < delegates.size(); i++) {
            ItemViewDelegate delegate = delegates.valueAt(i);
            if (delegate.isForViewType(item, position)) {
                return delegates.keyAt(i);
            }
        }
        throw new IllegalStateException(
                "No delegate for position=" + position + " ("
                        + item.getClass().getSimpleName() + ")");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemViewDelegate delegate = delegates.get(viewType);
        return delegate.onCreateViewHolder(parent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(ViewHolder holder, int position) {
        ModuleItem item = items.get(position);
        ItemViewDelegate delegate = delegates.get(getItemViewType(position));
        delegate.onBind(holder, item, position);
    }

    @Override
    public void onViewAttachedToWindow(ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ItemViewDelegate delegate = delegates.get(holder.getItemViewType());
        if (delegate != null) {
            delegate.onViewAttached(holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        ItemViewDelegate delegate = delegates.get(holder.getItemViewType());
        if (delegate != null) {
            delegate.onViewDetached(holder);
        }
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        ItemViewDelegate delegate = delegates.get(holder.getItemViewType());
        if (delegate != null) {
            delegate.onViewRecycled(holder);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
