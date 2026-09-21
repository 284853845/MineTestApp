package com.example.myapplication.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Synchronously transcodes one audio track to a small AAC-LC M4A file. */
public final class AudioCompressor {

    public interface ProgressCallback {
        void onProgress(int progress);
    }

    private static final String MIME_AAC = "audio/mp4a-latm";
    private static final String MIME_RAW = "audio/raw";
    private static final int OUTPUT_SAMPLE_RATE = 22_050;
    private static final int OUTPUT_CHANNEL_COUNT = 1;
    private static final int OUTPUT_BIT_RATE = 32_000;
    private static final int PCM_BYTES_PER_SAMPLE = 2;
    private static final int MAX_PENDING_PCM_BYTES = 512 * 1024;
    private static final long CODEC_TIMEOUT_US = 10_000L;
    private static final long STALL_TIMEOUT_MS = 30_000L;
    private static final String KEY_PCM_ENCODING = "pcm-encoding";

    public void compress(Context context, Uri inputUri, File outputFile,
                         ProgressCallback callback) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        boolean decoderStarted = false;
        boolean encoderStarted = false;
        boolean muxerStarted = false;

        try {
            checkCancelled();
            extractor.setDataSource(context, inputUri, null);
            int audioTrack = findAudioTrack(extractor);
            if (audioTrack < 0) {
                throw new IOException("选择的文件不包含可识别的音频轨道");
            }
            extractor.selectTrack(audioTrack);
            MediaFormat sourceFormat = extractor.getTrackFormat(audioTrack);
            String sourceMime = sourceFormat.getString(MediaFormat.KEY_MIME);
            if (sourceMime == null || !sourceMime.startsWith("audio/")) {
                throw new IOException("选择的文件不是受支持的音频文件");
            }
            long durationUs = sourceFormat.containsKey(MediaFormat.KEY_DURATION)
                    ? sourceFormat.getLong(MediaFormat.KEY_DURATION) : -1L;
            boolean rawInput = MIME_RAW.equalsIgnoreCase(sourceMime);

            if (!rawInput) {
                try {
                    decoder = MediaCodec.createDecoderByType(sourceMime);
                    decoder.configure(sourceFormat, null, null, 0);
                    decoder.start();
                    decoderStarted = true;
                } catch (RuntimeException error) {
                    throw new IOException("设备无法解码该音频格式", error);
                }
            }

            muxer = new MediaMuxer(outputFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            PcmTransformer transformer = null;
            PcmByteQueue pcmQueue = new PcmByteQueue();
            ByteBuffer rawBuffer = rawInput ? ByteBuffer.allocateDirect(256 * 1024) : null;
            MediaCodec.BufferInfo decoderInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo encoderInfo = new MediaCodec.BufferInfo();

            boolean sourceInputEnded = false;
            boolean decodedOutputEnded = false;
            boolean encoderInputEnded = false;
            boolean encoderOutputEnded = false;
            int muxerTrack = -1;
            long encoderInputSamples = 0L;
            long decodedSamples = 0L;
            long lastProgressAt = 0L;
            long lastActivityAt = SystemClock.elapsedRealtime();

            if (rawInput) {
                int sampleRate = requirePositive(sourceFormat, MediaFormat.KEY_SAMPLE_RATE,
                        "音频采样率无效");
                int channels = requirePositive(sourceFormat, MediaFormat.KEY_CHANNEL_COUNT,
                        "音频声道数无效");
                validatePcmEncoding(sourceFormat);
                transformer = new PcmTransformer(sampleRate, channels);
                encoder = createAndStartEncoder();
                encoderStarted = true;
            }

            while (!encoderOutputEnded) {
                checkCancelled();
                boolean progressed = false;

                if (rawInput && !sourceInputEnded
                        && pcmQueue.size() < MAX_PENDING_PCM_BYTES) {
                    rawBuffer.clear();
                    int read = extractor.readSampleData(rawBuffer, 0);
                    if (read < 0) {
                        sourceInputEnded = true;
                        decodedOutputEnded = true;
                    } else {
                        rawBuffer.position(0);
                        rawBuffer.limit(read);
                        decodedSamples += transformer.transform(rawBuffer, read, pcmQueue);
                        long now = SystemClock.elapsedRealtime();
                        if (callback != null && durationUs > 0L
                                && now - lastProgressAt >= 100L) {
                            callback.onProgress(progressFor(
                                    extractor.getSampleTime(), durationUs));
                            lastProgressAt = now;
                        }
                        extractor.advance();
                    }
                    progressed = true;
                }

                if (!rawInput && !sourceInputEnded) {
                    int inputIndex = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                        if (inputBuffer == null) {
                            throw new IOException("无法取得音频解码输入缓冲区");
                        }
                        inputBuffer.clear();
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sourceInputEnded = true;
                        } else {
                            long sampleTime = Math.max(0L, extractor.getSampleTime());
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0);
                            extractor.advance();
                        }
                        progressed = true;
                    }
                }

