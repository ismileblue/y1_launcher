package com.themoon.y1;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;

public class MainActivity extends Activity {
    public static MainActivity instance;
    private static final int STATE_MENU = 1;
    private static final int STATE_BROWSER = 2;
    private static final int STATE_PLAYER = 3;
    private static final int STATE_SETTINGS = 4;
    private static final int STATE_BLUETOOTH = 5;
    private static final int STATE_WIFI = 6;
    private static final int STATE_WIFI_KEYBOARD = 7;
    private static final int STATE_BRIGHTNESS = 8;
    private static final int STATE_STORAGE = 9;
    private static final int STATE_WEBSERVER = 10;
    // 💡 미디어 라이브러리 브라우저 상태 관리 변수들
    private static final int BROWSER_ROOT = 0;
    private static final int BROWSER_FOLDER = 1;
    private static final int BROWSER_ARTISTS = 2;
    private static final int BROWSER_ALBUMS = 3;
    private static final int BROWSER_VIRTUAL_SONGS = 4;
    // 💡 [추가] 손상되어 앱을 터뜨린 '독약 파일'들을 기억하는 블랙리스트
    private java.util.Set<String> blacklist = new java.util.HashSet<>();
    private int currentBrowserMode = BROWSER_ROOT;
    private String virtualQueryType = "";
    private String virtualQueryValue = "";
    private List<File> virtualSongList = new ArrayList<>();
    // 💡 백그라운드 미디어 제어권(스크린 오프) 변수
    private MediaSession mediaSession;
    // 💡 [추가] OS 스캐너를 대체할 '자체 미디어 라이브러리 엔진' 변수들
    private static class SongItem {
        File file;
        String title;
        String artist;
        String album;

        public SongItem(File f, String t, String a, String al) {
            file = f;
            title = t;
            artist = a;
            album = al;
        }
    }

    private List<SongItem> customLibrary = new ArrayList<>();
    private boolean isCustomScanning = false;
    private int currentScreenState = STATE_MENU;
    // 💡 자체 날짜/시간 설정용 임시 변수
    private int dtYear = 2026, dtMonth = 1, dtDay = 1, dtHour = 12, dtMinute = 0;
    private View layoutMainMenu, layoutBrowserMode, layoutSettingsMode;
    private View layoutBluetoothMode, layoutWifiMode, layoutWifiKeyboard;
    private View layoutPlayerMode, layoutVolumeOverlay;
    private View layoutBrightnessMode, layoutStorageMode, layoutWebServerMode;

    private LinearLayout containerBrowserItems, containerSettingsItems;
    private LinearLayout containerBtItems, containerWifiItems;

    private TextView tvStatusClock, tvStatusBattery;
    private ImageView ivStatusBluetooth, ivStatusWifi, ivStatusHeadphone, ivMainBg;

    private TextView tvBrowserPath, tvPlayerTitle, tvPlayerArtist, tvPlayerTimeCurrent, tvPlayerTimeTotal;
    private TextView tvPlayerTrackCount;
    private ImageView ivPlayerShuffleStatus, ivPlayerRepeatStatus; // 💡 텍스트뷰에서 이미지뷰로 변경!
    private ProgressBar playerProgress, volumeProgress, pbBrightness, pbStorage;
    private TextView tvBrightnessVal, tvStorageDetails;

    private TextView tvServerStatus, tvServerIp;
    private Button btnServerToggle;

    private ImageView ivMenuPreview, ivAlbumArt, ivPlayerBgBlur, ivPauseOverlay;

    private Button btnNowPlaying, btnPlay, btnSettings, btnBluetooth, btnRadio;
    private Button btnScanBt, btnScanWifi;

    private TextView tvKeyboardSsid, tvKeyboardInput;
    private TextView tvKeyPprev, tvKeyPrev, tvKeyCurrent, tvKeyNext, tvKeyNnext;

