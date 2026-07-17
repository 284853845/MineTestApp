package com.example.myapplication.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.example.myapplication.databinding.LayoutRefreshHintBinding;

public class RefreshHintView extends FrameLayout implements SecondFloorLayout.RefreshHintStyler {

    private final LayoutRefreshHintBinding binding;

    public RefreshHintView(Context context) {
        this(context, null);
    }

    public RefreshHintView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshHintView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = LayoutRefreshHintBinding.inflate(LayoutInflater.from(context), this, true);
    }

    @Override
    public void onPullHint(View hintView, SecondFloorLayout.PullState state,
                           float pullPx, float progress) {
        switch (state) {
            case REFRESHING:
                binding.pbRefresh.setVisibility(View.VISIBLE);
                binding.ivRefreshArrow.setVisibility(View.GONE);
                binding.tvRefreshHint.setText("正在刷新...");
                break;
            case RELEASE_TO_SECOND_FLOOR:
                binding.pbRefresh.setVisibility(View.GONE);
                binding.ivRefreshArrow.setVisibility(View.VISIBLE);
                binding.ivRefreshArrow.setRotation(180f);
                binding.tvRefreshHint.setText("松手进入二楼");
                break;
            case PULL_TO_SECOND_FLOOR:
                binding.pbRefresh.setVisibility(View.GONE);
                binding.ivRefreshArrow.setVisibility(View.VISIBLE);
                binding.ivRefreshArrow.setRotation(0f);
                binding.tvRefreshHint.setText("下拉进入二楼");
                break;
            case RELEASE_TO_REFRESH:
                binding.pbRefresh.setVisibility(View.GONE);
                binding.ivRefreshArrow.setVisibility(View.VISIBLE);
                binding.ivRefreshArrow.setRotation(180f);
                binding.tvRefreshHint.setText("松手立即刷新");
                break;
            case PULL_TO_REFRESH:
            default:
                binding.pbRefresh.setVisibility(View.GONE);
                binding.ivRefreshArrow.setVisibility(View.VISIBLE);
                binding.ivRefreshArrow.setRotation(0f);
                binding.tvRefreshHint.setText("下拉刷新");
                break;
        }
    }
}
