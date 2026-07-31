package com.themoon.y1.adapters;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.themoon.y1.MainActivity;
import com.themoon.y1.ThemeManager;
import com.themoon.y1.models.SongItem;

import java.util.List;

public class SongListAdapter extends BaseAdapter {
    private List<SongItem> items;

    public SongListAdapter(List<SongItem> items) {
        this.items = items;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final android.view.View btn;
        final SongItem song = items.get(position);

        String iconCode = MainActivity.instance.isAudiobookLibraryMode ? "\uE310" : "\uE405";
        String displayTitle = song.title;
        int customColor = 0;

        if (MainActivity.instance != null) {


            // ==========================================
            // 🚀 1. 팟캐스트 에피소드 모드 (시각적 구분 디자인 대개조!)
            // ==========================================
            if (MainActivity.instance.currentBrowserMode == 14) {
                String audioUrl = song.genre;
                String channelName = song.artist;
                String pubDate = song.year;
                String datePrefix = (pubDate != null && !pubDate.isEmpty()) ? "[" + pubDate.trim() + "] " : "";

                String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
                String safeTitle = song.title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
                java.io.File localFile = new java.io.File("/storage/sdcard0/Podcasts/" + safeChannel, safeTitle);

                // ⏱ 저장된 재생 위치 시간 가져오기
                int savedPos = MainActivity.instance.prefs.getInt("book_pos_" + localFile.getAbsolutePath(), 0);
                if (savedPos == 0) {
                    String streamKey = "/PODCAST_STREAM/" + safeChannel + "/" + safeTitle;
                    savedPos = MainActivity.instance.prefs.getInt("book_pos_" + streamKey, 0);
                }

                String progressText = "";
                if (savedPos > 0) {
                    long min = (savedPos / 1000) / 60;
                    long sec = (savedPos / 1000) % 60;
                    progressText = String.format(" [⏱ %02d:%02d]", min, sec);
                }

                // 🚀 [디자인 대개조 핵심 구역]
                if (MainActivity.instance.activePodcastDownloads.containsKey(audioUrl)) {
                    // 1. ⏳ 현재 열심히 다운로드 받고 있는 중일 때 (진행률 표시)
                    int prog = 0;
                    if (MainActivity.instance.podcastDownloadProgress.containsKey(audioUrl)) {
                        prog = MainActivity.instance.podcastDownloadProgress.get(audioUrl);
                    }
                    displayTitle = "⏳ [" + prog + "%] " + datePrefix + song.title;
                    customColor = 0xFFFF8800; // 눈에 확 띄는 오렌지색 유지!
                } else if (localFile.exists() && localFile.length() > 0) {
                    // 2. 🎵 완벽하게 기기에 다운로드가 끝난 파일일 때!
                    // 💡 요청사항 적용 완료: 귀찮은 체크마크(✔)를 싹 지우고 순수하게 텍스트만 표시합니다!
                    displayTitle = datePrefix + song.title + progressText;
                    customColor = ThemeManager.getTextColorPrimary(); // 기본 테마 흰색(주 색상)으로 얌전하게!
                } else {
                    // 3. ☁️ 아직 안 받았고, 인터넷 연결해서 다운로드가 필요한 상태일 때!
                    // 💡 요청사항 적용 완료: 제목 앞에 '클라우드(구름)' 아이콘을 붙이고 색상을 흐리게 해서 시각적으로 확 떨어트립니다.
                    displayTitle = "☁️ " + datePrefix + song.title + progressText;
                    customColor = ThemeManager.getTextColorSecondary(); // 약간 흐릿한 회색(보조 색상) 적용!
                }
            }
            // ==========================================
            // 📅 2. '최근 추가된 곡' 모드일 때
            // ==========================================
            else if ("RECENT".equals(MainActivity.instance.virtualQueryType)) {
                long lastMod = song.file.lastModified();
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0);
                long todayStart = cal.getTimeInMillis();
                long yesterdayStart = todayStart - (24 * 60 * 60 * 1000);
                String datePrefix = "";
                if (lastMod >= todayStart) datePrefix = "[" + MainActivity.instance.t("Today") + "] ";
                else if (lastMod >= yesterdayStart) datePrefix = "[" + MainActivity.instance.t("Yesterday") + "] ";
                else {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yy.MM.dd");
                    datePrefix = "[" + sdf.format(new java.util.Date(lastMod)) + "] ";
                }
                displayTitle = datePrefix + song.title;
            }
        }

