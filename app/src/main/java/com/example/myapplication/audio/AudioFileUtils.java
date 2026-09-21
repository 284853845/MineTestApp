package com.example.myapplication.audio;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Locale;

/** File metadata and output naming helpers for audio compression. */
public final class AudioFileUtils {

    public static final long MAX_INPUT_BYTES = 20L * 1024L * 1024L;

    private AudioFileUtils() {
    }

    public static FileInfo inspect(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String name = null;
        long size = -1L;
        try (Cursor cursor = resolver.query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex);
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } catch (RuntimeException ignored) {
            // Some document providers do not implement metadata queries correctly.
        }

        if (name == null || name.trim().isEmpty()) {
            String segment = uri.getLastPathSegment();
            name = segment == null || segment.trim().isEmpty() ? "audio" : segment;
        }
        if (size < 0L) {
            try (InputStream input = resolver.openInputStream(uri)) {
                if (input == null) {
                    throw new FileNotFoundException("无法读取选择的音频文件");
                }
                size = measureUpToLimit(input, MAX_INPUT_BYTES);
            }
        }
        return new FileInfo(name, size);
    }

    /** Reads at most limit + 1 bytes so unknown-length streams can be rejected safely. */
    static long measureUpToLimit(InputStream input, long limit) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long total = 0L;
        while (total <= limit) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("文件检查已取消");
            }
            int maxRead = (int) Math.min(buffer.length, limit + 1L - total);
            if (maxRead <= 0) {
                break;
            }
            int read = input.read(buffer, 0, maxRead);
            if (read < 0) {
                break;
            }
            if (read == 0) {
                int one = input.read();
                if (one < 0) {
                    break;
                }
                total++;
            } else {
                total += read;
            }
        }
        return total;
    }

    public static boolean isAcceptedSize(long size) {
        return size >= 0L && size < MAX_INPUT_BYTES;
    }

    public static File createOutputFile(Context context, String sourceName, long timestamp)
            throws IOException {
        File musicRoot = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicRoot == null) {
            musicRoot = new File(context.getFilesDir(), "music");
        }
        File outputDir = new File(musicRoot, "compressed_audio");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("无法创建压缩文件目录");
        }

        String baseName = sanitizeBaseName(sourceName);
        String timestampText = String.format(Locale.US, "%d", timestamp);
        File candidate = new File(outputDir,
                baseName + "_compressed_" + timestampText + ".m4a");
        int suffix = 1;
        while (candidate.exists()) {
            candidate = new File(outputDir,
                    baseName + "_compressed_" + timestampText + "_" + suffix + ".m4a");
            suffix++;
        }
        return candidate;
    }

    static String sanitizeBaseName(String sourceName) {
        String value = sourceName == null ? "audio" : sourceName.trim();
        int dot = value.lastIndexOf('.');
        if (dot >= 0) {
            value = value.substring(0, dot);
        }
        value = value.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", " ")
                .replaceAll("^[_ .]+|[_ .]+$", "")
                .trim();
        if (value.isEmpty()) {
            return "audio";
        }
        return value.length() > 80 ? value.substring(0, 80).trim() : value;
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f);
        }
        return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024f * 1024f));
    }

    public static final class FileInfo {
        public final String displayName;
        public final long size;

        FileInfo(String displayName, long size) {
            this.displayName = displayName;
            this.size = size;
        }
    }
}
