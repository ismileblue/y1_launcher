package com.themoon.y1.managers;

import com.themoon.y1.StoragePaths;

import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.widget.Toast;
import com.themoon.y1.MainActivity;
import java.io.File;

public class AudioEffectManager {
    private static AudioEffectManager instance;

    private AudioEffectManager() {}

    public static synchronized AudioEffectManager getInstance() {
        if (instance == null) {
            instance = new AudioEffectManager();
        }
        return instance;
    }

    // 🎧 1. 오디오 이펙트 하드웨어 칩셋 항시 대기 및 재연결
    public void ensureAudioEffectsReady() {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        try {
            // 🚀 [수정] 낡은 mediaPlayer 대신, 새 매니저의 다리(Bridge) 함수를 통해 안전하게 세션 ID를 가져옵니다!
            int sessionId = com.themoon.y1.managers.AudioPlayerManager.getInstance().getAudioSessionId();
            if (main.equalizer == null || main.currentAudioSessionId != sessionId) {
                if (main.equalizer != null) main.equalizer.release();
                main.equalizer = new Equalizer(0, sessionId); main.equalizer.setEnabled(true);
            }
            if (main.bassBoost == null || main.currentAudioSessionId != sessionId) {
                if (main.bassBoost != null) main.bassBoost.release();
                main.bassBoost = new android.media.audiofx.BassBoost(0, sessionId); main.bassBoost.setEnabled(true);
            }
            if (main.virtualizer == null || main.currentAudioSessionId != sessionId) {
                if (main.virtualizer != null) main.virtualizer.release();
                main.virtualizer = new android.media.audiofx.Virtualizer(0, sessionId); main.virtualizer.setEnabled(true);
            }
            main.currentAudioSessionId = sessionId;
        } catch (Exception e) {}
    }

    // 🎧 2. 4구 베이스 부스터 및 공간감 이펙터 물리 주파수 압력 매핑
    public void applyAudioEffects() {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        ensureAudioEffectsReady();

        com.themoon.y1.managers.AudioPlayerManager.getInstance().crossfeedProcessor.setIntensity(main.currentCrossfeedStep);

        if (main.bassBoost == null || main.virtualizer == null) return;
        try {
            short bassStrength = (short) (main.currentBassBoostStep * 333);
            if (main.currentBassBoostStep == 3) bassStrength = 1000;
            if (main.bassBoost.getStrengthSupported()) main.bassBoost.setStrength(bassStrength);

            short virtStrength = (short) (main.currentVirtualizerStep * 333);
            if (main.currentVirtualizerStep == 3) virtStrength = 1000;
            if (main.virtualizer.getStrengthSupported()) main.virtualizer.setStrength(virtStrength);
        } catch (Exception e) {}
    }

    // 🎧 3. 순정 프리셋 및 커스텀 fader 밴드 데이터 로드 (하이브리드 스위칭 엔진 적용)
    public void applyEqProfile() {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        ensureAudioEffectsReady();

        try {
            // 🔴 [소프트웨어 10밴드 모드]
            if (main.isSoftwareEqEnabled) {
                if (main.equalizer != null) main.equalizer.setEnabled(false); // 순정 하드웨어 전원 차단!
                com.themoon.y1.managers.AudioPlayerManager.getInstance().customEqProcessor.setEnabled(true); // 소프트웨어 DSP 가동!

                // 소프트웨어 모드에선 프리셋(Rock, Pop 등)을 무시하고 무조건 커스텀(10밴드) 설정만 읽어옵니다.
                String name = main.currentEqProfile.replace("custom_", "");
                if (main.currentEqProfile.startsWith("preset_")) name = "Flat"; // 프리셋일 땐 임시로 0점(Flat) 세팅

                for (short i = 0; i < 10; i++) {
                    int level = main.prefs.getInt("eq_custom_" + name + "_band_" + i, 0);
                    main.customBandLevels[i] = level;
                    // 🚀 (-1500 ~ 1500) 값을 (-15.0f ~ 15.0f) 실수형으로 변환하여 수학 공식에 즉시 주입!
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().customEqProcessor.setBandLevel(i, level / 100.0f);
                }
                main.prefs.edit().putString("eq_profile_id", main.currentEqProfile).commit();
            }
            // 🟢 [하드웨어 5밴드 모드]
            else {
                com.themoon.y1.managers.AudioPlayerManager.getInstance().customEqProcessor.setEnabled(false); // 소프트웨어 DSP 차단!
                if (main.equalizer == null) return;
                main.equalizer.setEnabled(true); // 하드웨어 칩셋 가동!

                if (main.currentEqProfile.startsWith("preset_")) {
                    int pIdx = Integer.parseInt(main.currentEqProfile.replace("preset_", ""));
                    if (pIdx < main.equalizer.getNumberOfPresets()) {
                        main.equalizer.usePreset((short) pIdx);
                        main.currentEqPresetIndex = pIdx;
                        main.prefs.edit().putInt("eq_preset", main.currentEqPresetIndex).putString("eq_profile_id", main.currentEqProfile).commit();
                    }
                } else {
                    String name = main.currentEqProfile.replace("custom_", "");
                    short bands = main.equalizer.getNumberOfBands();
                    for (short i = 0; i < bands; i++) {
                        int level = main.prefs.getInt("eq_custom_" + name + "_band_" + i, 0);
                        main.customBandLevels[i] = level;
                        main.equalizer.setBandLevel(i, (short) level);
                    }
                    main.prefs.edit().putString("eq_profile_id", main.currentEqProfile).commit();
                }
            }
        } catch (Exception e) {}
    }

