package com.themoon.y1.managers;

import com.themoon.y1.StoragePaths;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LanguageManager {
    private static LanguageManager instance;
    private final Context context;
    private final SharedPreferences prefs;

    // 💡 번역된 단어들이 저장될 메모리 단어장
    private final HashMap<String, String> dictionary = new HashMap<>();

    public List<File> availableLangFiles = new ArrayList<>();
    public String currentLangFileName = "English (Default)";

    private LanguageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("Y1_SETTINGS", Context.MODE_PRIVATE);
        loadAvailableLanguages();
        String saved = prefs.getString("app_language", "English (Default)");
        applyLanguage(saved);
    }

    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) instance = new LanguageManager(context);
        return instance;
    }

    // 1. assets 및 SD카드 폴더에서 .json 언어팩 파일들을 스캔합니다.
    public void loadAvailableLanguages() {
        availableLangFiles.clear();
        
        // 1-1. 기기 저장소 폴더 스캔
        File langDir = StoragePaths.getLanguagesDir();
        if (!langDir.exists()) langDir.mkdirs();

        File[] files = langDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().toLowerCase().endsWith(".json")) {
                    availableLangFiles.add(f);
                }
            }
        }

        // 1-2. 만약 저장소에 파일이 없거나 기본 언어팩이 누락되었으면 assets 기준 목록 확보
        try {
            String[] assetLangs = context.getAssets().list("languages");
            if (assetLangs != null) {
                for (String name : assetLangs) {
                    if (name.toLowerCase().endsWith(".json")) {
                        File virtualFile = new File(langDir, name);
                        boolean alreadyAdded = false;
                        for (File existing : availableLangFiles) {
                            if (existing.getName().equalsIgnoreCase(name)) {
                                alreadyAdded = true;
                                break;
                            }
                        }
                        if (!alreadyAdded) {
                            availableLangFiles.add(virtualFile);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // 2. 선택된 언어팩 JSON 파일을 읽어와 단어장에 등록합니다.
    public void applyLanguage(String fileName) {
        dictionary.clear();
        currentLangFileName = fileName != null ? fileName : "English (Default)";

        if (prefs != null) {
            prefs.edit().putString("app_language", currentLangFileName).commit();
        }

        if ("English (Default)".equalsIgnoreCase(currentLangFileName)) return; // 기본값일 경우 빈 단어장 유지 (원본 영어 출력)

        InputStream is = null;
        try {
            // 1순위: 외장/내장 저장소의 Y1_Languages 폴더에서 파일 읽기
            File f = new File(StoragePaths.getLanguagesDir(), currentLangFileName);
            if (f.exists() && f.length() > 0) {
                is = new FileInputStream(f);
            } else {
                // 2순위: 부팅 직후 SD카드가 아직 마운트되지 않았거나 파일이 없는 경우, APK 내장 assets에서 직접 읽기!
                is = context.getAssets().open("languages/" + currentLangFileName);
            }

            if (is != null) {
                byte[] data = new byte[is.available()];
                int read = is.read(data);
                is.close();

                String jsonStr = new String(data, 0, read, "UTF-8");
                JSONObject json = new JSONObject(jsonStr);

                java.util.Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String originalText = keys.next();
                    String translatedText = json.getString(originalText);
                    dictionary.put(originalText, translatedText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 혹시 대소문자 문제(korean.json vs Korean.json)일 수 있으므로 소문자로 재시도
            try {
                if (is == null && !currentLangFileName.equals(currentLangFileName.toLowerCase())) {
                    InputStream fallbackIs = context.getAssets().open("languages/" + currentLangFileName.toLowerCase());
                    byte[] data = new byte[fallbackIs.available()];
                    int read = fallbackIs.read(data);
                    fallbackIs.close();
                    String jsonStr = new String(data, 0, read, "UTF-8");
                    JSONObject json = new JSONObject(jsonStr);
                    java.util.Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        String originalText = keys.next();
                        dictionary.put(originalText, json.getString(originalText));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    // 🚀 [초고속 단어장 검색 엔진 - O(1) 단일 룩업 최적화]
    public String t(String originalText) {
        if (originalText == null) return "";
        if (dictionary.isEmpty()) return originalText;

        String translated = dictionary.get(originalText);
        if (translated != null) {
            return translated;
        }

        String trimmed = originalText.trim();
        translated = dictionary.get(trimmed);
        if (translated != null) {
            return originalText.replace(trimmed, translated);
        }

        return originalText;
    }
}