        // 🚀 UI 그리기 (계산된 텍스트와 색상을 여기서 한 번에 입힙니다)
        if (convertView == null) {
            btn = MainActivity.instance.createListButtonWithIcon(iconCode, displayTitle, customColor);
            btn.setLayoutParams(new android.widget.AbsListView.LayoutParams(android.widget.AbsListView.LayoutParams.MATCH_PARENT, android.widget.AbsListView.LayoutParams.WRAP_CONTENT));
        } else {
            btn = convertView;
            if (btn instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout layout = (android.widget.LinearLayout) btn;
                if (layout.getChildCount() > 1) {
                    android.widget.TextView tvIcon = (android.widget.TextView) layout.getChildAt(0);
                    android.widget.TextView tvText = (android.widget.TextView) layout.getChildAt(1);
                    tvIcon.setText(iconCode);
                    tvText.setText(displayTitle);
                    int applyColor = (customColor != 0) ? customColor : com.themoon.y1.ThemeManager.getTextColorPrimary();
                    if (!btn.hasFocus()) { tvIcon.setTextColor(applyColor); tvText.setTextColor(applyColor); }
                }
            }
        }

        // =======================================================
        // 🚀 [포커스 늪 버그 수리 완료] 진행률과 포커스 센서를 하나로 완벽하게 묶습니다!
        // =======================================================
        int pos = 0;
        int dur = 0;

        if (MainActivity.instance.isAudiobookLibraryMode) {
            pos = MainActivity.instance.prefs.getInt("book_pos_" + song.file.getAbsolutePath(), 0);
            dur = MainActivity.instance.prefs.getInt("book_dur_" + song.file.getAbsolutePath(), 0);
        }

        if (MainActivity.instance.currentBrowserMode == 14) {
            String safeChannel = song.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
            String safeTitle = song.title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
            java.io.File localFile = new java.io.File("/storage/sdcard0/Podcasts/" + safeChannel, safeTitle);
            String streamKey = "/PODCAST_STREAM/" + safeChannel + "/" + safeTitle;

            pos = MainActivity.instance.prefs.getInt("book_pos_" + localFile.getAbsolutePath(), 0);
            dur = MainActivity.instance.prefs.getInt("book_dur_" + localFile.getAbsolutePath(), 0);

            if (pos == 0 || dur == 0) {
                pos = MainActivity.instance.prefs.getInt("book_pos_" + streamKey, 0);
                dur = MainActivity.instance.prefs.getInt("book_dur_" + streamKey, 0);
            }
        }

        // 처음 화면에 그릴 때 진행률이 있으면 그려주고, 없으면 일반 배경 세팅
        if (pos > 0 && dur > 0) {
            MainActivity.instance.setupAudiobookProgress(btn, pos, dur);
        } else {
            btn.setBackground(MainActivity.instance.createButtonBackground(com.themoon.y1.ThemeManager.getListButtonNormalBg()));
        }

        // 🚨 예외 없이 무조건 포커스 센서 장착! (pos, dur 값을 같이 넘겨줍니다)
        applyDefaultFocusListener(btn, song.title, customColor, pos, dur);
        // =======================================================

        // 🚀 [클릭 이벤트 처리]
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance.clickFeedback();

