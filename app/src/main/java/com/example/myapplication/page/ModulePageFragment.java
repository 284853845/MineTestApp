package com.example.myapplication.page;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.myapplication.R;
import com.example.myapplication.data.DataProvider;
import com.example.myapplication.list.core.MultiTypeAdapter;
import com.example.myapplication.module.core.ModuleManager;
import com.example.myapplication.module.core.ModuleRegistry;
import com.example.myapplication.widget.SecondFloorLayout;

/**
 * Base page for module-driven tabs.
 */
public abstract class ModulePageFragment extends Fragment {

    private static final int SCROLL_THRESHOLD_DP = 200;

    public interface OnScrollThresholdListener {
        void onScrollThreshold(boolean beyond);
    }

    protected SecondFloorLayout secondFloorLayout;
    protected RecyclerView recyclerView;
    protected ModuleManager moduleManager;

    private boolean resumed;
    private int scrollThresholdPx;
    private boolean scrolledBeyond;
    private OnScrollThresholdListener scrollThresholdListener;

    protected abstract String getPageId();

    protected boolean isRefreshEnabled() {
        return true;
    }

    protected boolean isSecondFloorEnabled() {
        return false;
    }

    protected int getLayoutId() {
        return R.layout.layout_module_page;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindPageViews(view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        scrollThresholdPx = (int) (SCROLL_THRESHOLD_DP
                * getResources().getDisplayMetrics().density + 0.5f);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                boolean beyond = rv.computeVerticalScrollOffset() > scrollThresholdPx;
                if (beyond != scrolledBeyond) {
                    scrolledBeyond = beyond;
                    if (scrollThresholdListener != null) {
                        scrollThresholdListener.onScrollThreshold(beyond);
                    }
                }
            }
        });

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof android.support.v7.widget.SimpleItemAnimator) {
            ((android.support.v7.widget.SimpleItemAnimator) animator)
                    .setSupportsChangeAnimations(false);
        }

        MultiTypeAdapter adapter = new MultiTypeAdapter();
        recyclerView.setAdapter(adapter);

        moduleManager = new ModuleManager(recyclerView, adapter,
                DataProvider.get(), ModuleRegistry.buildFactory(), getPageId());
        moduleManager.setKeepTopOnInitialLoad(shouldKeepTopOnInitialLoad());
        moduleManager.setRefreshCompleteListener(new ModuleManager.RefreshCompleteListener() {
            @Override
            public void onRefreshComplete() {
                if (secondFloorLayout != null) {
                    secondFloorLayout.finishRefresh();
                }
            }
        });

        secondFloorLayout.setPullEnabled(isRefreshEnabled(), isSecondFloorEnabled());
        secondFloorLayout.setOnRefreshListener(new SecondFloorLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SecondFloorLayout layout) {
                scrollToTopImmediately();
                moduleManager.refresh();
            }
        });

        onPageViewReady(view);
        moduleManager.start();
    }

    protected void onPageViewReady(View view) {
    }

    protected void bindPageViews(View view) {
        secondFloorLayout = view.findViewById(R.id.second_floor_layout);
        recyclerView = view.findViewById(R.id.rv_modules);
    }

    protected boolean shouldKeepTopOnInitialLoad() {
        return false;
    }

    public void setOnScrollThresholdListener(OnScrollThresholdListener listener) {
        this.scrollThresholdListener = listener;
    }

    public boolean isScrolledBeyondThreshold() {
        return scrolledBeyond;
    }

    public void scrollToTop() {
        if (recyclerView == null) {
            return;
        }
        recyclerView.stopScroll();
        recyclerView.smoothScrollToPosition(0);
    }

    protected void scrollToTopImmediately() {
        if (recyclerView == null) {
            return;
        }
        recyclerView.stopScroll();
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .scrollToPositionWithOffset(0, 0);
        } else {
            recyclerView.scrollToPosition(0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (moduleManager != null) {
            moduleManager.onPageStarted();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        if (moduleManager != null) {
            moduleManager.onPageResumed();
        }
        if (!isHidden()) {
            dispatchVisible();
        }
    }

    @Override
    public void onPause() {
        dispatchInvisible();
        if (moduleManager != null) {
            moduleManager.onPagePaused();
        }
        resumed = false;
        super.onPause();
    }

    @Override
    public void onStop() {
        if (moduleManager != null) {
            moduleManager.onPageStopped();
        }
        super.onStop();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            dispatchInvisible();
        } else if (resumed) {
            dispatchVisible();
        }
    }

    @Override
    public void onDestroyView() {
        if (moduleManager != null) {
            moduleManager.destroy();
            moduleManager = null;
        }
        secondFloorLayout = null;
        recyclerView = null;
        super.onDestroyView();
    }

    private void dispatchVisible() {
        if (moduleManager != null) {
            moduleManager.onPageVisible();
        }
    }

    private void dispatchInvisible() {
        if (moduleManager != null) {
            moduleManager.onPageInvisible();
        }
    }
}