    private final String[] KEYBOARD_CHARS = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u",
            "v", "w", "x", "y", "z",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "!", "@", "#", "$", "%", "^", "&", "*", "-", "_", "+", "=", ".", "?",
            "[DEL]", "[CONN]"
    };
    private int keyboardIndex = 0;
    private String targetWifiSsid = "";
    private String typedPassword = "";
    private boolean isTargetWifiOpen = false;
    // 💡 미디어 스캐너가 현재 작업 중인지 추적하는 변수
    private boolean isMediaScanning = false;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private File rootFolder = new File("/storage/sdcard0/Music");
    private File currentFolder = rootFolder;
    private List<File> originalPlaylist = new ArrayList<File>();
    private List<File> currentPlaylist = new ArrayList<File>();
    private int currentIndex = 0;
    private boolean isPausedByHand = true;

    private java.io.FileInputStream currentFileInputStream = null;
    private TextView tvMenuPreviewTitle, tvMenuPreviewArtist;
    private SharedPreferences prefs;
    private boolean isShuffleMode = false;
    private int repeatMode = 0; // 0: OFF, 1: ONE (Repeat One), 2: ALL (Repeat Folder/All)
    private boolean isSoundEffectEnabled = true;
    private boolean isVibrationEnabled = true;
    private boolean isPickingBackground = false;

    private boolean isScreenOffControlEnabled = false;
    // 💡 마지막으로 재생된 앨범 아트를 기억하는 변수
    private byte[] lastAlbumArtBytes = null;
    // 💡 이퀄라이저 관련 변수 추가
    private Equalizer equalizer;
    private List<String> eqPresetNames = new ArrayList<String>();
    private int currentEqPresetIndex = 0;

    private int lastSettingsFocusIndex = 1;

    private boolean isScreenSleeping = false;
    private long lastScreenOnTime = 0;

    private int currentTimeoutIndex = 1;
    private final int[] TIMEOUT_VALUES = { 15000, 30000, 60000, 300000 };
    private final String[] TIMEOUT_NAMES = { "15 Sec", "30 Sec", "1 Min", "5 Min" };

    private int currentSystemBrightness = 255;
    private Random random = new Random();

    private List<String> foundBtDevices = new ArrayList<String>();
    private List<String> foundWifiNetworks = new ArrayList<String>();

    private Y1WebServer webServer;
    private boolean isServerRunning = false;

    private Handler clockHandler = new Handler();
    private Runnable clockTask = new Runnable() {
        @Override
        public void run() {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            tvStatusClock.setText(sdf.format(new Date()));
            clockHandler.postDelayed(this, 1000);
        }
    };

    private Handler progressHandler = new Handler();
    private Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int current = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();
                    int progress = (int) (((float) current / duration) * 100);
                    playerProgress.setProgress(progress);
                    tvPlayerTimeCurrent.setText(formatTime(current));
                    tvPlayerTimeTotal.setText(formatTime(duration));
                }
            } catch (Exception e) {
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private Handler volumeHandler = new Handler();
    private Runnable hideVolumeTask = new Runnable() {
        @Override
        public void run() {
            layoutVolumeOverlay.setVisibility(View.GONE);
        }
    };

    private BroadcastReceiver systemStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenSleeping = true;
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenSleeping = false;
                lastScreenOnTime = System.currentTimeMillis();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = (int) ((level / (float) scale) * 100);
                tvStatusBattery.setText(batteryPct + "%");
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                int state = intent.getIntExtra("state", -1);
                if (state == 1) {
                    ivStatusHeadphone.setVisibility(View.VISIBLE);
                    ivStatusHeadphone.setColorFilter(0xFF00FFFF);
                } else {
                    ivStatusHeadphone.setVisibility(View.GONE);
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    ivStatusBluetooth.setVisibility(View.VISIBLE);
                    ivStatusBluetooth.setColorFilter(0xFF5555FF);
                } else {
                    ivStatusBluetooth.setVisibility(View.GONE);
                }
                if (currentScreenState == STATE_SETTINGS)
                    buildSettingsUI();
                else if (currentScreenState == STATE_BLUETOOTH)
                    startBluetoothScan();
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    ivStatusWifi.setVisibility(View.VISIBLE);
                    ivStatusWifi.setColorFilter(0xFFFFBB00);
                } else {
                    ivStatusWifi.setVisibility(View.GONE);
                }
                if (currentScreenState == STATE_SETTINGS)
                    buildSettingsUI();
                else if (currentScreenState == STATE_WIFI)
                    startWifiScan();
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    ivStatusWifi.setColorFilter(0xFF00FF00);
                    if (currentScreenState == STATE_WIFI)
                        startWifiScan();
                } else {
                    ivStatusWifi.setColorFilter(0xFFFFBB00);
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                if (deviceName != null && !foundBtDevices.contains(deviceAddress)) {
                    foundBtDevices.add(deviceAddress);
                    // 💡 새로 발견된 낯선 기기는 isPaired = false 로 보냅니다.
                    addBluetoothItemToUI(deviceName, device, false);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btnScanBt.setText("Scan Complete (Retry)");
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    List<ScanResult> results = wm.getScanResults();
                    btnScanWifi.setText("Scan Complete (Retry)");
                    updateWifiUI(results);
                }
            }
            // 🚀 [여기에 추가!] 시스템 미디어 스캐너 감지 센서
            else if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
                isMediaScanning = true;
            } else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                isMediaScanning = false;
                Toast.makeText(context, "Media Scan Complete! Library Updated.", Toast.LENGTH_SHORT).show();

                // 🚀 시스템 스캔이 끝났을 때 우리 자체 라이브러리도 다시 갱신해 줍니다!
                if (!isCustomScanning) {
                    isCustomScanning = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            customLibrary.clear();
                            buildCustomLibrary(rootFolder);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isCustomScanning = false;
                                    if (currentScreenState == STATE_BROWSER) {
                                        if (currentBrowserMode == BROWSER_ROOT) {
                                            buildFileBrowserUI();
                                        } else if (currentBrowserMode == BROWSER_ARTISTS) {
                                            buildVirtualCategories("ARTIST");
                                        } else if (currentBrowserMode == BROWSER_ALBUMS) {
                                            buildVirtualCategories("ALBUM");
                                        } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                                            buildVirtualSongs();
                                        }
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
// 🚀 앱이 켜지면 자기 자신을 변수에 등록합니다.
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                try {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                    e.printStackTrace(pw);
                    java.io.File logFile = new java.io.File("/storage/sdcard0/y1_crash_log.txt");
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true);
                    fos.write(("\n\n--- 💥 CRASH REPORT (" + new java.util.Date().toString() + ") ---\n").getBytes());
                    fos.write(sw.toString().getBytes());
                    fos.close();
                } catch (Exception ex) {
                }
                System.exit(1);
            }
        });

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // 🚀 [시스템 공식 등록] 화면이 꺼져도 버튼 신호를 받을 수 있도록 수신기를 장착합니다!
        ComponentName componentName = new ComponentName(getPackageName(), MediaBtnReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(componentName);
// 🚀 [스크린 오프 완벽 제어 1단계] 시스템의 미디어/버튼 제어권을 앱이 뺏어옵니다!
        try {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                mediaSession = new MediaSession(this, "Y1_MEDIA_SESSION");
                mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
                mediaSession.setCallback(new MediaSession.Callback() {
                    @Override
                    public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                        KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {

                            // 💡 설정에서 스크린 오프 컨트롤이 꺼져(OFF) 있으면 무시합니다!
                            if (!isScreenOffControlEnabled) return super.onMediaButtonEvent(mediaButtonIntent);

                            int keyCode = event.getKeyCode();

                            // 🔊 휠 왼쪽 (볼륨 다운)
                            if (keyCode == 21) { adjustVolume(false); clickFeedback(); return true; }
                            // 🔊 휠 오른쪽 (볼륨 업)
                            if (keyCode == 22) { adjustVolume(true); clickFeedback(); return true; }
                            // ⏮ 이전 곡 버튼
                            if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) { prevTrack(); clickFeedback(); return true; }
                            // ⏭ 다음 곡 버튼
                            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) { nextTrack(); clickFeedback(); return true; }
                            // ⏯ 재생/일시정지 버튼
                            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == 86) { playOrPauseMusic(); clickFeedback(); return true; }
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent);
                    }
                });
                mediaSession.setActive(true);
            }
        } catch (Exception e) {}

        prefs = getSharedPreferences("Y1_SETTINGS", MODE_PRIVATE);

        // 🚀 [테마 파일 동적 로드] 기기 내부의 폴더에서 테마 파일들을 읽어옵니다!
        File themeFolder = new File("/storage/sdcard0/Y1_Themes");
        ThemeManager.loadThemesFromStorage(themeFolder);

        try {
            // 저장된 인덱스 번호를 불러옵니다. (파일이 지워졌을 수도 있으니 안전하게 처리됨)
            int savedThemeIndex = prefs.getInt("app_theme_index", 0);
            ThemeManager.setThemeIndex(savedThemeIndex);
        } catch (Exception e) {}

        // (이하 블랙리스트 및 다른 설정 불러오기 코드 유지)
        // 💡 1. 블랙리스트 (안드로이드 내부 버그 방지를 위해 HashSet을 새로 감싸서 안전하게 로드)
        try {
            java.util.Set<String> savedBlacklist = prefs.getStringSet("blacklist", new java.util.HashSet<String>());
            blacklist = new java.util.HashSet<>(savedBlacklist);

            String poisonFile = prefs.getString("last_attempted_file", null);
            if (poisonFile != null) {
                blacklist.add(poisonFile);
                prefs.edit().putStringSet("blacklist", blacklist).remove("last_attempted_file").commit();
            }
        } catch (Exception e) {}

        // 💡 2. 설정값들을 각각 독립적으로 불러오기 (어떤 상황에서도 절대 스킵되지 않습니다!)
        try { isShuffleMode = prefs.getBoolean("shuffle", false); } catch (Exception e) {}

        try {
            if (prefs.contains("repeat_mode")) {
                repeatMode = prefs.getInt("repeat_mode", 0);
            } else {
                repeatMode = prefs.getBoolean("repeat", false) ? 1 : 0;
            }
        } catch (Exception e) {}

        try {
            isSoundEffectEnabled = prefs.getBoolean("sound", true);
            applySoundSetting();
        } catch (Exception e) {}

        try { isVibrationEnabled = prefs.getBoolean("vibrate", true); } catch (Exception e) {}
        try { isScreenOffControlEnabled = prefs.getBoolean("screen_off_control", false); } catch (Exception e) {}
        try { currentTimeoutIndex = prefs.getInt("timeout_idx", 1); } catch (Exception e) {}

        try {
            currentSystemBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } catch (Exception e) {}

        // 💡 [EQ 프리셋 목록 자동 로드] 기기가 지원하는 이퀄라이저 리스트를 가져옵니다.
        try {
            MediaPlayer dummyMp = new MediaPlayer();
            Equalizer dummyEq = new Equalizer(0, dummyMp.getAudioSessionId());
            short presets = dummyEq.getNumberOfPresets();
            for (short i = 0; i < presets; i++) {
                eqPresetNames.add(dummyEq.getPresetName(i));
            }
            dummyEq.release();
            dummyMp.release();
        } catch (Exception e) {
            eqPresetNames.add("Normal (Default)");
        }

        currentEqPresetIndex = prefs.getInt("eq_preset", 0);
        if (currentEqPresetIndex >= eqPresetNames.size())
            currentEqPresetIndex = 0;

        if (!rootFolder.exists())
            rootFolder.mkdirs();

        // 🚀 [추가된 부분] 앱이 켜질 때(혹은 튕기고 재시작될 때) 조용히 자동 스캔을 돌려 리스트를 복구합니다!
        if (customLibrary.isEmpty() && !isCustomScanning) {
            isCustomScanning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    customLibrary.clear();
                    buildCustomLibrary(rootFolder);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isCustomScanning = false;
                            // 스캔이 끝났을 때 사용자가 이미 브라우저 화면에 진입해 있다면 화면을 새로고침해 줍니다.
                            if (currentScreenState == STATE_BROWSER) {
                                if (currentBrowserMode == BROWSER_ROOT) {
                                    buildFileBrowserUI();
                                } else if (currentBrowserMode == BROWSER_ARTISTS) {
                                    buildVirtualCategories("ARTIST");
                                } else if (currentBrowserMode == BROWSER_ALBUMS) {
                                    buildVirtualCategories("ALBUM");
                                } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                                    buildVirtualSongs();
                                }
                            }
                        }
                    });
                }
            }).start();
        }
        layoutMainMenu = findViewById(R.id.layout_main_menu);
        ivMainBg = findViewById(R.id.iv_main_bg);
        ivMenuPreview = findViewById(R.id.iv_menu_preview);
        tvMenuPreviewTitle = findViewById(R.id.tv_menu_preview_title);
        tvMenuPreviewArtist = findViewById(R.id.tv_menu_preview_artist);
        // 🚀 핵심: 안드로이드는 이 코드가 있어야만 흐르는 글씨(Marquee) 애니메이션을 작동시킵니다!
        tvMenuPreviewTitle.setSelected(true);
        updateMainMenuBackground(); // 💡 앱을 켜면 저장된 상태에 맞춰 배경 자동 적용

        layoutBrowserMode = findViewById(R.id.layout_browser_mode);
        layoutPlayerMode = findViewById(R.id.layout_player_mode);
        containerBrowserItems = findViewById(R.id.container_browser_items);
        layoutVolumeOverlay = findViewById(R.id.layout_volume_overlay);
        volumeProgress = findViewById(R.id.volume_progress);
        volumeProgress.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

        layoutSettingsMode = findViewById(R.id.layout_settings_mode);
        containerSettingsItems = findViewById(R.id.container_settings_items);
        layoutBluetoothMode = findViewById(R.id.layout_bluetooth_mode);
        containerBtItems = findViewById(R.id.container_bt_items);
        btnScanBt = findViewById(R.id.btn_scan_bt);
        layoutWifiMode = findViewById(R.id.layout_wifi_mode);
        containerWifiItems = findViewById(R.id.container_wifi_items);
        btnScanWifi = findViewById(R.id.btn_scan_wifi);
        // 💡 [추가] 스캔 버튼에 휠 포커스가 닿았을 때 색상 변화 및 중복 소리 차단
        View.OnFocusChangeListener scanFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Button btn = (Button) v;
                if (hasFocus) {
                    btn.setBackgroundColor(0x88FFFFFF); // 휠이 닿으면 반투명 흰색 배경
                    btn.setTextColor(0xFF000000); // 글자는 검은색으로 반전!
                } else {
                    btn.setBackgroundColor(0x00000000); // 휠이 벗어나면 다시 투명 배경
                    btn.setTextColor(0xFFFFFFFF); // 글자는 원래대로 흰색!
                }
            }
        };
        btnScanBt.setOnFocusChangeListener(scanFocusListener);
        btnScanWifi.setOnFocusChangeListener(scanFocusListener);
        btnScanBt.setSoundEffectsEnabled(false);
        btnScanWifi.setSoundEffectsEnabled(false);
        layoutWifiKeyboard = findViewById(R.id.layout_wifi_keyboard);
        tvKeyboardSsid = findViewById(R.id.tv_keyboard_ssid);
        tvKeyboardInput = findViewById(R.id.tv_keyboard_input);
        tvKeyPprev = findViewById(R.id.tv_key_pprev);
        tvKeyPrev = findViewById(R.id.tv_key_prev);
        tvKeyCurrent = findViewById(R.id.tv_key_current);
        tvKeyNext = findViewById(R.id.tv_key_next);
        tvKeyNnext = findViewById(R.id.tv_key_nnext);

        layoutBrightnessMode = findViewById(R.id.layout_brightness_mode);
        pbBrightness = findViewById(R.id.pb_brightness);
        tvBrightnessVal = findViewById(R.id.tv_brightness_val);

        layoutStorageMode = findViewById(R.id.layout_storage_mode);
        pbStorage = findViewById(R.id.pb_storage);
        tvStorageDetails = findViewById(R.id.tv_storage_details);

        layoutWebServerMode = findViewById(R.id.layout_webserver_mode);
        // (기존 코드)
        tvServerStatus = findViewById(R.id.tv_server_status);
        tvServerIp = findViewById(R.id.tv_server_ip);
        btnServerToggle = findViewById(R.id.btn_server_toggle);

        // 🚀 2. 테마 매니저를 통해 각 화면의 반투명 덮개 색상을 한 번에 갈아입힙니다!
        int overlayColor = ThemeManager.getOverlayBackgroundColor();
        layoutBrowserMode.setBackgroundColor(overlayColor);
        layoutSettingsMode.setBackgroundColor(overlayColor);
        layoutBluetoothMode.setBackgroundColor(overlayColor);
        layoutWifiMode.setBackgroundColor(overlayColor);
        layoutWifiKeyboard.setBackgroundColor(overlayColor);
        layoutBrightnessMode.setBackgroundColor(overlayColor);
        layoutStorageMode.setBackgroundColor(overlayColor);
        layoutWebServerMode.setBackgroundColor(overlayColor);

        // 브라우저 텍스트 등 주요 고정 텍스트도 테마에 맞게 변경


        // 💡 평상시에도 옅은 유리 질감을 주어 버튼 영역이 어디인지 시각적으로 보여줍니다.
        btnServerToggle.setBackgroundColor(0x15FFFFFF);

        btnServerToggle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 🚀 휠이 올라갔을 때: 확실한 우유빛 배경과 검은색 굵은(Bold) 글씨로 반전!
                    btnServerToggle.setBackgroundColor(0xDDFFFFFF);
                    btnServerToggle.setTextColor(0xFF000000);
                    btnServerToggle.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    // 🚀 휠이 벗어났을 때: 다시 은은한 반투명 유리창과 얇은 흰색 글씨로 복귀!
                    btnServerToggle.setBackgroundColor(0x15FFFFFF);
                    btnServerToggle.setTextColor(0xFFFFFFFF);
                    btnServerToggle.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
            }
        });

        btnServerToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                toggleWebServer();
                updateWebServerUI();
            }
        });

        tvStatusClock = findViewById(R.id.tv_status_clock);
        tvStatusBattery = findViewById(R.id.tv_status_battery);
        ivStatusBluetooth = findViewById(R.id.iv_status_bluetooth);
        ivStatusWifi = findViewById(R.id.iv_status_wifi);
        ivStatusHeadphone = findViewById(R.id.iv_status_headphone);

        tvBrowserPath = findViewById(R.id.tv_browser_path);
        tvBrowserPath.setTextColor(ThemeManager.getTextColorPrimary()); // 🚀 고정된 흰색을 테마 색상으로 변경!

        btnNowPlaying = findViewById(R.id.btn_now_playing);
        btnPlay = findViewById(R.id.btn_play);
        btnSettings = findViewById(R.id.btn_settings);
        btnBluetooth = findViewById(R.id.btn_bluetooth);
        btnRadio = findViewById(R.id.btn_radio);
        Button btnWebServer = findViewById(R.id.btn_webserver);
        tvPlayerTitle = findViewById(R.id.tv_player_title);
        tvPlayerArtist = findViewById(R.id.tv_player_artist);
        tvPlayerTimeCurrent = findViewById(R.id.tv_player_time_current);
        tvPlayerTimeTotal = findViewById(R.id.tv_player_time_total);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        ivPlayerBgBlur = findViewById(R.id.iv_player_bg_blur);
        ivPauseOverlay = findViewById(R.id.iv_pause_overlay);
        playerProgress = findViewById(R.id.player_progress);
        tvPlayerTrackCount = findViewById(R.id.tv_player_track_count);

        // 🚀 [수정 후]
        ivPlayerShuffleStatus = findViewById(R.id.iv_player_shuffle_status);
        ivPlayerRepeatStatus = findViewById(R.id.iv_player_repeat_status);
        updatePlayerStatusIndicators();

        // 💡 R.drawable.뒤에 방금 추가한 파일의 이름을 적어줍니다.
        setupMenuButton(btnNowPlaying, R.drawable.music_circle);
        setupMenuButton(btnPlay, R.drawable.music_list);
        setupMenuButton(btnBluetooth, R.drawable.bluetooth_circle);
        setupMenuButton(btnSettings, R.drawable.setting_circle);
        setupMenuButton(btnRadio, R.drawable.radio_circle);
        // [아이콘 세팅 부분에 추가]
        // (안드로이드 기본 공유 아이콘을 넣었습니다. 나중에 R.drawable.icon_server 처럼 직접 만든 아이콘으로 변경하실 수
        // 있습니다!)
        setupMenuButton(btnWebServer, R.drawable.file_sync);

        // [클릭 리스너 부분에 추가]
        btnWebServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_WEBSERVER); // 서버 화면으로 바로 이동!
            }
        });
        btnNowPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPlaylist.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No music is currently playing.", Toast.LENGTH_SHORT).show();
                } else {
                    changeScreen(STATE_PLAYER);
                }
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentBrowserMode = BROWSER_ROOT; // 💡 뮤직 진입 시 라이브러리 최상단으로!

                // 🚀 재부팅 직후 SD 카드가 늦게 인식되어 초기 스캔이 실패했을 경우를 대비해,
                // 뮤직 메뉴 진입 시 라이브러리가 비어있다면 다시 한번 스캔을 자동으로 돌립니다.
                if (customLibrary.isEmpty() && !isCustomScanning) {
                    isCustomScanning = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            customLibrary.clear();
                            buildCustomLibrary(rootFolder);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    isCustomScanning = false;
                                    if (currentScreenState == STATE_BROWSER) {
                                        if (currentBrowserMode == BROWSER_ROOT) {
                                            buildFileBrowserUI();
                                        } else if (currentBrowserMode == BROWSER_ARTISTS) {
                                            buildVirtualCategories("ARTIST");
                                        } else if (currentBrowserMode == BROWSER_ALBUMS) {
                                            buildVirtualCategories("ALBUM");
                                        } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                                            buildVirtualSongs();
                                        }
                                    }
                                }
                            });
                        }
                    }).start();
                }

                changeScreen(STATE_BROWSER);
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_SETTINGS);
            }
        });
        btnBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_BLUETOOTH);
            }
        });
        btnRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(
                            new Intent().setClassName("com.mediatek.FMRadio", "com.mediatek.FMRadio.FMRadioActivity"));
                } catch (Exception e) {
                    Intent b = getPackageManager().getLaunchIntentForPackage("com.mediatek.FMRadio");
                    if (b != null)
                        startActivity(b);
                }
            }
        });

        btnScanBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBluetoothScan();
            }
        });
        btnScanWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWifiScan();
            }
        });

        clockHandler.post(clockTask);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(systemStatusReceiver, filter);

        try {
            if (audioManager.isWiredHeadsetOn()) {
                ivStatusHeadphone.setVisibility(View.VISIBLE);
                ivStatusHeadphone.setColorFilter(0xFF00FFFF);
            }
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba != null && ba.isEnabled()) {
                ivStatusBluetooth.setVisibility(View.VISIBLE);
                ivStatusBluetooth.setColorFilter(0xFF5555FF);
            }
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                ivStatusWifi.setVisibility(View.VISIBLE);
                WifiInfo info = wm.getConnectionInfo();
                if (info != null && info.getNetworkId() != -1)
                    ivStatusWifi.setColorFilter(0xFF00FF00);
                else
                    ivStatusWifi.setColorFilter(0xFFFFBB00);
            }
        } catch (Exception e) {
        }

        btnNowPlaying.requestFocus();


        // 🚀 1. 메인 화면의 배경과 글자색도 테마 매니저에 맞춰 갈아입힙니다!
        applyThemeToMainMenu();

        triggerAutoReconnect();

        // 🚀 2. 테마를 바꾸고 화면이 새로고침(recreate)되었을 때, 메인 화면이 아닌 '테마 선택 리스트'로 돌아오게 만듭니다!
        boolean rebootToTheme = prefs.getBoolean("reboot_to_theme", false);
        if (rebootToTheme) {
            prefs.edit().remove("reboot_to_theme").commit(); // 기억을 사용했으니 지웁니다.
            changeScreen(STATE_SETTINGS);
            buildThemeSelectorUI(); // 테마 리스트 화면을 바로 띄워줍니다.
        } else {
            btnNowPlaying.requestFocus(); // 평소 앱을 켤 때는 원래대로 메인 메뉴 포커스
        }
    }
    // 💡 [추가] XML의 고정값을 무시하고, 메인 화면 전체를 테마에 맞게 강제로 칠해주는 함수
    private void applyThemeToMainMenu() {
        try {
            // 🚀 1. 가장 핵심! 전체 배경 이미지(ivMainBg) 위에 테마 색상을 셀로판지처럼 덮어씌웁니다.
            if (ivMainBg != null) {
                int themeColor = ThemeManager.getOverlayBackgroundColor();
                // 색상 정보(RGB)만 뽑아낸 뒤, 앞에 0x66(투명도 40%)을 강제로 결합합니다.
                int softTint = (themeColor & 0x00FFFFFF) | 0x66000000;
                ivMainBg.setColorFilter(softTint, android.graphics.PorterDuff.Mode.SRC_ATOP);
            }

            // 2. 좌측 반투명 메뉴 배경판 색칠하기
            LinearLayout leftPane = (LinearLayout) ((LinearLayout) layoutMainMenu).getChildAt(0);
            leftPane.setBackgroundColor(ThemeManager.getOverlayBackgroundColor());

            int primary = ThemeManager.getTextColorPrimary();
            int secondary = ThemeManager.getTextColorSecondary();

            // 3. 메인 메뉴 버튼들의 기본 글자색 변경
            btnNowPlaying.setTextColor(primary);
            btnPlay.setTextColor(primary);
            btnBluetooth.setTextColor(primary);
            btnSettings.setTextColor(primary);
            btnRadio.setTextColor(primary);
            Button btnWebServer = findViewById(R.id.btn_webserver);
            if (btnWebServer != null) btnWebServer.setTextColor(primary);

            // 4. 버튼 옆에 있는 〉(화살표) 들의 색상 변경
            for (int i = 0; i < leftPane.getChildCount(); i++) {
                View child = leftPane.getChildAt(i);
                if (child instanceof android.widget.FrameLayout) {
                    android.widget.FrameLayout fl = (android.widget.FrameLayout) child;
                    for (int j = 0; j < fl.getChildCount(); j++) {
                        View flChild = fl.getChildAt(j);
                        if (flChild instanceof TextView && !(flChild instanceof Button)) {
                            ((TextView) flChild).setTextColor(secondary);
                        }
                    }
                }
            }

            // 5. 우측 빈 공간의 곡 제목/가수 및 상단 상태바(시계, 배터리) 글자색 덮어쓰기!
            if (tvMenuPreviewTitle != null) tvMenuPreviewTitle.setTextColor(primary);
            if (tvMenuPreviewArtist != null) tvMenuPreviewArtist.setTextColor(secondary);
            if (tvStatusClock != null) tvStatusClock.setTextColor(primary);
            if (tvStatusBattery != null) tvStatusBattery.setTextColor(primary);

        } catch (Exception e) {}
    }
    // 💡 [추가] 테마 리스트를 쫙 보여주고 사용자가 고를 수 있게 하는 전용 화면
    private void buildThemeSelectorUI() {
        File themeFolder = new File("/storage/sdcard0/Y1_Themes");
        ThemeManager.loadThemesFromStorage(themeFolder);

        containerSettingsItems.removeAllViews();
        createCategoryHeader("━ SELECT APP THEME ━");

        Button btnBack = createListButton("〈 CANCEL & BACK");
        btnBack.setTextColor(ThemeManager.getTextColorSecondary());
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildSettingsUI(); // 다시 설정 메인으로 돌아갑니다.
            }
        });
        containerSettingsItems.addView(btnBack);

        // SD카드 폴더에서 읽어온 테마들을 하나씩 버튼으로 만듭니다.
        for (int i = 0; i < ThemeManager.availableThemes.size(); i++) {
            final int index = i;
            ThemeManager.ThemeData theme = ThemeManager.availableThemes.get(i);

            String prefix = (ThemeManager.getCurrentThemeIndex() == i) ? "✔ " : "   ";
            Button btn = createListButton(prefix + theme.name);

            if (ThemeManager.getCurrentThemeIndex() == i) {
                btn.setTypeface(null, android.graphics.Typeface.BOLD);
                btn.setTextColor(0xFF00FF00); // 현재 사용 중인 테마는 초록색으로 굵게 강조!
            }

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    ThemeManager.setThemeIndex(index);
                    try {
                        // 🚀 테마를 바꾸고 새로고침될 때 다시 이 리스트 화면으로 돌아오도록 '티켓'을 발급해 둡니다.
                        prefs.edit().putInt("app_theme_index", index).putBoolean("reboot_to_theme", true).commit();
                    } catch (Exception e) {}

                    recreate(); // 화면 새로고침! (이제 메인으로 튕기지 않고 리스트 화면으로 복귀합니다)
                }
            });
            containerSettingsItems.addView(btn);
        }

        // 화면이 열리면 맨 처음 테마 버튼에 휠 포커스를 맞춰줍니다.
        if (containerSettingsItems.getChildCount() > 1) {
            containerSettingsItems.getChildAt(1).requestFocus();
        }
    }
    private void triggerAutoReconnect() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                wm.reconnect();
            }
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba != null && ba.isEnabled()) {
                java.util.Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
            }
        } catch (Exception e) {
        }
    }

    private void toggleWebServer() {
        if (isServerRunning) {
            if (webServer != null)
                webServer.stopServer();
            isServerRunning = false;
        } else {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) {
                Toast.makeText(this, "Please turn ON Wi-Fi first", Toast.LENGTH_SHORT).show();
                return;
            }
            webServer = new Y1WebServer(getApplicationContext(), rootFolder);
            webServer.start();
            isServerRunning = true;
        }
    }

    private void updateWebServerUI() {
        if (isServerRunning) {
            // 💡 애플 스타일: 이모지를 빼고 깔끔한 흰색으로!
            tvServerStatus.setText("SERVER RUNNING");
            tvServerStatus.setTextColor(0xFFFFFFFF);
            tvServerIp.setText("http://" + webServer.getLocalIpAddress() + ":8080");
            tvServerIp.setTextColor(0xFFFFFFFF);
            btnServerToggle.setText("STOP SERVER");
        } else {
            // 💡 애플 스타일: 튀지 않는 은은한 회색으로!
            tvServerStatus.setText("SERVER STOPPED");
            tvServerStatus.setTextColor(0xFF888888);
            tvServerIp.setText("http://---.---.---.---:8080");
            tvServerIp.setTextColor(0xFF888888);
            btnServerToggle.setText("START SERVER");
        }
    }

    // 💡 메인 화면 배경 자동 업데이트 (고화질 가우시안 블러 적용)
    // 💡 메인 화면 배경 자동 업데이트 (고화질 가우시안 블러 적용)
    private void updateMainMenuBackground() {
        try {
            Bitmap sourceBitmap = null;

            if (lastAlbumArtBytes != null && lastAlbumArtBytes.length > 0) {
                // 1. 앨범 아트 로드
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;
                sourceBitmap = BitmapFactory.decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length, opts);
            } else {
                // 2. 사용자 배경 이미지 로드
                String savedBgPath = prefs.getString("bg_path", null);
                if (savedBgPath != null && !savedBgPath.isEmpty()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(savedBgPath, opts);

                    int scale = 1;
                    int maxDim = Math.max(opts.outWidth, opts.outHeight);
                    while (maxDim / scale > 400) {
                        scale *= 2;
                    }

                    opts.inJustDecodeBounds = false;
                    opts.inSampleSize = scale;
                    sourceBitmap = BitmapFactory.decodeFile(savedBgPath, opts);
                } else {
                    // 🚀 3. [추가된 부분] 앨범 아트도 없고 지정한 배경도 없을 때 (최초 실행 시)
                    // 아이콘 폴더(drawable)에 있는 default_back 이미지를 불러옵니다!
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 2; // 메모리 절약을 위해 화질을 살짝만 조절
                    sourceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_back, opts);
                }
            }

            if (sourceBitmap != null) {
                // 🚀 불러온 이미지(default_back 포함)에 렌더스크립트 블러 엔진을 입힙니다.
                Bitmap blurredBitmap = applyGaussianBlur(sourceBitmap);
                ivMainBg.setImageBitmap(blurredBitmap);

                if (sourceBitmap != blurredBitmap) {
                    sourceBitmap.recycle();
                }
            } else {
                // 만약 위 과정에서 에러가 났을 경우를 대비한 최후의 안전장치
                ivMainBg.setImageResource(R.drawable.default_back);
            }
        } catch (Throwable t) {
            ivMainBg.setImageResource(R.drawable.default_back);
        }
    }

    // 💡 [수정] 메인 화면의 버튼들에 휠이 닿았을 때의 색상을 테마 엔진과 연결합니다!
    private void setupMenuButton(final Button btn, final int imageResId) {
        btn.setSoundEffectsEnabled(false);
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 🚀 휠이 닿았을 때: XML 고정값을 무시하고 테마의 강조 색상으로 변경!
                    btn.setBackgroundColor(ThemeManager.getListButtonFocusedBg());
                    btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());

                    if (btn.getId() == R.id.btn_now_playing && lastAlbumArtBytes != null
                            && lastAlbumArtBytes.length > 0) {
                        try {
                            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                            opts.inSampleSize = 2;
                            android.graphics.Bitmap bmp = android.graphics.BitmapFactory
                                    .decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length, opts);
                            ivMenuPreview.setImageBitmap(bmp);
                        } catch (Exception e) {
                            ivMenuPreview.setImageResource(imageResId);
                        }

                        if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                            tvMenuPreviewTitle.setVisibility(View.VISIBLE);
                            tvMenuPreviewArtist.setVisibility(View.VISIBLE);
                            tvMenuPreviewTitle.setText(tvPlayerTitle.getText());
                            tvMenuPreviewArtist.setText(tvPlayerArtist.getText());
                        }

                    } else {
                        ivMenuPreview.setImageResource(imageResId);

                        if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                            tvMenuPreviewTitle.setVisibility(View.GONE);
                            tvMenuPreviewArtist.setVisibility(View.GONE);
                        }
                    }

                } else {
                    // 🚀 휠이 빠졌을 때: 다시 투명 배경과 테마의 기본 글자색으로 변경!
                    btn.setBackgroundColor(0x00000000);
                    btn.setTextColor(ThemeManager.getTextColorPrimary());
                }
            }
        });
    }
    private void changeScreen(int state) {
        currentScreenState = state;
        layoutMainMenu.setVisibility(state == STATE_MENU ? View.VISIBLE : View.GONE);
        layoutBrowserMode.setVisibility(state == STATE_BROWSER ? View.VISIBLE : View.GONE);
        layoutPlayerMode.setVisibility(state == STATE_PLAYER ? View.VISIBLE : View.GONE);
        layoutSettingsMode.setVisibility(state == STATE_SETTINGS ? View.VISIBLE : View.GONE);
        layoutBluetoothMode.setVisibility(state == STATE_BLUETOOTH ? View.VISIBLE : View.GONE);
        layoutWifiMode.setVisibility(state == STATE_WIFI ? View.VISIBLE : View.GONE);
        layoutWifiKeyboard.setVisibility(state == STATE_WIFI_KEYBOARD ? View.VISIBLE : View.GONE);
        layoutBrightnessMode.setVisibility(state == STATE_BRIGHTNESS ? View.VISIBLE : View.GONE);
        layoutStorageMode.setVisibility(state == STATE_STORAGE ? View.VISIBLE : View.GONE);
        layoutWebServerMode.setVisibility(state == STATE_WEBSERVER ? View.VISIBLE : View.GONE);
        layoutVolumeOverlay.setVisibility(View.GONE);

        if (state == STATE_MENU) {
            isPickingBackground = false;
            View c = getCurrentFocus();
            if (c == null)
                btnNowPlaying.requestFocus();
        } else if (state == STATE_BROWSER) {
            if (currentBrowserMode == BROWSER_ROOT || currentBrowserMode == BROWSER_FOLDER) {
                buildFileBrowserUI();
            } else if (currentBrowserMode == BROWSER_ARTISTS) {
                buildVirtualCategories("ARTIST");
            } else if (currentBrowserMode == BROWSER_ALBUMS) {
                buildVirtualCategories("ALBUM");
            } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                buildVirtualSongs();
            }
        } else if (state == STATE_SETTINGS) {
            buildSettingsUI();
        } else if (state == STATE_BLUETOOTH) {
            startBluetoothScan();
        } else if (state == STATE_WIFI) {
            startWifiScan();
        } else if (state == STATE_WIFI_KEYBOARD) {
            openKeyboard();
        } else if (state == STATE_BRIGHTNESS) {
            loadBrightnessUI();
        } else if (state == STATE_STORAGE) {
            loadStorageUI();
        } else if (state == STATE_WEBSERVER) {
            updateWebServerUI();
            btnServerToggle.requestFocus();
        }
    }

    private void loadBrightnessUI() {
        try {
            currentSystemBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                    255);
        } catch (Exception e) {
        }
        pbBrightness.setProgress(currentSystemBrightness);
        int percent = (int) (((float) currentSystemBrightness / 255.0f) * 100);
        tvBrightnessVal.setText(percent + "%");
    }

    private void updateBrightness(int newBrightness) {
        currentSystemBrightness = newBrightness;
        pbBrightness.setProgress(currentSystemBrightness);
        int percent = (int) (((float) currentSystemBrightness / 255.0f) * 100);
        tvBrightnessVal.setText(percent + "%");

        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, currentSystemBrightness);
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = currentSystemBrightness / 255.0f;
            getWindow().setAttributes(layoutParams);
        } catch (Exception e) {
        }
    }

    private void loadStorageUI() {
        try {
            android.os.StatFs stat = new android.os.StatFs("/storage/sdcard0");
            long blockSize = stat.getBlockSize();
            long total = (stat.getBlockCount() * blockSize) / (1024 * 1024);
            long free = (stat.getAvailableBlocks() * blockSize) / (1024 * 1024);
            long used = total - free;
            pbStorage.setMax((int) total);
            pbStorage.setProgress((int) used);
            tvStorageDetails.setText("Used: " + used + "MB\nTotal: " + total + "MB");
        } catch (Exception e) {
            tvStorageDetails.setText("Storage: Unknown");
        }
    }

    private void handleCenterShortClick() {
        if (currentScreenState == STATE_WIFI_KEYBOARD) {
            handleKeyboardInput();
        } else if (currentScreenState != STATE_BRIGHTNESS && currentScreenState != STATE_STORAGE
                && currentScreenState != STATE_PLAYER) {
            View c = getCurrentFocus();
            if (c != null)
                c.performClick();
        }
    }

    // 💡 앱 자체의 억지 소리 발생 코드를 완전히 삭제합니다! (기기 하드웨어 소리만 사용)
    private void clickFeedback() {
        try {
            if (isVibrationEnabled) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null)
                    v.vibrate(30);
            }
        } catch (Exception e) {
        }
    }

    private void openKeyboard() {
        typedPassword = "";
        keyboardIndex = 0;
        tvKeyboardSsid.setText("Target: " + targetWifiSsid);
        updateKeyboardUI();
    }

    private void updateKeyboardUI() {
        int len = KEYBOARD_CHARS.length;
        int idxPprev = (keyboardIndex - 2 + len) % len;
        int idxPrev = (keyboardIndex - 1 + len) % len;
        int idxNext = (keyboardIndex + 1) % len;
        int idxNnext = (keyboardIndex + 2) % len;
        tvKeyPprev.setText(KEYBOARD_CHARS[idxPprev]);
        tvKeyPrev.setText(KEYBOARD_CHARS[idxPrev]);
        tvKeyCurrent.setText(KEYBOARD_CHARS[keyboardIndex]);
        tvKeyNext.setText(KEYBOARD_CHARS[idxNext]);
        tvKeyNnext.setText(KEYBOARD_CHARS[idxNnext]);
        if (isTargetWifiOpen) {
            tvKeyboardInput.setText("Open Network (Direct Connect)");
            keyboardIndex = len - 1;
            tvKeyCurrent.setText(KEYBOARD_CHARS[keyboardIndex]);
        } else {
            tvKeyboardInput.setText(typedPassword.length() == 0 ? "Enter Password..." : typedPassword);
        }
    }

    private void handleKeyboardInput() {
        String selectedChar = KEYBOARD_CHARS[keyboardIndex];
        clickFeedback();
        if (selectedChar.equals("[DEL]")) {
            if (typedPassword.length() > 0)
                typedPassword = typedPassword.substring(0, typedPassword.length() - 1);
        } else if (selectedChar.equals("[CONN]")) {
            connectToWifi();
        } else {
            typedPassword += selectedChar;
        }
        updateKeyboardUI();
    }

    private void connectToWifi() {
        Toast.makeText(this, "Connecting to " + targetWifiSsid + "...", Toast.LENGTH_SHORT).show();
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + targetWifiSsid + "\"";
            if (isTargetWifiOpen)
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            else
                conf.preSharedKey = "\"" + typedPassword + "\"";
            int netId = wm.addNetwork(conf);
            wm.disconnect();
            wm.enableNetwork(netId, true);
            wm.reconnect();
            wm.saveConfiguration();
        }
        changeScreen(STATE_WIFI);
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void startBluetoothScan() {
        final BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        boolean isOn = false;
        String statusText = "OFF";

        if (ba != null) {
            int state = ba.getState();
            if (state == BluetoothAdapter.STATE_ON) {
                isOn = true;
                statusText = "ON";
            } else if (state == BluetoothAdapter.STATE_TURNING_ON || state == BluetoothAdapter.STATE_TURNING_OFF) {
                statusText = "Wait...";
            }
        }

        View existingToggle = containerBtItems.findViewById(999991);
        if (existingToggle == null) {
            final LinearLayout btnToggle = createSettingRow("Bluetooth Power", statusText);
            btnToggle.setId(999991);
            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    if (ba != null) {
                        boolean isCurrentlyOn = ba.isEnabled();
                        if (isCurrentlyOn) {
                            Toast.makeText(MainActivity.this, "Turning Bluetooth OFF...", Toast.LENGTH_SHORT).show();
                            ba.disable();
                        } else {
                            Toast.makeText(MainActivity.this, "Turning Bluetooth ON...", Toast.LENGTH_SHORT).show();
                            ba.enable();
                        }
                        TextView tvRight = (TextView) btnToggle.getChildAt(1);
                        tvRight.setText("Wait...");
                        if (!btnToggle.hasFocus())
                            tvRight.setTextColor(0xFFFFFF00);
                    }
                }
            });
            containerBtItems.addView(btnToggle, 0);

            if (btnScanBt.getParent() != null) {
                ((android.view.ViewGroup) btnScanBt.getParent()).removeView(btnScanBt);
            }
            containerBtItems.addView(btnScanBt);
        } else {
            LinearLayout btnToggle = (LinearLayout) existingToggle;
            TextView tvRight = (TextView) btnToggle.getChildAt(1);
            tvRight.setText(statusText);
            if (!btnToggle.hasFocus()) {
                if (statusText.equals("ON"))
                    tvRight.setTextColor(0xFFFFFFFF);
                else if (statusText.equals("OFF"))
                    tvRight.setTextColor(0xFF888888);
                else
                    tvRight.setTextColor(0xFFFFFFFF);
            }
            for (int i = containerBtItems.getChildCount() - 1; i > 0; i--) {
                View v = containerBtItems.getChildAt(i);
                if (v != btnScanBt) {
                    containerBtItems.removeViewAt(i);
                }
            }
        }

        if (!isOn) {
            btnScanBt.setText("Bluetooth is OFF");
            if (getCurrentFocus() == null && containerBtItems.getChildCount() > 0)
                containerBtItems.getChildAt(0).requestFocus();
            return;
        }

        btnScanBt.setText("Scanning...");
        foundBtDevices.clear();

        // 🚀 1순위: 기기에 이미 페어링(저장)된 블루투스 기기들을 최상단에 먼저 깔아줍니다!
        try {
            java.util.Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    foundBtDevices.add(device.getAddress());
                    addBluetoothItemToUI(device.getName() != null ? device.getName() : "Unknown Device", device, true);
                }
            }
        } catch (Exception e) {
        }

        if (getCurrentFocus() == null && containerBtItems.getChildCount() > 0)
            containerBtItems.getChildAt(0).requestFocus();

        if (ba.isDiscovering())
            ba.cancelDiscovery();
        ba.startDiscovery();
    }

    // 💡 페어링 상태(isPaired)를 파라미터로 받아서 색상과 아이콘을 바꾸는 함수
    private void addBluetoothItemToUI(String name, final BluetoothDevice device, boolean isPaired) {
        String prefix = isPaired ? "✔ [PAIRED] " : "🎧 ";
        final Button btnDevice = createListButton(prefix + name);

        if (isPaired) {
            btnDevice.setTextColor(0xFF00FF00); // 💡 페어링된 기기도 초록색!
            btnDevice.setTypeface(null, android.graphics.Typeface.BOLD); // 💡 굵은 글씨!
        }

        btnDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                Toast.makeText(MainActivity.this, "Pairing/Connecting: " + device.getName(), Toast.LENGTH_SHORT).show();
                try {
                    Method method = device.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(device, (Object[]) null);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Pairing failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        containerBtItems.addView(btnDevice);
    }

    private void startWifiScan() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isOn = wm != null && wm.isWifiEnabled();
        updateWifiUI(null);

        if (isOn) {
            btnScanWifi.setText("Scanning...");
            foundWifiNetworks.clear();
            // 💡 무조건 최상단 전원 버튼으로 포커스 강제 이동!
            if (containerWifiItems.getChildCount() > 0)
                containerWifiItems.getChildAt(0).requestFocus();
            wm.startScan();
        } else {
            btnScanWifi.setText("Wi-Fi is OFF");
            // 💡 무조건 최상단 전원 버튼으로 포커스 강제 이동!
            if (containerWifiItems.getChildCount() > 0)
                containerWifiItems.getChildAt(0).requestFocus();
        }
    }

    private void updateWifiUI(List<ScanResult> results) {
        final WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isOn = false;
        String statusText = "OFF";

        if (wm != null) {
            int state = wm.getWifiState();
            if (state == WifiManager.WIFI_STATE_ENABLED) {
                isOn = true;
                statusText = "ON";
            } else if (state == WifiManager.WIFI_STATE_ENABLING || state == WifiManager.WIFI_STATE_DISABLING) {
                statusText = "Wait...";
            }
        }

        View existingToggle = containerWifiItems.findViewById(999992);
        if (existingToggle == null) {
            final LinearLayout btnToggle = createSettingRow("Wi-Fi Power", statusText);
            btnToggle.setId(999992);
            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    if (wm != null) {
                        boolean isCurrentlyOn = wm.isWifiEnabled();
                        if (isCurrentlyOn) {
                            Toast.makeText(MainActivity.this, "Turning Wi-Fi OFF...", Toast.LENGTH_SHORT).show();
                            wm.setWifiEnabled(false);
                        } else {
                            Toast.makeText(MainActivity.this, "Turning Wi-Fi ON...", Toast.LENGTH_SHORT).show();
                            wm.setWifiEnabled(true);
                        }
                        TextView tvRight = (TextView) btnToggle.getChildAt(1);
                        tvRight.setText("Wait...");
                        if (!btnToggle.hasFocus())
                            tvRight.setTextColor(0xFFFFFF00);
                    }
                }
            });
            containerWifiItems.addView(btnToggle, 0);

            if (btnScanWifi.getParent() != null) {
                ((android.view.ViewGroup) btnScanWifi.getParent()).removeView(btnScanWifi);
            }
            containerWifiItems.addView(btnScanWifi);
        } else {
            LinearLayout btnToggle = (LinearLayout) existingToggle;
            TextView tvRight = (TextView) btnToggle.getChildAt(1);
            tvRight.setText(statusText);
            if (!btnToggle.hasFocus()) {
                if (statusText.equals("ON"))
                    tvRight.setTextColor(0xFFFFFFFF);
                else if (statusText.equals("OFF"))
                    tvRight.setTextColor(0xFF888888);
                else
                    tvRight.setTextColor(0xFFFFFFFF);
            }
            for (int i = containerWifiItems.getChildCount() - 1; i > 0; i--) {
                View v = containerWifiItems.getChildAt(i);
                if (v != btnScanWifi) {
                    containerWifiItems.removeViewAt(i);
                }
            }
        }

        if (!isOn)
            return;

        if (results != null) {
            foundWifiNetworks.clear();
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = manager.getConnectionInfo();
            String connectedSSID = "";
            if (wifiInfo != null && wifiInfo.getSSID() != null) {
                connectedSSID = wifiInfo.getSSID().replace("\"", "");
            }

            // 🚀 1순위: 현재 연결된 와이파이를 가장 먼저 찾아서 최상단에 배치!
            for (ScanResult result : results) {
                if (result.SSID != null && !result.SSID.isEmpty() && !foundWifiNetworks.contains(result.SSID)) {
                    if (result.SSID.equals(connectedSSID)) {
                        foundWifiNetworks.add(result.SSID);
                        addWifiItemToUI(result.SSID, result.capabilities, true);
                    }
                }
            }

            // 🚀 2순위: 연결되지 않은 나머지 잡다한 와이파이들을 그 밑으로 나열
            for (ScanResult result : results) {
                if (result.SSID != null && !result.SSID.isEmpty() && !foundWifiNetworks.contains(result.SSID)) {
                    foundWifiNetworks.add(result.SSID);
                    addWifiItemToUI(result.SSID, result.capabilities, false);
                }
            }
        }
    }

    // 💡 연결 상태(isConnected)를 파라미터로 직접 전달받도록 개조된 함수
    private void addWifiItemToUI(final String ssid, String capabilities, final boolean isConnected) {
        final boolean isOpen = !capabilities.contains("WPA") && !capabilities.contains("WEP");
        String lockIcon = isOpen ? "📶 " : "🔒 ";

        // 연결된 기기 앞에는 투박한 글씨 대신 애플처럼 예쁜 체크마크(✔) 부여!
        String prefix = isConnected ? "✔ " : "";

        Button btnWifi = createListButton(prefix + lockIcon + ssid);

        if (isConnected) {
            btnWifi.setTextColor(0xFF00FF00); // 눈에 확 띄는 초록색!
            btnWifi.setTypeface(null, android.graphics.Typeface.BOLD); // 굵은 글씨로 강조!
        }

        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                if (isConnected) {
                    Toast.makeText(MainActivity.this, "Already connected.", Toast.LENGTH_SHORT).show();
                    return;
                }

                WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                boolean isSaved = false;
                int savedNetId = -1;
                try {
                    List<WifiConfiguration> configuredNetworks = manager.getConfiguredNetworks();
                    if (configuredNetworks != null) {
                        for (WifiConfiguration conf : configuredNetworks) {
                            if (conf.SSID != null && conf.SSID.equals("\"" + ssid + "\"")) {
                                isSaved = true;
                                savedNetId = conf.networkId;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                }

                if (isSaved && savedNetId != -1) {
                    Toast.makeText(MainActivity.this, "Connecting to saved network...", Toast.LENGTH_SHORT).show();
                    manager.disconnect();
                    manager.enableNetwork(savedNetId, true);
                    manager.reconnect();
                } else {
                    targetWifiSsid = ssid;
                    isTargetWifiOpen = isOpen;
                    changeScreen(STATE_WIFI_KEYBOARD);
                }
            }
        });
        containerWifiItems.addView(btnWifi);
    }

    private void createCategoryHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        // 💡 하늘색을 빼고, 애플 스타일의 은은한 반투명 흰색 & 굵은 글씨로 변경!
        tv.setTextColor(0xBBFFFFFF);
        tv.setTextSize(14);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(10, 30, 10, 5);
        containerSettingsItems.addView(tv);
    }

    private LinearLayout createSettingRow(String leftText, String rightText) {
        final LinearLayout layout = new LinearLayout(this);
        layout.setSoundEffectsEnabled(false);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setFocusable(true);
        layout.setPadding(20, 15, 20, 15);

        // 🚀 테마 매니저 연결!
        layout.setBackgroundColor(ThemeManager.getListButtonNormalBg());

        TextView tvLeft = new TextView(this);
        tvLeft.setText(leftText);
        tvLeft.setTextColor(ThemeManager.getTextColorPrimary()); // 🚀 테마 매니저
        tvLeft.setTextSize(18);
        tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvRight = new TextView(this);
        tvRight.setText(rightText);
        tvRight.setTextSize(18);
        tvRight.setTypeface(null, android.graphics.Typeface.BOLD);
        tvRight.setGravity(android.view.Gravity.RIGHT);
        tvRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (rightText.equals("ON") || rightText.equals("ONE") || rightText.equals("ALL"))
            tvRight.setTextColor(ThemeManager.getTextColorPrimary());
        else
            tvRight.setTextColor(ThemeManager.getTextColorSecondary());

        layout.addView(tvLeft);
        layout.addView(tvRight);

        layout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    layout.setBackgroundColor(ThemeManager.getListButtonFocusedBg());
                    ((TextView) layout.getChildAt(0)).setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    ((TextView) layout.getChildAt(1)).setTextColor(ThemeManager.getListButtonFocusedTextColor());

                    if (currentScreenState == STATE_SETTINGS) {
                        int idx = containerSettingsItems.indexOfChild(layout);
                        if (idx != -1) lastSettingsFocusIndex = idx;
                    }
                } else {
                    layout.setBackgroundColor(ThemeManager.getListButtonNormalBg());
                    ((TextView) layout.getChildAt(0)).setTextColor(ThemeManager.getTextColorPrimary());

                    TextView rightTv = (TextView) layout.getChildAt(1);
                    String currentText = rightTv.getText().toString();
                    if (currentText.equals("ON") || currentText.equals("ONE") || currentText.equals("ALL"))
                        rightTv.setTextColor(ThemeManager.getTextColorPrimary());
                    else
                        rightTv.setTextColor(ThemeManager.getTextColorSecondary());
                }
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 2, 0, 2);
        layout.setLayoutParams(lp);

        return layout;
    }
    private Button createListButton(String text) {
        final Button btn = new Button(this);
        btn.setSoundEffectsEnabled(false);
        btn.setText(text);
        btn.setTextSize(18);

        // 🚀 테마 매니저 연결!
        btn.setTextColor(ThemeManager.getTextColorPrimary());
        btn.setBackgroundColor(ThemeManager.getListButtonNormalBg());

        btn.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
        btn.setPadding(20, 10, 10, 10);
        btn.setFocusable(true);
        btn.setSingleLine(true);

        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 🚀 테마 매니저 연결!
                    btn.setBackgroundColor(ThemeManager.getListButtonFocusedBg());
                    btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                } else {
                    // 🚀 테마 매니저 연결!
                    btn.setBackgroundColor(ThemeManager.getListButtonNormalBg());
                    btn.setTextColor(ThemeManager.getTextColorPrimary());
                }
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 2, 0, 2);
        btn.setLayoutParams(lp);

        return btn;
    }
    private void buildSettingsUI() {
        containerSettingsItems.removeAllViews();

        // createCategoryHeader("━ QUICK SETTINGS ━");

        final LinearLayout btnShuffle = createSettingRow("Shuffle Mode", isShuffleMode ? "ON" : "OFF");
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isShuffleMode = !isShuffleMode;
                TextView tvStatus = (TextView) btnShuffle.getChildAt(1);
                tvStatus.setText(isShuffleMode ? "ON" : "OFF");
                updatePlayerStatusIndicators();
                try {
                    prefs.edit().putBoolean("shuffle", isShuffleMode).commit();
                } catch (Exception e) {
                }

                if (!currentPlaylist.isEmpty() && !originalPlaylist.isEmpty()) {
                    File currentSong = currentPlaylist.get(currentIndex);
                    if (isShuffleMode) {
                        java.util.Collections.shuffle(currentPlaylist);
                    } else {
                        currentPlaylist.clear();
                        currentPlaylist.addAll(originalPlaylist);
                    }
                    currentIndex = currentPlaylist.indexOf(currentSong);
                    if (currentIndex == -1)
                        currentIndex = 0;
                }
            }
        });
        containerSettingsItems.addView(btnShuffle);

        final LinearLayout btnRepeat = createSettingRow("Repeat Mode", getRepeatModeText(repeatMode));
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                repeatMode = (repeatMode + 1) % 3;
                TextView tvStatus = (TextView) btnRepeat.getChildAt(1);
                tvStatus.setText(getRepeatModeText(repeatMode));
                updatePlayerStatusIndicators();
                try {
                    prefs.edit().putInt("repeat_mode", repeatMode).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnRepeat);

        // 💡 [EQ 메뉴 추가] 이퀄라이저 프리셋 순환 버튼
        final LinearLayout btnEq = createSettingRow("Equalizer (EQ)", eqPresetNames.get(currentEqPresetIndex));
        btnEq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                if (eqPresetNames.size() > 1) {
                    currentEqPresetIndex = (currentEqPresetIndex + 1) % eqPresetNames.size();
                    ((TextView) btnEq.getChildAt(1)).setText(eqPresetNames.get(currentEqPresetIndex));
                    try {
                        prefs.edit().putInt("eq_preset", currentEqPresetIndex).commit();
                    } catch (Exception e) {
                    }

                    // 재생 중이라면 즉시 EQ를 변경합니다!
                    if (equalizer != null) {
                        try {
                            equalizer.usePreset((short) currentEqPresetIndex);
                        } catch (Exception e) {
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "This device does not support EQ presets.", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
        containerSettingsItems.addView(btnEq);

        final LinearLayout btnSound = createSettingRow("Button Sound", isSoundEffectEnabled ? "ON" : "OFF");
        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSoundEffectEnabled = !isSoundEffectEnabled;
                applySoundSetting(); // 💡 [여기 추가] 사용자가 누르는 즉시 시스템 음소거 제어
                clickFeedback();
                TextView tvStatus = (TextView) btnSound.getChildAt(1);
                tvStatus.setText(isSoundEffectEnabled ? "ON" : "OFF");
                try {
                    prefs.edit().putBoolean("sound", isSoundEffectEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnSound);

        final LinearLayout btnVibrate = createSettingRow("Button Vibrate", isVibrationEnabled ? "ON" : "OFF");
        btnVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVibrationEnabled = !isVibrationEnabled;
                clickFeedback();
                TextView tvStatus = (TextView) btnVibrate.getChildAt(1);
                tvStatus.setText(isVibrationEnabled ? "ON" : "OFF");
                try {
                    prefs.edit().putBoolean("vibrate", isVibrationEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnVibrate);

        final LinearLayout btnScreenOffCtrl = createSettingRow("Screen-Off Control",
                isScreenOffControlEnabled ? "ON" : "OFF");
        btnScreenOffCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isScreenOffControlEnabled = !isScreenOffControlEnabled;
                TextView tvStatus = (TextView) btnScreenOffCtrl.getChildAt(1);
                tvStatus.setText(isScreenOffControlEnabled ? "ON" : "OFF");
                try {
                    prefs.edit().putBoolean("screen_off_control", isScreenOffControlEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnScreenOffCtrl);
// 🚀 [수정된 테마 설정 버튼]
        final LinearLayout btnTheme = createSettingRow("App Theme", ThemeManager.getCurrentTheme().name);
        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                // 누르면 순환하지 않고, 전체 테마 리스트 화면으로 이동합니다!
                buildThemeSelectorUI();
            }
        });
        containerSettingsItems.addView(btnTheme);
        final LinearLayout btnTimeout = createSettingRow("Screen Timeout", TIMEOUT_NAMES[currentTimeoutIndex]);
        btnTimeout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                currentTimeoutIndex = (currentTimeoutIndex + 1) % TIMEOUT_VALUES.length;
                ((TextView) btnTimeout.getChildAt(1)).setText(TIMEOUT_NAMES[currentTimeoutIndex]);
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                            TIMEOUT_VALUES[currentTimeoutIndex]);
                } catch (Exception e) {
                }
                try {
                    prefs.edit().putInt("timeout_idx", currentTimeoutIndex).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnTimeout);

        // createCategoryHeader("━ SYSTEM MENUS ━");

        LinearLayout btnPowerOff = createSettingRow("Power Off (Shutdown)", "〉 ");
        btnPowerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("Power Off")
                        .setMessage("Do you want to shut down the device?")
                        .setPositiveButton("Shut Down", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot -p" });
                                    proc.waitFor();
                                } catch (Exception e) {
                                    try {
                                        Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    } catch (Exception ex) {
                                        Toast.makeText(MainActivity.this, "⚠️ 시스템 보안으로 인해 앱에서 직접 전원을 끌 수 없습니다.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        containerSettingsItems.addView(btnPowerOff);
        // 🚀 [추가] 락박스(Rockbox) OS로 재부팅하는 버튼
       /* LinearLayout btnRockbox = createSettingRow("Reboot to Rockbox", "〉 ");
        btnRockbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("Reboot to Rockbox")
                        .setMessage("Do you want to reboot into Rockbox OS?")
                        .setPositiveButton("Reboot", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Toast.makeText(MainActivity.this, "Rebooting to Rockbox...", Toast.LENGTH_SHORT)
                                            .show();

                                    // 💡 락박스 진입 명령어 실행 (기기 파티션 구조에 따라 다를 수 있습니다)
                                    // 기본적으로 대부분의 듀얼 부팅 기기는 아래 명령어 중 하나를 사용합니다.
                                    Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot alternate" });

                                    // 만약 위 명령어로 일반 안드로이드 재부팅이 된다면, 아래 명령어의 주석(//)을 해제하고 시도해 보세요.
                                    // Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot recovery"});

                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Failed: Root access required.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        containerSettingsItems.addView(btnRockbox);
        */
        LinearLayout btnServerMenu = createSettingRow("Web Server (PC Upload)", "〉 ");
        btnServerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_WEBSERVER);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnServerMenu);

        LinearLayout btnWifiMenu = createSettingRow("Wi-Fi Setup", "〉 ");
        btnWifiMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_WIFI);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnWifiMenu);

        LinearLayout btnBtMenu = createSettingRow("Bluetooth Setup", "〉 ");
        btnBtMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_BLUETOOTH);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnBtMenu);

        LinearLayout btnBrightMenu = createSettingRow("Display Brightness", "〉 ");
        btnBrightMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_BRIGHTNESS);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnBrightMenu);

        LinearLayout btnStorageMenu = createSettingRow("Storage Info", "〉 ");
        btnStorageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_STORAGE);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnStorageMenu);

        LinearLayout btnBg = createSettingRow("Change Background", "〉 ");
        btnBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isPickingBackground = true;
                currentBrowserMode = BROWSER_FOLDER; // 💡 배경화면 찾을 땐 강제로 파일 탐색기 모드로!
                changeScreen(STATE_BROWSER);
                Toast.makeText(MainActivity.this, "Select a JPG/PNG image", Toast.LENGTH_SHORT).show();
            }
        });
        containerSettingsItems.addView(btnBg);

        LinearLayout btnTime = createSettingRow("Date & Time Settings", "〉");
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                // 시스템 시간을 먼저 읽어와서 임시 변수에 저장합니다.
                java.util.Calendar c = java.util.Calendar.getInstance();
                dtYear = c.get(java.util.Calendar.YEAR);
                dtMonth = c.get(java.util.Calendar.MONTH) + 1;
                dtDay = c.get(java.util.Calendar.DAY_OF_MONTH);
                dtHour = c.get(java.util.Calendar.HOUR_OF_DAY);
                dtMinute = c.get(java.util.Calendar.MINUTE);

                // 우리가 새로 만든 예쁜 리스트 화면을 띄웁니다!
                buildDateTimeUI();
            }
        });
        containerSettingsItems.addView(btnTime);

        if (lastSettingsFocusIndex >= 0 && lastSettingsFocusIndex < containerSettingsItems.getChildCount()) {
            containerSettingsItems.getChildAt(lastSettingsFocusIndex).requestFocus();
        } else if (containerSettingsItems.getChildCount() > 1) {
            containerSettingsItems.getChildAt(1).requestFocus();
        }
    }

    private boolean isAudioFile(File f) {
        if (f == null || !f.isFile())
            return false;
        String name = f.getName().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg")
                || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ape") || name.endsWith(".wma");
    }

    private boolean isApkFile(File f) {
        if (f == null || !f.isFile())
            return false;
        return f.getName().toLowerCase().endsWith(".apk");
    }

    private boolean isImageFile(File f) {
        if (f == null || !f.isFile())
            return false;
        String name = f.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    // 💡 하위 폴더까지 뒤져서 음악 파일의 '경로'만 모두 수집해 오는 함수
    private void collectAudioFiles(File file, List<String> paths) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    collectAudioFiles(f, paths); // 폴더면 파고들기
                }
            }
        } else if (isAudioFile(file)) {
            paths.add(file.getAbsolutePath()); // 음악 파일이면 명단에 추가!
        }
    }

    // 💡 1. 안드로이드 스캐너를 버리고, 앱이 직접 MP3 태그를 추출하여 분류하는 함수!
    private void buildCustomLibrary(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    buildCustomLibrary(f); // 폴더면 끝까지 파고듭니다.
                } else if (isAudioFile(f)) {

                    if (blacklist.contains(f.getAbsolutePath()))
                        continue;
                    try {
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        java.io.FileInputStream fis = new java.io.FileInputStream(f);
                        mmr.setDataSource(fis.getFD());

                        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

                        if (title == null || title.isEmpty())
                            title = f.getName();
                        if (artist == null || artist.isEmpty())
                            artist = "Unknown Artist";
                        if (album == null || album.isEmpty())
                            album = "Unknown Album";

                        customLibrary.add(new SongItem(f, title, artist, album));

                        fis.close();
                        mmr.release();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    // 💡 2. 라이브러리 메인 라우터 (자체 스캔 버튼 적용)
    private void buildFileBrowserUI() {
        containerBrowserItems.removeAllViews();

        if (isPickingBackground || currentBrowserMode == BROWSER_FOLDER) {
            buildFolderBrowserUI();
            return;
        }

        if (currentBrowserMode == BROWSER_ROOT) {
            tvBrowserPath.setText("Library: Main Menu");

            Button btnFolder = createListButton("📁 Folders (Original)");
            btnFolder.setOnClickListener(v -> {
                clickFeedback();
                currentBrowserMode = BROWSER_FOLDER;
                buildFileBrowserUI();
            });
            containerBrowserItems.addView(btnFolder);

            Button btnArtist = createListButton("👤 Artists");
            btnArtist.setOnClickListener(v -> {
                clickFeedback();
                currentBrowserMode = BROWSER_ARTISTS;
                buildVirtualCategories("ARTIST");
            });
            containerBrowserItems.addView(btnArtist);

            Button btnAlbum = createListButton("💿 Albums");
            btnAlbum.setOnClickListener(v -> {
                clickFeedback();
                currentBrowserMode = BROWSER_ALBUMS;
                buildVirtualCategories("ALBUM");
            });
            containerBrowserItems.addView(btnAlbum);

            Button btnAll = createListButton("🎵 All Songs");
            btnAll.setOnClickListener(v -> {
                clickFeedback();
                currentBrowserMode = BROWSER_VIRTUAL_SONGS;
                virtualQueryType = "ALL";
                buildVirtualSongs();
            });
            containerBrowserItems.addView(btnAll);

            // 🚀 시스템을 거치지 않는 '앱 자체 스캔 엔진' 버튼!
            Button btnScan = createListButton(isCustomScanning ? "⏳ Scanning Media..." : "🔄 Scan Media Library");
            btnScan.setTextColor(isCustomScanning ? 0xFF000000 : 0xFFFFFFFF);
            btnScan.setOnClickListener(v -> {
                clickFeedback();
                if (isCustomScanning)
                    return;

                isCustomScanning = true;
                btnScan.setText("⏳ Scanning Media...");
                btnScan.setTextColor(0xFF000000);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        customLibrary.clear();
                        buildCustomLibrary(rootFolder);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isCustomScanning = false;
                                Toast.makeText(MainActivity.this,
                                                "Scan Complete! " + customLibrary.size() + " songs found.", Toast.LENGTH_SHORT)
                                        .show();
                                if (currentScreenState == STATE_BROWSER) {
                                    if (currentBrowserMode == BROWSER_ROOT) {
                                        buildFileBrowserUI();
                                    } else if (currentBrowserMode == BROWSER_ARTISTS) {
                                        buildVirtualCategories("ARTIST");
                                    } else if (currentBrowserMode == BROWSER_ALBUMS) {
                                        buildVirtualCategories("ALBUM");
                                    } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                                        buildVirtualSongs();
                                    }
                                }
                            }
                        });
                    }
                }).start();
            });
            containerBrowserItems.addView(btnScan);
            if (containerBrowserItems.getChildCount() > 0)
                containerBrowserItems.getChildAt(0).requestFocus();
        }
    }

    // 💡 3. 자체 DB에서 아티스트/앨범 카테고리 추출
    private void buildVirtualCategories(final String type) {
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText("Library: " + type + "s");

        List<String> categories = new ArrayList<>();
        for (SongItem song : customLibrary) {
            String val = type.equals("ARTIST") ? song.artist : song.album;
            if (!categories.contains(val))
                categories.add(val);
        }
        java.util.Collections.sort(categories);

        for (final String name : categories) {
            Button btn = createListButton((type.equals("ARTIST") ? "👤 " : "💿 ") + name);
            btn.setOnClickListener(v -> {
                clickFeedback();
                virtualQueryType = type;
                virtualQueryValue = name;
                currentBrowserMode = BROWSER_VIRTUAL_SONGS;
                buildVirtualSongs();
            });
            containerBrowserItems.addView(btn);
        }
        if (containerBrowserItems.getChildCount() > 0)
            containerBrowserItems.getChildAt(0).requestFocus();
    }

    // 💡 4. 자체 DB에서 선택한 곡들만 뽑아내는 함수
    private void buildVirtualSongs() {
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText("Library: " + (virtualQueryType.equals("ALL") ? "All Songs" : virtualQueryValue));

        virtualSongList.clear();
        for (SongItem song : customLibrary) {
            if (virtualQueryType.equals("ALL") ||
                    (virtualQueryType.equals("ARTIST") && song.artist.equals(virtualQueryValue)) ||
                    (virtualQueryType.equals("ALBUM") && song.album.equals(virtualQueryValue))) {

                virtualSongList.add(song.file);
                final int index = virtualSongList.size() - 1;
                Button btn = createListButton("🎵 " + song.title);
                btn.setOnClickListener(v -> {
                    clickFeedback();
                    playTrackList(virtualSongList, index);
                });
                containerBrowserItems.addView(btn);
            }
        }
        if (containerBrowserItems.getChildCount() > 0)
            containerBrowserItems.getChildAt(0).requestFocus();
    }

    private void buildFolderBrowserUI() {
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText("Path: " + currentFolder.getAbsolutePath().replace("/storage/sdcard0", ""));
        File[] files = currentFolder.listFiles();

        if (files == null || files.length == 0) {
            Button btnEmpty = createListButton(
                    files == null ? "⚠️ USB Disconnect Required (Tap to go back)" : "📂 Empty Folder (Tap to go back)");
            btnEmpty.setTextColor(0xFFFF5555);
            btnEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    if (currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())) {
                        isPickingBackground = false;
                        changeScreen(STATE_MENU);
                    } else {
                        currentFolder = currentFolder.getParentFile();
                        buildFileBrowserUI();
                    }
                }
            });
            containerBrowserItems.addView(btnEmpty);
            return;
        }

        List<File> folders = new ArrayList<File>();
        List<File> audioFiles = new ArrayList<File>();
        List<File> apkFiles = new ArrayList<File>();
        List<File> imageFiles = new ArrayList<File>();

        for (File f : files) {
            if (f.isDirectory())
                folders.add(f);
            else if (isPickingBackground && isImageFile(f))
                imageFiles.add(f);
            else if (!isPickingBackground && isAudioFile(f))
                audioFiles.add(f);
            else if (!isPickingBackground && isApkFile(f))
                apkFiles.add(f);
        }

        for (final File folder : folders) {
            Button b = createListButton("📁 " + folder.getName());
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    currentFolder = folder;
                    buildFileBrowserUI();
                }
            });
            containerBrowserItems.addView(b);
        }

        if (isPickingBackground) {
            for (final File img : imageFiles) {
                Button b = createListButton("🖼 " + img.getName());
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        try {
                            prefs.edit().putString("bg_path", img.getAbsolutePath()).commit();
                        } catch (Exception e) {
                        }

                        updateMainMenuBackground(); // 💡 선택 즉시 블러 처리해서 메인 화면에 적용

                        Toast.makeText(MainActivity.this, "Background Applied!", Toast.LENGTH_SHORT).show();
                        isPickingBackground = false;
                        changeScreen(STATE_MENU);
                    }
                });
                containerBrowserItems.addView(b);
            }
        } else {
            for (final File apk : apkFiles) {
                Button b = createListButton("📦 [INSTALL] " + apk.getName());
                b.setTextColor(0xFF00FFFF);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        installApk(apk);
                    }
                });
                containerBrowserItems.addView(b);
            }
            for (final File audio : audioFiles) {
                Button b = createListButton("🎵 " + audio.getName());
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        setupFolderPlaylist(audio);
                    }
                });
                containerBrowserItems.addView(b);
            }
        }
        if (containerBrowserItems.getChildCount() > 0)
            containerBrowserItems.getChildAt(0).requestFocus();
    }

    private void installApk(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Install Failed.", Toast.LENGTH_SHORT).show();
        }
    }

    // 💡 1. 가상의 리스트(아티스트, 앨범 등)를 통째로 플레이어에 밀어 넣는 핵심 함수
    private void playTrackList(List<File> playlist, int startIndex) {
        originalPlaylist.clear();
        originalPlaylist.addAll(playlist);
        currentPlaylist.clear();
        currentPlaylist.addAll(playlist);

        if (!playlist.isEmpty()) {
            File currentSong = originalPlaylist.get(startIndex);
            if (isShuffleMode) {
                java.util.Collections.shuffle(currentPlaylist);
                currentIndex = currentPlaylist.indexOf(currentSong);
                if (currentIndex == -1)
                    currentIndex = 0;
            } else {
                currentIndex = startIndex;
            }
        } else {
            currentIndex = 0;
        }

        prepareMusicTrack(currentIndex);
        try {
            if (mediaPlayer != null) {
                try {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                } catch (Exception e) {
                }
                mediaPlayer.start();
                isPausedByHand = false;
            }
        } catch (Exception e) {
        }
        updatePlayerUI();
        changeScreen(STATE_PLAYER);
    }

    // 💡 2. 기존 폴더 방식의 플레이리스트 생성기 (playTrackList를 부르도록 개조됨)
    private void setupFolderPlaylist(File selectedFile) {
        List<File> list = new ArrayList<>();
        File[] files = currentFolder.listFiles();
        int matchIndex = 0;
        if (files != null) {
            for (File f : files) {
                if (isAudioFile(f)) {
                    list.add(f);
                    if (f.getAbsolutePath().equals(selectedFile.getAbsolutePath()))
                        matchIndex = list.size() - 1;
                }
            }
        }
        playTrackList(list, matchIndex);
    }

    private void prepareMusicTrack(int index) {
        if (currentPlaylist.isEmpty())
            return;
        final File track = currentPlaylist.get(index);
        // 🚀 [추가된 부분] 손상된 파일 방어막: 파일이 없거나 용량이 1KB(1024 bytes) 미만인 껍데기 파일일 경우
        if (!track.exists() || track.length() < 1024) {
            tvPlayerTitle.setText("Corrupted File");
            tvPlayerArtist.setText("Skipping...");
            ivAlbumArt.setImageResource(R.drawable.default_album);

            // 시스템이 뻗기 전에 경고창을 띄우고 1.5초 뒤에 다음 곡으로 자동으로 부드럽게 넘겨버립니다!
            Toast.makeText(this, "Corrupted file detected. Skipping...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    nextTrack();
                }
            }, 1500);
            return;
        }
        tvPlayerTitle.setText(track.getName());
        tvPlayerArtist.setText("Loading...");
        ivAlbumArt.setImageResource(R.drawable.default_album);
        ivPlayerBgBlur.setImageResource(0);
        playerProgress.setProgress(0);
        tvPlayerTimeCurrent.setText("00:00");
        tvPlayerTimeTotal.setText("00:00");

        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            java.io.FileInputStream fisMmr = new java.io.FileInputStream(track);
            mmr.setDataSource(fisMmr.getFD());

            String t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (t != null && !t.isEmpty())
                tvPlayerTitle.setText(t);
            if (a != null && !a.isEmpty())
                tvPlayerArtist.setText(a);
            else
                tvPlayerArtist.setText("Unknown Artist");

            lastAlbumArtBytes = mmr.getEmbeddedPicture();
            updateMainMenuBackground();
            refreshNowPlayingPreview();
            if (lastAlbumArtBytes != null) {
                try {
                    // 중앙의 선명한 앨범 아트
                    BitmapFactory.Options optsCenter = new BitmapFactory.Options();
                    optsCenter.inSampleSize = 2;
                    Bitmap bmpCenter = BitmapFactory.decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length,
                            optsCenter);
                    ivAlbumArt.setImageBitmap(bmpCenter);

                    // 🚀 플레이어 뒷배경도 고급 블러 처리!
                    BitmapFactory.Options optsBg = new BitmapFactory.Options();
                    optsBg.inSampleSize = 4;
                    Bitmap sourceBg = BitmapFactory.decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length,
                            optsBg);
                    Bitmap blurredBg = applyGaussianBlur(sourceBg);
                    ivPlayerBgBlur.setImageBitmap(blurredBg);

                    if (sourceBg != blurredBg)
                        sourceBg.recycle(); // 메모리 정리
                } catch (Throwable e) {
                }
            }
            fisMmr.close();
            mmr.release();
        } catch (Throwable t) {
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset();
            }
            // 🚀 [버그 수정] 권한 누락으로 인해 음악 재생이 통째로 취소되는 것을 막는 방어막!
            try {
                mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            } catch (Exception e) {
                // 권한이 없어서 에러가 나더라도 무시하고 다음 단계(음악 준비)로 무사히 넘어가게 합니다.
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    String err = "Audio Error: what=" + what + " extra=" + extra;
                    try {
                        java.io.File log = new java.io.File("/storage/sdcard0/y1_audio_error.txt");
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(log, true);
                        fos.write((new java.util.Date().toString() + " - " + err + " File: " + track.getName() + "\n")
                                .getBytes());
                        fos.close();
                    } catch (Exception e) {
                    }
                    return true;
                }
            });

            if (currentFileInputStream != null) {
                try {
                    currentFileInputStream.close();
                } catch (Exception e) {
                }
            }
            currentFileInputStream = new java.io.FileInputStream(track);

            mediaPlayer.setDataSource(currentFileInputStream.getFD());
            mediaPlayer.prepare();

            // 💡 [이퀄라이저(EQ) 연결] 현재 재생되는 트랙에 EQ를 적용시킵니다.
            try {
                if (equalizer != null) {
                    equalizer.release();
                    equalizer = null;
                }
                equalizer = new Equalizer(0, mediaPlayer.getAudioSessionId());
                equalizer.setEnabled(true);
                if (currentEqPresetIndex < equalizer.getNumberOfPresets()) {
                    equalizer.usePreset((short) currentEqPresetIndex);
                }
            } catch (Exception e) {
            }

            tvPlayerTimeTotal.setText(formatTime(mediaPlayer.getDuration()));
            String currentTrackNum = String.format(Locale.US, "%02d", index + 1);
            String totalTrackNum = String.format(Locale.US, "%02d", currentPlaylist.size());
            tvPlayerTrackCount.setText(currentTrackNum + " / " + totalTrackNum);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    try {
                        if (repeatMode == 1) { // Repeat One
                            mediaPlayer.seekTo(0);
                            mediaPlayer.start();
                        } else if (repeatMode == 2) { // Repeat All
                            nextTrack();
                        } else { // Repeat Off
                            if (currentIndex < currentPlaylist.size() - 1) {
                                nextTrack();
                            } else {
                                // Reached the end, stop playback
                                currentIndex = 0;
                                prepareMusicTrack(currentIndex);
                                isPausedByHand = true;
                                updatePlayerUI();
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });
        } catch (Throwable e) {
            tvPlayerTitle.setText("Load Failed: " + track.getName());
        }
    }

    private String getRepeatModeText(int mode) {
        switch (mode) {
            case 1:
                return "ONE";
            case 2:
                return "ALL";
            default:
                return "OFF";
        }
    }

    //    private void updatePlayerStatusIndicators() {
