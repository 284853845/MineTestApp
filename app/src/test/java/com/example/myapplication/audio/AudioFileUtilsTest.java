package com.example.myapplication.audio;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AudioFileUtilsTest {

    @Test
    public void knownSizeMustBeStrictlyLessThanTwentyMiB() {
        assertTrue(AudioFileUtils.isAcceptedSize(AudioFileUtils.MAX_INPUT_BYTES - 1L));
        assertFalse(AudioFileUtils.isAcceptedSize(AudioFileUtils.MAX_INPUT_BYTES));
        assertFalse(AudioFileUtils.isAcceptedSize(AudioFileUtils.MAX_INPUT_BYTES + 1L));
        assertFalse(AudioFileUtils.isAcceptedSize(-1L));
    }

    @Test
    public void unknownSizeStopsAfterLimitPlusOneByte() throws IOException {
        long measured = AudioFileUtils.measureUpToLimit(
                new FixedLengthInputStream(AudioFileUtils.MAX_INPUT_BYTES + 500L),
                AudioFileUtils.MAX_INPUT_BYTES);

        assertEquals(AudioFileUtils.MAX_INPUT_BYTES + 1L, measured);
        assertFalse(AudioFileUtils.isAcceptedSize(measured));
    }

    @Test
    public void unknownSizeAcceptsShortStream() throws IOException {
        long expected = 123_456L;
        long measured = AudioFileUtils.measureUpToLimit(
                new FixedLengthInputStream(expected), AudioFileUtils.MAX_INPUT_BYTES);

        assertEquals(expected, measured);
        assertTrue(AudioFileUtils.isAcceptedSize(measured));
    }

    @Test
    public void unknownSizeCheckRespondsToCancellation() throws IOException {
        Thread.currentThread().interrupt();
        try {
            AudioFileUtils.measureUpToLimit(
                    new FixedLengthInputStream(100L), AudioFileUtils.MAX_INPUT_BYTES);
        } catch (InterruptedIOException expected) {
            return;
        } finally {
            Thread.interrupted();
        }
        throw new AssertionError("Expected InterruptedIOException");
    }

    @Test
    public void outputNameRemovesExtensionAndUnsafeCharacters() {
        assertEquals("meeting_audio", AudioFileUtils.sanitizeBaseName(" meeting:audio?.mp3 "));
        assertEquals("audio", AudioFileUtils.sanitizeBaseName(".mp3"));
    }

    @Test
    public void pcmTransformerDownmixesAndHalvesSampleRate() throws IOException {
        ByteBuffer input = ByteBuffer.allocate(4 * 2 * 2).order(ByteOrder.LITTLE_ENDIAN);
        putStereoFrame(input, (short) 100, (short) 300);
        putStereoFrame(input, (short) 300, (short) 500);
        putStereoFrame(input, (short) -200, (short) 200);
        putStereoFrame(input, (short) -600, (short) -200);
        input.flip();

        AudioCompressor.PcmByteQueue output = new AudioCompressor.PcmByteQueue();
        AudioCompressor.PcmTransformer transformer =
                new AudioCompressor.PcmTransformer(44_100, 2);
        long samples = transformer.transform(input, input.remaining(), output);

        assertEquals(2L, samples);
        ByteBuffer result = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(4, output.readTo(result));
        result.flip();
        assertEquals(400, result.getShort());
        assertEquals(-400, result.getShort());
    }

    private static void putStereoFrame(ByteBuffer buffer, short left, short right) {
        buffer.putShort(left);
        buffer.putShort(right);
    }

    private static final class FixedLengthInputStream extends InputStream {
        private long remaining;

        FixedLengthInputStream(long remaining) {
            this.remaining = remaining;
        }

        @Override
        public int read() {
            if (remaining <= 0L) {
                return -1;
            }
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (remaining <= 0L) {
                return -1;
            }
            int read = (int) Math.min(remaining, length);
            remaining -= read;
            return read;
        }
    }
}