                // 팟캐스트 인터셉터
                if (MainActivity.instance.currentBrowserMode == 14) {
                    String audioUrl = song.genre;
                    String imageUrl = song.album;
                    String channelName = song.artist;

                    String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
                    String safeTitle = song.title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
                    java.io.File localFile = new java.io.File("/storage/sdcard0/Podcasts/" + safeChannel, safeTitle);

                    if (localFile.exists() && localFile.length() > 0) {
                        int savedPos = MainActivity.instance.prefs.getInt("book_pos_" + localFile.getAbsolutePath(), 0);
                        if (savedPos == 0) {
                            String streamKey = "/PODCAST_STREAM/" + safeChannel + "/" + safeTitle;
                            savedPos = MainActivity.instance.prefs.getInt("book_pos_" + streamKey, 0);
                        }

                        // =======================================================
                        // 🚀 [연속 재생 엔진 탑재!]
                        // 현재 채널 리스트(items)를 훑어서 '다운로드된' 파일들만 싹 다 바구니에 담습니다.
                        // =======================================================
                        java.util.List<java.io.File> playList = new java.util.ArrayList<>();
                        int targetIdx = 0;

                        for (SongItem ep : items) {
                            String epTitle = ep.title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
                            java.io.File epFile = new java.io.File("/storage/sdcard0/Podcasts/" + safeChannel, epTitle);

                            // 파일이 기기에 실제로 존재할 때만 바구니에 합류!
                            if (epFile.exists() && epFile.length() > 0) {
                                playList.add(epFile);
                                // 지금 내가 누른 이 파일이 바구니의 몇 번째(인덱스)에 담겼는지 추적합니다.
                                if (epFile.getAbsolutePath().equals(localFile.getAbsolutePath())) {
                                    targetIdx = playList.size() - 1;
                                }
                            }
                        }

                        // 바구니 통째로(playList)와 내가 누른 곡 번호(targetIdx)를 엔진에 장전!
                        if (savedPos > 0) {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().playTrackListWithOffset(playList, targetIdx, savedPos);
                        } else {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().playTrackList(playList, targetIdx);
                        }
                        MainActivity.instance.changeScreen(3);
                    } else {
                        MainActivity.instance.showPodcastActionDialog(song.title, audioUrl, imageUrl, channelName);
                    }

                    // 🚨 [핵심] 여기서 반드시 리턴(탈출)해야 에러가 발생하지 않습니다!
                    return;
                }

