package com.themoon.y1;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.media.audiofx.Equalizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class VideoPlayerActivity extends Activity {
    private VideoView videoView;
    private String videoPath;
    private LinearLayout layoutControls, layoutVolumeOverlay;
    private TextView tvCurrent, tvTotal, tvSubtitle;
    private ProgressBar progressVideo, volumeProgress;
    private ImageView ivPauseIcon;
    // 꾹 누르기(Seek) 연사 속도 조절용 변수
    private boolean isSeekPerformed = false;
    private long lastSeekTime = 0;
    private Handler uiHandler = new Handler();
    private boolean isUIHiding = false;

    // 볼륨 오버레이 자동 숨김용 타이머
    private Handler volumeHandler = new Handler();
    private Runnable hideVolumeTask = () -> layoutVolumeOverlay.setVisibility(View.GONE);

    // 자막(SRT) 파서 금고
    private TreeMap<Integer, String> subtitlesMap = new TreeMap<>();
    private AudioManager audioManager;

    @Override
    protected void onResume() {
        super.onResume();
        com.themoon.y1.managers.BatteryStatsManager.getInstance(this).setMode(com.themoon.y1.managers.BatteryStatsManager.MODE_VIDEO);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        // 비디오 재생 시 배경 음악 중지
        try {
            if (com.themoon.y1.managers.AudioPlayerManager.getInstance().isPlaying()) {
                com.themoon.y1.managers.AudioPlayerManager.getInstance().playOrPauseMusic();
            }
        } catch (Exception e) {}

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        videoView = findViewById(R.id.video_view);
        layoutControls = findViewById(R.id.layout_controls);
        tvCurrent = findViewById(R.id.tv_time_current);
        tvTotal = findViewById(R.id.tv_time_total);
        progressVideo = findViewById(R.id.progress_video);
        ivPauseIcon = findViewById(R.id.iv_pause_icon);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        layoutVolumeOverlay = findViewById(R.id.layout_volume_overlay);
        volumeProgress = findViewById(R.id.volume_progress);

        volumeProgress.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

        // 🚀 볼륨바 색상을 테마의 포커스 색상으로 맞춰줌!
        try {
            int themeFocusColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            volumeProgress.getProgressDrawable().setColorFilter(themeFocusColor,
                    android.graphics.PorterDuff.Mode.SRC_IN);
        } catch (Exception e) {
        }

        videoPath = getIntent().getStringExtra("VIDEO_PATH");

        if (videoPath == null || !new File(videoPath).exists()) {
            Toast.makeText(this, "⚠️ Invalid Video File", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        forceFiveBandAudioMode();
        loadSubtitles(videoPath);

        videoView.setVideoURI(Uri.parse(videoPath));
        videoView.setOnPreparedListener(mp -> {
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    float speed = com.themoon.y1.managers.AudioPlayerManager.getInstance().getCurrentSpeed();
                    if (speed != 1.0f) {
                        mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(speed));
                    }
                } catch (Exception e) {
                }
            }

            SharedPreferences prefs = getSharedPreferences("y1_prefs", MODE_PRIVATE);
            int savedPos = prefs.getInt("video_pos_" + videoPath, 0);
            
            if (savedPos > 0) {
                int duration = mp.getDuration();
                if (duration > 0 && savedPos >= duration - 5000) {
                    savedPos = 0;
                    prefs.edit().putInt("video_pos_" + videoPath, 0).apply();
                }
            }
            
            if (savedPos > 0) {
                mp.seekTo(savedPos);
                lastKnownPosition = savedPos;
            }

            videoView.start();
            showControls(false); // 재생 시작 시 3초 후 UI 자동 숨김
            uiHandler.post(updateUITask); // 실시간 재생 바 루프 가동
        });

        videoView.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING || what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                Toast.makeText(VideoPlayerActivity.this, "⚠️ 비디오가 무거워 재생이 지연될 수 있습니다.", Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        videoView.setOnCompletionListener(mp -> {
            getSharedPreferences("y1_prefs", MODE_PRIVATE).edit().putInt("video_pos_" + videoPath, 0).apply();
            lastKnownPosition = 0;
            finish();
        });
    }

    private Runnable hideUITask = () -> {
        layoutControls.setVisibility(View.GONE);
        isUIHiding = true;
    };

    private void showControls(boolean keepVisible) {
        layoutControls.setVisibility(View.VISIBLE);
        isUIHiding = false;
        uiHandler.removeCallbacks(hideUITask);
        if (!keepVisible) {
            uiHandler.postDelayed(hideUITask, 3000);
        }
    }

    // 🚀 [수정 완료] 메인 앱의 볼륨 엔진 100% 이식 (라디오 동기화 포함)
    private void adjustVolume(boolean up) {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (up && currentVol < maxVol)
            currentVol++;
        else if (!up && currentVol > 0)
            currentVol--;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);

        try {
            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
            if (fm != null && fm.isPowerUp) {
                int streamFm = 10;
                try {
                    streamFm = (Integer) AudioManager.class.getDeclaredField("STREAM_FM").get(null);
                } catch (Exception e) {
                }
                int fmMax = audioManager.getStreamMaxVolume(streamFm);
                int fmVol = (int) (((float) currentVol / maxVol) * fmMax);
                audioManager.setStreamVolume(streamFm, fmVol, 0);
            }
        } catch (Exception e) {
        }

        showDynamicVolumeOverlay();
    }

    // 🚀 [수정 완료] 오리지널 애니메이션을 위한 오버레이 호출 함수
    private void showDynamicVolumeOverlay() {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        layoutVolumeOverlay.setVisibility(View.VISIBLE);
        volumeProgress.setProgress(currentVol);
        
        // 테마 색상 적용 및 밝기 보정
        try {
            int baseColor = com.themoon.y1.ThemeManager.getListButtonFocusedBg();
            int r = android.graphics.Color.red(baseColor);
            int g = android.graphics.Color.green(baseColor);
            int b = android.graphics.Color.blue(baseColor);
            
            // 명도(Luma)가 100 미만으로 너무 어두우면 밝게 보정
            if ((r * 0.299 + g * 0.587 + b * 0.114) < 100) {
                r = Math.min(255, r + 80);
                g = Math.min(255, g + 80);
                b = Math.min(255, b + 80);
            }
            int finalColor = android.graphics.Color.argb(255, r, g, b);
            
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                volumeProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(finalColor));
            } else {
                volumeProgress.getProgressDrawable().setColorFilter(finalColor, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } catch (Exception e) {}

        volumeHandler.removeCallbacks(hideVolumeTask);
        volumeHandler.postDelayed(hideVolumeTask, 2000); // 2초 뒤에 사라짐
    }

    private Runnable updateUITask = new Runnable() {
        @Override
        public void run() {
            if (videoView != null && videoView.isPlaying()) {
                int current = videoView.getCurrentPosition();
                lastKnownPosition = current;
                int total = videoView.getDuration();

                tvCurrent.setText(formatTime(current));
                tvTotal.setText(formatTime(total));
                if (total > 0)
                    progressVideo.setProgress((int) (((float) current / total) * 100));

                // 자막 업데이트
                if (!subtitlesMap.isEmpty()) {
                    Map.Entry<Integer, String> entry = subtitlesMap.floorEntry(current);
                    if (entry != null && !entry.getValue().isEmpty()) {
                        tvSubtitle.setText(entry.getValue());
                        tvSubtitle.setVisibility(View.VISIBLE);
                    } else {
                        tvSubtitle.setVisibility(View.GONE);
                    }
                }
            }
            uiHandler.postDelayed(this, 300);
        }
    };

    // 🚀 통합 물리 키 조작 엔진
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // 🚀 1. 뒤로 가기
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }

        // 🚀 2. 휠 조작 (방향을 메인 음악 플레이어랑 100% 똑같이 맞춤!)
        if (keyCode == 21 || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == 19) {
            adjustVolume(false); // 💡 왼쪽(21)이나 위로 돌리면 소리 줄이기
            return true;
        }
        if (keyCode == 22 || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == 20) {
            adjustVolume(true); // 💡 오른쪽(22)이나 아래로 돌리면 소리 키우기
            return true;
        }

        // 🚀 3. 재생/정지 통합 (가운데 확인 버튼 & 하단 미디어 버튼)
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == 23 ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {

            if (videoView.isPlaying()) {
                videoView.pause();
                ivPauseIcon.setVisibility(View.VISIBLE);
                showControls(true); // 정지 시 재생바 무한 고정!
            } else {
                videoView.start();
                ivPauseIcon.setVisibility(View.GONE);
                showControls(false); // 재생 시작 시 3초 후 스르륵 숨김!
            }
            return true;
        }
        if (keyCode == 88 || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            if (videoView != null) {
                int pos = videoView.getCurrentPosition() - 10000;
                videoView.seekTo(pos < 0 ? 0 : pos);
                showControls(false);
            }
            return true;
        }

        if (keyCode == 87 || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            if (videoView != null) {
                int pos = videoView.getCurrentPosition() + 10000;
                int dur = videoView.getDuration();
                videoView.seekTo(dur > 0 && pos > dur ? dur : pos);
                showControls(false);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // 🚀 자막(SRT) 파서 엔진
    private void loadSubtitles(String videoPath) {
        try {
            String basePath = videoPath.substring(0, videoPath.lastIndexOf('.'));
            File srtFile = new File(basePath + ".srt");
            if (!srtFile.exists())
                return;

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(srtFile), "UTF-8"));
            String line;
            int startTime = 0;
            StringBuilder text = new StringBuilder();

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\d+")) { // 번호표
                    if (text.length() > 0 && startTime > 0) {
                        subtitlesMap.put(startTime, text.toString().trim());
                    }
                    text.setLength(0);
                } else if (line.contains("-->")) { // 타임스탬프
                    String[] parts = line.split("-->");
                    startTime = parseSrtTime(parts[0].trim());
                    int endTime = parseSrtTime(parts[1].trim());
                    subtitlesMap.put(endTime, ""); // 끝나는 시간에 자막 지우기
                } else if (!line.isEmpty()) { // 자막 텍스트
                    text.append(line).append("\n");
                }
            }
            if (text.length() > 0 && startTime > 0) {
                subtitlesMap.put(startTime, text.toString().trim());
            }
            br.close();
        } catch (Exception e) {
        }
    }

    private int parseSrtTime(String timeStr) {
        try {
            String[] parts = timeStr.replace(',', '.').split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String[] sParts = parts[2].split("\\.");
            int s = Integer.parseInt(sParts[0]);
            int ms = sParts.length > 1 ? Integer.parseInt(sParts[1]) : 0;
            return (h * 3600 + m * 60 + s) * 1000 + ms;
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    private void forceFiveBandAudioMode() {
        try {
            if (MainActivity.instance != null) {
                MainActivity.instance.isSoftwareEqEnabled = false;
                MainActivity.instance.prefs.edit().putBoolean("software_eq_enabled", false).commit();
                if (MainActivity.instance.equalizer != null) {
                    MainActivity.instance.equalizer.release();
                }
                int sessionId = MainActivity.instance.currentAudioSessionId;
                if (sessionId != -1) {
                    MainActivity.instance.equalizer = new Equalizer(0, sessionId);
                    MainActivity.instance.equalizer.setEnabled(true);
                }
            }
        } catch (Exception e) {
        }
    }

    private int lastKnownPosition = 0;

    @Override
    protected void onPause() {
        super.onPause();
        com.themoon.y1.managers.BatteryStatsManager.getInstance(this).setMode(com.themoon.y1.managers.BatteryStatsManager.MODE_MUSIC);
        if (videoPath != null && lastKnownPosition > 0) {
            int duration = videoView != null ? videoView.getDuration() : 0;
            int posToSave = lastKnownPosition;
            if (duration > 0 && posToSave >= duration - 5000) {
                posToSave = 0;
            }
            getSharedPreferences("y1_prefs", MODE_PRIVATE).edit()
                    .putInt("video_pos_" + videoPath, posToSave)
                    .apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(updateUITask);
        uiHandler.removeCallbacks(hideUITask);
        volumeHandler.removeCallbacks(hideVolumeTask);
        if (videoView != null) {
            if (videoPath != null && lastKnownPosition > 0) {
                int duration = videoView.getDuration();
                int posToSave = lastKnownPosition;
                if (duration > 0 && posToSave >= duration - 5000) {
                    posToSave = 0;
                }
                getSharedPreferences("y1_prefs", MODE_PRIVATE).edit()
                        .putInt("video_pos_" + videoPath, posToSave)
                        .apply();
            }
            videoView.stopPlayback();
        }
    }
}