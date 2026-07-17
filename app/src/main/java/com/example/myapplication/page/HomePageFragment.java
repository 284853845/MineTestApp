package com.example.myapplication.page;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.myapplication.R;
import com.example.myapplication.databinding.LayoutHomePageBinding;
import com.example.myapplication.widget.SecondFloorLayout;
import com.example.myapplication.widget.SecondFloorView;

/**
 * 首页:下拉刷新 + 下拉进入二楼。
 * 二楼需要覆盖包括 BottomTabBar 在内的整屏内容,所以会挂到 Activity content 上。
 */
public class HomePageFragment extends ModulePageFragment {

    private LayoutHomePageBinding binding;
    private SecondFloorView secondFloorView;

    @Override
    protected String getPageId() {
        return "home";
    }

    @Override
    protected boolean isSecondFloorEnabled() {
        return true;
    }

    @Override
    protected boolean isRefreshEnabled() {
        return true;
    }

    @Override
    protected boolean shouldKeepTopOnInitialLoad() {
        return true;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.layout_home_page;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = LayoutHomePageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void bindPageViews(View view) {
        secondFloorLayout = binding.secondFloorLayout;
        recyclerView = binding.rvModules;
        binding.tvLoadingDemo.setDotsAnimEnabled(true);
    }

    @Override
    protected void onPageViewReady(View view) {
        secondFloorView = binding.secondFloorView;
        moveSecondFloorToActivityRoot(secondFloorView);

        secondFloorLayout.setOnSecondFloorListener(new SecondFloorLayout.OnSecondFloorListener() {
            @Override
            public void onEnterSecondFloor(SecondFloorLayout layout) {
                secondFloorView.show();
            }
        });
        binding.secondFloorContent.btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                secondFloorView.hide();
            }
        });

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                scrollToTopImmediately();
            }
        });
    }

    public void onReadSmsPermissionChanged() {
        if (moduleManager != null) {
            moduleManager.refresh();
        }
    }

    private void moveSecondFloorToActivityRoot(SecondFloorView floor) {
        Activity activity = getActivity();
        if (floor == null || activity == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) floor.getParent();
        if (parent != null) {
            parent.removeView(floor);
        }
        activity.addContentView(floor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onDestroyView() {
        if (secondFloorView != null) {
            ViewGroup parent = (ViewGroup) secondFloorView.getParent();
            if (parent != null) {
                parent.removeView(secondFloorView);
            }
            secondFloorView = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
