package com.themoon.y1.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.themoon.y1.StoragePaths;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Y2 MicroSD watchdog: detects when {@code /storage/sdcard1} is not usable and
 * attempts to remount via {@code vdc}/{@code fuse_sdcard1} (requires root, which
 * JJ already uses elsewhere on these devices).
 * <p>
 * Safe on Y1: if the secondary path does not exist, the monitor is a no-op.
 * Designed to be upstreamable to ismileblue/y1_launcher.
 */
public final class ExternalSdMountMonitor {
    private static final String TAG = "Y1SdMount";
    private static final String SECONDARY = StoragePaths.SECONDARY_PATH;
    private static final long POLL_MS = 12_000L;
    private static final long MOUNT_COOLDOWN_MS = 45_000L;
    private static final int CMD_TIMEOUT_MS = 8_000;

    public interface Listener {
        /** Called on a background thread after a successful remount. */
        void onSecondaryStorageReady();
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean mountInFlight = new AtomicBoolean(false);
    private Listener listener;
    private BroadcastReceiver mediaReceiver;
    private long lastMountAttemptMs;
    private boolean lastReady;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!running.get())
                return;
            checkAndMaybeMount("poll");
            mainHandler.postDelayed(this, POLL_MS);
        }
    };

    public ExternalSdMountMonitor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (!hasSecondarySlot()) {
            Log.i(TAG, "no /storage/sdcard1 slot — monitor idle (Y1-compatible)");
            return;
        }
        if (!running.compareAndSet(false, true))
            return;

        registerMediaReceiver();
        mainHandler.post(pollTask);
        // Immediate first pass after boot / resume.
        new Thread(new Runnable() {
            @Override
            public void run() {
                checkAndMaybeMount("start");
            }
        }, "y1-sd-mount-start").start();
    }

    public void stop() {
        running.set(false);
        mainHandler.removeCallbacks(pollTask);
        unregisterMediaReceiver();
    }

    /** True when apps can list the secondary volume (FUSE up). */
    public static boolean isSecondaryReady() {
        if (!hasSecondarySlot())
            return false;
        String fuse = readProp("init.svc.fuse_sdcard1");
        if (!"running".equals(fuse))
            return false;
        return mountsContain(SECONDARY) || mountsContain("/mnt/media_rw/sdcard1");
    }

    public static boolean hasSecondarySlot() {
        try {
            File stub = new File(SECONDARY);
            return stub.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private void checkAndMaybeMount(String reason) {
        boolean ready = isSecondaryReady();
        if (ready) {
            if (!lastReady) {
                Log.i(TAG, "secondary ready (" + reason + ")");
                StoragePaths.invalidate();
                notifyReady();
            }
            lastReady = true;
            return;
        }
        lastReady = false;

        long now = System.currentTimeMillis();
        if (now - lastMountAttemptMs < MOUNT_COOLDOWN_MS)
            return;
        if (!mountInFlight.compareAndSet(false, true))
            return;
        lastMountAttemptMs = now;

        try {
            Log.i(TAG, "secondary not ready (" + reason + ") — attempting remount");
            boolean ok = attemptRemount();
            if (ok && isSecondaryReady()) {
                Log.i(TAG, "remount succeeded");
                StoragePaths.invalidate();
                lastReady = true;
                notifyReady();
            } else {
                Log.w(TAG, "remount did not yield a usable /storage/sdcard1");
            }
        } finally {
            mountInFlight.set(false);
        }
    }

    /**
     * Clear a wedged exFAT/FUSE stack then ask vold to mount and start fuse_sdcard1.
     * All commands run under {@code su} with a hard timeout so the UI cannot hang.
     */
    private boolean attemptRemount() {
        // Kill stuck mount.exfat / zombie fuse before asking vold again.
        runSuTimed(
                "killall -9 mount.exfat 2>/dev/null; "
                        + "stop fuse_sdcard1 2>/dev/null; "
                        + "sleep 1; "
                        + "vdc volume mount sdcard1; "
                        + "start fuse_sdcard1; "
                        + "sleep 2; "
                        + "getprop init.svc.fuse_sdcard1");
        return isSecondaryReady();
    }

    private void notifyReady() {
        final Listener l = listener;
        if (l == null)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    l.onSecondaryStorageReady();
                } catch (Exception e) {
                    Log.w(TAG, "listener failed", e);
                }
            }
        }, "y1-sd-ready").start();
    }

    private void registerMediaReceiver() {
        if (mediaReceiver != null)
            return;
        mediaReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null)
                    return;
                String action = intent.getAction();
                if (Intent.ACTION_MEDIA_MOUNTED.equals(action)
                        || Intent.ACTION_MEDIA_UNMOUNTED.equals(action)
                        || Intent.ACTION_MEDIA_REMOVED.equals(action)
                        || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)
                        || Intent.ACTION_MEDIA_EJECT.equals(action)
                        || "android.intent.action.MEDIA_CHECKING".equals(action)) {
                    StoragePaths.invalidate();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            checkAndMaybeMount("broadcast:" + action);
                        }
                    }, "y1-sd-bcast").start();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction("android.intent.action.MEDIA_CHECKING");
        filter.addDataScheme("file");
        try {
            appContext.registerReceiver(mediaReceiver, filter);
        } catch (Exception e) {
            Log.w(TAG, "registerReceiver failed", e);
        }
    }

    private void unregisterMediaReceiver() {
        if (mediaReceiver == null)
            return;
        try {
            appContext.unregisterReceiver(mediaReceiver);
        } catch (Exception ignored) {
        }
        mediaReceiver = null;
    }

    private static boolean mountsContain(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            while ((line = br.readLine()) != null) {
                // mountpoint is field 2
                String[] parts = line.split(" ");
                if (parts.length > 1 && parts[1].equals(path))
                    return true;
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static String readProp(String key) {
        Process p = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(new String[] { "getprop", key });
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            return line != null ? line.trim() : "";
        } catch (Exception e) {
            return "";
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception ignored) {
            }
            if (p != null)
                p.destroy();
        }
    }

    private static String runSuTimed(String cmd) {
        Process p = null;
        final StringBuilder out = new StringBuilder();
        try {
            p = Runtime.getRuntime().exec(new String[] { "su", "-c", cmd });
            final Process proc = p;
            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (out.length() > 0)
                                out.append('\n');
                            out.append(line);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        try {
                            if (br != null)
                                br.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }, "y1-sd-su-out");
            reader.start();

            long deadline = System.currentTimeMillis() + CMD_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {
                try {
                    proc.exitValue();
                    break;
                } catch (IllegalThreadStateException stillRunning) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            try {
                proc.exitValue();
            } catch (IllegalThreadStateException stillRunning) {
                proc.destroy();
                Log.w(TAG, "su command timed out");
            }
            try {
                reader.join(500);
            } catch (InterruptedException ignored) {
            }
        } catch (Exception e) {
            Log.w(TAG, "su failed: " + e.getMessage());
        } finally {
            if (p != null)
                p.destroy();
        }
        return out.toString();
    }
}
