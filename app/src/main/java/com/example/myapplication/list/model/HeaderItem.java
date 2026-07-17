package com.example.myapplication.list.model;

import com.example.myapplication.list.core.ModuleItem;

/** 列表顶部的独立头部 */
public class HeaderItem implements ModuleItem {
    public final String title;
    public final String subtitle;

    public HeaderItem(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }
}
