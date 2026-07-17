package com.example.myapplication.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * 底部 Tab 栏。常规矩形背景,无凸起。
 *
 * <p><b>用法概览</b></p>
 * <ol>
 *     <li>用 {@link TabConfig} 描述每个 Tab 的 <b>文案、普通图标、选中图标(gif)、颜色</b>;</li>
 *     <li>调用 {@link #initTabs(int, List)} 一次性把这些样式装配好;</li>
 *     <li>之后选中/非选中的「图标显示与隐藏、文字颜色、加粗」全部由控件内部完成,
 *     使用方无需再写任何样式回调。</li>
 * </ol>
 *
 * <p>每个 Tab 的 xml 布局需包含以下控件(id 可通过 {@link TabViewIds} 自定义,默认见其常量):
 * 普通图标 {@code tab_icon}(ImageView)、选中图标 {@code tab_check_icon}
 * ({@link GifImageView})、文案 {@code tab_text}(TextView)。</p>
 *
 * <p>背景:可通过 {@link #setBackgroundLayout(int, BackgroundStyler)} 指定一个 xml 布局
 * 作为整个 Tab 栏的背景(铺满、位于 Tab 之下),并在选中变化时通过 {@link BackgroundStyler}
 * 回调自行改变背景。</p>
 */
public class BottomTabBar extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    /**
     * 背景样式回调。设置背景布局后、以及每次选中变化时调用,
     * 由使用方根据当前选中下标自行改变背景视图。
     */
    public interface BackgroundStyler {
        /**
         * @param backgroundView 背景布局的根 View
         * @param selectedIndex  当前选中的 Tab 下标
         */
        void onBind(View backgroundView, int selectedIndex);
    }

    /** 选中 Tab 处于回顶部态时被点击的回调。 */
    public interface OnTabBackToTopClickListener {
        void onBackToTop(int index);
    }

    /**
     * 每个 Tab 的样式描述。在 {@link #initTabs(int, List)} 时使用,装配一次即可。
     *
     * <p>必填:{@link #text}、{@link #normalIcon}、{@link #selectedIcon}。
     * 选填:{@link #normalColor}、{@link #selectedColor}、{@link #boldOnSelected}。</p>
     */
    public static class TabConfig {
        /** 文案 */
        public CharSequence text;
        /** 普通(非选中)图标,设置到普通 ImageView */
        @DrawableRes
        public int normalIcon;
        /** 选中图标(gif),设置到 {@link GifImageView};选中时从头播放一遍 */
        @RawRes
        public int selectedIcon;
        /** 非选中文字颜色 */
        @ColorInt
        public int normalColor = Color.parseColor("#000000");
        /** 选中文字颜色 */
        @ColorInt
        public int selectedColor = Color.parseColor("#FFEC1B30");
        /** 选中时文字是否加粗 */
        public boolean boldOnSelected = true;

        public TabConfig() {
        }

        public TabConfig(CharSequence text, @DrawableRes int normalIcon, @RawRes int selectedIcon) {
            this.text = text;
            this.normalIcon = normalIcon;
            this.selectedIcon = selectedIcon;
        }

        public TabConfig text(CharSequence text) {
            this.text = text;
            return this;
        }

        public TabConfig icons(@DrawableRes int normalIcon, @RawRes int selectedIcon) {
            this.normalIcon = normalIcon;
            this.selectedIcon = selectedIcon;
            return this;
        }

        public TabConfig colors(@ColorInt int normalColor, @ColorInt int selectedColor) {
            this.normalColor = normalColor;
            this.selectedColor = selectedColor;
            return this;
        }

        public TabConfig boldOnSelected(boolean bold) {
            this.boldOnSelected = bold;
            return this;
        }
    }

    /** Tab 布局中各控件的 id。不指定时使用与 {@code item_bottom_tab} 一致的默认 id。 */
    public static class TabViewIds {
        @IdRes
        public int normalIconId;
        @IdRes
        public int selectedIconId;
        @IdRes
        public int textId;

        public TabViewIds(@IdRes int normalIconId, @IdRes int selectedIconId, @IdRes int textId) {
            this.normalIconId = normalIconId;
            this.selectedIconId = selectedIconId;
            this.textId = textId;
        }
    }

    /** 承载各个 Tab 的行容器(位于背景之上)。 */
    private final LinearLayout tabRow;

    private View[] tabs = new View[0];
    private TabConfig[] configs = new TabConfig[0];
    private TabViewIds viewIds;
    private int selectedIndex = 0;
    private OnTabSelectedListener listener;

    private View backgroundView;
    private BackgroundStyler backgroundStyler;

    /** 当前选中 Tab 是否处于回顶部态(仅作用于选中 Tab) */
    private boolean selectedBackToTop;
    private OnTabBackToTopClickListener backToTopClickListener;

    public BottomTabBar(Context context) {
        this(context, null);
    }

    public BottomTabBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomTabBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 默认透明背景,使用方设置背景布局后会覆盖在其之上
        setBackgroundColor(Color.TRANSPARENT);

        tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setGravity(Gravity.CENTER);
        addView(tabRow, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /**
     * 设置整个 Tab 栏的背景布局(铺满整条栏,位于所有 Tab 之下)。
     * 设置后会立即回调一次 {@link BackgroundStyler} 以应用当前选中态的背景。
     *
     * @param layoutResId 背景的 xml 布局
     * @param styler      选中变化时改变背景的回调(可为 null)
     */
    public void setBackgroundLayout(@LayoutRes int layoutResId, @Nullable BackgroundStyler styler) {
        // 移除旧背景
        if (backgroundView != null) {
            removeView(backgroundView);
            backgroundView = null;
        }
        this.backgroundStyler = styler;

        backgroundView = LayoutInflater.from(getContext())
                .inflate(layoutResId, this, false);
        // 背景始终铺满,且加在最底层(index 0),保证位于 Tab 行之下
        addView(backgroundView, 0, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        if (backgroundStyler != null) {
            backgroundStyler.onBind(backgroundView, selectedIndex);
        }
    }

    /** 返回背景布局的根 View(未设置时为 null)。 */
    @Nullable
    public View getBackgroundView() {
        return backgroundView;
    }

    /**
     * 初始化 Tab(使用默认 id:{@code tab_icon}/{@code tab_check_icon}/{@code tab_text})。
     *
     * @param layoutResId 每个 Tab 的 xml 布局
     * @param tabConfigs  每个 Tab 的样式描述,装配一次
     * @see #initTabs(int, List, TabViewIds)
     */
    public void initTabs(@LayoutRes int layoutResId, List<TabConfig> tabConfigs) {
        initTabs(layoutResId, tabConfigs, defaultViewIds());
    }

    /**
     * 初始化 Tab,一次性把每个 Tab 的文案/普通图标/选中图标/颜色装配好。
     * 装配后:
     * <ul>
     *     <li>文案、普通图标、选中图标资源 <b>仅在此设置一次</b>;</li>
     *     <li>之后切换选中态时,控件内部只做「显示/隐藏对应图标 + 改文字颜色/加粗」,
     *     无需任何样式回调。</li>
     * </ul>
     *
     * @param layoutResId 每个 Tab 的 xml 布局
     * @param tabConfigs  每个 Tab 的样式描述
     * @param ids         Tab 布局中各控件的 id
     */
    public void initTabs(@LayoutRes int layoutResId, List<TabConfig> tabConfigs, TabViewIds ids) {
        if (tabConfigs == null || tabConfigs.isEmpty()) {
            return;
        }
        int tabCount = tabConfigs.size();
        this.viewIds = ids;
        this.configs = tabConfigs.toArray(new TabConfig[0]);
        this.tabs = new View[tabCount];
        this.selectedIndex = Math.min(selectedIndex, tabCount - 1);
        this.selectedBackToTop = false;
        tabRow.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < tabCount; i++) {
            View tab = inflater.inflate(layoutResId, tabRow, false);
            final int index = i;
            tab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 点击的是处于回顶部态的当前选中 Tab -> 触发回顶部,不重新选中
                    if (index == selectedIndex && selectedBackToTop) {
                        if (backToTopClickListener != null) {
                            backToTopClickListener.onBackToTop(index);
                        }
                        return;
                    }
                    selectTab(index);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            tabRow.addView(tab, lp);
            tabs[i] = tab;

            // 装配一次:文案 + 普通图标资源(选中图标资源在选中时按需设置以触发 gif 重播)
            applyStaticContent(tab, configs[i]);
            // 应用初始选中/非选中态(仅切换显示/隐藏 + 文字样式)
            applySelectedState(i, i == selectedIndex);
        }
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    /** 设置选中 Tab 处于回顶部态时的点击回调。 */
    public void setOnTabBackToTopClickListener(OnTabBackToTopClickListener l) {
        this.backToTopClickListener = l;
    }

    /**
     * 设置当前选中 Tab 是否处于回顶部态。回顶部态下:隐藏选中 gif,
     * 普通图标位置显示一个向上箭头,文案改为「回顶部」;还原时回到普通选中态。
     */
    public void setSelectedTabBackToTop(boolean backToTop) {
        if (selectedBackToTop == backToTop) {
            return;
        }
        selectedBackToTop = backToTop;
        if (selectedIndex < 0 || selectedIndex >= tabs.length) {
            return;
        }
        if (backToTop) {
            View tab = tabs[selectedIndex];
            View normalIcon = findNormalIcon(tab);
            View selectedIcon = findSelectedIcon(tab);
            TextView text = findText(tab);
            if (selectedIcon != null) {
                selectedIcon.setVisibility(GONE);
            }
            if (normalIcon instanceof ImageView) {
                ((ImageView) normalIcon).setImageResource(android.R.drawable.arrow_up_float);
            }
            if (normalIcon != null) {
                normalIcon.setVisibility(VISIBLE);
            }
            if (text != null) {
                text.setText("回顶部");
            }
        } else {
            // 还原普通选中态
            applyStaticContent(tabs[selectedIndex], configs[selectedIndex]);
            applySelectedState(selectedIndex, true);
        }
    }

    /** 当前选中 Tab 是否处于回顶部态。 */
    public boolean isSelectedBackToTop() {
        return selectedBackToTop;
    }

    /** 返回某个 Tab 的视图,便于使用方进一步操作(如设置徽标等)。 */
    @Nullable
    public View getTabView(int index) {
        if (index < 0 || index >= tabs.length) {
            return null;
        }
        return tabs[index];
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabs.length || index == selectedIndex) {
            return;
        }

        int oldIndex = selectedIndex;
        selectedIndex = index;
        // 切换 Tab 后回顶部态失效:新选中 Tab 默认普通选中态
        selectedBackToTop = false;

        applySelectedState(oldIndex, false);
        applySelectedState(index, true);

        // 通知背景随选中变化更新
        if (backgroundStyler != null && backgroundView != null) {
            backgroundStyler.onBind(backgroundView, index);
        }

        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    // ---- 内部:样式装配 ------------------------------------------------------

    /** 一次性内容:文案 + 普通图标资源。选中图标资源在切到选中态时再设置以触发 gif 重播。 */
    private void applyStaticContent(View tab, TabConfig config) {
        TextView text = findText(tab);
        if (text != null) {
            text.setText(config.text);
        }
        View normalIcon = findNormalIcon(tab);
        if (normalIcon instanceof ImageView && config.normalIcon != 0) {
            ((ImageView) normalIcon).setImageResource(config.normalIcon);
        }
    }

    /**
     * 切换某个 Tab 的选中/非选中态:只做「图标显示与隐藏」+ 文字颜色/加粗。
     * 选中时给 gif 重新设资源,从头播放一遍。
     */
    private void applySelectedState(int index, boolean selected) {
        if (index < 0 || index >= tabs.length) {
            return;
        }
        View tab = tabs[index];
        TabConfig config = configs[index];
        View normalIcon = findNormalIcon(tab);
        View selectedIcon = findSelectedIcon(tab);
        TextView text = findText(tab);

        if (selected) {
            if (selectedIcon instanceof GifImageView && config.selectedIcon != 0) {
                // 重新设资源 -> 从头播放一遍
                ((GifImageView) selectedIcon).setGifResource(config.selectedIcon);
            }
            if (selectedIcon != null) {
                selectedIcon.setVisibility(VISIBLE);
            }
            if (normalIcon != null) {
                normalIcon.setVisibility(GONE);
            }
        } else {
            if (selectedIcon != null) {
                selectedIcon.setVisibility(GONE);
            }
            if (normalIcon != null) {
                normalIcon.setVisibility(VISIBLE);
            }
        }

        if (text != null) {
            text.setTextColor(selected ? config.selectedColor : config.normalColor);
            int style = (selected && config.boldOnSelected) ? Typeface.BOLD : Typeface.NORMAL;
            text.setTypeface(Typeface.DEFAULT, style);
        }
    }

    private View findNormalIcon(View tab) {
        return tab.findViewById(viewIds.normalIconId);
    }

    private View findSelectedIcon(View tab) {
        return tab.findViewById(viewIds.selectedIconId);
    }

    private TextView findText(View tab) {
        return tab.findViewById(viewIds.textId);
    }

    private static TabViewIds defaultViewIds() {
        return new TabViewIds(
                com.example.myapplication.R.id.tab_icon,
                com.example.myapplication.R.id.tab_check_icon,
                com.example.myapplication.R.id.tab_text);
    }
}
