package com.example.myapplication.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.example.myapplication.R;

/**
 * 可展开 / 收起的容器。
 *
 * 你只需往里放三类子 View,并用 app:ec_layout_role 标记角色:
 *   - content :  实际内容(必填)
 *   - expand  :  收起状态下显示的"展开"按钮(必填)
 *   - collapse:  展开状态下显示的"收起"按钮(必填)
 *
 * 容器负责:
 *   1. 收起时高度 = ec_collapsedHeight(默认 200dp),内容被裁剪;
 *   2. 展开时高度动画过渡到 content 的实际高度;
 *   3. 动画过程中 expand 与 collapse 按钮交叉淡入淡出;
 *   4. 内容本身高度不变,只是容器逐渐变高/变矮来"露出/盖住"它。
 *
 * 内容与按钮的具体 UI 全部由使用方自己实现,本容器不关心。
 */
public class ExpandableContainer extends ViewGroup {

    public static final int ROLE_CONTENT = 0;
    public static final int ROLE_EXPAND = 1;
    public static final int ROLE_COLLAPSE = 2;

    public interface OnExpandStateChangeListener {
        void onExpandStateChanged(boolean expanded);
    }

    public interface OnExpandProgressChangeListener {
        /** progress:0=完全收起,1=完全展开 */
        void onExpandProgressChanged(float progress);
    }

    private int collapsedHeight;
    private int animDuration;
    private boolean expanded;

    private View contentView;
    private View expandButton;
    private View collapseButton;

    /** 当前容器高度(动画过程中实时变化) */
    private int currentHeight = -1;
    /** content 测量出的完整高度 */
    private int fullContentHeight;

    private ValueAnimator animator;
    private OnExpandStateChangeListener listener;
    private OnExpandProgressChangeListener progressListener;

    public ExpandableContainer(Context context) {
        this(context, null);
    }

