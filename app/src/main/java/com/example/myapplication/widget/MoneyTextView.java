package com.example.myapplication.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import com.example.myapplication.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * 自定义金额文本控件。
 *
 * 规则:
 *  1. setMoney 设置金额时,按"每 3 位一个逗号 + 保留两位小数"展示(整数也补 .00),
 *     例:1234567.5 -> 1,234,567.50;1000 -> 1,000.00。
 *  2. 设置的文本为空(null/空串) -> 显示 loading 态(自绘菊花 spinner)。
 *  3. 设置的文本为 "****" -> 原样显示(脱敏占位)。
 *  4. loading 菊花的大小按"文本单行最大高度"显示(取字体单行高度作为直径)。
 *
 * 可选 XML 属性:
 *   app:loadingColor        loading 菊花颜色,默认取当前文字颜色
 *   app:loadingStrokeWidth  loading 辐条线宽,默认 2dp
 */
public class MoneyTextView extends IconTextView {

    private static final String MASK = "****";

    private final DecimalFormat moneyFormat =
            new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private boolean loading = false;

    /** 最近一次设置的金额原始值(隐藏态下也记住,以便重新显示时恢复) */
    private CharSequence lastMoney;
    /** 当前是否处于隐藏态(显示 ****) */
    private boolean hidden = false;
    private boolean showRightIconWhenHidden = true;

    // loading 菊花绘制
    private Paint loadingPaint;
    private int loadingColor;
    private boolean loadingColorSet = false;
    private float loadingStrokeWidth;
    private ValueAnimator loadingAnimator;
    /** 菊花辐条数量 */
    private static final int SPOKE_COUNT = 12;
    /** 当前处于最亮位置的辐条索引 */
    private int activeSpoke = 0;

    public MoneyTextView(Context context) {
        this(context, null);
    }

