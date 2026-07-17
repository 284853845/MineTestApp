package com.example.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.example.myapplication.R;

/**
 * 自定义文本:可在文本左侧、右侧显示图标。
 *
 * 通过 XML 属性控制(均可选):
 *   app:leftIcon      左侧图标资源(drawable)
 *   app:rightIcon     右侧图标资源(drawable)
 *   app:iconSize      图标大小(dimension,宽高一致,左右通用);<=0 则用图标原始尺寸
 *   app:iconColor     图标着色(color,左右通用);不设置则不染色,保留原色
 *   app:iconPadding   图标与文字的间距(dimension)
 *
 * 用法:
 *   <com.example.myapplication.widget.IconTextView
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content"
 *       android:text="更多"
 *       app:leftIcon="@drawable/ic_tag"
 *       app:rightIcon="@drawable/ic_arrow"
 *       app:iconSize="16dp"
 *       app:iconColor="#FF6A00"
 *       app:iconPadding="6dp" />
 */
public class IconTextView extends AppCompatTextView {

    private Drawable leftIcon;
    private Drawable rightIcon;
    private boolean rightIconVisible = true;
    private int iconSizePx = 0;   // <=0 表示用原始尺寸
    private int iconColor = 0;    // 配合 hasIconColor 使用
    private boolean hasIconColor = false;

    public IconTextView(Context context) {
        this(context, null);
    }

    public IconTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconTextView);
            leftIcon = a.getDrawable(R.styleable.IconTextView_leftIcon);
            rightIcon = a.getDrawable(R.styleable.IconTextView_rightIcon);
            iconSizePx = a.getDimensionPixelSize(R.styleable.IconTextView_iconSize, 0);
            if (a.hasValue(R.styleable.IconTextView_iconColor)) {
                iconColor = a.getColor(R.styleable.IconTextView_iconColor, 0);
                hasIconColor = true;
            }
            int padding = a.getDimensionPixelSize(R.styleable.IconTextView_iconPadding, -1);
            if (padding >= 0) {
                setCompoundDrawablePadding(padding);
            }
            a.recycle();
        }
        applyIcons();
    }

    /** 动态设置左侧图标 */
    public void setLeftIcon(Drawable drawable) {
        this.leftIcon = drawable;
        applyIcons();
    }

    /** 动态设置右侧图标 */
    public void setRightIcon(Drawable drawable) {
        this.rightIcon = drawable;
        applyIcons();
    }

    /** Sets whether the right-side icon is displayed. */
    public void setRightIconVisible(boolean visible) {
        if (this.rightIconVisible == visible) {
            return;
        }
        this.rightIconVisible = visible;
        applyIcons();
    }

    /** 动态设置图标大小(px,左右通用) */
    public void setIconSizePx(int px) {
        this.iconSizePx = px;
        applyIcons();
    }

    /** 动态设置图标颜色(左右通用) */
    public void setIconColor(int color) {
        this.iconColor = color;
        this.hasIconColor = true;
        applyIcons();
    }

    /** 动态设置图标与文字间距(px) */
    public void setIconPaddingPx(int px) {
        setCompoundDrawablePadding(px);
    }

    private void applyIcons() {
        Drawable[] cur = getCompoundDrawables();
        Drawable left = prepare(leftIcon);
        Drawable right = rightIconVisible ? prepare(rightIcon) : null;
        // 上、下保持原样,只替换左右
        setCompoundDrawables(left, cur[1], right, cur[3]);
    }

    /** 对图标做染色与尺寸处理;null 直接返回 null(表示该侧无图标) */
    private Drawable prepare(Drawable src) {
        if (src == null) {
            return null;
        }
        // mutate 避免影响同一资源的其它使用方
        Drawable d = src.mutate();

        // 着色
        if (hasIconColor) {
            d = DrawableCompat.wrap(d);
            DrawableCompat.setTint(d, iconColor);
            DrawableCompat.setTintMode(d, PorterDuff.Mode.SRC_IN);
        }

        // 尺寸:设置了就用指定大小,否则用原始固有尺寸
        if (iconSizePx > 0) {
            d.setBounds(0, 0, iconSizePx, iconSizePx);
        } else {
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }
        return d;
    }
}
