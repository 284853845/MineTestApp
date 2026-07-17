package com.example.myapplication.list.model;

import com.example.myapplication.list.core.ModuleItem;

/** 左图右文模块 */
public class ImageTextModuleItem implements ModuleItem {
    public final int imageResId;
    public final String title;
    public final String desc;

    public ImageTextModuleItem(int imageResId, String title, String desc) {
        this.imageResId = imageResId;
        this.title = title;
        this.desc = desc;
    }
}
