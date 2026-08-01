package com.themoon.y1;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Resolves Y1/Y2 storage roots while keeping Y1 paths as the default.
 * <ul>
 *   <li>Y1 / primary: {@code /storage/sdcard0} — browser defaults, app data, web server</li>
 *   <li>Y2 microSD: {@code /storage/sdcard1} — scanned when readable; never replaces Y1 defaults</li>
 * </ul>
 */
public final class StoragePaths {

    public static final String PRIMARY_PATH = "/storage/sdcard0";
    public static final String SECONDARY_PATH = "/storage/sdcard1";

    private static volatile File cachedPrimary;
    private static volatile List<File> cachedRoots;
    private static volatile Boolean cachedSecondaryAvailable;

    private StoragePaths() {
    }

    /** Primary writable root — same volume Y1 always used. */
    public static File getPrimaryRoot() {
        if (cachedPrimary != null)
            return cachedPrimary;
        File primary = firstUsableRoot(PRIMARY_PATH, "/storage/emulated/legacy", "/sdcard");
        cachedPrimary = primary != null ? primary : new File(PRIMARY_PATH);
        return cachedPrimary;
    }

    /**
     * True when {@code /storage/sdcard1} exists. Does not call {@code list()}
     * (that can hang on a wedged FUSE mount). Y1 devices without this path are
     * unaffected; scan still no-ops on missing Music/Audiobooks folders.
     */
    public static boolean hasSecondaryStorage() {
        if (cachedSecondaryAvailable != null)
            return cachedSecondaryAvailable;
        // Prefer a usable FUSE mount; fall back to the slot stub so scans still
        // probe Music/ once remount succeeds after invalidate().
        boolean available = false;
        try {
            if (com.themoon.y1.managers.ExternalSdMountMonitor.isSecondaryReady())
                available = true;
            else
                available = new File(SECONDARY_PATH).exists();
        } catch (Exception ignored) {
        }
        cachedSecondaryAvailable = available;
        return available;
    }

    /**
     * Primary always; secondary only when {@link #hasSecondaryStorage()} is true.
     * Y1 without a usable SD therefore behaves exactly as before.
     */
    public static List<File> getAllRoots() {
        if (cachedRoots != null)
            return cachedRoots;

        LinkedHashSet<String> paths = new LinkedHashSet<>();
        paths.add(getPrimaryRoot().getAbsolutePath());
        if (!PRIMARY_PATH.equals(getPrimaryRoot().getAbsolutePath()))
            paths.add(PRIMARY_PATH);

        if (hasSecondaryStorage()) {
            paths.add(SECONDARY_PATH);
            String secondaryEnv = System.getenv("SECONDARY_STORAGE");
            if (secondaryEnv != null && !secondaryEnv.isEmpty()) {
                for (String part : secondaryEnv.split(":")) {
                    if (part != null && !part.trim().isEmpty())
                        paths.add(part.trim());
                }
            }
        }

        List<File> roots = new ArrayList<>();
        for (String path : paths) {
            File f = new File(path);
            if (!containsSameRoot(roots, f))
                roots.add(f);
        }
        if (roots.isEmpty())
            roots.add(getPrimaryRoot());

        cachedRoots = roots;
        return cachedRoots;
    }

    /** Force re-probe after SD insert/eject. */
    public static void invalidate() {
        cachedPrimary = null;
        cachedRoots = null;
        cachedSecondaryAvailable = null;
    }

    /** Folder-browser / mkdir default — always primary (Y1-compatible). */
    public static File getMusicDir() {
        return primaryMediaDir("Music");
    }

    public static File getAudiobooksDir() {
        return primaryMediaDir("Audiobooks");
    }

    public static File getVideosDir() {
        return primaryMediaDir("Videos");
    }

    /** All Music folders to scan (primary + readable secondary). */
    public static List<File> getMusicDirs() {
        List<File> dirs = mediaDirs("Music");
        // Y2 stock layout keeps playable audio in custom_media.
        for (File root : getAllRoots()) {
            File custom = new File(root, "custom_media");
            try {
                if (custom.exists() && custom.isDirectory())
                    addIfMissing(dirs, custom);
            } catch (Exception ignored) {
            }
        }
        return dirs;
    }

    public static List<File> getAudiobooksDirs() {
        return mediaDirs("Audiobooks");
    }

    public static File getPodcastsDir() {
        return new File(getPrimaryRoot(), "Podcasts");
    }

    public static File getPodcastChannelDir(String safeChannel) {
        return new File(getPodcastsDir(), safeChannel);
    }

    public static File getCoversDir() {
        return ensureDir(new File(getPrimaryRoot(), "Y1_Covers"));
    }

    public static File getThemesDir() {
        return ensureDir(new File(getPrimaryRoot(), "Y1_Themes"));
    }

    public static File getLanguagesDir() {
        return ensureDir(new File(getPrimaryRoot(), "Y1_Languages"));
    }