    public ExpandableContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defCollapsed = (int) (200 * getResources().getDisplayMetrics().density);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableContainer);
            collapsedHeight = a.getDimensionPixelSize(
                    R.styleable.ExpandableContainer_ec_collapsedHeight, defCollapsed);
            animDuration = a.getInt(R.styleable.ExpandableContainer_ec_animDuration, 300);
            expanded = a.getBoolean(R.styleable.ExpandableContainer_ec_expanded, false);
            a.recycle();
        } else {
            collapsedHeight = defCollapsed;
            animDuration = 300;
            expanded = false;
        }
        // 关键:子内容超出容器高度时裁剪掉,从而实现"逐渐露出"
        setClipChildren(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            switch (lp.role) {
                case ROLE_EXPAND:
                    expandButton = child;
                    break;
                case ROLE_COLLAPSE:
                    collapseButton = child;
                    break;
                default:
                    contentView = child;
                    break;
            }
        }
        applyStateImmediately(expanded);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        // content 按它自身需要的完整高度测量(不受容器收起高度限制)
        if (contentView != null) {
            measureContent(contentView, widthMeasureSpec);
            fullContentHeight = contentView.getMeasuredHeight();
        }
        // 两个按钮按自身大小测量
        measureButton(expandButton, widthMeasureSpec);
        measureButton(collapseButton, widthMeasureSpec);

        // 首次测量时根据初始状态确定高度
        if (currentHeight < 0) {
            currentHeight = expanded ? fullContentHeight : collapsedHeight;
        }

        setMeasuredDimension(widthSize, currentHeight);
    }

    private void measureContent(View child, int parentWidthSpec) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int widthSpec = getChildMeasureSpec(parentWidthSpec, 0, lp.width);
        int heightSpec;
        if (lp.height >= 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        } else if (lp.height == LayoutParams.MATCH_PARENT) {
            heightSpec = MeasureSpec.makeMeasureSpec(fullContentHeight, MeasureSpec.EXACTLY);
        } else { // WRAP_CONTENT
            heightSpec = MeasureSpec.makeMeasureSpec(100000, MeasureSpec.AT_MOST);
        }
        child.measure(widthSpec, heightSpec);
    }

    private void measureButton(View child, int parentWidthSpec) {
        if (child == null) {
            return;
        }
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int widthSpec = getChildMeasureSpec(parentWidthSpec, 0, lp.width);
        int heightSpec;
        if (lp.height >= 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        } else {
            heightSpec = MeasureSpec.makeMeasureSpec(100000, MeasureSpec.AT_MOST);
        }
        child.measure(widthSpec, heightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        // 内容固定从顶部铺,超出 currentHeight 的部分被裁剪
        if (contentView != null) {
            contentView.layout(0, 0, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        }
        // 两个按钮都贴在当前高度的底部,水平居中
        layoutBottomCenter(expandButton, width, currentHeight);
        layoutBottomCenter(collapseButton, width, currentHeight);
    }

    private void layoutBottomCenter(View child, int width, int bottom) {
        if (child == null) {
            return;
        }
        int cw = child.getMeasuredWidth();
        int ch = child.getMeasuredHeight();
        int left = (width - cw) / 2;
        int top = bottom - ch;
        child.layout(left, top, left + cw, top + ch);
    }

    // ---------------- 对外 API ----------------

    public void toggle() {
        setExpanded(!expanded, true);
    }

    public void expand() {
        setExpanded(true, true);
    }

    public void collapse() {
        setExpanded(false, true);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setOnExpandStateChangeListener(OnExpandStateChangeListener l) {
        this.listener = l;
    }

    public void setOnExpandProgressChangeListener(OnExpandProgressChangeListener l) {
        this.progressListener = l;
    }

    public void setExpanded(boolean expand, boolean animate) {
        if (this.expanded == expand && currentHeight >= 0) {
            return;
        }
        this.expanded = expand;
        // 还没测量出尺寸时,直接置位,等测量
        if (getWidth() == 0 || fullContentHeight == 0) {
            currentHeight = -1;
            requestLayout();
            applyStateImmediately(expand);
            return;
        }
        int from = currentHeight;
        int to = expand ? fullContentHeight : collapsedHeight;
        if (!animate) {
            currentHeight = to;
            updateButtonProgress(expand ? 1f : 0f);
            notifyProgress(expand ? 1f : 0f);
            requestLayout();
            notifyState();
            return;
        }
        startAnim(from, to, expand);
    }

    private void startAnim(int from, int to, final boolean expand) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        // 动画过程中两个按钮都参与淡入淡出,故都先可见
        if (expandButton != null) {
            expandButton.setVisibility(VISIBLE);
        }
        if (collapseButton != null) {
            collapseButton.setVisibility(VISIBLE);
        }
        animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(animDuration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentHeight = (int) animation.getAnimatedValue();
                // 进度:0=完全收起 1=完全展开
                float denom = (fullContentHeight - collapsedHeight);
                float progress = denom == 0 ? (expand ? 1f : 0f)
                        : (currentHeight - collapsedHeight) / denom;
                if (progress < 0f) progress = 0f;
                if (progress > 1f) progress = 1f;
                updateButtonProgress(progress);
                notifyProgress(progress);
                requestLayout();
            }
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator a) {
                float endProgress = expand ? 1f : 0f;
                updateButtonProgress(endProgress);
                notifyProgress(endProgress);
                notifyState();
            }
        });
        animator.start();
    }

    /** progress:0 显示展开按钮,1 显示收起按钮,中间交叉淡变 */
    private void updateButtonProgress(float progress) {
        if (expandButton != null) {
            expandButton.setAlpha(1f - progress);
            expandButton.setVisibility(progress >= 1f ? GONE : VISIBLE);
        }
        if (collapseButton != null) {
            collapseButton.setAlpha(progress);
            collapseButton.setVisibility(progress <= 0f ? GONE : VISIBLE);
        }
    }

    private void applyStateImmediately(boolean expand) {
        float progress = expand ? 1f : 0f;
        updateButtonProgress(progress);
        notifyProgress(progress);
    }

    private void notifyProgress(float progress) {
        if (progressListener != null) {
            progressListener.onExpandProgressChanged(progress);
        }
    }

    private void notifyState() {
        if (listener != null) {
            listener.onExpandStateChanged(expanded);
        }
    }

    // ---------------- LayoutParams ----------------

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int role = ROLE_CONTENT;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs,
                    R.styleable.ExpandableContainer_Layout);
            role = a.getInt(R.styleable.ExpandableContainer_Layout_ec_layout_role, ROLE_CONTENT);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
