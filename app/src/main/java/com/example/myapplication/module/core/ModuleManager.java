package com.example.myapplication.module.core;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.myapplication.data.CacheProvider;
import com.example.myapplication.data.DataSource;
import com.example.myapplication.data.DiskCache;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.MultiTypeAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates one page's modules: config, loading, flattening, exposure,
 * lazy load, refresh, and lifecycle dispatch.
 */
public class ModuleManager implements ModuleHost, Module.LoadCallback {

    private static final int PREFETCH_DP = 160;
    private static final long REFRESH_TIMEOUT_MS = 8000L;

    public interface RefreshCompleteListener {
        void onRefreshComplete();
    }

    private final RecyclerView recyclerView;
    private final MultiTypeAdapter adapter;
    private final DataSource dataSource;
    private final ModuleFactory factory;
    private final String pageId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final RecyclerView.OnScrollListener scrollListener;

    private final List<Module> modules = new ArrayList<>();
    private final List<Module> itemOwners = new ArrayList<>();
    private final Set<Module> exposedModules = new HashSet<>();
    private final Set<Module> pendingInitialTopModules = new HashSet<>();

    private boolean started;
    private boolean refreshing;
    private boolean keepTopOnInitialLoad;
    private boolean pageStarted;
    private boolean pageResumed;
    private boolean pageVisible;
    private boolean destroyed;
    private int pendingRefreshCount;
    private RefreshCompleteListener refreshListener;

