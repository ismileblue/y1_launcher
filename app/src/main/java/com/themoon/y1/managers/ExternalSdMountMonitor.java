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
    private static final String PRIMARY = StoragePaths.PRIMARY_PATH;
    private static final String SECONDARY = StoragePaths.SECONDARY_PATH;
    private static final long POLL_MS = 12_000L;
    private static final long MOUNT_COOLDOWN_MS = 30_000L;
    private static final int CMD_TIMEOUT_MS = 8_000;

    public interface Listener {
        /** Called on a background thread after a successful remount. */
        void onSecondaryStorageReady();
    }

    private final Context appContext;
    private android.os.HandlerThread bgThread;
    private Handler bgHandler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean mountInFlight = new AtomicBoolean(false);
    private Listener listener;
    private BroadcastReceiver mediaReceiver;
    private BroadcastReceiver usbReceiver;
    private long lastMountAttemptMs;
    private boolean lastPriReady = false;
    private boolean lastSecReady = false;

    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (!running.get())
                return;
            checkAndMaybeMount("poll");
            if (bgHandler != null && running.get()) {
                bgHandler.postDelayed(this, POLL_MS);
            }
        }
    };

    public ExternalSdMountMonitor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start() {
        if (!running.compareAndSet(false, true))
            return;

        if (bgThread == null || !bgThread.isAlive()) {
            bgThread = new android.os.HandlerThread("y1-sd-monitor-bg");
            bgThread.start();
            bgHandler = new Handler(bgThread.getLooper());
        }

        registerMediaReceiver();
        registerUsbReceiver();
        if (bgHandler != null) {
            bgHandler.post(pollTask);
            bgHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkAndMaybeMount("start");
                }
            });
        }
    }

    public void stop() {
        running.set(false);
        if (bgHandler != null) {
            bgHandler.removeCallbacks(pollTask);
            bgHandler = null;
        }
        if (bgThread != null) {
            bgThread.quit();
            bgThread = null;
        }
        unregisterMediaReceiver();
        unregisterUsbReceiver();
    }

    /** True when primary storage (/storage/sdcard0) is mounted. */
    public static boolean isPrimaryReady() {
        return mountsContain(PRIMARY) || mountsContain("/mnt/media_rw/sdcard0");
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
        boolean priReady = isPrimaryReady();
        boolean secReady = isSecondaryReady();

        boolean newlyMounted = (priReady && !lastPriReady) || (secReady && !lastSecReady);

        lastPriReady = priReady;
        lastSecReady = secReady;

        if (newlyMounted) {
            Log.i(TAG, "Storage volume newly mounted (pri=" + priReady + ", sec=" + secReady + ", reason=" + reason + ")");
            StoragePaths.invalidate();
            notifyReady();
        }

        // 🚀 [독립 검사] 내장 메모리(sdcard0)와 외장 슬롯(sdcard1)이 모두 정상이면 추가 작업 불필요
        boolean priNeeded = !priReady;
        boolean secNeeded = hasSecondarySlot() && !secReady;

        if (!priNeeded && !secNeeded) {
            return;
        }

        // 브로드캐스트 이벤트(배드 리무벌, USB 해제 등)인 경우 쿨다운을 무시하고 즉시 자가 치유 시도
        boolean isBroadcastEvent = reason.startsWith("broadcast:") || reason.startsWith("usb_disconnect:");
        long now = System.currentTimeMillis();
        if (!isBroadcastEvent && (now - lastMountAttemptMs < MOUNT_COOLDOWN_MS))
            return;
        if (!mountInFlight.compareAndSet(false, true))
            return;
        lastMountAttemptMs = now;

        try {
            Log.i(TAG, "Storage unmounted or corrupted (pri=" + priReady + ", sec=" + secReady + ", " + reason + ") — running self-healing engine");
            boolean ok = attemptRemount();
            boolean afterPri = isPrimaryReady();
            boolean afterSec = isSecondaryReady();
            if ((afterPri && !priReady) || (afterSec && !secReady)) {
                Log.i(TAG, "Self-healing remount succeeded (pri=" + afterPri + ", sec=" + afterSec + ")");
                StoragePaths.invalidate();
                lastPriReady = afterPri;
                lastSecReady = afterSec;
                notifyReady();
            } else {
                Log.w(TAG, "Self-healing remount completed (pri=" + afterPri + ", sec=" + afterSec + ")");
            }
        } finally {
            mountInFlight.set(false);
        }
    }

    /**
     * 🚀 [자가 치유 엔진]
     * 비정상 USB 분리 등으로 dirty bit가 걸리거나 vold가 꼬였을 때,
     * 파일시스템 점검 툴(dosfsck/fsck_msdos/fsck.vfat/fsck.exfat)로 손상을 복구하고 안전하게 재마운트합니다.
     */
    private boolean attemptRemount() {
        // 1. 내장 메모리 (/storage/sdcard0) 자가 치유 및 재마운트
        if (!isPrimaryReady()) {
            Log.i(TAG, "Attempting self-healing repair for primary storage (sdcard0)...");
            // 1-A: 꼬인 볼륨 핸들 강제 언마운트
            runSuTimed("vdc volume unmount sdcard0 force 2>/dev/null");

            // 1-B: 파일시스템 자동 복구 (더티 비트 해제 및 고아 클러스터 수리)
            runSuTimed(
                    "dosfsck -a -w /dev/block/mmcblk0p1 2>/dev/null; "
                    + "fsck_msdos -y /dev/block/mmcblk0p1 2>/dev/null; "
                    + "fsck.vfat -a -w /dev/block/mmcblk0p1 2>/dev/null; "
                    + "fsck.exfat -y /dev/block/mmcblk0p1 2>/dev/null; "
                    + "dosfsck -a -w /dev/block/mmcblk1p1 2>/dev/null; "
                    + "fsck_msdos -y /dev/block/mmcblk1p1 2>/dev/null; "
                    + "fsck.vfat -a -w /dev/block/mmcblk1p1 2>/dev/null; "
                    + "fsck.exfat -y /dev/block/mmcblk1p1 2>/dev/null"
            );

            // 1-C: vold에 마운트 요청
            runSuTimed("vdc volume mount sdcard0 2>/dev/null");

            // 1-D: vold로 마운트되지 않은 경우 안드로이드 표준 권한(uid=1000/gid=1015)으로 커널 direct mount 시도
            if (!isPrimaryReady()) {
                runSuTimed(
                        "mkdir -p /storage/sdcard0 2>/dev/null; "
                        + "mount -t vfat -o rw,nosuid,nodev,noexec,uid=1000,gid=1015,fmask=0702,dmask=0702,shortname=mixed,utf8 /dev/block/mmcblk0p1 /storage/sdcard0 2>/dev/null || "
                        + "mount -t exfat -o rw,nosuid,nodev,noexec,uid=1000,gid=1015,fmask=0702,dmask=0702 /dev/block/mmcblk0p1 /storage/sdcard0 2>/dev/null || "
                        + "mount -t vfat -o rw /dev/block/mmcblk0p1 /storage/sdcard0 2>/dev/null || "
                        + "mount -t exfat -o rw /dev/block/mmcblk0p1 /storage/sdcard0 2>/dev/null || "
                        + "mount -t vfat -o rw,nosuid,nodev,noexec,uid=1000,gid=1015,fmask=0702,dmask=0702,shortname=mixed,utf8 /dev/block/mmcblk1p1 /storage/sdcard0 2>/dev/null || "
                        + "mount -t vfat -o rw /dev/block/mmcblk1p1 /storage/sdcard0 2>/dev/null"
                );
            }
        }

        // 2. 외장 SD카드 (/storage/sdcard1) 자가 치유 및 FUSE 재가동
        if (hasSecondarySlot() && !isSecondaryReady()) {
            Log.i(TAG, "Attempting self-healing repair for secondary storage (sdcard1)...");
            // 2-A: 멈춘 FUSE 및 마운트 프로세스 정리
            runSuTimed(
                    "killall -9 mount.exfat 2>/dev/null; "
                    + "stop fuse_sdcard1 2>/dev/null; "
                    + "vdc volume unmount sdcard1 force 2>/dev/null"
            );

            // 2-B: 외장 SD카드 파일시스템 복구
            runSuTimed(
                    "dosfsck -a -w /dev/block/mmcblk1p1 2>/dev/null; "
                    + "fsck_msdos -y /dev/block/mmcblk1p1 2>/dev/null; "
                    + "fsck.vfat -a -w /dev/block/mmcblk1p1 2>/dev/null; "
                    + "fsck.exfat -y /dev/block/mmcblk1p1 2>/dev/null"
            );

            // 2-C: vold 마운트 및 FUSE 재시작
            runSuTimed(
                    "vdc volume mount sdcard1 2>/dev/null; "
                    + "start fuse_sdcard1 2>/dev/null; "
                    + "sleep 1; "
                    + "getprop init.svc.fuse_sdcard1"
            );
        }

        return isPrimaryReady() || isSecondaryReady();
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
                        || Intent.ACTION_MEDIA_NOFS.equals(action)
                        || Intent.ACTION_MEDIA_UNMOUNTABLE.equals(action)
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
        filter.addAction(Intent.ACTION_MEDIA_NOFS);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        filter.addAction("android.intent.action.MEDIA_CHECKING");
        filter.addDataScheme("file");
        try {
            appContext.registerReceiver(mediaReceiver, filter);
        } catch (Exception e) {
            Log.w(TAG, "registerMediaReceiver failed", e);
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

    private void registerUsbReceiver() {
        if (usbReceiver != null)
            return;
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null)
                    return;
                String action = intent.getAction();
                boolean trigger = false;
                if ("android.hardware.usb.action.USB_STATE".equals(action)) {
                    boolean connected = intent.getBooleanExtra("connected", false);
                    if (!connected) {
                        trigger = true; // USB 케이블 분리 감지!
                    }
                } else if ("android.intent.action.UMS_DISCONNECTED".equals(action)) {
                    trigger = true;
                }

                if (trigger) {
                    Log.i(TAG, "USB disconnected event (" + action + ") — scheduling self-healing check in 500ms");
                    if (bgHandler != null) {
                        bgHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkAndMaybeMount("usb_disconnect:" + action);
                            }
                        }, 500);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        filter.addAction("android.intent.action.UMS_DISCONNECTED");
        try {
            appContext.registerReceiver(usbReceiver, filter);
        } catch (Exception e) {
            Log.w(TAG, "registerUsbReceiver failed", e);
        }
    }

    private void unregisterUsbReceiver() {
        if (usbReceiver == null)
            return;
        try {
            appContext.unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {
        }
        usbReceiver = null;
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
