package com.example.myapplication.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.example.myapplication.R;

/**
 * 下拉刷新 + 下拉二楼触发容器。
 * 子 View 顺序:
 *  1. 下拉提示:可自定义的 xml 布局,下拉过程中显示,样式由 {@link RefreshHintStyler} 回调驱动
 *  2. 主内容:RecyclerView
 *
 * 本容器只负责下拉手势、刷新状态和触发二楼回调;
 * 提示区域的具体样式、二楼实际布局和展开/关闭动画都由使用方自己实现。
 */
public class SecondFloorLayout extends FrameLayout {

    // 主流默认值:刷新触发 ≈60dp(对齐 Material SwipeRefreshLayout),
    // 刷新保持高度 = 头部高度 60dp,二楼触发需要明显更长的下拉 ≈160dp。
    private static final int REFRESH_THRESHOLD_DP = 60;
    private static final int REFRESH_HOLD_HEIGHT_DP = 60;
    private static final int SWITCH_THRESHOLD_DP = 160;

    /** 下拉提示的状态。 */
    public enum PullState {
        /** 未下拉(空闲,提示区隐藏) */
        IDLE,
        /** 下拉中,但还未到刷新阈值 */
        PULL_TO_REFRESH,
        PULL_TO_SECOND_FLOOR,
        /** 已下拉超过刷新阈值,松手即刷新 */
        RELEASE_TO_REFRESH,
        /** 启用二楼且已下拉超过二楼阈值,松手进入二楼 */
        RELEASE_TO_SECOND_FLOOR,
        /** 正在刷新 */
        REFRESHING
    }

    public interface OnRefreshListener {
        void onRefresh(SecondFloorLayout layout);
    }

    public interface OnSecondFloorListener {
        void onEnterSecondFloor(SecondFloorLayout layout);
    }

    /**
     * 下拉提示样式回调。下拉过程中、状态或下拉距离变化时调用,
     * 由使用方根据当前状态/进度自行设置提示区域(child 0 的那个 xml 布局)的样式。
     */
    public interface RefreshHintStyler {
        /**
         * @param hintView 提示区域的根 View(SecondFloorLayout 的第一个子 View)
         * @param state    当前下拉状态
         * @param pullPx   当前下拉距离(像素)
         * @param progress 相对刷新阈值的进度,0~1(超过阈值后可 >1)
         */
        void onPullHint(View hintView, PullState state, float pullPx, float progress);
    }

    private View refreshHintView;
    private View contentView;
    private int touchSlop;
    private int refreshThreshold;
    private int refreshHoldHeight;
    private int switchThreshold;

    private float downX;
    private float downY;
    private float lastY;
    private float pullOffset;
    private boolean dragging;
    private boolean refreshing;
    // 是否启用二楼。首页为 true(下拉刷新+二楼),其余 Tab 为 false(仅下拉刷新)
    private boolean refreshEnabled = true;
    private boolean secondFloorEnabled = true;
    private ValueAnimator pullAnimator;
    private OnRefreshListener refreshListener;
    private OnSecondFloorListener secondFloorListener;
    private RefreshHintStyler refreshHintStyler;
    private PullState currentState = PullState.IDLE;
    private final Handler handler = new Handler();

    public SecondFloorLayout(Context context) {
        this(context, null);
    }

