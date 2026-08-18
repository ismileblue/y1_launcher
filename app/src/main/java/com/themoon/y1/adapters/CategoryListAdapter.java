package com.themoon.y1.adapters;

import com.themoon.y1.StoragePaths;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    private static class AlbumViewHolder {
        LinearLayout rowView;
        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;

        AlbumViewHolder(LinearLayout rowView, ImageView ivCover, TextView tvTitle, TextView tvArtist) {
            this.rowView = rowView;
            this.ivCover = ivCover;
            this.tvTitle = tvTitle;
            this.tvArtist = tvArtist;
        }
    }

    public CategoryListAdapter(List<String> items, String type) {
        this.items = items;
        this.type = type;

        if (coverCache == null) {
            coverCache = new LruCache<>(50); // 최대 50개의 앨범 아트를 메모리에 안전하게 기억
        }
    }

    private boolean isAlbumType() {
        return type != null && (type.equals("ALBUM") || type.equals("ARTIST_ALBUM") || 
               type.equals("YEAR_ARTIST_ALBUM") || type.equals("GENRE_ARTIST_ALBUM"));
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return isAlbumType() ? 0 : 1;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final String name = items.get(position);
        float d = MainActivity.instance.getResources().getDisplayMetrics().density;

        if (isAlbumType()) {
            final AlbumViewHolder holder;
            final LinearLayout rowView;

            if (convertView == null) {
                rowView = new LinearLayout(MainActivity.instance);
                rowView.setOrientation(LinearLayout.HORIZONTAL);
                rowView.setGravity(Gravity.CENTER_VERTICAL);
                rowView.setFocusable(true);
                rowView.setClickable(true);
                rowView.setSoundEffectsEnabled(false);
                rowView.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                rowView.setPadding((int)(8 * d), (int)(6 * d), (int)(8 * d), (int)(6 * d));

                AbsListView.LayoutParams rowLp = new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
                rowView.setLayoutParams(rowLp);

                ImageView ivCover = new ImageView(MainActivity.instance);
                int thumbSize = (int)(60 * d); // 🚀 60dp로 썸네일 대폭 확대!
                LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(thumbSize, thumbSize);
                imgLp.rightMargin = (int)(12 * d);
                ivCover.setLayoutParams(imgLp);
                ivCover.setScaleType(ImageView.ScaleType.FIT_CENTER);

                LinearLayout textContainer = new LinearLayout(MainActivity.instance);
                textContainer.setOrientation(LinearLayout.VERTICAL);
                textContainer.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                textContainer.setLayoutParams(tcLp);

                TextView tvTitle = new TextView(MainActivity.instance);
                tvTitle.setTextSize(18f); // 🚀 기존 리스트 폰트 크기(18sp) 유지!
                tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
                tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
                tvTitle.setSingleLine(true);
                tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
                tvTitle.setMarqueeRepeatLimit(-1);

                TextView tvArtist = new TextView(MainActivity.instance);
                tvArtist.setTextSize(14f); // 🚀 아티스트 이름도 시원하게 키움(14sp)!
                tvArtist.setTextColor(ThemeManager.getTextColorPrimary() & 0xAAFFFFFF);
                tvArtist.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
                tvArtist.setSingleLine(true);
                tvArtist.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
                tvArtist.setMarqueeRepeatLimit(-1);

                textContainer.addView(tvTitle);
                textContainer.addView(tvArtist);

                rowView.addView(ivCover);
                rowView.addView(textContainer);

                holder = new AlbumViewHolder(rowView, ivCover, tvTitle, tvArtist);
                rowView.setTag(holder);
            } else {
                rowView = (LinearLayout) convertView;
                holder = (AlbumViewHolder) rowView.getTag();
            }

            holder.tvTitle.setText(name);

            // 🚀 [추가] 앨범 아티스트 이름 조회 및 하단 표시!
            String artistName = "";
            for (SongItem song : MainActivity.customLibrary) {
                if (song.album != null && song.album.equals(name)) {
                    if (song.artist != null && !song.artist.isEmpty()) {
                        artistName = song.artist;
                        break;
                    }
                }
            }
            if (artistName.isEmpty()) {
                artistName = "Unknown Artist";
            }
            holder.tvArtist.setText(artistName);

            // 1. 메모리 금고에 이미 불러온 그림이 있는지 확인!
            Drawable leftDrawable = coverCache.get(name);

            // 2. 금고에 그림이 없다면? 직접 찾아서 그립니다.
            if (leftDrawable == null) {
                String artPath = "";
                byte[] embeddedPic = null;

                for (SongItem song : MainActivity.customLibrary) {
                    if (song.album != null && song.album.equals(name)) {
                        String trackPath = song.file.getAbsolutePath();

                        if (MainActivity.instance.prefs != null) {
                            String savedPath = MainActivity.instance.prefs.getString("album_art_" + trackPath, "");
                            if (!savedPath.isEmpty() && new File(savedPath).exists()) {
                                artPath = savedPath;
                                break;
                            }
                        }

                        String safeFileName = song.file.getName().replace(".mp3", "").replace(".flac", "").replace(".wav", "").replace(".m4a", "").replace(".aac", "").replace(".ogg", "");
                        File manualCoverFile = new File(StoragePaths.getCoversDir(), safeFileName + ".jpg");
                        if (manualCoverFile.exists()) {
                            artPath = manualCoverFile.getAbsolutePath();
                            break;
                        }

                        if (embeddedPic == null) {
                            if (trackPath.toLowerCase().endsWith(".opus")) {
                                try {
                                    Object[] opusTags = com.themoon.y1.managers.AudioPlayerManager.getInstance().extractOpusMetadata(new File(trackPath));
                                    if (opusTags != null && opusTags.length > 5 && opusTags[5] != null) {
                                        embeddedPic = (byte[]) opusTags[5];
                                    }
                                } catch (Exception e) {}
                            } else if (trackPath.toLowerCase().endsWith(".flac")) {
                                try {
                                    Object[] flacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance().extractFlacMetadata(new File(trackPath));
                                    if (flacTags != null && flacTags.length > 5 && flacTags[5] != null) {
                                        embeddedPic = (byte[]) flacTags[5];
                                    }
                                } catch (Exception e) {}
                            } else {
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

                int size = (int) (60 * d); // 🚀 60dp 썸네일 크기
                Bitmap bmp = null;

                if (!artPath.isEmpty()) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 2;
                        bmp = BitmapFactory.decodeFile(artPath, opts);
                    } catch (Throwable e) {}
                } else if (embeddedPic != null) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 2;
                        bmp = BitmapFactory.decodeByteArray(embeddedPic, 0, embeddedPic.length, opts);
                    } catch (Throwable e) {}
                }

                if (bmp == null) {
                    bmp = BitmapFactory.decodeResource(MainActivity.instance.getResources(), R.drawable.default_album);
                }

                if (bmp != null) {
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, size, size, true);
                    leftDrawable = new BitmapDrawable(MainActivity.instance.getResources(), scaled);
                    leftDrawable.setBounds(0, 0, size, size);
                    coverCache.put(name, leftDrawable);
                }
            }

            holder.ivCover.setImageDrawable(leftDrawable);

            rowView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        rowView.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                        int focusedColor = ThemeManager.getListButtonFocusedTextColor();
                        if (focusedColor == 0) focusedColor = 0xFF000000;
                        holder.tvTitle.setTextColor(focusedColor);
                        holder.tvArtist.setTextColor(focusedColor & 0xCCFFFFFF);
                        holder.tvTitle.setSelected(true);
                        holder.tvArtist.setSelected(true);
                        MainActivity.instance.showFastScrollLetter(name);
                    } else {
                        rowView.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                        int normalColor = ThemeManager.getTextColorPrimary();
                        holder.tvTitle.setTextColor(normalColor);
                        holder.tvArtist.setTextColor(normalColor & 0xAAFFFFFF);
                        holder.tvTitle.setSelected(false);
                        holder.tvArtist.setSelected(false);
                    }
                }
            });

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.instance.clickFeedback();
                    MainActivity.instance.virtualQueryType = type;
                    MainActivity.instance.virtualQueryValue = name;
                    MainActivity.instance.currentBrowserMode = MainActivity.BROWSER_VIRTUAL_SONGS;
                    MainActivity.instance.buildVirtualSongs();
                }
            });

            return rowView;
        } else {
            final Button btn;
            if (convertView == null) {
                btn = MainActivity.instance.createListButton("");
                btn.setLayoutParams(new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        AbsListView.LayoutParams.WRAP_CONTENT));
            } else {
                btn = (Button) convertView;
            }

            btn.setText("👤 " + name);
            btn.setCompoundDrawables(null, null, null, null);

            btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        btn.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                        btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                        MainActivity.instance.showFastScrollLetter(name);
                        btn.setSelected(true);
                    } else {
                        btn.setBackground(MainActivity.instance.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                        btn.setTextColor(ThemeManager.getTextColorPrimary());
                        btn.setSelected(false);
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
}