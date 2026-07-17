package com.example.myapplication.module.core;

import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;

/**
 * A single-card module whose layout, bind logic, and view lifecycle all live
 * inside the module instance.
 */
public abstract class IndependentCardModule<T> extends SingleCardModule<T> {

    protected abstract int getLayoutId();

    protected abstract void onBindView(ViewHolder holder, T data, int position);

    protected void onViewBound(ViewHolder holder, T data, int position) {
    }

    protected void onViewAttached(ViewHolder holder) {
    }

    protected void onViewDetached(ViewHolder holder) {
    }

    protected void onViewRecycled(ViewHolder holder) {
    }

    @Override
    protected final ModuleItem createCardItem() {
        return new CardItem<T>(this, getData());
    }

    @Override
    public final void registerDelegates(DelegateRegistry registry) {
        registry.register(new Delegate<T>(this));
    }

    private static class CardItem<T> implements ModuleItem {
        final IndependentCardModule<T> module;
        final T data;

        CardItem(IndependentCardModule<T> module, T data) {
            this.module = module;
            this.data = data;
        }
    }

    private static class Delegate<T> implements ItemViewDelegate<CardItem<T>> {
        private final IndependentCardModule<T> module;

        Delegate(IndependentCardModule<T> module) {
            this.module = module;
        }

        @Override
        public String getViewTypeKey() {
            return module.getKey();
        }

        @Override
        public boolean isForViewType(ModuleItem item, int position) {
            return item instanceof CardItem && ((CardItem) item).module == module;
        }

        @Override
        public int getLayoutId() {
            return module.getLayoutId();
        }

        @Override
        public void onBind(ViewHolder holder, CardItem<T> item, int position) {
            module.onBindView(holder, item.data, position);
            module.onViewBound(holder, item.data, position);
        }

        @Override
        public void onViewAttached(ViewHolder holder) {
            module.onViewAttached(holder);
        }

        @Override
        public void onViewDetached(ViewHolder holder) {
            module.onViewDetached(holder);
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            module.onViewRecycled(holder);
        }
    }
}
