package com.themoon.y1.managers;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import java.nio.ByteBuffer;

public class OpenSLESAudioSink implements AudioSink {

    static {
        System.loadLibrary("opensles_sink");
    }

    private native boolean nativeInit(int sampleRate, int channelCount);
    private native void nativePlay();
    private native void nativePause();
    private native void nativeStop();
    private native void nativeFlush();
    private native int nativeGetQueuedBufferCount();
    private native boolean nativeWrite(ByteBuffer directBuffer, int offset, int size);
    private native long nativeGetPositionFrames();
    private native void nativeRelease();

    private boolean isInitialized = false;
    private long submittedBytes = 0;
    private long submittedFrames = 0;
    private int sampleRate = 48000;
    private int channelCount = 2;
    private int bytesPerFrame = 4;
    private boolean isPlaying = false;
    private PlaybackParameters playbackParameters = PlaybackParameters.DEFAULT;

    @Override
    public void setListener(Listener listener) { }

    @Override
    public boolean supportsFormat(Format format) {
        return true; 
    }

    @Override
    public int getFormatSupport(Format format) {
        return 2; // AudioCapabilities.FORMAT_SUPPORTED
    }

    @Override
    public void configure(Format inputFormat, int specifiedBufferSize, int[] outputChannels) throws ConfigurationException {
        if (isInitialized) {
            nativeRelease();
        }
        this.sampleRate = inputFormat.sampleRate;
        this.channelCount = inputFormat.channelCount;
        this.bytesPerFrame = 2 * channelCount; // 16-bit
        
        if (!nativeInit(sampleRate, channelCount)) {
            throw new ConfigurationException("OpenSL ES Init Failed", inputFormat);
        }
        isInitialized = true;
        submittedBytes = 0;
        submittedFrames = 0;
    }

    @Override
    public void play() {
        isPlaying = true;
        if (isInitialized) {
            nativePlay();
        }
    }

    @Override
    public void handleDiscontinuity() { }

    @Override
    public boolean handleBuffer(ByteBuffer buffer, long presentationTimeUs, int encodedAccessUnitCount) throws InitializationException, WriteException {
        if (!isInitialized) return false;
        
        if (nativeGetQueuedBufferCount() >= 2) {
            return false; // Queue is full, wait
        }

        int size = buffer.remaining();
        if (size == 0) return true;

        if (buffer.isDirect()) {
            boolean success = nativeWrite(buffer, buffer.position(), size);
            if (success) {
                buffer.position(buffer.position() + size);
                submittedBytes += size;
                submittedFrames += size / bytesPerFrame;
                return true;
            }
        }
        return false;
    }

    @Override
    public void playToEndOfStream() throws WriteException { }

    @Override
    public boolean isEnded() {
        return submittedFrames > 0 && nativeGetQueuedBufferCount() == 0;
    }

    @Override
    public boolean hasPendingData() {
        return nativeGetQueuedBufferCount() > 0;
    }

    @Override
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.playbackParameters = playbackParameters;
    }

    @Override
    public PlaybackParameters getPlaybackParameters() {
        return playbackParameters;
    }

    @Override
    public void setSkipSilenceEnabled(boolean skipSilenceEnabled) { }

    @Override
    public boolean getSkipSilenceEnabled() { return false; }

    @Override
    public void setAudioAttributes(AudioAttributes audioAttributes) { }

    @Override
    public void setAudioSessionId(int audioSessionId) { }

    @Override
    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) { }

    @Override
    public void enableTunnelingV21() { }

    @Override
    public void disableTunneling() { }

    @Override
    public void setVolume(float volume) { }

    @Override
    public void pause() {
        isPlaying = false;
        if (isInitialized) {
            nativePause();
        }
    }

    @Override
    public void flush() {
        if (isInitialized) {
            nativeFlush();
            submittedBytes = 0;
            submittedFrames = 0;
        }
    }

    @Override
    public void experimentalFlushWithoutAudioTrackRelease() {
        flush();
    }

    @Override
    public void reset() {
        if (isInitialized) {
            nativeRelease();
            isInitialized = false;
        }
    }

    public long getCurrentPositionUs(boolean sourceEnded) {
        if (!isInitialized) return 0;
        long frames = nativeGetPositionFrames();
        return (frames * 1000000L) / sampleRate;
    }
}
