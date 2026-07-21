package com.themoon.y1.managers;

import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.C;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NativeResampleAudioProcessor implements AudioProcessor {

    static {
        // CMakeLists.txt에 정의된 실제 라이브러리 이름(opensles_sink)을 정확히 로드합니다.
        // 잘못된 이름(y1_audio_engine)으로 인해 발생하던 치명적 크래시(앱 튕김)를 완벽히 해결합니다.
        System.loadLibrary("opensles_sink");
    }

    private int channelCount = -1;
    private int inRate = -1;
    private int outRate = -1;
    private int inputEncoding = -1;
    private boolean isActive = false;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private boolean inputEnded = false;

    // 네이티브 JNI 함수 선언 (다이렉트 버퍼의 주소를 직접 읽고 씁니다)
    // 반환값: 출력된 바이트 수
    private native int nativeProcess24Bit96kHzTo16Bit48kHz(ByteBuffer inputBuffer, int inputSize, ByteBuffer outputBuffer);
    private native int nativeProcess16Bit96kHzTo16Bit48kHz(ByteBuffer inputBuffer, int inputSize, ByteBuffer outputBuffer);

    @Override
    public AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        inRate = inputAudioFormat.sampleRate;
        channelCount = inputAudioFormat.channelCount;
        inputEncoding = inputAudioFormat.encoding;

        outRate = inRate;
        
        // 96kHz 24비트 -> 48kHz 16비트로 C++ 네이티브 다운샘플링
        if (inRate > 48000) {
            outRate = inRate / 2;
        }

        // 💥 [안전 장치 강화] 
        // 96kHz 이상 음원이 들어오면 (그게 16비트든 24비트든) 무조건 48kHz로 다운샘플링 해야만
        // 젤리빈 기기(최대 48kHz 지원)의 AudioTrack이 뻗거나 스킵하는 것을 막을 수 있습니다.
        boolean needsDownsample = (inRate > 48000 && channelCount == 2);
        boolean is24Bit = (inputEncoding != C.ENCODING_PCM_16BIT);
        isActive = needsDownsample || (is24Bit && channelCount == 2);

        if (isActive) {
            return new AudioFormat(outRate, channelCount, C.ENCODING_PCM_16BIT);
        }
        
        // 16비트 음원은 아무 처리도 하지 않고 그대로 다음 파이프라인으로 통과시킵니다.
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

        // 출력 버퍼 크기 계산 (24비트 96kHz -> 16비트 48kHz)
        // 24비트 96kHz 프레임은 3바이트 * 2채널 = 6바이트
        // 16비트 48kHz 프레임은 2바이트 * 2채널 = 4바이트
        // 게다가 샘플 레이트가 절반이므로, 입력 바이트 수의 정확히 1/3이 출력 크기가 됨.
        // ex) 60바이트 입력 -> 30바이트 출력(샘플수 반) -> 16비트로 변환하므로 다시 2/3 = 20바이트
        int maxOutputSize = inputSize; // 넉넉하게 입력 크기와 동일하게 할당

        if (buffer.capacity() < maxOutputSize) {
            buffer = ByteBuffer.allocateDirect(maxOutputSize).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }

        // C++ 네이티브 호출 (Zero-copy 다이렉트 버퍼)
        int bytesWritten = 0;

        if (inputBuffer.isDirect() && buffer.isDirect()) {
            inputBuffer.position(position);
            // 💥 [핵심 수리] 16비트와 24비트를 분기하여 각각 맞는 C++ 함수를 호출합니다!
            // 이렇게 하면 16비트 96kHz FLAC이 들어와도 완벽하게 48kHz로 낮춰져 기기 호환성 에러(스킵)를 방지합니다.
            if (inputEncoding == C.ENCODING_PCM_16BIT) {
                bytesWritten = nativeProcess16Bit96kHzTo16Bit48kHz(inputBuffer, inputSize, buffer);
            } else {
                bytesWritten = nativeProcess24Bit96kHzTo16Bit48kHz(inputBuffer, inputSize, buffer);
            }
            inputBuffer.position(limit);
        } else {
            inputBuffer.position(limit);
        }

        if (bytesWritten > 0) {
            buffer.limit(bytesWritten);
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
        return inputEnded && buffer == EMPTY_BUFFER && outputBuffer == EMPTY_BUFFER;
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
        inRate = -1;
        outRate = -1;
        channelCount = -1;
        inputEncoding = -1;
        isActive = false;
    }
}
