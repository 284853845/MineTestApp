package com.example.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.example.myapplication.R;

/**
 * 可在右上角显示小红点的文本控件。
 *
 * 直接在自身右上角绘制一个小红点,无需包裹其它控件。
 *
 * 通过 XML 属性控制(均可选):
 *   app:dotVisible      是否显示红点(boolean,默认 false)
 *   app:dotColor        红点颜色(color,默认红色)
 *   app:dotSize         红点直径(dimension,默认 8dp)
 *   app:dotMarginRight  红点距右边缘的内边距(dimension,默认 2dp)
 *   app:dotMarginTop    红点距上边缘的内边距(dimension,默认 2dp)
 *
 * 示例:
 *   <com.example.myapplication.widget.BadgeView
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content"
 *       android:text="消息"
 *       app:dotVisible="true" />
 *
 * 动态控制:
 *   badgeView.setDotVisible(true);
 *   badgeView.setDotColor(Color.RED);
 */
public class BadgeView extends AppCompatTextView {

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean dotVisible = false;
    private int dotSizePx;
    private int dotMarginRightPx;
    private int dotMarginTopPx;

    public BadgeView(Context context) {
        this(context, null);
    }

    public BadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BadgeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int dotColor = Color.parseColor("#FF3B30");
        dotSizePx = dp(8);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BadgeView);
            dotVisible = a.getBoolean(R.styleable.BadgeView_dotVisible, false);
            dotColor = a.getColor(R.styleable.BadgeView_dotColor, dotColor);
            dotSizePx = a.getDimensionPixelSize(R.styleable.BadgeView_dotSize, dotSizePx);
            dotMarginRightPx = a.getDimensionPixelSize(R.styleable.BadgeView_dotMarginRight, dotMarginRightPx);
            dotMarginTopPx = a.getDimensionPixelSize(R.styleable.BadgeView_dotMarginTop, dotMarginTopPx);
            a.recycle();
        }

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(dotColor);
    }

    /** 是否显示红点 */
    public void setDotVisible(boolean visible) {
        if (this.dotVisible != visible) {
            this.dotVisible = visible;
            invalidate();
        }
    }

    public boolean isDotVisible() {
        return dotVisible;
    }

    /** 红点颜色 */
    public void setDotColor(int color) {
        dotPaint.setColor(color);
        invalidate();
    }

    /** 红点直径(px) */
    public void setDotSizePx(int px) {
        this.dotSizePx = px;
        invalidate();
    }

    /** 红点距右、上边缘的内边距(px) */
    public void setDotMarginPx(int rightPx, int topPx) {
        this.dotMarginRightPx = rightPx;
        this.dotMarginTopPx = topPx;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!dotVisible) {
            return;
        }
        float radius = dotSizePx / 2f;
        // 圆心贴在控件右上角,右、上各留出对应的内边距
        float cx = getWidth() - radius - dotMarginRightPx;
        float cy = radius + dotMarginTopPx;
        canvas.drawCircle(cx, cy, radius, dotPaint);
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
