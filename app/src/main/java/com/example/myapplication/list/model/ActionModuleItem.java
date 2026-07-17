package com.example.myapplication.list.model;

import com.example.myapplication.list.core.ModuleItem;

/** 带按钮的操作模块 */
public class ActionModuleItem implements ModuleItem {

    public interface OnActionClick {
        void onClick(ActionModuleItem item);
    }

    public final String title;
    public final String buttonText;
    public final OnActionClick callback;

    public ActionModuleItem(String title, String buttonText, OnActionClick callback) {
        this.title = title;
        this.buttonText = buttonText;
        this.callback = callback;
    }
}