                if (!rawInput && !decodedOutputEnded
                        && pcmQueue.size() < MAX_PENDING_PCM_BYTES) {
                    int outputIndex = decoder.dequeueOutputBuffer(decoderInfo, CODEC_TIMEOUT_US);
                    if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (encoder != null) {
                            throw new IOException("音频解码器重复改变输出格式");
                        }
                        MediaFormat pcmFormat = decoder.getOutputFormat();
                        int sampleRate = requirePositive(pcmFormat, MediaFormat.KEY_SAMPLE_RATE,
                                "解码后的采样率无效");
                        int channels = requirePositive(pcmFormat, MediaFormat.KEY_CHANNEL_COUNT,
                                "解码后的声道数无效");
                        validatePcmEncoding(pcmFormat);
                        transformer = new PcmTransformer(sampleRate, channels);
                        encoder = createAndStartEncoder();
                        encoderStarted = true;
                        progressed = true;
                    } else if (outputIndex >= 0) {
                        if (transformer == null || encoder == null) {
                            throw new IOException("音频解码器未提供有效的 PCM 格式");
                        }
                        ByteBuffer outputBuffer = decoder.getOutputBuffer(outputIndex);
                        if (decoderInfo.size > 0) {
                            if (outputBuffer == null) {
                                throw new IOException("无法取得音频解码输出缓冲区");
                            }
                            outputBuffer.position(decoderInfo.offset);
                            outputBuffer.limit(decoderInfo.offset + decoderInfo.size);
                            decodedSamples += transformer.transform(
                                    outputBuffer, decoderInfo.size, pcmQueue);
                            long now = SystemClock.elapsedRealtime();
                            if (callback != null && durationUs > 0L
                                    && now - lastProgressAt >= 100L) {
                                callback.onProgress(progressFor(
                                        decoderInfo.presentationTimeUs, durationUs));
                                lastProgressAt = now;
                            }
                        }
                        boolean endOfStream = (decoderInfo.flags
                                & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        decoder.releaseOutputBuffer(outputIndex, false);
                        if (endOfStream) {
                            decodedOutputEnded = true;
                        }
                        progressed = true;
                    }
                }

                if (encoder != null && !encoderInputEnded
                        && (pcmQueue.size() > 0 || decodedOutputEnded)) {
                    int inputIndex = encoder.dequeueInputBuffer(CODEC_TIMEOUT_US);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = encoder.getInputBuffer(inputIndex);
                        if (inputBuffer == null) {
                            throw new IOException("无法取得音频编码输入缓冲区");
                        }
                        inputBuffer.clear();
                        if (pcmQueue.size() > 0) {
                            int bytes = pcmQueue.readTo(inputBuffer);
                            long presentationTimeUs = encoderInputSamples * 1_000_000L
                                    / OUTPUT_SAMPLE_RATE;
                            encoder.queueInputBuffer(inputIndex, 0, bytes,
                                    presentationTimeUs, 0);
                            encoderInputSamples += bytes / PCM_BYTES_PER_SAMPLE;
                        } else {
                            if (decodedSamples <= 0L) {
                                throw new IOException("音频轨道中没有可压缩的声音数据");
                            }
                            long presentationTimeUs = encoderInputSamples * 1_000_000L
                                    / OUTPUT_SAMPLE_RATE;
                            encoder.queueInputBuffer(inputIndex, 0, 0,
                                    presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            encoderInputEnded = true;
                        }
                        progressed = true;
                    }
                }

