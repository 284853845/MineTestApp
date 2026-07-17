package com.example.myapplication.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.myapplication.R;

/**
 * 自定义滑块控件。
 *
 * <ul>
 *   <li>整条轨道横向等分为若干份(默认 5 份);</li>
 *   <li>滑块(thumb)可设置图片资源;</li>
 *   <li>调用 {@link #setSelectedIndex(int)} 时,滑块移动到对应 index 槽位的正中间。</li>
 * </ul>
 */
public class SliderBar extends FrameLayout {

    /** 默认等分份数 */
    private static final int DEFAULT_PART_COUNT = 5;
    /** 滑块默认宽/高(dp) */
    private static final int DEFAULT_THUMB_SIZE_DP = 40;
    /** 移动动画时长(毫秒) */
    private static final int MOVE_DURATION = 280;

    /** 等分份数 */
    private int partCount = DEFAULT_PART_COUNT;
    /** 当前选中下标 */
    private int selectedIndex = 0;

    private final ImageView thumb;
    /** 滑块宽 / 高(像素) */
    private int thumbWidth;
    private int thumbHeight;
    private ValueAnimator moveAnimator;

    public SliderBar(Context context) {
        this(context, null);
    }

    public SliderBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SliderBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        thumbWidth = dp(DEFAULT_THUMB_SIZE_DP);
        thumbHeight = dp(DEFAULT_THUMB_SIZE_DP);
        int thumbResId = 0;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderBar);
            thumbResId = a.getResourceId(R.styleable.SliderBar_sb_thumb, 0);
            thumbWidth = a.getDimensionPixelSize(
                    R.styleable.SliderBar_sb_thumbWidth, thumbWidth);
            thumbHeight = a.getDimensionPixelSize(
                    R.styleable.SliderBar_sb_thumbHeight, thumbHeight);
            partCount = a.getInteger(
                    R.styleable.SliderBar_sb_partCount, DEFAULT_PART_COUNT);
            if (partCount <= 0) {
                partCount = DEFAULT_PART_COUNT;
            }
            a.recycle();
        }

        thumb = new ImageView(context);
        thumb.setScaleType(ImageView.ScaleType.FIT_XY);
        if (thumbResId != 0) {
            thumb.setImageResource(thumbResId);
        }
        addView(thumb, new LayoutParams(thumbWidth, thumbHeight));
    }

    /** 设置等分份数(默认 5)。 */
    public void setPartCount(int partCount) {
        if (partCount <= 0 || partCount == this.partCount) {
            return;
        }
        this.partCount = partCount;
        if (selectedIndex > partCount - 1) {
            selectedIndex = partCount - 1;
        }
        layoutThumbToSelected(false);
    }

    public int getPartCount() {
        return partCount;
    }

    /** 设置滑块图片资源。 */
    public void setThumbResource(@DrawableRes int resId) {
        thumb.setImageResource(resId);
    }

    /** 设置滑块宽高(像素)。 */
    public void setThumbSize(int widthPx, int heightPx) {
        if (widthPx <= 0 || heightPx <= 0
                || (widthPx == thumbWidth && heightPx == thumbHeight)) {
            return;
        }
        thumbWidth = widthPx;
        thumbHeight = heightPx;
        LayoutParams lp = (LayoutParams) thumb.getLayoutParams();
        lp.width = widthPx;
        lp.height = heightPx;
        thumb.setLayoutParams(lp);
        layoutThumbToSelected(false);
    }

    /**
     * 选中某个 index,滑块移动到该 index 槽位的正中间(带动画)。
     */
    public void setSelectedIndex(int index) {
        setSelectedIndex(index, true);
    }

    /**
     * 选中某个 index。
     *
     * @param index    目标下标
     * @param animated 是否带移动动画
     */
    public void setSelectedIndex(int index, boolean animated) {
        if (index < 0) {
            index = 0;
        } else if (index > partCount - 1) {
            index = partCount - 1;
        }
        selectedIndex = index;
        layoutThumbToSelected(animated);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 尺寸变化时,无动画地把滑块摆到当前选中位置
        layoutThumbToSelected(false);
    }

    /**
     * 把滑块定位到当前选中槽位的正中间。
     *
     * thumb 由 FrameLayout 默认摆在左上角(getLeft()/getTop() 均为 0),
     * 因此这里直接用 translationX/Y 作为相对该原点的偏移即可。
     */
    private void layoutThumbToSelected(boolean animated) {
        if (getWidth() == 0) {
            // 还没测量,等 onSizeChanged 再摆位
            return;
        }
        // 垂直方向始终居中
        thumb.setTranslationY((getHeight() - thumbHeight) / 2f);

        final float targetX = centerXOf(selectedIndex) - thumbWidth / 2f;

        if (moveAnimator != null && moveAnimator.isRunning()) {
            moveAnimator.cancel();
        }

        if (!animated) {
            thumb.setTranslationX(targetX);
            return;
        }

        final float startX = thumb.getTranslationX();
        moveAnimator = ValueAnimator.ofFloat(0f, 1f);
        moveAnimator.setDuration(MOVE_DURATION);
        moveAnimator.setInterpolator(new DecelerateInterpolator());
        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float p = (float) animation.getAnimatedValue();
                thumb.setTranslationX(startX + (targetX - startX) * p);
            }
        });
        moveAnimator.start();
    }

    /** 第 index 个槽位的水平中心 X(像素)。 */
    private float centerXOf(int index) {
        float partWidth = getWidth() / (float) partCount;
        return partWidth * (index + 0.5f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
