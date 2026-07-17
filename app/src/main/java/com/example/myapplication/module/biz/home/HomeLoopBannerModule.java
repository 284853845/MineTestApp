package com.example.myapplication.module.biz.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.module.core.IndependentCardModule;
import com.example.myapplication.widget.LoopBannerView;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Independent home loop banner module.
 */
public class HomeLoopBannerModule extends IndependentCardModule<Map<String, Object>> {

    public static final String KEY = "home_loop_banner";

    private WeakReference<LoopBannerView<Map<String, Object>>> bannerViewRef;
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
        return R.layout.item_home_loop_banner;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBindView(ViewHolder holder, final Map<String, Object> data, int position) {
        final LoopBannerView<Map<String, Object>> bannerView =
                holder.getView(R.id.loop_banner_view);
        final List<Map<String, Object>> banners = data == null
                ? Collections.<Map<String, Object>>emptyList()
                : (List<Map<String, Object>>) data.get("banners");

        bannerViewRef = new WeakReference<>(bannerView);
        bannerView.setBannerAdapter(new LoopBannerView.BannerItemAdapter<Map<String, Object>>() {
            @Override
            public View onCreateView(ViewGroup parent) {
                return LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_home_loop_banner_cell, parent, false);
            }

            @Override
            public void onBindView(View view, Map<String, Object> banner, int pagePosition) {
                TextView title = view.findViewById(R.id.tv_loop_banner_title);
                TextView subtitle = view.findViewById(R.id.tv_loop_banner_subtitle);
                ImageView icon = view.findViewById(R.id.iv_loop_banner_icon);

                if (title != null) {
                    title.setText(String.valueOf(banner.get("title")));
                }
                if (subtitle != null) {
                    Object text = banner.get("subtitle");
                    subtitle.setText(text == null ? "" : String.valueOf(text));
                }
                if (icon != null) {
                    int resId = view.getResources().getIdentifier(
                            String.valueOf(banner.get("icon")),
                            "drawable",
                            view.getContext().getPackageName());
                    if (resId != 0) {
                        icon.setImageResource(resId);
                    } else {
                        icon.setImageDrawable(null);
                    }
                }
            }
        });
        bannerView.setOnPageSelectedListener(null);
        bannerView.setOnBannerClickListener(new LoopBannerView.OnBannerClickListener() {
            @Override
            public void onBannerClick(int pagePosition) {
                if (banners == null || banners.isEmpty()) {
                    return;
                }
                Map<String, Object> clicked = banners.get(pagePosition % banners.size());
                Toast.makeText(
                        bannerView.getContext(),
                        String.valueOf(clicked.get("title")),
                        Toast.LENGTH_SHORT).show();
            }
        });
        bannerView.setItems(banners == null
                ? Collections.<Map<String, Object>>emptyList() : banners);
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
        LoopBannerView<Map<String, Object>> bannerView =
                bannerViewRef == null ? null : bannerViewRef.get();
        if (bannerView == null) {
            return;
        }
        if (pageVisible && moduleExposed && viewAttached) {
            bannerView.resumeAutoScroll();
        } else {
            bannerView.pauseAutoScroll();
        }
    }
}