    public SecondFloorLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SecondFloorLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // 默认值(dp);可被 xml 属性覆盖
        refreshThreshold = dp(REFRESH_THRESHOLD_DP);
        refreshHoldHeight = dp(REFRESH_HOLD_HEIGHT_DP);
        switchThreshold = dp(SWITCH_THRESHOLD_DP);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SecondFloorLayout);
            refreshEnabled = a.getBoolean(
                    R.styleable.SecondFloorLayout_sfl_refreshEnabled, refreshEnabled);
            secondFloorEnabled = a.getBoolean(
                    R.styleable.SecondFloorLayout_sfl_secondFloorEnabled, secondFloorEnabled);
            refreshThreshold = a.getDimensionPixelSize(
                    R.styleable.SecondFloorLayout_sfl_refreshThreshold, refreshThreshold);
            refreshHoldHeight = a.getDimensionPixelSize(
                    R.styleable.SecondFloorLayout_sfl_refreshHoldHeight, refreshHoldHeight);
            switchThreshold = a.getDimensionPixelSize(
                    R.styleable.SecondFloorLayout_sfl_switchThreshold, switchThreshold);
            a.recycle();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() >= 2) {
            refreshHintView = getChildAt(0);
            contentView = getChildAt(1);
            // 提示区域若自身实现了样式回调,自动作为 styler,使用方无需再手动设置
            if (refreshHintView instanceof RefreshHintStyler) {
                refreshHintStyler = (RefreshHintStyler) refreshHintView;
            }
            applyPullOffset(0f);
            applyRefreshHint();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (contentView == null || refreshing || !isPullEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = lastY = event.getY();
                dragging = false;
                cancelPullAnim();
                return false;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (dy > touchSlop && Math.abs(dy) > Math.abs(dx)
                        && !contentView.canScrollVertically(-1)) {
                    dragging = true;
                    lastY = event.getY();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (contentView == null || refreshing || !isPullEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - lastY;
                lastY = event.getY();
                moveBy(dy);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                finishDrag();
                performClick();
                return true;
        }
        return true;
    }

    private void moveBy(float dy) {
        // 最大下拉量:拉到这里时阻力趋近无穷,基本拉不动,避免拉出过长空白。
        // 留出明显大于触发阈值的余量,配合阻尼后仍能轻松拉过阈值。
        float maxPull = getMaxPull();

        float next;
        if (dy > 0f) {
            // 向下拉:加阻尼。下拉越多阻力越大,实际增量随 pullOffset 接近 maxPull 而衰减到 0,
            // 手感是"越往下越拉不动",而不是和手指 1:1 同步。
            float damping = 1f - pullOffset / maxPull;
            if (damping < 0f) {
                damping = 0f;
            }
            next = pullOffset + dy * damping;
        } else {
            // 向上收(回拉):跟手 1:1,松手前能顺畅退回
            next = pullOffset + dy;
        }

        if (next < 0f) {
            next = 0f;
        } else if (next > maxPull) {
            next = maxPull;
        }
        applyPullOffset(next);
    }

    private void finishDrag() {
        dragging = false;
        if (secondFloorEnabled && pullOffset > switchThreshold) {
            animatePullBack(true);
            if (secondFloorListener != null) {
                secondFloorListener.onEnterSecondFloor(this);
            }
        } else if (refreshEnabled && pullOffset >= refreshThreshold) {
            startRefresh();
        } else {
            animatePullBack(false);
        }
    }

    /**
     * 设置是否启用二楼。
     * true:下拉刷新 + 下拉到二楼(首页);false:仅下拉刷新(其余 Tab)。
     */
    public void setRefreshEnabled(boolean enabled) {
        this.refreshEnabled = enabled;
        onPullAbilityChanged();
    }

    public boolean isRefreshEnabled() {
        return refreshEnabled;
    }

    public void setSecondFloorEnabled(boolean enabled) {
        this.secondFloorEnabled = enabled;
        onPullAbilityChanged();
    }

    public boolean isSecondFloorEnabled() {
        return secondFloorEnabled;
    }

    public void setPullEnabled(boolean refreshEnabled, boolean secondFloorEnabled) {
        this.refreshEnabled = refreshEnabled;
        this.secondFloorEnabled = secondFloorEnabled;
        onPullAbilityChanged();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    public void setOnSecondFloorListener(OnSecondFloorListener listener) {
        this.secondFloorListener = listener;
    }

    /**
     * 设置下拉提示样式回调。提示区域(第一个子 View)的样式由使用方在回调里
     * 根据 {@link PullState} 自行处理。设置后会立即按当前状态回调一次。
     */
    public void setRefreshHintStyler(RefreshHintStyler styler) {
        this.refreshHintStyler = styler;
        // 立即按当前状态同步一次
        if (refreshHintView != null) {
            notifyHint(currentState);
        }
    }

    public void finishRefresh() {
        refreshing = false;
        animatePullBack(false);
    }

    private void startRefresh() {
        refreshing = true;
        notifyHint(PullState.REFRESHING);
        animatePullTo(refreshHoldHeight, 180);
        if (refreshListener != null) {
            refreshListener.onRefresh(this);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishRefresh();
                }
            }, 800);
        }
    }

    private void animatePullBack(boolean fast) {
        animatePullTo(0f, fast ? 120 : 240);
    }

    private void animatePullTo(float target, int duration) {
        cancelPullAnim();
        pullAnimator = ValueAnimator.ofFloat(pullOffset, target);
        pullAnimator.setDuration(duration);
        pullAnimator.setInterpolator(new DecelerateInterpolator());
        pullAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                applyPullOffset((Float) animation.getAnimatedValue());
            }
        });
        pullAnimator.start();
    }

    private void applyPullOffset(float offset) {
        pullOffset = offset;
        if (contentView != null) {
            contentView.setTranslationY(pullOffset);
        }
        applyRefreshHint();
    }

    private void applyRefreshHint() {
        if (refreshHintView == null) {
            return;
        }
        if (pullOffset <= 0f && !refreshing) {
            refreshHintView.setVisibility(GONE);
        } else {
            refreshHintView.setVisibility(VISIBLE);
            refreshHintView.bringToFront();
        }
        refreshHintView.setAlpha(1f);

        // 提示区域高度跟随下拉距离
        ViewGroup.LayoutParams lp = refreshHintView.getLayoutParams();
        int targetHeight = Math.max(0, Math.round(pullOffset));
        if (lp.height != targetHeight) {
            lp.height = targetHeight;
            refreshHintView.setLayoutParams(lp);
        }
        refreshHintView.setTranslationY(0f);

        notifyHint(computeState());
    }

    /** 根据当前下拉距离 / 刷新态推导出提示状态。 */
    private PullState computeState() {
        if (refreshing) {
            return PullState.REFRESHING;
        }
        if (pullOffset <= 0f) {
            return PullState.IDLE;
        }
        if (secondFloorEnabled && pullOffset > switchThreshold) {
            return PullState.RELEASE_TO_SECOND_FLOOR;
        }
        if (refreshEnabled && pullOffset >= refreshThreshold) {
            return PullState.RELEASE_TO_REFRESH;
        }
        if (!refreshEnabled && secondFloorEnabled) {
            return PullState.PULL_TO_SECOND_FLOOR;
        }
        return PullState.PULL_TO_REFRESH;
    }

    /** 把当前状态与下拉进度回调给使用方,由其设置提示区域样式。 */
    private void notifyHint(PullState state) {
        currentState = state;
        if (refreshHintStyler != null && refreshHintView != null) {
            float progress = refreshThreshold > 0 ? pullOffset / refreshThreshold : 0f;
            refreshHintStyler.onPullHint(refreshHintView, state, pullOffset, progress);
        }
    }

    private void cancelPullAnim() {
        if (pullAnimator != null && pullAnimator.isRunning()) {
            pullAnimator.cancel();
        }
    }

    private void onPullAbilityChanged() {
        if (refreshing && !refreshEnabled) {
            refreshing = false;
        }
        if (!isPullEnabled() || pullOffset > getMaxPull()) {
            animatePullBack(true);
        } else {
            applyRefreshHint();
        }
    }

    private boolean isPullEnabled() {
        return refreshEnabled || secondFloorEnabled;
    }

    private float getMaxPull() {
        if (secondFloorEnabled) {
            return switchThreshold * 2.2f;
        }
        if (refreshEnabled) {
            return refreshThreshold * 3f;
        }
        return 0f;
    }
}
