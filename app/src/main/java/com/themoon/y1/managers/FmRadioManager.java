package com.themoon.y1.managers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import java.lang.reflect.Method;

public class FmRadioManager {
    private static FmRadioManager instance;
    private Context context;
    private AudioManager audioManager;

    public boolean isDeviceOpen = false;
    public boolean isPowerUp = false;
    public float currentFreq = 87.5f; // 기본 주파수
    public boolean isSpeakerOn = false;

    public String lastError = "";

    private Class<?> fmNativeClass;
    private MediaPlayer fmPlayer; // 🚀 [핵심] 소리를 스피커로 빼내 줄 미디어 플레이어

    private FmRadioManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);

        try {
            fmNativeClass = Class.forName("com.mediatek.FMRadio.FMRadioNative");
        } catch (Throwable t1) {
            String[] jarPaths = {
                    "/system/framework/mediatek-framework.jar",
                    "/system/framework/custom_ext.jar",
                    "/system/framework/com.mediatek.hardware.jar"
            };
            for (String path : jarPaths) {
                if (new java.io.File(path).exists()) {
                    try {
                        dalvik.system.PathClassLoader cl = new dalvik.system.PathClassLoader(path, ClassLoader.getSystemClassLoader());
                        fmNativeClass = Class.forName("com.mediatek.FMRadio.FMRadioNative", true, cl);
                        break;
                    } catch (Throwable t2) {}
                }
            }
            if (fmNativeClass == null) {
                String[] packages = {"com.mediatek.FMRadio", "com.innioasis.fm", "com.innioasis.y1"};
                for (String pkg : packages) {
                    try {
                        android.content.pm.ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkg, 0);
                        dalvik.system.PathClassLoader cl = new dalvik.system.PathClassLoader(info.sourceDir, ClassLoader.getSystemClassLoader());
                        fmNativeClass = Class.forName("com.mediatek.FMRadio.FMRadioNative", true, cl);
                        break;
                    } catch (Throwable t3) {}
                }
            }
        }

        if (fmNativeClass == null) {
            lastError = "FMRadioNative Driver completely missing.";
        } else {
            try { System.loadLibrary("fmjni"); } catch (Throwable t) {
                try { System.load("/system/lib/libfmjni.so"); } catch (Throwable ignore) {}
            }
        }
    }

    public static FmRadioManager getInstance(Context context) {
        if (instance == null) instance = new FmRadioManager(context);
        return instance;
    }

    private Method getNativeMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> clazz = fmNativeClass;
        while (clazz != null) {
            try {
                Method m = clazz.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) { clazz = clazz.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }

    // 🚀 [신규 핵심 기술] 칩셋의 소리를 안드로이드 스피커/이어폰으로 연결해 주는 함수!
    private void startFmAudio() {
        try {
            if (fmPlayer != null) {
                fmPlayer.release();
            }
            fmPlayer = new MediaPlayer();

            // =========================================================
            // 🚀 [라디오 백그라운드 무한 재생 보호막 장착!]
            // 화면이 꺼졌을 때 안드로이드 시스템이 MediaPlayer를 잠재우는 것을 막기 위해,
            // WakeMode를 'PARTIAL_WAKE_LOCK'으로 강제 세팅하여 CPU가 계속 소리를 뿜어내도록 만듭니다!
            // =========================================================
            fmPlayer.setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK);

            // 💡 미디어텍 전용 숨겨진 FM 라디오 오디오 스트림 주소
            fmPlayer.setDataSource("MEDIATEK://MEDIAPLAYER_PLAYERTYPE_FM");

            // 💡 숨겨진 STREAM_FM(보통 10번) 채널을 찾아서 볼륨을 물립니다.
            int streamFm = 10;
            try { streamFm = (Integer) AudioManager.class.getDeclaredField("STREAM_FM").get(null); } catch (Exception e) {}

            fmPlayer.setAudioStreamType(streamFm);
            fmPlayer.prepare();
            fmPlayer.start();

            // 🚀 [버그 수리 2] 시스템이 멋대로 스피커로 소리를 빼버리는 것을 막고, 현재 UI에 설정된 출력(isSpeakerOn) 상태로 물리적 강제 고정!
            setSpeaker(isSpeakerOn);

        } catch (Throwable t) {
            lastError = "Audio Routing Failed: " + t.getMessage();
        }
    }

    // 🚀 [신규 기술] 라디오 소리 끄기
    private void stopFmAudio() {
        if (fmPlayer != null) {
            try {
                if (fmPlayer.isPlaying()) fmPlayer.stop();
                fmPlayer.release();
            } catch (Throwable t) {}
            fmPlayer = null;
        }
    }

    // 1. 하드웨어 전원 켜기
    public boolean powerUp(float freq) {
        if (fmNativeClass == null) {
            if (lastError.isEmpty()) lastError = "Driver class is null.";
            return false;
        }
        try {
            // 🚀 1. 백그라운드에 숨어있는 순정 라디오 앱들을 모두 확실하게 사살하여 점유를 해제합니다.
            Runtime.getRuntime().exec(new String[]{"su", "-c", "killall com.mediatek.FMRadio"});
            Runtime.getRuntime().exec(new String[]{"su", "-c", "killall com.innioasis.fm"});
            Runtime.getRuntime().exec(new String[]{"su", "-c", "killall com.android.fmradio"});

            // 🚀 2. [가장 핵심!] 시스템이 꽉 쥐고 있는 FM 하드웨어 칩셋(/dev/fm)의 권한을 모든 앱이 쓸 수 있도록 강제 개방(chmod 666)합니다!
            Runtime.getRuntime().exec(new String[]{"su", "-c", "chmod 666 /dev/fm"});

            Thread.sleep(300); // 💡 권한이 적용되고 칩셋이 정신을 차릴 시간 0.3초 부여

            // 🚀 3. 혹시 이전 연결이 비정상적으로 꼬여서 열려있다면, 강제로 한 번 닫아버리고 뇌관을 초기화합니다.
            try {
                Method closeDev = getNativeMethod("closedev");
                closeDev.invoke(null);
            } catch (Throwable ignore) {}

            isDeviceOpen = false;

            // 🚀 4. 방해물이 모두 사라진 깨끗한 상태에서 칩셋을 엽니다!
            if (!isDeviceOpen) {
                Method openDev = getNativeMethod("opendev");
                isDeviceOpen = (Boolean) openDev.invoke(null);
            }

            if (!isDeviceOpen) {
                lastError = "Failed to open /dev/fm (Hardware is busy or blocked)";
                return false;
            }

            Method powerUp = getNativeMethod("powerup", float.class);
            isPowerUp = (Boolean) powerUp.invoke(null, freq);

            if (isPowerUp) {
                currentFreq = freq;
                setMute(false);
                startFmAudio(); // 🚀 전원이 켜지면 소리 통로를 연결합니다!
            } else {
                try {
                    Method switchAntenna = getNativeMethod("switchAntenna", int.class);
                    switchAntenna.invoke(null, 1);

                    isPowerUp = (Boolean) powerUp.invoke(null, freq);
                    if (isPowerUp) {
                        currentFreq = freq;
                        setMute(false);
                        startFmAudio(); // 🚀 안테나 우회 성공 시에도 소리 통로 연결!
                    } else {
                        lastError = "Power up rejected by Hardware.";
                    }
                } catch (Throwable ex) {
                    lastError = "Earphones required and Antenna bypass failed.";
                }
            }
            return isPowerUp;
        } catch (Throwable t) {
            lastError = "Exception: " + t.getClass().getSimpleName() + " - " + t.getMessage();
        }
        return false;
    }
    // 2. 하드웨어 전원 끄기
    public void powerDown() {
        if (fmNativeClass == null || !isPowerUp) return;
        try {
            stopFmAudio(); // 🚀 전원 끌 때 소리 통로도 같이 뽑아줍니다!
            setMute(true);
            Method powerDown = getNativeMethod("powerdown", int.class);
            powerDown.invoke(null, 0);

            Method closeDev = getNativeMethod("closedev");
            closeDev.invoke(null);

            isPowerUp = false;
            isDeviceOpen = false;
        } catch (Throwable e) { }
    }

    // 3. 주파수 수동 맞추기 (Manual Tuning)
    public boolean tune(float freq) {
        if (fmNativeClass == null || !isPowerUp) return false;
        try {
            Method tuneMethod = getNativeMethod("tune", float.class);
            boolean success = (Boolean) tuneMethod.invoke(null, freq);
            if (success) currentFreq = freq;
            return success;
        } catch (Throwable e) { return false; }
    }

    // 🚀 4. 자동 스캔 엔진 (Auto Scan)
    public float[] autoScan() {
        if (fmNativeClass == null || !isPowerUp) {
            lastError = "Turn on the radio first.";
            return null;
        }
        try {
            Method autoScanMethod = getNativeMethod("autoscan");
            short[] result = (short[]) autoScanMethod.invoke(null);

            if (result != null && result.length > 0) {
                float[] freqs = new float[result.length];
                for (int i = 0; i < result.length; i++) {
                    freqs[i] = result[i] / 10.0f; // 미디어텍은 875를 87.5로 반환하므로 소수점 변환
                }
                return freqs;
            }
        } catch (Throwable t) {
            lastError = "AutoScan failed: " + t.getMessage();
        }
        return null;
    }

    // 5. 음소거 제어
    public void setMute(boolean mute) {
        if (fmNativeClass == null) return;
        try {
            Method setMuteMethod = getNativeMethod("setmute", boolean.class);
            setMuteMethod.invoke(null, mute);
        } catch (Throwable e) {}
    }

    // 6. 스피커 출력 강제 변경
    public void setSpeaker(boolean useSpeaker) {
        try {
            Method setForceUse = Class.forName("android.media.AudioSystem").getDeclaredMethod("setForceUse", int.class, int.class);
            setForceUse.setAccessible(true);
            setForceUse.invoke(null, 5, useSpeaker ? 1 : 0);
            isSpeakerOn = useSpeaker;
        } catch (Throwable e) {}
    }
}