    public ModuleManager(RecyclerView recyclerView, MultiTypeAdapter adapter,
                         DataSource dataSource, ModuleFactory factory, String pageId) {
        this.recyclerView = recyclerView;
        this.adapter = adapter;
        this.dataSource = dataSource;
        this.factory = factory;
        this.pageId = pageId;
        this.adapter.register(new SkeletonItem.Delegate());
        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                for (Module m : modules) {
                    m.onPageScroll(dx, dy);
                }
                evaluateScroll();
            }
        };
        this.recyclerView.addOnScrollListener(scrollListener);
    }

    public void setRefreshCompleteListener(RefreshCompleteListener l) {
        this.refreshListener = l;
    }

    public void setKeepTopOnInitialLoad(boolean keepTopOnInitialLoad) {
        this.keepTopOnInitialLoad = keepTopOnInitialLoad;
    }

    @Override
    public android.content.Context getContext() {
        return recyclerView.getContext();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DiskCache getCache() {
        return CacheProvider.get(recyclerView.getContext());
    }

    @Override
    public void requestRebuild() {
        rebuild();
    }

    public void start() {
        if (started || destroyed) {
            return;
        }
        started = true;
        dataSource.getPageConfig(pageId, new DataSource.ConfigCallback() {
            @Override
            public void onResult(List<String> orderedModuleKeys) {
                if (!destroyed) {
                    setupModules(orderedModuleKeys);
                }
            }

            @Override
            public void onError(Throwable error) {
                // Keep the list empty for this demo.
            }
        });
    }

    private void setupModules(List<String> orderedKeys) {
        modules.clear();
        modules.addAll(factory.create(orderedKeys));

        Module.DelegateRegistry registry = new Module.DelegateRegistry() {
            @Override
            public void register(ItemViewDelegate<?> delegate) {
                adapter.register(delegate);
            }
        };
        for (Module m : modules) {
            m.attach(this, this);
            m.registerDelegates(registry);
            m.onModuleCreated();
            replayPageState(m);
        }

        pendingInitialTopModules.clear();
        for (Module m : modules) {
            if (!m.isLazyLoad()) {
                if (keepTopOnInitialLoad) {
                    pendingInitialTopModules.add(m);
                }
                m.startLoad(false);
            }
        }
        rebuild();
        if (keepTopOnInitialLoad) {
            scrollToTop();
        }
    }

    private void replayPageState(Module module) {
        if (pageStarted) {
            module.onPageStarted();
        }
        if (pageResumed) {
            module.onPageResumed();
        }
        if (pageVisible) {
            module.onPageVisible();
        }
    }

    private void rebuild() {
        if (destroyed) {
            return;
        }
        List<ModuleItem> flat = new ArrayList<>();
        List<MultiTypeAdapter.CellKey> keys = new ArrayList<>();
        itemOwners.clear();
        for (Module m : modules) {
            switch (m.getState()) {
                case LOADED:
                    List<ModuleItem> items = m.buildItems();
                    for (int i = 0; i < items.size(); i++) {
                        flat.add(items.get(i));
                        itemOwners.add(m);
                        keys.add(new MultiTypeAdapter.CellKey(
                                m.getKey() + "#" + i, m.getDataVersion()));
                    }
                    break;
                case NOT_LOADED:
                case LOADING:
                    flat.add(new SkeletonItem(m.getKey(), skeletonHeight(m)));
                    itemOwners.add(m);
                    keys.add(new MultiTypeAdapter.CellKey(m.getKey() + "#skeleton", 0L));
                    break;
                case EMPTY:
                case ERROR:
                default:
                    break;
            }
        }
        adapter.setItems(flat, keys);
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                evaluateScroll();
            }
        });
    }

    private int skeletonHeight(Module m) {
        return 120;
    }

    @Override
    public void onLoadFinished(Module module) {
        if (destroyed) {
            return;
        }
        boolean keepTop = pendingInitialTopModules.remove(module);
        rebuild();
        if (keepTop) {
            scrollToTop();
        }
        if (refreshing) {
            pendingRefreshCount--;
            if (pendingRefreshCount <= 0) {
                finishRefresh();
            }
        }
    }

    private void evaluateScroll() {
        if (destroyed || !pageVisible) {
            return;
        }
        if (!(recyclerView.getLayoutManager() instanceof LinearLayoutManager)) {
            return;
        }
        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
        int first = lm.findFirstVisibleItemPosition();
        int last = lm.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
            return;
        }

        Set<Module> nowVisible = new HashSet<>();
        for (int pos = first; pos <= last && pos < itemOwners.size(); pos++) {
            nowVisible.add(itemOwners.get(pos));
        }
        for (Module m : nowVisible) {
            if (!exposedModules.contains(m)) {
                m.onModuleExposed();
            }
        }
        for (Module m : new HashSet<>(exposedModules)) {
            if (!nowVisible.contains(m)) {
                m.onModuleHidden();
            }
        }
        exposedModules.clear();
        exposedModules.addAll(nowVisible);

        int prefetchPx = (int) (PREFETCH_DP
                * recyclerView.getResources().getDisplayMetrics().density + 0.5f);
        int triggerLine = recyclerView.getHeight() + prefetchPx;
        for (int pos = 0; pos < itemOwners.size(); pos++) {
            Module m = itemOwners.get(pos);
            if (!m.isLazyLoad() || m.getState() != Module.State.NOT_LOADED) {
                continue;
            }
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForLayoutPosition(pos);
            if (vh != null && vh.itemView.getTop() <= triggerLine) {
                m.startLoad(false);
            }
        }
    }

    public void refresh() {
        if (refreshing || destroyed) {
            return;
        }
        List<Module> toRefresh = new ArrayList<>();
        for (Module m : modules) {
            if (m.isLazyLoad() && m.getState() == Module.State.NOT_LOADED) {
                continue;
            }
            toRefresh.add(m);
        }
        if (toRefresh.isEmpty()) {
            finishRefresh();
            return;
        }
        pendingRefreshCount = toRefresh.size();
        refreshing = true;
        for (Module m : toRefresh) {
            m.onRefresh();
        }
        mainHandler.postDelayed(refreshTimeout, REFRESH_TIMEOUT_MS);
    }

    private final Runnable refreshTimeout = new Runnable() {
        @Override
        public void run() {
            if (refreshing) {
                finishRefresh();
            }
        }
    };

    private void finishRefresh() {
        refreshing = false;
        pendingRefreshCount = 0;
        mainHandler.removeCallbacks(refreshTimeout);
        scrollToTop();
        if (refreshListener != null) {
            refreshListener.onRefreshComplete();
        }
    }

    private void scrollToTop() {
        recyclerView.stopScroll();
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                scrollToTopNow();
            }
        });
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollToTopNow();
            }
        }, 80);
    }

    private void scrollToTopNow() {
        if (destroyed) {
            return;
        }
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .scrollToPositionWithOffset(0, 0);
        } else {
            recyclerView.scrollToPosition(0);
        }
    }

    public void onPageStarted() {
        if (destroyed || pageStarted) {
            return;
        }
        pageStarted = true;
        for (Module m : modules) {
            m.onPageStarted();
        }
    }

    public void onPageStopped() {
        if (destroyed || !pageStarted) {
            return;
        }
        if (pageVisible) {
            onPageInvisible();
        }
        if (pageResumed) {
            onPagePaused();
        }
        pageStarted = false;
        for (Module m : modules) {
            m.onPageStopped();
        }
    }

    public void onPageResumed() {
        if (destroyed || pageResumed) {
            return;
        }
        pageResumed = true;
        for (Module m : modules) {
            m.onPageResumed();
        }
    }

    public void onPagePaused() {
        if (destroyed || !pageResumed) {
            return;
        }
        if (pageVisible) {
            onPageInvisible();
        }
        pageResumed = false;
        for (Module m : modules) {
            m.onPagePaused();
        }
    }

    public void onPageVisible() {
        if (destroyed || pageVisible) {
            return;
        }
        pageVisible = true;
        for (Module m : modules) {
            m.onPageVisible();
        }
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                evaluateScroll();
            }
        });
    }

    public void onPageInvisible() {
        if (destroyed || !pageVisible) {
            return;
        }
        endExposedModules();
        pageVisible = false;
        for (Module m : modules) {
            m.onPageInvisible();
        }
    }

    private void endExposedModules() {
        for (Module m : new HashSet<>(exposedModules)) {
            m.onModuleHidden();
        }
        exposedModules.clear();
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        endExposedModules();
        if (pageVisible) {
            pageVisible = false;
            for (Module m : modules) {
                m.onPageInvisible();
            }
        }
        if (pageResumed) {
            pageResumed = false;
            for (Module m : modules) {
                m.onPagePaused();
            }
        }
        if (pageStarted) {
            pageStarted = false;
            for (Module m : modules) {
                m.onPageStopped();
            }
        }
        for (Module m : modules) {
            m.onModuleDestroyed();
        }
        destroyed = true;
        refreshing = false;
        pendingRefreshCount = 0;
        mainHandler.removeCallbacks(refreshTimeout);
        recyclerView.removeOnScrollListener(scrollListener);
        modules.clear();
        itemOwners.clear();
        pendingInitialTopModules.clear();
        refreshListener = null;
    }
}
