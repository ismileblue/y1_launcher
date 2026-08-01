
import sys

filepath = r"c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add coverFlowExecutor
target_executor = """    private java.util.concurrent.ExecutorService thumbnailExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();"""
replacement_executor = """    private java.util.concurrent.ExecutorService thumbnailExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

    // 앨범 아트 로딩용 스레드 풀 (메모리 폭발 방지 및 동시 로딩 제한)
    private java.util.concurrent.ExecutorService coverFlowExecutor = java.util.concurrent.Executors.newFixedThreadPool(3);

    // OOM 방지용 이미지 리사이징 헬퍼
    private Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = 1;
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / options.inSampleSize) >= reqHeight && (halfWidth / options.inSampleSize) >= reqWidth) {
                    options.inSampleSize *= 2;
                }
            }
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);
        } catch (OutOfMemoryError e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }"""

if target_executor in content:
    content = content.replace(target_executor, replacement_executor)
else:
    print("Failed to inject executor")
    sys.exit(1)


# 2. Update bindCoverData thread logic
target_thread = """        // 3. 백그라운드 로딩 엔진 발사
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                String cachedArtPath = prefs.getString("album_art_" + path, null);

                if (cachedArtPath != null && new File(cachedArtPath).exists()) {
                    bmp = BitmapFactory.decodeFile(cachedArtPath);
                } else {
                    try {
                        String songName = item.file.getName();
                        int dot = songName.lastIndexOf(".");
                        if (dot > 0)
                            songName = songName.substring(0, dot);

                        // 1순위: Y1_Covers 전용 폴더 검색
                        File fallbackFile = new File("/storage/sdcard0/Y1_Covers", songName + ".jpg");
                        if (fallbackFile.exists()) {
                            bmp = BitmapFactory.decodeFile(fallbackFile.getAbsolutePath());
                        } else {
                            // 💡 [신규 도착!] 2순위: 혹시 같은 폴더 안에 cover.jpg 가 있는지 검색해볼까?
                            File folderCover = findFolderCover(item.file.getParentFile());
                            if (folderCover != null) {
                                bmp = BitmapFactory.decodeFile(folderCover.getAbsolutePath());
                            }
                        }
                    } catch (Exception e) {
                    }
                }"""

replacement_thread = """        // 3. 백그라운드 로딩 엔진 발사
        coverFlowExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                String cachedArtPath = prefs.getString("album_art_" + path, null);

                try {
                    if (cachedArtPath != null && new File(cachedArtPath).exists()) {
                        bmp = decodeSampledBitmap(cachedArtPath, 400, 400);
                    } else {
                        String songName = item.file.getName();
                        int dot = songName.lastIndexOf(".");
                        if (dot > 0)
                            songName = songName.substring(0, dot);

                        // 1순위: Y1_Covers 전용 폴더 검색
                        File fallbackFile = new File("/storage/sdcard0/Y1_Covers", songName + ".jpg");
                        if (fallbackFile.exists()) {
                            bmp = decodeSampledBitmap(fallbackFile.getAbsolutePath(), 400, 400);
                        } else {
                            // 💡 [신규 도착!] 2순위: 혹시 같은 폴더 안에 cover.jpg 가 있는지 검색해볼까?
                            File folderCover = findFolderCover(item.file.getParentFile());
                            if (folderCover != null) {
                                bmp = decodeSampledBitmap(folderCover.getAbsolutePath(), 400, 400);
                            }
                        }
                    }
                } catch (OutOfMemoryError e) {
                    bmp = null;
                } catch (Exception e) {
                    bmp = null;
                }"""

if target_thread in content:
    content = content.replace(target_thread, replacement_thread)
else:
    print("Failed to inject thread logic")
    sys.exit(1)


# 3. Add OutOfMemoryError catch for embedded byte array decode and reflection bitmap
target_embedded = """                        // 🎨 빼온 사진 데이터(Byte)를 예쁜 비트맵(Bitmap)으로 구워냅니다.
                        if (embeddedArt != null) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = 2;
                            bmp = BitmapFactory.decodeByteArray(embeddedArt, 0, embeddedArt.length, opts);
                        }
                    } catch (Exception e) {
                    }
                }

                final Bitmap finalBmp = bmp;

                // 메인 스레드가 아닌, 이 백그라운드 공간에서 반사영을 생성하므로 성능 과부하 0%입니다!
                final Bitmap finalRef = getReflectionBitmap(finalBmp);"""

replacement_embedded = """                        // 🎨 빼온 사진 데이터(Byte)를 예쁜 비트맵(Bitmap)으로 구워냅니다.
                        if (embeddedArt != null) {
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(embeddedArt, 0, embeddedArt.length, opts);
                            opts.inSampleSize = 1;
                            if (opts.outHeight > 400 || opts.outWidth > 400) {
                                final int halfHeight = opts.outHeight / 2;
                                final int halfWidth = opts.outWidth / 2;
                                while ((halfHeight / opts.inSampleSize) >= 400 && (halfWidth / opts.inSampleSize) >= 400) {
                                    opts.inSampleSize *= 2;
                                }
                            }
                            opts.inJustDecodeBounds = false;
                            bmp = BitmapFactory.decodeByteArray(embeddedArt, 0, embeddedArt.length, opts);
                        }
                    } catch (OutOfMemoryError e) {
                        bmp = null;
                    } catch (Exception e) {
                        bmp = null;
                    }
                }

                final Bitmap finalBmp = bmp;

                // 메인 스레드가 아닌, 이 백그라운드 공간에서 반사영을 생성하므로 성능 과부하 0%입니다!
                Bitmap tempRef = null;
                try {
                    tempRef = getReflectionBitmap(finalBmp);
                } catch (OutOfMemoryError e) {
                    tempRef = null;
                }
                final Bitmap finalRef = tempRef;"""

if target_embedded in content:
    content = content.replace(target_embedded, replacement_embedded)
else:
    print("Failed to inject embedded art logic")
    sys.exit(1)


with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
print("Patch applied successfully.")
