package com.example.myapplication.list.delegate;

import com.example.myapplication.R;
import com.example.myapplication.list.core.ItemViewDelegate;
import com.example.myapplication.list.core.ModuleItem;
import com.example.myapplication.list.core.ViewHolder;
import com.example.myapplication.list.model.TextModuleItem;
import com.example.myapplication.widget.InfiniteBannerView;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/** 纯文本卡片 delegate */
public class TextDelegate implements ItemViewDelegate<TextModuleItem> {

    private static final int[] DEBUG_BANNER_COLORS = {
            Color.rgb(66, 165, 245),
            Color.rgb(255, 112, 67),
            Color.rgb(102, 187, 106),
            Color.rgb(171, 71, 188)
    };

    @Override
    public boolean isForViewType(ModuleItem item, int position) {
        return item instanceof TextModuleItem;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_text;
    }

    @Override
    public void onBind(ViewHolder holder, TextModuleItem item, int position) {
        ((TextView) holder.getView(R.id.tv_text_title)).setText(item.title);
        ((TextView) holder.getView(R.id.tv_text_content)).setText(item.content);
        InfiniteBannerView<String> bannerView = holder.getView(R.id.banner_view);
        bannerView.setBannerAdapter(new InfiniteBannerView.BannerItemAdapter<String>() {
            @Override
            public View onCreateView(ViewGroup parent) {
                TextView view = new TextView(parent.getContext());
                view.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                view.setGravity(Gravity.CENTER);
                view.setTextColor(Color.WHITE);
                view.setTextSize(18);
                return view;
            }

            @Override
            public void onBindView(View view, String item, int position) {
                ((TextView) view).setText(item);
                view.setBackgroundColor(DEBUG_BANNER_COLORS[position % DEBUG_BANNER_COLORS.length]);
            }
        });
        final java.util.List<String> banners = Arrays.asList("Banner 1", "Banner 2", "Banner 3", "Banner 4");
        bannerView.setItems(banners);
        bannerView.setOnBannerScrollListener(new InfiniteBannerView.OnBannerScrollListener() {
            @Override
            public void onScroll(int currentIndex, float offset) {
                // No-op. Only show a toast when a page is selected.
            }

            @Override
            public void onPageSelected(int currentIndex) {
                Toast.makeText(
                        bannerView.getContext(),
                        "滚动到第 " + (currentIndex + 1) + " 页: "
                                + banners.get(currentIndex % banners.size()),
                        Toast.LENGTH_SHORT).show();
            }
        });
        bannerView.setOnBannerClickListener(new InfiniteBannerView.OnBannerClickListener() {
            @Override
            public void onBannerClick(int currentIndex) {
                Toast.makeText(
                        bannerView.getContext(),
                        "点击第 " + (currentIndex + 1) + " 页: "
                                + banners.get(currentIndex % banners.size()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
