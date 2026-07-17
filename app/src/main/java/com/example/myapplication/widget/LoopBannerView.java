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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A pager-style infinite banner built on top of RecyclerView.
 * Business modules provide each page view via BannerItemAdapter.
 */
public class LoopBannerView<T> extends FrameLayout {

    private static final int MSG_AUTO_SCROLL = 1;
    private static final long DEFAULT_AUTO_SCROLL_INTERVAL = 3000L;
    private static final int LOOP_COUNT = Integer.MAX_VALUE;
    private static final int TOUCH_MODE_NONE = 0;
    private static final int TOUCH_MODE_HORIZONTAL = 1;
    private static final int TOUCH_MODE_VERTICAL = 2;
    // 手势方向锁参数:在横向轮播和外层纵向列表之间做斜滑容错。
    private static final float VERTICAL_LOCK_FACTOR = 1.3f;
    private static final float HORIZONTAL_LOCK_FACTOR = 0.75f;
    private static final float TOUCH_SLOP_BUFFER = 1.5f;

    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final PagerSnapHelper snapHelper = new PagerSnapHelper();
    private final BannerAdapter internalAdapter = new BannerAdapter();
    private final List<T> items = new ArrayList<>();
    private final int touchSlop;

    // 自动轮播只在主线程发消息,避免跨线程直接操作 RecyclerView。
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_AUTO_SCROLL) {
                performAutoScroll();
            }
        }
    };

    private BannerItemAdapter<T> bannerAdapter;
    private OnBannerClickListener clickListener;
    private OnPageSelectedListener pageSelectedListener;

    private boolean autoScrollEnabled = true;
    private long autoScrollInterval = DEFAULT_AUTO_SCROLL_INTERVAL;
    private boolean indicatorEnabled = true;
    private int indicatorSelectedColor = 0xFFEC1B30;
    private int indicatorNormalColor = 0x99FFFFFF;
    private float indicatorSelectedWidth;
    private float indicatorNormalWidth;
    private float indicatorHeight;
    private float indicatorSpacing;
    private float indicatorBottomMargin;
    private float cornerRadius;

    private boolean attached;
    private boolean touchPaused;
    private boolean manuallyPaused;
    private boolean scrollIdle = true;
    private boolean parentDisallowed;
    private float downX;
    private float downY;
    private int touchMode = TOUCH_MODE_NONE;
    private int currentAdapterPosition = RecyclerView.NO_POSITION;
    private int currentRealPosition = 0;

    public LoopBannerView(Context context) {
        this(context, null);
    }

    public LoopBannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        float density = getResources().getDisplayMetrics().density;
        indicatorSelectedWidth = 8f * density;
        indicatorNormalWidth = 4f * density;
        indicatorHeight = 2f * density;
        indicatorSpacing = 2f * density;
        indicatorBottomMargin = 5f * density;

        // 读取独立的 LoopBannerView 属性,不复用旧 InfiniteBannerView 的 attrs。
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoopBannerView);
            autoScrollEnabled = a.getBoolean(
                    R.styleable.LoopBannerView_loopBanner_autoScroll, autoScrollEnabled);
            autoScrollInterval = a.getInt(
                    R.styleable.LoopBannerView_loopBanner_autoScrollInterval,
                    (int) autoScrollInterval);
            indicatorEnabled = a.getBoolean(
                    R.styleable.LoopBannerView_loopBanner_indicatorEnabled, indicatorEnabled);
            indicatorSelectedColor = a.getColor(
                    R.styleable.LoopBannerView_loopBanner_indicatorSelectedColor,
                    indicatorSelectedColor);
            indicatorNormalColor = a.getColor(
                    R.styleable.LoopBannerView_loopBanner_indicatorNormalColor,
                    indicatorNormalColor);
            indicatorSelectedWidth = a.getDimension(
                    R.styleable.LoopBannerView_loopBanner_indicatorSelectedWidth,
                    indicatorSelectedWidth);
            indicatorNormalWidth = a.getDimension(
                    R.styleable.LoopBannerView_loopBanner_indicatorNormalWidth,
                    indicatorNormalWidth);
            indicatorHeight = a.getDimension(
                    R.styleable.LoopBannerView_loopBanner_indicatorHeight, indicatorHeight);
            indicatorSpacing = a.getDimension(
                    R.styleable.LoopBannerView_loopBanner_indicatorSpacing, indicatorSpacing);
            indicatorBottomMargin = a.getDimension(
                    R.styleable.LoopBannerView_loopBanner_indicatorBottomMargin,
                    indicatorBottomMargin);
            cornerRadius = a.getDimension(
                    R.styleable.LoopBannerView_loopBanner_cornerRadius, 0f);
            a.recycle();
        }

        // 内部 RecyclerView 只负责横向分页,业务页面内容由外部 adapter 注入。
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(internalAdapter);
        recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        recyclerView.setNestedScrollingEnabled(false);
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                // 滑动过程中实时刷新当前位置,让内置指示器跟随手势变化。
                if (dx != 0) {
                    updateCurrentPosition(false);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView rv, int newState) {
                scrollIdle = newState == RecyclerView.SCROLL_STATE_IDLE;
                if (scrollIdle) {
                    updateCurrentPosition(false);
                    scheduleAutoScroll();
                } else {
                    cancelAutoScroll();
                }
            }
        });
        addView(recyclerView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        updateOutline();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 在 ViewGroup 分发层处理手势冲突,比内部 RecyclerView 触摸回调更早。
        if (items.size() > 1) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchPaused = true;
                    cancelAutoScroll();
                    downX = event.getRawX();
                    downY = event.getRawY();
                    touchMode = TOUCH_MODE_NONE;
                    requestParentIntercept(false);
                    break;
                case MotionEvent.ACTION_MOVE:
                    handleTouchMove(event.getRawX() - downX, event.getRawY() - downY);
                    break;
                case MotionEvent.ACTION_UP:
                    touchPaused = false;
                    touchMode = TOUCH_MODE_NONE;
                    requestParentIntercept(true);
                    scheduleAutoScroll();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    touchPaused = false;
                    touchMode = TOUCH_MODE_NONE;
                    requestParentIntercept(true);
                    settleToNearestPage();
                    scheduleAutoScroll();
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void handleTouchMove(float dx, float dy) {
        // 一次手势只锁定一个方向:横向留给 Banner,明确竖向交还给外层列表。
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        if (touchMode == TOUCH_MODE_HORIZONTAL) {
            requestParentIntercept(false);
            return;
        }
        if (touchMode == TOUCH_MODE_VERTICAL) {
            requestParentIntercept(true);
            return;
        }
        if (absDx <= touchSlop && absDy <= touchSlop) {
            requestParentIntercept(false);
            return;
        }
        if (isVerticalGesture(absDx, absDy)) {
            touchMode = TOUCH_MODE_VERTICAL;
            requestParentIntercept(true);
            return;
        }
        if (!isHorizontalGesture(absDx, absDy)) {
            requestParentIntercept(false);
            return;
        }
        touchMode = TOUCH_MODE_HORIZONTAL;
        requestParentIntercept(false);
    }

    private boolean isVerticalGesture(float absDx, float absDy) {
        if (absDy <= touchSlop) {
            return false;
        }
        if (absDy > absDx * VERTICAL_LOCK_FACTOR) {
            return true;
        }
        return absDy > absDx && absDx <= touchSlop * TOUCH_SLOP_BUFFER;
    }

    private boolean isHorizontalGesture(float absDx, float absDy) {
        return absDx > touchSlop && absDx >= absDy * HORIZONTAL_LOCK_FACTOR;
    }

    private void requestParentIntercept(boolean allowIntercept) {
        // 减少重复调用 requestDisallowInterceptTouchEvent,避免父容器状态抖动。
        if (getParent() == null) {
            return;
        }
        boolean shouldDisallow = !allowIntercept;
        if (parentDisallowed == shouldDisallow) {
            return;
        }
        getParent().requestDisallowInterceptTouchEvent(shouldDisallow);
        parentDisallowed = shouldDisallow;
    }

    public void setBannerAdapter(@Nullable BannerItemAdapter<T> adapter) {
        bannerAdapter = adapter;
        internalAdapter.notifyDataSetChanged();
        resetAfterDataChanged();
    }

    public void setItems(@Nullable List<T> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        currentAdapterPosition = RecyclerView.NO_POSITION;
        currentRealPosition = 0;
        internalAdapter.notifyDataSetChanged();
        resetAfterDataChanged();
    }

    public void setAutoScrollEnabled(boolean enabled) {
        autoScrollEnabled = enabled;
        if (!enabled) {
            cancelAutoScroll();
        } else {
            scheduleAutoScroll();
        }
    }

    public void setAutoScrollInterval(long intervalMs) {
        autoScrollInterval = Math.max(1000L, intervalMs);
        scheduleAutoScroll();
    }

    public void setIndicatorEnabled(boolean enabled) {
        indicatorEnabled = enabled;
        invalidate();
    }

    public void setOnBannerClickListener(@Nullable OnBannerClickListener listener) {
        clickListener = listener;
    }

    public void setOnPageSelectedListener(@Nullable OnPageSelectedListener listener) {
        pageSelectedListener = listener;
    }

    public void pauseAutoScroll() {
        manuallyPaused = true;
        cancelAutoScroll();
        requestParentIntercept(true);
        settleToNearestPage();
    }

    public void resumeAutoScroll() {
        manuallyPaused = false;
        scheduleAutoScroll();
    }

    public void setCornerRadius(float radiusPx) {
        cornerRadius = Math.max(0f, radiusPx);
        updateOutline();
    }

    public int getCurrentRealPosition() {
        return currentRealPosition;
    }

    private void resetAfterDataChanged() {
        // 数据或 adapter 变化后重新定位到循环区间中间,避免接近 Integer 边界。
        cancelAutoScroll();
        if (items.isEmpty() || bannerAdapter == null) {
            invalidate();
            return;
        }
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                scrollToInitialPosition();
                updateCurrentPosition(true);
                scheduleAutoScroll();
            }
        });
    }

    private void scrollToInitialPosition() {
        if (items.isEmpty()) {
            return;
        }
        int initialPosition = items.size() <= 1 ? 0 : buildInitialPosition();
        layoutManager.scrollToPositionWithOffset(initialPosition, 0);
        currentAdapterPosition = initialPosition;
        currentRealPosition = toRealPosition(initialPosition);
    }

    private void settleToNearestPage() {
        // 页面隐藏/取消触摸时立即归位,防止回来后出现两页各露一半。
        recyclerView.stopScroll();
        scrollIdle = true;
        if (items.isEmpty() || bannerAdapter == null) {
            invalidate();
            return;
        }
        int adapterPosition = findCurrentAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            adapterPosition = currentAdapterPosition != RecyclerView.NO_POSITION
                    ? currentAdapterPosition : buildInitialPosition();
        }
        layoutManager.scrollToPositionWithOffset(adapterPosition, 0);
        int realPosition = toRealPosition(adapterPosition);
        boolean changed = currentRealPosition != realPosition;
        currentAdapterPosition = adapterPosition;
        currentRealPosition = realPosition;
        invalidate();
        if (pageSelectedListener != null && changed) {
            pageSelectedListener.onPageSelected(realPosition);
        }
    }

    private int buildInitialPosition() {
        if (items.isEmpty()) {
            return 0;
        }
        int middle = LOOP_COUNT / 2;
        return middle - middle % items.size();
    }

    private void performAutoScroll() {
        // 每次自动轮播只前进一页,真实下标由 position % size 转换。
        if (!canAutoScroll()) {
            scheduleAutoScroll();
            return;
        }
        int current = findCurrentAdapterPosition();
        if (current == RecyclerView.NO_POSITION) {
            current = currentAdapterPosition != RecyclerView.NO_POSITION
                    ? currentAdapterPosition : buildInitialPosition();
        }
        recyclerView.smoothScrollToPosition(current + 1);
    }

    private void scheduleAutoScroll() {
        cancelAutoScroll();
        if (canAutoScroll()) {
            handler.sendEmptyMessageDelayed(MSG_AUTO_SCROLL, autoScrollInterval);
        }
    }

    private void cancelAutoScroll() {
        handler.removeMessages(MSG_AUTO_SCROLL);
    }

    private boolean canAutoScroll() {
        // 自动轮播需要同时满足 View 生命周期、模块暂停状态和数据量条件。
        return autoScrollEnabled
                && !manuallyPaused
                && !touchPaused
                && attached
                && scrollIdle
                && getWindowVisibility() == VISIBLE
                && getVisibility() == VISIBLE
                && items.size() > 1
                && bannerAdapter != null;
    }

    private void updateCurrentPosition(boolean forceDispatch) {
        // currentRealPosition 同时驱动指示器和外部页面选中回调。
        int adapterPosition = findCurrentAdapterPosition();
        if (adapterPosition == RecyclerView.NO_POSITION) {
            invalidate();
            return;
        }
        int realPosition = toRealPosition(adapterPosition);
        boolean changed = currentRealPosition != realPosition;
        currentAdapterPosition = adapterPosition;
        currentRealPosition = realPosition;
        invalidate();
        if (pageSelectedListener != null && (forceDispatch || changed)) {
            pageSelectedListener.onPageSelected(realPosition);
        }
    }

    private int findCurrentAdapterPosition() {
        // 优先使用 PagerSnapHelper 的吸附页;滑动中再退化到可见 item。
        View snapView = snapHelper.findSnapView(layoutManager);
        if (snapView != null) {
            int position = recyclerView.getChildAdapterPosition(snapView);
            if (position != RecyclerView.NO_POSITION) {
                return position;
            }
        }
        int firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstCompletelyVisible != RecyclerView.NO_POSITION) {
            return firstCompletelyVisible;
        }
        return layoutManager.findFirstVisibleItemPosition();
    }

    private int toRealPosition(int adapterPosition) {
        if (items.isEmpty()) {
            return 0;
        }
        int size = items.size();
        int position = adapterPosition % size;
        return position < 0 ? position + size : position;
    }

    private void updateOutline() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (cornerRadius <= 0f) {
            setClipToOutline(false);
            setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            invalidateOutline();
            return;
        }
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        invalidateOutline();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawIndicator(canvas);
    }

    private void drawIndicator(Canvas canvas) {
        // 指示器由 Banner 自绘,布局侧不需要额外放点位控件。
        if (!indicatorEnabled || items.size() <= 1) {
            return;
        }
        float totalWidth = 0f;
        for (int i = 0; i < items.size(); i++) {
            totalWidth += i == currentRealPosition ? indicatorSelectedWidth : indicatorNormalWidth;
        }
        totalWidth += (items.size() - 1) * indicatorSpacing;

        float left = (getWidth() - totalWidth) / 2f;
        float top = getHeight() - indicatorBottomMargin - indicatorHeight;
        for (int i = 0; i < items.size(); i++) {
            float width = i == currentRealPosition ? indicatorSelectedWidth : indicatorNormalWidth;
            indicatorPaint.setColor(i == currentRealPosition
                    ? indicatorSelectedColor : indicatorNormalColor);
            canvas.drawRect(left, top, left + width, top + indicatorHeight, indicatorPaint);
            left += width + indicatorSpacing;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        scheduleAutoScroll();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // detach 时清理消息和父容器拦截状态,避免 RecyclerView 复用后残留。
        attached = false;
        cancelAutoScroll();
        requestParentIntercept(true);
        settleToNearestPage();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            scheduleAutoScroll();
        } else {
            cancelAutoScroll();
            requestParentIntercept(true);
            settleToNearestPage();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView != this) {
            return;
        }
        if (visibility == VISIBLE) {
            scheduleAutoScroll();
        } else {
            cancelAutoScroll();
            requestParentIntercept(true);
            settleToNearestPage();
        }
    }

    public static abstract class BannerItemAdapter<T> {
        // 业务模块通过这个 adapter 自定义每一页布局和绑定逻辑。
        public View onCreateView(ViewGroup parent) {
            return LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        public abstract void onBindView(View view, T item, int position);
    }

    public interface OnBannerClickListener {
        void onBannerClick(int position);
    }

    public interface OnPageSelectedListener {
        void onPageSelected(int position);
    }

    private class BannerAdapter extends RecyclerView.Adapter<BannerHolder> {

        @Override
        public BannerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = bannerAdapter == null
                    ? new FrameLayout(parent.getContext())
                    : bannerAdapter.onCreateView(parent);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            view.setLayoutParams(params);
            return new BannerHolder(view);
        }

        @Override
        public void onBindViewHolder(BannerHolder holder, int position) {
            if (bannerAdapter == null || items.isEmpty()) {
                return;
            }
            final int realPosition = toRealPosition(position);
            final T item = items.get(realPosition);
            bannerAdapter.onBindView(holder.itemView, item, realPosition);
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onBannerClick(realPosition);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            if (items.isEmpty() || bannerAdapter == null) {
                return 0;
            }
            return items.size() <= 1 ? items.size() : LOOP_COUNT;
        }
    }

    private static class BannerHolder extends RecyclerView.ViewHolder {

        BannerHolder(View itemView) {
            super(itemView);
        }
    }
}
