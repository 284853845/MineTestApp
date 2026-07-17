package com.example.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewOutlineProvider;

import com.example.myapplication.R;

/**
 * 圆角图片控件，图片和背景色都会一起被圆角裁剪。
 */
public class RoundedImageView extends AppCompatImageView {

    private float cornerRadiusPx;

    public RoundedImageView(Context context) {
        this(context, null);
    }

    public RoundedImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView);
            cornerRadiusPx = a.getDimension(R.styleable.RoundedImageView_riv_cornerRadius, 0f);
            a.recycle();
        }
        applyCorner();
    }

    public void setCornerRadius(float radiusPx) {
        cornerRadiusPx = Math.max(0f, radiusPx);
        applyCorner();
        invalidate();
    }

    public void setCornerRadiusDp(float radiusDp) {
        setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                radiusDp,
                getResources().getDisplayMetrics()));
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        applyCorner();
    }

    private void applyCorner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (cornerRadiusPx > 0f) {
                setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadiusPx);
                    }
                });
                setClipToOutline(true);
            } else {
                setClipToOutline(false);
                setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            }
        }
    }
}
