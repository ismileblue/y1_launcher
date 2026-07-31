package com.themoon.y1.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class BatteryStatsManager {
    private static BatteryStatsManager instance;
    private SharedPreferences prefs;

    public static final int MODE_STANDBY = 0;
    public static final int MODE_MUSIC = 1;
    public static final int MODE_VIDEO = 2;
    public static final int MODE_RADIO = 3;
    public static final int MODE_GAME = 4;
    public static final int MODE_OTHER = 5;

    private int currentMode = MODE_STANDBY;
    private long modeStartTime = 0;
    private int lastBatteryLevel = -1;

    private BatteryStatsManager(Context context) {
        prefs = context.getSharedPreferences("BatteryStats", Context.MODE_PRIVATE);
    }

    public static BatteryStatsManager getInstance(Context context) {
        if (instance == null) {
            instance = new BatteryStatsManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setMode(int mode) {
        if (currentMode == mode) return;
        
        long now = System.currentTimeMillis();
        if (modeStartTime > 0) {
            long elapsed = now - modeStartTime;
            addTime(currentMode, elapsed);
        }
        currentMode = mode;
        modeStartTime = now;
    }

    public void onBatteryLevelChanged(int level) {
        if (lastBatteryLevel == -1) {
            lastBatteryLevel = level;
            return;
        }

        if (level < lastBatteryLevel) {
            int drop = lastBatteryLevel - level;
            addDrop(currentMode, drop);
        }
        lastBatteryLevel = level;
    }
    
    public void onPowerDisconnected(int level) {
        // When unplugged at high level, reset stats
        if (level >= 90) {
            resetStats();
        }
        lastBatteryLevel = level;
    }

    private void addTime(int mode, long ms) {
        String key = "time_" + mode;
        long current = prefs.getLong(key, 0);
        prefs.edit().putLong(key, current + ms).apply();
    }

    private void addDrop(int mode, int drop) {
        String key = "drop_" + mode;
        int current = prefs.getInt(key, 0);
        prefs.edit().putInt(key, current + drop).apply();
    }

    public void resetStats() {
        prefs.edit().clear()
             .putLong("last_reset_time", System.currentTimeMillis())
             .apply();
        modeStartTime = System.currentTimeMillis();
    }

    // Getters for UI
    public long getTime(int mode) {
        long time = prefs.getLong("time_" + mode, 0);
        if (currentMode == mode && modeStartTime > 0) {
            time += (System.currentTimeMillis() - modeStartTime);
        }
        return time;
    }

    public int getDrop(int mode) {
        return prefs.getInt("drop_" + mode, 0);
    }
    
    public long getLastResetTime() {
        long t = prefs.getLong("last_reset_time", 0);
        if (t == 0) {
            t = System.currentTimeMillis();
            prefs.edit().putLong("last_reset_time", t).apply();
        }
        return t;
    }
}
