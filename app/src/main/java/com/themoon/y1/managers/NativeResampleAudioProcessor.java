package com.themoon.y1.managers;

import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.C;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NativeResampleAudioProcessor implements AudioProcessor {

    private int channelCount = -1;
    private int inRate = -1;
    private int outRate = -1;
    private int inputEncoding = -1;
    private int decimateFactor = 1;
    private int bytesPerSample = 2;
    private boolean isActive = false;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private boolean inputEnded = false;

    private byte[] inputArray = new byte[0];
    private byte[] outArray = new byte[0];

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        inRate = inputAudioFormat.sampleRate;
        channelCount = inputAudioFormat.channelCount;
        inputEncoding = inputAudioFormat.encoding;

        // 1. 샘플레이트 다운샘플 계수 계산 (최대 48kHz로 안전 제한)
        if (inRate > 96000) {
            // 176.4kHz -> 44.1kHz / 192kHz -> 48kHz (4배 다운샘플링)
            outRate = inRate / 4;
            decimateFactor = 4;
        } else if (inRate > 48000) {
            // 88.2kHz -> 44.1kHz / 96kHz -> 48kHz (2배 다운샘플링)
            outRate = inRate / 2;
            decimateFactor = 2;
        } else {
            outRate = inRate;
            decimateFactor = 1;
        }

        // 2. 바이트 크기 계산
        if (inputEncoding == C.ENCODING_PCM_16BIT) {
            bytesPerSample = 2;
        } else if (inputEncoding == C.ENCODING_PCM_24BIT) {
            bytesPerSample = 3;
        } else if (inputEncoding == C.ENCODING_PCM_32BIT || inputEncoding == C.ENCODING_PCM_FLOAT) {
            bytesPerSample = 4;
        } else {
            bytesPerSample = 2;
        }

        boolean isHighSampleRate = (decimateFactor > 1);
        boolean isNot16Bit = (inputEncoding != C.ENCODING_PCM_16BIT);

        // 모노(1) 및 스테레오(2) 모두 지원
        isActive = (isHighSampleRate || isNot16Bit) && (channelCount >= 1 && channelCount <= 2);

        if (isActive) {
            return new AudioFormat(outRate, channelCount, C.ENCODING_PCM_16BIT);
        }

        // 변환이 필요 없는 16비트 48kHz 이하는 그대로 통과
        return new AudioFormat(inRate, channelCount, inputEncoding);
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int inputSize = limit - position;
        if (inputSize <= 0) {
            return;
        }

        if (inputArray.length < inputSize) {
            inputArray = new byte[inputSize];
        }

        // 출력 배열 최대 크기 확보
        int maxEstimatedOutput = (inputSize / decimateFactor) + 32;
        if (outArray.length < maxEstimatedOutput) {
            outArray = new byte[maxEstimatedOutput];
        }

        // 🎯 1. 단 한번의 bulk read로 전체 바이트를 자바 배열로 고속 복사
        inputBuffer.get(inputArray, 0, inputSize);

        int outIndex = 0;
        int frameBytes = bytesPerSample * channelCount;
        int stride = frameBytes * decimateFactor;

        if (channelCount == 2) {
            // ==========================================
            // 🎧 스테레오 (2채널) 처리
            // ==========================================
            if (inputEncoding == C.ENCODING_PCM_16BIT) {
                // 16비트 스테레오: stride마다 4바이트(L:2, R:2) 추출
                for (int i = 0; i + 3 < inputSize; i += stride) {
                    outArray[outIndex++] = inputArray[i];
                    outArray[outIndex++] = inputArray[i + 1];
                    outArray[outIndex++] = inputArray[i + 2];
                    outArray[outIndex++] = inputArray[i + 3];
                }
            } else if (inputEncoding == C.ENCODING_PCM_24BIT) {
                // 24비트 스테레오: L:3바이트(하위1, 상위2), R:3바이트(하위1, 상위2) -> 상위 16비트만 추출
                for (int i = 0; i + 5 < inputSize; i += stride) {
                    outArray[outIndex++] = inputArray[i + 1];
                    outArray[outIndex++] = inputArray[i + 2];
                    outArray[outIndex++] = inputArray[i + 4];
                    outArray[outIndex++] = inputArray[i + 5];
                }
            } else {
                // 32비트 / Float 스테레오: L:4바이트, R:4바이트 -> 상위 16비트 추출
                for (int i = 0; i + 7 < inputSize; i += stride) {
                    outArray[outIndex++] = inputArray[i + 2];
                    outArray[outIndex++] = inputArray[i + 3];
                    outArray[outIndex++] = inputArray[i + 6];
                    outArray[outIndex++] = inputArray[i + 7];
                }
            }
        } else if (channelCount == 1) {
            // ==========================================
            // 🎙️ 모노 (1채널) 처리
            // ==========================================
            if (inputEncoding == C.ENCODING_PCM_16BIT) {
                for (int i = 0; i + 1 < inputSize; i += stride) {
                    outArray[outIndex++] = inputArray[i];
                    outArray[outIndex++] = inputArray[i + 1];
                }
            } else if (inputEncoding == C.ENCODING_PCM_24BIT) {
                for (int i = 0; i + 2 < inputSize; i += stride) {
                    outArray[outIndex++] = inputArray[i + 1];
                    outArray[outIndex++] = inputArray[i + 2];
                }
            } else {
                for (int i = 0; i + 3 < inputSize; i += stride) {
                    outArray[outIndex++] = inputArray[i + 2];
                    outArray[outIndex++] = inputArray[i + 3];
                }
            }
        }

        if (outIndex > 0) {
            if (buffer.capacity() < outIndex) {
                buffer = ByteBuffer.allocateDirect(outIndex).order(ByteOrder.nativeOrder());
            } else {
                buffer.clear();
            }
            buffer.put(outArray, 0, outIndex);
            buffer.limit(outIndex);
            buffer.position(0);
            outputBuffer = buffer;
        }
    }

    @Override
    public void queueEndOfStream() {
        inputEnded = true;
    }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer output = outputBuffer;
        outputBuffer = EMPTY_BUFFER;
        return output;
    }

    @Override
    public boolean isEnded() {
        return inputEnded && outputBuffer == EMPTY_BUFFER;
    }

    @Override
    public void flush() {
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        inputArray = new byte[0];
        outArray = new byte[0];
        inRate = -1;
        outRate = -1;
        channelCount = -1;
        inputEncoding = -1;
        decimateFactor = 1;
        bytesPerSample = 2;
        isActive = false;
    }
}