                // 🎵 일반 음악 재생 (팟캐스트가 아닐 때만 여기로 내려옵니다)
                com.themoon.y1.managers.AudioPlayerManager.getInstance().playTrackList(MainActivity.instance.virtualSongList, position);
                MainActivity.instance.changeScreen(3);
            }
        });

        // 롱클릭 이벤트
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MainActivity.instance.clickFeedback();
                MainActivity.instance.isLongPressConsumed = true;

                // =======================================================
                // 🚀 [초강력 팟캐스트 식별 엔진]
                // 모드 번호(14)가 꼬였거나, '최근 추가된 곡/전체 곡'에서 눌렀을 때를 대비해
                // 파일의 태생(경로)이 팟캐스트면 무조건 이벤트를 가로챕니다!
                // =======================================================
                boolean isPodcast = false;
                if (MainActivity.instance.currentBrowserMode == 14) {
                    isPodcast = true;
                } else if (song.file != null && song.file.getAbsolutePath() != null) {
                    // 실제 파일 주소에 /Podcasts/ 가 포함되어 있거나, 가상 주소인 /PODCAST 를 쓰면 팟캐스트로 인정!
                    if (song.file.getAbsolutePath().contains("/Podcasts/") || "/PODCAST".equals(song.file.getAbsolutePath())) {
                        isPodcast = true;
                    }
                }

                if (isPodcast) {
                    String safeChannel = song.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
                    String safeTitle = song.title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
                    java.io.File localFile = new java.io.File("/storage/sdcard0/Podcasts/" + safeChannel, safeTitle);

                    // 💡 만약 '전체 곡'이나 '최근 추가된 곡'에서 실제 다운로드된 MP3를 눌렀다면 그 파일 자체를 타겟으로!
                    if (song.file != null && song.file.getAbsolutePath() != null && song.file.getAbsolutePath().contains("/Podcasts/") && song.file.exists()) {
                        localFile = song.file;
                    }

                    if (localFile.exists() && localFile.length() > 0) {
                        // 🎯 다운로드 완료된 파일이면 삭제 팝업 호출!
                        MainActivity.instance.showDeletePodcastDialog(localFile, song.title);
                    } else {
                        // 🎯 파일이 없으면 안내 메시지만 띄우고 창 열기 원천 차단!
                        android.widget.Toast.makeText(MainActivity.instance, "📡 " + MainActivity.instance.t("Offline Mode: This episode has not been downloaded yet."), android.widget.Toast.LENGTH_SHORT).show();
                    }
                    return true; // 🚨 여기서 완벽하게 탈출! 플레이리스트 창 절대 안 뜸!
                }
                // =======================================================

                if (MainActivity.instance.currentBrowserMode == 5) {
                    MainActivity.instance.showRemoveFromFavoritesDialog(song.file);
                } else if (MainActivity.instance.currentBrowserMode == 7) {
                    MainActivity.instance.showRemoveFromPlaylistDialog(song.file);
                } else {
                    MainActivity.instance.showAddToPlaylistDialog(song.file);
                }
                return true;
            }
        });

        return btn;
    }

    // 🚀 [신규] 배경(Progress)과 포커스를 100% 동시 제어하는 무적의 하이브리드 리스너!
    private void applyDefaultFocusListener(final android.view.View btn, final String title, final int customColor, final int pos, final int dur) {
        final int normalColor = (customColor != 0) ? customColor : com.themoon.y1.ThemeManager.getTextColorPrimary();

        final android.view.View.OnFocusChangeListener listener = new android.view.View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(android.view.View v, boolean hasFocus) {
                if (hasFocus) {
                    // 🎯 포커스가 닿으면 무조건 강조 색상으로 덮습니다!
                    btn.setBackground(MainActivity.instance.createButtonBackground(com.themoon.y1.ThemeManager.getListButtonFocusedBg()));

                    if (v instanceof android.widget.LinearLayout) {
                        android.widget.LinearLayout row = (android.widget.LinearLayout) v;
                        if (row.getChildCount() > 1) {
                            ((android.widget.TextView) row.getChildAt(0)).setTextColor(com.themoon.y1.ThemeManager.getListButtonFocusedTextColor());
                            android.widget.TextView tvText = (android.widget.TextView) row.getChildAt(1);
                            tvText.setTextColor(com.themoon.y1.ThemeManager.getListButtonFocusedTextColor());
                            tvText.setSelected(true); // 🚀 텍스트 흐르기 가동!
                        }
                    } else if (v instanceof android.widget.Button) {
                        ((android.widget.Button) v).setTextColor(com.themoon.y1.ThemeManager.getListButtonFocusedTextColor());
                        v.setSelected(true);
                    }

                    MainActivity.instance.showFastScrollLetter(title);
                } else {
                    // 🎯 포커스가 빠져나갈 때, 재생 기록이 있으면 진행률 바를 다시 복구해 줍니다!
                    if (pos > 0 && dur > 0) {
                        MainActivity.instance.setupAudiobookProgress(btn, pos, dur);
                    } else {
                        btn.setBackground(MainActivity.instance.createButtonBackground(com.themoon.y1.ThemeManager.getListButtonNormalBg()));
                    }

                    if (v instanceof android.widget.LinearLayout) {
                        android.widget.LinearLayout row = (android.widget.LinearLayout) v;
                        if (row.getChildCount() > 1) {
                            ((android.widget.TextView) row.getChildAt(0)).setTextColor(normalColor);
                            android.widget.TextView tvText = (android.widget.TextView) row.getChildAt(1);
                            tvText.setTextColor(normalColor);
                            tvText.setSelected(false); // 🚀 텍스트 흐르기 정지!
                        }
                    } else if (v instanceof android.widget.Button) {
                        ((android.widget.Button) v).setTextColor(normalColor);
                        v.setSelected(false);
                    }
                }
            }
        };

        btn.setOnFocusChangeListener(listener);

        // UI가 그려진 직후 포커스 동기화
        btn.post(new Runnable() {
            @Override
            public void run() {
                if (btn.isFocused()) {
                    listener.onFocusChange(btn, true);
                }
            }
        });
    }
}