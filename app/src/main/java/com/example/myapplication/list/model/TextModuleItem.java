package com.example.myapplication.list.model;

import com.example.myapplication.list.core.ModuleItem;

/** 纯文本卡片模块 */
public class TextModuleItem implements ModuleItem {
    public final String title;
    public final String content;

    public TextModuleItem(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
