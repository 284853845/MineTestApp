package com.example.myapplication.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * 二楼展示容器。
 * 使用方只需要把真实二楼布局放进这个容器里,show/hide 动画由组件内部处理。
 */
public class SecondFloorView extends FrameLayout {

    private static final int SCRIM_COLOR = 0xCC000000;
    private static final long SHOW_DURATION_MS = 500L;
    private static final long HIDE_DURATION_MS = 500L;

    private View scrimView;
    private FrameLayout contentContainer;
    private boolean opened;
    private boolean addingInternalView;

    public SecondFloorView(Context context) {
        this(context, null);
    }

    public SecondFloorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SecondFloorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setVisibility(GONE);
        init();
    }

    private void init() {
        scrimView = new View(getContext());
        scrimView.setAlpha(0f);
        scrimView.setBackgroundColor(SCRIM_COLOR);

        contentContainer = new FrameLayout(getContext());

        addingInternalView = true;
        super.addView(scrimView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        super.addView(contentContainer, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addingInternalView = false;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (addingInternalView || child == scrimView || child == contentContainer) {
            super.addView(child, index, params);
            return;
        }
        int childIndex = index < 0 ? -1 : Math.min(index, contentContainer.getChildCount());
        contentContainer.addView(child, childIndex, params);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!opened) {
            scrimView.setAlpha(0f);
            contentContainer.setTranslationY(-h);
        }
    }

    public void show() {
        if (opened) {
            return;
        }
        opened = true;
        setVisibility(VISIBLE);
        bringToFront();
        animate().cancel();
        if (getHeight() == 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    startShowAnim();
                }
            });
        } else {
            startShowAnim();
        }
    }

    private void startShowAnim() {
        cancelAnimations();
        scrimView.setAlpha(0f);
        contentContainer.setTranslationY(-getHeight());
        contentContainer.setAlpha(1f);

        scrimView.animate()
                .alpha(1f)
                .setDuration(SHOW_DURATION_MS)
                .start();
        contentContainer.animate()
                .translationY(0f)
                .setDuration(SHOW_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public void hide() {
        if (!opened) {
            return;
        }
        opened = false;
        cancelAnimations();
        scrimView.animate()
                .alpha(0f)
                .setDuration(HIDE_DURATION_MS)
                .start();
        contentContainer.animate()
                .translationY(-getHeight())
                .setDuration(HIDE_DURATION_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!opened) {
                            setVisibility(View.GONE);
                        }
                    }
                })
                .start();
    }

    private void cancelAnimations() {
        animate().cancel();
        scrimView.animate().cancel();
        contentContainer.animate().cancel();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 空白区域吞掉触摸,但子 View 的点击事件仍然由它们自己处理
        return opened || getVisibility() == VISIBLE || super.onTouchEvent(event);
    }

    public boolean isOpened() {
        return opened;
    }
}
