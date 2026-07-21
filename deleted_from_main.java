    private static final String SERVER_BASE_URL = "http://knock2025.cafe24.com/knock_knock/y1/";
    private static final String METADATA_URL = SERVER_BASE_URL + "output-metadata.json";
    private static long lastWakeUpTime = 0;
    private FrameLayout coverFlowContainer;
    private View[] cfViews; // ?뮕 ?ш린???꾨옒 UI ?앹꽦湲곗뿉???숈쟻?쇰줈 寃곗젙?⑸땲??
    private boolean isNavigatingToSubMenu = false; // ?? [?ш린????以?異붽?!] ?ㅼ씠?됲듃 ?묒냽 ???ъ빱??瑗ъ엫??留됰뒗 諛⑹뼱留?+    public boolean isNavigatingToSubMenu = false; // ?? [?ш린????以?異붽?!] ?ㅼ씠?됲듃 ?묒냽 ???ъ빱??瑗ъ엫??留됰뒗 諛⑹뼱留?     // ?? [異붽?] ?ㅻ뵒??梨꾨꼸????떆 ?湲곗떆?ㅻ뒗 ?꾩뿭 蹂??-    private BluetoothProfile globalA2dp;
    private BluetoothDevice targetDeviceForAudio = null; // ?? [異붽?] 醫鍮꾩쿂??臾쇨퀬 ?섏뼱吏??寃?湲곌린
    private boolean isBtConnectingState = false;
    private TextView tvFastScrollLetter;
    private int backTargetForPlayer = STATE_BROWSER;
    private int backTargetForUtility = STATE_SETTINGS;
    private static final int BROWSER_COVER_FLOW = 9;
    private List<SongItem> uniqueAlbumList = new ArrayList<>();
    private int currentCoverFlowIndex = 0;
    private java.util.concurrent.ExecutorService thumbnailExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private Typeface materialIconFont = null;
    private long lastSeekTime = 0;
    private int settingsSubMode = 0; // 0: ?쇰컲, 1: ?좎쭨?쒓컙, 2: ?댄꾨씪?댁? ?쇱슦??+    public int settingsSubMode = 0; // 0: ?쇰컲, 1: ?좎쭨?쒓컙, 2: ?댄꾨씪?댁? ?쇱슦??     public int currentAudioSessionId = -1; // ?? [異붽?] ?꾩옱 ?ъ슜 以묒씤 ?ㅻ뵒???뚯꽑 踰덊샇瑜?湲곗뼲??蹂??-    private int currentAdjustingBand = -1; // ?? [異붽?] 洹몃옒??EQ?먯꽌 ?꾩옱 蹂쇰ⅷ 議곗젅 以묒씤 二쇳뙆?섎? 湲곗뼲?⑸땲??
    private boolean isWidgetFocusImageOn = false; // ?? [異붽?] ?ъ빱???꾩젽 ?꾩썝 蹂??+    public boolean isWidgetFocusImageOn = false; // ?? [異붽?] ?ъ빱???꾩젽 ?꾩썝 蹂??     // ?뮕 [異붽?] ???ㅽ겕由??꾩젽 愿??蹂?섎뱾
    private boolean isWidgetClockOn = false;
    private boolean isWidgetBatteryOn = false;
    private boolean isWidgetAlbumOn = false;
    private boolean isWidgetAnalogClockOn = false;
    private boolean isWidgetCircularBatteryOn = false;
    private boolean isLoopScrollOn = true; // ?뮕 湲곕낯?곸쑝濡?臾댄븳 猷⑦봽媛 ?묐룞?섎룄濡?true ?μ쟾!
    private TextView tvWidgetClock;
    private WidgetBatteryBarView widgetBatteryView;
    private ImageView ivWidgetAlbum;
    private String lastBrowserFocusText = "";
    private TextView tvWidgetAlbumArtist;
    private List<String> currentScrollIndexList = new ArrayList<>();
    private long lastWheelTime = 0;
    private int wheelFastCount = 0;
    private android.media.audiofx.Visualizer audioVisualizer;
    private AudioVisualizerView visualizerView;
    private TextView tvLyrics;
    private java.util.TreeMap<Integer, String> currentLyrics = new java.util.TreeMap<>();
    private List<Integer> lyricTimestamps = new ArrayList<>();
    private int lastLyricIndex = -1;
    private boolean isBottomButtonDown = false;
    private long bottomButtonDownTime = 0;
    private boolean isCenterButtonDown = false;
    private long centerButtonDownTime = 0;
    private boolean isVisualizerShowing = false;
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
    private static final int BROWSER_ROOT = 0;
    private static final int BROWSER_FOLDER = 1;
    private static final int BROWSER_ARTISTS = 2;
    private static final int BROWSER_ALBUMS = 3;
    private ImageView ivStatusPlay;
    private ImageView ivStatusServer; // ?? [?좉퇋 異붽?] ?곹깭諛????쒕쾭 ?꾩씠肄?+    public ImageView ivStatusPlay;
    private static final int BROWSER_FAVORITES = 5;
    private static final int BROWSER_PLAYLISTS = 6;
    private static final int BROWSER_M3U_SONGS = 7;
    private static final int BROWSER_AUDIOBOOKS = 8; // ?? [異붽?] ?ㅻ뵒?ㅻ턿 釉뚮씪?곗? ?곹깭 媛??+    public static final int BROWSER_PLAYLISTS = 6;
    private static final int BROWSER_YEARS = 10;
    private static final int BROWSER_GENRES = 11;
    private static final int BROWSER_RECENTLY_ADDED = 12; // ?? [?좉퇋 ?μ갑] 理쒓렐 異붽???怨??곹깭
    private static final int BROWSER_PODCAST_CHANNELS = 13;
    private static final int BROWSER_PODCAST_EPISODES = 14;
    private static final int BROWSER_PODCAST_MANAGE = 15; // ?? [?좉퇋 異붽?] 援щ룆 愿由??붾㈃ ?꾩슜 踰덊샇??private void
    private static final int BROWSER_VIDEOS = 16; // ?? 鍮꾨뵒??釉뚮씪?곗? ?꾩슜 踰덊샇???좎꽕!
    private String currentPodcastUrl = ""; // ?꾩옱 ?좏깮???잛틦?ㅽ듃 ?듭떊留?二쇱냼
    private File currentM3uFile = null; // ?꾩옱 ?ъ슜?먭? ?ㅼ뿬?ㅻ낫怨??덈뒗 M3U ?뚯씪 二쇱냼李?+    public File currentM3uFile = null; // ?꾩옱 ?ъ슜?먭? ?ㅼ뿬?ㅻ낫怨??덈뒗 M3U ?뚯씪 二쇱냼李?     // ?? [異붽?] 利먭꺼李얘린 ?꾩슜 蹂?섎뱾
    private Set<String> favoritePaths = new HashSet<>();
    private TextView tvPlayerFavoriteStatus;
    private ProgressBar pbLoadingProgress;
    private TextView tvLoadingProgress;
    private int totalAudioFiles = 0;
    private int scannedAudioFiles = 0;
    private ListView listVirtualSongs;
    private View scrollViewBrowser;
    private boolean isScreenOffControlEnabled = false;
    private boolean isCenterLongPressed = false;
    private int lastRadioFocusIndex = 1;
    private boolean isCustomScanning = false;
    private int currentScreenState = STATE_MENU;
    private View layoutMainMenu, layoutBrowserMode, layoutSettingsMode;
    private View layoutBluetoothMode, layoutWifiMode, layoutWifiKeyboard;
    private View layoutPlayerMode, layoutVolumeOverlay;
    private View layoutBrightnessMode, layoutStorageMode, layoutWebServerMode;
    private LinearLayout containerBrowserItems, containerSettingsItems;
    private LinearLayout containerBtItems, containerWifiItems;
    private TextView tvStatusClock, tvStatusBattery;
    private ImageView ivStatusBluetooth, ivStatusWifi, ivStatusHeadphone, ivMainBg;
    private LinearLayout layoutAudioQualityContainer;
    private TextView tvQualityExt;
    private TextView tvQualityFormat;
    private TextView tvQualityBitrate;
    private ImageView ivPlayerShuffleStatus, ivPlayerRepeatStatus; // ?뮕 ?띿뒪?몃럭?먯꽌 ?대?吏酉곕줈 蹂寃?
    private ProgressBar volumeProgress, pbBrightness, pbStorage;
    private TextView tvBrightnessVal, tvStorageDetails;
    private TextView tvServerStatus, tvServerIp;
    private Button btnServerToggle;
    private LinearLayout layoutLoadingOverlay;
    private Button btnNowPlaying, btnPlay, btnSettings, btnBluetooth, btnRadio;
    private Button btnScanBt, btnScanWifi;
    private TextView tvKeyboardSsid, tvKeyboardInput;
    private TextView tvKeyPprev, tvKeyPrev, tvKeyCurrent, tvKeyNext, tvKeyNnext;
    private long lastBtToggleTime = 0;
    private final String[] KEYBOARD_CHARS = {
    private android.media.RemoteControlClient remoteControlClient;
    private ComponentName mediaButtonReceiver;
    private boolean wasWifiOnBeforeSleep = false;
    private int keyboardIndex = 0;
    private String targetWifiSsid = "";
    private String typedPassword = "";
    private boolean isTargetWifiOpen = false;
    private AudioManager audioManager;
    private File rootFolder = new File("/storage/sdcard0/Music");
    private File currentFolder = rootFolder;
    private float currentClockSize = 48f;
    private TextView tvMenuPreviewTitle, tvMenuPreviewArtist;
    private boolean isShuffleMode = false;
    private int repeatMode = 0; // 0: OFF, 1: ONE (Repeat One), 2: ALL (Repeat Folder/All)
    private boolean isSoundEffectEnabled = true;
    private boolean isVibrationEnabled = true;
    private boolean isPickingBackground = false;
    private List<String> eqPresetNames = new ArrayList<String>();
    private int lastSettingsFocusIndex = 0;
    private int currentSettingsDepth = 1;
    private boolean isScreenSleeping = false;
    private long lastScreenOnTime = 0;
    private int currentTimeoutIndex = 1;
    private final int[] TIMEOUT_VALUES = { 15000, 30000, 60000, 300000 };
    private final String[] TIMEOUT_NAMES = { "15 Sec", "30 Sec", "1 Min", "5 Min" };
    private TextView tvFocusPreviewClock; // ?? [?좉퇋 ?붿쭊] ?쇱씠釉??꾨━酉??곸옄 ?대??먯꽌 吏멸퉵嫄곕┫ ?붿????쒓퀎
    private ImageView ivWidgetFocusImage; // ?? [異붽?] ?ㅼ씠?대? ?ъ빱???꾩젽 蹂??+    public BatteryIconView batteryIconView;
    private LinearLayout layoutWidgetAlbumContainer; // ?⑤쾾 ?꾩젽 ?⑹뼱由?二쇱냼
    private int lastMainMenuFocusIndex = 0;
    private int currentSystemBrightness = 255;
    private List<String> foundBtDevices = new ArrayList<String>();
    private List<String> foundWifiNetworks = new ArrayList<String>();
    private Y1WebServer webServer;
    private boolean isServerRunning = false;
    private WifiManager.WifiLock serverWifiLock = null;
    private int vibrationStrengthLevel = 1; // 0: Weak, 1: Normal, 2: Strong
    private final String[] VIBE_STRENGTH_NAMES = { "Weak", "Normal (Vibe)", "Strong" };
    private final int[] VIBE_DURATIONS = { 10, 25, 50 };
    private long lastCenterUpTime = 0;
    private LinearLayout layoutRadioCandyContainer;
    private BroadcastReceiver systemStatusReceiver = new BroadcastReceiver() {
    private void buildThemeSelectorUI() {
                lp.setMargins(0, 0, 0, 30);
    private void startBluetoothScan() {
    private void startWifiScan() {
    private LinearLayout createSettingRow(String leftText, String rightText) {
    private void buildSettingsUI() {
        currentSettingsDepth = 0; // ?? 硫붿씤 ?ㅼ젙? 源딆씠 0

        // ?? [?덉쟾?μ튂] ?쇰컲 ?명똿 ?붾㈃?쇰줈 ?ㅼ뼱?ㅻ㈃ ?쇰뵒??UI ?뚮옒洹몃? ?꾨꼍?섍쾶 ?댁젣?⑸땲??
        isRadioUIShowing = false;
        isRadioSettingsMode = false;

        // ?? [異붽?] ?쇰컲 ?ㅼ젙李쎌쑝濡??뚯븘???뚮뒗 ?④꺼???곷떒 ?쒕ぉ 湲?⑤? ?ㅼ떆 ?꾩썙以띾땲??
        ViewGroup settingsGroup = (ViewGroup) layoutSettingsMode;
        if (settingsGroup != null && settingsGroup.getChildCount() > 0
                && settingsGroup.getChildAt(0) instanceof TextView) {
            settingsGroup.getChildAt(0).setVisibility(View.VISIBLE);
        }

        final int targetFocusIndex = lastSettingsFocusIndex;
        containerSettingsItems.removeAllViews();

        // createCategoryHeader("??QUICK SETTINGS ??);

        final LinearLayout btnShuffle = createSettingRow("Shuffle Mode", isShuffleMode ? t("ON") : t("OFF"));
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isShuffleMode = !isShuffleMode;
                TextView tvStatus = (TextView) btnShuffle.getChildAt(1);
                tvStatus.setText(isShuffleMode ? t("ON") : t("OFF"));
                updatePlayerStatusIndicators();
                try {
                    prefs.edit().putBoolean("shuffle", isShuffleMode).commit();
                } catch (Exception e) {
                }

                // =======================================================
                // ?? [?듭떖 ?붿쭊 ?숆린?? ?ㅼ젣 ?뚯븙???몃뒗 AudioPlayerManager???뷀뵆 紐낅졊???ㅼ씠?됲듃濡??⑸땲??
                // =======================================================
                if (com.themoon.y1.managers.AudioPlayerManager.getInstance() != null) {
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().setShuffleMode(isShuffleMode);
                }

                // (湲곗〈 ?덇굅???대뜑 ?ъ깮???뷀뵆 肄붾뱶???덉쟾???꾪빐 ?좎??⑸땲??
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

        final LinearLayout btnRepeat = createSettingRow("Repeat Mode", t(getRepeatModeText(repeatMode)));
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                repeatMode = (repeatMode + 1) % 3;
                TextView tvStatus = (TextView) btnRepeat.getChildAt(1);
                tvStatus.setText(t(getRepeatModeText(repeatMode)));
                updatePlayerStatusIndicators();
                try {
                    prefs.edit().putInt("repeat_mode", repeatMode).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnRepeat);

        // ?? 1. 硫붿씤 ?ㅼ젙李?EQ ?쒖떆
        String eqDisplayName = "Normal";
        if (currentEqProfile.startsWith("preset_")) {
            int pIdx = Integer.parseInt(currentEqProfile.replace("preset_", ""));
            if (pIdx < eqPresetNames.size())
                eqDisplayName = t(eqPresetNames.get(pIdx)); // ?? OS ?곗씠?곕? 踰덉뿭湲곕줈!
        } else {
            eqDisplayName = currentEqProfile.replace("custom_", ""); // ?? 瑗щ━?쒕룄 踰덉뿭!
        }
        final LinearLayout btnEq = createSettingRow("Equalizer & Audio Effects", eqDisplayName + " ??);

        btnEq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildEqualizerSettingsUI();
            }
        });
        containerSettingsItems.addView(btnEq);
        // ?? [?좉퇋 異붽?] 援ш? ExoPlayer ????ㅽ듃?덉묶 (諛곗냽 ?ъ깮) 而⑦듃濡??ㅼ쐞移?
        final String[] speedLabels = { "1.0x (Normal)", "1.2x (Fast)", "1.5x (Faster)", "2.0x (Very Fast)" };
        final float[] speedValues = { 1.0f, 1.2f, 1.5f, 2.0f };

        // ?꾩옱 ?곸슜??諛곗냽??紐?踰덉㎏ ?몃뜳?ㅼ씤吏 ?뺤씤
        float currentSpd = com.themoon.y1.managers.AudioPlayerManager.getInstance().getCurrentSpeed();
        int spdIdx = 0;
        for (int i = 0; i < speedValues.length; i++) {
            if (speedValues[i] == currentSpd)
                spdIdx = i;
        }

        final LinearLayout btnSpeed = createSettingRow("Playback Speed", t(speedLabels[spdIdx]));
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                float current = com.themoon.y1.managers.AudioPlayerManager.getInstance().getCurrentSpeed();
                int nextIdx = 0;
                for (int i = 0; i < speedValues.length; i++) {
                    if (speedValues[i] == current)
                        nextIdx = (i + 1) % speedValues.length;
                }

                // ?붿쭊???덈줈??諛곗냽 利됱떆 二쇱엯! (?ㅻ엺伊?紐⑹냼由??놁씠 源붾걫?섍쾶 鍮⑤씪吏묐땲??
                com.themoon.y1.managers.AudioPlayerManager.getInstance().setPlaybackSpeed(speedValues[nextIdx]);

                TextView tvStatus = (TextView) btnSpeed.getChildAt(1);
                tvStatus.setText(t(speedLabels[nextIdx])); // ?? ?대┃???뚮룄 諛섎뱶??踰덉뿭湲?t()瑜?嫄곗튂?꾨줉 ?섏젙!
                Toast.makeText(MainActivity.this, t("Speed set to ") + t(speedLabels[nextIdx]),
                        Toast.LENGTH_SHORT).show();
            }
        });
        containerSettingsItems.addView(btnSpeed);
        final LinearLayout btnSound = createSettingRow("Button Sound", isSoundEffectEnabled ? t("ON") : t("OFF"));
        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSoundEffectEnabled = !isSoundEffectEnabled;
                applySoundSetting(); // ?뮕 [?ш린 異붽?] ?ъ슜?먭? ?꾨Ⅴ??利됱떆 ?쒖뒪???뚯냼嫄??쒖뼱
                clickFeedback();
                TextView tvStatus = (TextView) btnSound.getChildAt(1);
                tvStatus.setText(isSoundEffectEnabled ? t("ON") : t("OFF"));
                try {
                    prefs.edit().putBoolean("sound", isSoundEffectEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnSound);

        LinearLayout btnVibrateMenu = createSettingRow("Vibration", "??");
        btnVibrateMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildVibrationSettingsUI(); // ?? ?덈줈 留뚮뱺 吏꾨룞 ?쒕툕 硫붾돱 ?닿린!
            }
        });
        containerSettingsItems.addView(btnVibrateMenu);

        final LinearLayout btnScreenOffCtrl = createSettingRow("Screen-Off Control",
                isScreenOffControlEnabled ? t("ON") : t("OFF"));
        btnScreenOffCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isScreenOffControlEnabled = !isScreenOffControlEnabled;
                TextView tvStatus = (TextView) btnScreenOffCtrl.getChildAt(1);
                tvStatus.setText(isScreenOffControlEnabled ? t("ON") : t("OFF"));
                try {
                    prefs.edit().putBoolean("screen_off_control", isScreenOffControlEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnScreenOffCtrl);
        // ?? [?섏젙???뚮쭏 ?ㅼ젙 踰꾪듉]
        final LinearLayout btnTheme = createSettingRow("Theme", ThemeManager.getCurrentTheme().name);
        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                // ?꾨Ⅴ硫??쒗솚?섏? ?딄퀬, ?꾩껜 ?뚮쭏 由ъ뒪???붾㈃?쇰줈 ?대룞?⑸땲??
                buildThemeSelectorUI();
            }
        });
        containerSettingsItems.addView(btnTheme);

        // ?? [?좉퇋 ?붿쭊] ?닿? ?먰븯??硫붿씤 ?붾㈃ 踰꾪듉留?媛쒕퀎?곸쑝濡??꾧퀬 耳????덈뒗 ?쒕툕 硫붾돱 吏꾩엯湲?-        final LinearLayout btnMenuVisibility = createSettingRow("Main Menu Items", t("Edit") + " ??);
        btnMenuVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildMainMenuVisibilitySettingsUI(); // ?留앹쓽 媛쒕퀎 ?④? ?몄쭛李??몄텧!

                // ?? [?媛쒖“ ?꾨즺] OnGlobalLayoutListener瑜??쒖슜???ㅼ젙 ?붾㈃ 蹂듦? ???쒓컖??紐⑥뀡 ?덉씠?먯떆瑜??꾩쟾???쒓굅?⑸땲??
                containerSettingsItems.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    containerSettingsItems.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                } else {
                                    containerSettingsItems.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                }

                                if (targetFocusIndex >= 0
                                        && targetFocusIndex < containerSettingsItems.getChildCount()) {
                                    View target = containerSettingsItems.getChildAt(targetFocusIndex);
                                    if (containerSettingsItems.getParent() instanceof ScrollView) {
                                        ScrollView sv = (ScrollView) containerSettingsItems
                                                .getParent();
                                        sv.scrollTo(0, target.getTop()); // 泥??꾨젅??異쒕젰 ??誘몃━ ?ㅽ겕濡??뺣젹!
                                    }
                                    target.requestFocus();
                                    lastSettingsFocusIndex = targetFocusIndex;
                                } else if (containerSettingsItems.getChildCount() > 0) {
                                    containerSettingsItems.getChildAt(0).requestFocus();
                                }
                            }
                        });
            }
        });
        containerSettingsItems.addView(btnMenuVisibility);

        // ?? [??猷⑦봽 踰꾧렇 ?섎━] 硫붿씤 ?붾㈃ ?곌껐 怨좊━ 利됱떆 ?덈줈怨좎묠 ?묒옱!
        final LinearLayout btnLoopScrollToggle = createSettingRow("Wheel Loop Scroll",
                isLoopScrollOn ? t("ON") : t("OFF"));
        btnLoopScrollToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isLoopScrollOn = !isLoopScrollOn;
                ((TextView) btnLoopScrollToggle.getChildAt(1)).setText(isLoopScrollOn ? t("ON") : t("OFF"));
                prefs.edit().putBoolean("loop_scroll_on", isLoopScrollOn).commit();

                // ?뮕 [?듭떖 ?닿껐] ?ㅼ쐞移섎? ?꾧굅??耳쒕뒗 利됱떆 諛깃렇?쇱슫?쒖뿉??硫붿씤 ?붾㈃ ?ъ빱??怨좊━留앹쓣 ?ㅼ떆 ??뼱以띾땲??
                // applyThemeToMainMenu();
            }
        });
        containerSettingsItems.addView(btnLoopScrollToggle);
        final LinearLayout btnTimeout = createSettingRow("Screen Timeout", t(TIMEOUT_NAMES[currentTimeoutIndex]));
        btnTimeout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                currentTimeoutIndex = (currentTimeoutIndex + 1) % TIMEOUT_VALUES.length;

                // ?? [?섏젙 ?꾨즺] 踰꾪듉???뚮윭???띿뒪?멸? 諛붾??뚮룄 踰덉뿭湲?t()瑜?臾댁“嫄??듦낵?섎룄濡??뚯썙以띾땲??
                ((TextView) btnTimeout.getChildAt(1)).setText(t(TIMEOUT_NAMES[currentTimeoutIndex]));

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
        // (湲곗〈 ??꾩븘??踰꾪듉 肄붾뱶)
        containerSettingsItems.addView(btnTimeout);

        // (洹??꾨옒???댁뼱吏??Power Off 硫붾돱 ??湲곗〈 肄붾뱶 ?좎?...)

        // createCategoryHeader("??SYSTEM MENUS ??);

        LinearLayout btnPowerOff = createSettingRow("Power Off", "??");
        btnPowerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle(t("Power Off"))
                        .setMessage(t("Do you want to shut down the device?"))
                        .setPositiveButton(t("Shut Down"), new DialogInterface.OnClickListener() {
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
                                        Toast.makeText(MainActivity.this,
                                                t("System security prevents powering off directly from the app."),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        })
                        .setNegativeButton(t("Cancel"), null)
                        .show();
            }
        });
        containerSettingsItems.addView(btnPowerOff);

        LinearLayout btnServerMenu = createSettingRow(t("Web Server"), "??");
        btnServerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_WEBSERVER);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnServerMenu);

        LinearLayout btnWifiMenu = createSettingRow(t("Wi-Fi"), "??");
        btnWifiMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_WIFI);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnWifiMenu);
        // ?? [異붽? 1] ?명꽣?룹뿉???⑤쾾 ?꾪듃 諛?怨??뺣낫 ?먮룞 寃??耳쒓린/?꾧린
        final LinearLayout btnAutoFetch = createSettingRow("Auto Fetch Album Art",
                isAutoFetchEnabled ? t("ON") : t("OFF"));
        btnAutoFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isAutoFetchEnabled = !isAutoFetchEnabled;
                ((TextView) btnAutoFetch.getChildAt(1)).setText(isAutoFetchEnabled ? t("ON") : t("OFF"));
                try {
                    prefs.edit().putBoolean("auto_fetch", isAutoFetchEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnAutoFetch);
        // ?? [?섏젙] 湲곌린???볦씤 ?⑤쾾 ?꾪듃 ?대?吏? ??λ맂 怨??뺣낫(?쒕ぉ, 媛??源뚯? ??踰덉뿉 ??珥덇린?뷀빀?덈떎!
        LinearLayout btnClearCache = createSettingRow("Clear Album Art & Info", "??");
        btnClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle(t("Clear Cache & Track Info"))
                        .setMessage(t("Delete all downloaded album covers and saved track information (Title/Artist)?"))
                        .setPositiveButton(t("Clear"), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    // 1. 臾쇰━?곸씤 ?대?吏 ?뚯씪(?⑤쾾 而ㅻ쾭) ??젣
                                    File coverFolder = new File("/storage/sdcard0/Y1_Covers");
                                    int count = 0;
                                    if (coverFolder.exists()) {
                                        File[] files = coverFolder.listFiles();
                                        if (files != null) {
                                            for (File f : files) {
                                                if (f.isFile() && f.delete())
                                                    count++;
                                            }
                                        }
                                    }

                                    // ?? 2. [?듭떖 異붽?] 湲덇퀬(SharedPreferences)????λ맂 ?쒕ぉ, 媛???뺣낫 ????吏?곌린
                                    SharedPreferences.Editor editor = prefs.edit();
                                    java.util.Map<String, ?> allEntries = prefs.getAll();
                                    for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
                                        String key = entry.getKey();
                                        // "meta_title_", "meta_artist_", "album_art_" 濡??쒖옉?섎뒗 湲곗뼲?ㅻ쭔 怨⑤씪??吏?곷땲??
                                        if (key.startsWith("meta_title_") || key.startsWith("meta_artist_")
                                                || key.startsWith("album_art_")) {
                                            editor.remove(key);
                                        }
                                    }
                                    editor.commit(); // 蹂寃쎌궗???곴뎄 ???

                                    Toast.makeText(MainActivity.this,
                                            "Deleted " + count + " covers & cleared track info.",
                                            Toast.LENGTH_SHORT).show();

                                    // 3. 硫붿씤 ?붾㈃???⑥븘?덈뒗 ?대?吏瑜?湲곕낯 ?꾩씠肄섏쑝濡?珥덇린?뷀빀?덈떎.
                                    ivAlbumArt.setImageResource(R.drawable.default_album);
                                    ivPlayerBgBlur.setImageResource(0);
                                    lastAlbumArtBytes = null;

                                    // ?? 4. [異붽?] ?꾩옱 ??댁졇 ?덈뒗 怨≪쓽 ?쒕ぉ怨?媛?섎룄 ?뚯씪 ?먮낯 ?대쫫?쇰줈 利됱떆 ?섎룎由ш린
                                    if (!currentPlaylist.isEmpty()) {
                                        File currentFile = currentPlaylist.get(currentIndex);
                                        tvPlayerTitle.setText(currentFile.getName());
                                        tvPlayerArtist.setText("Unknown Artist");
                                    }

                                    updateMainMenuBackground();
                                    refreshNowPlayingPreview();
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Failed to clear cache.", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        })
                        .setNegativeButton(t("Cancel"), null)
                        .show();
            }
        });
        containerSettingsItems.addView(btnClearCache);
        LinearLayout btnBtMenu = createSettingRow("Bluetooth", "??");
        btnBtMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_BLUETOOTH);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnBtMenu);

        LinearLayout btnBrightMenu = createSettingRow("Display Brightness", "??");
        btnBrightMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_BRIGHTNESS);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnBrightMenu);

        LinearLayout btnStorageMenu = createSettingRow("Storage", "??");
        btnStorageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScreen(STATE_STORAGE);
                clickFeedback();
            }
        });
        containerSettingsItems.addView(btnStorageMenu);

        // ?? [?섏젙] ?⑹뼱???덈뜕 ??媛吏 諛곌꼍 湲곕뒫??'Background Settings' ?쇰뒗 ?섎굹???쒕툕 硫붾돱濡?臾띠뼱踰꾨┰?덈떎!
        LinearLayout btnBgMenu = createSettingRow("Background", "??");
        btnBgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildBackgroundSettingsUI(); // ?? ?꾩뿉??留뚮뱺 諛곌꼍 ?ㅼ젙 ?쒕툕 硫붾돱瑜??꾩썎?덈떎!
            }
        });
        containerSettingsItems.addView(btnBgMenu);

        LinearLayout btnTime = createSettingRow("Date & Time", "??);
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                // ?쒖뒪???쒓컙??癒쇱? ?쎌뼱????꾩떆 蹂?섏뿉 ??ν빀?덈떎.
                java.util.Calendar c = java.util.Calendar.getInstance();
                dtYear = c.get(java.util.Calendar.YEAR);
                dtMonth = c.get(java.util.Calendar.MONTH) + 1;
                dtDay = c.get(java.util.Calendar.DAY_OF_MONTH);
                dtHour = c.get(java.util.Calendar.HOUR_OF_DAY);
                dtMinute = c.get(java.util.Calendar.MINUTE);

                // ?곕━媛 ?덈줈 留뚮뱺 ?덉걶 由ъ뒪???붾㈃???꾩썎?덈떎!
                buildDateTimeUI();
            }
        });
        containerSettingsItems.addView(btnTime);
        // ?? [?섏젙] 硫붿씤 ?ㅼ젙 ?붾㈃?먯꽌????踰꾩쟾留?媛꾨떒??猿띾뜲湲곗뿉 蹂댁뿬二쇨퀬, ?꾨Ⅴ硫??쒕툕 ?섏씠吏濡??대룞?⑸땲??
        String myVersionName = "1.0";
        try {
            myVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
        }
        // ?? 1. ?꾩옱 湲곌린??鍮꾪뻾湲?紐⑤뱶 ?곹깭瑜??쎌뼱?듬땲?? (?ㅻ━鍮?4.2 湲곗? Global ?명똿)
        boolean isAirplaneModeOn = false;
        try {
            isAirplaneModeOn = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        } catch (Exception e) {
        }
        // ?? [?뷀뀒???섎━] ?붾㈃??蹂댁뿬以??뚮쭔 ".json" 瑗щ━?쒕? 鍮덉뭏("")?쇰줈 ?좊젮踰꾨┰?덈떎!
        String displayLang = LanguageManager.getInstance(this).currentLangFileName
                .replace(".json", "");
        LinearLayout btnLangMenu = createSettingRow("Language", displayLang);
        btnLangMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildLanguageSelectorUI(); // ?몄뼱 ?좏깮 ?쒕툕 硫붾돱 ?닿린
            }
        });
        containerSettingsItems.addView(btnLangMenu);

        LinearLayout btnUpdateCheck = createSettingRow("System Update", "v" + myVersionName);
        btnUpdateCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildUpdateSettingsUI(); // ?? ?앹뾽 ????덈줈 留뚮뱺 ?쒕툕 ?섏씠吏瑜??쎈땲??
            }
        });
        containerSettingsItems.addView(btnUpdateCheck);

        // ?? [?ㅼ젙李??꾩슜 臾닿컧???ㅽ겕濡??붿쭊 ?곸슜]
        final ScrollView sv = (ScrollView) containerSettingsItems.getParent();
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    sv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                if (targetFocusIndex >= 0 && targetFocusIndex < containerSettingsItems.getChildCount()) {
                    View target = containerSettingsItems.getChildAt(targetFocusIndex);

                    // ?? [?먮낯 ?꾩튂 100% 蹂듭썝] ?ㅼ젙李??꾩슜 ?몃뜳?ㅻ줈 ??λ맂 ?ㅽ봽?뗭쓣 爰쇰궡?듬땲??
                    int offset = (sv.getHeight() / 2) - (target.getHeight() / 2);
                    if (exactOffsetMemory.containsKey("SETTINGS_" + targetFocusIndex)) {
                        offset = exactOffsetMemory.get("SETTINGS_" + targetFocusIndex);
                    }

                    int targetY = target.getTop() - offset;
                    if (targetY < 0)
                        targetY = 0;

                    sv.scrollTo(0, targetY);
                    target.requestFocus();
                    lastSettingsFocusIndex = targetFocusIndex;
                } else if (containerSettingsItems.getChildCount() > 0) {
                    containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        });
    } // buildSettingsUI ?⑥닔 ?? buildSettingsUI ?⑥닔 ??-      // ?뮕 [?좉퇋 異붽?] ?몄뼱???좏깮 ?꾩슜 ?붾㈃
    private void buildLanguageSelectorUI() {
    private void buildUpdateSettingsUI() {
    private void buildVibrationSettingsUI() {
    private void buildBackgroundSettingsUI() {
    private boolean isAudioFile(File f) {
    private boolean isApkFile(File f) {
    private boolean isImageFile(File f) {
    private char getInitialChar(String text) {
    private float getTransXForDist(int dist, float d) {
    private float getRotYForDist(int dist) {
    private float getScaleForDist(int dist) {
    private float getAlphaForDist(int dist) {
    private View createSingleCoverView() {
    private Bitmap getReflectionBitmap(Bitmap src) {
    private long lastCoverFlowTime = 0; // ?? ?ㅻ쭏??蹂?띿슜 ??꾨㉧??蹂??+    public long lastCoverFlowTime = 0; // ?? ?ㅻ쭏??蹂?띿슜 ??꾨㉧??蹂?? 
    private int parseGravity(String gravityStr) {
    private FrameLayout.LayoutParams createDynamicLayoutParams(ThemeManager.MenuElement el,
    private GradientDrawable createDynamicButtonBackground(int color, int elementRadius) {
    private GradientDrawable createWidgetBackground(String bgColorStr, int elementRadius) {
    private String extractEmbeddedLyrics(File file) {
    private String getRepeatModeText(int mode) {
    private void updatePlayerStatusIndicators() {
    private String formatTime(int ms) {
    private void applySoundSetting() {
    private void buildDateTimeUI() {
        private static long lastWakeUpTime = 0;
        private javax.net.ssl.SSLSocketFactory internalSSLSocketFactory;
        private java.net.Socket enableTLSOnSocket(java.net.Socket socket) {
    private void buildEqualizerSettingsUI() {
    private List<SongItem> parseM3uFile(File m3uFile) {
    private void buildMainMenuVisibilitySettingsUI() {
