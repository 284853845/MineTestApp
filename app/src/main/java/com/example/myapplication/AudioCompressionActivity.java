package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.audio.AudioCompressor;
import com.example.myapplication.audio.AudioFileUtils;
import com.example.myapplication.security.CaptureAwareActivity;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Demonstrates selecting and compressing a small audio file from system storage. */
public class AudioCompressionActivity extends CaptureAwareActivity {

    private static final int REQUEST_PICK_AUDIO = 6201;
    private static final String STATE_RESULT_PATH = "audio_result_path";
    private static final String STATE_WAS_PROCESSING = "audio_was_processing";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Button selectButton;
    private ProgressBar progressBar;
    private TextView selectedFileView;
    private TextView statusView;
    private TextView resultSizeView;
    private TextView resultPathView;

    private Future<?> compressionTask;
    private volatile boolean destroyed;
    private boolean processing;
    private File resultFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_compression);

        selectButton = findViewById(R.id.btn_select_audio);
        progressBar = findViewById(R.id.progress_audio_compression);
        selectedFileView = findViewById(R.id.tv_selected_audio);
        statusView = findViewById(R.id.tv_compression_status);
        resultSizeView = findViewById(R.id.tv_compressed_size);
        resultPathView = findViewById(R.id.tv_compressed_path);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAudioPicker();
            }
        });

        if (savedInstanceState != null) {
            String resultPath = savedInstanceState.getString(STATE_RESULT_PATH);
            if (resultPath != null) {
                File restored = new File(resultPath);
                if (restored.isFile()) {
                    resultFile = restored;
                    showResult(restored);
                }
            } else if (savedInstanceState.getBoolean(STATE_WAS_PROCESSING, false)) {
                statusView.setText(R.string.audio_compression_cancelled_recreated);
            }
        }
    }

    private void openAudioPicker() {
        if (processing) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PICK_AUDIO);
        } catch (RuntimeException error) {
            showError(getString(R.string.audio_picker_unavailable));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_AUDIO || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null && !mimeType.startsWith("audio/")) {
            showError(getString(R.string.audio_invalid_type));
            return;
        }
        startCompression(uri);
    }

    private void startCompression(final Uri uri) {
        if (processing) {
            return;
        }
        setProcessing(true);
        selectedFileView.setText(R.string.audio_checking_file);
        statusView.setText(R.string.audio_checking_file);
        resultSizeView.setText("");
        resultPathView.setText("");
        resultFile = null;

        compressionTask = executor.submit(new Runnable() {
            @Override
            public void run() {
                File temporaryFile = null;
                try {
                    final AudioFileUtils.FileInfo info = AudioFileUtils.inspect(
                            AudioCompressionActivity.this, uri);
                    if (!AudioFileUtils.isAcceptedSize(info.size)) {
                        throw new IOException(getString(R.string.audio_file_too_large));
                    }
                    postSelectedFile(info);

                    File outputFile = AudioFileUtils.createOutputFile(
                            AudioCompressionActivity.this, info.displayName,
                            System.currentTimeMillis());
                    temporaryFile = new File(outputFile.getParentFile(),
                            "." + outputFile.getName() + ".part");
                    if (temporaryFile.exists() && !temporaryFile.delete()) {
                        throw new IOException(getString(R.string.audio_temp_file_error));
                    }

                    new AudioCompressor().compress(
                            AudioCompressionActivity.this, uri, temporaryFile,
                            new AudioCompressor.ProgressCallback() {
                                @Override
                                public void onProgress(int progress) {
                                    postProgress(progress);
                                }
                            });
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedIOException("cancelled");
                    }
                    if (!temporaryFile.renameTo(outputFile)) {
                        throw new IOException(getString(R.string.audio_finalize_file_error));
                    }
                    temporaryFile = null;
                    postSuccess(outputFile);
                } catch (InterruptedIOException ignored) {
                    // Lifecycle cancellation is expected and should not update a dead Activity.
                } catch (Exception error) {
                    postFailure(messageFor(error));
                } finally {
                    if (temporaryFile != null && temporaryFile.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        temporaryFile.delete();
                    }
                }
            }
        });
    }

    private void postSelectedFile(final AudioFileUtils.FileInfo info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (destroyed) {
                    return;
                }
                selectedFileView.setText(getString(R.string.audio_selected_file_format,
                        info.displayName, AudioFileUtils.formatSize(info.size)));
                statusView.setText(R.string.audio_compressing);
                progressBar.setIndeterminate(false);
                progressBar.setProgress(0);
            }
        });
    }

    private void postProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!destroyed && processing) {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(progress);
                    statusView.setText(getString(
                            R.string.audio_compressing_progress, progress));
                }
            }
        });
    }

    private void postSuccess(final File outputFile) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (destroyed) {
                    return;
                }
                setProcessing(false);
                resultFile = outputFile;
                showResult(outputFile);
                Toast.makeText(AudioCompressionActivity.this,
                        R.string.audio_compression_success, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postFailure(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (destroyed) {
                    return;
                }
                setProcessing(false);
                showError(message);
            }
        });
    }

    private void setProcessing(boolean processing) {
        this.processing = processing;
        selectButton.setEnabled(!processing);
        progressBar.setVisibility(processing ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(processing);
    }

    private void showResult(File outputFile) {
        progressBar.setVisibility(View.GONE);
        statusView.setText(R.string.audio_compression_success);
        resultSizeView.setText(getString(R.string.audio_compressed_size_format,
                AudioFileUtils.formatSize(outputFile.length())));
        resultPathView.setText(getString(R.string.audio_compressed_path_format,
                outputFile.getAbsolutePath()));
    }

    private void showError(String message) {
        statusView.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String messageFor(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return getString(R.string.audio_compression_failed);
        }
        return getString(R.string.audio_compression_failed_with_reason, message);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_WAS_PROCESSING, processing);
        if (resultFile != null && resultFile.isFile()) {
            outState.putString(STATE_RESULT_PATH, resultFile.getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (compressionTask != null) {
            compressionTask.cancel(true);
            compressionTask = null;
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
