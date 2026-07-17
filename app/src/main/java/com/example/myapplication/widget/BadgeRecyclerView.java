package com.example.myapplication.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 带角标的 RecyclerView:像普通 RecyclerView 一样使用(setLayoutManager / setAdapter),
 * 角标由内部统一绘制在所有 item 之上,因此:
 *   - item 永远盖不住角标;
 *   - 角标可以超出 item 完整显示;
 *   - 角标之间按从左到右叠放(只有角标压角标)。
 *
 * 用法:
 * <pre>
 *   rv.setLayoutManager(new GridLayoutManager(ctx, 5));
 *   rv.setAdapter(adapter);
 *   rv.setBadgeAnchorId(R.id.iv_icon);              // 角标锚到 item 内哪个 View 的右上角(可选)
 *   rv.setBadgeProvider(new BadgeRecyclerView.BadgeProvider() {
 *       public View onCreateBadge(int position, BadgeRecyclerView parent) {
 *           if (没有角标) return null;               // 返回 null = 该 item 不显示角标
 *           return 任意自定义角标 View;               // 每个 position 可返回不同布局
 *       }
 *   });
 * </pre>
 *
 * 说明:角标用 onDrawOver 以 Canvas 形式绘制,是"展示型"浮层,本身不接收点击事件。
 */
public class BadgeRecyclerView extends RecyclerView {

    /** 角标提供器:为指定 position 提供角标 View;返回 null 表示该 item 无角标。 */
    public interface BadgeProvider {
        View onCreateBadge(int position, BadgeRecyclerView parent);
    }

    private BadgeProvider badgeProvider;

    /** 角标锚点子 View 的 id;为 0 时锚到整个 item。角标贴在锚点的右上角。 */
    private int anchorId = 0;
    /** 角标相对锚点右上角的偏移(px)。默认略向左上,使其压住锚点右上角。 */
    private int offsetX;
    private int offsetY;

    // position -> 角标 View 缓存;resolved 记录"已问过 provider"的 position(含返回 null 的)
    private final Map<Integer, View> badgeCache = new HashMap<>();
    private final Set<Integer> resolved = new HashSet<>();
    private final Rect tmpRect = new Rect();

    private final AdapterDataObserver dataObserver = new AdapterDataObserver() {
        @Override public void onChanged() { clearBadges(); }
        @Override public void onItemRangeChanged(int s, int c) { clearBadges(); }
        @Override public void onItemRangeChanged(int s, int c, Object p) { clearBadges(); }
        @Override public void onItemRangeInserted(int s, int c) { clearBadges(); }
        @Override public void onItemRangeRemoved(int s, int c) { clearBadges(); }
        @Override public void onItemRangeMoved(int f, int t, int c) { clearBadges(); }
    };

    public BadgeRecyclerView(Context c) { super(c); init(); }
    public BadgeRecyclerView(Context c, AttributeSet a) { super(c, a); init(); }
    public BadgeRecyclerView(Context c, AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        offsetX = -dp(15);
        offsetY = -dp(4);
        addItemDecoration(new BadgeDecoration());
    }

    /** 设置角标提供器:决定每个 item 是否有角标、以及角标长什么样。 */
    public void setBadgeProvider(BadgeProvider provider) {
        this.badgeProvider = provider;
        clearBadges();
    }

    /** 角标锚到 item 内某个子 View 的右上角(如图片 id);不设置则锚到整个 item。 */
    public void setBadgeAnchorId(int viewId) {
        this.anchorId = viewId;
        clearBadges();
    }

    /** 微调角标相对锚点右上角的偏移(px)。 */
    public void setBadgeOffset(int xPx, int yPx) {
        this.offsetX = xPx;
        this.offsetY = yPx;
        invalidate();
    }

    /** 角标内容/规则变化后,清缓存并重绘。 */
    public void notifyBadgesChanged() {
        clearBadges();
    }

    @Override
    public void setAdapter(Adapter adapter) {
        Adapter old = getAdapter();
        if (old != null) {
            try { old.unregisterAdapterDataObserver(dataObserver); } catch (Exception ignore) { }
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(dataObserver);
        }
        clearBadges();
    }

    private void clearBadges() {
        badgeCache.clear();
        resolved.clear();
        invalidate();
    }

    private View getBadge(int position) {
        if (resolved.contains(position)) {
            return badgeCache.get(position);
        }
        View v = (badgeProvider == null) ? null : badgeProvider.onCreateBadge(position, this);
        if (v != null) {
            measureBadge(v);
            badgeCache.put(position, v);
        }
        resolved.add(position);
        return v;
    }

    /** 用 UNSPECIFIED 测量,角标拿到完整自然尺寸,不被容器限制。 */
    private void measureBadge(View v) {
        int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        v.measure(spec, spec);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** 在所有 item 之上绘制角标。 */
    private class BadgeDecoration extends ItemDecoration {
        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            if (badgeProvider == null) {
                return;
            }
            // 倒序:左边(索引小)的角标后画,叠在最上层 -> 左压右
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                View child = parent.getChildAt(i);
                int pos = parent.getChildAdapterPosition(child);
                if (pos == NO_POSITION) {
                    continue;
                }
                View badge = getBadge(pos);
                if (badge == null) {
                    continue;
                }

                View anchor = (anchorId != 0) ? child.findViewById(anchorId) : child;
                if (anchor == null) {
                    anchor = child;
                }

                // 把锚点矩形换算到 RecyclerView 坐标系,取其右上角
                tmpRect.set(0, 0, anchor.getWidth(), anchor.getHeight());
                offsetDescendantRectToMyCoords(anchor, tmpRect);

                int x = tmpRect.right + offsetX;
                int y = tmpRect.top + offsetY;
                c.save();
                c.translate(x, y);
                badge.draw(c);
                c.restore();
            }
        }
    }
}
