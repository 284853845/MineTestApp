package com.example.myapplication.page;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.databinding.LayoutHomePageBinding;
import com.example.myapplication.loading.GlobalLoading;
import com.example.myapplication.sequential.SequentialExecutorManager;
import com.example.myapplication.sequential.SequentialTask;
import com.example.myapplication.widget.SecondFloorLayout;
import com.example.myapplication.widget.SecondFloorView;

/**
 * 首页:下拉刷新 + 下拉进入二楼。
 * 二楼需要覆盖包括 BottomTabBar 在内的整屏内容,所以会挂到 Activity content 上。
 */
public class HomePageFragment extends ModulePageFragment {

    private LayoutHomePageBinding binding;
    private SecondFloorView secondFloorView;

    @Override
    protected String getPageId() {
        return "home";
    }

    @Override
    protected boolean isSecondFloorEnabled() {
        return true;
    }

    @Override
    protected boolean isRefreshEnabled() {
        return true;
    }

    @Override
    protected boolean shouldKeepTopOnInitialLoad() {
        return true;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.layout_home_page;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = LayoutHomePageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void bindPageViews(View view) {
        secondFloorLayout = binding.secondFloorLayout;
        recyclerView = binding.rvModules;
        binding.tvLoadingDemo.setDotsAnimEnabled(true);
    }

    @Override
    protected void onPageViewReady(View view) {
        secondFloorView = binding.secondFloorView;
        moveSecondFloorToActivityRoot(secondFloorView);

        secondFloorLayout.setOnSecondFloorListener(new SecondFloorLayout.OnSecondFloorListener() {
            @Override
            public void onEnterSecondFloor(SecondFloorLayout layout) {
                secondFloorView.show();
            }
        });
        binding.secondFloorContent.btnBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                secondFloorView.hide();
            }
        });

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                scrollToTopImmediately();
            }
        });

        binding.btnSequentialDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSequentialDemo();
            }
        });

        binding.btnLoadingSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSingleTransaction();
            }
        });
        binding.btnLoadingConcurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConcurrentTransactions();
            }
        });
        binding.btnLoadingMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTransactionWithMessage();
            }
        });
        binding.btnLoadingForce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalLoading.dismissAll(getActivity());
            }
        });
    }

    private final Handler demoHandler = new Handler(Looper.getMainLooper());

    /** 单笔交易:显示 loading,2 秒后结束。 */
    private void startSingleTransaction() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        GlobalLoading.show(activity);
        demoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GlobalLoading.hide(activity);
            }
        }, 2000);
    }

    /**
     * 并发 3 笔交易:各自 show,分别在 2s/3s/4s 结束。
     * loading 会一直显示,直到最后一笔(4s)结束才关闭,演示引用计数。
     */
    private void startConcurrentTransactions() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        long[] durations = {2000, 3000, 4000};
        for (long d : durations) {
            GlobalLoading.show(activity);
            demoHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GlobalLoading.hide(activity);
                }
            }, d);
        }
        Toast.makeText(activity, "已发起 3 笔交易,4 秒后才会关闭 loading", Toast.LENGTH_SHORT).show();
    }

    /** 带自定义文案的交易:显示"支付中",2 秒后结束。 */
    private void startTransactionWithMessage() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        GlobalLoading.show(activity, "支付中");
        demoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GlobalLoading.hide(activity);
            }
        }, 2000);
    }

    /**
     * 顺序执行器用例:一次性入队 3 个弹窗任务,它们会一个接一个地弹出,
     * 前一个消失后才弹下一个。中途退出/销毁首页所在 Activity,剩余弹窗不再弹出。
     */
    private void startSequentialDemo() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        SequentialExecutorManager manager = SequentialExecutorManager.get();
        manager.enqueue(activity, new DialogTask("第 1 个弹窗", "关闭后会自动弹出第 2 个"));
        manager.enqueue(activity, new DialogTask("第 2 个弹窗", "关闭后会自动弹出第 3 个"));
        manager.enqueue(activity, new DialogTask("第 3 个弹窗", "这是最后一个"));
        Toast.makeText(activity, "已入队 3 个弹窗,将依次弹出", Toast.LENGTH_SHORT).show();
    }

    /** 一个把 AlertDialog 包成顺序任务的示例:dismiss 时回调完成,cancel 时主动关闭。 */
    private static final class DialogTask implements SequentialTask {

        private final String title;
        private final String message;
        private AlertDialog dialog;

        DialogTask(String title, String message) {
            this.title = title;
            this.message = message;
        }

        @Override
        public void execute(@NonNull Activity host, @NonNull final Callback callback) {
            dialog = new AlertDialog.Builder(host)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("下一个", null)
                    .create();
            // dialog 消失(点按钮、返回键、外部点击)统一触发完成,推进队列。
            dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(android.content.DialogInterface d) {
                    callback.onComplete();
                }
            });
            dialog.show();
        }

        @Override
        public void cancel() {
            // 执行器被清空(如页面销毁)时主动关闭当前弹窗。
            if (dialog != null) {
                dialog.setOnDismissListener(null); // 避免 cancel 引发的 dismiss 再回调
                dialog.dismiss();
                dialog = null;
            }
        }
    }

    public void onReadSmsPermissionChanged() {
        if (moduleManager != null) {
            moduleManager.refresh();
        }
    }

    private void moveSecondFloorToActivityRoot(SecondFloorView floor) {
        Activity activity = getActivity();
        if (floor == null || activity == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) floor.getParent();
        if (parent != null) {
            parent.removeView(floor);
        }
        activity.addContentView(floor, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onDestroyView() {
        demoHandler.removeCallbacksAndMessages(null);
        if (secondFloorView != null) {
            ViewGroup parent = (ViewGroup) secondFloorView.getParent();
            if (parent != null) {
                parent.removeView(secondFloorView);
            }
            secondFloorView = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
