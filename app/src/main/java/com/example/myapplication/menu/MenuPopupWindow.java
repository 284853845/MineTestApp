package com.example.myapplication.menu;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.example.myapplication.R;

import java.util.List;

/**
 * 封装好的菜单弹窗。
 * 使用方只需传入数据 + 点击回调,点击某条后自动消失。
 */
public class MenuPopupWindow extends PopupWindow {

    public interface OnMenuClickListener {
        void onMenuClick(MenuItem item, int position);
    }

    public MenuPopupWindow(Context context, List<MenuItem> items,
                           final OnMenuClickListener clickListener) {
        super(context);
        View content = LayoutInflater.from(context)
                .inflate(R.layout.popup_menu_list, null);
        RecyclerView recyclerView = content.findViewById(R.id.rv_menu);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        MenuAdapter adapter = new MenuAdapter(items);
        adapter.setOnItemClickListener(new MenuAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MenuItem item, int position) {
                if (clickListener != null) {
                    clickListener.onMenuClick(item, position);
                }
                dismiss();
            }
        });
        recyclerView.setAdapter(adapter);

        setContentView(content);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT));
        setElevation(dp(context, 8));
    }

    /**
     * 在锚点正下方弹出;下方放不下则放正上方。
     * 水平方向默认与按钮居中对齐;若会超出屏幕,则贴近对应一边,
     * 但左右始终与屏幕保留 10dp 边距。
     */
    public void showUnderAnchor(View anchor) {
        final Context context = anchor.getContext();
        final View content = getContentView();

        // 先测量弹窗内容的实际宽高
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        content.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST));
        int popupWidth = content.getMeasuredWidth();
        int popupHeight = content.getMeasuredHeight();

        int margin = dp(context, 10);
        int gap = dp(context, 4);
        // 弹窗内容四周留了 10dp 给阴影,计算时要把这层 padding 抵消掉,
        // 让"卡片本体"而不是阴影区域来对齐边距/间隙
        int shadowPad = dp(context, 10);

        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int anchorLeft = loc[0];
        int anchorTop = loc[1];
        int anchorBottom = anchorTop + anchor.getHeight();
        int anchorCenterX = anchorLeft + anchor.getWidth() / 2;

        // 水平:默认与按钮居中对齐(popupWidth 含阴影,居中公式不变)
        int x = anchorCenterX - popupWidth / 2;
        // 卡片本体距屏幕保留 10dp,换算到含阴影的弹窗坐标即抵消一层 shadowPad
        int minX = margin - shadowPad;
        int maxX = screenWidth - margin - popupWidth + shadowPad;
        if (x > maxX) {
            x = maxX;
        }
        if (x < minX) {
            x = minX;
        }

        // 垂直:优先正下方,放不下则放正上方;同样抵消阴影 padding
        int y;
        boolean below;
        boolean enoughBelow = anchorBottom + gap + (popupHeight - shadowPad) <= screenHeight;
        if (enoughBelow) {
            y = anchorBottom + gap - shadowPad;
            below = true;
        } else {
            y = anchorTop - gap - popupHeight + shadowPad;
            if (y < 0) {
                y = 0;
            }
            below = false;
        }

        // 显示对应方向的三角,并水平对准按钮中心
        layoutTriangle(content, below, x, anchorCenterX, shadowPad);

        showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    /**
     * 选中紧挨按钮那一侧的三角并让其尖角对准按钮水平中心。
     * @param below   弹窗是否在按钮下方(下方时显示上三角,上方时显示下三角)
     * @param popupX  弹窗(含阴影)在屏幕上的左边坐标
     */
    private void layoutTriangle(View content, boolean below, int popupX,
                                int anchorCenterX, int shadowPad) {
        TriangleView top = content.findViewById(R.id.triangle_top);
        TriangleView bottom = content.findViewById(R.id.triangle_bottom);
        View card = content.findViewById(R.id.rv_menu);

        TriangleView active = below ? top : bottom;
        TriangleView hidden = below ? bottom : top;
        hidden.setVisibility(View.GONE);
        active.setVisibility(View.VISIBLE);
        active.setPointUp(below);

        int triangleWidth = active.getLayoutParams().width;
        int cardWidth = card.getLayoutParams().width;
        // 卡片本体左边在屏幕上的坐标
        int cardLeft = popupX + shadowPad;
        // 让三角中心对准按钮中心,换算成相对卡片左边的偏移
        int desiredLeft = anchorCenterX - cardLeft - triangleWidth / 2;
        // 限制三角不超出卡片(两端各留 8dp,避开圆角)
        int inset = dp(content.getContext(), 8);
        int minLeft = inset;
        int maxLeft = cardWidth - triangleWidth - inset;
        if (desiredLeft < minLeft) {
            desiredLeft = minLeft;
        }
        if (desiredLeft > maxLeft) {
            desiredLeft = maxLeft;
        }
        active.setTranslationX(desiredLeft);
    }

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
