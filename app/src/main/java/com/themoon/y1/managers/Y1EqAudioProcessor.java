package com.themoon.y1.managers;

import com.google.android.exoplayer2.audio.AudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Y1EqAudioProcessor implements AudioProcessor {

    private boolean isActive = false;
    private boolean isFormatSupported = false; // 🚀 신규 방어 센서 장착

    private AudioFormat pendingAudioFormat = AudioFormat.NOT_SET;
    private AudioFormat outputAudioFormat = AudioFormat.NOT_SET;

    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private boolean inputEnded = false;

    private final float[] CENTER_FREQS = {31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f};
    private BiquadFilter[][] filters = new BiquadFilter[2][10];
    private float[] currentGains = new float[10];

    public Y1EqAudioProcessor() {
        for (int ch = 0; ch < 2; ch++) {
            for (int band = 0; band < 10; band++) {
                filters[ch][band] = new BiquadFilter();
            }
        }
    }

    public void setBandLevel(int bandIndex, float dbGain) {
        if (bandIndex < 0 || bandIndex >= 10) return;
        currentGains[bandIndex] = dbGain;

        if (outputAudioFormat.sampleRate > 0) {
            float fs = outputAudioFormat.sampleRate;
            float f0 = CENTER_FREQS[bandIndex];
            
            // 🚀 [버그 수정] f0가 Nyquist 주파수(fs/2)를 넘어가면 필터가 붕괴(NaN)되어 소리가 안 나는 현상 방지!
            if (f0 >= fs / 2.0f) {
                f0 = (fs / 2.0f) * 0.99f;
            }
            
            for (int ch = 0; ch < 2; ch++) {
                filters[ch][bandIndex].setPeakingEQ(fs, f0, 1.414f, dbGain);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.isActive = enabled;
        flush();
    }

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        pendingAudioFormat = inputAudioFormat;

        // 🚀 [폭탄 완전 해체] 16비트일 때만 수학 공식을 세팅합니다!
        // 🚀 [V3 추가 방어막] 96kHz 이상의 고음질은 200MHz CPU에서 EQ 연산 시 무조건 렉이 발생합니다.
        // 따라서 48kHz 이하의 음원(MP3 등)에서만 EQ가 작동하도록 강제 차단합니다! (고음질은 EQ 자동 패스)
        isFormatSupported = (inputAudioFormat.encoding == com.google.android.exoplayer2.C.ENCODING_PCM_16BIT) 
                            && (inputAudioFormat.sampleRate <= 48000);

        return inputAudioFormat; // 💡 입력된 포맷을 그대로 안전하게 출력 선언!
    }

    @Override
    public boolean isActive() {
        // 🚀 [진짜 프리패스 장착] 16비트가 아닌 고음질 파일은 필터를 배관에서 완전히 뽑아냅니다! (절단 버그 원천 차단)
        if (!isFormatSupported) return false;

        return pendingAudioFormat != AudioFormat.NOT_SET;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }

        // 🚀 스위치가 꺼져있거나 16비트가 아니라면, 0.0001초 만에 원본 그대로 프리패스!
        // 🚀 [초극강 최적화] 사용자가 EQ를 '기본(Flat)'으로 설정했거나 모든 밴드가 0dB라면 연산을 100% 스킵합니다!
        boolean isAllFlat = true;
        for (int i = 0; i < 10; i++) {
            if (Math.abs(currentGains[i]) > 0.01f) {
                isAllFlat = false;
                break;
            }
        }

        if (!isActive || !isFormatSupported || isAllFlat) {
            if (inputBuffer.hasRemaining()) {
                buffer.put(inputBuffer);
            }
            buffer.flip();
            outputBuffer = buffer;
            return;
        }

        int channelCount = outputAudioFormat.channelCount;
        int bytesPerFrame = channelCount * 2; // 채널당 2바이트

        // 🚀 온전한 프레임 묶음일 때만 연산 가동!
        while (inputBuffer.remaining() >= bytesPerFrame) {
            for (int ch = 0; ch < channelCount; ch++) {
                short rawSample = inputBuffer.getShort();
                float sample = rawSample / 32768.0f;

                // 🚀 [핵심 최적화] 0dB가 아닌 밴드에 대해서만 필터 연산을 수행합니다!
                // 10개 밴드 중 1개만 건드려도 9개는 패스하므로 연산량이 90% 줄어듭니다!
                for (int band = 0; band < 10; band++) {
                    if (Math.abs(currentGains[band]) > 0.01f) {
                        sample = filters[ch][band].process(sample);
                    }
                }

                if (sample > 1.0f) sample = 1.0f;
                else if (sample < -1.0f) sample = -1.0f;

                buffer.putShort((short) (sample * 32767.0f));
            }
        }

        // 🚀 [찌꺼기 방어막] 남은 찌꺼기 바이트는 계산하지 않고 안전하게 패스!
        if (inputBuffer.hasRemaining()) {
            buffer.put(inputBuffer);
        }

        buffer.flip();
        outputBuffer = buffer;
    }

    @Override
    public void queueEndOfStream() { inputEnded = true; }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer output = outputBuffer;
        outputBuffer = EMPTY_BUFFER;
        return output;
    }

    @Override
    public boolean isEnded() { return inputEnded && outputBuffer == EMPTY_BUFFER; }

    @Override
    public void flush() {
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
        outputAudioFormat = pendingAudioFormat;

        for (int ch = 0; ch < 2; ch++) {
            for (int band = 0; band < 10; band++) {
                filters[ch][band].reset();
            }
        }

        if (outputAudioFormat.sampleRate > 0) {
            for (int band = 0; band < 10; band++) {
                setBandLevel(band, currentGains[band]);
            }
        }
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        pendingAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
        isFormatSupported = false;
    }

    private static class BiquadFilter {
        private float a0, a1, a2, b1, b2;
        private float x1 = 0, x2 = 0, y1 = 0, y2 = 0;

        public void setPeakingEQ(float fs, float f0, float Q, float dbGain) {
            float A = (float) Math.pow(10, dbGain / 40.0);
            float w0 = (float) (2 * Math.PI * f0 / fs);
            float alpha = (float) Math.sin(w0) / (2 * Q);

            float b0 = 1 + alpha * A;
            b1 = -2 * (float) Math.cos(w0);
            b2 = 1 - alpha * A;
            a0 = 1 + alpha / A;
            a1 = -2 * (float) Math.cos(w0);
            a2 = 1 - alpha / A;

            a0 /= b0; a1 /= b0; a2 /= b0; b1 /= b0; b2 /= b0;
        }

        public float process(float x) {
            float y = a0 * x + a1 * x1 + a2 * x2 - b1 * y1 - b2 * y2;
            x2 = x1; x1 = x;
            y2 = y1; y1 = y;
            return y;
        }

        public void reset() {
            x1 = 0; x2 = 0; y1 = 0; y2 = 0;
        }
    }
}