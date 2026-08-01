package com.themoon.y1.managers;

import com.google.android.exoplayer2.audio.AudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Y1CrossfeedAudioProcessor implements AudioProcessor {

    private boolean isActive = false;
    private boolean isFormatSupported = false; // 🚀 신규 방어 센서 장착

    private AudioFormat pendingAudioFormat = AudioFormat.NOT_SET;
    private AudioFormat outputAudioFormat = AudioFormat.NOT_SET;

    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private boolean inputEnded = false;

    private float crossfeedLevel = 0.0f;
    private float alpha = 0.0f;
    private float lastL = 0.0f, lastR = 0.0f;

    public void setIntensity(int level) {
        if (level == 0) {
            isActive = false;
            crossfeedLevel = 0.0f;
        } else {
            isActive = true;
            if (level == 1) crossfeedLevel = 0.15f;
            else if (level == 2) crossfeedLevel = 0.25f;
            else if (level >= 3) crossfeedLevel = 0.35f;
        }
        flush();
    }

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        pendingAudioFormat = inputAudioFormat;

        // 🚀 [폭탄 완전 해체] 16비트 스테레오일 때만 수학 공식을 세팅합니다!
        // 🚀 [V3 추가 방어막] 96kHz 이상의 고음질은 200MHz CPU에서 연산 시 무조건 렉이 발생합니다.
        // 따라서 48kHz 이하의 음원(MP3 등)에서만 작동하도록 강제 차단합니다!
        isFormatSupported = (inputAudioFormat.encoding == com.google.android.exoplayer2.C.ENCODING_PCM_16BIT) 
                            && (inputAudioFormat.channelCount == 2)
                            && (inputAudioFormat.sampleRate <= 48000);

        if (isFormatSupported) {
            float fc = 700.0f;
            float dt = 1.0f / inputAudioFormat.sampleRate;
            float rc = 1.0f / (float)(2.0 * Math.PI * fc);
            alpha = dt / (rc + dt);
        }

        return inputAudioFormat; // 💡 입력된 포맷을 그대로 안전하게 출력 선언!
    }

    @Override
    public boolean isActive() {
        // 🚀 [진짜 프리패스 장착] 16비트 스테레오가 아닌 파일은 필터를 배관에서 완전히 뽑아냅니다! (절단 버그 원천 차단)
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

        // 🚀 스위치가 꺼져있거나 16비트 스테레오가 아니라면, 수학 공식을 건너뛰고 0.0001초 만에 원본 그대로 프리패스!
        if (!isActive || crossfeedLevel == 0.0f || !isFormatSupported) {
            if (inputBuffer.hasRemaining()) {
                buffer.put(inputBuffer);
            }
            buffer.flip();
            outputBuffer = buffer;
            return;
        }

        // 🔴 16비트 스테레오가 확인되면 4바이트(좌우 1세트) 묶음일 때만 연산 폭격 가동!
        while (inputBuffer.remaining() >= 4) {
            short rawL = inputBuffer.getShort();
            short rawR = inputBuffer.getShort();

            float L = rawL / 32768.0f;
            float R = rawR / 32768.0f;

            lastL = lastL + alpha * (L - lastL);
            lastR = lastR + alpha * (R - lastR);

            float outL = L + (lastR * crossfeedLevel);
            float outR = R + (lastL * crossfeedLevel);

            outL = outL / (1.0f + crossfeedLevel);
            outR = outR / (1.0f + crossfeedLevel);

            if (outL > 1.0f) outL = 1.0f; else if (outL < -1.0f) outL = -1.0f;
            if (outR > 1.0f) outR = 1.0f; else if (outR < -1.0f) outR = -1.0f;

            buffer.putShort((short) (outL * 32767.0f));
            buffer.putShort((short) (outR * 32767.0f));
        }

        // 🚀 [찌꺼기 방어막] 남은 홀수 찌꺼기 바이트는 계산하지 않고 안전하게 뒤로 넘깁니다.
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
        lastL = 0.0f; lastR = 0.0f;
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        pendingAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
        isFormatSupported = false;
    }
}