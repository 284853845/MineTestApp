package com.example.myapplication.module.biz.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;
import com.example.myapplication.widget.InfiniteBannerView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BannerModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "home_banner";
    private static final int[] DEBUG_BANNER_COLORS = {
            Color.rgb(66, 165, 245),
            Color.rgb(255, 112, 67),
            Color.rgb(102, 187, 106),
            Color.rgb(171, 71, 188),
            Color.rgb(255, 202, 40)
    };

    private WeakReference<InfiniteBannerView<Map<String, Object>>> bannerViewRef;
    private boolean pageVisible;
    private boolean moduleExposed;
    private boolean viewAttached;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parse(Object raw) {
        return (Map<String, Object>) raw;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.item_home_banner;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, final Map<String, Object> data, int position) {
        final InfiniteBannerView<Map<String, Object>> banner =
                holder.getView(R.id.banner_view);
        bannerViewRef = new WeakReference<>(banner);

        banner.setBannerAdapter(new InfiniteBannerView.BannerItemAdapter<Map<String, Object>>() {
            @Override
            public View onCreateView(ViewGroup parent) {
                return LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_home_banner_cell, parent, false);
            }

            @Override
            public void onBindView(View view, Map<String, Object> item, int pos) {
                view.setBackgroundColor(DEBUG_BANNER_COLORS[pos % DEBUG_BANNER_COLORS.length]);

                TextView title = view.findViewById(R.id.tv_banner_title);
                if (title != null) {
                    title.setText(String.valueOf(item.get("title")));
                }

                TextView subtitle = view.findViewById(R.id.tv_banner_subtitle);
                if (subtitle != null) {
                    Object sub = item.get("subtitle");
                    subtitle.setText(sub == null ? "" : String.valueOf(sub));
                }

                ImageView icon = view.findViewById(R.id.iv_banner_icon);
                if (icon != null) {
                    String iconName = String.valueOf(item.get("icon"));
                    int resId = view.getResources().getIdentifier(
                            iconName, "drawable", view.getContext().getPackageName());
                    if (resId != 0) {
                        icon.setImageResource(resId);
                    }
                }
            }
        });

        final List<Map<String, Object>> banners =
                (List<Map<String, Object>>) data.get("banners");
        banner.setItems(banners == null
                ? Collections.<Map<String, Object>>emptyList() : banners);
        banner.setOnBannerScrollListener(null);
        banner.setOnBannerClickListener(new InfiniteBannerView.OnBannerClickListener() {
            @Override
            public void onBannerClick(int currentIndex) {
                if (banners == null || banners.isEmpty()) {
                    return;
                }
                Map<String, Object> clicked = banners.get(currentIndex % banners.size());
                Toast.makeText(
                        banner.getContext(),
                        "点击第" + (currentIndex + 1) + "页 "
                                + String.valueOf(clicked.get("title")),
                        Toast.LENGTH_SHORT).show();
            }
        });
        syncBannerPlayback();
    }

    @Override
    public void onPageVisible() {
        pageVisible = true;
        syncBannerPlayback();
    }

    @Override
    public void onPageInvisible() {
        pageVisible = false;
        syncBannerPlayback();
    }

    @Override
    public void onModuleExposed() {
        moduleExposed = true;
        syncBannerPlayback();
    }

    @Override
    public void onModuleHidden() {
        moduleExposed = false;
        syncBannerPlayback();
    }

    @Override
    protected void onViewAttached(ViewHolder holder) {
        viewAttached = true;
        syncBannerPlayback();
    }

    @Override
    protected void onViewDetached(ViewHolder holder) {
        viewAttached = false;
        syncBannerPlayback();
    }

    @Override
    protected void onViewRecycled(ViewHolder holder) {
        viewAttached = false;
        syncBannerPlayback();
        bannerViewRef = null;
    }

    @Override
    public void onModuleDestroyed() {
        pageVisible = false;
        moduleExposed = false;
        viewAttached = false;
        syncBannerPlayback();
        bannerViewRef = null;
    }

    private void syncBannerPlayback() {
        InfiniteBannerView<Map<String, Object>> banner =
                bannerViewRef == null ? null : bannerViewRef.get();
        if (banner == null) {
            return;
        }
        banner.setAutoScroll(pageVisible && moduleExposed && viewAttached);
    }
}
