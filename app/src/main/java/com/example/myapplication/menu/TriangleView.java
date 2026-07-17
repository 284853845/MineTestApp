package com.example.myapplication.menu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * 弹窗指向按钮的小三角。
 * pointUp = true 时尖角朝上(弹窗在按钮下方),false 时尖角朝下(弹窗在按钮上方)。
 */
public class TriangleView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean pointUp = true;

    public TriangleView(Context context) {
        this(context, null);
    }

    public TriangleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setPointUp(boolean up) {
        this.pointUp = up;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        path.reset();
        if (pointUp) {
            path.moveTo(w / 2f, 0);
            path.lineTo(w, h);
            path.lineTo(0, h);
        } else {
            path.moveTo(0, 0);
            path.lineTo(w, 0);
            path.lineTo(w / 2f, h);
        }
        path.close();
        canvas.drawPath(path, paint);
    }
}
