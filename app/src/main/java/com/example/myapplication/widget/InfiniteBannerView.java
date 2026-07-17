package com.example.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A stacked infinite banner implemented as a custom ViewGroup.
 *
 * Three pages are kept alive: previous, current and next. The current page is
 * centered, the previous page exposes its left edge, and the next page exposes
 * its right edge. Dragging interpolates those three states directly.
 */
public class InfiniteBannerView<T> extends ViewGroup {

    private static final int MSG_SCROLL = 1;
    private static final int ANIM_RANGE = 1000;
    private static final float SIDE_ALPHA = 0.2f;

    private final List<T> items = new ArrayList<>();
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Scroller scroller;
    private final int touchSlop;

    private BannerItemAdapter<T> itemAdapter;
    private BannerPageLayout prevPage;
    private BannerPageLayout currentPage;
    private BannerPageLayout nextPage;
    private OnBannerScrollListener scrollListener;
    private OnBannerClickListener clickListener;

    private float radius;
    private int sidePeekWidth;
    private int sideHeightShrink;
    private int currentIndex;
    private float dragOffset;
    private int preboundDirection;
    private int animFrom;
    private int animTarget;
    private boolean settling;
    private float downX;
    private float downY;
    private float lastX;
    private boolean dragging;
    private boolean horizontalLocked;
    private boolean parentDisallowed;
    private int scrollDuration = 420;

    private boolean indicatorEnabled = false;
    private float dotRadius;
    private float dotSpacing;
    private float dotBottomMargin;
    private int colorActive = 0xFFFFFFFF;
    private int colorInactive = 0x66FFFFFF;

