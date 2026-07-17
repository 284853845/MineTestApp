package com.example.myapplication.sms;

public interface SmsPermissionRequester {

    boolean hasReadSmsPermission();

    void requestReadSmsPermission();
}
