package com.example.myapplication.sms;

public class LatestSms {

    public final String source;
    public final long id;
    public final String address;
    public final String body;
    public final long date;
    public final long dateSent;
    public final int type;

    public LatestSms(String source, long id, String address, String body,
                     long date, long dateSent, int type) {
        this.source = source;
        this.id = id;
        this.address = address;
        this.body = body;
        this.date = date;
        this.dateSent = dateSent;
        this.type = type;
    }
}