    public MoneyTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoneyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        loadingStrokeWidth = 2.5f * density;
        loadingColor = getCurrentTextColor();

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MoneyTextView);
            if (a.hasValue(R.styleable.MoneyTextView_loadingColor)) {
                loadingColor = a.getColor(R.styleable.MoneyTextView_loadingColor, loadingColor);
                loadingColorSet = true;
            }
            loadingStrokeWidth = a.getDimension(
                    R.styleable.MoneyTextView_loadingStrokeWidth, loadingStrokeWidth);
            showRightIconWhenHidden = a.getBoolean(
                    R.styleable.MoneyTextView_showRightIconWhenHidden, true);
            a.recycle();
        }

        loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        loadingPaint.setStyle(Paint.Style.STROKE);
        loadingPaint.setStrokeCap(Paint.Cap.ROUND);
        loadingPaint.setColor(loadingColor);
        loadingPaint.setStrokeWidth(loadingStrokeWidth);
    }

    /**
     * 设置金额。入参可为纯数字字符串、空串、或 "****"。
     *  - 空 -> loading
     *  - "****" -> 原样显示
     *  - 数字 -> 千分位 + 两位小数
     *  - 其它无法解析的字符串 -> 原样显示(兜底)
     *
     * 注:隐藏态({@link #setHidden(boolean)})下仍会记住该金额,
     * 重新显示时按此值恢复;当前画面保持 **** 不变。
     */
    public void setMoney(CharSequence money) {
        lastMoney = money;
        if (hidden) {
            // 隐藏态:只记值,画面仍是 ****
            return;
        }
        render();
    }

    /** 数字便捷入口 */
    public void setMoney(double amount) {
        setMoney(String.valueOf(amount));
    }

    /**
     * 设置显示/隐藏。
     *  - 隐藏(true):直接显示 ****。
     *  - 显示(false):有金额则显示金额,没金额则显示 loading。
     */
    public void setHidden(boolean hidden) {
        if (this.hidden == hidden) {
            return;
        }
        this.hidden = hidden;
        updateRightIconVisibility();
        render();
    }

    /** 便捷方法:显示金额(等价 setHidden(false)) */
    public void show() {
        setHidden(false);
    }

    /** 便捷方法:隐藏金额(等价 setHidden(true)) */
    public void hide() {
        setHidden(true);
    }

    public boolean isHidden() {
        return hidden;
    }

    private void updateRightIconVisibility() {
        setRightIconVisible(!hidden || showRightIconWhenHidden);
    }

    /** 根据 hidden 与 lastMoney 决定当前画面 */
    private void render() {
        // 隐藏态:一律 ****
        if (hidden) {
            exitLoading();
            setText(MASK);
            return;
        }

        // 显示态:空 -> loading
        if (lastMoney == null || TextUtils.isEmpty(lastMoney.toString().trim())) {
            enterLoading();
            return;
        }
        String s = lastMoney.toString().trim();

        // 脱敏占位,原样显示
        if (MASK.equals(s)) {
            exitLoading();
            setText(MASK);
            return;
        }

        // 数字格式化;无法解析时 format 返回原串,直接展示
        exitLoading();
        setText(format(s));
    }

    /** 把数字字符串格式化为 千分位+两位小数;无法解析则原样返回 */
    private String format(String raw) {
        // 去掉可能已有的逗号和空格再解析
        String clean = raw.replace(",", "").replace(" ", "");
        try {
            BigDecimal bd = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
            return moneyFormat.format(bd);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    // ===== loading 态 =====

    private void enterLoading() {
        if (!loading) {
            loading = true;
            setText("");           // 清空文字,让高度仍占一行
            startLoadingAnim();
            requestLayout();
        }
        invalidate();
    }

    private void exitLoading() {
        if (loading) {
            loading = false;
            stopLoadingAnim();
            requestLayout();
        }
    }

    public boolean isLoading() {
        return loading;
    }

    /** loading 圈直径 = 文本单行最大高度(字体单行高度) */
    private int singleLineHeight() {
        Paint.FontMetrics fm = getPaint().getFontMetrics();
        return Math.round(fm.descent - fm.ascent);
    }

    private void startLoadingAnim() {
        if (loadingAnimator == null) {
            // 步进旋转:每帧切到下一根辐条,形成菊花转动效果
            loadingAnimator = ValueAnimator.ofInt(0, SPOKE_COUNT - 1);
            loadingAnimator.setDuration(800);
            loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            loadingAnimator.setInterpolator(new LinearInterpolator());
            loadingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    activeSpoke = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
        }
        if (!loadingAnimator.isStarted()) {
            loadingAnimator.start();
        }
    }

    private void stopLoadingAnim() {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!loading) {
            return;
        }
        // 保证有足够空间画下 loading 圈
        int d = singleLineHeight();
        int wantW = d + getPaddingLeft() + getPaddingRight();
        int wantH = d + getPaddingTop() + getPaddingBottom();
        int w = Math.max(getMeasuredWidth(), wantW);
        int h = Math.max(getMeasuredHeight(), wantH);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec),
                resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!loading) {
            super.onDraw(canvas);
            return;
        }
        drawLoading(canvas);
    }

    private void drawLoading(Canvas canvas) {
        int d = singleLineHeight();
        float cx = getPaddingLeft() + d / 2f;
        float cy = getHeight() / 2f;
        // 外半径留出线宽边距;内半径较大,辐条短粗(贴近 iOS 菊花)
        float outer = d / 2f - loadingStrokeWidth;
        float inner = outer * 0.58f;
        if (outer <= inner) {
            inner = outer * 0.4f;
        }

        int baseAlpha = 255;
        for (int i = 0; i < SPOKE_COUNT; i++) {
            // 距离当前亮点越远越淡(菊花的拖尾效果)
            int dist = (activeSpoke - i + SPOKE_COUNT) % SPOKE_COUNT;
            float ratio = 1f - (dist / (float) SPOKE_COUNT);
            loadingPaint.setAlpha(Math.max(40, (int) (baseAlpha * ratio)));

            double angle = Math.toRadians(i * (360.0 / SPOKE_COUNT) - 90);
            float sx = cx + (float) (Math.cos(angle) * inner);
            float sy = cy + (float) (Math.sin(angle) * inner);
            float ex = cx + (float) (Math.cos(angle) * outer);
            float ey = cy + (float) (Math.sin(angle) * outer);
            canvas.drawLine(sx, sy, ex, ey, loadingPaint);
        }
        loadingPaint.setAlpha(255);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (loading) {
            startLoadingAnim();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoadingAnim();
        super.onDetachedFromWindow();
    }
}
