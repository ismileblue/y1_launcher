package com.themoon.y1.managers;

import com.themoon.y1.StoragePaths;

import android.content.Context;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LanguageManager {
    private static LanguageManager instance;
    private Context context;

    // 💡 번역된 단어들이 저장될 메모리 단어장
    private HashMap<String, String> dictionary = new HashMap<>();

    public List<File> availableLangFiles = new ArrayList<>();
    public String currentLangFileName = "English (Default)";

    private LanguageManager(Context context) {
        this.context = context.getApplicationContext();
        loadAvailableLanguages();
    }

    public static LanguageManager getInstance(Context context) {
        if (instance == null) instance = new LanguageManager(context);
        return instance;
    }

    // 1. 폴더에서 .json 언어팩 파일들을 스캔합니다.
    public void loadAvailableLanguages() {
        availableLangFiles.clear();
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
    }

    // 2. 선택된 언어팩 JSON 파일을 읽어와 단어장에 등록합니다.
    public void applyLanguage(String fileName) {
        dictionary.clear();
        currentLangFileName = fileName;

        if (fileName.equals("English (Default)")) return; // 기본값일 경우 빈 단어장 유지 (원본 출력)

        try {
            File f = new File(StoragePaths.getLanguagesDir(), fileName);
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                byte[] data = new byte[(int) f.length()];
                fis.read(data);
                fis.close();

                String jsonStr = new String(data, "UTF-8");
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
        }
    }

    // 🚀 [핵심 기술] 화면에 글씨를 그리기 직전에, 단어장을 뒤져보고 번역본이 있으면 바꿔서 내보냅니다!
    public String t(String originalText) {
        if (originalText == null) return "";

        // 아이콘 등 보이지 않는 문자가 앞에 섞여 있을 경우를 대비해 원본 그대로 검색
        if (dictionary.containsKey(originalText)) {
            return dictionary.get(originalText);
        }

        // 만약 완벽히 일치하지 않는다면 양쪽 공백을 제거하고 다시 한 번 검색
        String trimmed = originalText.trim();
        if (dictionary.containsKey(trimmed)) {
            // 원본의 앞뒤 공백이나 이모지 형태를 유지하기 위해 살짝 가공
            return originalText.replace(trimmed, dictionary.get(trimmed));
        }

        // 번역팩에 해당 단어가 없으면 그냥 원래 영어 단어를 그대로 내보냅니다.
        return originalText;
    }
}