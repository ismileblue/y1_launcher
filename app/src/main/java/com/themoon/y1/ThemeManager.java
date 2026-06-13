package com.themoon.y1;

import android.graphics.Color;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    // 💡 개별 테마의 정보를 담는 그릇 (버튼 둥글기 추가!)
    public static class ThemeData {
        public String folderPath;
        public String name;
        public android.graphics.Typeface customFont;
        public int textPrimary;
        public int textSecondary;
        public int bgOverlay;
        public int statusBarBg;
        public int btnNormal;
        public int btnFocused;
        public int btnFocusedText;
        public int buttonRadius; // 🚀 [추가] 버튼의 둥글기(모서리) 값

        public ThemeData(String folderPath, String name, android.graphics.Typeface customFont, int textPrimary, int textSecondary, int bgOverlay, int statusBarBg, int btnNormal, int btnFocused, int btnFocusedText, int buttonRadius) {
            this.folderPath = folderPath;
            this.name = name;
            this.customFont = customFont;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.bgOverlay = bgOverlay;
            this.statusBarBg = statusBarBg;
            this.btnNormal = btnNormal;
            this.btnFocused = btnFocused;
            this.btnFocusedText = btnFocusedText;
            this.buttonRadius = buttonRadius; // 🚀 장착 완료
        }
    }

    public static List<ThemeData> availableThemes = new ArrayList<>();
    private static int currentThemeIndex = 0;

    // 🚀 [추가] 폴더 안에 커스텀 아이콘(png)이 있으면 가져오고, 없으면 안드로이드 기본 아이콘을 쓰는 마법의 함수!
    public static android.graphics.Bitmap getCustomIcon(String iconFileName, android.content.Context context, int defaultResId) {
        if (!availableThemes.isEmpty()) {
            String folder = getCurrentTheme().folderPath;
            if (!folder.equals("default")) {
                File iconFile = new File(folder, iconFileName);
                if (iconFile.exists()) {
                    try {
                        return android.graphics.BitmapFactory.decodeFile(iconFile.getAbsolutePath());
                    } catch (Exception e) {}
                }
            }
        }
        // 폴더에 이미지가 없거나 에러가 나면, 기존에 쓰던 기본 아이콘을 안전하게 반환합니다.
        return android.graphics.BitmapFactory.decodeResource(context.getResources(), defaultResId);
    }

    public static int getStatusBarBackgroundColor() {
        if (availableThemes.isEmpty()) return 0x66000000;
        return availableThemes.get(currentThemeIndex).statusBarBg;
    }

    public static void loadThemesFromStorage(File themeFolder) {
        availableThemes.clear();

        // 🚀 [수정] 둥글기 기본값을 0(직각)으로 넣습니다.
        availableThemes.add(new ThemeData("default", "Dark (Default)", android.graphics.Typeface.DEFAULT,
                0xFFFFFFFF, 0xFF888888, 0x88000000, 0x88000000, 0x15FFFFFF, 0xDDFFFFFF, 0xFF000000, 0));

        if (!themeFolder.exists()) {
            themeFolder.mkdirs();
            createSampleThemeFolder(themeFolder);
        }

        File[] folders = themeFolder.listFiles();
        if (folders != null) {
            for (File subFolder : folders) {
                if (subFolder.isDirectory()) {
                    File configFile = new File(subFolder, "config.json");
                    if (configFile.exists() && configFile.isFile()) {
                        try {
                            FileInputStream fis = new FileInputStream(configFile);
                            byte[] data = new byte[(int) configFile.length()];
                            fis.read(data);
                            fis.close();

                            String jsonStr = new String(data, "UTF-8");
                            JSONObject json = new JSONObject(jsonStr);

                            int parsedOverlayBg = Color.parseColor(json.getString("bgOverlay"));
                            int parsedStatusBarBg = json.has("statusBarBg") ? Color.parseColor(json.getString("statusBarBg")) : parsedOverlayBg;

                            android.graphics.Typeface parsedFont = android.graphics.Typeface.DEFAULT;
                            if (json.has("font")) {
                                File fontFile = new File(subFolder, json.getString("font"));
                                if (fontFile.exists() && fontFile.isFile()) {
                                    try { parsedFont = android.graphics.Typeface.createFromFile(fontFile); } catch (Exception e) {}
                                }
                            }

                            // 🚀 [추가] JSON에서 "button_radius"를 읽어옵니다. (없으면 기본값 10)
                            int parsedRadius = json.has("button_radius") ? json.getInt("button_radius") : 10;

                            ThemeData theme = new ThemeData(
                                    subFolder.getAbsolutePath(),
                                    json.getString("name"),
                                    parsedFont,
                                    Color.parseColor(json.getString("textPrimary")),
                                    Color.parseColor(json.getString("textSecondary")),
                                    parsedOverlayBg,
                                    parsedStatusBarBg,
                                    Color.parseColor(json.getString("btnNormal")),
                                    Color.parseColor(json.getString("btnFocused")),
                                    Color.parseColor(json.getString("btnFocusedText")),
                                    parsedRadius // 🚀 파싱한 둥글기 값 탑재
                            );

                            availableThemes.add(theme);
                        } catch (Exception e) {}
                    }
                }
            }
        }
    }

    private static void createSampleThemeFolder(File rootFolder) {
        try {
            File sampleFolder = new File(rootFolder, "Ocean_Blue");
            if (!sampleFolder.exists()) sampleFolder.mkdirs();

            File configFile = new File(sampleFolder, "config.json");
            String json = "{\n" +
                    "  \"name\": \"Ocean Blue\",\n" +
                    "  \"font\": \"myfont.ttf\",\n" +
                    "  \"textPrimary\": \"#FFFFFF\",\n" +
                    "  \"textSecondary\": \"#88AADD\",\n" +
                    "  \"bgOverlay\": \"#DD0F172A\",\n" +
                    "  \"statusBarBg\": \"#99002255\",\n" +
                    "  \"btnNormal\": \"#221E40AF\",\n" +
                    "  \"btnFocused\": \"#DD3B82F6\",\n" +
                    "  \"btnFocusedText\": \"#000000\",\n" +
                    "  \"button_radius\": 30\n" + // 🚀 샘플에 둥글기 값 힌트 추가 (30 = 상당히 둥근 형태)
                    "}";
            FileOutputStream fos = new FileOutputStream(configFile);
            fos.write(json.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {}
    }

    public static void setThemeIndex(int index) {
        if (index >= 0 && index < availableThemes.size()) currentThemeIndex = index;
        else currentThemeIndex = 0;
    }

    public static int getCurrentThemeIndex() { return currentThemeIndex; }
    public static ThemeData getCurrentTheme() { return availableThemes.get(currentThemeIndex); }
    public static android.graphics.Typeface getCustomFont() {
        if (availableThemes.isEmpty()) return android.graphics.Typeface.DEFAULT;
        return availableThemes.get(currentThemeIndex).customFont;
    }

    public static int getTextColorPrimary() { return getCurrentTheme().textPrimary; }
    public static int getTextColorSecondary() { return getCurrentTheme().textSecondary; }
    public static int getOverlayBackgroundColor() { return getCurrentTheme().bgOverlay; }
    public static int getListButtonNormalBg() { return getCurrentTheme().btnNormal; }
    public static int getListButtonFocusedBg() { return getCurrentTheme().btnFocused; }
    public static int getListButtonFocusedTextColor() { return getCurrentTheme().btnFocusedText; }
    public static int getButtonRadius() { return getCurrentTheme().buttonRadius; } // 🚀 [추가] 둥글기 Getter
}