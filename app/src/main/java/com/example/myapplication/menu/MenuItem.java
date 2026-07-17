package com.example.myapplication.menu;

/** 菜单弹窗的一条数据:图片 + 文字 */
public class MenuItem {
    public final int iconResId;
    public final String text;

    public MenuItem(int iconResId, String text) {
        this.iconResId = iconResId;
        this.text = text;
    }
}