    // 🎧 4. 로컬 공유 메모리 저장
    public void saveCustomEqProfile(String name) {
        MainActivity main = MainActivity.instance;
        if (main == null || main.equalizer == null) return;
        short bands = main.equalizer.getNumberOfBands();
        SharedPreferences.Editor editor = main.prefs.edit();
        for (short i = 0; i < bands; i++) {
            editor.putInt("eq_custom_" + name + "_band_" + i, main.customBandLevels[i]);
        }
        editor.commit();
    }

    // 🎧 5. 내부 금고 및 외부 JSON 백업 물리 소멸 삭제
    public void deleteCustomEqProfile(String name) {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        try {
            File file = new File(StoragePaths.getEqDir(), name + ".json");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String listStr = main.prefs.getString("custom_eq_list", "");
        String[] items = listStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (!item.equals(name) && !item.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(item);
            }
        }

        SharedPreferences.Editor editor = main.prefs.edit();
        editor.putString("custom_eq_list", sb.toString());

        for (int i = 0; i < 32; i++) {
            editor.remove("eq_custom_" + name + "_band_" + i);
        }
        editor.commit();

        Toast.makeText(main, "Profile Deleted Completely.", Toast.LENGTH_SHORT).show();
    }

    // 🎧 6. [외부 공유용] JSON 배출 엔진
    public void exportEqProfileToFile(String name) {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        try {
            File folder = StoragePaths.getEqDir();
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, name + ".json");

            org.json.JSONObject json = new org.json.JSONObject();
            json.put("profile_name", name);

            org.json.JSONArray bandsArray = new org.json.JSONArray();
            short bands = (main.equalizer != null) ? main.equalizer.getNumberOfBands() : 5;
            for (short i = 0; i < bands; i++) {
                bandsArray.put(main.prefs.getInt("eq_custom_" + name + "_band_" + i, 0));
            }
            json.put("bands", bandsArray);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(json.toString(2).getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🎧 7. [양방향 동기화] 폴더 스캔 및 자동 주입 구조
    public void loadAndSyncExternalEqProfiles() {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        try {
            File folder = StoragePaths.getEqDir();
            if (!folder.exists()) { folder.mkdirs(); return; }
            File[] files = folder.listFiles();

            java.util.ArrayList<String> validCustomList = new java.util.ArrayList<>();

            if (files != null) {
                SharedPreferences.Editor editor = main.prefs.edit();
                for (File f : files) {
                    if (f.isFile() && f.getName().toLowerCase().endsWith(".json")) {
                        try {
                            java.io.FileInputStream fis = new java.io.FileInputStream(f);
                            byte[] data = new byte[(int) f.length()];
                            fis.read(data);
                            fis.close();

                            org.json.JSONObject json = new org.json.JSONObject(new String(data, "UTF-8"));
                            String name = json.optString("profile_name", f.getName().replace(".json", ""));
                            org.json.JSONArray bandsArray = json.optJSONArray("bands");

                            if (bandsArray != null) {
                                for (int i = 0; i < bandsArray.length(); i++) {
                                    editor.putInt("eq_custom_" + name + "_band_" + i, bandsArray.getInt(i));
                                }
                                if (!validCustomList.contains(name)) {
                                    validCustomList.add(name);
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < validCustomList.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(validCustomList.get(i));
                }
                editor.putString("custom_eq_list", sb.toString());
                editor.commit();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}