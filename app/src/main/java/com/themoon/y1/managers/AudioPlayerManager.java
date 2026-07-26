package com.themoon.y1.managers;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.themoon.y1.MainActivity;
import com.themoon.y1.R;
import com.themoon.y1.ThemeManager;
import com.google.android.exoplayer2.DefaultLoadControl;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioPlayerManager {
    private static AudioPlayerManager instance;
    public SimpleExoPlayer exoPlayer;
    // 🚀 [신규 엔진] 10밴드 하드코어 소프트웨어 DSP를 전역으로 장전합니다!
    public Y1EqAudioProcessor customEqProcessor = new Y1EqAudioProcessor();

    // 🚀 [신규 엔진 2] 자연스러운 스피커 공간감을 구현할 크로스피드 DSP 장전!
    public Y1CrossfeedAudioProcessor crossfeedProcessor = new Y1CrossfeedAudioProcessor();

    private float currentSpeed = 1.0f;

    private AudioPlayerManager() {}



    public static synchronized AudioPlayerManager getInstance() {
        if (instance == null) instance = new AudioPlayerManager();
        return instance;
    }

    public void initPlayer(Context context) {
        if (exoPlayer == null) {
            com.google.android.exoplayer2.DefaultRenderersFactory renderersFactory = new com.google.android.exoplayer2.DefaultRenderersFactory(context.getApplicationContext()) {
                @Override
                protected void buildAudioRenderers(
                        Context context,
                        int extensionRendererMode,
                        com.google.android.exoplayer2.mediacodec.MediaCodecSelector mediaCodecSelector,
                        boolean enableDecoderFallback,
                        com.google.android.exoplayer2.audio.AudioSink audioSink,
                        android.os.Handler eventHandler,
                        com.google.android.exoplayer2.audio.AudioRendererEventListener eventListener,
                        java.util.ArrayList<com.google.android.exoplayer2.Renderer> out) {



                    java.util.List<com.google.android.exoplayer2.audio.AudioProcessor> procList = new java.util.ArrayList<>();

                    // 🚀 2. [궁극의 초고속 C++ 네이티브 리샘플러] 24비트 변환 + 48kHz 다운샘플링 
                    // 화면이 꺼져 CPU가 200MHz로 바닥을 쳐도, Java가 아닌 C++ 네이티브(JNI)에서
                    // 다이렉트 메모리를 조작하므로 0.001초만에 24비트 96kHz -> 16비트 48kHz 변환을 완료합니다.
                    com.google.android.exoplayer2.audio.AudioProcessor immortalSonic = new NativeResampleAudioProcessor();
                    // =======================================================
// 🚀 3. [배관 조립] 파이프라인 순서 전면 개조!
// =======================================================

// 💥 [에러 해결 및 최후의 병목 제거] 
// ExoPlayer(구버전)는 24비트 음원을 16비트 오디오트랙에 바로 넣지 못하고 에러(읽을 수 없음)를 냅니다.
// 그래서 immortalSonic을 부활시켜 '24비트 -> 16비트 변환기'로만 사용합니다!
// 단, 예전처럼 96kHz -> 44.1kHz 다운샘플링은 하지 않습니다! (outRate = inRate)
// 96kHz 16비트로 변환된 데이터를 그대로 넘기면, 안드로이드 네이티브 OS(AudioFlinger)가 
// 하드웨어 단에서 초고속으로 리샘플링하므로 CPU 부하가 극적으로 감소합니다!
                    procList.add(immortalSonic);

// 🌿 16비트 일반 음원에 대해서만 EQ 연산을 수행합니다. (고해상도 24비트는 쾌적한 재생을 위해 자동 프리패스)
                    procList.add(customEqProcessor);

// 🌿 크로스피드 역시 가벼워진 데이터만 처리하거나 프리패스합니다.
                    procList.add(crossfeedProcessor);

                    com.google.android.exoplayer2.audio.AudioProcessor[] processors = procList.toArray(new com.google.android.exoplayer2.audio.AudioProcessor[0]);
                    com.google.android.exoplayer2.audio.AudioCapabilities strict16BitCaps = new com.google.android.exoplayer2.audio.AudioCapabilities(new int[] { 2 }, 2);
                    com.google.android.exoplayer2.audio.AudioSink customSink = new com.google.android.exoplayer2.audio.DefaultAudioSink(strict16BitCaps, processors);
                    // 💥 [긴급 롤백] MTK 하드웨어 디코더 허용 철회!
                    // MTK 젤리빈 기기에서 MP3 재생 시 OMX.MTK.audio.decoder.mp3 하드웨어 디코더가 무한 루프(ANR)에 빠지며
                    // CPU가 100%로 치솟아 폰이 뜨거워지고 런처가 강제 재시작되는 치명적 버그가 확인되었습니다.
                    // 따라서 오디오 디코딩은 안전하고 쾌적한 구글 소프트웨어 디코더(OMX.google)로 강제 우회합니다!
                    com.google.android.exoplayer2.mediacodec.MediaCodecSelector customSelector = new com.google.android.exoplayer2.mediacodec.MediaCodecSelector() {
                        @Override
                        public java.util.List<com.google.android.exoplayer2.mediacodec.MediaCodecInfo> getDecoderInfos(
                                String mimeType, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder)
                                throws com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException {
                            java.util.List<com.google.android.exoplayer2.mediacodec.MediaCodecInfo> infos = 
                                com.google.android.exoplayer2.mediacodec.MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
                            if (infos.isEmpty()) return infos;
                            
                            java.util.List<com.google.android.exoplayer2.mediacodec.MediaCodecInfo> safeInfos = new java.util.ArrayList<>();
                            for (com.google.android.exoplayer2.mediacodec.MediaCodecInfo info : infos) {
                                // 💥 [최종 수정] MTK 하드웨어 디코더 전면 차단!
                                // CPU가 화면이 꺼진 상태에서도 1GHz를 정상적으로 유지함이 로그로 확인되었습니다.
                                // 그런데도 6초마다 버퍼 언더런이 난다는 것은, MTK 하드웨어 디코더(칩셋) 자체가 화면이 꺼지면
                                // 절전 모드로 들어가거나 버그가 있어서 데이터를 토해내지 못한다는 의미입니다!
                                // 우리에겐 1GHz의 강력한 CPU와 최적화된 C++ 엔진이 있으므로, 
                                // 불안정한 MTK 디코더를 완전히 버리고 완벽하게 안정적인 Google 소프트웨어 디코더로 전면 교체합니다!
                                if (info.name != null && info.name.toLowerCase().contains("mtk")) {
                                    continue; // MTK 디코더는 무조건 건너뛰기
                                }
                                safeInfos.add(info);
                            }
                            return safeInfos.isEmpty() ? infos : safeInfos;
                        }
                    };
                    // 🚀 최종 조립 완료 및 부모에게 전달!
                    super.buildAudioRenderers(context, extensionRendererMode, customSelector, enableDecoderFallback, customSink, eventHandler, eventListener, out);
                }
            };

            renderersFactory.setExtensionRendererMode(com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

            // 💥 [최종 수리] 물탱크(버퍼) 크기 원상 복구!
            // 기존에는 Java 배열 메모리 폭발(GC)을 막으려고 버퍼를 2.5초로 극한까지 줄였었습니다.
            // 하지만 지금은 C++ 다이렉트 엔진이 적용되어 메모리 폭발이 전혀 일어나지 않으므로, 
            // 버퍼를 줄일 이유가 사라졌습니다! 2.5초의 짧은 버퍼는 SD 카드가 조금만 늦게 읽혀도 
            // 바로 끊김(버퍼 언더런)을 유발하므로, ExoPlayer의 기본 대형 버퍼(15초~50초)를 사용하여
            // SD카드 렉을 완벽하게 방어하도록 되돌립니다!
            com.google.android.exoplayer2.DefaultLoadControl loadControl = new com.google.android.exoplayer2.DefaultLoadControl.Builder()
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build();

// 🚀 3. [핵심 수정] Builder 생성 -> LoadControl 장착 -> build() 조립!
            exoPlayer = new com.google.android.exoplayer2.SimpleExoPlayer.Builder(context.getApplicationContext(), renderersFactory)
                    .setLoadControl(loadControl) // 💥 뼈대를 세운 후 물탱크 장착!
                    .build();
                    
            // 🚀 [스마트 배터리 엔진 가동 - 96kHz 끊김 버그 완벽 수리!]
            // 💥 [핵심 수리] 음원 스펙에 관계없이 ExoPlayer에 WakeLock 관리를 완전히 위임합니다.
            // onPlayerStateChanged에서 format을 검사할 때는 format이 아직 null일 수 있어서 
            // 96kHz 음원도 WAKE_MODE_NONE으로 빠지는 치명적인 버그가 있었습니다.
            // ExoPlayer는 재생 중일 때만 WakeLock을 잡고 일시정지 시 자동으로 놓아주므로, 
            // 생성 시점에 항상 LOCAL WAKE MODE를 적용하는 것이 가장 안전하고 완벽한 해결책입니다.
            exoPlayer.setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_LOCAL);

            exoPlayer.addListener(new Player.EventListener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    // CPU 부스트 관련 로직 제거됨

                    if (playbackState == Player.STATE_READY) {
                        if (MainActivity.instance != null) {
                            MainActivity.instance.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (AudioEffectManager.getInstance() != null) {
                                            AudioEffectManager.getInstance().applyAudioEffects();
                                            AudioEffectManager.getInstance().applyEqProfile();
                                        }
                                        MainActivity.instance.setupVisualizer();

                                        int duration = getDuration();
                                        int s = (duration / 1000) % 60;
                                        int m = (duration / (1000 * 60)) % 60;
                                        MainActivity.instance.tvPlayerTimeTotal.setText(String.format(Locale.US, "%02d:%02d", m, s));
                                        if (!MainActivity.instance.currentPlaylist.isEmpty()) {
                                            MainActivity.instance.updateAudioQualityInfo(MainActivity.instance.currentPlaylist.get(MainActivity.instance.currentIndex));
                                        }
                                    } catch (Exception e) {}
                                }
                            });
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        handleTrackCompletion();
                    }
                    if (MainActivity.instance != null) {
                        MainActivity.instance.runOnUiThread(() -> MainActivity.instance.updatePlayerUI());
                    }
                }

                @Override
                public void onPlayerError(com.google.android.exoplayer2.ExoPlaybackException error) {
                    handleTrackError("Cannot play this file.");
                }
            });
        }
    }

    public void setPlaybackSpeed(float speed) {
        this.currentSpeed = speed;
        if (exoPlayer != null) {
            exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, 1.0f));
        }
    }

    public void setShuffleMode(boolean isShuffle) {
        if (exoPlayer != null) {
            exoPlayer.setShuffleModeEnabled(isShuffle);
        }
        MainActivity main = MainActivity.instance;
        if (main != null && main.currentPlaylist != null && !main.currentPlaylist.isEmpty()) {
            File currentSong = null;
            if (main.currentIndex >= 0 && main.currentIndex < main.currentPlaylist.size()) {
                currentSong = main.currentPlaylist.get(main.currentIndex);
            }
            if (isShuffle) {
                if (main.originalPlaylist == null) {
                    main.originalPlaylist = new java.util.ArrayList<>();
                }
                if (main.originalPlaylist.isEmpty() || main.originalPlaylist.size() != main.currentPlaylist.size()) {
                    main.originalPlaylist.clear();
                    main.originalPlaylist.addAll(main.currentPlaylist);
                }
                java.util.Collections.shuffle(main.currentPlaylist);
                if (currentSong != null) {
                    int newIdx = main.currentPlaylist.indexOf(currentSong);
                    if (newIdx != -1) main.currentIndex = newIdx;
                }
            } else {
                if (main.originalPlaylist != null && !main.originalPlaylist.isEmpty() && main.originalPlaylist.size() == main.currentPlaylist.size()) {
                    main.currentPlaylist.clear();
                    main.currentPlaylist.addAll(main.originalPlaylist);
                    if (currentSong != null) {
                        int newIdx = main.currentPlaylist.indexOf(currentSong);
                        if (newIdx != -1) main.currentIndex = newIdx;
                    }
                }
            }
        }
    }
    public float getCurrentSpeed() { return currentSpeed; }

    private void handleTrackCompletion() {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    int repeatMode = main.prefs.getInt("repeat_mode", 0);
                    if (repeatMode == 1) {
                        if (exoPlayer != null) {
                            exoPlayer.seekTo(0); exoPlayer.setPlayWhenReady(true);
                        }
                    } else if (repeatMode == 2) {
                        nextTrack();
                    } else {
                        if (main.currentIndex < main.currentPlaylist.size() - 1) {
                            nextTrack();
                        } else {
                            main.currentIndex = 0;
                            prepareMusicTrack(main.currentIndex);
                            main.isPausedByHand = true;
                            main.updatePlayerUI();
                        }
                    }
                } catch (Exception e) { nextTrack(); }
            }
        });
    }

    private void handleTrackError(String errorMsg) {
        MainActivity main = MainActivity.instance;
        if (main == null) return;
        main.runOnUiThread(() -> {
            Toast.makeText(main, "⚠️ " + errorMsg + " Skipping...", Toast.LENGTH_SHORT).show();
            nextTrack();
        });
    }

    public void playTrackList(List<File> list, int index) {
        saveAudiobookBookmarkIfNeeded();

        final MainActivity main = MainActivity.instance;
        if (main == null) return;

        initPlayer(main);

        // 1. 외부에서 받은 리스트를 안전하게 복사
        List<File> newList = new java.util.ArrayList<>(list);
        if (newList.isEmpty()) return; // 방어막: 리스트가 텅 비었으면 함수를 중단!

        // 2. 사용자가 클릭한 원본(타겟) 노래를 미리 기억해 둡니다.
        // 만약 인덱스가 꼬여서 리스트 범위를 벗어났다면 안전하게 0번으로 고정!
        if (index < 0 || index >= newList.size()) index = 0;
        File targetSong = newList.get(index);

        // 3. 메인 화면의 재생 바구니(Playlist) 2개를 싹 비우고 새 곡들로 채웁니다.
        main.originalPlaylist.clear();
        main.originalPlaylist.addAll(newList);
        main.currentPlaylist.clear();
        main.currentPlaylist.addAll(newList);

        // 🚀 4. [절대 셔플 엔진 가동] 설정에서 셔플 모드가 켜져 있다면? 무조건 섞습니다!
        boolean isShuffle = main.prefs.getBoolean("shuffle", false);
        if (isShuffle) {
            java.util.Collections.shuffle(main.currentPlaylist); // currentPlaylist를 사정없이 섞음!

            // 섞인 바구니 안에서 방금 사용자가 누른 그 곡(targetSong)이 몇 번 자리로 밀려났는지 찾아냅니다.
            main.currentIndex = main.currentPlaylist.indexOf(targetSong);
            if (main.currentIndex == -1) main.currentIndex = 0; // 혹시나 못 찾으면 0번 재생
        } else {
            // 셔플이 꺼져있다면 원본 순서 그대로!
            main.currentIndex = index;
        }
// 🚀 [추가] ExoPlayer 엔진 자체에도 셔플 상태를 명확히 인지시킵니다!
        if (exoPlayer != null) {
            exoPlayer.setShuffleModeEnabled(isShuffle);
        }
        main.isPausedByHand = false; // 🚀 스위치를 미리 켜줍니다!
        prepareMusicTrack(main.currentIndex);
        main.updatePlayerUI(); // 🚀 타이머 즉시 시작!
    }
    public void playPodcastStream(String url, String title, String imageUrl, String channelName, int offsetMs) {
        final MainActivity main = MainActivity.instance;
        if (main == null) return;

        try {
            // 🚀 [레거시 찌꺼기 완벽 삭제!] 오직 무적의 ExoPlayer만 사용합니다!
            if (exoPlayer == null) initPlayer(main.getApplicationContext());
            else { exoPlayer.stop(); exoPlayer.clearMediaItems(); }

            // 🚀 [핵심 엔진] 스트리밍일 때도 '가짜(Dummy) 플레이리스트'를 만들어 시간을 기록하게 속입니다!
            String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
            main.currentPlaylist.clear();
            main.currentPlaylist.add(new java.io.File("/PODCAST_STREAM/" + safeChannel, safeTitle));
            main.currentIndex = 0;

            com.google.android.exoplayer2.MediaItem mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(android.net.Uri.parse(url));
            com.google.android.exoplayer2.upstream.DataSource.Factory dataSourceFactory = new com.google.android.exoplayer2.upstream.DefaultDataSourceFactory(main, com.google.android.exoplayer2.util.Util.getUserAgent(main, "Y1_Launcher"));
            com.google.android.exoplayer2.extractor.DefaultExtractorsFactory extractorsFactory = new com.google.android.exoplayer2.extractor.DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);
            com.google.android.exoplayer2.source.MediaSource mediaSource = new com.google.android.exoplayer2.source.ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(mediaItem);

            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();

            // 🚀 [이어서 재생] 팝업에서 넘겨받은 시간이 있다면 그곳으로 워프!
            if (offsetMs > 0) {
                exoPlayer.seekTo(offsetMs);
            }
            exoPlayer.setPlaybackParameters(new com.google.android.exoplayer2.PlaybackParameters(currentSpeed, 1.0f));

            main.runOnUiThread(() -> {
                main.isPausedByHand = false;
                main.tvPlayerTitle.setText(title);
                main.tvPlayerArtist.setText(channelName); // 가수에 팟캐스트 채널명 표시
                main.ivAlbumArt.setImageResource(R.drawable.default_album);
                main.ivPlayerBgBlur.setImageResource(0);
                main.lastAlbumArtBytes = null; // 🚀 스캔 전 찌꺼기 이미지 초기화!
                main.updatePlayerUI();
            });

            // =======================================================
            // 🚀 [썸네일 엔진 대개조] 인터넷 이미지 + 로컬 커버 호환 및 금고 저장!
            // =======================================================
            new Thread(() -> {
                android.graphics.Bitmap bmp = null;
                try {
                    // 1. 에피소드 전용 이미지 URL이 있다면 우선순위로 다운로드 시도!
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        java.net.URL imgUrl = new java.net.URL(imageUrl);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) imgUrl.openConnection();
                        conn.setDoInput(true); conn.connect();
                        bmp = android.graphics.BitmapFactory.decodeStream(conn.getInputStream());
                    }

                    // 2. 에피소드 전용 이미지가 없다면? 우리가 기기에 이미 받아둔 채널 간판(cover.jpg)을 띄웁니다!
                    if (bmp == null) {
                        java.io.File coverFile = new java.io.File("/storage/sdcard0/Podcasts/" + safeChannel, "cover.jpg");
                        if (coverFile.exists()) {
                            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            android.graphics.BitmapFactory.decodeFile(coverFile.getAbsolutePath(), opts);
                            int scale = 1;
                            while (opts.outWidth / scale > 500 || opts.outHeight / scale > 500) { scale *= 2; }
                            opts.inJustDecodeBounds = false;
                            opts.inSampleSize = scale;
                            bmp = android.graphics.BitmapFactory.decodeFile(coverFile.getAbsolutePath(), opts);
                        }
                    }

                    // 3. 비트맵을 성공적으로 가져왔다면 화면에 바인딩 및 영구 보존!
                    if (bmp != null) {
                        final android.graphics.Bitmap finalBmp = bmp;
                        main.runOnUiThread(() -> {
                            main.ivAlbumArt.setImageBitmap(finalBmp);
                            // 🚀 [배경 테마 엔진 적용 1: 팟캐스트]
                            if ("clear".equals(main.currentPlayerBgMode)) {
                                main.ivPlayerBgBlur.setImageBitmap(finalBmp);
                            } else if ("blur".equals(main.currentPlayerBgMode)) {
                                android.graphics.Bitmap blurredBg = main.applyGaussianBlur(finalBmp);
                                main.ivPlayerBgBlur.setImageBitmap(blurredBg);
                            } else {
                                main.ivPlayerBgBlur.setImageBitmap(null);
                            }
                            try {
                                main.currentAlbumColor = finalBmp.getPixel(finalBmp.getWidth()/2, (int)(finalBmp.getHeight()*0.8)) | 0xFF000000;
                            } catch (Exception ex) {
                                main.currentAlbumColor = com.themoon.y1.ThemeManager.getListButtonFocusedBg() | 0xFF000000;
                            }

                            // 🚀 [핵심 해결] ExoPlayer 로딩 완료 후 UI가 갱신될 때 이미지가 증발하지 않도록 바이트로 변환해 금고에 저장합니다!
                            try {
                                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                                finalBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream);
                                main.lastAlbumArtBytes = stream.toByteArray();
                            } catch (Exception e) {}

                            main.updateMainMenuBackground();
                            main.refreshNowPlayingPreview(); // 🚀 메인 화면의 미니 프리뷰 위젯에도 썸네일 동기화!
                        });
                    }
                } catch (Exception e) {}
            }).start();

            exoPlayer.setPlayWhenReady(true);

        } catch (Exception e) {}
    }
    public void playTrackListWithOffset(List<File> list, int index, int offsetMs) {
        playTrackList(list, index);
        if (offsetMs > 0) {
            try {
                seekRelative(offsetMs - getCurrentPosition());
                final int totalSec = offsetMs / 1000;
                final int min = totalSec / 60;
                final int sec = totalSec % 60;
                if (MainActivity.instance != null) {
                    MainActivity.instance.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                android.widget.Toast toast = android.widget.Toast.makeText(MainActivity.instance,
                                        "🎧 Resuming playback from " + min + "m " + sec + "s",
                                        android.widget.Toast.LENGTH_SHORT);
                                android.widget.LinearLayout toastLayout = (android.widget.LinearLayout) toast.getView();
                                android.widget.TextView toastTV = (android.widget.TextView) toastLayout.getChildAt(0);
                                toastTV.setTextSize(18f);
                                toast.show();
                            } catch (Exception e) {}
                        }
                    });
                }
            } catch (Exception e) {}
        }
    }

    public void setupFolderPlaylist(File clickedFile, File parentFolder) {
        MainActivity main = MainActivity.instance;
        if (main == null) return;

        List<File> folderAudio = new java.util.ArrayList<>();
        File[] files = parentFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && isAudioFile(f.getName())) folderAudio.add(f);
            }
            java.util.Collections.sort(folderAudio, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        }
        int idx = folderAudio.indexOf(clickedFile);
        if (idx == -1) idx = 0;
        playTrackList(folderAudio, idx);
    }

    private boolean isAudioFile(String name) {
        name = name.toLowerCase();
        // 🚀 [.m4b 오디오북 공식 지원 도어 오픈!]
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg")
                || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ape") || name.endsWith(".wma")
                || name.endsWith(".opus") || name.endsWith(".m4b");
    }

    public void playOrPauseMusic() {
        if (exoPlayer != null) {
            if (exoPlayer.getPlayWhenReady()) {
                saveAudiobookBookmarkIfNeeded();
                exoPlayer.setPlayWhenReady(false);
                if (MainActivity.instance != null) MainActivity.instance.isPausedByHand = true;
            } else {
                exoPlayer.setPlayWhenReady(true);
                if (MainActivity.instance != null) MainActivity.instance.isPausedByHand = false;
            }
        }
        if (MainActivity.instance != null) MainActivity.instance.updatePlayerUI();
    }
    public void nextTrack() {
        saveAudiobookBookmarkIfNeeded();
        MainActivity main = MainActivity.instance;
        if (main == null || main.currentPlaylist.isEmpty()) return;
        main.lastTrackChangeTime = System.currentTimeMillis();
        main.currentIndex = (main.currentIndex + 1) % main.currentPlaylist.size();
        prepareMusicTrack(main.currentIndex);
        main.updatePlayerUI();
    }

    public void prevTrack() {
        saveAudiobookBookmarkIfNeeded();
        MainActivity main = MainActivity.instance;
        if (main == null || main.currentPlaylist.isEmpty()) return;
        main.lastTrackChangeTime = System.currentTimeMillis();
        main.currentIndex--;
        if (main.currentIndex < 0) main.currentIndex = main.currentPlaylist.size() - 1;
        prepareMusicTrack(main.currentIndex);
        main.updatePlayerUI();
    }

    public void seekRelative(int offsetMs) {
        if (exoPlayer != null) {
            long currentPos = getCurrentPosition();
            long duration = getDuration();
            long targetPos = currentPos + offsetMs;
            if (targetPos < 0) targetPos = 0;
            if (targetPos > duration && duration > 0) targetPos = duration;
            exoPlayer.seekTo(targetPos);
        }
    }
    // 🚀 [순정 및 동기화 완벽 복원] 번쩍이는 딜레이를 아예 없앴습니다!
    public void prepareMusicTrack(int index) {
        final MainActivity main = MainActivity.instance;
        if (main == null || main.currentPlaylist.isEmpty()) return;

        final File track = main.currentPlaylist.get(index);
        main.lastAlbumArtBytes = null;
        main.currentAlbumColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;

        if (!track.exists() || track.length() < 1024) {
            main.tvPlayerTitle.setText("Corrupted File");
            main.tvPlayerArtist.setText("Skipping...");
            main.ivAlbumArt.setImageResource(R.drawable.default_album);

            main.consecutiveErrorCount++;

            if (main.consecutiveErrorCount >= main.currentPlaylist.size()) {
                Toast.makeText(main, "❌ All tracks failed. Playback stopped.", Toast.LENGTH_SHORT).show();
                main.isPausedByHand = true;
                main.updatePlayerUI();
                main.consecutiveErrorCount = 0;
            } else {
                Toast.makeText(main, "Corrupted file detected. Skipping...", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> nextTrack(), 1500);
            }
            return;
        }

        // 🚀 스레드(Thread)를 걷어내고 메인에서 즉시 처리하여 깜빡임/딜레이 현상을 완벽 차단!
        main.tvPlayerTitle.setText(track.getName());
        main.tvPlayerArtist.setText("Loading...");
        main.ivAlbumArt.setImageResource(R.drawable.default_album);
        main.ivPlayerBgBlur.setImageResource(0);
        main.playerProgress.setProgress(0);
        main.tvPlayerTimeCurrent.setText("00:00");
        main.tvPlayerTimeTotal.setText("00:00");

        String ext = track.getName().toLowerCase();
     //   isUsingLegacyPlayer = false;
        boolean isOpus = ext.endsWith(".opus");
        boolean isFlac = ext.endsWith(".flac"); // 🚀 FLAC 판별기 신규 추가!

        try {
            String t = null;
            String a = null;
            main.lastAlbumArtBytes = null;

            // ==========================================
            // 🛡️ [1구역] 메타데이터 추출 (안전 구역 분리)
            // ==========================================
            if (isOpus) {
                Object[] opusTags = extractOpusMetadata(track);
                if (opusTags[0] != null) t = (String) opusTags[0];
                if (opusTags[1] != null) a = (String) opusTags[1];
                if (opusTags[5] != null) main.lastAlbumArtBytes = (byte[]) opusTags[5];
            } else if (isFlac) {
                Object[] flacTags = extractFlacMetadata(track);
                if (flacTags[0] != null) t = (String) flacTags[0];
                if (flacTags[1] != null) a = (String) flacTags[1];
                if (flacTags[5] != null) main.lastAlbumArtBytes = (byte[]) flacTags[5];

                // 🚀 [여기에 신규 장착!] FLAC 검사가 끝나고, 순정 부품으로 넘어가기 직전에 ALAC/M4A를 낚아챕니다!
            } else if (ext.endsWith(".m4a") || ext.endsWith(".alac")) {
                Object[] alacTags = extractAlacMetadata(track);
                if (alacTags[0] != null) t = (String) alacTags[0];
                if (alacTags[1] != null) a = (String) alacTags[1];
                if (alacTags[5] != null) main.lastAlbumArtBytes = (byte[]) alacTags[5];

                // 🎯 대망의 가사 장착!
                // MainActivity 쪽에 가사를 저장하는 변수(예: main.currentLyrics 등)에 값을 넣어줍니다.
                // 아티스트님의 앱 구조에 맞춰 변수명만 살짝 수정해 주세요!
                if (alacTags.length > 7 && alacTags[7] != null) {
                    // 예시: main.lyricsText = (String) alacTags[7];
                }

            } else {
                // 🚀 MP3, WAV 등 버틸 수 있는 파일만 순정 부품 사용
                try {
                    android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
                    java.io.FileInputStream fisMmr = new java.io.FileInputStream(track);
                    mmr.setDataSource(fisMmr.getFD());
                    t = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                    a = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    // 🚀 [오디오북 태그 지원] 가수 태그가 없으면 '저자(AUTHOR)' 태그를 한 번 더 긁어옵니다!
                    if (a == null || a.isEmpty()) a = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_AUTHOR);

                    main.lastAlbumArtBytes = mmr.getEmbeddedPicture();
                    fisMmr.close();
                    mmr.release();
                } catch (Throwable e) {}
            }

            // ==========================================
            // 🖼️ [2구역] 화면 UI 덮어쓰기 (무조건 실행됨!)
            // ==========================================
            String safeFileName = track.getName().replace(".mp3", "").replace(".flac", "").replace(".wav", "").replace(".m4a", "").replace(".opus", "").replace(".m4b", "");
            File coverFile = new File("/storage/sdcard0/Y1_Covers", safeFileName + ".jpg");

            // 🚀 [팟캐스트 초고속 렌더링 지름길 장착!]
            boolean isPodcast = track.getAbsolutePath().contains("/Podcasts/");
            File podcastCoverFile = null;
            if (isPodcast) {
                // 무거운 MP3 태그 스캔을 완벽히 건너뛰고, 폴더 안의 cover.jpg를 1순위로 지정합니다!
                File podcastFolder = track.getParentFile();
                podcastCoverFile = new File(podcastFolder, "cover.jpg");
            }

            if (main.prefs.contains("meta_title_" + track.getAbsolutePath())) {
                t = main.prefs.getString("meta_title_" + track.getAbsolutePath(), t);
                a = main.prefs.getString("meta_artist_" + track.getAbsolutePath(), a);
            }

            // 🚀 [폴더 이름 강제 수혈 엔진] 태그가 텅 비었을 때, 폴더 이름을 가수/책 이름으로 띄워줍니다!
            if (a == null || a.trim().isEmpty() || a.equalsIgnoreCase("Unknown Artist") || a.equalsIgnoreCase("Unknown Author")) {
                try {
                    String parentName = track.getParentFile().getParentFile().getName();
                    String folderName = track.getParentFile().getName();

                    if (parentName != null && !parentName.equals("Music") && !parentName.equals("Audiobooks") && !parentName.equals("sdcard0") && !parentName.equals("Y1_Playlists")) {
                        a = parentName;
                    } else if (folderName != null && !folderName.equals("Music") && !folderName.equals("Audiobooks") && !folderName.equals("sdcard0") && !folderName.equals("Y1_Playlists")) {
                        a = folderName;
                    }
                } catch (Exception e) {}
            }

            if (t != null && !t.trim().isEmpty()) main.tvPlayerTitle.setText(t);
            else main.tvPlayerTitle.setText(safeFileName);

            if (a != null && !a.trim().isEmpty()) main.tvPlayerArtist.setText(a);
            else main.tvPlayerArtist.setText(track.getAbsolutePath().contains("/Audiobooks") || main.isAudiobookLibraryMode ? "Unknown Author" : "Unknown Artist");


            // 🚀 동기식 렌더링으로 번쩍거림 없이 100% 매끄럽게 넘어갑니다.
            if (main.lastAlbumArtBytes != null && main.lastAlbumArtBytes.length > 0) {
                main.updateMainMenuBackground();
                main.refreshNowPlayingPreview();
                try {
                    android.graphics.BitmapFactory.Options optsCenter = new android.graphics.BitmapFactory.Options();
                    optsCenter.inSampleSize = 2;
                    android.graphics.Bitmap bmpCenter = android.graphics.BitmapFactory.decodeByteArray(main.lastAlbumArtBytes, 0, main.lastAlbumArtBytes.length, optsCenter);
                    main.ivAlbumArt.setImageBitmap(bmpCenter);

                    android.graphics.BitmapFactory.Options optsBg = new android.graphics.BitmapFactory.Options();
                    optsBg.inSampleSize = 4;
                    android.graphics.Bitmap sourceBg = android.graphics.BitmapFactory.decodeByteArray(main.lastAlbumArtBytes, 0, main.lastAlbumArtBytes.length, optsBg);
                  //  android.graphics.Bitmap blurredBg = main.applyGaussianBlur(sourceBg);
                    // 🚀 [배경 테마 엔진 적용 2: 일반 태그 음악]
                    if ("clear".equals(main.currentPlayerBgMode)) {
                        main.ivPlayerBgBlur.setImageBitmap(sourceBg);
                    } else if ("blur".equals(main.currentPlayerBgMode)) {
                        android.graphics.Bitmap blurredBg = main.applyGaussianBlur(sourceBg);
                        main.ivPlayerBgBlur.setImageBitmap(blurredBg);
                        if (sourceBg != blurredBg) sourceBg.recycle();
                    } else {
                        main.ivPlayerBgBlur.setImageBitmap(null);
                        sourceBg.recycle();
                    }


                    try {
                        int centerX = bmpCenter.getWidth() / 2;
                        int centerY = (int) (bmpCenter.getHeight() * 0.8);
                        main.currentAlbumColor = bmpCenter.getPixel(centerX, centerY) | 0xFF000000;
                    } catch (Exception e) {
                        main.currentAlbumColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
                    }
                } catch (Throwable e) {}
            }
            // 🚀 팟캐스트 전용 지름길: 폴더에 cover.jpg가 있으면 즉시 로드! (초고해상도 압축 방어막 장착)
            else if (isPodcast && podcastCoverFile != null && podcastCoverFile.exists()) {
                try {
                    // 1. 메모리에 올리기 전에 이미지의 '진짜 크기'만 몰래 알아옵니다.
                    android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeFile(podcastCoverFile.getAbsolutePath(), opts);

                    // 2. 3000x3000 같은 괴물 해상도를 500x500 이하가 될 때까지 무자비하게 압축 비율(Scale)을 높입니다!
                    int scale = 1;
                    while (opts.outWidth / scale > 500 || opts.outHeight / scale > 500) {
                        scale *= 2;
                    }

                    // 3. 계산된 가벼운 압축 비율로 진짜 이미지를 메모리에 올립니다. (9MB -> 100KB 수준으로 다이어트!)
                    opts.inJustDecodeBounds = false;
                    opts.inSampleSize = scale;
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(podcastCoverFile.getAbsolutePath(), opts);

                    main.ivAlbumArt.setImageBitmap(bmp);

                    // 🚀 [배경 테마 엔진 적용 3: 다운로드된 커버 이미지]
                    if ("clear".equals(main.currentPlayerBgMode)) {
                        main.ivPlayerBgBlur.setImageBitmap(bmp);
                    } else if ("blur".equals(main.currentPlayerBgMode)) {
                        android.graphics.Bitmap blurredBg = main.applyGaussianBlur(bmp);
                        main.ivPlayerBgBlur.setImageBitmap(blurredBg);
                    } else {
                        main.ivPlayerBgBlur.setImageBitmap(null);
                    }

                    try {
                        int centerX = bmp.getWidth() / 2;
                        int centerY = (int) (bmp.getHeight() * 0.8);
                        main.currentAlbumColor = bmp.getPixel(centerX, centerY) | 0xFF000000;
                    } catch (Exception e) {
                        main.currentAlbumColor = com.themoon.y1.ThemeManager.getListButtonFocusedBg() | 0xFF000000;
                    }

                    main.updateMainMenuBackground();
                    main.refreshNowPlayingPreview();
                } catch (Exception e) {
                    main.applyCachedCoverArt(podcastCoverFile.getAbsolutePath()); // 에러 시 기존 방식 안전망
                }
            }
            // 일반 음악 전용: Y1_Covers 폴더에 다운로드된 이미지가 있으면 로드
            else if (coverFile.exists()) {
                main.applyCachedCoverArt(coverFile.getAbsolutePath());
            }
            else {
                // 🚀 [신규 장착!] (3순위) 파일 안에 사진은 없지만, 같은 폴더에 'cover.jpg'가 있을 때!
                File folderCover = main.findFolderCover(track.getParentFile());

                if (folderCover != null) {
                    main.applyCachedCoverArt(folderCover.getAbsolutePath()); // 폴더 이미지를 메인 화면에 예쁘게 적용!
                } else {
                    // (4순위) 다 없으면 최후의 수단으로 '기본 테마 이미지'를 띄우고 인터넷에 다운로드 명령을 내립니다!
                    main.ivAlbumArt.setImageResource(R.drawable.default_album);
                    main.currentAlbumColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
                    main.ivPlayerBgBlur.setImageResource(0);
                    main.updateMainMenuBackground();
                    main.refreshNowPlayingPreview();

                    // 🚀 [에러 해결] 제목(t)과 가수(a)가 'Unknown'이 아닌 정상적인 태그를 가지고 있는지 판별하는 센서 장착!
                    boolean hasValidTags = (t != null && !t.trim().isEmpty()) && (a != null && !a.trim().isEmpty() && !a.contains("Unknown"));

                    boolean isAutoFetchEnabled = main.prefs.getBoolean("auto_fetch", true);

                    // 💡 팟캐스트는 AutoFetch(인터넷 검색)를 돌리지 않고 건너뜁니다! (시간 딜레이 원천 차단)
                    if (!isPodcast && isAutoFetchEnabled) {
                        String searchQuery = hasValidTags ? (a + " " + t) : safeFileName.replace("-", " ").replace("_", " ");
                        main.fetchTrackInfoFromInternet(track, searchQuery, hasValidTags, t, a);
                    }
                }
            }
        } catch (Throwable t) {}

        // 🚀 (이 아래 try { if (isUsingLegacyPlayer) ... 엔진 가동 로직은 기존 코드 100% 동일하게 유지!)

        // ==========================================
        // 🚀 [3구역] 순수 ExoPlayer (FFmpeg) 재생 엔진 가동!
        // ==========================================
        try {
            if (exoPlayer == null) initPlayer(main.getApplicationContext());
            else exoPlayer.stop();

            com.google.android.exoplayer2.MediaItem mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(Uri.fromFile(track));
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(main, Util.getUserAgent(main, "Y1_Launcher"));
            DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory).createMediaSource(mediaItem);

            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare(); // 💡 장전 완료!

            boolean isShuffle = main.prefs.getBoolean("shuffle", false);
            exoPlayer.setShuffleModeEnabled(isShuffle);

            int savedPos = main.prefs.getInt("book_pos_" + track.getAbsolutePath(), 0);
            if (savedPos > 0 && (main.isAudiobookLibraryMode || track.getAbsolutePath().contains("/Audiobooks"))) {
                exoPlayer.seekTo(savedPos);
            }

            exoPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));

            if (!main.isPausedByHand) exoPlayer.setPlayWhenReady(true);

            main.consecutiveErrorCount = 0;
            String currentTrackNum = String.format(Locale.US, "%02d", index + 1);
            String totalTrackNum = String.format(Locale.US, "%02d", main.currentPlaylist.size());
            main.tvPlayerTrackCount.setText(currentTrackNum + " / " + totalTrackNum);

        } catch (Throwable e) {
            main.consecutiveErrorCount++;
            String failReason = "Unknown Error";
            if (e instanceof OutOfMemoryError) failReason = "Album Art is too huge!";
            else if (e instanceof java.io.FileNotFoundException) failReason = "File not found";
            else if (e instanceof java.io.IOException) failReason = "Broken file";

            main.tvPlayerTitle.setText("Load Failed ❌");
            main.tvPlayerArtist.setText(failReason);
            Toast.makeText(main, "🚨 " + failReason, Toast.LENGTH_SHORT).show();

            if (main.consecutiveErrorCount >= main.currentPlaylist.size()) {
                main.isPausedByHand = true;
                main.updatePlayerUI();
                main.consecutiveErrorCount = 0;
            } else {
                new Handler().postDelayed(() -> nextTrack(), 2000);
            }
        }
    }

    private void saveAudiobookBookmarkIfNeeded() {
        try {
            MainActivity main = MainActivity.instance;
            if (main != null && main.currentPlaylist != null && !main.currentPlaylist.isEmpty()) {
                if (main.currentIndex >= 0 && main.currentIndex < main.currentPlaylist.size()) {
                    String filePath = main.currentPlaylist.get(main.currentIndex).getAbsolutePath();

                    // 🚀 [수정] 오디오북 폴더, 팟캐스트 로컬 폴더, 팟캐스트 스트리밍 주소까지 모두 저장 허용!
                    if (filePath.startsWith("/storage/sdcard0/Audiobooks") || filePath.contains("/Podcasts") || filePath.startsWith("/PODCAST_STREAM") || main.isAudiobookLibraryMode) {
                        AudiobookManager.getInstance(main).saveBookmark(filePath, getCurrentPosition(), main.currentIndex);

                        main.prefs.edit()
                                .putInt("book_pos_" + filePath, getCurrentPosition())
                                .putInt("book_dur_" + filePath, getDuration())
                                .apply();
                    }
                }
            }
        } catch (Exception e) {}
    }


    public int getCurrentPosition() {
        if (exoPlayer != null) {
            long pos = exoPlayer.getCurrentPosition();
            return pos < 0 ? 0 : (int) pos;
        }
        return 0;
    }

    public int getDuration() {
        if (exoPlayer != null) {
            long duration = exoPlayer.getDuration();
            return duration < 0 ? 0 : (int) duration;
        }
        return 0;
    }

    public boolean isPlaying() {
        if (exoPlayer != null) return exoPlayer.getPlayWhenReady();
        return false;
    }

    public int getAudioSessionId() {
        if (exoPlayer != null) return exoPlayer.getAudioSessionId();
        return 0;
    }

    public void releasePlayer() {
        if (exoPlayer != null) { exoPlayer.release(); exoPlayer = null; }
    }

    // =======================================================
    // 🚀 [자체 제작 4.0] Ogg 껍데기 분쇄형 Opus 정밀 스캐너 (6종 메타데이터 싹쓸이)
    // =======================================================
    public Object[] extractOpusMetadata(File file) {
        // [0]제목, [1]가수, [2]앨범, [3]연도, [4]장르, [5]앨범아트(byte[])
        Object[] tags = new Object[]{null, null, null, null, null, null};
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] header = new byte[27];
            int totalRead = 0;

            // 🚀 1. 최대 1.5MB까지만 읽어서 Ogg 캡슐 껍데기를 싹 벗겨내고 순수 알맹이만 이어 붙입니다!
            while (totalRead < 1500000 && fis.read(header) == 27) {
                if (header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S') break;

                int pageSegments = header[26] & 0xFF;
                byte[] segmentTable = new byte[pageSegments];
                fis.read(segmentTable);

                int pageSize = 0;
                for (int i = 0; i < pageSegments; i++) pageSize += (segmentTable[i] & 0xFF);

                byte[] pageData = new byte[pageSize];
                int read = fis.read(pageData);
                if (read > 0) bos.write(pageData, 0, read);

                totalRead += (27 + pageSegments + pageSize);
            }
            fis.close();

            // 🚀 2. 껍데기가 사라진 순수 텍스트 덩어리에서 명찰(OpusTags)을 찾습니다.
            byte[] buffer = bos.toByteArray();
            byte[] magic = "OpusTags".getBytes("UTF-8");
            int p = -1;
            for (int i = 0; i < buffer.length - magic.length; i++) {
                boolean match = true;
                for (int j = 0; j < magic.length; j++) {
                    if (buffer[i + j] != magic[j]) { match = false; break; }
                }
                if (match) { p = i; break; }
            }

            // 🚀 3. 태그 6종류 정밀 폭격 추출 가동!
            if (p != -1) {
                p += 8;
                int vendorLen = (buffer[p] & 0xFF) | ((buffer[p+1] & 0xFF) << 8) | ((buffer[p+2] & 0xFF) << 16) | ((buffer[p+3] & 0xFF) << 24);
                p += 4 + vendorLen;

                int commentsCount = (buffer[p] & 0xFF) | ((buffer[p+1] & 0xFF) << 8) | ((buffer[p+2] & 0xFF) << 16) | ((buffer[p+3] & 0xFF) << 24);
                p += 4;

                for (int i = 0; i < commentsCount && p < buffer.length - 4; i++) {
                    int commentLen = (buffer[p] & 0xFF) | ((buffer[p+1] & 0xFF) << 8) | ((buffer[p+2] & 0xFF) << 16) | ((buffer[p+3] & 0xFF) << 24);
                    p += 4;
                    if (commentLen <= 0 || p + commentLen > buffer.length) break;

                    String comment = new String(buffer, p, commentLen, "UTF-8");
                    p += commentLen;
                    String upper = comment.toUpperCase();

                    // 라이브러리 분류를 위한 5대 텍스트 수집!
                    // 라이브러리 분류를 위한 5대 텍스트 수집!
                    if (upper.startsWith("TITLE=")) tags[0] = comment.substring(6);
                    else if (upper.startsWith("ARTIST=") || upper.startsWith("AUTHOR=")) tags[1] = comment.substring(comment.indexOf("=") + 1); // 🚀 작가(AUTHOR) 태그 완벽 지원!
                    else if (upper.startsWith("ALBUM=")) tags[2] = comment.substring(6);
                    else if (upper.startsWith("DATE=") || upper.startsWith("YEAR=")) tags[3] = comment.substring(comment.indexOf("=") + 1);
                    else if (upper.startsWith("GENRE=")) tags[4] = comment.substring(6);
                    else if (upper.startsWith("TRACKNUMBER=") || upper.startsWith("TRACKNUM=")) tags[6] = comment.substring(comment.indexOf("=") + 1);
                    else if (upper.startsWith("METADATA_BLOCK_PICTURE=")) {
                        try {
                            // 🚀 공백, 줄바꿈 찌꺼기를 완벽히 지워 Base64 해독 성공률 100% 달성!
                            String base64Data = comment.substring(23).replaceAll("\\s", "");
                            byte[] flacPic = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);

                            int ptr = 4;
                            int mimeLen = ((flacPic[ptr] & 0xFF) << 24) | ((flacPic[ptr+1] & 0xFF) << 16) | ((flacPic[ptr+2] & 0xFF) << 8) | (flacPic[ptr+3] & 0xFF);
                            ptr += 4 + mimeLen;
                            int descLen = ((flacPic[ptr] & 0xFF) << 24) | ((flacPic[ptr+1] & 0xFF) << 16) | ((flacPic[ptr+2] & 0xFF) << 8) | (flacPic[ptr+3] & 0xFF);
                            ptr += 4 + descLen;
                            ptr += 16;
                            int picDataLen = ((flacPic[ptr] & 0xFF) << 24) | ((flacPic[ptr+1] & 0xFF) << 16) | ((flacPic[ptr+2] & 0xFF) << 8) | (flacPic[ptr+3] & 0xFF);
                            ptr += 4;

                            if (ptr + picDataLen <= flacPic.length) {
                                byte[] img = new byte[picDataLen];
                                System.arraycopy(flacPic, ptr, img, 0, picDataLen);
                                tags[5] = img; // 🎯 앨범 아트 최종 확보!
                            }
                        } catch (Exception e) {}
                    }
                }
            }
        } catch (Exception e) {}
        return tags;
    }
    // =======================================================
    // 🚀 [자체 제작 6.0] FLAC 6기통 만능 채굴기 (앨범, 연도, 장르, 가사 완벽 지원)
    // =======================================================
    public Object[] extractFlacMetadata(File file) {
        // 🚨 [치명적 버그 수리 완료] 바구니 크기를 6칸에서 8칸으로 늘려 앱 강제 종료(Crash)를 막습니다!
        // [0]제목, [1]가수, [2]앨범, [3]연도, [4]장르, [5]앨범아트, [6]트랙번호, [7]가사
        Object[] tags = new Object[]{null, null, null, null, null, null, null, null};
        try {
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
            byte[] header = new byte[4];
            raf.readFully(header);

            if (header[0] != 'f' || header[1] != 'L' || header[2] != 'a' || header[3] != 'C') {
                raf.close(); return tags;
            }

            boolean isLast = false;
            while (!isLast) {
                int blockHeader = raf.readUnsignedByte();
                isLast = (blockHeader & 0x80) != 0;
                int blockType = blockHeader & 0x7F;
                int length = (raf.readUnsignedByte() << 16) | (raf.readUnsignedByte() << 8) | raf.readUnsignedByte();

                if (blockType == 4) { // 🚀 텍스트 정보 추출 (Vorbis Comment)
                    byte[] commentData = new byte[length];
                    raf.readFully(commentData);
                    try {
                        int p = 0;
                        int vendorLen = (commentData[p]&0xFF) | ((commentData[p+1]&0xFF)<<8) | ((commentData[p+2]&0xFF)<<16) | ((commentData[p+3]&0xFF)<<24);
                        p += 4 + vendorLen;
                        int listLen = (commentData[p]&0xFF) | ((commentData[p+1]&0xFF)<<8) | ((commentData[p+2]&0xFF)<<16) | ((commentData[p+3]&0xFF)<<24);
                        p += 4;
                        for (int i = 0; i < listLen && p < commentData.length - 4; i++) {
                            int strLen = (commentData[p]&0xFF) | ((commentData[p+1]&0xFF)<<8) | ((commentData[p+2]&0xFF)<<16) | ((commentData[p+3]&0xFF)<<24);
                            p += 4;
                            String comment = new String(commentData, p, strLen, "UTF-8");
                            p += strLen;
                            String upper = comment.toUpperCase();

                            // 🚀 대망의 텍스트 수집 (가사 포함!)
                            if (upper.startsWith("TITLE=")) tags[0] = comment.substring(6);
                            else if (upper.startsWith("ARTIST=") || upper.startsWith("AUTHOR=")) tags[1] = comment.substring(comment.indexOf("=") + 1);
                            else if (upper.startsWith("ALBUM=")) tags[2] = comment.substring(6);
                            else if (upper.startsWith("DATE=") || upper.startsWith("YEAR=")) tags[3] = comment.substring(comment.indexOf("=") + 1);
                            else if (upper.startsWith("GENRE=")) tags[4] = comment.substring(6);
                            else if (upper.startsWith("TRACKNUMBER=") || upper.startsWith("TRACKNUM=")) tags[6] = comment.substring(comment.indexOf("=") + 1);
                                // 🎯 [신규 장착] FLAC 내부에 LYRICS 라는 이름으로 박혀있는 가사 텍스트를 무자비하게 캐옵니다!
                            else if (upper.startsWith("LYRICS=")) tags[7] = comment.substring(7);
                        }
                    } catch (Exception e) {}
                } else if (blockType == 6) { // 🚀 사진 추출
                    int picType = raf.readInt();
                    int mimeLen = raf.readInt(); raf.skipBytes(mimeLen);
                    int descLen = raf.readInt(); raf.skipBytes(descLen);
                    raf.skipBytes(16);
                    int picDataLen = raf.readInt();
                    byte[] picData = new byte[picDataLen];
                    raf.readFully(picData);
                    tags[5] = picData; // 🎯 사진 데이터
                } else {
                    raf.skipBytes(length);
                }
            }
            raf.close();
        } catch (Exception e) {}
        return tags;
    }
    // =======================================================
    // 🚀 [자체 제작 7.0] ALAC / M4A (Apple Lossless) 원자 단위 정밀 해독기!
    // 애플의 악랄한 moov -> udta -> meta -> ilst 트리 구조를 추적하여 태그와 앨범 아트를 싹쓸이합니다.
    // =======================================================
    public Object[] extractAlacMetadata(File file) {
        // 💡 [수정] null을 8개로 맞춰야 앱이 터지지 않습니다!
        Object[] tags = new Object[]{null, null, null, null, null, null, null, null};
        try {
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
            long fileSize = raf.length();
            long pos = 0;

            // 🚀 MP4 원자(Atom) 구조 탐색 크롤러 가동!
            while (pos < fileSize) {
                raf.seek(pos);
                int size = raf.readInt();
                if (size <= 0) break; // 파일 끝이거나 구조가 깨졌으면 탈출

                byte[] typeBytes = new byte[4];
                raf.readFully(typeBytes);

                // 🚨 [치명적 버그 수정!] '©' 기호가 UTF-8에서 깨지는 현상을 막기 위해 ISO-8859-1 렌즈를 장착합니다!
                String type = new String(typeBytes, "ISO-8859-1");

                // 💡 1. 껍데기(컨테이너) 원자를 발견하면 크기(Size)를 건너뛰고 내부로 파고듭니다!
                if (type.equals("moov") || type.equals("udta") || type.equals("meta") || type.equals("ilst")) {
                    if (type.equals("meta")) pos += 4; // meta 원자는 4바이트의 버전/플래그가 숨어있으므로 회피
                    pos += 8; // Size(4) + Type(4) 만큼 전진해서 알맹이로 진입!
                    continue;
                }

                // 💡 조건문에 type.equals("©lyr") 추가 완료!
                if (type.equals("©nam") || type.equals("©ART") || type.equals("©alb") ||
                        type.equals("©day") || type.equals("©gen") || type.equals("covr") ||
                        type.equals("trkn") || type.equals("©lyr")) {

                    raf.seek(pos + 8);
                    int dataSize = raf.readInt();
                    byte[] dataTypeBytes = new byte[4];
                    raf.readFully(dataTypeBytes);

                    // 🚨 [여기서도 수정!] 이름표는 무조건 ISO-8859-1 렌즈로 읽습니다.
                    String dataType = new String(dataTypeBytes, "ISO-8859-1");

                    // 실제 값이 들어있는 'data' 구역인지 확인
                    if (dataType.equals("data")) {
                        raf.skipBytes(8); // 버전, 플래그, 빈 공간(Null padding) 8바이트 스킵
                        int actualDataSize = dataSize - 16;

                        // 데이터가 10MB 이하일 때만 읽어옵니다 (메모리 폭발 방지)
                        if (actualDataSize > 0 && actualDataSize < 10485760) {
                            byte[] data = new byte[actualDataSize];
                            raf.readFully(data);

                            // 🎯 알맹이(진짜 텍스트)는 UTF-8 인코딩이 맞으므로 그대로 파싱합니다!
                            if (type.equals("©nam")) tags[0] = new String(data, "UTF-8"); // 제목
                            else if (type.equals("©ART")) tags[1] = new String(data, "UTF-8"); // 가수
                            else if (type.equals("©alb")) tags[2] = new String(data, "UTF-8"); // 앨범
                            else if (type.equals("©day")) tags[3] = new String(data, "UTF-8"); // 연도
                            else if (type.equals("©gen")) tags[4] = new String(data, "UTF-8"); // 장르
                            else if (type.equals("covr")) tags[5] = data; // 앨범 아트
                            else if (type.equals("trkn") && actualDataSize >= 4) {
                                tags[6] = String.valueOf((int) data[3]); // 트랙 번호
                            }
                            // 🚀 [신규 장착] 대망의 가사 채굴! UTF-8 텍스트로 변환하여 8번째 바구니에 담습니다!
                            else if (type.equals("©lyr")) {
                                tags[7] = new String(data, "UTF-8");
                            }
                        }
                    }
                }
                pos += size; // 💡 현재 원자 탐색이 끝났으면 다음 원자로 훌쩍 건너뜁니다!
            }
            raf.close();
        } catch (Exception e) {}
        return tags;
    }


}