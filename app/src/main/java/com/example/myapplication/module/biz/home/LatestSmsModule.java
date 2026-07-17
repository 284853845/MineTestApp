package com.example.myapplication.module.biz.home;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;
import com.example.myapplication.sms.LatestSms;
import com.example.myapplication.sms.LatestSmsRepository;
import com.example.myapplication.sms.LatestSmsState;
import com.example.myapplication.sms.SmsPermissionRequester;

import java.util.List;

public class LatestSmsModule extends IndependentCardModule<LatestSmsState> {

    public static final String KEY = "home_latest_sms";

    private LatestSmsRepository repository;
    private boolean destroyed;
    private int loadVersion;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    protected boolean isCacheEnabled() {
        return false;
    }

    @Override
    protected LatestSmsState parse(Object raw) {
        return (LatestSmsState) raw;
    }

    @Override
    protected void loadData(boolean isRefresh) {
        final int requestVersion = ++loadVersion;
        final Context context = getContext();
        if (!(context instanceof SmsPermissionRequester)
                || !((SmsPermissionRequester) context).hasReadSmsPermission()) {
            dispatchDataLoaded(LatestSmsState.permissionRequired());
            return;
        }
        if (repository == null) {
            repository = new LatestSmsRepository(context);
        }
        repository.loadLatest(new LatestSmsRepository.Callback() {
            @Override
            public void onResult(LatestSmsState state) {
                if (destroyed || requestVersion != loadVersion) {
                    return;
                }
                dispatchDataLoaded(state);
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.item_home_latest_sms;
    }

    @Override
    protected void onBindView(ViewHolder holder, LatestSmsState data, int position) {
        LinearLayout list = holder.getView(R.id.ll_latest_sms_list);
        TextView message = holder.getView(R.id.tv_latest_sms_message);
        TextView action = holder.getView(R.id.tv_latest_sms_action);

        list.removeAllViews();
        list.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);
        action.setVisibility(View.GONE);
        action.setOnClickListener(null);
        if (data == null) {
            message.setText("正在读取最新短信...");
            return;
        }
        switch (data.status) {
            case LOADED:
                bindSmsList(list, message, data.messages);
                break;
            case PERMISSION_REQUIRED:
                message.setText(data.message);
                action.setVisibility(View.VISIBLE);
                action.setText("授权读取短信");
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = v.getContext();
                        if (context instanceof SmsPermissionRequester) {
                            ((SmsPermissionRequester) context).requestReadSmsPermission();
                        }
                    }
                });
                break;
            case EMPTY:
                message.setText(data.message);
                break;
            case ERROR:
            default:
                message.setText(data.message);
                break;
        }
    }

    private void bindSmsList(LinearLayout list, TextView message, List<LatestSms> messages) {
        if (messages == null || messages.isEmpty()) {
            message.setVisibility(View.VISIBLE);
            message.setText("暂无短信");
            return;
        }
        message.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(list.getContext());
        for (int i = 0; i < messages.size(); i++) {
            LatestSms sms = messages.get(i);
            View row = inflater.inflate(R.layout.item_latest_sms_row, list, false);
            TextView index = row.findViewById(R.id.tv_latest_sms_row_index);
            TextView sender = row.findViewById(R.id.tv_latest_sms_row_sender);
            TextView time = row.findViewById(R.id.tv_latest_sms_row_time);
            TextView body = row.findViewById(R.id.tv_latest_sms_row_body);
            index.setText("#" + (i + 1));
            sender.setText(TextUtils.isEmpty(sms.address) ? "未知发件人" : sms.address);
            time.setText(formatTime(effectiveTime(sms))
                    + "  source=" + sms.source
                    + "  type=" + sms.type
                    + "  id=" + sms.id
                    + "  sent=" + formatTime(sms.dateSent));
            body.setText(TextUtils.isEmpty(sms.body) ? "短信内容为空" : sms.body);
            list.addView(row);
        }
    }

    private String formatTime(long timeMillis) {
        if (timeMillis <= 0L) {
            return "";
        }
        return DateFormat.format("yyyy-MM-dd HH:mm", timeMillis).toString();
    }

    private long effectiveTime(LatestSms sms) {
        return Math.max(sms.date, sms.dateSent);
    }

    @Override
    public void onModuleCreated() {
        destroyed = false;
        super.onModuleCreated();
    }

    @Override
    public void onModuleDestroyed() {
        destroyed = true;
        loadVersion++;
        super.onModuleDestroyed();
    }
}