    public static File getPlaylistsDir() {
        return ensureDir(new File(getPrimaryRoot(), "Y1_Playlists"));
    }

    public static File getEqDir() {
        return ensureDir(new File(getPrimaryRoot(), "Y1_EQs"));
    }

    public static File primaryFile(String relativePath) {
        return new File(getPrimaryRoot(), relativePath);
    }

    /**
     * Web upload root stays on primary so Y1 URL paths are unchanged.
     * Secondary media is still reachable after upload via the library scan.
     */
    public static File getWebServerRoot() {
        return getPrimaryRoot();
    }

    public static boolean isStorageVolumeName(String name) {
        if (name == null)
            return false;
        String n = name.toLowerCase(Locale.US);
        return n.equals("sdcard0") || n.equals("sdcard1") || n.equals("emulated")
                || n.equals("legacy") || n.equals("media_rw");
    }

    public static boolean isAnyStorageRoot(String absolutePath) {
        if (absolutePath == null)
            return false;
        if (absolutePath.equals(PRIMARY_PATH) || absolutePath.equals(SECONDARY_PATH))
            return true;
        for (File root : getAllRoots()) {
            if (absolutePath.equals(root.getAbsolutePath()))
                return true;
        }
        return false;
    }

    public static boolean isVideosRoot(String absolutePath) {
        if (absolutePath == null)
            return false;
        if (absolutePath.equals(PRIMARY_PATH + "/Videos")
                || absolutePath.equals(SECONDARY_PATH + "/Videos"))
            return true;
        for (File root : getAllRoots()) {
            if (absolutePath.equals(new File(root, "Videos").getAbsolutePath()))
                return true;
        }
        return false;
    }

    public static boolean isAudiobooksRoot(String absolutePath) {
        if (absolutePath == null)
            return false;
        if (absolutePath.equals(PRIMARY_PATH + "/Audiobooks")
                || absolutePath.equals(SECONDARY_PATH + "/Audiobooks"))
            return true;
        for (File dir : getAudiobooksDirs()) {
            if (absolutePath.equals(dir.getAbsolutePath()))
                return true;
        }
        return false;
    }

    public static boolean isUnderAudiobooks(String absolutePath) {
        if (absolutePath == null)
            return false;
        if (absolutePath.startsWith(PRIMARY_PATH + "/Audiobooks")
                || absolutePath.startsWith(SECONDARY_PATH + "/Audiobooks"))
            return true;
        for (File dir : getAudiobooksDirs()) {
            String root = dir.getAbsolutePath();
            if (absolutePath.equals(root) || absolutePath.startsWith(root + "/"))
                return true;
        }
        return false;
    }

    /** Strip known volume prefixes so the browser path stays short. */
    public static String toDisplayPath(String absolutePath) {
        if (absolutePath == null)
            return "";
        String path = absolutePath;
        // Prefer stripping primary first so Y1 display matches historical paths.
        for (String prefix : new String[] {
                getPrimaryRoot().getAbsolutePath(), PRIMARY_PATH, SECONDARY_PATH,
                "/storage/emulated/legacy"
        }) {
            if (path.equals(prefix))
                return "/";
            if (path.startsWith(prefix + "/"))
                return path.substring(prefix.length());
        }
        for (File root : getAllRoots()) {
            String prefix = root.getAbsolutePath();
            if (path.equals(prefix))
                return "/";
            if (path.startsWith(prefix + "/"))
                return path.substring(prefix.length());
        }
        return path;
    }

    private static File primaryMediaDir(String name) {
        File primary = new File(getPrimaryRoot(), name);
        if (!primary.exists())
            primary.mkdirs();
        return primary;
    }

    private static List<File> mediaDirs(String name) {
        List<File> dirs = new ArrayList<>();
        addIfMissing(dirs, new File(getPrimaryRoot(), name));
        addIfMissing(dirs, new File(PRIMARY_PATH, name));
        if (hasSecondaryStorage())
            addIfMissing(dirs, new File(SECONDARY_PATH, name));
        return dirs;
    }

    private static void addIfMissing(List<File> dirs, File dir) {
        if (!containsSameRoot(dirs, dir))
            dirs.add(dir);
    }

    private static File firstUsableRoot(String... candidates) {
        for (String path : candidates) {
            File f = new File(path);
            if (isUsableRoot(f))
                return f;
        }
        return null;
    }

    private static boolean isUsableRoot(File f) {
        if (f == null)
            return false;
        try {
            return f.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean containsSameRoot(List<File> roots, File candidate) {
        String path = candidate.getAbsolutePath();
        for (File existing : roots) {
            if (existing.getAbsolutePath().equals(path))
                return true;
        }
        return false;
    }

    private static File ensureDir(File dir) {
        try {
            if (!dir.exists())
                dir.mkdirs();
        } catch (Exception ignored) {
        }
        return dir;
    }
}
