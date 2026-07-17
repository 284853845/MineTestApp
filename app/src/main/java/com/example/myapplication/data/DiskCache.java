package com.example.myapplication.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 模块数据的磁盘缓存。
 *
 * - 序列化:用 Java 原生 ObjectStream,数据本身是 HashMap/ArrayList/String/Boolean
 *   等天然 Serializable 的类型,无需引入 Gson 等三方库。
 * - 线程:读写都在单线程后台执行(保证同一 key 的写顺序),结果切回主线程回调。
 * - 健壮性:读出损坏/不兼容的缓存时,删除该文件并回调 null,不影响主流程。
 *
 * 缓存文件:{filesDir}/module_cache/{key}.cache
 */
public class DiskCache {

    private static final String TAG = "DiskCache";
    private static final String DIR = "module_cache";

    /** 异步读取回调,主线程触发 */
    public interface ReadCallback {
        void onRead(Object data);
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final File cacheDir;

    public DiskCache(Context context) {
        cacheDir = new File(context.getApplicationContext().getFilesDir(), DIR);
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Log.w(TAG, "缓存目录创建失败: " + cacheDir);
        }
    }

    /** 异步读取某 key 的缓存;无缓存或损坏回调 null */
    public void read(final String key, final ReadCallback callback) {
        io.execute(new Runnable() {
            @Override
            public void run() {
                final Object data = readSync(key);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onRead(data);
                    }
                });
            }
        });
    }

    /** 异步写入某 key 的缓存。data 必须可序列化;不可序列化时静默跳过 */
    public void write(final String key, final Object data) {
        if (data == null || !(data instanceof Serializable)) {
            return;
        }
        final Serializable value = (Serializable) data;
        io.execute(new Runnable() {
            @Override
            public void run() {
                writeSync(key, value);
            }
        });
    }

    private Object readSync(String key) {
        File f = fileFor(key);
        if (f == null || !f.exists()) {
            return null;
        }
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(f));
            return in.readObject();
        } catch (Throwable e) {
            // 损坏/类不兼容:丢弃,避免反复失败
            Log.w(TAG, "读取缓存失败,丢弃: " + key, e);
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    private void writeSync(String key, Serializable value) {
        File f = fileFor(key);
        if (f == null) {
            return;
        }
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(f));
            out.writeObject(value);
            out.flush();
        } catch (Throwable e) {
            Log.w(TAG, "写入缓存失败: " + key, e);
        } finally {
            closeQuietly(out);
        }
    }

    private File fileFor(String key) {
        if (TextUtils.isEmpty(key)) {
            return null;
        }
        // key 即模块 key,形如 home_asset_overview,本身就是安全文件名
        return new File(cacheDir, key + ".cache");
    }

    private void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