//        try {
//            if (tvPlayerShuffleStatus != null) {
//                tvPlayerShuffleStatus.setVisibility(isShuffleMode ? View.VISIBLE : View.GONE);
//            }
//            if (tvPlayerRepeatStatus != null) {
//                if (repeatMode == 1) {
//                    tvPlayerRepeatStatus.setText("REPEAT ONE");
//                    tvPlayerRepeatStatus.setVisibility(View.VISIBLE);
//                } else if (repeatMode == 2) {
//                    tvPlayerRepeatStatus.setText("REPEAT ALL");
//                    tvPlayerRepeatStatus.setVisibility(View.VISIBLE);
//                } else {
//                    tvPlayerRepeatStatus.setVisibility(View.GONE);
//                }
//            }
//        } catch (Exception e) {
//        }
//    }
    private void updatePlayerStatusIndicators() {
        try {
// 1. 셔플 아이콘 세팅
            if (ivPlayerShuffleStatus != null) {
                if (isShuffleMode) {
                    ivPlayerShuffleStatus.setImageResource(R.drawable.ic_shuffle);
                    ivPlayerShuffleStatus.setVisibility(View.VISIBLE);
                } else {
                    ivPlayerShuffleStatus.setVisibility(View.GONE);
                }
            }
            if (ivPlayerRepeatStatus != null) {
                if (repeatMode == 1) { // 한 곡 반복
                    ivPlayerRepeatStatus.setImageResource(R.drawable.ic_repeat_one);
                    ivPlayerRepeatStatus.setVisibility(View.VISIBLE);
                } else if (repeatMode == 2) { // 전곡 반복
                    ivPlayerRepeatStatus.setImageResource(R.drawable.ic_repeat);
                    ivPlayerRepeatStatus.setVisibility(View.VISIBLE);
                } else { // 반복 꺼짐
                    ivPlayerRepeatStatus.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
        }
    }
    private void updatePlayerUI() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                ivAlbumArt.setAlpha(1.0f);
                ivPauseOverlay.setVisibility(View.GONE);
                progressHandler.post(updateProgressTask);

                // 🚀 [스크린 오프 완벽 제어 3단계] 재생 상태(PLAYING)를 시스템에 신고하여 제어권 유지
                if (mediaSession != null && android.os.Build.VERSION.SDK_INT >= 21) {
                    PlaybackState state = new PlaybackState.Builder()
                            .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                            .setState(PlaybackState.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1.0f)
                            .build();
                    mediaSession.setPlaybackState(state);
                }
            } else {
                ivAlbumArt.setAlpha(0.4f);
                ivPauseOverlay.setVisibility(View.VISIBLE);
                progressHandler.removeCallbacks(updateProgressTask);

                // 일시정지 상태(PAUSED) 신고
                if (mediaSession != null && android.os.Build.VERSION.SDK_INT >= 21) {
                    PlaybackState state = new PlaybackState.Builder()
                            .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                            .setState(PlaybackState.STATE_PAUSED, mediaPlayer == null ? 0 : mediaPlayer.getCurrentPosition(), 1.0f)
                            .build();
                    mediaSession.setPlaybackState(state);
                }
            }
            updatePlayerStatusIndicators();
        } catch (Exception e) {
        }
    }
    private void playOrPauseMusic() {
        try {
            if (mediaPlayer == null || currentPlaylist.isEmpty())
                return;
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPausedByHand = true;
            } else {
                try {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                } catch (Exception e) {
                }
                mediaPlayer.start();
                isPausedByHand = false;
            }
            updatePlayerUI();
        } catch (Throwable e) {
        }
    }

    private void nextTrack() {
        if (currentPlaylist.isEmpty())
            return;
        currentIndex = (currentIndex + 1) % currentPlaylist.size();
        prepareMusicTrack(currentIndex);
        if (!isPausedByHand) {
            try {
                try {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                } catch (Exception e) {
                }
                mediaPlayer.start();
                updatePlayerUI();
            } catch (Exception e) {
            }
        } else {
            updatePlayerUI();
        }
    }

    private void prevTrack() {
        if (currentPlaylist.isEmpty())
            return;
        currentIndex = (currentIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
        prepareMusicTrack(currentIndex);
        if (!isPausedByHand) {
            try {
                try {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                } catch (Exception e) {
                }
                mediaPlayer.start();
                updatePlayerUI();
            } catch (Exception e) {
            }
        } else {
            updatePlayerUI();
        }
    }

    private void adjustVolume(boolean up) {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (up && currentVol < maxVol)
            currentVol++;
        else if (!up && currentVol > 0)
            currentVol--;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);
        showDynamicVolumeOverlay();
    }

    private void showDynamicVolumeOverlay() {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        layoutVolumeOverlay.setVisibility(View.VISIBLE);
        volumeProgress.setProgress(currentVol);
        volumeHandler.removeCallbacks(hideVolumeTask);
        volumeHandler.postDelayed(hideVolumeTask, 2000);
    }

    private String formatTime(int ms) {
        int s = (ms / 1000) % 60;
        int m = (ms / (1000 * 60)) % 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = true;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 20)
                isScreenOn = pm.isInteractive();
            else
                isScreenOn = pm.isScreenOn();
        } catch (Exception e) {
        }

        boolean isWakingUp = !isScreenOn || ((event.getFlags() & KeyEvent.FLAG_WOKE_HERE) != 0)
                || (System.currentTimeMillis() - lastScreenOnTime < 500);

        if (isWakingUp) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return true;
            }

            if (isScreenOffControlEnabled && currentScreenState == STATE_PLAYER) {
                if (keyCode == 21) {
                    adjustVolume(false);
                    clickFeedback();
                    return true;
                }
                if (keyCode == 22) {
                    adjustVolume(true);
                    clickFeedback();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
                    prevTrack();
                    clickFeedback();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
                    nextTrack();
                    clickFeedback();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == 86) {
                    playOrPauseMusic();
                    clickFeedback();
                    return true;
                }
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == 86) {
            if (event.getRepeatCount() == 0) {
                playOrPauseMusic();
            }
            return true;
        }

        // 🚀 [수정] 플레이어 화면(STATE_PLAYER) 제한을 완전히 삭제했습니다!
        // 이제 메인 화면, 브라우저, 설정 창 등 어느 화면에 있든 버튼(87, 88)을 누르면 즉시 곡이 넘어갑니다.
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
            if (event.getRepeatCount() == 0) {
                clickFeedback();
                nextTrack();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
            if (event.getRepeatCount() == 0) {
                clickFeedback();
                prevTrack();
            }
            return true;
        }

        if (currentScreenState == STATE_WIFI_KEYBOARD) {
            if (keyCode == 21) {
                keyboardIndex = (keyboardIndex - 1 + KEYBOARD_CHARS.length) % KEYBOARD_CHARS.length;
                updateKeyboardUI();
                clickFeedback();
                return true;
            }
            if (keyCode == 22) {
                keyboardIndex = (keyboardIndex + 1) % KEYBOARD_CHARS.length;
                updateKeyboardUI();
                clickFeedback();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                changeScreen(STATE_WIFI);
                clickFeedback();
                return true;
            }
            return true;
        }

        if (currentScreenState == STATE_PLAYER) {
            if (keyCode == 21) {
                adjustVolume(false);
                clickFeedback();
                return true;
            }
            if (keyCode == 22) {
                adjustVolume(true);
                clickFeedback();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                changeScreen(STATE_BROWSER);
                clickFeedback();
                return true;
            }
            return true;
        }

        if (currentScreenState == STATE_BRIGHTNESS) {
            if (keyCode == 21) {
                currentSystemBrightness = Math.max(10, currentSystemBrightness - 15);
                updateBrightness(currentSystemBrightness);
                clickFeedback();
                return true;
            }
            if (keyCode == 22) {
                currentSystemBrightness = Math.min(255, currentSystemBrightness + 15);
                updateBrightness(currentSystemBrightness);
                clickFeedback();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                changeScreen(STATE_SETTINGS);
                clickFeedback();
                return true;
            }
            return true;
        }

        if (currentScreenState == STATE_STORAGE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                changeScreen(STATE_SETTINGS);
                clickFeedback();
                return true;
            }
            return true;
        }

        if (currentScreenState == STATE_WEBSERVER) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                clickFeedback();
                if (isServerRunning) {
                    new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setTitle("Server is Running")
                            .setMessage(
                                    "The Web Server is still active. Do you want to shut it down completely and exit?")
                            .setPositiveButton("Stop Server (Exit)", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    toggleWebServer();
                                    changeScreen(STATE_SETTINGS);
                                }
                            })
                            .setNegativeButton("Keep Running (Stay)", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    changeScreen(STATE_SETTINGS);
                                }
                            })
                            .show();
                } else {
                    changeScreen(STATE_SETTINGS);
                }
                return true;
            }
        }

        if (currentScreenState == STATE_MENU || currentScreenState == STATE_BROWSER
                || currentScreenState == STATE_SETTINGS || currentScreenState == STATE_BLUETOOTH
                || currentScreenState == STATE_WIFI) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                clickFeedback();
                if (currentScreenState == STATE_BROWSER) {
                    if (isPickingBackground) {
                        if (currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())) {
                            isPickingBackground = false;
                            changeScreen(STATE_MENU);
                        } else {
                            currentFolder = currentFolder.getParentFile();
                            buildFileBrowserUI();
                        }
                    } else {
                        // 💡 라이브러리 뒤로가기 로직
                        if (currentBrowserMode == BROWSER_ROOT) {
                            changeScreen(STATE_MENU);
                        } else if (currentBrowserMode == BROWSER_FOLDER) {
                            if (currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())) {
                                currentBrowserMode = BROWSER_ROOT;
                                buildFileBrowserUI();
                            } else {
                                currentFolder = currentFolder.getParentFile();
                                buildFileBrowserUI();
                            }
                        } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                            currentBrowserMode = virtualQueryType.equals("ALL") ? BROWSER_ROOT
                                    : (virtualQueryType.equals("ARTIST") ? BROWSER_ARTISTS : BROWSER_ALBUMS);
                            if (currentBrowserMode == BROWSER_ROOT)
                                buildFileBrowserUI();
                            else
                                buildVirtualCategories(virtualQueryType);
                        } else {
                            currentBrowserMode = BROWSER_ROOT;
                            buildFileBrowserUI();
                        }
                    }
                } else if (currentScreenState == STATE_BLUETOOTH || currentScreenState == STATE_WIFI) {
                    changeScreen(STATE_SETTINGS);
                } else if (currentScreenState == STATE_SETTINGS) {
                    changeScreen(STATE_MENU);
                }
                return true;
            }

            View c = getCurrentFocus();
            if (c != null) {
                if (keyCode == 21) {
                    View n = c.focusSearch(View.FOCUS_UP);
                    if (n != null)
                        n.requestFocus();
                    clickFeedback();
                    return true;
                }
                if (keyCode == 22) {
                    View n = c.focusSearch(View.FOCUS_DOWN);
                    if (n != null)
                        n.requestFocus();
                    clickFeedback();
                    return true;
                }
            }
            return super.onKeyDown(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = true;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 20)
                isScreenOn = pm.isInteractive();
            else
                isScreenOn = pm.isScreenOn();
        } catch (Exception e) {
        }

        boolean isWakingUp = !isScreenOn || ((event.getFlags() & KeyEvent.FLAG_WOKE_HERE) != 0)
                || (System.currentTimeMillis() - lastScreenOnTime < 500);

        if (isWakingUp) {
            return true;
        }

        // 💡 [핵심 차단 구역] 휠 조작(21, 22)이나 뒤로가기(BACK)를 '뗄 때'
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 21 || keyCode == 22) {
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            try {
                handleCenterShortClick();
            } catch (Exception e) {
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == 86
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87 || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == 88) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockTask);
        progressHandler.removeCallbacks(updateProgressTask);
        volumeHandler.removeCallbacks(hideVolumeTask);

        if (currentFileInputStream != null) {
            try {
                currentFileInputStream.close();
            } catch (Exception e) {
            }
        }

        // 💡 앱이 꺼질 때 엔진도 안전하게 종료
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // 🚀 [스크린 오프 완벽 제어 4단계] 앱 종료 시 권한 반납
        if (mediaSession != null && android.os.Build.VERSION.SDK_INT >= 21) {
            mediaSession.release();
        }

        unregisterReceiver(systemStatusReceiver);
    }


    // 💡 안드로이드 시스템 자체의 하드웨어 삑 소리 스트림을 직접 차단/허용하는 함수
    private void applySoundSetting() {
        try {
            if (audioManager != null) {
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, !isSoundEffectEnabled);
            }
            // 💡 핵심: 기기 터치 패널의 하드웨어 삑 소리를 강제로 차단하는 시스템 설정 덮어쓰기!
            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED,
                    isSoundEffectEnabled ? 1 : 0);
        } catch (Exception e) {
        }
    }

    // 💡 안드로이드 하드웨어 가속(RenderScript)을 이용한 고화질 가우시안 블러 함수!
    private Bitmap applyGaussianBlur(Bitmap original) {
        if (original == null)
            return null;
        try {
            Bitmap output = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
            RenderScript rs = RenderScript.create(this);
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation inAlloc = Allocation.createFromBitmap(rs, original, Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
            Allocation outAlloc = Allocation.createFromBitmap(rs, output);

            script.setRadius(25f); // 💡 블러 강도 설정 (0.0 ~ 25.0 범위, 25가 최대)
            script.setInput(inAlloc);
            script.forEach(outAlloc);
            outAlloc.copyTo(output);
            rs.destroy();

            return output;
        } catch (Exception e) {
            return original;
        }
    }

    // 💡 1. 날짜/시간 설정 메인 화면 (시간 오류 및 포커스 락 버그 완벽 수정 버전)
    private void buildDateTimeUI() {
        containerSettingsItems.removeAllViews();
        createCategoryHeader("━ SET DATE & TIME ━");

        final LinearLayout rowYear = createSettingRow("Year", String.valueOf(dtYear));
        rowYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Year", 2020, 2035, dtYear);
            }
        });
        containerSettingsItems.addView(rowYear);

        final LinearLayout rowMonth = createSettingRow("Month", String.format(java.util.Locale.US, "%02d", dtMonth));
        rowMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Month", 1, 12, dtMonth);
            }
        });
        containerSettingsItems.addView(rowMonth);

        final LinearLayout rowDay = createSettingRow("Day", String.format(java.util.Locale.US, "%02d", dtDay));
        rowDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Day", 1, 31, dtDay);
            }
        });
        containerSettingsItems.addView(rowDay);

        final LinearLayout rowHour = createSettingRow("Hour (24H)", String.format(java.util.Locale.US, "%02d", dtHour));
        rowHour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Hour", 0, 23, dtHour);
            }
        });
        containerSettingsItems.addView(rowHour);

        final LinearLayout rowMinute = createSettingRow("Minute", String.format(java.util.Locale.US, "%02d", dtMinute));
        rowMinute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Minute", 0, 59, dtMinute);
            }
        });
        containerSettingsItems.addView(rowMinute);

        createCategoryHeader("━━━━━━━━━━━━━━");

        final Button btnApply = createListButton("✅ APPLY DATE & TIME");
        btnApply.setTextColor(0xFFFFFFFF);
        btnApply.setTypeface(null, android.graphics.Typeface.BOLD);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                try {
                    // 🚀 [시간 오류 영구 해결] 기기의 기존 타임존을 건드리지 않고, 시간을 설정합니다.
                    // 기존 안드로이드의 `date` 명령어는 기기에 내장된 쉘(Toolbox vs Toybox)에 따라 파싱 방식이 완전히 달라,
                    // 잘못된 포맷이 들어가면 무조건 1970년이나 1980년으로 초기화(리셋)해버리는 심각한 버그가 있습니다.
                    // 이를 완벽히 방지하기 위해, 하나의 포맷을 적용해본 후 ➡️ 제대로 연도/월/일이 적용되었는지 확인하고 ➡️ 실패했다면 다음 포맷을
                    // 시도하는 자동 검증(Self-Verifying) 스크립트를 작성합니다!

                    String cmd = "settings put global auto_time 0; settings put system auto_time 0; ";

                    // 목표 날짜를 YYYYMMDD 형태로 만듭니다 (검증용)
                    String targetYMD = String.format(java.util.Locale.US, "%04d%02d%02d", dtYear, dtMonth, dtDay);

                    // 포맷 1: 구형 안드로이드(Toolbox) 전용 포맷 -> YYYYMMDD.HHmmss
                    String dateToolbox = String.format(java.util.Locale.US, "%04d%02d%02d.%02d%02d%02d", dtYear,
                            dtMonth, dtDay, dtHour, dtMinute, 0);
                    // 포맷 2: POSIX 국제 표준 포맷 (Toybox/Busybox 호환) -> MMDDhhmmYYYY.ss
                    String datePosix = String.format(java.util.Locale.US, "%02d%02d%02d%02d%04d.00", dtMonth, dtDay,
                            dtHour, dtMinute, dtYear);
                    // 포맷 3: 최신 안드로이드(Toybox) 문자열 포맷 -> YYYY-MM-DD HH:MM:SS
                    String dateString = String.format(java.util.Locale.US, "%04d-%02d-%02d %02d:%02d:%02d", dtYear,
                            dtMonth, dtDay, dtHour, dtMinute, 0);

                    // 💡 자체 검증 쉘 스크립트:
                    // 1. Toolbox 포맷을 먼저 시도합니다. (Toybox 기기에서는 에러가 나거나 시간이 뒤틀립니다)
                    // 2. 적용된 시간을 즉시 확인하여 목표 날짜와 다르면(1970년 등으로 초기화되었으면) POSIX 포맷을 시도합니다.
                    // 3. 그래도 안 되면 문자열 포맷을 시도합니다.
                    String executeCmd = cmd +
                            "date -s " + dateToolbox + "; " +
                            "if [ \"$(date +%Y%m%d)\" != \"" + targetYMD + "\" ]; then " +
                            "  date " + datePosix + "; " +
                            "  if [ \"$(date +%Y%m%d)\" != \"" + targetYMD + "\" ]; then " +
                            "    date -s \"" + dateString + "\"; " +
                            "  fi; " +
                            "fi; " +
                            "hwclock -w; sync";

                    Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", executeCmd });
                    proc.waitFor(); // 💡 시스템에 시간이 완벽하게 적용될 때까지 잠깐 기다립니다.

                    // 시스템 전역에 시간이 변경되었음을 강제로 방송하여 메인 페이지 시계와 시스템 앱들을 동기화시킵니다.
                    sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED));

                    Toast.makeText(MainActivity.this, "Time applied successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed: Root access required.", Toast.LENGTH_SHORT).show();
                }

                // 🚀 [포커스 버그 해결 1] 오염된 인덱스를 'Date & Time Settings' 메뉴 위치(14번째 항목)로 강제 정화
                lastSettingsFocusIndex = 14;
                buildSettingsUI();

                // 🚀 [포커스 버그 해결 2] 50ms의 미세한 안전 딜레이를 주어 UI 가 완벽히 배치된 후 포커스를 확실히 꽂아줍니다.
                containerSettingsItems.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (containerSettingsItems != null && containerSettingsItems.getChildCount() > 0) {
                            containerSettingsItems.getChildAt(containerSettingsItems.getChildCount() - 1)
                                    .requestFocus();
                        }
                    }
                }, 50);
            }
        });
        containerSettingsItems.addView(btnApply);

        final Button btnCancel = createListButton("❌ CANCEL (BACK)");
        btnCancel.setTextColor(0xFF888888);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                // 취소하고 나갈 때도 인덱스를 안전하게 복구하고 포커스를 인위적으로 매핑합니다.
                lastSettingsFocusIndex = 14;
                buildSettingsUI();
                containerSettingsItems.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (containerSettingsItems != null && containerSettingsItems.getChildCount() > 0) {
                            containerSettingsItems.getChildAt(containerSettingsItems.getChildCount() - 1)
                                    .requestFocus();
                        }
                    }
                }, 50);
            }
        });
        containerSettingsItems.addView(btnCancel);

        if (containerSettingsItems.getChildCount() > 1)
            containerSettingsItems.getChildAt(1).requestFocus();
    }

    // 💡 2. 숫자(년/월/일/시/분) 선택용 세로 리스트 화면
    private void buildDateTimeSelectorUI(final String type, int min, int max, int currentValue) {
        containerSettingsItems.removeAllViews();
        createCategoryHeader("━ SELECT " + type.toUpperCase() + " ━");

        Button btnBack = createListButton("〈 CANCEL & BACK");
        btnBack.setTextColor(0xFF888888);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeUI();
            }
        });
        containerSettingsItems.addView(btnBack);

        Button focusBtn = null;
        for (int i = min; i <= max; i++) {
            final int val = i;
            String displayVal = (type.equals("Minute") || type.equals("Hour") || type.equals("Month")
                    || type.equals("Day")) ? String.format(java.util.Locale.US, "%02d", val) : String.valueOf(val);
            Button btn = createListButton(displayVal);
            btn.setGravity(android.view.Gravity.CENTER); // 가운데 정렬로 예쁘게!

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    if (type.equals("Year"))
                        dtYear = val;
                    else if (type.equals("Month"))
                        dtMonth = val;
                    else if (type.equals("Day"))
                        dtDay = val;
                    else if (type.equals("Hour"))
                        dtHour = val;
                    else if (type.equals("Minute"))
                        dtMinute = val;
                    buildDateTimeUI(); // 선택하면 자동으로 이전 화면으로 복귀!
                }
            });
            containerSettingsItems.addView(btn);
            if (val == currentValue)
                focusBtn = btn;
        }

        // 현재 설정되어 있는 시간으로 포커스 자동 이동
        if (focusBtn != null)
            focusBtn.requestFocus();
        else if (containerSettingsItems.getChildCount() > 1)
            containerSettingsItems.getChildAt(1).requestFocus();
    }
    // 💡 [화면 꺼짐 전용 수신기] 화면이 꺼진 상태에서 시스템이 버튼 신호를 여기로 쏴줍니다!
    public static class MediaBtnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) && MainActivity.instance != null) {
                KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // 설정에서 스크린 오프가 꺼져있으면 무시합니다.
                    if (!MainActivity.instance.isScreenOffControlEnabled) return;

                    int keyCode = event.getKeyCode();

                    // ⏮ 이전 곡 버튼
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
                        MainActivity.instance.prevTrack();
                        MainActivity.instance.clickFeedback();
                    }
                    // ⏭ 다음 곡 버튼
                    else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
                        MainActivity.instance.nextTrack();
                        MainActivity.instance.clickFeedback();
                    }
                    // ⏯ 재생/일시정지 버튼
                    else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == 86) {
                        MainActivity.instance.playOrPauseMusic();
                        MainActivity.instance.clickFeedback();
                    }
                    // 🔊 혹시 기기가 휠 조작(21, 22)을 미디어 신호로 보내줄 경우를 대비한 방어 코드
                    else if (keyCode == 21) {
                        MainActivity.instance.adjustVolume(false);
                        MainActivity.instance.clickFeedback();
                    }
                    else if (keyCode == 22) {
                        MainActivity.instance.adjustVolume(true);
                        MainActivity.instance.clickFeedback();
                    }
                }
            }
        }
    }
    // 💡 [추가] 곡이 바뀌었을 때 메인 화면의 'Now Playing' 미리보기(앨범아트/제목)를 즉시 새로고침하는 함수
    private void refreshNowPlayingPreview() {
        // 메인 메뉴 화면에 있고, 포커스가 Now Playing 버튼에 맞춰져 있을 때만 작동합니다.
        if (currentScreenState == STATE_MENU && btnNowPlaying != null && btnNowPlaying.hasFocus()) {
            if (lastAlbumArtBytes != null && lastAlbumArtBytes.length > 0) {
                try {
                    android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                    opts.inSampleSize = 2;
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length, opts);
                    ivMenuPreview.setImageBitmap(bmp);
                } catch (Exception e) {
                    ivMenuPreview.setImageResource(R.drawable.music_circle);
                }

                if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                    tvMenuPreviewTitle.setVisibility(View.VISIBLE);
                    tvMenuPreviewArtist.setVisibility(View.VISIBLE);
                    tvMenuPreviewTitle.setText(tvPlayerTitle.getText());
                    tvMenuPreviewArtist.setText(tvPlayerArtist.getText());
                }
            } else {
                ivMenuPreview.setImageResource(R.drawable.music_circle);
                if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                    tvMenuPreviewTitle.setVisibility(View.GONE);
                    tvMenuPreviewArtist.setVisibility(View.GONE);
                }
            }
        }
    }
}