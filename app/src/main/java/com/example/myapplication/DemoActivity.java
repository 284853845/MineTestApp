package com.example.myapplication;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.example.myapplication.error.GlobalError;
import com.example.myapplication.loading.GlobalLoading;
import com.example.myapplication.security.CaptureAwareActivity;
import com.example.myapplication.sequential.SequentialExecutorManager;
import com.example.myapplication.sequential.SequentialTask;

/**
 * 功能 Demo 示例页：集中承载全局 Loading、全局错误弹窗、顺序执行器、LoadingTextView 等演示，
 * 从「我的」Tab 的设置项进入。原先这些演示散落在首页，现统一收拢到此页。
 */
public class DemoActivity extends CaptureAwareActivity {

    private final Handler demoHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function_demo);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btn_loading_single).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSingleTransaction();
            }
        });
        findViewById(R.id.btn_loading_concurrent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConcurrentTransactions();
            }
        });
        findViewById(R.id.btn_loading_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTransactionWithMessage();
            }
        });
        findViewById(R.id.btn_loading_force).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlobalLoading.dismissAll(DemoActivity.this);
            }
        });

        findViewById(R.id.btn_error_single).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSingleError();
            }
        });
        findViewById(R.id.btn_error_concurrent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConcurrentErrors();
            }
        });

        findViewById(R.id.btn_sequential_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSequentialDemo();
            }
        });
    }

    /** 单笔交易:显示 loading,2 秒后结束。 */
    private void startSingleTransaction() {
        GlobalLoading.show(this);
        demoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GlobalLoading.hide(DemoActivity.this);
            }
        }, 2000);
    }

    /**
     * 并发 3 笔交易:各自 show,分别在 2s/3s/4s 结束。
     * loading 会一直显示,直到最后一笔(4s)结束才关闭,演示引用计数。
     */
    private void startConcurrentTransactions() {
        long[] durations = {2000, 3000, 4000};
        for (long d : durations) {
            GlobalLoading.show(this);
            demoHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GlobalLoading.hide(DemoActivity.this);
                }
            }, d);
        }
        Toast.makeText(this, "已发起 3 笔交易,4 秒后才会关闭 loading", Toast.LENGTH_SHORT).show();
    }

    /** 带自定义文案的交易:显示"支付中",2 秒后结束。 */
    private void startTransactionWithMessage() {
        GlobalLoading.show(this, "支付中");
        demoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GlobalLoading.hide(DemoActivity.this);
            }
        }, 2000);
    }

    /** 单个错误:直接弹一个错误弹窗。 */
    private void startSingleError() {
        GlobalError.show(this, "交易失败", "网络异常，请稍后重试");
    }

    /**
     * 并发报错:连续触发 3 个错误,验证「已有弹窗时后续丢弃」——最终只会弹出第 1 个,
     * 关闭它之后也不会再补弹第 2、3 个。
     */
    private void startConcurrentErrors() {
        GlobalError.show(this, "交易失败", "错误 1：余额不足");
        GlobalError.show(this, "交易失败", "错误 2：超出限额");
        GlobalError.show(this, "交易失败", "错误 3：风控拦截");
        Toast.makeText(this, "已触发 3 个错误,只会弹出第 1 个", Toast.LENGTH_SHORT).show();
    }

    /**
     * 顺序执行器用例:一次性入队 3 个弹窗任务,它们会一个接一个地弹出,
     * 前一个消失后才弹下一个。中途退出/销毁本页,剩余弹窗不再弹出。
     */
    private void startSequentialDemo() {
        SequentialExecutorManager manager = SequentialExecutorManager.get();
        manager.enqueue(this, new DialogTask("第 1 个弹窗", "关闭后会自动弹出第 2 个"));
        manager.enqueue(this, new DialogTask("第 2 个弹窗", "关闭后会自动弹出第 3 个"));
        manager.enqueue(this, new DialogTask("第 3 个弹窗", "这是最后一个"));
        Toast.makeText(this, "已入队 3 个弹窗,将依次弹出", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        demoHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
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
        public void execute(@NonNull android.app.Activity host, @NonNull final Callback callback) {
            dialog = new AlertDialog.Builder(host)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("下一个", null)
                    .create();
            // dialog 消失(点按钮、返回键、外部点击)统一触发完成,推进队列。
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface d) {
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
}