                if (encoder != null) {
                    int outputIndex = encoder.dequeueOutputBuffer(encoderInfo, CODEC_TIMEOUT_US);
                    if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw new IOException("音频编码器重复改变输出格式");
                        }
                        muxerTrack = muxer.addTrack(encoder.getOutputFormat());
                        muxer.start();
                        muxerStarted = true;
                        progressed = true;
                    } else if (outputIndex >= 0) {
                        ByteBuffer outputBuffer = encoder.getOutputBuffer(outputIndex);
                        if ((encoderInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            encoderInfo.size = 0;
                        }
                        if (encoderInfo.size > 0) {
                            if (!muxerStarted || outputBuffer == null) {
                                throw new IOException("音频封装器尚未准备完成");
                            }
                            outputBuffer.position(encoderInfo.offset);
                            outputBuffer.limit(encoderInfo.offset + encoderInfo.size);
                            muxer.writeSampleData(muxerTrack, outputBuffer, encoderInfo);
                        }
                        encoderOutputEnded = (encoderInfo.flags
                                & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        encoder.releaseOutputBuffer(outputIndex, false);
                        progressed = true;
                    }
                }

                if (progressed) {
                    lastActivityAt = SystemClock.elapsedRealtime();
                } else if (SystemClock.elapsedRealtime() - lastActivityAt > STALL_TIMEOUT_MS) {
                    throw new IOException("音频编解码超时，请更换文件后重试");
                }
            }

            if (!muxerStarted) {
                throw new IOException("未生成有效的音频输出");
            }
            muxer.stop();
            muxerStarted = false;
            if (callback != null) {
                callback.onProgress(100);
            }
        } finally {
            if (decoder != null) {
                if (decoderStarted) {
                    try {
                        decoder.stop();
                    } catch (RuntimeException ignored) {
                    }
                }
                try {
                    decoder.release();
                } catch (RuntimeException ignored) {
                }
            }
            if (encoder != null) {
                if (encoderStarted) {
                    try {
                        encoder.stop();
                    } catch (RuntimeException ignored) {
                    }
                }
                try {
                    encoder.release();
                } catch (RuntimeException ignored) {
                }
            }
            if (muxer != null) {
                if (muxerStarted) {
                    try {
                        muxer.stop();
                    } catch (RuntimeException ignored) {
                    }
                }
                try {
                    muxer.release();
                } catch (RuntimeException ignored) {
                }
            }
            try {
                extractor.release();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static MediaCodec createAndStartEncoder() throws IOException {
        MediaFormat outputFormat = MediaFormat.createAudioFormat(
                MIME_AAC, OUTPUT_SAMPLE_RATE, OUTPUT_CHANNEL_COUNT);
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_BIT_RATE);
        outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024);

        MediaCodec encoder = null;
        try {
            encoder = MediaCodec.createEncoderByType(MIME_AAC);
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            return encoder;
        } catch (RuntimeException error) {
            if (encoder != null) {
                encoder.release();
            }
            throw new IOException("设备无法创建 AAC 音频编码器", error);
        }
    }

    private static int findAudioTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    private static int requirePositive(MediaFormat format, String key, String message)
            throws IOException {
        if (!format.containsKey(key)) {
            throw new IOException(message);
        }
        int value = format.getInteger(key);
        if (value <= 0) {
            throw new IOException(message);
        }
        return value;
    }

    private static void validatePcmEncoding(MediaFormat format) throws IOException {
        if (format.containsKey(KEY_PCM_ENCODING)
                && format.getInteger(KEY_PCM_ENCODING) != AudioFormat.ENCODING_PCM_16BIT) {
            throw new IOException("当前仅支持 16 位 PCM 音频数据");
        }
    }

    private static int progressFor(long presentationTimeUs, long durationUs) {
        if (durationUs <= 0L) {
            return 0;
        }
        return (int) Math.max(0L,
                Math.min(99L, presentationTimeUs * 100L / durationUs));
    }

    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("音频压缩已取消");
        }
    }

    /** Converts interleaved 16-bit PCM to 22.05 kHz mono without buffering the whole track. */
    static final class PcmTransformer {
        private final int inputSampleRate;
        private final int inputChannels;
        private final int inputFrameBytes;
        private final byte[] partialFrame;
        private int partialLength;
        private long resampleAccumulator;

        PcmTransformer(int inputSampleRate, int inputChannels) throws IOException {
            if (inputSampleRate <= 0 || inputChannels <= 0 || inputChannels > 32) {
                throw new IOException("不支持该音频的采样率或声道数");
            }
            this.inputSampleRate = inputSampleRate;
            this.inputChannels = inputChannels;
            this.inputFrameBytes = inputChannels * PCM_BYTES_PER_SAMPLE;
            this.partialFrame = new byte[inputFrameBytes];
        }

        long transform(ByteBuffer source, int byteCount, PcmByteQueue output) {
            ByteBuffer input = source.slice().order(ByteOrder.LITTLE_ENDIAN);
            input.limit(byteCount);
            long outputSamples = 0L;

            if (partialLength > 0) {
                int needed = inputFrameBytes - partialLength;
                int copied = Math.min(needed, input.remaining());
                input.get(partialFrame, partialLength, copied);
                partialLength += copied;
                if (partialLength == inputFrameBytes) {
                    outputSamples += processFrame(
                            ByteBuffer.wrap(partialFrame).order(ByteOrder.LITTLE_ENDIAN), output);
                    partialLength = 0;
                }
            }

            while (input.remaining() >= inputFrameBytes) {
                int oldLimit = input.limit();
                input.limit(input.position() + inputFrameBytes);
                outputSamples += processFrame(input.slice().order(ByteOrder.LITTLE_ENDIAN), output);
                input.position(input.limit());
                input.limit(oldLimit);
            }
            if (input.hasRemaining()) {
                partialLength = input.remaining();
                input.get(partialFrame, 0, partialLength);
            }
            return outputSamples;
        }

        private long processFrame(ByteBuffer frame, PcmByteQueue output) {
            int sum = 0;
            for (int channel = 0; channel < inputChannels; channel++) {
                sum += frame.getShort();
            }
            short mono = (short) (sum / inputChannels);
            long emitted = 0L;
            resampleAccumulator += OUTPUT_SAMPLE_RATE;
            while (resampleAccumulator >= inputSampleRate) {
                output.appendShort(mono);
                resampleAccumulator -= inputSampleRate;
                emitted++;
            }
            return emitted;
        }
    }

    static final class PcmByteQueue {
        private byte[] data = new byte[32 * 1024];
        private int start;
        private int end;

        int size() {
            return end - start;
        }

        void appendShort(short value) {
            ensureCapacity(PCM_BYTES_PER_SAMPLE);
            data[end++] = (byte) (value & 0xff);
            data[end++] = (byte) ((value >>> 8) & 0xff);
        }

        int readTo(ByteBuffer target) {
            int count = Math.min(size(), target.remaining());
            count -= count % PCM_BYTES_PER_SAMPLE;
            target.put(data, start, count);
            start += count;
            if (start == end) {
                start = 0;
                end = 0;
            }
            return count;
        }

        private void ensureCapacity(int additional) {
            if (end + additional <= data.length) {
                return;
            }
            int currentSize = size();
            if (start > 0) {
                System.arraycopy(data, start, data, 0, currentSize);
                start = 0;
                end = currentSize;
            }
            if (end + additional <= data.length) {
                return;
            }
            byte[] larger = new byte[Math.max(data.length * 2, end + additional)];
            System.arraycopy(data, 0, larger, 0, end);
            data = larger;
        }
    }
}
