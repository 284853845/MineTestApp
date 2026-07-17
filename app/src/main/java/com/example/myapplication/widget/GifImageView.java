package com.example.myapplication.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

/**
 * 基于 {@link Movie} 的 GIF 播放控件,不依赖任何第三方库。
 *
 * 特性:
 * - 设置 GIF 资源后自动播放一遍;
 * - 播放结束后停留在 GIF 的最后一帧(不循环)。
 *
 * 注意:Movie 虽被标记为 deprecated,但在本工程 targetSdk 范围内仍可正常工作,
 * 且能精确控制"停在最后一帧"这一需求。
 */
public class GifImageView extends View {

    private Movie movie;
    private long movieStart;
    /** GIF 一轮的总时长(毫秒) */
    private int duration;
    /** 是否已经播放完一轮并停在最后一帧 */
    private boolean finished;

    public GifImageView(Context context) {
        this(context, null);
    }

    public GifImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GifImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Movie 绘制不走硬件加速,需关闭以保证可见
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * 设置要播放的 GIF 资源(放在 res/drawable 或 res/raw 下的 .gif),并从头播放一遍。
     */
    public void setGifResource(@RawRes int resId) {
        InputStream is = null;
        try {
            is = getResources().openRawResource(resId);
            movie = Movie.decodeStream(is);
            duration = movie != null ? movie.duration() : 0;
            // 某些 GIF 不带时长信息,给一个兜底值
            if (duration <= 0) {
                duration = 1000;
            }
            movieStart = 0L;
            finished = false;
            requestLayout();
            invalidate();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** 从头重新播放一遍。 */
    public void restart() {
        movieStart = 0L;
        finished = false;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (movie != null) {
            int w = resolveSize(movie.width(), widthMeasureSpec);
            int h = resolveSize(movie.height(), heightMeasureSpec);
            setMeasuredDimension(w, h);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (movie == null) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (movieStart == 0L) {
            movieStart = now;
        }

        int relTime;
        boolean lastFrame = false;
        if (finished) {
            // 已结束,固定停在最后一帧
            relTime = duration - 1;
        } else {
            long elapsed = now - movieStart;
            if (elapsed >= duration) {
                // 本轮播放结束:定位到最后一帧并不再继续
                relTime = duration - 1;
                finished = true;
                lastFrame = true;
            } else {
                relTime = (int) elapsed;
            }
        }
        if (relTime < 0) {
            relTime = 0;
        }

        drawScaledMovie(canvas, relTime);

        // 未结束则继续请求重绘以推进动画
        if (!finished && !lastFrame) {
            invalidate();
        }
    }

    private void drawScaledMovie(Canvas canvas, int relTime) {
        movie.setTime(relTime);

        int movieW = movie.width();
        int movieH = movie.height();
        if (movieW <= 0 || movieH <= 0) {
            return;
        }

        // 等比缩放并居中绘制,适配控件实际尺寸
        float scale = Math.min(
                getWidth() / (float) movieW,
                getHeight() / (float) movieH);
        float dx = (getWidth() - movieW * scale) / 2f;
        float dy = (getHeight() - movieH * scale) / 2f;

        int save = canvas.save();
        canvas.translate(dx, dy);
        canvas.scale(scale, scale);
        movie.draw(canvas, 0, 0);
        canvas.restoreToCount(save);
    }
}
