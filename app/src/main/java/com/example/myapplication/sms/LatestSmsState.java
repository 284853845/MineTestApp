package com.example.myapplication.sms;

import java.util.Collections;
import java.util.List;

public class LatestSmsState {

    public enum Status {
        PERMISSION_REQUIRED,
        EMPTY,
        LOADED,
        ERROR
    }

    public final Status status;
    public final List<LatestSms> messages;
    public final String message;

    private LatestSmsState(Status status, List<LatestSms> messages, String message) {
        this.status = status;
        this.messages = messages;
        this.message = message;
    }

    public static LatestSmsState permissionRequired() {
        return new LatestSmsState(Status.PERMISSION_REQUIRED, null, "需要短信读取权限后显示最新短信");
    }

    public static LatestSmsState empty() {
        return new LatestSmsState(Status.EMPTY, null, "暂无短信");
    }

    public static LatestSmsState loaded(List<LatestSms> messages) {
        return new LatestSmsState(Status.LOADED,
                messages == null ? Collections.<LatestSms>emptyList() : messages,
                null);
    }

    public static LatestSmsState error() {
        return new LatestSmsState(Status.ERROR, null, "短信读取失败，请稍后重试");
    }
}
