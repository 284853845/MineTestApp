package com.example.myapplication.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;

import com.example.myapplication.R;

/**
 * 单行加载文本控件:文本结尾固定跟随三个点,点底部与文字平齐(不跳动),
 * 循环依次显示 1 个、2 个、3 个点,以此表现"加载中"。
 *
 * 特点:
 *  - 文本强制单行,超出部分末尾省略(...),不会换行;
 *  - 三个点由控件自行绘制,与文字颜色一致、底部对齐;
 *  - 右侧自动预留三个点所需的空间,点不会被裁剪;
 *  - 控件可见时自动播放动画,不可见/移出窗口时自动停止,避免空耗。
 *
 * XML 属性(均可选):
 *   app:ltv_dotRadius    点半径(dimension,默认 2.5dp)
 *   app:ltv_dotSpacing   相邻两点圆心间距(dimension,默认 7dp)
 *   app:ltv_dotGap       文本到第一个点的间距(dimension,默认 4dp)
 *   app:ltv_animDuration 一轮(1->2->3)动画时长(integer 毫秒,默认 900)
 *   app:ltv_dotsAnimEnabled 是否显示结尾三个点的动画(boolean,默认 true)
 *
 * 也可在代码中通过 {@link #setDotsAnimEnabled(boolean)} 动态开关。
 *
 * 示例:
 *   <com.example.myapplication.widget.LoadingTextView
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content"
 *       android:text="加载中" />
 */
public class LoadingTextView extends AppCompatTextView {

    private static final int DOT_COUNT = 3;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float dotRadius;
    private float dotSpacing;
    private float dotGap;
    private int animDuration;
    /** 是否显示结尾三个点的动画 */
    private boolean dotsAnimEnabled = true;

    /** 当前动画进度 0~1,驱动应显示的点数 */
    private float animatedFraction;
    /** 上一帧已绘制的点数,用于仅在点数变化时重绘 */
    private int lastDrawnCount = -1;
    @Nullable
    private ValueAnimator animator;

    public LoadingTextView(Context context) {
        this(context, null);
    }

    public LoadingTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        dotRadius = dp(1f);
        dotSpacing = dp(5f);
        dotGap = dp(4f);
        animDuration = 900;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadingTextView);
            dotRadius = a.getDimension(R.styleable.LoadingTextView_ltv_dotRadius, dotRadius);
            dotSpacing = a.getDimension(R.styleable.LoadingTextView_ltv_dotSpacing, dotSpacing);
            dotGap = a.getDimension(R.styleable.LoadingTextView_ltv_dotGap, dotGap);
            animDuration = a.getInteger(R.styleable.LoadingTextView_ltv_animDuration, animDuration);
            dotsAnimEnabled = a.getBoolean(
                    R.styleable.LoadingTextView_ltv_dotsAnimEnabled, dotsAnimEnabled);
            a.recycle();
        }

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(getCurrentTextColor());

        // 右侧预留三个点的横向空间,文字省略发生在预留区之前,点不会被裁剪
        setPadding(getPaddingLeft(), getPaddingTop(),
                getPaddingRight() + Math.round(reservedDotsWidth()), getPaddingBottom());
    }

    /** 三个点(含与文字的间距)占用的总横向宽度 */
    private float reservedDotsWidth() {
        // 间距 + 第一个点左半径 + (n-1)*圆心间距 + 最后一个点右半径
        return dotGap + dotRadius + (DOT_COUNT - 1) * dotSpacing + dotRadius;
    }

    /**
     * 设置是否显示结尾三个点的动画。
     * true:播放 1->2->3 循环动画;false:停止动画并隐藏三个点(右侧预留宽度保留,文字不会回流抖动)。
     */
    public void setDotsAnimEnabled(boolean enabled) {
        if (this.dotsAnimEnabled == enabled) {
            return;
        }
        this.dotsAnimEnabled = enabled;
        if (enabled) {
            startAnim();
        } else {
            stopAnim();
        }
        invalidate();
    }

    public boolean isDotsAnimEnabled() {
        return dotsAnimEnabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!dotsAnimEnabled) {
            return;
        }

        CharSequence text = getText();
        // 文字实际占用宽度,封顶到文本可用区(被省略时即为可用区宽度)
        float textAreaWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float textWidth = getPaint().measureText(text == null ? "" : text.toString());
        if (textWidth > textAreaWidth) {
            textWidth = textAreaWidth;
        }

        float firstCx = getPaddingLeft() + textWidth + dotGap + dotRadius;
        // 点底部与文字基线平齐(不跳动)
        float cy = getBaseline() - dotRadius;

        // 循环依次显示 1、2、3 个点
        int visibleCount = visibleDotCount();
        lastDrawnCount = visibleCount;
        dotPaint.setColor(getCurrentTextColor());
        for (int i = 0; i < visibleCount; i++) {
            float cx = firstCx + i * dotSpacing;
            canvas.drawCircle(cx, cy, dotRadius, dotPaint);
        }
    }

    /** 根据动画进度计算当前应显示的点数:1 -> 2 -> 3 循环。 */
    private int visibleDotCount() {
        int step = (int) (animatedFraction * DOT_COUNT); // 0,1,2
        if (step >= DOT_COUNT) {
            step = DOT_COUNT - 1; // animatedFraction==1 的边界保护
        }
        return step + 1;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnim();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnim();
    }

    @Override
    protected void onVisibilityChanged(android.view.View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startAnim();
        } else {
            stopAnim();
        }
    }

    private void startAnim() {
        if (animator != null || !dotsAnimEnabled || getVisibility() != VISIBLE) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(animDuration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animatedFraction = (float) animation.getAnimatedValue();
                // 仅当显示的点数发生变化时才重绘
                int count = visibleDotCount();
                if (count != lastDrawnCount) {
                    invalidate();
                }
            }
        });
        animator.start();
    }

    private void stopAnim() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
