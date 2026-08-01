package com.themoon.y1.managers;

import com.themoon.y1.StoragePaths;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudiobookManager {
    private static AudiobookManager instance;
    private SharedPreferences bookPrefs;
    private File audiobookRoot = StoragePaths.getAudiobooksDir();

    private List<File> currentBookChapters = new ArrayList<>();
    private int currentChapterIndex = -1;

    private AudiobookManager(Context context) {
        // 오디오북 전용 독립 금고 (음악 설정 파일과 완전히 분리)
        this.bookPrefs = context.getSharedPreferences("Y1_AUDIOBOOK_BOOKMARKS", Context.MODE_PRIVATE);
        if (!audiobookRoot.exists()) {
            audiobookRoot.mkdirs();
        }
    }

    public static synchronized AudiobookManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudiobookManager(context.getApplicationContext());
        }
        return instance;
    }

    public File getRootFolder() {
        return audiobookRoot;
    }

    // 🚀 [북마크 저장] 책 파일 경로별로 마지막 들은 위치(ms)를 기록
    public void saveBookmark(String filePath, int positionMs, int chapterIdx) {
        if (filePath == null || filePath.isEmpty()) return;
        bookPrefs.edit()
                .putInt("POS_" + filePath, positionMs)
                .putInt("CHAP_" + filePath, chapterIdx)
                .apply(); // 백그라운드 비동기 저장으로 UI 멈춤 방지
    }

    // 🚀 [북마크 로드] 저장된 이어듣기 위치 반환
    public int getSavedPosition(String filePath) {
        return bookPrefs.getInt("POS_" + filePath, 0);
    }

    // 오디오북 전용 플레이리스트 장전 및 이어듣기 연동
    public void setupBookPlaylist(Context context, File clickedFile, File parentFolder) {
        currentBookChapters.clear();

        File[] files = parentFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (isAudioFile(f)) currentBookChapters.add(f);
            }
            java.util.Collections.sort(currentBookChapters, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        }

        this.currentChapterIndex = currentBookChapters.indexOf(clickedFile);
        if (this.currentChapterIndex == -1) this.currentChapterIndex = 0;

        // 저장된 이어듣기 위치(ms)를 가져옵니다.
        int savedOffset = getSavedPosition(clickedFile.getAbsolutePath());

        // 🚀 기존 음악 엔진을 활용하되, 오디오북 전용 오프셋 재생 메서드를 호출합니다.
        AudioPlayerManager.getInstance().playTrackListWithOffset(currentBookChapters, currentChapterIndex, savedOffset);
    }

    private boolean isAudioFile(File f) {
        if (f == null || !f.isFile()) return false;
        String name = f.getName().toLowerCase();
        // 🚀 [해결] 좁았던 출입문을 활짝 열고 .opus 를 포함한 모든 음악 확장자를 허용합니다!
        return name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".flac") || name.endsWith(".wav")
                || name.endsWith(".ogg") || name.endsWith(".aac") || name.endsWith(".ape") || name.endsWith(".wma")
                || name.endsWith(".opus");
    }

}