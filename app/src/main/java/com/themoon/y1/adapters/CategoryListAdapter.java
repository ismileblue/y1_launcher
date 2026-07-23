package com.themoon.y1.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.themoon.y1.MainActivity;
import com.themoon.y1.R;
import com.themoon.y1.ThemeManager;
import com.themoon.y1.models.SongItem;

import java.io.File;
import java.util.List;

public class CategoryListAdapter extends BaseAdapter {
    private List<String> items;
    private String type;

    // 🚀 스크롤 할 때 버벅거리지 않도록 이미지를 기억해두는 '메모리 캐시 금고'입니다!
    private static LruCache<String, Drawable> coverCache;

    public CategoryListAdapter(List<String> items, String type) {
        this.items = items;
        this.type = type;

        if (coverCache == null) {
            coverCache = new LruCache<>(50); // 최대 50개의 앨범 아트를 메모리에 안전하게 기억
        }
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Button btn;

        if (convertView == null) {
            btn = MainActivity.instance.createListButton("");
            btn.setLayoutParams(new AbsListView.LayoutParams(
                    AbsListView.LayoutParams.MATCH_PARENT,
                    AbsListView.LayoutParams.WRAP_CONTENT));
        } else {
            btn = (Button) convertView;
        }

        final String name = items.get(position);

        // 🚀 [핵심 개조] 앨범 모드거나, 가수의 앨범(ARTIST_ALBUM) 모드일 때 모두 왼쪽 앨범 아트 썸네일을 세팅합니다!
        // 🚀 [핵심 개조] 앨범 모드일 때만 왼쪽 아이콘(앨범 아트 썸네일)을 세팅합니다!
        if (type.equals("ALBUM") || type.equals("ARTIST_ALBUM") || type.equals("YEAR_ARTIST_ALBUM") || type.equals("GENRE_ARTIST_ALBUM")) {
            btn.setText(name); // 💿 이모티콘을 없애고 순수 앨범 이름만 넣습니다.
            // 1. 메모리 금고에 이미 불러온 그림이 있는지 확인!
            Drawable leftDrawable = coverCache.get(name);

            // 2. 금고에 그림이 없다면? 직접 찾아서 그립니다.
            if (leftDrawable == null) {
                String artPath = "";
                byte[] embeddedPic = null;

                // 🚀 [버그 대수술] 이 앨범에 속한 '모든 노래'를 전부 뒤집니다!
                // 특정 곡(예: 3번 트랙)을 재생할 때 다운로드된 이미지가 저장되었더라도,
                // 앨범 카테고리 전체 리스트에서 완벽하게 찾아내도록 조회 범위를 넓힙니다.
                for (SongItem song : MainActivity.customLibrary) {
                    if (song.album.equals(name)) {
                        String trackPath = song.file.getAbsolutePath();

                        // ① SharedPreferences 금고에 다운로드 경로가 등록되어 있는지 확인
                        if (MainActivity.instance.prefs != null) {
                            String savedPath = MainActivity.instance.prefs.getString("album_art_" + trackPath, "");
                            if (!savedPath.isEmpty() && new File(savedPath).exists()) {
                                artPath = savedPath;
                                break; // 이미지를 찾았으면 즉시 탈출!
                            }
                        }

                        // ② 금고 등록 정보가 누락되었을 경우를 대비해, 파일 이름 매칭으로 폴더 직접 스캔 더블 체크!
                        String safeFileName = song.file.getName().replace(".mp3", "").replace(".flac", "").replace(".wav", "").replace(".m4a", "").replace(".aac", "").replace(".ogg", "");
                        File manualCoverFile = new File("/storage/sdcard0/Y1_Covers", safeFileName + ".jpg");
                        if (manualCoverFile.exists()) {
                            artPath = manualCoverFile.getAbsolutePath();
                            break; // 실제 파일이 존재하면 즉시 탈출!
                        }

                        // 🚀 ③ 인터넷 이미지가 없다면 파일 내부 내장 아트(Embedded) 후보로 등록
                        if (embeddedPic == null) {

                            // 🌟 [추가된 4.0 스캐너] Opus 파일일 경우 바주카포 출동!
                            if (trackPath.toLowerCase().endsWith(".opus")) {
                                try {
                                    Object[] opusTags = com.themoon.y1.managers.AudioPlayerManager.getInstance().extractOpusMetadata(new File(trackPath));
                                    if (opusTags != null && opusTags.length > 5 && opusTags[5] != null) {
                                        embeddedPic = (byte[]) opusTags[5]; // 5번 서랍에 든 앨범 아트 빼오기
                                    }
                                } catch (Exception e) {}
                            }
                            // 🌟 [초강력 FLAC 해독기 장착!] FLAC 앨범아트 누락 완벽 해결
                            else if (trackPath.toLowerCase().endsWith(".flac")) {
                                try {
                                    Object[] flacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance().extractFlacMetadata(new File(trackPath));
                                    if (flacTags != null && flacTags.length > 5 && flacTags[5] != null) {
                                        embeddedPic = (byte[]) flacTags[5]; // FLAC은 무조건 5번 방에서 꺼내옵니다!
                                    }
                                } catch (Exception e) {}
                            }
                            // 🌟 기존 파일(MP3 등)은 안드로이드 순정 부품 사용
                            else {
                                android.media.MediaMetadataRetriever mmr = null;
                                java.io.FileInputStream fis = null;
                                try {
                                    mmr = new android.media.MediaMetadataRetriever();
                                    fis = new java.io.FileInputStream(trackPath);
                                    mmr.setDataSource(fis.getFD());
                                    byte[] pic = mmr.getEmbeddedPicture();
                                    if (pic != null && pic.length > 0) {
                                        embeddedPic = pic;
                                    }
                                } catch (Exception e) {
                                } finally {
                                    try { if (fis != null) fis.close(); } catch (Exception e) {}
                                    try { if (mmr != null) mmr.release(); } catch (Exception e) {}
                                }
                            }
                        }
                    }
                }

                int size = (int) (40 * MainActivity.instance.getResources().getDisplayMetrics().density);
                Bitmap bmp = null;

                // [선택 1] 인터넷 다운로드 커버가 있으면 최우선 로딩
                if (!artPath.isEmpty()) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 4;
                        bmp = BitmapFactory.decodeFile(artPath, opts);
                    } catch (Exception e) {}
                }
                // [선택 2] 인터넷 커버가 없으면 파일 내장 아트 로딩
                else if (embeddedPic != null) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 4;
                        bmp = BitmapFactory.decodeByteArray(embeddedPic, 0, embeddedPic.length, opts);
                    } catch (Exception e) {}
                }

                // [선택 3] 둘 다 없으면 기본 이미지
                if (bmp == null) {
                    bmp = BitmapFactory.decodeResource(MainActivity.instance.getResources(), R.drawable.default_album);
                }

                if (bmp != null) {
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, size, size, true);
                    leftDrawable = new BitmapDrawable(MainActivity.instance.getResources(), scaled);
                    leftDrawable.setBounds(0, 0, size, size);
                    coverCache.put(name, leftDrawable); // 다음번 고속 스크롤을 위해 메모리에 저장
                }
            }

            // 버튼 왼쪽에 이미지를 세팅하고, 글자와의 간격을 15dp 띄워줍니다.
            btn.setCompoundDrawables(leftDrawable, null, null, null);
            btn.setCompoundDrawablePadding((int)(15 * MainActivity.instance.getResources().getDisplayMetrics().density));

        } else {
            // 👤 가수(ARTIST) 모드일 때는 기존처럼 텍스트 이모티콘만 씁니다!
            btn.setText("👤 " + name);
            btn.setCompoundDrawables(null, null, null, null);
        }

        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    btn.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    MainActivity.instance.showFastScrollLetter(name);
                    btn.setSelected(true); // 🚀 추가! 텍스트 흐르기 가동
                } else {
                    btn.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btn.setTextColor(ThemeManager.getTextColorPrimary());
                    btn.setSelected(false); // 🚀 추가! 텍스트 흐르기 정지
                }
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance.clickFeedback();
                MainActivity.instance.virtualQueryType = type;
                MainActivity.instance.virtualQueryValue = name;
                MainActivity.instance.currentBrowserMode = MainActivity.BROWSER_VIRTUAL_SONGS;
                MainActivity.instance.buildVirtualSongs();
            }
        });

        return btn;
    }
}