import sys

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\VideoPlayerActivity.java'

new_content = """package com.themoon.y1;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
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
import android.media.audiofx.Equalizer;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class VideoPlayerActivity extends Activity {
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private String videoPath;
    
    private LinearLayout layoutControls, layoutVolumeOverlay;
    private TextView tvCurrent, tvTotal, tvSubtitle;
    private ProgressBar progressVideo, volumeProgress;
    private ImageView ivPauseIcon;

    private boolean isSeekPerformed = false;
    private long lastSeekTime = 0;
    private Handler uiHandler = new Handler();
    private boolean isUIHiding = false;

    private Handler volumeHandler = new Handler();
    private Runnable hideVolumeTask = () -> layoutVolumeOverlay.setVisibility(View.GONE);

    private TreeMap<Integer, String> subtitlesMap = new TreeMap<>();
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_player);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        playerView = findViewById(R.id.video_view);
        layoutControls = findViewById(R.id.layout_controls);
        tvCurrent = findViewById(R.id.tv_time_current);
        tvTotal = findViewById(R.id.tv_time_total);
        progressVideo = findViewById(R.id.progress_video);
        ivPauseIcon = findViewById(R.id.iv_pause_icon);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        layoutVolumeOverlay = findViewById(R.id.layout_volume_overlay);
        volumeProgress = findViewById(R.id.volume_progress);

        volumeProgress.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

        try {
            int themeFocusColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            volumeProgress.getProgressDrawable().setColorFilter(themeFocusColor, android.graphics.PorterDuff.Mode.SRC_IN);
        } catch (Exception e) {}

        videoPath = getIntent().getStringExtra("VIDEO_PATH");

        if (videoPath == null || !new File(videoPath).exists()) {
            Toast.makeText(this, "⚠️ Invalid Video File", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        forceFiveBandAudioMode();
        loadSubtitles(videoPath);

        // 🚀 ExoPlayer 셋업 (FFmpeg Extension 우선 사용)
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
        player = new SimpleExoPlayer.Builder(this, renderersFactory).build();
        playerView.setPlayer(player);

        // 🚀 배속 설정 (API 버전 무관하게 소프트웨어 처리 지원)
        try {
            float speed = com.themoon.y1.managers.AudioPlayerManager.getInstance().getCurrentSpeed();
            if (speed != 1.0f) {
                player.setPlaybackParameters(new PlaybackParameters(speed));
            }
        } catch (Exception e) {}

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoPath));
        player.setMediaItem(mediaItem);
        
        // 🚀 이어보기 (Resume Playback) 복원 로직
        SharedPreferences prefs = getSharedPreferences("y1_prefs", MODE_PRIVATE);
        int savedPos = prefs.getInt("video_pos_" + videoPath, 0);
        if (savedPos > 0) {
            player.seekTo(savedPos);
        }

        player.prepare();
        player.play();

        playerView.setOnClickListener(v -> {
            if (isUIHiding) showControls(false);
            else showControls(false);
        });

        uiHandler.postDelayed(updateUITask, 300);
        showControls(false);
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

    private void adjustVolume(boolean up) {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (up && currentVol < maxVol) currentVol++;
        else if (!up && currentVol > 0) currentVol--;

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);

        try {
            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
            if (fm != null && fm.isPowerUp) {
                int streamFm = 10;
                try {
                    streamFm = (Integer) AudioManager.class.getDeclaredField("STREAM_FM").get(null);
                } catch (Exception e) {}
                int fmMax = audioManager.getStreamMaxVolume(streamFm);
                int fmVol = (int) (((float) currentVol / maxVol) * fmMax);
                audioManager.setStreamVolume(streamFm, fmVol, 0);
            }
        } catch (Exception e) {}

        showDynamicVolumeOverlay();
    }

    private void showDynamicVolumeOverlay() {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        layoutVolumeOverlay.setVisibility(View.VISIBLE);
        volumeProgress.setProgress(currentVol);
        volumeHandler.removeCallbacks(hideVolumeTask);
        volumeHandler.postDelayed(hideVolumeTask, 2000);
    }

    private Runnable updateUITask = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                int current = (int) player.getCurrentPosition();
                int total = (int) player.getDuration();

                tvCurrent.setText(formatTime(current));
                tvTotal.setText(formatTime(total));
                if (total > 0) progressVideo.setProgress((int) (((float) current / total) * 100));

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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }

        if (keyCode == 21 || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == 19) {
            adjustVolume(false);
            return true;
        }
        if (keyCode == 22 || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == 20) {
            adjustVolume(true);
            return true;
        }

        // 🚀 건너뛰기 기능 (이전/다음 휠 버튼 매핑)
        if (keyCode == 88 || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            if (player != null) {
                long pos = player.getCurrentPosition() - 10000; // 10초 뒤로
                player.seekTo(pos < 0 ? 0 : pos);
                showControls(false);
            }
            return true;
        }
        
        if (keyCode == 87 || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            if (player != null) {
                long pos = player.getCurrentPosition() + 10000; // 10초 앞으로
                long dur = player.getDuration();
                player.seekTo(dur > 0 && pos > dur ? dur : pos);
                showControls(false);
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == 23 ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {

            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                    ivPauseIcon.setVisibility(View.VISIBLE);
                    showControls(true);
                } else {
                    player.play();
                    ivPauseIcon.setVisibility(View.GONE);
                    showControls(false);
                }
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void loadSubtitles(String videoPath) {
        try {
            String basePath = videoPath.substring(0, videoPath.lastIndexOf('.'));
            File srtFile = new File(basePath + ".srt");
            if (!srtFile.exists()) return;

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(srtFile), "UTF-8"));
            String line;
            int startTime = 0;
            StringBuilder text = new StringBuilder();

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.matches("\\\\d+")) {
                    if (text.length() > 0 && startTime > 0) {
                        subtitlesMap.put(startTime, text.toString().trim());
                    }
                    text.setLength(0);
                } else if (line.contains("-->")) {
                    String[] parts = line.split("-->");
                    startTime = parseSrtTime(parts[0].trim());
                    int endTime = parseSrtTime(parts[1].trim());
                    subtitlesMap.put(endTime, "");
                } else if (!line.isEmpty()) {
                    text.append(line).append("\\n");
                }
            }
            if (text.length() > 0 && startTime > 0) {
                subtitlesMap.put(startTime, text.toString().trim());
            }
            br.close();
        } catch (Exception e) {}
    }

    private int parseSrtTime(String timeStr) {
        try {
            String[] parts = timeStr.replace(',', '.').split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            String[] sParts = parts[2].split("\\\\.");
            int s = Integer.parseInt(sParts[0]);
            int ms = sParts.length > 1 ? Integer.parseInt(sParts[1]) : 0;
            return (h * 3600 + m * 60 + s) * 1000 + ms;
        } catch (Exception e) { return 0; }
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
        } catch (Exception e) {}
    }

    // 🚀 이어보기 (Resume Playback) 저장 로직
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && videoPath != null) {
            getSharedPreferences("y1_prefs", MODE_PRIVATE).edit()
                .putInt("video_pos_" + videoPath, (int) player.getCurrentPosition())
                .apply();
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(updateUITask);
        uiHandler.removeCallbacks(hideUITask);
        volumeHandler.removeCallbacks(hideVolumeTask);
        if (player != null) {
            getSharedPreferences("y1_prefs", MODE_PRIVATE).edit()
                .putInt("video_pos_" + videoPath, (int) player.getCurrentPosition())
                .apply();
            player.release();
            player = null;
        }
    }
}
"""

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)
print('Done!')
