package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.example.myapplication.page.HomePageFragment;
import com.example.myapplication.page.LifePageFragment;
import com.example.myapplication.page.LoanPageFragment;
import com.example.myapplication.page.ModulePageFragment;
import com.example.myapplication.page.ProfilePageFragment;
import com.example.myapplication.page.WealthPageFragment;
import com.example.myapplication.security.CaptureAwareActivity;
import com.example.myapplication.sms.SmsPermissionRequester;
import com.example.myapplication.widget.BottomTabBar;
import com.example.myapplication.widget.SliderBar;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用宿主。底部 5 个 Tab,每个 Tab 对应一个模块化页面 Fragment。
 * 用 show/hide 切换,Fragment 状态完整保留,从而能精确驱动各页面的
 * "重新可见"回调(ModulePageFragment#onHiddenChanged)。
 */
public class MainActivity extends CaptureAwareActivity implements SmsPermissionRequester {

    private static final int REQUEST_READ_SMS_PERMISSION = 5301;

    /**
     * 由桌面小组件菜单传入，指定打开后切到哪个 Tab 下标
     */
    public static final String EXTRA_OPEN_TAB = "extra_open_tab";

    private static final String[] TAB_NAMES = {
            "首页",   // 首页
            "财富",   // 财富
            "贷款",   // 贷款
            "生活",   // 生活
            "我的"    // 我的
    };

    private int currentIndex = -1;
    private BottomTabBar bottomTabBar;
    /** 缓存 5 个 Tab 的 Fragment,首次切到才创建,之后只做 show/hide */
    private final ModulePageFragment[] fragments = new ModulePageFragment[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 恢复重建场景:FragmentManager 已自动重建过的 Fragment 按 tag 找回,避免重复 add
        restoreFragments();
        initBottomTabs();
        // 默认进入首页
        switchTab(0);
        // 若由小组件菜单带 Tab 进入，则切到目标 Tab
        applyOpenTabIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyOpenTabIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_READ_SMS_PERMISSION) {
            return;
        }
        notifyHomeSmsPermissionChanged();
    }

    @Override
    public boolean hasReadSmsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestReadSmsPermission() {
        if (hasReadSmsPermission()) {
            notifyHomeSmsPermissionChanged();
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_SMS},
                REQUEST_READ_SMS_PERMISSION);
    }

    private void notifyHomeSmsPermissionChanged() {
        ModulePageFragment fragment = fragments[0];
        if (fragment instanceof HomePageFragment) {
            ((HomePageFragment) fragment).onReadSmsPermissionChanged();
        }
    }

    /**
     * 处理小组件深链：切到指定 Tab（同时驱动底部栏视觉与 Fragment 切换）
     */
    private void applyOpenTabIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        int tab = intent.getIntExtra(EXTRA_OPEN_TAB, -1);
        if (tab < 0 || tab >= TAB_NAMES.length) {
            return;
        }
        if (tab == currentIndex) {
            return;
        }
        if (bottomTabBar != null) {
            // selectTab 会触发监听器进而调用 switchTab，并带动画切换底部栏选中态
            bottomTabBar.selectTab(tab);
        } else {
            switchTab(tab);
        }
    }

    private static final int[] TAB_ICONS = {
            R.drawable.sy_check,
            R.drawable.cf_check,
            R.drawable.dk_check,
            R.drawable.sh_check,
            R.drawable.wd_check,
    };

    private static final int[] TAB_CHECK_ICONS = {
            R.drawable.sy_gif,
            R.drawable.cf_gif,
            R.drawable.dk_gif,
            R.drawable.sh_gif,
            R.drawable.wd_gif,
    };

    /**
     * 选中色 / 非选中色
     */
    private static final int TAB_ACTIVE_COLOR = Color.parseColor("#FFEC1B30");
    private static final int TAB_NORMAL_COLOR = Color.parseColor("#000000");

    private void initBottomTabs() {
        bottomTabBar = findViewById(R.id.bottom_tab_bar);

        // 一次性装配每个 Tab 的文案、普通图标、选中图标(gif)与颜色;
        // 之后选中/非选中的图标显示与隐藏、文字样式全部由控件内部处理。
        List<BottomTabBar.TabConfig> configs = new ArrayList<>();
        for (int i = 0; i < TAB_NAMES.length; i++) {
            BottomTabBar.TabConfig config = new BottomTabBar.TabConfig(
                    TAB_NAMES[i], TAB_ICONS[i], TAB_CHECK_ICONS[i]);
            config.colors(TAB_NORMAL_COLOR, TAB_ACTIVE_COLOR);
            configs.add(config);
        }
        bottomTabBar.initTabs(R.layout.item_bottom_tab, configs);

        bottomTabBar.setOnTabSelectedListener(new BottomTabBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int index) {
                switchTab(index);
            }
        });
        bottomTabBar.setBackgroundLayout(R.layout.layout_tab_bg, new BottomTabBar.BackgroundStyler() {
            @Override
            public void onBind(View backgroundView, int selectedIndex) {
                SliderBar bar = backgroundView.findViewById(R.id.sb);
                bar.setSelectedIndex(selectedIndex);
            }
        });
        // 点击处于回顶部态的选中 Tab -> 当前页面列表回顶部
        bottomTabBar.setOnTabBackToTopClickListener(new BottomTabBar.OnTabBackToTopClickListener() {
            @Override
            public void onBackToTop(int index) {
                if (currentIndex >= 0 && fragments[currentIndex] != null) {
                    fragments[currentIndex].scrollToTop();
                }
            }
        });
    }

    /** 给页面挂上滚动阈值监听:仅当它是当前选中页时,驱动底部栏回顶部态。 */
    private void bindScrollThreshold(final ModulePageFragment fragment, final int index) {
        if (fragment == null) {
            return;
        }
        fragment.setOnScrollThresholdListener(new ModulePageFragment.OnScrollThresholdListener() {
            @Override
            public void onScrollThreshold(boolean beyond) {
                if (index == currentIndex) {
                    bottomTabBar.setSelectedTabBackToTop(beyond);
                }
            }
        });
    }

    /**
     * Activity 重建(如旋转)后,FragmentManager 会自动重建之前 add 过的 Fragment。
     * 这里按 tag 把它们找回填进缓存数组,并全部先 hide,避免重复创建与多页叠加显示。
     */
    private void restoreFragments() {
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();
        boolean any = false;
        for (int i = 0; i < fragments.length; i++) {
            Fragment f = fm.findFragmentByTag("tab_" + i);
            if (f instanceof ModulePageFragment) {
                fragments[i] = (ModulePageFragment) f;
                bindScrollThreshold(fragments[i], i);
                ft.hide(f);
                any = true;
            }
        }
        if (any) {
            ft.commitAllowingStateLoss();
        }
    }

    /**
     * 切换 Tab:每个页面只创建一次。
     * 首次切到某 Tab 时 add 其 Fragment,之后仅 show/hide,
     * 旧页面隐藏但不销毁,状态完整保留。
     */
    private void switchTab(int index) {
        if (index == currentIndex) {
            return;
        }
        android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction ft = fm.beginTransaction();

        // 隐藏当前页面
        if (currentIndex >= 0 && fragments[currentIndex] != null) {
            ft.hide(fragments[currentIndex]);
        }

        ModulePageFragment target = fragments[index];
        if (target == null) {
            // 首次进入:创建并 add
            target = createFragment(index);
            fragments[index] = target;
            bindScrollThreshold(target, index);
            ft.add(R.id.fragment_container, target, "tab_" + index);
        } else {
            // 已创建:直接显示
            ft.show(target);
        }

        ft.commitAllowingStateLoss();
        currentIndex = index;

        // 切到新页面后,按该页当前滚动状态同步底部栏回顶部态
        // (页面 show/hide 复用,滚动位置保留;之前滚动过的页面切回应立即显示回顶部态)
        bottomTabBar.setSelectedTabBackToTop(target.isScrolledBeyondThreshold());
    }

    private ModulePageFragment createFragment(int index) {
        switch (index) {
            case 0:
                return new HomePageFragment();
            case 1:
                return new WealthPageFragment();
            case 2:
                return new LoanPageFragment();
            case 3:
                return new LifePageFragment();
            case 4:
            default:
                return new ProfilePageFragment();
        }
    }
}