    private boolean autoScrollEnabled = true;
    private long autoScrollInterval = 3000L;
    private boolean attached;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SCROLL) {
                if (canAutoScroll()) {
                    animateTo(1);
                } else if (autoScrollEnabled && attached) {
                    sendScrollDelayed();
                }
            }
        }
    };

    public InfiniteBannerView(Context context) {
        this(context, null);
    }

    public InfiniteBannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawingOrderEnabled(true);

        float density = getResources().getDisplayMetrics().density;
        dotRadius = 3f * density;
        dotSpacing = 8f * density;
        dotBottomMargin = 10f * density;
        sidePeekWidth = 30;
        sideHeightShrink = 60;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        scroller = new Scroller(context, new DecelerateInterpolator(1.45f));

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InfiniteBannerView);
            radius = a.getDimension(R.styleable.InfiniteBannerView_banner_radius, 0f);
            indicatorEnabled = a.getBoolean(
                    R.styleable.InfiniteBannerView_banner_indicatorEnabled, indicatorEnabled);
            autoScrollEnabled = a.getBoolean(
                    R.styleable.InfiniteBannerView_banner_autoScroll, autoScrollEnabled);
            autoScrollInterval = a.getInt(
                    R.styleable.InfiniteBannerView_banner_autoScrollInterval,
                    (int) autoScrollInterval);
            sidePeekWidth = (int) a.getDimension(
                    R.styleable.InfiniteBannerView_banner_sidePeek, sidePeekWidth);
            sideHeightShrink = (int) a.getDimension(
                    R.styleable.InfiniteBannerView_banner_sideHeightShrink, sideHeightShrink);
            a.recycle();
        }
    }

    public void setRadius(float radiusPx) {
        radius = radiusPx;
        updatePageOutlines();
    }

    public void setIndicatorEnabled(boolean enabled) {
        indicatorEnabled = enabled;
        invalidate();
    }

    public void setIndicatorColors(int active, int inactive) {
        colorActive = active;
        colorInactive = inactive;
        invalidate();
    }

    public void setAutoScroll(boolean enabled) {
        autoScrollEnabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    public void setAutoScrollInterval(long intervalMs) {
        autoScrollInterval = intervalMs;
        if (autoScrollEnabled) {
            start();
        }
    }

    /** Set how much of each side page remains visible, in px. */
    public void setSidePeekWidth(int widthPx) {
        sidePeekWidth = Math.max(0, widthPx);
        requestLayout();
    }

    /** Set how much shorter side pages are than the centered page, in px. */
    public void setSideHeightShrink(int heightPx) {
        sideHeightShrink = Math.max(0, heightPx);
        applyTransforms();
    }

    public void setBannerAdapter(BannerItemAdapter<T> adapter) {
        itemAdapter = adapter;
        rebuildPages();
    }

    public void setOnBannerScrollListener(@Nullable OnBannerScrollListener listener) {
        scrollListener = listener;
    }

    public void setOnBannerClickListener(@Nullable OnBannerClickListener listener) {
        clickListener = listener;
    }

    public void setItems(List<T> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        currentIndex = 0;
        dragOffset = 0f;
        preboundDirection = 0;
        scroller.abortAnimation();
        rebuildPages();
        dispatchPageSelected();
        dispatchScrollChanged();
        start();
    }

    private boolean canAutoScroll() {
        return autoScrollEnabled
                && attached
                && getWindowVisibility() == VISIBLE
                && getVisibility() == VISIBLE
                && items.size() > 1
                && !dragging
                && scroller.isFinished();
    }

    private void start() {
        stop();
        if (canAutoScroll()) {
            sendScrollDelayed();
        }
    }

    private void stop() {
        handler.removeMessages(MSG_SCROLL);
    }

    private void sendScrollDelayed() {
        handler.sendEmptyMessageDelayed(MSG_SCROLL, autoScrollInterval);
    }

    private void rebuildPages() {
        removeAllViews();
        prevPage = null;
        currentPage = null;
        nextPage = null;

        if (itemAdapter == null || items.isEmpty()) {
            invalidate();
            return;
        }

        if (items.size() == 1) {
            currentPage = createPage(0);
            addView(currentPage);
        } else {
            prevPage = createPage(wrapIndex(currentIndex - 1));
            nextPage = createPage(wrapIndex(currentIndex + 1));
            currentPage = createPage(currentIndex);
            addView(prevPage);
            addView(nextPage);
            addView(currentPage);
        }
        bringPagesToFront();
        requestLayout();
        invalidate();
    }

    private BannerPageLayout createPage(int realPosition) {
        BannerPageLayout page = new BannerPageLayout(getContext(), radius);
        bindPage(page, realPosition);
        return page;
    }

    private void bindPage(BannerPageLayout page, int realPosition) {
        page.removeBannerContent();
        View view = itemAdapter.onCreateView(page);
        itemAdapter.onBindView(view, items.get(realPosition), realPosition);
        page.addBannerContent(view);
        page.setPageClickListener(realPosition, clickListener);
    }

    private int wrapIndex(int index) {
        int size = items.size();
        if (size == 0) {
            return 0;
        }
        int wrapped = index % size;
        return wrapped < 0 ? wrapped + size : wrapped;
    }

    private void updatePageOutlines() {
        if (prevPage != null) {
            prevPage.setRadius(radius);
        }
        if (currentPage != null) {
            currentPage.setRadius(radius);
        }
        if (nextPage != null) {
            nextPage.setRadius(radius);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        int childWidth = items.size() <= 1 ? width : width - sidePeekWidth * 2;
        int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int height = b - t;
        int childWidth = items.size() <= 1 ? (r - l) : (r - l - sidePeekWidth * 2);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(0, 0, childWidth, height);
        }
        applyTransforms();
    }

    private void applyTransforms() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        if (items.size() <= 1) {
            positionPage(currentPage, 0f, 1f, 1f, 1f, 1f);
            invalidate();
            return;
        }

        float progress = Math.max(-1f, Math.min(1f, dragOffset));
        float t = ease(Math.abs(progress));
        float sideScaleY = Math.max(0f, (getHeight() - sideHeightShrink) / (float) Math.max(1, getHeight()));
        float leftSlot = 0f;
        float centerSlot = sidePeekWidth;
        float rightSlot = sidePeekWidth * 2f;

        updatePreboundLoopPage(progress);

        if (progress >= 0f) {
            positionPage(prevPage, lerp(leftSlot, rightSlot, t), 1f, sideScaleY, SIDE_ALPHA, 1f);
            positionPage(currentPage, lerp(centerSlot, leftSlot, t), 1f, lerp(1f, sideScaleY, t),
                    lerp(1f, SIDE_ALPHA, t), progress < 0.5f ? 3f : 2f);
            positionPage(nextPage, lerp(rightSlot, centerSlot, t), 1f, lerp(sideScaleY, 1f, t),
                    lerp(SIDE_ALPHA, 1f, t), progress >= 0.5f ? 3f : 2f);
        } else {
            positionPage(nextPage, lerp(rightSlot, leftSlot, t), 1f, sideScaleY, SIDE_ALPHA, 1f);
            positionPage(currentPage, lerp(centerSlot, rightSlot, t), 1f, lerp(1f, sideScaleY, t),
                    lerp(1f, SIDE_ALPHA, t), progress > -0.5f ? 3f : 2f);
            positionPage(prevPage, lerp(leftSlot, centerSlot, t), 1f, lerp(sideScaleY, 1f, t),
                    lerp(SIDE_ALPHA, 1f, t), progress <= -0.5f ? 3f : 2f);
        }
        invalidate();
    }

    private void updatePreboundLoopPage(float progress) {
        if (itemAdapter == null || items.size() <= 1) {
            preboundDirection = 0;
            return;
        }
        if (progress >= 0.5f) {
            if (preboundDirection != 1) {
                bindPage(prevPage, wrapIndex(currentIndex + 2));
                preboundDirection = 1;
            }
        } else if (progress <= -0.5f) {
            if (preboundDirection != -1) {
                bindPage(nextPage, wrapIndex(currentIndex - 2));
                preboundDirection = -1;
            }
        } else if (preboundDirection != 0) {
            if (preboundDirection == 1) {
                bindPage(prevPage, wrapIndex(currentIndex - 1));
            } else {
                bindPage(nextPage, wrapIndex(currentIndex + 1));
            }
            preboundDirection = 0;
        }
    }

    private float ease(float t) {
        return t * t * (3f - 2f * t);
    }

    private void positionPage(@Nullable BannerPageLayout page, float translationX, float scaleX, float scaleY,
                              float alpha, float translationZ) {
        if (page == null) {
            return;
        }
        page.setTranslationX(translationX);
        page.setPivotX(page.getMeasuredWidth() / 2f);
        page.setPivotY(getHeight() / 2f);
        page.setScaleX(scaleX);
        page.setScaleY(scaleY);
        page.setAlpha(Math.max(0f, Math.min(1f, alpha)));
        page.setVisibility(VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            page.setTranslationZ(translationZ);
        }
    }

    private float lerp(float start, float end, float fraction) {
        return start + (end - start) * fraction;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        if (childCount != 3 || items.size() <= 1) {
            return super.getChildDrawingOrder(childCount, drawingPosition);
        }

        View bottom;
        View middle;
        View top;
        if (dragOffset >= 0.5f) {
            bottom = prevPage;
            middle = currentPage;
            top = nextPage;
        } else if (dragOffset <= -0.5f) {
            bottom = nextPage;
            middle = currentPage;
            top = prevPage;
        } else {
            bottom = dragOffset < 0f ? nextPage : prevPage;
            middle = dragOffset < 0f ? prevPage : nextPage;
            top = currentPage;
        }

        if (drawingPosition == 0) {
            return safeChildIndex(bottom, drawingPosition);
        } else if (drawingPosition == 1) {
            return safeChildIndex(middle, drawingPosition);
        } else {
            return safeChildIndex(top, drawingPosition);
        }
    }

    private int safeChildIndex(@Nullable View child, int fallback) {
        int index = indexOfChild(child);
        return index >= 0 ? index : fallback;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (items.size() <= 1) {
            return false;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = ev.getX();
                downY = ev.getY();
                dragging = false;
                horizontalLocked = false;
                parentDisallowed = false;
                pauseAutoScrollForTouch();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                float absDx = Math.abs(dx);
                float absDy = Math.abs(dy);
                if (absDx > touchSlop && absDx > absDy) {
                    dragging = true;
                    horizontalLocked = true;
                    lastX = ev.getX();
                    disallowParentIntercept();
                    return true;
                } else if (absDy > touchSlop && absDy > absDx) {
                    horizontalLocked = false;
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                horizontalLocked = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                parentDisallowed = false;
                start();
                break;
            default:
                break;
        }
        return horizontalLocked;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (items.size() <= 1) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = ev.getX();
                downY = ev.getY();
                dragging = true;
                horizontalLocked = false;
                parentDisallowed = false;
                pauseAutoScrollForTouch();
                return true;
            case MotionEvent.ACTION_MOVE:
                float totalDx = ev.getX() - downX;
                float totalDy = ev.getY() - downY;
                float absDx = Math.abs(totalDx);
                float absDy = Math.abs(totalDy);
                if (!horizontalLocked) {
                    if (absDx > touchSlop / 2f && absDx > absDy) {
                        disallowParentIntercept();
                    }
                    if (absDx > touchSlop && absDx > absDy) {
                        horizontalLocked = true;
                        disallowParentIntercept();
                    } else if (absDy > touchSlop && absDy > absDx) {
                        dragging = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                        parentDisallowed = false;
                        return false;
                    }
                }
                if (horizontalLocked) {
                    float x = ev.getX();
                    float dx = x - lastX;
                    lastX = x;
                    dragOffset -= dx / Math.max(1f, getWidth() - sidePeekWidth * 2f);
                    dragOffset = Math.max(-1f, Math.min(1f, dragOffset));
                    applyTransforms();
                    dispatchScrollChanged();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (horizontalLocked) {
                    finishDrag();
                } else {
                    start();
                }
                dragging = false;
                horizontalLocked = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                parentDisallowed = false;
                return true;
            default:
                return true;
        }
    }

    private void finishDrag() {
        int target;
        if (dragOffset > 0.22f) {
            target = 1;
        } else if (dragOffset < -0.22f) {
            target = -1;
        } else {
            target = 0;
        }
        animateTo(target);
    }

    private void pauseAutoScrollForTouch() {
        stop();
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
            if (settling) {
                settleAbortedAnimation();
            }
        }
    }

    private void disallowParentIntercept() {
        if (!parentDisallowed) {
            getParent().requestDisallowInterceptTouchEvent(true);
            parentDisallowed = true;
        }
    }

    private void settleAbortedAnimation() {
        settling = false;
        if (dragOffset >= 0.5f) {
            completeTransition(1, false);
        } else if (dragOffset <= -0.5f) {
            completeTransition(-1, false);
        } else {
            resetDragState(true);
        }
    }

    private void animateTo(int target) {
        if (items.size() <= 1) {
            return;
        }
        animFrom = Math.round(dragOffset * ANIM_RANGE);
        animTarget = target * ANIM_RANGE;
        settling = true;
        scroller.startScroll(animFrom, 0, animTarget - animFrom, 0, scrollDuration);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            dragOffset = scroller.getCurrX() / (float) ANIM_RANGE;
            applyTransforms();
            dispatchScrollChanged();
            postInvalidateOnAnimationCompat();
        } else if (settling) {
            settling = false;
            if (dragOffset >= 0.999f) {
                completeTransition(1, true);
            } else if (dragOffset <= -0.999f) {
                completeTransition(-1, true);
            } else {
                resetDragState(true);
                start();
            }
        }
    }

    private void completeTransition(int direction) {
        completeTransition(direction, true);
    }

    private void completeTransition(int direction, boolean restartAutoScroll) {
        if (items.size() <= 1) {
            resetDragState(true);
            return;
        }
        if (direction > 0) {
            currentIndex = wrapIndex(currentIndex + 1);
            BannerPageLayout recycled = prevPage;
            prevPage = currentPage;
            currentPage = nextPage;
            nextPage = recycled;
            if (preboundDirection != 1) {
                bindPage(nextPage, wrapIndex(currentIndex + 1));
            }
        } else {
            currentIndex = wrapIndex(currentIndex - 1);
            BannerPageLayout recycled = nextPage;
            nextPage = currentPage;
            currentPage = prevPage;
            prevPage = recycled;
            if (preboundDirection != -1) {
                bindPage(prevPage, wrapIndex(currentIndex - 1));
            }
        }
        resetDragState(false);
        bringPagesToFront();
        dispatchPageSelected();
        if (restartAutoScroll) {
            start();
        }
    }

    private void resetDragState(boolean restorePreboundPage) {
        if (restorePreboundPage && preboundDirection == 1 && prevPage != null) {
            bindPage(prevPage, wrapIndex(currentIndex - 1));
        } else if (restorePreboundPage && preboundDirection == -1 && nextPage != null) {
            bindPage(nextPage, wrapIndex(currentIndex + 1));
        }
        dragOffset = 0f;
        preboundDirection = 0;
        applyTransforms();
        dispatchScrollChanged();
    }

    private void dispatchScrollChanged() {
        if (scrollListener != null) {
            scrollListener.onScroll(currentIndex, dragOffset);
        }
    }

    private void dispatchPageSelected() {
        if (scrollListener != null && !items.isEmpty()) {
            scrollListener.onPageSelected(currentIndex);
        }
    }

    private void bringPagesToFront() {
        if (prevPage != null) {
            prevPage.bringToFront();
        }
        if (nextPage != null) {
            nextPage.bringToFront();
        }
        if (currentPage != null) {
            currentPage.bringToFront();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (items.size() > 1) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    getParent().requestDisallowInterceptTouchEvent(true);
                    parentDisallowed = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = ev.getX() - downX;
                    float dy = ev.getY() - downY;
                    float absDx = Math.abs(dx);
                    float absDy = Math.abs(dy);
                    if (!horizontalLocked && absDy > touchSlop && absDy > absDx) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        parentDisallowed = false;
                    } else if (absDx > touchSlop / 2f && absDx > absDy) {
                        disallowParentIntercept();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    getParent().requestDisallowInterceptTouchEvent(false);
                    parentDisallowed = false;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void postInvalidateOnAnimationCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            invalidate();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawIndicator(canvas);
    }

    private void drawIndicator(Canvas canvas) {
        if (!indicatorEnabled || items.size() <= 1) {
            return;
        }
        int count = items.size();
        int current = currentIndex;
        if (dragOffset > 0.5f) {
            current = wrapIndex(currentIndex + 1);
        } else if (dragOffset < -0.5f) {
            current = wrapIndex(currentIndex - 1);
        }

        float step = dotRadius * 2f + dotSpacing;
        float totalWidth = count * dotRadius * 2f + (count - 1) * dotSpacing;
        float startX = (getWidth() - totalWidth) / 2f + dotRadius;
        float cy = getHeight() - dotBottomMargin - dotRadius;
        for (int i = 0; i < count; i++) {
            indicatorPaint.setColor(i == current ? colorActive : colorInactive);
            canvas.drawCircle(startX + i * step, cy, dotRadius, indicatorPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attached = false;
        stop();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            start();
        } else {
            stop();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            start();
        } else {
            stop();
        }
    }

    public static abstract class BannerItemAdapter<T> {
        public abstract View onCreateView(ViewGroup parent);

        public abstract void onBindView(View view, T item, int position);
    }

    public interface OnBannerScrollListener {
        void onScroll(int currentIndex, float offset);

        void onPageSelected(int currentIndex);
    }

    public interface OnBannerClickListener {
        void onBannerClick(int currentIndex);
    }

    private static class BannerPageLayout extends FrameLayout {
        private final FrameLayout contentHost;
        private float pageRadius;

        BannerPageLayout(Context context, float radius) {
            super(context);
            setClipChildren(true);
            contentHost = new FrameLayout(context);
            contentHost.setClipChildren(true);
            addView(contentHost, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            setRadius(radius);
        }

        void removeBannerContent() {
            contentHost.removeAllViews();
        }

        void addBannerContent(View view) {
            contentHost.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        void setPageClickListener(final int index, @Nullable final OnBannerClickListener listener) {
            setClickable(true);
            setFocusable(true);
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onBannerClick(index);
                    }
                }
            });
        }

        void setRadius(float radius) {
            pageRadius = radius;
            if (pageRadius > 0f) {
                contentHost.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), pageRadius);
                    }
                });
                contentHost.setClipToOutline(true);
            } else {
                contentHost.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                contentHost.setClipToOutline(false);
            }
        }
    }
}
