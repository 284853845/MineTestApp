package com.example.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;

import com.example.myapplication.R;

/**
 * 自定义文本:当字符串中含有 '%' 时,自动缩小所有 '%' 字符的字号。
 *
 * '%' 的字号通过 XML 属性 app:percentTextSize 设置(像素/sp 单位)。
 * 未设置时默认为正文字号的 0.6 倍。
 *
 * 用法:
 *   <com.example.myapplication.widget.PercentTextView
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content"
 *       android:textSize="24sp"
 *       app:percentTextSize="12sp"
 *       android:text="收益率 12.5%" />
 */
public class PercentTextView extends AppCompatTextView {

    private static final char PERCENT = '%';
    /** 未显式设置 percentTextSize 时,相对正文字号的缩放比例 */
    private static final float DEFAULT_SCALE = 0.6f;

    /** '%' 的目标字号(px);<=0 表示未设置,按 DEFAULT_SCALE 计算 */
    private int percentTextSizePx = 0;

    public PercentTextView(Context context) {
        this(context, null);
    }

    public PercentTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PercentTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PercentTextView);
            percentTextSizePx = a.getDimensionPixelSize(
                    R.styleable.PercentTextView_percentTextSize, 0);
            a.recycle();
        }
        // 用当前已设置好的文本走一遍渲染
        applyPercentSpan(getText());
    }

    /** 代码动态设置 '%' 字号(px) */
    public void setPercentTextSizePx(int px) {
        this.percentTextSizePx = px;
        applyPercentSpan(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        // 拦截所有 setText,统一加上缩小 '%' 的 span
        CharSequence styled = buildPercentText(text);
        super.setText(styled, type);
    }

    /** 重新渲染当前文本(字号变化后调用) */
    private void applyPercentSpan(CharSequence text) {
        setText(text, BufferType.SPANNABLE);
    }

    private CharSequence buildPercentText(CharSequence source) {
        if (TextUtils.isEmpty(source) || source.toString().indexOf(PERCENT) < 0) {
            return source;
        }
        int sizePx = resolvePercentSizePx();
        if (sizePx <= 0) {
            return source;
        }
        SpannableString sp = new SpannableString(source);
        String s = source.toString();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == PERCENT) {
                sp.setSpan(new AbsoluteSizeSpan(sizePx),
                        i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sp;
    }

    /** 计算最终使用的 '%' 字号(px) */
    private int resolvePercentSizePx() {
        if (percentTextSizePx > 0) {
            return percentTextSizePx;
        }
        // 未设置则按正文字号的固定比例缩小
        return Math.round(getTextSize() * DEFAULT_SCALE);
    }
}
