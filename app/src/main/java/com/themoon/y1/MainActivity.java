package com.themoon.y1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.themoon.y1.adapters.CategoryListAdapter;
import com.themoon.y1.adapters.SongListAdapter;
import com.themoon.y1.managers.LanguageManager;
import com.themoon.y1.models.SongItem;
import com.themoon.y1.views.AudioVisualizerView;
import com.themoon.y1.views.BatteryIconView;
import com.themoon.y1.views.CircularBatteryView;
import com.themoon.y1.views.CustomAnalogClockView;
import com.themoon.y1.views.EqSliderView;
import com.themoon.y1.views.PieChartView;
import com.themoon.y1.views.WidgetBatteryBarView;

import org.conscrypt.Conscrypt;

public class MainActivity extends Activity {
    // 주의: 주소 맨 끝에 반드시 슬래시(/)를 붙여주세요!
    private static final String SERVER_BASE_URL = "http://knock2025.cafe24.com/knock_knock/y1/";
    private static final String METADATA_URL = SERVER_BASE_URL + "output-metadata.json";
    // 🚀 [대개조 완료] 원하는 앨범 개수(홀수: 3, 5, 7 등)를 언제든 설정할 수 있는 스마트 제어판
    private int visibleCoversCount = 7; // 💡 5개로 복귀! 테스트 시 7 등으로 여기만 바꾸면 전체 자동 연동됩니다.
    private static long lastWakeUpTime = 0;
    private FrameLayout coverFlowContainer;
    private View[] cfViews; // 💡 크기는 아래 UI 생성기에서 동적으로 결정됩니다.
    private static final int BROWSER_ARTIST_ALBUMS = 17; // 🚀 가수 -> 앨범 전용 화면 모드
    // 🚀 [추가] 셔플/반복 설정 팝업 오버레이 변수
    public LinearLayout layoutShuffleRepeatOverlay;
    public Button btnOverlayShuffle;
    public Button btnOverlayRepeat;
    // =========================================================
    // 🚀 [신규 추가] 연도/장르 3단 딥 다이브 전용 모드 및 기억 금고!
    // =========================================================
    private static final int BROWSER_YEAR_ARTISTS = 18;
    private static final int BROWSER_YEAR_ALBUMS = 19;
    private static final int BROWSER_GENRE_ARTISTS = 20;
    private static final int BROWSER_GENRE_ALBUMS = 21;
    public String yearOrGenreFilter = ""; // 선택한 연도/장르를 기억할 메모장
    // =========================================================
    public String artistForAlbumFilter = ""; // 🚀 선택한 가수 이름을 기억해둘 메모장
    public boolean isArtistAlbumMode = false; // 🚀 아티스트->앨범 직결 모드 스위치
    private boolean isNavigatingToSubMenu = false; // 🚀 [여기에 한 줄 추가!] 다이렉트 접속 시 포커스 꼬임을 막는 방어막
    // 🚀 [추가] 오디오 채널을 항시 대기시키는 전역 변수
    private BluetoothProfile globalA2dp;
    private BluetoothDevice targetDeviceForAudio = null; // 🚀 [추가] 좀비처럼 물고 늘어질 타겟 기기
    private boolean isBtConnectingState = false;

    // 🚀 [추가] 스펙트럼/가사 모드 전용 하단 고정 프로그레스 바 변수
    public LinearLayout layoutVisProgress;
    public ProgressBar visProgressBar;
    public TextView tvVisTimeCurrent, tvVisTimeTotal;

    // 💡 [추가] 퀵 스크롤 (알파벳 인덱스) 관련 변수들
    private TextView tvFastScrollLetter;
    private Handler fastScrollHandler = new Handler();
    private Runnable hideFastScrollTask = new Runnable() {
        @Override
        public void run() {
            if (tvFastScrollLetter != null) {
                tvFastScrollLetter.setVisibility(View.GONE);
            }
        }
    };

    public String currentPlayerBgMode = "blur"; // 🚀 플레이어 배경 모드 (blur, clear, color, none)
    // =======================================================
    // 🚀 [5초 꾹 누르기 강제 재부팅 엔진] 타이머 및 실행 명령
    // =======================================================
    private Handler rebootHandler = new Handler();
    private Runnable rebootTask = new Runnable() {
        @Override
        public void run() {
            try {
                // 1. 재부팅된다는 걸 손맛으로 알 수 있게 0.5초 동안 아주 강한 진동을 울립니다!
                android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null)
                    v.vibrate(500);

                Toast.makeText(MainActivity.this, "🚨 reboot...", Toast.LENGTH_LONG).show();

                // 2. 루팅(su) 권한을 이용해 안드로이드 코어에 다이렉트 재부팅 명령 발사!
                Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot" });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    // 🚀 [신규 추가] 가상 암전 화면 끄기 제어 스위치
    public boolean isFakeScreenOff = false;
    // 🚀 [신규 추가] 플레이리스트에 메인 메뉴에서 숏컷으로 들어왔는지, 라이브러리를 거쳐서 들어왔는지 구분하는 지능형 플래그!
    public boolean isPlaylistOpenedFromLibrary = false;
    // 🚀 [여기에 추가!] 팟캐스트 전용 내비게이션 깃발!
    public boolean isPodcastOpenedFromLibrary = false;
    public boolean isVideoOpenedFromLibrary = false;

    // 🚀 [신규 추가] 다이렉트 숏컷 뒤로 가기 복귀 경로 추적기!
    private int backTargetForPlayer = STATE_BROWSER;
    private int backTargetForUtility = STATE_SETTINGS;
    // 🚀 [신규 추가] 가상 암전이 깨어날 때 가짜 클릭 이벤트가 터지는 것을 막아주는 방어막
    public boolean ignoreNextKeyUp = false;
    // 🚀 [신규 추가] 라디오 통제용 변수들
    public int activePlayer = 0; // 0: 음악 플레이어, 1: 라디오
    public boolean isRadioScanning = false;
    public List<Float> savedRadioStations = new ArrayList<>();

    private static final int BROWSER_COVER_FLOW = 9;
    private List<SongItem> uniqueAlbumList = new ArrayList<>();
    private int currentCoverFlowIndex = 0;
    // 🚀 [신규 엔진] 비디오 썸네일 백그라운드 전담 일꾼 (화면 멈춤 방지)
    private java.util.concurrent.ExecutorService thumbnailExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

    // 🚀 [메모리 엔진] 앨범 아트 로딩용 스레드 풀 (메모리 폭발 방지 및 동시 로딩 제한)
    private java.util.concurrent.ExecutorService coverFlowExecutor = java.util.concurrent.Executors.newFixedThreadPool(3);

    // 🚀 [메모리 엔진] OOM 방지용 이미지 리사이징 헬퍼
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
    }

    // 🚀 [통합 엔진] 라디오와 음악 플레이어 중 누가 켜져 있든 상태바(ivStatusPlay)를 완벽하게 동기화합니다!
    public void updateGlobalStatusPlayIcon() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager
                            .getInstance(MainActivity.this);
                    com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager
                            .getInstance();

                    // 💡 음악이 재생 중이면 제어권은 무조건 음악! (라디오 강제 종료)
                    if (am.isPlaying()) {
                        activePlayer = 0;
                        if (fm.isPowerUp)
                            fm.powerDown();
                    } else if (fm.isPowerUp) {
                        activePlayer = 1; // 💡 라디오가 켜져 있으면 제어권은 라디오!
                    }

                    if (tvStatusPlay != null) {
                        if (fm.isPowerUp || am.isPlaying()) {
                            tvStatusPlay.setVisibility(View.VISIBLE);
                            tvStatusPlay.setText("▶"); // 💡 재생 기호
                        } else {
                            // 둘 다 꺼져있을 때
                            if (currentPlaylist.isEmpty() && activePlayer == 0) {
                                tvStatusPlay.setVisibility(View.GONE);
                            } else {
                                tvStatusPlay.setVisibility(View.VISIBLE);
                                tvStatusPlay.setText("❚❚"); // 💡 정지(일시정지) 기호 (알파벳이나 파이프 기호 2개)
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    // 🚀 [신규 도구] 하드웨어 버튼용 좌/우 저장된 채널 점프 기능
    private void tuneToNextSavedRadioChannel(boolean isNext) {
        com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
        if (savedRadioStations.isEmpty()) {
            Toast.makeText(this, t("No saved channels."), Toast.LENGTH_SHORT).show();
            return;
        }
        float target = savedRadioStations.get(0);
        if (isNext) {
            for (float f : savedRadioStations) {
                if (f > fm.currentFreq) {
                    target = f;
                    break;
                }
            }
        } else {
            target = savedRadioStations.get(savedRadioStations.size() - 1);
            for (int i = savedRadioStations.size() - 1; i >= 0; i--) {
                if (savedRadioStations.get(i) < fm.currentFreq) {
                    target = savedRadioStations.get(i);
                    break;
                }
            }
        }
        if (fm.isPowerUp)
            fm.tune(target);
        else
            fm.currentFreq = target;

        // 🚀 [수정 완료] 전체 리로드를 차단하고, 플레이어 화면일 때는 초고속 부분 새로고침만 작동시켜 깜빡임을 방지합니다!
        if (currentScreenState == STATE_SETTINGS) {
            if (isRadioUIShowing && !isRadioSettingsMode) {
                updateRadioMainPlayerUI();
            } else {
                buildRadioUI();
            }
        }
    }

    // 🚀 [신규 추가] 머티리얼 아이콘 폰트를 담아둘 메모리 공간
    private Typeface materialIconFont = null;
    public boolean isLongPressConsumed = false; // 🚀 롱클릭 방어막 변수 추가
    public boolean isMediaLongPressConsumed = false; // 🚀 [신규 추가] 미디어 버튼 롱클릭 방어막
    private boolean isSeekPerformed = false;
    private long lastSeekTime = 0;
    // 🚀 [신규 추가] 오디오 이펙트 전역 변수 및 프로필 상태 관리
    public android.media.audiofx.BassBoost bassBoost;
    public android.media.audiofx.Virtualizer virtualizer;
    public int currentBassBoostStep = 0; // 0: OFF, 1: Weak, 2: Normal, 3: Strong
    public int currentVirtualizerStep = 0; // 0: OFF, 1: Weak, 2: Normal, 3: Strong

    public int currentCrossfeedStep = 0; // 0: OFF, 1: Weak, 2: Normal, 3: Strong
    public String currentEqProfile = "preset_0"; // preset_0~X 혹은 custom_이름
    public int[] customBandLevels = new int[32]; // 커스텀 튜닝값 캐시 뱅크
    private int settingsSubMode = 0; // 0: 일반, 1: 날짜시간, 2: 이퀄라이저 라우팅
    public int currentAudioSessionId = -1; // 🚀 [추가] 현재 사용 중인 오디오 회선 번호를 기억할 변수
    private int currentAdjustingBand = -1; // 🚀 [추가] 그래픽 EQ에서 현재 볼륨 조절 중인 주파수를 기억합니다.

    // 🚀 [신규 추가] 소프트웨어 10밴드 EQ 모드 전원 스위치 (기본값은 무조건 안전한 false!)
    public boolean isSoftwareEqEnabled = false;

    private boolean isWidgetFocusImageOn = false; // 🚀 [추가] 포커스 위젯 전원 변수
    // 💡 [추가] 홈 스크린 위젯 관련 변수들
    private boolean isWidgetClockOn = false;
    private boolean isWidgetBatteryOn = false;
    private boolean isWidgetAlbumOn = false;
    private boolean isWidgetAnalogClockOn = false;
    private boolean isWidgetCircularBatteryOn = false;

    // 🚀 [신규 엔진 제어 변수] 리스트 박스 숨김 및 루프 스크롤 스위치
    public android.widget.HorizontalScrollView hzIndexScroll;
    public LinearLayout layoutIndexContainer;
    public LinearLayout listContainer; // 🚀 이거 꼭 추가해 줘!
    public boolean isLoopScrollOn = true; // 💡 기본적으로 무한 루프가 작동하도록 true 장전!
    private TextView tvWidgetClock;
    // 🚀 [수정] 가로형 바(Bar) 클래스로 이름 변경!
    private WidgetBatteryBarView widgetBatteryView;
    // 다른 위젯 변수들이 선언된 곳 (예: WidgetBatteryBarView widgetBatteryView; 등) 근처에 아래 줄을
    // 추가합니다.
    CircularBatteryView customCircularBatteryView;
    CustomAnalogClockView customAnalogClockView;
    private ImageView ivWidgetAlbum;
    private String lastBrowserFocusText = "";
    // 🚀 [추가] 앨범 위젯 전용 제목/가수 변수
    private TextView tvWidgetAlbumTitle;
    private TextView tvWidgetAlbumArtist;
    // 💡 [추가] 고속 인덱스 점프(알파벳 스크롤) 전용 변수들
    private List<String> currentScrollIndexList = new ArrayList<>();
    private long lastWheelTime = 0;
    private int wheelFastCount = 0;
    public static MainActivity instance;
    public long lastTrackChangeTime = 0; // 🚀 기기의 중복 키 신호를 막아줄 방어막 변수
    // 💡 [추가] 오디오 스펙트럼 관련 변수들
    private android.media.audiofx.Visualizer audioVisualizer;
    private AudioVisualizerView visualizerView;
    // 🚀 [신규 추가] LRC 가사 파서 및 UI 변수
    private ScrollView lyricScrollView;
    private TextView tvLyrics;
    private java.util.TreeMap<Integer, String> currentLyrics = new java.util.TreeMap<>();
    private List<Integer> lyricTimestamps = new ArrayList<>();
    private int lastLyricIndex = -1;
    // 💡 MP3 내부에 들어있는 '동기화 안 된' 통짜 가사를 담아두는 바구니
    private String plainLyrics = null;

    private boolean isBottomButtonDown = false;
    private long bottomButtonDownTime = 0;
    private boolean isCenterButtonDown = false;
    private long centerButtonDownTime = 0;

    private boolean isVisualizerShowing = false;
    public int currentAlbumColor = 0xFFFFFFFF; // 스펙트럼 바의 색상
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
    public static final int BROWSER_VIRTUAL_SONGS = 4;
    private static final int BROWSER_GAMES = 10;
    // 💡 [추가] 손상되어 앱을 터뜨린 '독약 파일'들을 기억하는 블랙리스트
    private Set<String> blacklist = new HashSet<>();
    public int currentBrowserMode = BROWSER_ROOT;
    public String virtualQueryType = "";
    public String virtualQueryValue = "";
    public List<File> virtualSongList = new ArrayList<>();
    // 💡 백그라운드 미디어 제어권(스크린 오프) 변수
    // 🚀 [팟캐스트 전용] 다운로드 고유 ID와 진행률(%)을 실시간으로 추적하는 지능형 메모장
    public java.util.HashMap<String, Long> activePodcastDownloads = new java.util.HashMap<>();
    public java.util.HashMap<String, Integer> podcastDownloadProgress = new java.util.HashMap<>();
    private TextView tvStatusPlay;
    private ImageView ivStatusServer; // 🚀 [신규 추가] 상태바 웹 서버 아이콘

    // 💡 미디어 라이브러리 브라우저 상태 관리 변수들 근처에 추가
    private static final int BROWSER_FAVORITES = 5;
    // 🚀 [네이티브 M3U 전용 가상 브라우저 모드 신설]
    private static final int BROWSER_PLAYLISTS = 6;
    private static final int BROWSER_M3U_SONGS = 7;
    private static final int BROWSER_AUDIOBOOKS = 8; // 🚀 [추가] 오디오북 브라우저 상태 가동
    // 🚀 [신규 상수 등록] 겹치지 않는 고유 번호로 연도와 장르 상태 스위치를 장전합니다.
    private static final int BROWSER_YEARS = 10;
    private static final int BROWSER_GENRES = 11;
    private static final int BROWSER_RECENTLY_ADDED = 12; // 🚀 [신규 장착] 최근 추가된 곡 상태
    // 🚀 [팟캐스트 엔진] 전용 상태 변수 추가!
    private static final int BROWSER_PODCAST_CHANNELS = 13;
    private static final int BROWSER_PODCAST_EPISODES = 14;
    private static final int BROWSER_PODCAST_MANAGE = 15; // 🚀 [신규 추가] 구독 관리 화면 전용 번호표!private void
    // 💡 기존 가상 브라우저 모드 상수들이 모여있는 곳에 슬쩍 추가해 줘!
    private static final int BROWSER_VIDEOS = 16; // 🚀 비디오 브라우저 전용 번호표 신설!
    private String currentPodcastUrl = ""; // 현재 선택한 팟캐스트 통신망 주소

    // 🚀 [신규 추가] 커버 플로우 상태 상수 및 데이터 저장소
    // private static final int BROWSER_COVER_FLOW = 9;
    // private java.util.List<SongItem> uniqueAlbumList = new
    // java.util.ArrayList<>();
    // private int currentCoverFlowIndex = 0;
    private File currentM3uFile = null; // 현재 사용자가 들여다보고 있는 M3U 파일 주소창
    // 🚀 [추가] 즐겨찾기 전용 변수들
    private Set<String> favoritePaths = new HashSet<>();
    private TextView tvPlayerFavoriteStatus;

    public int consecutiveErrorCount = 0;
    // 🚀 [추가] 스캔 진행률 표시용 변수들
    private ProgressBar pbLoadingProgress;
    private TextView tvLoadingProgress;
    private int totalAudioFiles = 0;
    private int scannedAudioFiles = 0;
    // 💡 [초고속 엔진] 수천 곡을 버티기 위한 재활용 리스트뷰와 기존 스크롤뷰
    private ListView listVirtualSongs;
    private View scrollViewBrowser;
    public boolean isScreenOffControlEnabled = false;
    public boolean isAutoFetchEnabled = true; // 🚀 [추가] 인터넷 자동 검색 스위치 기본값
    public static List<SongItem> customLibrary = new ArrayList<>();
    public static List<SongItem> audiobookLibrary = new ArrayList<>(); // 🚀 오디오북 전용 바구니 신설!
    private boolean isCenterLongPressed = false;
    public boolean isAudiobookLibraryMode = false; // 🚀 현재 무슨 모드인지 기억하는 스위치
    public File audiobookRootFolder = StoragePaths.getAudiobooksDir(); // 🚀 오디오북 전용 루트 폴더
    // 🚀 [추가] 내장 라디오 전용 지능형 조작 변수 뱅크
    // 🚀 [추가] 내장 라디오 전용 지능형 조작 변수 뱅크 (모달 UI 업그레이드!)
    public boolean isRadioUIShowing = false; // 현재 화면이 라디오인지 판별
    public boolean isRadioSettingsMode = false; // 라디오 내에서 설정 모드인지 판별
    public boolean isRadioAdjustingFreq = false;

    private int lastRadioFocusIndex = 1;
    // (볼륨 전용 변수와 복잡한 포커스 인덱스는 이제 필요 없으므로 과감히 삭제!)
    private boolean isCustomScanning = false;
    public static java.util.HashMap<String, Integer> trackNumberMap = new java.util.HashMap<>();
    private int currentScreenState = STATE_MENU;
    // 💡 자체 날짜/시간 설정용 임시 변수
    public int dtYear = 2026, dtMonth = 1, dtDay = 1, dtHour = 12, dtMinute = 0;
    public View layoutMainMenu, layoutBrowserMode, layoutSettingsMode;
    private View layoutBluetoothMode, layoutWifiMode, layoutWifiKeyboard;
    private View layoutPlayerMode, layoutVolumeOverlay;
    private View layoutBrightnessMode, layoutStorageMode, layoutWebServerMode;

    public LinearLayout containerBrowserItems, containerSettingsItems;
    private LinearLayout containerBtItems, containerWifiItems;

    private TextView tvStatusClock, tvStatusBattery;
    private ImageView ivStatusBluetooth, ivStatusWifi, ivStatusHeadphone, ivMainBg;

    public TextView tvBrowserPath, tvPlayerTitle, tvPlayerArtist, tvPlayerAlbum, tvPlayerTimeCurrent, tvPlayerTimeTotal;
    // 🚀 [캡슐 UI 전용 변수들]
    private LinearLayout layoutAudioQualityContainer;
    private TextView tvQualityExt;
    private TextView tvQualityFormat;
    private TextView tvQualityBitrate;

    public TextView tvPlayerTrackCount;
    private ImageView ivPlayerShuffleStatus, ivPlayerRepeatStatus; // 💡 텍스트뷰에서 이미지뷰로 변경!
    public ProgressBar playerProgress;
    private ProgressBar volumeProgress, pbBrightness, pbStorage;
    private TextView tvBrightnessVal, tvStorageDetails;
    // 💡 [수정] 수동 APP_VERSION 변수는 지우고 서버 폴더 주소만 적습니다.
    public boolean is24HourFormat = false;
    private TextView tvServerStatus, tvServerIp;
    private Button btnServerToggle;
    // 🚀 [추가] 화면 전체를 덮는 고급 로딩 인디케이터 오버레이
    private LinearLayout layoutLoadingOverlay;
    public ImageView ivMenuPreview, ivAlbumArt, ivPlayerBgBlur, ivPauseOverlay;
    // 🚀 [신규 엔진] 메인 메뉴 순서 변경(Reorder)을 위한 전역 변수들
    public boolean isMenuReorderMode = false;
    public View currentReorderRow = null;
    private Button btnNowPlaying, btnPlay, btnSettings, btnBluetooth, btnRadio;
    private Button btnScanBt, btnScanWifi;

    private TextView tvKeyboardSsid, tvKeyboardInput;
    private TextView tvKeyPprev, tvKeyPrev, tvKeyCurrent, tvKeyNext, tvKeyNnext;
    private long lastBtToggleTime = 0;

    public int currentKeyboardMode = 0;

    private final String[] KEYBOARD_CHARS = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u",
            "v", "w", "x", "y", "z",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "!", "@", "#", "$", "%", "^", "&", "*", "-", "_", "+", "=", ".", "?",
            "[SPACE]", "[DEL]", "[CONN]"
    };

    private android.media.RemoteControlClient remoteControlClient;
    private ComponentName mediaButtonReceiver;

    private boolean wasWifiOnBeforeSleep = false;
    private int keyboardIndex = 0;
    private String targetWifiSsid = "";
    private String typedPassword = "";
    private boolean isTargetWifiOpen = false;
    // 💡 미디어 스캐너가 현재 작업 중인지 추적하는 변수
    private boolean isMediaScanning = false;
    private com.themoon.y1.managers.ExternalSdMountMonitor externalSdMountMonitor;
    private AudioManager audioManager;
    private File rootFolder = StoragePaths.getMusicDir();
    private File currentFolder = rootFolder;
    public List<File> originalPlaylist = new ArrayList<File>();
    public List<File> currentPlaylist = new ArrayList<File>();
    public int currentIndex = 0;
    public boolean isPausedByHand = true;
    private float currentClockSize = 48f;
    public java.io.FileInputStream currentFileInputStream = null;
    private TextView tvMenuPreviewTitle, tvMenuPreviewArtist;
    public SharedPreferences prefs;
    public boolean isShuffleMode = false;
    public int repeatMode = 0; // 0: OFF, 1: ONE (Repeat One), 2: ALL (Repeat Folder/All)
    public boolean isSoundEffectEnabled = true;
    private boolean isVibrationEnabled = true;
    private boolean isPickingBackground = false;

    // 💡 마지막으로 재생된 앨범 아트를 기억하는 변수
    public byte[] lastAlbumArtBytes = null;
    // 💡 이퀄라이저 관련 변수 추가
    public Equalizer equalizer;
    public List<String> eqPresetNames = new ArrayList<String>();
    public int currentEqPresetIndex = 0;

    private int lastSettingsFocusIndex = 0;
    public int currentSettingsDepth = 0;
    private boolean isScreenSleeping = false;
    private long lastScreenOnTime = 0;
    // 💡 [추가] 커스텀 배터리 뷰 변수
    public BatteryIconView batteryIconView;
    public int currentBatteryStyleIndex = 0;
    public final String[] BATTERY_STYLE_NAMES = { "Icon Only", "Percent Only", "Icon + Percent" };
    public int currentTimeoutIndex = 1;
    public final int[] TIMEOUT_VALUES = { 15000, 30000, 60000, 300000 };
    public final String[] TIMEOUT_NAMES = { "15 Sec", "30 Sec", "1 Min", "5 Min" };
    private TextView tvFocusPreviewClock; // 🚀 [신규 엔진] 라이브 프리뷰 상자 내부에서 째깍거릴 디지털 시계
    private ImageView ivWidgetFocusImage; // 🚀 [추가] 다이내믹 포커스 위젯 변수

    // 🚀 [신규 엔진 변수] 기존 위젯의 몸체와 원래 좌표를 기억해 둘 백업 금고
    private LinearLayout layoutWidgetAlbumContainer; // 앨범 위젯 덩어리 주소
    // 🚀 [신규 전역 변수] 메인 메뉴에서 마지막으로 포커스되었던 버튼의 인덱스를 기억하는 블랙박스 장전!
    private int lastMainMenuFocusIndex = 0;
    // 🚀 [추가] 모든 위젯의 메모리를 전역에서 관리할 통합 주소록
    public java.util.HashMap<View, ThemeManager.MenuElement> widgetViewRegistry = new java.util.HashMap<>();
    private int currentSystemBrightness = 255;

    private List<String> foundBtDevices = new ArrayList<String>();
    private List<String> foundWifiNetworks = new ArrayList<String>();

    private Y1WebServer webServer;
    private boolean isServerRunning = false;

    // 🚀 [신규 전역 변수] 웹 서버 백그라운드 유지용 시스템 자물쇠
    private PowerManager.WakeLock serverWakeLock = null;
    private WifiManager.WifiLock serverWifiLock = null;

    private int vibrationStrengthLevel = 1; // 0: Weak, 1: Normal, 2: Strong
    // 🚀 괄호를 덧붙여서 이퀄라이저의 Normal과 완전히 다른 Key로 분리 독립시킵니다!
    private final String[] VIBE_STRENGTH_NAMES = { "Weak", "Normal (Vibe)", "Strong" };
    // 💡 핵심: 10ms(아주 짧게 튕김), 25ms(일반적인 휠), 50ms(묵직하게 울림)
    private final int[] VIBE_DURATIONS = { 10, 25, 50 };
    private Handler clockHandler = new Handler();
    private Runnable clockTask = new Runnable() {
        @Override
        public void run() {
            String timeFormat = is24HourFormat ? "HH:mm" : "hh:mm a";
            SimpleDateFormat sdf = new SimpleDateFormat(timeFormat, Locale.US);
            tvStatusClock.setText(sdf.format(new Date()));

            // 🚀 [라이브 엔진] 프리뷰 내부 시계가 화면에 노출 중(VISIBLE)이라면 매초 실시간으로 시간을 갈아끼워 째깍이게 만듭니다!
            if (tvFocusPreviewClock != null && tvFocusPreviewClock.getVisibility() == View.VISIBLE) {
                tvFocusPreviewClock.setText(sdf.format(new Date()));
            }

            refreshWidgets(); // 홈 스크린 위젯 동시 새로고침
            clockHandler.postDelayed(this, 1000);
        }
    };
    // 🚀 [신규 추가] 알약 UI 5초 자동 숨김 타이머 엔진
    private Handler qualityInfoHandler = new Handler();
    private Runnable hideQualityInfoTask = new Runnable() {
        @Override
        public void run() {
            // 타이머가 발동하면 알약 컨테이너를 화면에서 완전히 숨깁니다!
            if (layoutAudioQualityContainer != null) {
                layoutAudioQualityContainer.setVisibility(View.GONE);
            }
        }
    };
    // 🚀 [추가] 글로벌 더블 클릭 및 루트 전원 제어용 변수 뱅크
    private Handler doubleClickHandler = new Handler();
    private long lastCenterUpTime = 0;
    private Runnable singleClickRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                handleCenterShortClick();
            } catch (Exception e) {
            }
        }
    };

    // 🚀 [추가] 긴 영어 문장을 씌우기 편하도록 짧은 번역 도구를 둡니다.
    public String t(String text) {
        return LanguageManager.getInstance(this).t(text);
    }

    // 🚀 [신규 엔진] 와이파이 전원 관리용 전담 지연 타이머
    private Handler wifiPowerHandler = new Handler();

    // 🚀 [블랙스크린 버그 완벽 수리] 화면 전환 시 기기 멈춤 방지
    private void autoManageWifiPower(final boolean isGoingToSleep) {
        wifiPowerHandler.removeCallbacksAndMessages(null); // 기존 예약된 명령 취소

        wifiPowerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm == null)
                    return;

                int state = wm.getWifiState();

                // 🚨 [핵심 방어막] 와이파이가 켜지거나 꺼지는 도중(Wait...)일 때 건드리면 기기가 데드락에 빠집니다!
                if (state == WifiManager.WIFI_STATE_ENABLING || state == WifiManager.WIFI_STATE_DISABLING) {
                    wifiPowerHandler.postDelayed(this, 1500); // 1.5초 뒤에 다시 확인해서 처리!
                    return;
                }

                if (isGoingToSleep) {
                    if (state == WifiManager.WIFI_STATE_ENABLED) {
                        wasWifiOnBeforeSleep = true;
                        if (!isServerRunning) {
                            wm.setWifiEnabled(false); // 안전하게 끄기
                        }
                    } else {
                        wasWifiOnBeforeSleep = false;
                    }
                } else {
                    // 깨어날 때: 잠들기 전에 켜져 있었고, 지금 완전히 꺼진 상태라면 다시 켭니다.
                    if (wasWifiOnBeforeSleep && state == WifiManager.WIFI_STATE_DISABLED) {
                        wm.setWifiEnabled(true); // 안전하게 켜기
                    }
                }
            }
        }, 2500); // 🚀 [안전 지연] 화면이 완전히 꺼지거나 켜진 후 2.5초 뒤에 작동시켜 디스플레이 드라이버와의 충돌을 원천 차단!
    }

    // =======================================================
    // 🚀 [리플렉션 엔진] 화면 끄기 (젤리빈 호환성 + 전원 키 강제 전송)
    // =======================================================
    public void turnOffScreen() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.getClass().getMethod("goToSleep", long.class).invoke(pm, SystemClock.uptimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
            // 💡 223(Sleep)이 안 먹히는 기기를 위해, 무조건 작동하는 26(Power) 신호로 강제 소등!
            try {
                Runtime.getRuntime().exec(new String[] { "su", "-c", "input keyevent 26" });
            } catch (Exception ex) {
            }
        }
    }

    // 🚀 [신규 엔진] 리로드 없이 주파수와 사탕 캡슐 색상만 초고속으로 갈아끼우는 무감쇠 엔진
    private LinearLayout layoutRadioCandyContainer;

    // 🚀 [신규 엔진] 리로드 없이 주파수와 사탕 캡슐 색상만 초고속으로 갈아끼우는 무감쇠 엔진 (좌측 탈출 버그 완벽 수리)
    private void updateRadioMainPlayerUI() {
        final com.themoon.y1.managers.FmRadioManager fmManager = com.themoon.y1.managers.FmRadioManager
                .getInstance(this);

        // 1. 메인 대형 주파수 전광판 텍스트만 콕 집어서 새로고침
        TextView tvFreq = (TextView) containerSettingsItems.findViewWithTag("radio_main_freq_text");
        if (tvFreq != null) {
            tvFreq.setText(String.format(Locale.US, "%.1f MHz", fmManager.currentFreq));
            tvFreq.setTextColor(
                    fmManager.isPowerUp ? (ThemeManager.getListButtonFocusedBg() | 0xFF000000) : 0xFF888888);
        }

        // 2. 사탕 주머니 안의 알갱이들 배경색과 글자색만 무음 새로고침
        if (layoutRadioCandyContainer != null) {
            int themeHighlightColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < layoutRadioCandyContainer.getChildCount(); i++) {
                final View child = layoutRadioCandyContainer.getChildAt(i);
                if (child instanceof TextView && child.getTag() instanceof Float) {
                    TextView tvCandy = (TextView) child;
                    float stationFreq = (Float) child.getTag();
                    GradientDrawable candyBg = new GradientDrawable();
                    candyBg.setCornerRadius(20 * density);

                    if (Math.abs(fmManager.currentFreq - stationFreq) < 0.05f) {
                        candyBg.setColor(themeHighlightColor);
                        tvCandy.setTextColor(ThemeManager.getListButtonFocusedTextColor());

                        layoutRadioCandyContainer.post(new Runnable() {
                            @Override
                            public void run() {
                                android.view.ViewParent parent = layoutRadioCandyContainer.getParent();
                                if (parent instanceof android.widget.HorizontalScrollView) {
                                    android.widget.HorizontalScrollView hsv = (android.widget.HorizontalScrollView) parent;
                                    int scrollX = child.getLeft() - (hsv.getWidth() / 2) + (child.getWidth() / 2);

                                    // 🚀 [핵심 방어막] 스크롤 계산값이 음수가 되면 0(맨 왼쪽)으로 강제 고정하여 첫 채널 짤림을 영원히 방지!
                                    if (scrollX < 0)
                                        scrollX = 0;

                                    hsv.smoothScrollTo(scrollX, 0);
                                }
                            }
                        });
                    } else {
                        candyBg.setColor(ThemeManager.getListButtonNormalBg());
                        tvCandy.setTextColor(ThemeManager.getTextColorSecondary());
                    }
                    tvCandy.setBackground(candyBg);
                }
            }
        }
    }

    // 🚀 [핵심 기술 1] 모든 버튼에 직접 장착될 '글로벌 화면 끄기 센서'입니다!
    public View.OnLongClickListener globalScreenOffLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            clickFeedback();
            isLongPressConsumed = true; // 🚀 롱클릭 성공 마킹 (손 뗄 때 클릭되는 안드로이드 고질병 차단)
            turnOffScreen(); // 전역 화면 끄기 발동!
            return true; // 💡 true를 반환해야 버튼이 "아하, 롱클릭 처리했으니 일반 클릭은 취소해야지!" 하고 알아듣습니다.
        }
    };
    private Handler progressHandler = new Handler();
    // ⭕ [아래 코드로 덮어쓰기]
    private Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            try {
                com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager
                        .getInstance();
                if (am.isPlaying()) {
                    // =========================================================
                    // 🚀 [다음 곡 스펙트럼/가사 증발 방어막!]
                    if (isVisualizerShowing) {
                        // 곡이 바뀌면서 시스템이 앨범을 다시 켰다면? 투명 사각형을 믿고 알맹이만 조용히 숨깁니다!
                        if (ivAlbumArt != null && ivAlbumArt.getVisibility() == View.VISIBLE) {
                            ivAlbumArt.setVisibility(View.INVISIBLE);
                        }

                        boolean hasLyrics = (currentLyrics != null && !currentLyrics.isEmpty()) || (plainLyrics != null && !plainLyrics.isEmpty());
                        if (!hasLyrics) {
                            if (visualizerView != null && visualizerView.getVisibility() != View.VISIBLE) {
                                visualizerView.setVisibility(View.VISIBLE);
                            }
                            try { if (audioVisualizer != null && !audioVisualizer.getEnabled()) audioVisualizer.setEnabled(true); } catch(Exception e){}
                        }
                    }
                    // =========================================================
                    int current = am.getCurrentPosition();
                    int duration = am.getDuration();
                    int progress = duration > 0 ? (int) (((float) current / duration) * 100) : 0;

                    if (playerProgress != null) playerProgress.setProgress(progress);
                    if (tvPlayerTimeCurrent != null) tvPlayerTimeCurrent.setText(formatTime(current));
                    if (tvPlayerTimeTotal != null) tvPlayerTimeTotal.setText(formatTime(duration));
                    // =========================================================
                    // 🚀 [추가] 스펙트럼 모드 전용 하단 바도 실시간으로 똑같이 움직입니다!
                    // =========================================================
                    if (visProgressBar != null) visProgressBar.setProgress(progress);
                    if (tvVisTimeCurrent != null) tvVisTimeCurrent.setText(formatTime(current));
                    if (tvVisTimeTotal != null) tvVisTimeTotal.setText(formatTime(duration));

                    if (isVisualizerShowing && layoutAudioQualityContainer != null) {
                        layoutAudioQualityContainer.setVisibility(View.GONE);
                    }

                    // 🚀 [신규 엔진] USLT 통짜 가사 비례 오토 스크롤 엔진! (전주 5초 대기 기능 탑재)
                    if (isVisualizerShowing && plainLyrics != null && currentLyrics.isEmpty()) {
                        int maxScroll = tvLyrics.getHeight() - lyricScrollView.getHeight();

                        if (maxScroll > 0 && duration > 0) {
                            int delayMs = 5000; // 💡 5초(5000ms) 대기 설정

                            // 곡의 총 길이가 5초보다 길 때만 대기 알고리즘 작동
                            if (duration > delayMs) {
                                if (current <= delayMs) {
                                    // 5초 전까지는 스크롤을 맨 위(0)에 꽁꽁 묶어둡니다!
                                    lyricScrollView.smoothScrollTo(0, 0);
                                } else {
                                    // 5초가 지나면, '남은 시간'을 기준으로 진짜 진행률을 계산하여 스크롤 시작!
                                    float progressRatio = (float) (current - delayMs) / (duration - delayMs);
                                    int targetScroll = (int) (maxScroll * progressRatio);
                                    lyricScrollView.smoothScrollTo(0, targetScroll);
                                }
                            } else {
                                // 길이가 5초도 안 되는 짧은 효과음 같은 경우엔 그냥 딜레이 없이 스크롤
                                float progressRatio = (float) current / duration;
                                lyricScrollView.smoothScrollTo(0, (int) (maxScroll * progressRatio));
                            }
                        }
                    }

                    // (기존 코드) 🚀 [가사 스크롤 엔진] 가사 모드가 켜져 있다면... (이하 유지)
                    if (isVisualizerShowing && !currentLyrics.isEmpty()) {
                        int currentKey = -1;
                        for (int i = 0; i < lyricTimestamps.size(); i++) {
                            if (current >= lyricTimestamps.get(i))
                                currentKey = lyricTimestamps.get(i);
                            else
                                break;
                        }

                        if (currentKey != -1) {
                            int highlightIndex = lyricTimestamps.indexOf(currentKey);

                            // 부하 방지: 가사 줄이 넘어갔을 때만 UI를 새로 그립니다.
                            if (highlightIndex != lastLyricIndex) {
                                lastLyricIndex = highlightIndex;
                                StringBuilder sb = new StringBuilder();

                                // 🚀 [해결 1] 위아래로 무조건 3줄씩, 총 7줄짜리 보이지 않는 액자 틀을 만듭니다.
                                // 이렇게 하면 하늘색 하이라이트 줄이 액자의 '정중앙(4번째 줄)'에 항상 완벽하게 고정됩니다!
                                int start = highlightIndex - 3;
                                int end = highlightIndex + 3;

                                boolean isLightBg = isPlayerBackgroundLight();
                                String activeColor = isLightBg ? "#0044CC" : "#00FFFF";
                                String normalColor = isLightBg ? "#555555" : "#888888";

                                for (int i = start; i <= end; i++) {
                                    if (i < 0 || i >= lyricTimestamps.size()) {
                                        // 💡 가사가 없는 빈 공간(곡의 처음이나 끝부분)은 투명한 빈 줄(&nbsp;)을 세워서 중앙 균형을 강제로 맞춥니다.
                                        sb.append("&nbsp;<br>");
                                    } else {
                                        String lyricText = currentLyrics.get(lyricTimestamps.get(i));
                                        if (i == highlightIndex) {
                                            // 🚀 [해결 2] 줄바꿈을 <br><br>에서 <br> 하나로 줄여서 공간이 터지는 걸 막고, 글씨를 <big>으로 키워 확실하게
                                            // 강조합니다.
                                            sb.append("<font color='").append(activeColor).append("'><b><big>").append(lyricText)
                                                    .append("</big></b></font><br>");
                                        } else {
                                            sb.append("<font color='").append(normalColor).append("'>").append(lyricText).append("</font><br>");
                                        }
                                    }
                                }
                                if (tvLyrics != null) tvLyrics.setTextColor(isLightBg ? 0xFF111111 : 0xFFFFFFFF);
                                tvLyrics.setText(android.text.Html.fromHtml(sb.toString()));

                                // 🚀 [해결 3] 텍스트뷰의 과도한 상하 여백(Padding)을 다이어트시키고, 스크롤을 맨 위(0,0)로 꽉 잠가버립니다!
                                tvLyrics.setPadding(20, 10, 20, 10);
                                if (lyricScrollView != null) {
                                    lyricScrollView.scrollTo(0, 0);
                                }
                            }
                        }
                    }
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

    public void updateBatteryStatsMode() {
        try {
            int mode = com.themoon.y1.managers.BatteryStatsManager.MODE_OTHER;
            
            if (isScreenSleeping) {
                mode = com.themoon.y1.managers.BatteryStatsManager.MODE_STANDBY;
            } else {
                com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
                com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager.getInstance();
                
                if (fm != null && fm.isPowerUp) {
                    mode = com.themoon.y1.managers.BatteryStatsManager.MODE_RADIO;
                } else if (am != null && am.isPlaying()) {
                    mode = com.themoon.y1.managers.BatteryStatsManager.MODE_MUSIC;
                }
            }
            // VIDEO is handled by VideoPlayerActivity
            com.themoon.y1.managers.BatteryStatsManager.getInstance(this).setMode(mode);
        } catch (Exception e) {}
    }

    private BroadcastReceiver systemStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenSleeping = true;
                autoManageWifiPower(true); // 🚀 [절전 모드 진입]
                updateBatteryStatsMode();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenSleeping = false;
                lastScreenOnTime = System.currentTimeMillis();
                autoManageWifiPower(false); // 🚀 [절전 모드 해제]
                updateBatteryStatsMode();

                // =======================================================
                // 🚀 [LCD 딥슬립 강제 기상 충격기]
                // 장시간 방치 후 OS는 깼는데 화면 패널만 죽어있는(Sleep of Death) 하드웨어 버그 완벽 치료!
                // =======================================================
                try {
                    // 1. 강제 전력 펄스(WakeLock)를 발사해서 물리적 디스플레이 드라이버의 멱살을 잡고 강제로 깨웁니다!
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                                "Y1:WakeShockLock"
                        );
                        // 1.5초 동안 강제로 번개를 쏴서 LCD 전원을 복구!
                        wakeLock.acquire(1500);
                    }

                    // 2. OS가 잠결에 화면 밝기를 0으로 착각했을 수 있으므로, 우리가 기억하는 밝기 값으로 강제 렌더링!
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 만약 밝기 값이 10 이하로 너무 어둡게 꼬여있다면 기본값 255(최대)로 강제 소생!
                                if (currentSystemBrightness <= 10) currentSystemBrightness = 255;
                                updateBrightness(currentSystemBrightness);
                            } catch (Exception e) {}
                        }
                    }, 300); // 시스템이 깨어난 직후 0.3초 뒤에 밝기 리셋!

                } catch (Exception e) {}
                // =======================================================

            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                
                int batteryPct = (int) ((level / (float) scale) * 100);
                
                // 🚀 [추가] 배터리 충전 상태 확인
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);
                
                try {
                    com.themoon.y1.managers.BatteryStatsManager bsm = com.themoon.y1.managers.BatteryStatsManager.getInstance(context);
                    if (!isCharging) {
                        bsm.onBatteryLevelChanged(batteryPct);
                    } else {
                        bsm.onPowerDisconnected(batteryPct); // When charging, it triggers reset inside if level is high
                    }
                } catch (Exception e) {}

                tvStatusBattery.setText(batteryPct + "%");

                // 🚀 새로 만든 배터리 아이콘에 현재 용량과 충전 여부를 쏴줍니다!
                if (batteryIconView != null) {
                    batteryIconView.setBatteryLevel(batteryPct, isCharging);
                }
                if (currentBatteryStyleIndex == 0) { // Icon Only
                    if (batteryIconView != null) batteryIconView.setVisibility(android.view.View.VISIBLE);
                    tvStatusBattery.setVisibility(android.view.View.GONE);
                } else if (currentBatteryStyleIndex == 1) { // Percent Only
                    if (batteryIconView != null) batteryIconView.setVisibility(android.view.View.GONE);
                    tvStatusBattery.setVisibility(android.view.View.VISIBLE);
                } else { // Icon + Percent
                    if (batteryIconView != null) batteryIconView.setVisibility(android.view.View.VISIBLE);
                    tvStatusBattery.setVisibility(android.view.View.VISIBLE);
                }
                if (widgetBatteryView != null) {
                    widgetBatteryView.setBatteryLevel(batteryPct, isCharging);
                }
                if (customCircularBatteryView != null) {
                    customCircularBatteryView.setBatteryLevel(batteryPct, isCharging);
                }
            } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                int state = intent.getIntExtra("state", -1);
                if (state == 1) {
                    ivStatusHeadphone.setVisibility(View.VISIBLE);
                    // 🚀 이어폰 꽂았을 때 테마색 적용
                    ivStatusHeadphone.setColorFilter(ThemeManager.getTextColorPrimary());
                } else {
                    ivStatusHeadphone.setVisibility(View.GONE);
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    ivStatusBluetooth.setVisibility(View.VISIBLE);
                    ivStatusBluetooth.setColorFilter(ThemeManager.getTextColorPrimary());
                    BluetoothAdapter.getDefaultAdapter().getProfileProxy(context,
                            new BluetoothProfile.ServiceListener() {
                                @Override
                                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                                    if (profile == BluetoothProfile.A2DP)
                                        globalA2dp = proxy;
                                }

                                @Override
                                public void onServiceDisconnected(int profile) {
                                    if (profile == BluetoothProfile.A2DP)
                                        globalA2dp = null;
                                }
                            }, BluetoothProfile.A2DP);
                } else {
                    ivStatusBluetooth.setVisibility(View.GONE);
                    globalA2dp = null;
                    // 🚀 [방어막 3] 블루투스가 완전히 꺼지면 좀비 타겟도 초기화하여 찌꺼기 연결을 막습니다!
                    if (state == BluetoothAdapter.STATE_OFF)
                        targetDeviceForAudio = null;
                }

                // 🚀 [포커스 증발 방어막 4] 켜지는 중/꺼지는 중(TURNING)에는 UI 새로고침을 무시하고, '완전히 ON/OFF' 되었을 때 딱
                // 1번만 갱신합니다!
                if (currentScreenState == STATE_BLUETOOTH
                        && (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF)) {
                    startBluetoothScan();
                }
                // (이하 기존 코드 유지)
                // 🚀 [버그 해결 1] 사용자가 메인 셋팅창(깊이 0)에 있을 때만 새로고침 하도록 방어막 전개!
                if (currentScreenState == STATE_SETTINGS && currentSettingsDepth == 0)
                    buildSettingsUI();
                else if (currentScreenState == STATE_BLUETOOTH)
                    startBluetoothScan();
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    ivStatusWifi.setVisibility(View.VISIBLE);
                    // 🚀 [수정] 노란색(0xFFFFBB00) 제거 -> 테마 기본색 적용!
                    ivStatusWifi.setColorFilter(ThemeManager.getTextColorPrimary());
                } else {
                    ivStatusWifi.setVisibility(View.GONE);
                }

                // 🚀 [버그 해결 2] 와이파이 센서에도 똑같이 깊이 0 조건 추가!
                if (currentScreenState == STATE_SETTINGS && currentSettingsDepth == 0)
                    buildSettingsUI();
                else if (currentScreenState == STATE_WIFI)
                    startWifiScan();
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    ivStatusWifi.setColorFilter(0xFF00FF00); // 🟢 연결 완료 시 녹색
                    if (currentScreenState == STATE_WIFI)
                        startWifiScan();
                } else {
                    // 🚀 [수정] 노란색(0xFFFFBB00) 제거 -> 테마 기본색 적용!
                    ivStatusWifi.setColorFilter(ThemeManager.getTextColorPrimary());
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                // 🚀 ⭕ [수정] 이름이 아직 안 뜬 기기(null)라도 절대 버리지 말고 'Unknown Device (맥주소)'로 목록에 띄웁니다!
                String displayName = (deviceName != null && !deviceName.trim().isEmpty()) ? deviceName
                        : "Unknown (" + deviceAddress + ")";

                if (!foundBtDevices.contains(deviceAddress)) {
                    foundBtDevices.add(deviceAddress);
                    // 새로 발견된 기기는 무조건 목록에 추가!
                    addBluetoothItemToUI(displayName, device, false);
                }
            } else if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
                int profileState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                BluetoothDevice currentDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (profileState == BluetoothProfile.STATE_DISCONNECTED) {
                    ivStatusBluetooth.setColorFilter(ThemeManager.getTextColorPrimary());
                    Toast.makeText(context, t("Audio Disconnected"), Toast.LENGTH_SHORT).show();
                    // 🚀 [최종 방어막] 블루투스 자체가 꺼져서 끊긴 거라면(isEnabled == false) 절대 재연결 시도를 하지 않습니다!
                    BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                    if (ba != null && ba.isEnabled() && targetDeviceForAudio != null && currentDevice != null
                            && targetDeviceForAudio.getAddress().equals(currentDevice.getAddress())) {
                        connectBluetoothAudio(targetDeviceForAudio);
                    }
                } else if (profileState == BluetoothProfile.STATE_CONNECTED) {
                    ivStatusBluetooth.setColorFilter(0xFF00BFFF);
                    String name = currentDevice != null ? currentDevice.getName() : "Unknown";
                    Toast.makeText(context, t("Audio Connected to ") + name, Toast.LENGTH_SHORT).show();
                }

                if (profileState == BluetoothProfile.STATE_CONNECTED
                        || profileState == BluetoothProfile.STATE_DISCONNECTED) {
                    isBtConnectingState = false; // 잠금 해제!
                    if (currentScreenState == STATE_BLUETOOTH) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startBluetoothScan();
                            }
                        }, 300);
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                // 🚀 [무한 페어링 방어막 1] 페어링이 취소되거나 실패하면(BOND_NONE), 즉시 타겟을 지워서 좀비의 부활을 막습니다!
                if (state == BluetoothDevice.BOND_NONE) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (targetDeviceForAudio != null && device != null
                            && targetDeviceForAudio.getAddress().equals(device.getAddress())) {
                        targetDeviceForAudio = null; // 타겟 강제 해제!
                        Toast.makeText(context, t("Pairing failed or cancelled."), Toast.LENGTH_SHORT).show();
                    }
                }

                if (currentScreenState == STATE_BLUETOOTH && !isBtConnectingState) {
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            startBluetoothScan();
                        }
                    }, 300);
                }

                if (state == BluetoothDevice.BOND_BONDED) {
                    BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (bondedDevice != null)
                        connectBluetoothAudio(bondedDevice);
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();

                if (ba != null && ba.isEnabled() && targetDeviceForAudio != null && disconnectedDevice != null
                        && targetDeviceForAudio.getAddress().equals(disconnectedDevice.getAddress())) {

                    // 🚀 [무한 페어링 방어막 2] "이미 페어링이 완료된(BONDED)" 기기일 때만 좀비 엔진을 가동합니다!
                    // 페어링 도중에 튕긴 거라면 재연결을 시도하지 않고 타겟을 깔끔하게 포기합니다.
                    if (disconnectedDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        connectBluetoothAudio(targetDeviceForAudio);
                    } else {
                        targetDeviceForAudio = null; // 찌꺼기 타겟 삭제
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btnScanBt.setText(t("Scan Complete (Retry)"));
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    List<ScanResult> results = wm.getScanResults();
                    btnScanWifi.setText(t("Scan Complete (Retry)"));
                    updateWifiUI(results);
                }
            }
            // 🚀 [여기에 추가!] 시스템 미디어 스캐너 감지 센서
            else if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
                isMediaScanning = true;
            } else if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                isMediaScanning = false;

                // 🚀 [USB 오토 스캔 부활 엔진!]
                // 시스템이 USB(SD카드) 인식을 끝냈다고 알려주면,
                // 즉시 우리의 초고속 부분 스캐너를 돌려서 새 곡을 캐시 메모장에 영구 저장합니다!
                if (!isCustomScanning) {
                    startMediaLibraryScan();
                }
            }
            // 🚀 [여기에 신규 추가!] 다운로드가 끝나면 리스트를 새로고침하여 '✔' 아이콘을 띄웁니다!
            else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                if (currentBrowserMode == BROWSER_PODCAST_EPISODES && listVirtualSongs != null
                        && listVirtualSongs.getAdapter() != null) {
                    ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
                }
            }
        }
    };

    // 🚀 [순정 런처 완벽 이식] 중앙 통제형 블루투스 연결 엔진
    private void connectBluetoothAudio(final BluetoothDevice targetDevice) {
        if (targetDevice == null)
            return;
        targetDeviceForAudio = targetDevice; // 1. 목표물 영구 고정!

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isDiscovering()) {
            adapter.cancelDiscovery(); // 2. 과부하 방지를 위해 스캔 무조건 중지
        }

        // 3. 페어링이 안 되어 있다면 페어링부터 꽂습니다! (완료되면 수신기가 이 함수를 다시 부릅니다)
        if (targetDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            Toast.makeText(this, t("Pairing with ") + targetDevice.getName() + "...", Toast.LENGTH_SHORT).show();
            try {
                targetDevice.getClass().getMethod("createBond").invoke(targetDevice);
            } catch (Exception e) {
            }
            return;
        }

        Toast.makeText(this, t("Connecting Audio..."), Toast.LENGTH_SHORT).show();

        // 4. 엔진이 살아있으면 즉시 연결, 죽어있으면 백그라운드에서 살려낸 뒤 연결!
        if (globalA2dp != null) {
            executeA2dpConnect(targetDevice);
        } else {
            if (adapter != null) {
                adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        if (profile == BluetoothProfile.A2DP) {
                            globalA2dp = proxy;
                            executeA2dpConnect(targetDevice);
                        }
                    }

                    @Override
                    public void onServiceDisconnected(int profile) {
                        if (profile == BluetoothProfile.A2DP)
                            globalA2dp = null;
                    }
                }, BluetoothProfile.A2DP);
            }
        }
    }

    // 🚀 [핵심 디테일] 오디오 연결 실무 담당자
    private void executeA2dpConnect(BluetoothDevice targetDevice) {
        if (globalA2dp == null || targetDevice == null)
            return;
        try {
            // 💡 [순정 코드의 비밀] 연결 전, 이미 물려있는 다른 오디오 기기가 있다면 가차 없이 끊어버립니다!
            List<BluetoothDevice> connectedDevices = null;
            try {
                connectedDevices = globalA2dp.getConnectedDevices();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (connectedDevices != null) {
                for (BluetoothDevice connected : connectedDevices) {
                    if (!connected.getAddress().equals(targetDevice.getAddress())) {
                        try {
                            Method disconnectMethod = globalA2dp.getClass().getDeclaredMethod("disconnect",
                                    BluetoothDevice.class);
                            disconnectMethod.setAccessible(true);
                            disconnectMethod.invoke(globalA2dp, connected);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // 💡 방해물이 사라지면, 마침내 타겟 기기에 오디오 빔 발사!
            Method connectMethod = null;
            try {
                connectMethod = globalA2dp.getClass().getDeclaredMethod("connect", BluetoothDevice.class);
            } catch (NoSuchMethodException e) {
                // If not found, try to iterate
                for (Method m : globalA2dp.getClass().getMethods()) {
                    if (m.getName().equals("connect") && m.getParameterTypes().length == 1) {
                        connectMethod = m;
                        break;
                    }
                }
            }

            if (connectMethod == null) {
                Toast.makeText(this, t("Audio connection error: connect method not found"), Toast.LENGTH_LONG).show();
                return;
            }

            connectMethod.setAccessible(true);
            boolean result = (Boolean) connectMethod.invoke(globalA2dp, targetDevice);
            if (!result) {
                Toast.makeText(this, t("Audio connection rejected by system."), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, t("Audio connection initiated."), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorStr = e.getClass().getSimpleName();
            if (e instanceof java.lang.reflect.InvocationTargetException) {
                Throwable cause = ((java.lang.reflect.InvocationTargetException) e).getCause();
                if (cause != null) {
                    errorStr += " (Cause: " + cause.getClass().getSimpleName() + " - " + cause.getMessage() + ")";
                }
            } else {
                errorStr += " - " + e.getMessage();
            }
            Toast.makeText(this, "Audio error: " + errorStr, Toast.LENGTH_LONG).show();
        }
    }

    // 초기화 함수 내부 (앱 실행 시 1회 호출)
    public void initRemoteControlClient(Context context) {
        if (remoteControlClient == null) {
            AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);

            // 우리가 이전에 만들어둔 미디어 버튼 수신기(MediaBtnReceiver)를 연결합니다.
            mediaButtonReceiver = new ComponentName(context.getPackageName(),
                    MediaBtnReceiver.class.getName());
            audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver);

            // 리모컨 클라이언트를 위한 인텐트 생성
            Intent mediaButtonIntent = new Intent(
                    Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(mediaButtonReceiver);

            // 🚀 [보안 에러 완벽 해결] 안드로이드 12 이상을 위한 불변(IMMUTABLE) 플래그를 장착합니다!
            // 젤리빈 등 구형 기기(API 23 미만)에서는 에러가 나지 않도록 분기 처리를 해줍니다.
            int pendingIntentFlags = 0;
            if (Build.VERSION.SDK_INT >= 23) { // 안드로이드 6.0(마시멜로) 이상
                pendingIntentFlags = android.app.PendingIntent.FLAG_IMMUTABLE;
            }
            android.app.PendingIntent mediaPendingIntent = android.app.PendingIntent.getBroadcast(context, 0,
                    mediaButtonIntent, pendingIntentFlags);

            // 🚀 젤리빈 전용 방송국 개국!
            remoteControlClient = new android.media.RemoteControlClient(mediaPendingIntent);

            // 차량 핸들 및 블루투스 기기에서 무슨 버튼을 누를 수 있는지 권한 부여
            int flags = android.media.RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                    | android.media.RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                    | android.media.RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | android.media.RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                    | android.media.RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS;
            remoteControlClient.setTransportControlFlags(flags);

            audioManager.registerRemoteControlClient(remoteControlClient);
        }
    }

    // 🚀 곡이 바뀔 때 호출!
    public void updateBluetoothMetadata(String title, String artist, String album,
            Bitmap albumArtBmp) {
        if (remoteControlClient == null)
            return;

        android.media.RemoteControlClient.MetadataEditor editor = remoteControlClient.editMetadata(true);

        // 1. 텍스트 정보 입력 (젤리빈은 MediaMetadataRetriever의 상수를 가져다 씁니다)
        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                title != null ? title : "Unknown Title");
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                artist != null ? artist : "Unknown Artist");
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                album != null ? album : "Unknown Album");

        // 2. 🚀 [핵심] 앨범 아트 비트맵을 차량 디스플레이로 전송!
        if (albumArtBmp != null) {
            editor.putBitmap(android.media.RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, albumArtBmp);
        }

        // 포장 완료 후 시스템에 전송
        editor.apply();
    }

    // 🚀 음악이 재생되거나 정지될 때 호출!
    public void updateBluetoothPlaybackState(boolean isPlaying) {
        if (remoteControlClient == null)
            return;

        int state = isPlaying ? android.media.RemoteControlClient.PLAYSTATE_PLAYING
                : android.media.RemoteControlClient.PLAYSTATE_PAUSED;

        // 젤리빈은 현재 시간(currentPosition)을 쏘지 않아도 차량에서 자체적으로 타이머를 돌려줍니다.
        remoteControlClient.setPlaybackState(state);
    }

    // 🚀 [신규 도우미] 현재 화면의 곡 정보와 이미지를 읽어서 블루투스로 쏘는 함수
    private void sendBluetoothMetaToCar() {
        String title = tvPlayerTitle != null ? tvPlayerTitle.getText().toString() : "Unknown";
        String artist = tvPlayerArtist != null ? tvPlayerArtist.getText().toString() : "Unknown";
        Bitmap bmp = null;

        // 앨범 아트가 있으면 블루투스 전송 용량에 맞게 살짝 압축해서 보냅니다.
        if (lastAlbumArtBytes != null && lastAlbumArtBytes.length > 0) {
            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 2;
                bmp = BitmapFactory.decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length,
                        opts);
            } catch (Exception e) {
            }
        }

        updateBluetoothMetadata(title, artist, "Y1 Player", bmp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        
        // 🚀 앱이 켜지면 자기 자신을 변수에 등록합니다.
        instance = this;
        // 🚀 [초고속 캐시 엔진 가동] 기기 최대 메모리의 1/8을 앨범 아트 전용 금고로 할당합니다!
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // (예: 16MB 할당)

        albumArtCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // 비트맵이 램(RAM)에서 차지하는 실제 용량(KB)을 계산하여 금고 용량을 관리합니다.
                if (Build.VERSION.SDK_INT >= 12) {
                    return bitmap.getByteCount() / 1024;
                }
                return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    File logFile = StoragePaths.primaryFile("y1_crash_log.txt");
                    FileOutputStream fos = new FileOutputStream(logFile, true);
                    fos.write(("\n\n--- 💥 CRASH REPORT (" + new Date().toString() + ") ---\n").getBytes());
                    fos.write(sw.toString().getBytes());
                    fos.close();
                } catch (Exception ex) {
                }
                System.exit(1);
            }
        });
        // 🚀 [추가] A2DP 오디오 통제권을 미리 확보해 둡니다.
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(this,
                new BluetoothProfile.ServiceListener() {
                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        if (profile == BluetoothProfile.A2DP) {
                            globalA2dp = proxy; // 장전 완료!
                        }
                    }

                    @Override
                    public void onServiceDisconnected(int profile) {
                        if (profile == BluetoothProfile.A2DP)
                            globalA2dp = null;
                    }
                }, BluetoothProfile.A2DP);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        // =======================================================
        // 📺 [레트로 감성 엔진] 화면 전체 흑백(Grayscale) 강제 필터링
        // =======================================================
        // android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
        // matrix.setSaturation(0); // 🚀 채도를 0%로 증발시킴 (흑백화)
        //
        // // 🚀 2. 덧씌울 모노톤 색상을 결정합니다! (여기를 마음대로 바꾸면 돼!)
        // // 예시 1: 0xFF88CC88 (옛날 게임보이 녹색 액정 느낌)
        // // 예시 2: 0xFFD2B48C (클래식한 세피아/골드 톤)
        // // 예시 3: 0xFF8888FF (차가운 블루 틴트)
        // int monotoneColor = 0xFF88CC88;
        //
        // // 🚀 3. RGB 값을 비율로 쪼개서 흑백 화면 위에 색상 렌즈를 덧씌웁니다.
        // android.graphics.ColorMatrix tintMatrix = new android.graphics.ColorMatrix();
        // tintMatrix.setScale(
        // android.graphics.Color.red(monotoneColor) / 255f,
        // android.graphics.Color.green(monotoneColor) / 255f,
        // android.graphics.Color.blue(monotoneColor) / 255f,
        // 1.0f
        // );
        // matrix.postConcat(tintMatrix); // 흑백 매트릭스와 색상 매트릭스를 합체!
        //
        // android.graphics.Paint monotonePaint = new android.graphics.Paint();
        // monotonePaint.setColorFilter(new
        // android.graphics.ColorMatrixColorFilter(matrix));
        // // 앱의 최상위 도화지(Root View)를 가져와서 흑백 셀로판지를 하드웨어 가속으로 덮어씌웁니다!
        // getWindow().getDecorView().getRootView().setLayerType(View.LAYER_TYPE_HARDWARE,
        // monotonePaint);

        // ----------------------------------------------------------------------------------------------------------------
        // 🚀 [수정 완료] 기존 로딩 오버레이 코드를 아래 내용으로 통째로 덮어씌우세요!
        ViewGroup root = findViewById(android.R.id.content);
        layoutLoadingOverlay = new LinearLayout(this);
        layoutLoadingOverlay.setOrientation(LinearLayout.VERTICAL);
        layoutLoadingOverlay.setGravity(Gravity.CENTER);
        layoutLoadingOverlay.setBackgroundColor(0xDD000000);
        layoutLoadingOverlay.setClickable(true);
        layoutLoadingOverlay.setFocusable(true);
        layoutLoadingOverlay.setVisibility(View.GONE);

        // 1. 빙글빙글 스피너 대신, 쫙 차오르는 가로형 프로그레스 바(ProgressBar) 적용!
        pbLoadingProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pbLoadingProgress.setMax(100);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                (int) (250 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density));
        layoutLoadingOverlay.addView(pbLoadingProgress, pbLp);

        // 2. 실시간 숫자를 쏴줄 텍스트뷰
        tvLoadingProgress = new TextView(this);
        tvLoadingProgress.setText(t("Preparing to scan...\nPlease wait."));
        tvLoadingProgress.setTextColor(0xFFFFFFFF);
        tvLoadingProgress.setTextSize(18);
        tvLoadingProgress.setGravity(Gravity.CENTER);
        tvLoadingProgress.setPadding(0, 30, 0, 0);
        layoutLoadingOverlay.addView(tvLoadingProgress);

        root.addView(layoutLoadingOverlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // 🚀 [여기까지 추가 끝!]
        tvFastScrollLetter = new TextView(this);
        tvFastScrollLetter.setTextSize(50); // 글자 크기를 아주 큼직하게!
        tvFastScrollLetter.setGravity(Gravity.CENTER);
        tvFastScrollLetter.setVisibility(View.GONE);

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                (int) (80 * getResources().getDisplayMetrics().density), // 가로 80dp
                (int) (80 * getResources().getDisplayMetrics().density) // 세로 80dp
        );
        flp.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT; // 오른쪽 가운데 정렬
        flp.rightMargin = (int) (30 * getResources().getDisplayMetrics().density); // 오른쪽에서 30dp 띄움
        root.addView(tvFastScrollLetter, flp);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // 🚀 [시스템 공식 등록] 화면이 꺼져도 버튼 신호를 받을 수 있도록 수신기를 장착합니다!
        ComponentName componentName = new ComponentName(getPackageName(), MediaBtnReceiver.class.getName());
        audioManager.registerMediaButtonEventReceiver(componentName);
        // 🚀 [블루투스 엔진 시동] 젤리빈 AVRCP 방송국 개국!
        initRemoteControlClient(this);
        prefs = getSharedPreferences("Y1_SETTINGS", MODE_PRIVATE);

        // 🚀 1. 가장 먼저! APK 안에 숨겨둔 언어팩(.json)들을 기기로 싹 풀어줍니다.
        installBundledLanguages();

        // 🚀 2. 그 다음 언어 엔진을 가동하여 풀려난 파일 중 사용자가 선택한 언어를 읽어옵니다.
        String savedLang = prefs.getString("app_language", "English (Default)");
        LanguageManager.getInstance(this).applyLanguage(savedLang);
        // 🚀 [테마 파일 동적 로드] 기기 내부의 폴더에서 테마 파일들을 읽어옵니다!
        File themeFolder = StoragePaths.getThemesDir();

        // 💡 테마 목록을 읽어오기 전, APK에 내장된 기본 제공 테마가 있다면 먼저 압축을 풀어 설치합니다!
        installBundledThemes();

        ThemeManager.loadThemesFromStorage(themeFolder);
        try {
            // 🚀 [블루투스 AVRCP 1.6 강제 주입 엔진]
            // 개발자 옵션이 막혀있으므로, su 권한을 통해 ADB 쉘 명령어를 시스템에 직접 다이렉트로 쏩니다.
            String cmd1 = "setprop persist.bluetooth.avrcpversion 1.6";
            String cmd2 = "settings put global bluetooth_avrcp_version 1.6";

            // 두 명령어를 &&(AND)로 묶어 연달아 실행하고 시스템을 동기화(sync)합니다.
            String combinedCmd = cmd1 + " && " + cmd2 + " && sync";

            Process proc = Runtime.getRuntime().exec(new String[] { "su", "-c", combinedCmd });
            proc.waitFor(); // 명령어 적용이 끝날 때까지 잠시 대기

            // 조용히 적용하기 위해 토스트는 테스트가 끝나면 지우셔도 됩니다.
            // Toast.makeText(this, "Bluetooth AVRCP 1.6 forced via Root.",
            // Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // 저장된 인덱스 번호를 불러옵니다. (파일이 지워졌을 수도 있으니 안전하게 처리됨)
            int savedThemeIndex = prefs.getInt("app_theme_index", 0);
            ThemeManager.setThemeIndex(savedThemeIndex);
        } catch (Exception e) {
        }

        // (이하 블랙리스트 및 다른 설정 불러오기 코드 유지)
        // 💡 1. 블랙리스트 (안드로이드 내부 버그 방지를 위해 HashSet을 새로 감싸서 안전하게 로드)
        try {
            Set<String> savedBlacklist = prefs.getStringSet("blacklist", new HashSet<String>());
            blacklist = new HashSet<>(savedBlacklist);

            String poisonFile = prefs.getString("last_attempted_file", null);
            if (poisonFile != null) {
                blacklist.add(poisonFile);
                prefs.edit().putStringSet("blacklist", blacklist).remove("last_attempted_file").commit();
            }
        } catch (Exception e) {
        }

        try {
            isSoftwareEqEnabled = prefs.getBoolean("software_eq_enabled", false);
        } catch (Exception e) {
        }

        // 💡 2. 설정값들을 각각 독립적으로 불러오기 (어떤 상황에서도 절대 스킵되지 않습니다!)
        try {
            isShuffleMode = prefs.getBoolean("shuffle", false);
        } catch (Exception e) {
        }

        try {
            if (prefs.contains("repeat_mode")) {
                repeatMode = prefs.getInt("repeat_mode", 0);
            } else {
                repeatMode = prefs.getBoolean("repeat", false) ? 1 : 0;
            }
        } catch (Exception e) {
        }

        try {
            isSoundEffectEnabled = prefs.getBoolean("sound", true);
            applySoundSetting();
        } catch (Exception e) {
        }

        try {
            isVibrationEnabled = prefs.getBoolean("vibrate", true);
            vibrationStrengthLevel = prefs.getInt("vibrate_strength", 1);
        } catch (Exception e) {
        }
        try {
            isScreenOffControlEnabled = prefs.getBoolean("screen_off_control", false);
        } catch (Exception e) {
        }

        try {
            isAutoFetchEnabled = prefs.getBoolean("auto_fetch", true);
        } catch (Exception e) {
        } // 🚀 [추가]
        try {
            currentTimeoutIndex = prefs.getInt("timeout_idx", 1);
            currentBatteryStyleIndex = prefs.getInt("battery_indicator_style", 0);
        } catch (Exception e) {
        }

        try {
            currentSystemBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                    255);
        } catch (Exception e) {
        }
        try {
            // 금고(SharedPreferences)에서 기존에 저장된 포맷 설정을 불러옵니다. (기본값은 12시간 포맷)
            is24HourFormat = prefs.getBoolean("is_24h_format", false);
        } catch (Exception e) {
        }
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
        // 🚀 [추가] 고급 이펙트 및 커스텀 프로필 금고 데이터 연동
        currentEqProfile = prefs.getString("eq_profile_id", "preset_" + currentEqPresetIndex);
        currentBassBoostStep = prefs.getInt("bass_boost_step", 0);
        currentVirtualizerStep = prefs.getInt("virtualizer_step", 0);
        currentCrossfeedStep = prefs.getInt("crossfeed_step", 0);
        if (currentEqPresetIndex >= eqPresetNames.size())
            currentEqPresetIndex = 0;

        if (!rootFolder.exists())
            rootFolder.mkdirs();

        // 🚀 [추가된 부분] 앱이 켜질 때(혹은 튕기고 재시작될 때) 조용히 자동 스캔을 돌려 리스트를 복구합니다!
        if (customLibrary.isEmpty() && !isCustomScanning) {
            if (!loadLibraryFromCache()) {
                startMediaLibraryScan(); // 만약 파일이 지워졌거나 깨졌으면 그때 스캔!
            }
        }

        // Y2 MicroSD watchdog: remount /storage/sdcard1 when FUSE drops, then rescan.
        externalSdMountMonitor = new com.themoon.y1.managers.ExternalSdMountMonitor(this);
        externalSdMountMonitor.setListener(new com.themoon.y1.managers.ExternalSdMountMonitor.Listener() {
            @Override
            public void onSecondaryStorageReady() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isCustomScanning)
                            startMediaLibraryScan();
                    }
                });
            }
        });
        externalSdMountMonitor.start();

        layoutMainMenu = findViewById(R.id.layout_main_menu);
        ivMainBg = findViewById(R.id.iv_main_bg);
        ivMenuPreview = findViewById(R.id.iv_menu_preview);
        tvMenuPreviewTitle = findViewById(R.id.tv_menu_preview_title);
        tvMenuPreviewArtist = findViewById(R.id.tv_menu_preview_artist);

        // 🚀 1. 저장해둔 위젯 체크 상태 불러오기
        try {
            isWidgetClockOn = prefs.getBoolean("widget_clock", false);
        } catch (Exception e) {
        }
        try {
            isWidgetBatteryOn = prefs.getBoolean("widget_battery", false);
        } catch (Exception e) {
        }
        try {
            isWidgetAlbumOn = prefs.getBoolean("widget_album", false);
        } catch (Exception e) {
        }
        try {
            isWidgetFocusImageOn = prefs.getBoolean("widget_focus_image", false); // 🚀 [추가] 상태 불러오기
        } catch (Exception e) {
        }
        // 🚀 신규 스위치들의 기존 세팅값을 기억 금고에서 복원합니다.

        isLoopScrollOn = prefs.getBoolean("loop_scroll_on", true);
        updateMainMenuBackground(); // 💡 앱을 켜면 저장된 상태에 맞춰 배경 자동 적용

        layoutBrowserMode = findViewById(R.id.layout_browser_mode);
        layoutPlayerMode = findViewById(R.id.layout_player_mode);
        containerBrowserItems = findViewById(R.id.container_browser_items);

        // ⬇️ 기존 코드
        scrollViewBrowser = (View) containerBrowserItems.getParent();

        // 🚀 1. 알파벳 퀵 스크롤 바 생성
        hzIndexScroll = new android.widget.HorizontalScrollView(this);
        hzIndexScroll.setHorizontalScrollBarEnabled(false);
        hzIndexScroll.setVisibility(View.GONE);
        hzIndexScroll.setBackgroundColor(0x44000000); // 반투명 검정 배경

        layoutIndexContainer = new LinearLayout(this);
        layoutIndexContainer.setOrientation(LinearLayout.HORIZONTAL);
        hzIndexScroll.addView(layoutIndexContainer);

        // 🚀 2. 스크롤바와 리스트뷰를 담을 '수직 컨테이너' 생성
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.addView(hzIndexScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (40 * getResources().getDisplayMetrics().density) // 높이 40dp
        ));

        // 🚀 3. [핵심 엔진] 리스트뷰를 켜고 끌 때, 부모 컨테이너(알파벳 바)도 알아서 같이 반응하도록 개조!
        listVirtualSongs = new ListView(this) {
            @Override
            public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                if (listContainer != null) {
                    listContainer.setVisibility(visibility);
                }
            }
        };

        listVirtualSongs.setDivider(new ColorDrawable(0x00000000));
        listVirtualSongs.setDividerHeight((int) (4 * getResources().getDisplayMetrics().density));
        listVirtualSongs.setSelector(new ColorDrawable(0));
        listVirtualSongs.setItemsCanFocus(true);
        listVirtualSongs.setSoundEffectsEnabled(false);

        // 🚀 5. 리스트뷰를 수직 컨테이너에 조립
        listContainer.addView(listVirtualSongs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 이 한 줄로 listVirtualSongs와 listContainer 전체가 한 번에 숨겨집니다.
        listVirtualSongs.setVisibility(View.GONE);

        // 🚀 6. 완성된 컨테이너를 메인 화면(브라우저)에 최종 부착!
        ViewGroup browserParent = (ViewGroup) scrollViewBrowser.getParent();
        ViewGroup.LayoutParams originalLp = scrollViewBrowser.getLayoutParams();
        browserParent.addView(listContainer, originalLp);

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
                    btn.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                } else {
                    btn.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btn.setTextColor(ThemeManager.getTextColorPrimary());
                }
            }
        };
        btnScanBt.setOnFocusChangeListener(scanFocusListener);
        btnScanWifi.setOnFocusChangeListener(scanFocusListener);
        btnScanBt.setSoundEffectsEnabled(false);
        btnScanWifi.setSoundEffectsEnabled(false);
        btnScanBt.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
        btnScanBt.setTextColor(ThemeManager.getTextColorPrimary());
        btnScanWifi.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
        btnScanWifi.setTextColor(ThemeManager.getTextColorPrimary());
        layoutWifiKeyboard = findViewById(R.id.layout_wifi_keyboard);
        tvKeyboardSsid = findViewById(R.id.tv_keyboard_ssid);
        tvKeyboardInput = findViewById(R.id.tv_keyboard_input);
        // =========================================================
        // 🚀 [와이파이 키보드 입력창 글자 크기 업그레이드!]
        // 기본 콩알만한 글씨를 무시하고 여기서 시원시원하게 덮어씌웁니다.
        // =========================================================
        tvKeyboardInput.setTextSize(26f); // 💡 숫자를 올릴수록 글씨가 빵빵해집니다! (추천: 24f ~ 28f)
        tvKeyboardInput.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD); // 글씨를 두껍고 선명하게!
        // =========================================================

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
        tvServerStatus = findViewById(R.id.tv_server_status);
        tvServerIp = findViewById(R.id.tv_server_ip);
        btnServerToggle = findViewById(R.id.btn_server_toggle);
        try {
            // 1. Settings (세팅 메뉴)
            ((TextView) ((ViewGroup) layoutSettingsMode).getChildAt(0)).setText(t("Settings"));
            // 2. Bluetooth (블루투스)
            ((TextView) ((ViewGroup) layoutBluetoothMode).getChildAt(0)).setText(t("Bluetooth"));
            // 3. Wi-Fi (와이파이)
            ((TextView) ((ViewGroup) layoutWifiMode).getChildAt(0)).setText(t("Wi-Fi"));
            // 4. Brightness (화면 밝기)
            ((TextView) ((ViewGroup) layoutBrightnessMode).getChildAt(0)).setText(t("Display Brightness"));
            // 5. Storage (저장소)
            ((TextView) ((ViewGroup) layoutStorageMode).getChildAt(0)).setText(t("Storage"));
            // 6. Web Server (웹 서버)
            ((TextView) ((ViewGroup) layoutWebServerMode).getChildAt(0)).setText(t("Wireless PC Upload"));
        } catch (Exception e) {
            // 레이아웃 구조가 달라도 앱이 터지지 않도록 보호
        }
        // 🚀 [여기서부터 추가] PC Upload 화면 텍스트 높이 및 간격 조절
        float dt = getResources().getDisplayMetrics().density;

        try {
            ViewGroup webLayout = (ViewGroup) layoutWebServerMode;
            // 레이아웃의 맨 첫 번째(인덱스 0) 요소가 보통 제목 텍스트입니다.
            TextView tvHeader = (TextView) webLayout.getChildAt(0);

            // 💡 원하시는 제목으로 마음껏 바꿔주세요!
            // tvHeader.setText("Wireless PC Upload");

            // 💡 원하신다면 여기서 최상단 제목의 글씨 크기나 색상도 바꿀 수 있습니다.
            // tvHeader.setTextSize(26);
            // tvHeader.setTextColor(0xFF00FFFF);
            tvHeader.setTranslationY(20 * dt);
        } catch (Exception e) {
            // 레이아웃 구조가 다를 경우 앱이 튕기지 않도록 방어
        }
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

        // 💡 평상시에도 테마에 맞는 질감을 주어 버튼 영역이 어디인지 시각적으로 보여줍니다.
        btnServerToggle.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
        btnServerToggle.setTextColor(ThemeManager.getTextColorPrimary());

        btnServerToggle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    btnServerToggle.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btnServerToggle.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    btnServerToggle.setTypeface(null, Typeface.BOLD);
                } else {
                    btnServerToggle.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btnServerToggle.setTextColor(ThemeManager.getTextColorPrimary());
                    btnServerToggle.setTypeface(null, Typeface.NORMAL);
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
        tvStatusClock.setShadowLayer(0, 0, 0, 0);
        tvStatusBattery.setShadowLayer(0, 0, 0, 0);
        // 🚀 [여기에 새로 추가!] 기존 배터리 숫자(텍스트)를 숨기고 그 자리에 플랫 아이콘을 끼워 넣습니다.
        tvStatusBattery.setVisibility(View.GONE);
        batteryIconView = new BatteryIconView(this);
        ViewGroup statusParent = (ViewGroup) tvStatusBattery.getParent();
        int bIdx = statusParent.indexOfChild(tvStatusBattery);

        float density = getResources().getDisplayMetrics().density;
        // 🚀 [크기 폭업] 가로 54dp, 세로 24dp로 훨씬 더 크고 시원하게 키웁니다!
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                (int) (54 * density), (int) (24 * density));
        blp.gravity = Gravity.CENTER_VERTICAL;
        blp.setMargins((int) (15 * density), 0, (int) (6 * density), 0); // 커진 만큼 마진도 살짝 조정
        statusParent.addView(batteryIconView, bIdx, blp);
        ivStatusBluetooth = findViewById(R.id.iv_status_bluetooth);
        ivStatusWifi = findViewById(R.id.iv_status_wifi);
        ivStatusHeadphone = findViewById(R.id.iv_status_headphone);
        // 🚀🚀🚀 [수정 완료] 복잡한 폰트 파일 대신 100% 호환되는 직관적 텍스트 기호 사용! 🚀🚀🚀
        tvStatusPlay = new TextView(this);
        tvStatusPlay.setTypeface(null, Typeface.BOLD); // 기본 폰트를 아주 굵게!
        tvStatusPlay.setText("▶"); // ▶ 직관적인 재생 텍스트 기호
        tvStatusPlay.setTextSize(16f); // 기호에 맞게 크기 살짝 조정
        tvStatusPlay.setTextColor(0xFFFFFFFF);
        tvStatusPlay.setGravity(Gravity.CENTER);
        tvStatusPlay.setVisibility(View.GONE);

        ViewGroup rightStatusGroup = (ViewGroup) ivStatusBluetooth.getParent();
        float statusDensity = getResources().getDisplayMetrics().density;

        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        playLp.gravity = Gravity.CENTER_VERTICAL;
        playLp.setMargins(0, 0, (int) (8 * statusDensity), 0);

        rightStatusGroup.addView(tvStatusPlay, 0, playLp);

        // 🚀 [신규 추가] 웹 서버 아이콘도 똑같이 22dp 크기로 빚어서 상태바 우측 그룹에 꽂아 넣습니다!
        ivStatusServer = new ImageView(this);

        // 🚀 [순정 아이콘 교체 완료] 안드로이드 시스템 기본 '업로드(Upload)' 아이콘을 장착합니다!
        ivStatusServer.setImageResource(android.R.drawable.stat_sys_upload);
        // 💡 팁: 만약 둥글게 도는 '동기화' 화살표 모양이 더 좋으시다면 아래 코드로 바꾸셔도 예쁩니다!
        // ivStatusServer.setImageResource(android.R.drawable.ic_popup_sync);

        ivStatusServer.setColorFilter(0xFFFFFFFF); // 깔끔한 순백색으로 도색!
        ivStatusServer.setVisibility(View.GONE); // 평소엔 끄기

        LinearLayout.LayoutParams serverLp = new LinearLayout.LayoutParams(
                (int) (22 * statusDensity), (int) (22 * statusDensity));
        serverLp.gravity = Gravity.CENTER_VERTICAL;
        serverLp.setMargins(0, 0, (int) (8 * statusDensity), 0); // 우측 아이콘과의 간격 8dp

        rightStatusGroup.addView(ivStatusServer, 0, serverLp); // 재생 아이콘 옆에 나란히 배치!

        // [🟢 아래 코드로 교체!]
        tvBrowserPath = findViewById(R.id.tv_browser_path);
        tvBrowserPath.setTextColor(ThemeManager.getTextColorPrimary());

        // 🚀 [경로 배경색 테마 동기화 완료]
        // 아티스트님 기획대로 테마의 오버레이 배경색(bgOverlay) 코드를 그대로 수혈받아 적용합니다!
        // 리스트 스크롤 시 글씨가 뒤로 비쳐 보이는 현상을 완벽히 차단하기 위해 불투명도(| 0xFF000000)를 더해줍니다.
        // tvBrowserPath.setBackgroundColor(ThemeManager.getOverlayBackgroundColor());

        // 주변 소소한 간격(Padding)과 글씨 스타일을 고급스럽게 다듬어줍니다.
        // tvBrowserPath.setPadding((int) (15 *
        // getResources().getDisplayMetrics().density),
        // (int) (10 * getResources().getDisplayMetrics().density),
        // (int) (15 * getResources().getDisplayMetrics().density),
        // (int) (10 * getResources().getDisplayMetrics().density));
        // tvBrowserPath.setTypeface(null, android.graphics.Typeface.BOLD);

        btnNowPlaying = findViewById(R.id.btn_now_playing);
        btnPlay = findViewById(R.id.btn_play);
        btnSettings = findViewById(R.id.btn_settings);
        btnBluetooth = findViewById(R.id.btn_bluetooth);
        btnRadio = findViewById(R.id.btn_radio);
        ((View) btnRadio.getParent()).setVisibility(View.VISIBLE);
        Button btnWebServer = findViewById(R.id.btn_webserver);
        tvPlayerTitle = findViewById(R.id.tv_player_title);
        tvPlayerArtist = findViewById(R.id.tv_player_artist);
        tvPlayerAlbum = findViewById(R.id.tv_player_album);
        tvPlayerTimeCurrent = findViewById(R.id.tv_player_time_current);
        tvPlayerTimeTotal = findViewById(R.id.tv_player_time_total);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        ivPlayerBgBlur = findViewById(R.id.iv_player_bg_blur);
        ivPlayerBgBlur.setScaleX(1.05f);
        ivPlayerBgBlur.setScaleY(1.05f);
        ivPauseOverlay = findViewById(R.id.iv_pause_overlay);
        playerProgress = findViewById(R.id.player_progress); // 기존에 있던 코드

        // =========================================================
        // 🚀 [신규 장착] 스펙트럼/가사 모드 전용 하단 고정 프로그레스 바 생성
        // =========================================================
        layoutVisProgress = new LinearLayout(this);
        layoutVisProgress.setOrientation(LinearLayout.HORIZONTAL);
        layoutVisProgress.setGravity(Gravity.CENTER_VERTICAL);
        layoutVisProgress.setVisibility(View.GONE); // 평소엔 완벽하게 숨김!
        layoutVisProgress.setPadding((int)(15*density), (int)(10*density), (int)(15*density), (int)(20*density));

        tvVisTimeCurrent = new TextView(this);
        tvVisTimeCurrent.setTextColor(0xFFFFFFFF);
        tvVisTimeCurrent.setTextSize(14f);
        tvVisTimeCurrent.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvVisTimeCurrent.setText("00:00");

        tvVisTimeTotal = new TextView(this);
        tvVisTimeTotal.setTextColor(0xFFFFFFFF);
        tvVisTimeTotal.setTextSize(14f);
        tvVisTimeTotal.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvVisTimeTotal.setText("00:00");

        visProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        visProgressBar.setMax(100);
        LinearLayout.LayoutParams visPbLp = new LinearLayout.LayoutParams(0, (int)(12*density), 1.0f);
        visPbLp.setMargins((int)(12*density), 0, (int)(12*density), 0);
        visProgressBar.setLayoutParams(visPbLp);

        layoutVisProgress.addView(tvVisTimeCurrent);
        layoutVisProgress.addView(visProgressBar);
        layoutVisProgress.addView(tvVisTimeTotal);

        FrameLayout rootPlayerMode = findViewById(R.id.layout_player_mode);
        FrameLayout.LayoutParams visContainerLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        visContainerLp.gravity = Gravity.BOTTOM; // 🚀 화면 맨 밑바닥에 자석처럼 고정!
        rootPlayerMode.addView(layoutVisProgress, visContainerLp);
        tvPlayerTrackCount = findViewById(R.id.tv_player_track_count);

        ivPlayerShuffleStatus = findViewById(R.id.iv_player_shuffle_status);
        ivPlayerRepeatStatus = findViewById(R.id.iv_player_repeat_status);



        // =========================================================
        // 🚀 [순정 플레이어 붕괴 방지 & 가사 절대 영역 엔진]
        // 1. 앨범 이미지가 없을 때 하단 바가 솟구치는 것을 막기 위해 투명 사각형(Dummy Box)을 넣습니다.
        // =========================================================
   //     FrameLayout rootPlayerMode = findViewById(R.id.layout_player_mode);
        FrameLayout albumContainer = (FrameLayout) ivAlbumArt.getParent();
        float densityVis = getResources().getDisplayMetrics().density;

        // 🚀 [자기 아이디어 적용!] 더미 박스 장착! (앨범 이미지가 숨겨져도 상자 크기를 190dp로 강제 유지!)
        View dummyBox = new View(this);
        dummyBox.setMinimumHeight((int) (190 * densityVis));
        albumContainer.addView(dummyBox, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        visualizerView = new AudioVisualizerView(this);
        visualizerView.setVisibility(View.GONE);

        lyricScrollView = new ScrollView(this);
        lyricScrollView.setVisibility(View.GONE);
        lyricScrollView.setScrollbarFadingEnabled(true);

        tvLyrics = new TextView(this);
        tvLyrics.setTextColor(0xFFFFFFFF);
        tvLyrics.setTextSize(16f);
        tvLyrics.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        tvLyrics.setLineSpacing(10f, 1.2f);
        tvLyrics.setPadding(20, 40, 20, 40);
        tvLyrics.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);

        lyricScrollView.addView(tvLyrics, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        overlayLp.topMargin = (int) (40 * densityVis); // 💡 상단 상태바 피하기
        overlayLp.bottomMargin = (int) (180 * densityVis); // 💡 하단 프로그레스 바 영역 피하기
        overlayLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        rootPlayerMode.addView(visualizerView, overlayLp);
        rootPlayerMode.addView(lyricScrollView, overlayLp);
        View containerProgressView = findViewById(R.id.container_progress);
        if (containerProgressView != null) {
            containerProgressView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    updateVisualizerAndLyricsLayout();
                }
            });
        }
        // =========================================================
        // =========================================================
        // 🚀 상위 상대 레이아웃(parentRel) 획득 및 알약 UI 세팅
        // =========================================================
        LinearLayout statusIconsLayout = (LinearLayout) ivPlayerShuffleStatus.getParent();
        RelativeLayout parentRel = (RelativeLayout) statusIconsLayout.getParent();

        layoutAudioQualityContainer = new LinearLayout(this) {
            @Override
            public void setVisibility(int visibility) {
                // 스펙트럼이나 가사 모드가 켜져 있을 땐 알약을 강제로 숨깁니다!
                if (isVisualizerShowing && visibility == VISIBLE) return;
                super.setVisibility(visibility);
            }
        };
        layoutAudioQualityContainer.setOrientation(LinearLayout.VERTICAL);
        layoutAudioQualityContainer.setVisibility(View.GONE);

        int capsuleBgColor = 0x44000000;
        float capsuleRadius = 20 * densityVis;

        tvQualityExt = new TextView(this);
        tvQualityExt.setTextSize(13);
        tvQualityExt.setTextColor(0xbbFFFFFF);
        tvQualityExt.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tvQualityExt.setIncludeFontPadding(false);
        tvQualityExt.setPadding((int) (16 * densityVis), (int) (8 * densityVis), (int) (16 * densityVis), (int) (8 * densityVis));
        tvQualityExt.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        GradientDrawable bgExt = new GradientDrawable();
        bgExt.setColor(capsuleBgColor);
        bgExt.setCornerRadius(capsuleRadius);
        tvQualityExt.setBackground(bgExt);

        tvQualityFormat = new TextView(this);
        tvQualityFormat.setTextSize(13);
        tvQualityFormat.setTextColor(0xbbFFFFFF);
        tvQualityFormat.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tvQualityFormat.setIncludeFontPadding(false);
        tvQualityFormat.setPadding((int) (16 * densityVis), (int) (8 * densityVis), (int) (16 * densityVis), (int) (8 * densityVis));
        tvQualityFormat.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        GradientDrawable bgFormat = new GradientDrawable();
        bgFormat.setColor(capsuleBgColor);
        bgFormat.setCornerRadius(capsuleRadius);
        tvQualityFormat.setBackground(bgFormat);

        tvQualityBitrate = new TextView(this);
        tvQualityBitrate.setTextSize(13);
        tvQualityBitrate.setTextColor(0xbbFFFFFF);
        tvQualityBitrate.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tvQualityBitrate.setIncludeFontPadding(false);
        tvQualityBitrate.setPadding((int) (16 * densityVis), (int) (8 * densityVis), (int) (16 * densityVis), (int) (8 * densityVis));
        tvQualityBitrate.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        GradientDrawable bgBitrate = new GradientDrawable();
        bgBitrate.setColor(capsuleBgColor);
        bgBitrate.setCornerRadius(capsuleRadius);
        tvQualityBitrate.setBackground(bgBitrate);

        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.bottomMargin = (int) (6 * densityVis);
        lp1.gravity = Gravity.LEFT;

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.bottomMargin = (int) (6 * densityVis);
        lp2.gravity = Gravity.LEFT;

        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp3.gravity = Gravity.LEFT;

        layoutAudioQualityContainer.addView(tvQualityExt, lp1);
        layoutAudioQualityContainer.addView(tvQualityFormat, lp2);
        layoutAudioQualityContainer.addView(tvQualityBitrate, lp3);

        RelativeLayout.LayoutParams containerLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        containerLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        containerLp.addRule(RelativeLayout.BELOW, R.id.tv_player_track_count);
        containerLp.leftMargin = (int) (densityVis);
        containerLp.topMargin = (int) (16 * densityVis);
        parentRel.addView(layoutAudioQualityContainer, containerLp);

        // 🚀 하트 아이콘
        LinearLayout verticalWrapper = new LinearLayout(this);
        verticalWrapper.setOrientation(LinearLayout.VERTICAL);
        verticalWrapper.setGravity(Gravity.RIGHT);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) statusIconsLayout.getLayoutParams();
        parentRel.removeView(statusIconsLayout);

        statusIconsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        verticalWrapper.addView(statusIconsLayout);

        tvPlayerFavoriteStatus = new TextView(this);
        tvPlayerFavoriteStatus.setText("♥");
        tvPlayerFavoriteStatus.setTextSize(20);
        tvPlayerFavoriteStatus.setVisibility(View.GONE);

        LinearLayout.LayoutParams heartLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        heartLp.topMargin = (int) (8 * densityVis);
        heartLp.rightMargin = (int) (2 * densityVis);
        verticalWrapper.addView(tvPlayerFavoriteStatus, heartLp);

        parentRel.addView(verticalWrapper, params);

        try {
            Set<String> savedFavs = prefs.getStringSet("favorites", new HashSet<String>());
            favoritePaths = new HashSet<>(savedFavs);
        } catch (Exception e) {}

        updatePlayerStatusIndicators();

        setupMenuButton(btnNowPlaying, R.drawable.music_circle, "icon_now_playing.png");
        setupMenuButton(btnPlay, R.drawable.music_list, "icon_music.png");
        setupMenuButton(btnBluetooth, R.drawable.bluetooth_circle, "icon_bluetooth.png");
        setupMenuButton(btnSettings, R.drawable.setting_circle, "icon_setting.png");
        setupMenuButton(btnRadio, R.drawable.radio_circle, "icon_radio.png");
        setupMenuButton(btnWebServer, R.drawable.file_sync, "icon_server.png");

        btnWebServer.setOnClickListener(v -> { changeScreen(STATE_WEBSERVER); clickFeedback(); });
        btnNowPlaying.setOnClickListener(v -> {
            if (currentPlaylist.isEmpty()) Toast.makeText(MainActivity.this, t("No music is currently playing."), Toast.LENGTH_SHORT).show();
            else changeScreen(STATE_PLAYER);
        });
        btnPlay.setOnClickListener(v -> {
            currentBrowserMode = BROWSER_ROOT;
            if (customLibrary.isEmpty() && !isCustomScanning) startMediaLibraryScan();
            changeScreen(STATE_BROWSER);
            if (isCustomScanning) showLoadingPopup();
        });
        btnSettings.setOnClickListener(v -> { currentSettingsDepth = 0; changeScreen(STATE_SETTINGS); });
        btnBluetooth.setOnClickListener(v -> changeScreen(STATE_BLUETOOTH));
        btnRadio.setOnClickListener(v -> {
            try {
                startActivity(new Intent().setClassName("com.mediatek.FMRadio", "com.mediatek.FMRadio.FMRadioActivity"));
            } catch (Exception e) {
                Intent b = getPackageManager().getLaunchIntentForPackage("com.mediatek.FMRadio");
                if (b != null) startActivity(b);
            }
        });
        btnScanBt.setOnClickListener(v -> startBluetoothScan());
        btnScanWifi.setOnClickListener(v -> startWifiScan());

        clockHandler.post(clockTask);

        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(systemStatusReceiver, filter);

        IntentFilter mediaFilter = new IntentFilter();
        mediaFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        mediaFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        mediaFilter.addDataScheme("file");
        registerReceiver(systemStatusReceiver, mediaFilter);

        btnNowPlaying.requestFocus();

        applyThemeToMainMenu();
        applyThemeToPlayerUI();
        triggerAutoReconnect();

        boolean rebootToTheme = prefs.getBoolean("reboot_to_theme", false);
        if (rebootToTheme) {
            prefs.edit().remove("reboot_to_theme").commit();
            isNavigatingToSubMenu = true;
            changeScreen(STATE_SETTINGS);
            com.themoon.y1.managers.SettingsMenuManager.getInstance(this).lastSettingsFocusIndex = 2;
            buildThemeSelectorUI();
            isNavigatingToSubMenu = false;
        } else {
            btnNowPlaying.requestFocus();
        }

        // =========================================================
        // 🚀 [셔플/반복 재생 팝업 오버레이 조립 엔진]
        // =========================================================
        float dOver = getResources().getDisplayMetrics().density;
        layoutShuffleRepeatOverlay = new LinearLayout(this);
        layoutShuffleRepeatOverlay.setOrientation(LinearLayout.VERTICAL);
        layoutShuffleRepeatOverlay.setGravity(Gravity.CENTER);
        layoutShuffleRepeatOverlay.setVisibility(View.GONE); // 평소엔 꽁꽁 숨겨둡니다!

        // 예쁜 반투명 검정 배경과 둥근 테두리 적용
        GradientDrawable overlayBg = new GradientDrawable();
        overlayBg.setColor(ThemeManager.getOverlayBackgroundColor() | 0xEE000000);
        overlayBg.setCornerRadius(16 * dOver);
        overlayBg.setStroke((int)(2 * dOver), 0x88FFFFFF);
        layoutShuffleRepeatOverlay.setBackground(overlayBg);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            layoutShuffleRepeatOverlay.setElevation(35 * dOver); // 🚀 [API 21+ 그림자 추가, 구버전 크래시 방지]
        }
        layoutShuffleRepeatOverlay.setPadding((int)(10*dOver), (int)(15*dOver), (int)(10*dOver), (int)(15*dOver));

        // 우리가 뚫어놓은 만능 버튼 생성기 재활용!
        btnOverlayShuffle = createListButton("");
        btnOverlayRepeat = createListButton("");

        // 🚀 [취소 방어막 및 조향 장치] 팝업이 떴을 때 휠(21, 22)로 버튼 이동, 백/메뉴 버튼으로 창 닫기!
        View.OnKeyListener closeOverlayListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21 || keyCode == 19 || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (btnOverlayShuffle != null) {
                            btnOverlayShuffle.requestFocus();
                            clickFeedback();
                        }
                        return true;
                    }
                    if (keyCode == 22 || keyCode == 20 || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (btnOverlayRepeat != null) {
                            btnOverlayRepeat.requestFocus();
                            clickFeedback();
                        }
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                        clickFeedback();
                        layoutShuffleRepeatOverlay.setVisibility(View.GONE);
                        return true;
                    }
                }
                return false;
            }
        };
        btnOverlayShuffle.setOnKeyListener(closeOverlayListener);
        btnOverlayRepeat.setOnKeyListener(closeOverlayListener);

        // 🚀 [적용 및 실시간 모드 변환 엔진] 가운데 버튼을 누를 때 창을 닫지 않고 상태만 계속 토글합니다!
        btnOverlayShuffle.setOnClickListener(v -> {
            clickFeedback();
            isShuffleMode = !isShuffleMode;
            if (com.themoon.y1.managers.AudioPlayerManager.getInstance() != null) {
                com.themoon.y1.managers.AudioPlayerManager.getInstance().setShuffleMode(isShuffleMode);
            }
            try { prefs.edit().putBoolean("shuffle", isShuffleMode).commit(); } catch (Exception e) {}
            updatePlayerStatusIndicators(); // 💡 뒷배경 플레이어 아이콘 즉시 동기화
            btnOverlayShuffle.setText("🔀 " + t("Shuffle") + ": " + (isShuffleMode ? t("ON") : t("OFF")));
        });

        btnOverlayRepeat.setOnClickListener(v -> {
            clickFeedback();
            repeatMode = (repeatMode + 1) % 3;
            try { prefs.edit().putInt("repeat_mode", repeatMode).commit(); } catch (Exception e) {}
            updatePlayerStatusIndicators(); // 💡 뒷배경 플레이어 아이콘 즉시 동기화
            String[] repText = {"OFF", "ONE", "ALL"};
            btnOverlayRepeat.setText("🔁 " + t("Repeat") + ": " + t(repText[repeatMode]));
        });

        layoutShuffleRepeatOverlay.addView(btnOverlayShuffle);
        layoutShuffleRepeatOverlay.addView(btnOverlayRepeat);

        FrameLayout.LayoutParams overlayLpTop = new FrameLayout.LayoutParams(
                (int)(260 * dOver), // 적당한 가로 넓이
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        overlayLpTop.topMargin = (int) (60 * dOver); // 화면 맨 위에서 살짝 띄움

        // 플레이어 화면 최상단 뼈대에 단단히 고정!
        FrameLayout rootPlayer = findViewById(R.id.layout_player_mode);
        if(rootPlayer != null) rootPlayer.addView(layoutShuffleRepeatOverlay, overlayLpTop);
        // =========================================================

        applyScreenFilter();
    }

    // 1. 파일 개수 카운터 (폴더 경로를 받아서 셉니다)
    private void countAudioFiles(File folder) {
        if (!folder.exists())
            return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory())
                    countAudioFiles(f);
                else if (isAudioFile(f))
                    totalAudioFiles++;
            }
        }
    }

    // 🚀 [자체 제작 1] 확장자를 보고 음질 계급(서열)을 매기는 판독기
    private int getAudioQualityScore(File f) {
        String ext = f.getName().toLowerCase();
        if (ext.endsWith(".flac"))
            return 6; // 황제
        if (ext.endsWith(".wav"))
            return 5;
        if (ext.endsWith(".ape") || ext.endsWith(".alac"))
            return 4;
        if (ext.endsWith(".opus"))
            return 3;
        if (ext.endsWith(".ogg"))
            return 2;
        if (ext.endsWith(".m4a") || ext.endsWith(".aac"))
            return 1;
        return 0; // MP3, WMA 등 평민
    }

    // 🚀 [자체 제작 2] 중복 곡을 걸러내고 최고 음질 1개만 남기는 '입구컷 필터'
    private void filterDuplicateSongs(List<SongItem> library) {
        java.util.HashMap<String, SongItem> bestSongs = new java.util.HashMap<>();

        for (SongItem song : library) {
            // 💡 [암호키 생성] "가수 이름 + 노래 제목" (대소문자 무시, 공백 제거로 정확도 1000% 향상!)
            String key = (song.artist + "_" + song.title).toLowerCase().replaceAll("\\s", "");

            if (bestSongs.containsKey(key)) {
                SongItem existing = bestSongs.get(key);
                int newScore = getAudioQualityScore(song.file);
                int oldScore = getAudioQualityScore(existing.file);

                // 새로 들어온 곡의 음질 계급이 더 높다면? 기존 곡을 쓰레기통에 버리고 왕좌 차지!
                if (newScore > oldScore) {
                    bestSongs.put(key, song);
                }
                // (음질이 같거나 낮으면 아무것도 안 하고 조용히 무시함 = 중복 제거)
            } else {
                bestSongs.put(key, song); // 처음 보는 곡이면 무조건 프리패스 통과!
            }
        }

        // 도서관 바구니를 깨끗하게 비우고, 왕좌를 차지한 최고 음질 알맹이들만 다시 채워 넣습니다!
        library.clear();
        library.addAll(bestSongs.values());
    }

    // =======================================================
    // 🚀 [자체 제작 3] 대소문자 무시! 폴더 내 'cover' 이미지 자동 탐색기
    // =======================================================
    public File findFolderCover(File folder) {
        if (folder == null || !folder.exists())
            return null;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                // 대소문자 구분을 없애기 위해 소문자로 강제 변환 후 검사!
                String name = f.getName().toLowerCase();

                // 이름이 'cover.' 으로 시작하고, 이미지 확장자(.jpg, .jpeg, .png)로 끝나는 파일인지 확인
                if ((name.startsWith("cover.") || name.startsWith("folder."))
                        && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                    return f; // 🎯 발견 즉시 해당 파일 리턴!
                }
            }
        }
        return null; // 못 찾으면 null 리턴
    }

    // 2. 태그 추출 및 바구니 담기 (기존 명단을 받아서 새 파일만 부분 스캔합니다!)
    private void buildCustomLibrary(File folder, List<SongItem> targetLibrary,
            HashSet<String> existingPaths) {
        if (!folder.exists())
            return;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    buildCustomLibrary(f, targetLibrary, existingPaths); // 폴더 안으로 파고들 때 명단도 같이 넘김!
                } else if (isAudioFile(f)) {
                    if (blacklist.contains(f.getAbsolutePath()))
                        continue;

                    // 🚀 [초고속 점프 엔진] 이미 명단에 있는 파일이면, 무거운 태그 검사를 광속으로 건너뜁니다!
                    if (existingPaths.contains(f.getAbsolutePath())) {
                        scannedAudioFiles++;

                        // 💡 광속으로 넘어가면 화면이 깜빡이므로, UI 갱신 부하를 막기 위해 10번에 한 번씩만 화면을 그려줍니다.
                        if (totalAudioFiles > 0 && scannedAudioFiles % 10 == 0) {
                            final int progress = (int) (((float) scannedAudioFiles / totalAudioFiles) * 100);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (pbLoadingProgress != null)
                                        pbLoadingProgress.setProgress(progress);
                                    if (tvLoadingProgress != null) {
                                        String template = t(
                                                "Scanning Media: %d%%\n(%d / %d)\nDo not turn off the screen.");
                                        tvLoadingProgress.setText(String.format(Locale.US, template, progress,
                                                scannedAudioFiles, totalAudioFiles));
                                    }
                                }
                            });
                        }
                        continue; // 여기서 바로 다음 파일로 패스!
                    }

                    // ⬇️ 여기부터는 '새로 발견된 파일'에만 실행되는 무거운 태그 추출 구역입니다.
                    String title = f.getName();
                    boolean isBook = (targetLibrary == audiobookLibrary);
                    String artist = isBook ? t("Unknown Author") : t("Unknown Artist");
                    String album = isBook ? t("Unknown Book") : t("Unknown Album");
                    String year = t("Unknown Year");
                    String genre = t("Unknown Genre");
                    int trackNum = 0;

                    try {
                        String t = null;
                        String a = null;
                        String al = null;
                        String trackStr = null;
                        String y = null;
                        String g = null;

                        // 🚀 [스캐너 가동]
                        if (f.getName().toLowerCase().endsWith(".opus")) {
                            Object[] opusTags = com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .extractOpusMetadata(f);
                            if (opusTags[0] != null)
                                t = (String) opusTags[0];
                            if (opusTags[1] != null)
                                a = (String) opusTags[1];
                            if (opusTags[2] != null)
                                al = (String) opusTags[2];
                            if (opusTags[3] != null)
                                y = (String) opusTags[3];
                            if (opusTags[4] != null)
                                g = (String) opusTags[4];
                            // 🚀 [트랙 번호 줍기!] 7번째 방에 데이터가 있으면 꺼내옵니다.
                            if (opusTags.length > 6 && opusTags[6] != null)
                                trackStr = (String) opusTags[6];

                        } else if (f.getName().toLowerCase().endsWith(".flac")) {
                            Object[] flacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .extractFlacMetadata(f);
                            if (flacTags[0] != null)
                                t = (String) flacTags[0];
                            if (flacTags[1] != null)
                                a = (String) flacTags[1];
                            if (flacTags[2] != null)
                                al = (String) flacTags[2];
                            if (flacTags[3] != null)
                                y = (String) flacTags[3];
                            if (flacTags[4] != null)
                                g = (String) flacTags[4];
                            // 🚀 [트랙 번호 줍기!] 7번째 방에 데이터가 있으면 꺼내옵니다.
                            if (flacTags.length > 6 && flacTags[6] != null)
                                trackStr = (String) flacTags[6];

                        } else if (f.getName().toLowerCase().endsWith(".m4a")) {
                            // 🚀 [도서관 장부 개조] 스캔 돌릴 때도 ALAC 파일을 발견하면
                            // 시스템 순정 부품을 무시하고 자체 제작한 애플 원자 해독기를 돌립니다!
                            Object[] alacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .extractAlacMetadata(f);
                            if (alacTags[0] != null)
                                t = (String) alacTags[0];
                            if (alacTags[1] != null)
                                a = (String) alacTags[1];
                            if (alacTags[2] != null)
                                al = (String) alacTags[2];
                            if (alacTags[3] != null)
                                y = (String) alacTags[3];
                            if (alacTags[4] != null)
                                g = (String) alacTags[4];
                            if (alacTags.length > 6 && alacTags[6] != null)
                                trackStr = (String) alacTags[6];

                        } else {
                            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            java.io.FileInputStream fis = new java.io.FileInputStream(f);
                            mmr.setDataSource(fis.getFD());

                            t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

                            // =========================================================
                            // 🚀 [앨범 아티스트 최우선 추출 엔진]
                            // 1순위로 앨범 아티스트를 찾고, 비어있으면 일반 아티스트를 찾습니다!
                            // =========================================================
                            a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
                            if (a == null || a.trim().isEmpty()) {
                                a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                            }
                            if (a == null || a.trim().isEmpty()) {
                                a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR);
                            }
                            // =========================================================

                            al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                            trackStr = mmr
                                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                            y = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
                            g = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

                            fis.close();
                            mmr.release();
                        }

                        if (t != null && !t.trim().isEmpty())
                            title = t;

                        if (a != null && !a.trim().isEmpty()) {
                            artist = a;
                        } else {
                            try {
                                String parentName = f.getParentFile().getParentFile().getName();
                                String folderName = f.getParentFile().getName();

                                if (parentName != null && !parentName.equals("Music")
                                        && !parentName.equals("Audiobooks")
                                        && !StoragePaths.isStorageVolumeName(parentName)) {
                                    artist = parentName;
                                } else if (folderName != null && !folderName.equals("Music")
                                        && !folderName.equals("Audiobooks")
                                        && !StoragePaths.isStorageVolumeName(folderName)) {
                                    artist = folderName;
                                }
                            } catch (Exception e) {
                            }
                        }

                        if (al != null && !al.trim().isEmpty()) {
                            album = al;
                        } else {
                            String folderName = f.getParentFile().getName();
                            if (folderName != null && !folderName.equals("Music") && !folderName.equals("Audiobooks")
                                    && !StoragePaths.isStorageVolumeName(folderName)) {
                                album = folderName;
                            }
                        }

                        if (y != null && !y.trim().isEmpty())
                            year = y;
                        if (g != null && !g.trim().isEmpty())
                            genre = g;

                        if (trackStr != null && !trackStr.isEmpty()) {
                            try {
                                if (trackStr.contains("/"))
                                    trackNum = Integer.parseInt(trackStr.split("/")[0].trim());
                                else
                                    trackNum = Integer.parseInt(trackStr.trim());
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e) {
                    }

                    // 새 파일 장부에 적어 넣기!
                    targetLibrary.add(new SongItem(f, title, artist, album, year, genre));
                    trackNumberMap.put(f.getAbsolutePath(), trackNum);

                    scannedAudioFiles++;
                    if (totalAudioFiles > 0) {
                        final int progress = (int) (((float) scannedAudioFiles / totalAudioFiles) * 100);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (pbLoadingProgress != null)
                                    pbLoadingProgress.setProgress(progress);
                                if (tvLoadingProgress != null) {
                                    String template = t("Scanning Media: %d%%\n(%d / %d)\nDo not turn off the screen.");
                                    tvLoadingProgress.setText(String.format(Locale.US, template, progress,
                                            scannedAudioFiles, totalAudioFiles));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    // 3. 중앙 스캔 엔진 (초고속 부분 스캔 / 양방향 동기화 적용!)
    private void startMediaLibraryScan() {
        if (isCustomScanning)
            return;
        isCustomScanning = true;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pbLoadingProgress != null)
                    pbLoadingProgress.setProgress(0);
                if (tvLoadingProgress != null)
                    tvLoadingProgress.setText(t("Counting files...\nPlease wait."));
                showLoadingPopup();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 🚀 [부분 스캔 1단계] 삭제된 파일 감지 (기존 바구니 정리)
                java.util.Iterator<SongItem> itMusic = customLibrary.iterator();
                while (itMusic.hasNext()) {
                    SongItem item = itMusic.next();
                    if (!item.file.exists()) {
                        trackNumberMap.remove(item.file.getAbsolutePath());
                        itMusic.remove(); // 파일이 지워졌으면 장부에서 즉시 파쇄!
                    }
                }

                java.util.Iterator<SongItem> itBook = audiobookLibrary.iterator();
                while (itBook.hasNext()) {
                    SongItem item = itBook.next();
                    if (!item.file.exists()) {
                        trackNumberMap.remove(item.file.getAbsolutePath());
                        itBook.remove(); // 파일이 지워졌으면 장부에서 즉시 파쇄!
                    }
                }

                // 🚀 이미 안전하게 살아있는 파일들의 '주소 명단'을 추출합니다. (검색 속도 극대화)
                HashSet<String> existingMusic = new HashSet<>();
                for (SongItem item : customLibrary)
                    existingMusic.add(item.file.getAbsolutePath());

                HashSet<String> existingBooks = new HashSet<>();
                for (SongItem item : audiobookLibrary)
                    existingBooks.add(item.file.getAbsolutePath());

                totalAudioFiles = 0;
                scannedAudioFiles = 0;

                // Re-probe volumes so Y2 microSD (/storage/sdcard1) is included after insert.
                StoragePaths.invalidate();
                rootFolder = StoragePaths.getMusicDir();
                audiobookRootFolder = StoragePaths.getAudiobooksDir();

                // Count + scan Music/Audiobooks on every volume (sdcard0 + sdcard1).
                for (java.io.File musicDir : StoragePaths.getMusicDirs())
                    countAudioFiles(musicDir);
                for (java.io.File bookDir : StoragePaths.getAudiobooksDirs())
                    countAudioFiles(bookDir);

                for (java.io.File musicDir : StoragePaths.getMusicDirs())
                    buildCustomLibrary(musicDir, customLibrary, existingMusic);
                for (java.io.File bookDir : StoragePaths.getAudiobooksDirs())
                    buildCustomLibrary(bookDir, audiobookLibrary, existingBooks);

                // 🚀 [신규 엔진 장착] 중복 폭탄 해체! 최고 음질 1개만 남기고 다 분쇄합니다!
                filterDuplicateSongs(customLibrary);
                filterDuplicateSongs(audiobookLibrary);

                // 즐겨찾기 자동 청소기
                HashSet<String> aliveSongs = new HashSet<>();
                for (SongItem song : customLibrary)
                    aliveSongs.add(song.file.getAbsolutePath());
                for (SongItem book : audiobookLibrary)
                    aliveSongs.add(book.file.getAbsolutePath());

                boolean isCleanedUp = false;
                java.util.Iterator<String> favIterator = favoritePaths.iterator();
                while (favIterator.hasNext()) {
                    String favPath = favIterator.next();
                    if (!aliveSongs.contains(favPath)) {
                        favIterator.remove();
                        isCleanedUp = true;
                    }
                }
                if (isCleanedUp)
                    prefs.edit().putStringSet("favorites", favoritePaths).commit();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isCustomScanning = false;
                        saveLibraryToCache(); // 🚀 스캔 끝났으니 즉시 영구 저장!
                        Toast
                                .makeText(
                                        MainActivity.this, t("Scan Complete! Music") + ": " + customLibrary.size() + " "
                                                + t("Books: ") + audiobookLibrary.size(),
                                        Toast.LENGTH_SHORT)
                                .show();

                        if (currentScreenState == STATE_BROWSER) {
                            if (currentBrowserMode == BROWSER_ROOT)
                                buildFileBrowserUI();
                            else if (currentBrowserMode == BROWSER_ARTISTS)
                                buildVirtualCategories("ARTIST");
                            else if (currentBrowserMode == BROWSER_ALBUMS)
                                buildVirtualCategories("ALBUM");
                            else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS)
                                buildVirtualSongs();
                            else if (currentBrowserMode == BROWSER_COVER_FLOW)
                                buildCoverFlowUI();
                        }
                    }
                });
            }
        }).start();
    }

    // 💡 [개조 완료] 화면 전체를 덮는 확실한 로딩 팝업 & 화면 꺼짐 방지 엔진
    private void showLoadingPopup() {
        if (layoutLoadingOverlay != null) {
            // 🚀 [수리 3] 자동 스캔 화면을 띄울 때도 팝업의 투명도를 100%로 확실하게 채워줍니다!
            layoutLoadingOverlay.setAlpha(1.0f);
            layoutLoadingOverlay.setVisibility(View.VISIBLE);

            // 🚀 [수리 완료] 공용 도화지(tvLoadingProgress)의 글자 크기 및 글자색 강제 초기화!
            // 이렇게 하면 주파수 조절이나 커버플로우에서 변경된 속성이 다른 스캔 작업 시 완벽하게 복구됩니다.
            if (tvLoadingProgress != null) {
                tvLoadingProgress.setTextSize(18f);
                tvLoadingProgress.setTextColor(0xFFFFFFFF);
            }

            // 🚀 [핵심 기술] 스캔하는 동안 시스템이 화면을 절대 끄지 못하도록 강제 명령을 내립니다!
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            final Handler checker = new Handler();
            checker.post(new Runnable() {
                @Override
                public void run() {
                    // 🚀 [버그 수리 완료!] 음악 스캔이나 라디오 스캔 중 어느 하나라도 돌고 있으면 창을 닫지 않습니다!
                    if (!isCustomScanning && !isRadioScanning) {
                        layoutLoadingOverlay.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        checker.postDelayed(this, 200); // 0.2초마다 검사
                    }
                }
            });
        }
    }
    // 💡 [개조 완료] 다운로드 진행률(%)과 용량(MB)을 실시간 팝업으로 보여주는 엔진!
    // 위쪽에 있는 이 주석 바로 위에 있는 refreshWidgets() 함수를 통째로 덮어쓰세요!

    private void refreshWidgets() {
        // 1. 디지털 시계 위젯 업데이트
        if (tvWidgetClock != null) {
            ThemeManager.MenuElement el = widgetViewRegistry.get(tvWidgetClock);
            // 🚀 [핵심 방어막] 이 위젯이 특정 버튼을 감시 중(visibleOnFocus)이라면, 설정 스위치로 전원을 강제 종료하지 않습니다!
            if (el == null || el.visibleOnFocus == null || el.visibleOnFocus.trim().isEmpty()) {
                tvWidgetClock.setVisibility(isWidgetClockOn ? View.VISIBLE : View.GONE);
            }

            // 💡 화면에 보일 때(VISIBLE)만 시간을 새로고침하여 과부하 방지
            if (tvWidgetClock.getVisibility() == View.VISIBLE) {
                float d = getResources().getDisplayMetrics().density;
                tvWidgetClock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, (currentClockSize * 2.1f) * d);
                tvWidgetClock.setLineSpacing(0, 1.1f);

                Date now = new Date();
                String widgetTimeFormat = is24HourFormat ? "HH:mm" : "hh:mm";
                SimpleDateFormat sdfTime = new SimpleDateFormat(widgetTimeFormat,
                        Locale.US);
                SimpleDateFormat sdfDate = new SimpleDateFormat("EEE, MMM dd", Locale.US);
                String timeStr = sdfTime.format(now);
                String dateStr = sdfDate.format(now);
                String fullText = timeStr + "\n" + dateStr;

                android.text.SpannableString spannable = new android.text.SpannableString(fullText);
                spannable.setSpan(new android.text.style.RelativeSizeSpan(0.47f), timeStr.length() + 1,
                        fullText.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new android.text.style.StyleSpan(Typeface.NORMAL),
                        timeStr.length() + 1, fullText.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvWidgetClock.setText(spannable);
            }
        }

        // 2. 막대형 배터리 위젯 업데이트
        if (widgetBatteryView != null) {
            ThemeManager.MenuElement el = widgetViewRegistry.get(widgetBatteryView);
            if (el == null || el.visibleOnFocus == null || el.visibleOnFocus.trim().isEmpty()) {
                widgetBatteryView.setVisibility(isWidgetBatteryOn ? View.VISIBLE : View.GONE);
            }
            if (widgetBatteryView.getVisibility() == View.VISIBLE) {
                widgetBatteryView.setColor(ThemeManager.getTextColorPrimary());
                Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryIntent != null) {
                    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                            || status == BatteryManager.BATTERY_STATUS_FULL);
                    if (level != -1 && scale != -1)
                        widgetBatteryView.setBatteryLevel((int) ((level / (float) scale) * 100), isCharging);
                }
            }
        }

        // 3. 앨범 프리뷰 위젯 업데이트
        if (ivWidgetAlbum != null && tvWidgetAlbumTitle != null && tvWidgetAlbumArtist != null) {
            View parent = (View) ivWidgetAlbum.getParent();
            ThemeManager.MenuElement el = widgetViewRegistry.get(parent);

            if (el == null || el.visibleOnFocus == null || el.visibleOnFocus.trim().isEmpty()) {
                if (parent != null)
                    parent.setVisibility(isWidgetAlbumOn ? View.VISIBLE : View.GONE);
            }

            if (parent != null && parent.getVisibility() == View.VISIBLE) {
                // =========================================================
                // 🚀 [앨범 이미지 텍스트 숨김 & 자동 꽉 채우기 (CENTER_CROP) 엔진]
                // =========================================================
                // (주의: ThemeManager에서 파싱하는 변수명이 textPosition인지 확인하세요!)
                boolean hideText = el != null && "none".equalsIgnoreCase(el.textPosition);
                if (hideText) {
                    tvWidgetAlbumTitle.setVisibility(View.GONE);
                    tvWidgetAlbumArtist.setVisibility(View.GONE);

                    // 1. 이미지가 박스를 넘어가더라도 꽉 차게 잘라냅니다!
                    ivWidgetAlbum.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    // 2. 부모 박스(Width/Height)를 완전히 덮도록 사이즈를 MATCH_PARENT로 확장!
                    ViewGroup.LayoutParams imgLp = ivWidgetAlbum.getLayoutParams();
                    imgLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    imgLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    ivWidgetAlbum.setLayoutParams(imgLp);
                } else {
                    tvWidgetAlbumTitle.setVisibility(View.VISIBLE);
                    tvWidgetAlbumArtist.setVisibility(View.VISIBLE);
                    tvWidgetAlbumTitle.setText(tvPlayerTitle != null ? tvPlayerTitle.getText() : "");
                    tvWidgetAlbumArtist.setText(tvPlayerArtist != null ? tvPlayerArtist.getText() : "");

                    // 기존 모드로 복구
                    ivWidgetAlbum.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
                // =========================================================

                if (lastAlbumArtBytes != null) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 2;
                        Bitmap bmp = BitmapFactory.decodeByteArray(lastAlbumArtBytes,
                                0, lastAlbumArtBytes.length, opts);
                        ivWidgetAlbum.setImageBitmap(bmp);
                    } catch (Exception e) {
                    }
                } else {
                    ivWidgetAlbum.setImageBitmap(
                            ThemeManager.getCustomIcon("icon_default_album.png", this, R.drawable.default_album));
                }
            }
        }

        // 4. 아날로그 시계 업데이트
        if (customAnalogClockView != null) {
            ThemeManager.MenuElement el = widgetViewRegistry.get(customAnalogClockView);
            if (el == null || el.visibleOnFocus == null || el.visibleOnFocus.trim().isEmpty()) {
                View parent = (View) customAnalogClockView.getParent();
                if (parent != null && "analog_wrapper".equals(parent.getTag()))
                    parent.setVisibility(isWidgetAnalogClockOn ? View.VISIBLE : View.GONE);
                else
                    customAnalogClockView.setVisibility(isWidgetAnalogClockOn ? View.VISIBLE : View.GONE);
            }
        }

        // 5. 원형 배터리 위젯 업데이트
        if (customCircularBatteryView != null) {
            ThemeManager.MenuElement el = widgetViewRegistry.get(customCircularBatteryView);
            if (el == null || el.visibleOnFocus == null || el.visibleOnFocus.trim().isEmpty()) {
                View parent = (View) customCircularBatteryView.getParent();
                if (parent != null && "circular_wrapper".equals(parent.getTag()))
                    parent.setVisibility(isWidgetCircularBatteryOn ? View.VISIBLE : View.GONE);
                else
                    customCircularBatteryView.setVisibility(isWidgetCircularBatteryOn ? View.VISIBLE : View.GONE);
            }

            if (customCircularBatteryView.getVisibility() == View.VISIBLE) {
                Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryIntent != null) {
                    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                            || status == BatteryManager.BATTERY_STATUS_FULL);
                    if (level != -1 && scale != -1)
                        customCircularBatteryView.setBatteryLevel((int) ((level / (float) scale) * 100), isCharging);
                }
            }
        }

        // 6. 다이내믹 포커스 이미지 위젯 업데이트
        if (ivWidgetFocusImage != null) {
            ThemeManager.MenuElement el = widgetViewRegistry.get(ivWidgetFocusImage);
            // 🚀 [깜빡임 버그 수리 완료]
            // 타겟(visibleOnFocus)이 비어있는 '글로벌 만능 위젯' 모드일 때는,
            // 1초 타이머가 강제로 화면을 끄지 못하게 간섭 로직을 완전히 삭제합니다!
            // (화면 끄고 켜기는 포커스 이동 엔진이 100% 전담하여 60프레임으로 매끄럽게 작동합니다)
            if (el != null && el.visibleOnFocus != null && !el.visibleOnFocus.trim().isEmpty()) {
                ivWidgetFocusImage.setVisibility(isWidgetFocusImageOn ? View.VISIBLE : View.GONE);
            }
        }
    } // refreshWidgets 끝
      // 💡 [추가] 문자열에서 첫 글자를 뽑아내어 화면에 띄워주는 함수
      // =======================================================
      // 🚀 [퀵 스크롤 엔진] 현재 리스트에 존재하는 알파벳만 뽑아서 상단에 버튼으로 나열합니다!
      // =======================================================

    private void buildAlphabetIndexBar() {
        if (currentScrollIndexList == null || currentScrollIndexList.isEmpty()) {
            hzIndexScroll.setVisibility(View.GONE);
            return;
        }

        layoutIndexContainer.removeAllViews();
        java.util.LinkedHashMap<String, Integer> indexMap = new java.util.LinkedHashMap<>();

        // 1. 리스트를 훑어서 첫 글자가 나타나는 최초의 위치(인덱스)를 기록합니다.
        for (int i = 0; i < currentScrollIndexList.size(); i++) {
            String title = currentScrollIndexList.get(i);
            String firstChar = String.valueOf(getInitialChar(title)); // 💡 아까 만든 도우미 함수 활용
            if (!indexMap.containsKey(firstChar)) {
                indexMap.put(firstChar, i);
            }
        }

        float density = getResources().getDisplayMetrics().density;
        int themeColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;

        // 2. 추출된 알파벳 버튼들을 상단 가로 스크롤에 렌더링합니다.
        for (final java.util.Map.Entry<String, Integer> entry : indexMap.entrySet()) {
            final TextView tvAlpha = new TextView(this);
            tvAlpha.setText(entry.getKey());
            tvAlpha.setTextSize(18f);
            tvAlpha.setTextColor(ThemeManager.getTextColorPrimary());
            tvAlpha.setGravity(Gravity.CENTER);
            tvAlpha.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
            tvAlpha.setFocusable(true);
            tvAlpha.setClickable(true);
            tvAlpha.setPadding((int) (15 * density), 0, (int) (15 * density), 0);

            // 🚀 1. 포커스 애니메이션 + 자동 점프 기능 통합!
            tvAlpha.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    tvAlpha.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvAlpha.setBackgroundColor(themeColor);
                    hzIndexScroll.requestChildFocus(layoutIndexContainer, tvAlpha);

                    // 🎯 [핵심 변경] 휠을 돌려서 포커스가 닿는 즉시! 클릭 없이 리스트 위치를 동기화하여 이동시킵니다.
                    if (listVirtualSongs != null) {
                        listVirtualSongs.setSelectionFromTop(entry.getValue(), 0);
                    }
                } else {
                    tvAlpha.setTextColor(ThemeManager.getTextColorPrimary());
                    tvAlpha.setBackgroundColor(0x00000000); // 투명
                }
            });

            // 🚀 [수정] 휠(UP/DOWN)을 돌렸을 때 무한 루프(끝에서 처음으로) 돌아가도록 조향 장치 업그레이드!
            tvAlpha.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int childCount = layoutIndexContainer.getChildCount();
                    int currentIndex = layoutIndexContainer.indexOfChild(v);

                    // 휠을 위로 돌림 (왼쪽으로 이동, 첫 칸이면 맨 끝으로 점프!)
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == 19 || keyCode == 21) {
                        int prevIndex = (currentIndex - 1 + childCount) % childCount;
                        layoutIndexContainer.getChildAt(prevIndex).requestFocus();
                        clickFeedback(); // 💡 이동할 때마다 가벼운 진동/소리 추가
                        return true;
                    }
                    // 휠을 아래로 돌림 (오른쪽으로 이동, 끝 칸이면 맨 처음으로 점프!)
                    else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == 20 || keyCode == 22) {
                        int nextIndex = (currentIndex + 1) % childCount;
                        layoutIndexContainer.getChildAt(nextIndex).requestFocus();
                        clickFeedback(); // 💡 이동할 때마다 가벼운 진동/소리 추가
                        return true;
                    }
                }
                return false;
            });

            // 🚀 가운데 버튼 클릭 시: 퀵 바만 조용히 닫고 현재 위치의 곡으로 포커스 넘기기!
            tvAlpha.setOnClickListener(v -> {
                clickFeedback();

                // 1. 휠을 돌릴 때 이미 리스트가 움직였으므로, 여기선 퀵 바만 깔끔하게 숨깁니다!
                hzIndexScroll.setVisibility(View.GONE);

                // 2. 퀵 바가 사라지면서 휠 포커스가 허공에 뜨지 않게,
                // 지금 화면에 맞춰진 그 알파벳의 첫 번째 곡에 자석처럼 포커스를 딱! 꽂아줍니다.
                listVirtualSongs.postDelayed(() -> {
                    int visiblePos = entry.getValue() - listVirtualSongs.getFirstVisiblePosition();
                    if (visiblePos >= 0 && visiblePos < listVirtualSongs.getChildCount()) {
                        listVirtualSongs.getChildAt(visiblePos).requestFocus();
                    } else if (listVirtualSongs.getChildCount() > 0) {
                        listVirtualSongs.getChildAt(0).requestFocus(); // 튕김 방지용 안전장치
                    }
                }, 50);
            });

            layoutIndexContainer.addView(tvAlpha, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        // 상단 바 표시
        hzIndexScroll.setVisibility(View.GONE);
    }

    public void showFastScrollLetter(String rawText) {
        // 브라우저 모드(리스트 화면)가 아니면 띄우지 않습니다.
        if (tvFastScrollLetter == null || currentScreenState != STATE_BROWSER)
            return;

        // 버튼 텍스트 앞에 붙어있는 꾸밈용 이모지들을 싹 지우고 순수 제목만 남깁니다.
        String clean = rawText.replace("📁 ", "").replace("👤 ", "")
                .replace("💿 ", "").replace("🎵 ", "")
                .replace("📦 [INSTALL] ", "").trim();

        if (clean.isEmpty())
            return;
        // 첫 글자 1개만 추출 (무조건 대문자로 변환)
        String firstChar = String.valueOf(getInitialChar(clean));
        // 🚀 [그래픽 과부하 방지] 이미 화면에 떠 있는 알파벳과 '똑같은' 알파벳이라면?
        // 무거운 박스 그리기 작업을 생략하고 글자가 사라지는 타이머만 연장해 줍니다!
        if (tvFastScrollLetter.getVisibility() == View.VISIBLE
                && tvFastScrollLetter.getText().toString().equals(firstChar)) {
            fastScrollHandler.removeCallbacks(hideFastScrollTask);
            fastScrollHandler.postDelayed(hideFastScrollTask, 800);
            return; // 여기서 함수를 멈춰버립니다.
        }

        tvFastScrollLetter.setText(firstChar);

        // 🚀 현재 적용된 테마의 강조 색상을 가져옵니다.
        int bgColor = ThemeManager.getListButtonFocusedBg();

        // 💡 [지능형 대비 엔진] 배경색의 RGB 값을 쪼개어 사람 눈이 느끼는 진짜 밝기(Luminance)를 수치로 계산합니다!
        int r = android.graphics.Color.red(bgColor);
        int g = android.graphics.Color.green(bgColor);
        int b = android.graphics.Color.blue(bgColor);
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b);

        // 🚀 밝기 점수가 160 이상(밝은 색)이면 글씨를 짙은 검은색(0xFF000000)으로, 어두우면 흰색(0xFFFFFFFF)으로 자동 반전!
        int adaptiveTextColor = (luminance > 160) ? 0xFF000000 : 0xFFFFFFFF;

        tvFastScrollLetter.setTextColor(adaptiveTextColor);
        tvFastScrollLetter.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);

        GradientDrawable letterBg = new GradientDrawable();
        letterBg.setColor(bgColor | 0xDD000000); // 살짝 반투명하게 덮기
        letterBg.setCornerRadius(15 * getResources().getDisplayMetrics().density); // 둥근 모서리
        tvFastScrollLetter.setBackground(letterBg);

        tvFastScrollLetter.setVisibility(View.VISIBLE);

        // 0.8초 동안 휠 조작이 없으면 글자가 자동으로 스르륵 사라지도록 타이머 리셋
        fastScrollHandler.removeCallbacks(hideFastScrollTask);
        fastScrollHandler.postDelayed(hideFastScrollTask, 800);
    }

    // 💡 [수정 완료] 메인 화면 테마 적용기. 동적 렌더링 엔진 호출 추가!
    private void applyThemeToMainMenu() {
        try {
            if (ivMainBg != null) {
                int themeColor = ThemeManager.getOverlayBackgroundColor();
                int softTint = (themeColor & 0x00FFFFFF) | 0x66000000;
                ivMainBg.setColorFilter(softTint, android.graphics.PorterDuff.Mode.SRC_ATOP);
            }

            View statusBar = findViewById(R.id.layout_status_bar);
            if (statusBar != null) {
                statusBar.setBackgroundColor(ThemeManager.getStatusBarBackgroundColor());
            }

            int primary = ThemeManager.getTextColorPrimary();
            int secondary = ThemeManager.getTextColorSecondary();

            // 우측 빈 공간의 곡 제목/가수 및 상단 상태바(시계, 배터리) 글자색 덮어쓰기
            if (tvMenuPreviewTitle != null) tvMenuPreviewTitle.setTextColor(primary);
            if (tvMenuPreviewArtist != null) tvMenuPreviewArtist.setTextColor(secondary);
            if (tvStatusClock != null) tvStatusClock.setTextColor(primary);
            if (tvStatusBattery != null) tvStatusBattery.setTextColor(primary);
            if (batteryIconView != null) batteryIconView.setColor(primary);

            if (tvPlayerTimeCurrent != null) tvPlayerTimeCurrent.setTextColor(primary);
            if (tvPlayerTimeTotal != null) tvPlayerTimeTotal.setTextColor(primary);
            if (tvBrowserPath != null) tvBrowserPath.setTextColor(primary);

            View[] modeLayouts = new View[]{layoutSettingsMode, layoutBluetoothMode, layoutWifiMode, layoutBrightnessMode, layoutStorageMode, layoutWebServerMode};
            for (View modeLayout : modeLayouts) {
                if (modeLayout instanceof ViewGroup && ((ViewGroup) modeLayout).getChildCount() > 0 && ((ViewGroup) modeLayout).getChildAt(0) instanceof TextView) {
                    ((TextView) ((ViewGroup) modeLayout).getChildAt(0)).setTextColor(primary);
                }
            }
            if (btnScanBt != null) {
                if (!btnScanBt.hasFocus()) {
                    btnScanBt.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btnScanBt.setTextColor(primary);
                } else {
                    btnScanBt.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btnScanBt.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                }
            }
            if (btnScanWifi != null) {
                if (!btnScanWifi.hasFocus()) {
                    btnScanWifi.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btnScanWifi.setTextColor(primary);
                } else {
                    btnScanWifi.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btnScanWifi.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                }
            }
            if (tvBrightnessVal != null) tvBrightnessVal.setTextColor(primary);
            if (tvStorageDetails != null) tvStorageDetails.setTextColor(primary);
            if (layoutWebServerMode instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) layoutWebServerMode;
                for (int i = 1; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    if (child instanceof TextView && child != btnServerToggle) {
                        ((TextView) child).setTextColor(secondary);
                    }
                }
            }
            if (btnServerToggle != null) {
                if (!btnServerToggle.hasFocus()) {
                    btnServerToggle.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btnServerToggle.setTextColor(primary);
                } else {
                    btnServerToggle.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btnServerToggle.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                }
            }
            if (webServer != null) {
                updateWebServerUI();
            }
            if (tvKeyboardSsid != null) tvKeyboardSsid.setTextColor(primary);
            if (tvKeyboardInput != null) tvKeyboardInput.setTextColor(primary);
            if (tvKeyCurrent != null) tvKeyCurrent.setTextColor(primary);
            if (tvKeyPrev != null) tvKeyPrev.setTextColor(ThemeManager.getTextColorSecondary());
            if (tvKeyNext != null) tvKeyNext.setTextColor(ThemeManager.getTextColorSecondary());
            if (tvKeyPprev != null) tvKeyPprev.setTextColor(ThemeManager.getTextColorSecondary());
            if (tvKeyNnext != null) tvKeyNnext.setTextColor(ThemeManager.getTextColorSecondary());

            // 🚀 [추가] 상태바 우측 아이콘들을 테마 메인 글자색(primary)과 똑같이 도색합니다!
// 🚀 [수정] 색상 필터(setColorFilter) 대신 텍스트 컬러(setTextColor)로 변경!
            if (tvStatusPlay != null) tvStatusPlay.setTextColor(primary);
            if (ivStatusServer != null) ivStatusServer.setColorFilter(primary);
            if (ivStatusHeadphone != null) ivStatusHeadphone.setColorFilter(primary);

            // 🚀 [와이파이 지능형 도색 엔진] 와이파이도 블루투스처럼 연결 상태를 검사합니다!
            if (ivStatusWifi != null) {
                boolean isWifiConnected = false;
                try {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm != null && wm.isWifiEnabled()) {
                        WifiInfo info = wm.getConnectionInfo();
                        if (info != null && info.getNetworkId() != -1) {
                            isWifiConnected = true;
                        }
                    }
                } catch (Exception e) {}

                if (isWifiConnected) {
                    ivStatusWifi.setColorFilter(0xFF00FF00); // 🟢 연결 시 눈에 띄는 녹색!
                } else {
                    ivStatusWifi.setColorFilter(primary); // ⚪ 미연결 대기 중엔 테마 색상!
                }
            }

            // 🚀 [블루투스 지능형 도색] 블루투스는 현재 '연결 상태'를 확인해서 색상을 결정합니다!
            // 🚀 [블루투스 지능형 도색] 블루투스는 현재 '연결 상태'를 확인해서 색상을 결정합니다!
            if (ivStatusBluetooth != null) {
                boolean isBtConnected = false;
                try {
                    BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                    if (ba != null && ba.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED) {
                        isBtConnected = true;
                    }
                } catch (Exception e) {}

                if (isBtConnected) {
                    ivStatusBluetooth.setColorFilter(0xFF00BFFF); // 🔵 오디오 연결 시 맑은 파란색!
                } else {
                    ivStatusBluetooth.setColorFilter(primary); // ⚪ 미연결 대기 중엔 테마 색상!
                }
            }

            int themeFocusColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            int playerBarColor = getPlayerProgressBarColor();
            if (playerProgress != null) {
                try {
                    android.graphics.drawable.LayerDrawable layer = (android.graphics.drawable.LayerDrawable) playerProgress
                            .getProgressDrawable();
                    android.graphics.drawable.Drawable progress = layer.findDrawableByLayerId(android.R.id.progress);
                    if (progress != null)
                        progress.setColorFilter(playerBarColor, android.graphics.PorterDuff.Mode.SRC_IN);
                } catch (Exception e) {
                    playerProgress.getProgressDrawable().setColorFilter(playerBarColor,
                            android.graphics.PorterDuff.Mode.SRC_IN);
                }
                if (playerProgress instanceof android.widget.SeekBar && ((android.widget.SeekBar) playerProgress).getThumb() != null) {
                    ((android.widget.SeekBar) playerProgress).getThumb().setColorFilter(playerBarColor, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
            if (volumeProgress != null) {
                int volColor = themeFocusColor;
                int r = (volColor >> 16) & 0xFF;
                int g = (volColor >> 8) & 0xFF;
                int b = volColor & 0xFF;
                if ((r * 299 + g * 587 + b * 114) / 1000 < 80) {
                    volColor = 0xFFFFFFFF; // 어두운 배경에서 안 보이는 것 방지
                }
                try {
                    android.graphics.drawable.LayerDrawable layer = (android.graphics.drawable.LayerDrawable) volumeProgress
                            .getProgressDrawable();
                    android.graphics.drawable.Drawable progress = layer.findDrawableByLayerId(android.R.id.progress);
                    if (progress != null)
                        progress.setColorFilter(volColor, android.graphics.PorterDuff.Mode.SRC_IN);
                } catch (Exception e) {
                    volumeProgress.getProgressDrawable().setColorFilter(volColor,
                            android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
            if (pbBrightness != null) {
                try {
                    android.graphics.drawable.LayerDrawable layer = (android.graphics.drawable.LayerDrawable) pbBrightness
                            .getProgressDrawable();
                    android.graphics.drawable.Drawable progress = layer.findDrawableByLayerId(android.R.id.progress);
                    if (progress != null)
                        progress.setColorFilter(themeFocusColor, android.graphics.PorterDuff.Mode.SRC_IN);
                } catch (Exception e) {
                    pbBrightness.getProgressDrawable().setColorFilter(themeFocusColor,
                            android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }

            ViewGroup root = findViewById(android.R.id.content);
            applyFontToAllViews(root, ThemeManager.getCustomFont());

            // 🚀🚀🚀 [여기가 핵심!] 기존 낡은 메뉴를 싹 날려버리고 JSON 부품을 조립합니다!
            buildDynamicMainMenuUI();

        } catch (Exception e) {
        }
    }
    public int getPlayerProgressBarColor() {
        int barColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
        if (!ThemeManager.availableThemes.isEmpty()) {
            ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
            if (theme != null && theme.playerElements != null) {
                for (ThemeManager.MenuElement el : theme.playerElements) {
                    if ("progress_bar".equals(el.id) && el.bgColor != null && !el.bgColor.trim().isEmpty()) {
                        try {
                            barColor = android.graphics.Color.parseColor(el.bgColor.trim());
                        } catch (Exception e) {}
                        break;
                    }
                }
            }
        }
        return barColor;
    }

    public int getPlayerProgressBarBgColor() {
        int bgColor = 0x44FFFFFF;
        if (!ThemeManager.availableThemes.isEmpty()) {
            ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
            if (theme != null && theme.playerElements != null) {
                for (ThemeManager.MenuElement el : theme.playerElements) {
                    if ("progress_bar".equals(el.id) && el.progressBgColor != null && !el.progressBgColor.trim().isEmpty()) {
                        try {
                            bgColor = android.graphics.Color.parseColor(el.progressBgColor.trim());
                        } catch (Exception e) {}
                        break;
                    }
                }
            }
        }
        return bgColor;
    }

    public void updatePlayerBgOverlayVisibility() {
        View viewPlayerBgOverlay = findViewById(R.id.view_player_bg_overlay);
        if (viewPlayerBgOverlay != null) {
            if ("color".equals(currentPlayerBgMode) || "none".equals(currentPlayerBgMode)) {
                viewPlayerBgOverlay.setVisibility(View.GONE);
            } else {
                viewPlayerBgOverlay.setVisibility(View.VISIBLE);
                viewPlayerBgOverlay.setBackgroundColor(0x44000000);
            }
        }
    }

    // 💡 [지능형 대비 엔진] 현재 플레이어 배경이 밝은지 어두운지 판단하여 최적의 가사 텍스트 색상을 결정!
    public boolean isPlayerBackgroundLight() {
        if (!ThemeManager.availableThemes.isEmpty()) {
            ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
            if (theme != null && theme.playerElements != null) {
                for (ThemeManager.MenuElement el : theme.playerElements) {
                    if ("background".equals(el.id)) {
                        if (("color".equals(el.type) || "none".equals(el.type)) && el.bgColor != null && !el.bgColor.trim().isEmpty()) {
                            try {
                                int color = android.graphics.Color.parseColor(el.bgColor.trim());
                                int r = android.graphics.Color.red(color);
                                int g = android.graphics.Color.green(color);
                                int b = android.graphics.Color.blue(color);
                                double luminance = (0.299 * r + 0.587 * g + 0.114 * b);
                                return luminance > 160;
                            } catch (Exception e) {}
                        }
                    }
                }
            }
            int primaryText = ThemeManager.getTextColorPrimary();
            int r = android.graphics.Color.red(primaryText);
            int g = android.graphics.Color.green(primaryText);
            int b = android.graphics.Color.blue(primaryText);
            double textLuminance = (0.299 * r + 0.587 * g + 0.114 * b);
            return textLuminance < 130;
        }
        return false;
    }

    // 🚀 [플레이어 다이내믹 UI 렌더링 & 트랜스포머 엔진 (마스터 버전)]
    private void applyThemeToPlayerUI() {
        if (ThemeManager.availableThemes.isEmpty()) return;
        ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();

        restoreDefaultPlayerLayoutStructure();
        updateVisualizerAndLyricsLayout();

        // 💡 [핵심] JSON에 player_ui 코드가 없으면? 기본 블러 모드 유지 및 탈출!
        if (theme.playerElements == null || theme.playerElements.isEmpty()) {
            currentPlayerBgMode = "blur";
            ImageView ivBg = findViewById(R.id.iv_player_bg_blur);
            if (ivBg != null) ivBg.setBackgroundColor(0);
            updatePlayerBgOverlayVisibility();
            updateVisualizerAndLyricsLayout();
            return;
        }

        currentPlayerBgMode = "blur"; // 기본값 장전
        ImageView ivPlayerBgBlur = findViewById(R.id.iv_player_bg_blur);
        if (ivPlayerBgBlur != null) ivPlayerBgBlur.setBackgroundColor(0);
        updatePlayerBgOverlayVisibility();

        float d = getResources().getDisplayMetrics().density;
        FrameLayout rootPlayerMode = findViewById(R.id.layout_player_mode);

        View containerAlbum = findViewById(R.id.container_album_art);
        View tvTitle = findViewById(R.id.tv_player_title);
        View tvArtist = findViewById(R.id.tv_player_artist);
        View tvAlbum = findViewById(R.id.tv_player_album);
        View containerProgress = findViewById(R.id.container_progress);
        View tvTrackCount = findViewById(R.id.tv_player_track_count);

        // 🚀 [신규 장착] 셔플, 반복, 음질 정보 박스 타겟팅!
        View ivShuffle = ivPlayerShuffleStatus;
        View ivRepeat = ivPlayerRepeatStatus;
        View containerQuality = layoutAudioQualityContainer;
        if (ivShuffle != null && ivShuffle.getParent() != null) ((ViewGroup)ivShuffle.getParent()).removeView(ivShuffle);
        if (ivRepeat != null && ivRepeat.getParent() != null) ((ViewGroup)ivRepeat.getParent()).removeView(ivRepeat);
        if (containerQuality != null && containerQuality.getParent() != null) ((ViewGroup)containerQuality.getParent()).removeView(containerQuality);
        for (ThemeManager.MenuElement el : theme.playerElements) {
            View targetView = null;

            if ("background".equals(el.id)) {
                currentPlayerBgMode = el.type;
                updatePlayerBgOverlayVisibility();
                if (("color".equals(el.type) || "none".equals(el.type)) && el.bgColor != null && !el.bgColor.isEmpty()) {
                    ivPlayerBgBlur.setImageBitmap(null);
                    try {
                        // 🚀 .trim()을 달아서 쓸데없는 공백이나 찌꺼기를 완벽하게 잘라낸 순수 색상 코드만 주입!
                        ivPlayerBgBlur.setBackgroundColor(android.graphics.Color.parseColor(el.bgColor.trim()));
                    } catch (Exception e) {}
                }
                continue;
            }

            // 🎯 ID 매핑 구역
            // 🚀 [신규 장착] 선(Line) 타입 생성기! (플레이어 화면용)
            if ("line".equals(el.type)) {
                targetView = new View(this);
                targetView.setTag("player_line");
                if (el.bgColor != null && !el.bgColor.isEmpty()) {
                    try { targetView.setBackgroundColor(android.graphics.Color.parseColor(el.bgColor)); } catch (Exception e) {}
                } else {
                    targetView.setBackgroundColor(0xFFFFFFFF); // 기본색 (흰색)
                }
            }
            else if ("box".equals(el.type)) {
                targetView = new ImageView(this);
                targetView.setTag("player_box_" + el.id);
                ((ImageView)targetView).setScaleType(ImageView.ScaleType.CENTER_CROP);

                // 1. 배경색 및 라운딩(Radius) 처리
                if (el.radius > 0 || (el.bgColor != null && !el.bgColor.isEmpty())) {
                    android.graphics.drawable.GradientDrawable boxBg = new android.graphics.drawable.GradientDrawable();
                    if (el.bgColor != null && !el.bgColor.isEmpty()) {
                        try { boxBg.setColor(android.graphics.Color.parseColor(el.bgColor)); } catch (Exception e) {}
                    } else {
                        boxBg.setColor(0x00000000); // 색상이 없으면 투명
                    }
                    if (el.radius > 0) boxBg.setCornerRadius(el.radius * d);
                    targetView.setBackground(boxBg);
                }

                // 2. 이미지(icon_normal)가 등록되어 있으면 불러오기
                if (el.iconNormal != null && !el.iconNormal.trim().isEmpty()) {
                    android.graphics.Bitmap bmp = ThemeManager.getCustomIcon(el.iconNormal, this, 0);
                    if (bmp != null) {
                        ((ImageView)targetView).setImageBitmap(bmp);
                        // 🚀 [디테일] 둥근 모서리 설정 시 이미지가 모서리 밖으로 삐져나가지 않게 잘라냅니다! (API 21+)
                        if (android.os.Build.VERSION.SDK_INT >= 21 && el.radius > 0) {
                            targetView.setClipToOutline(true);
                        }
                    }
                }
            }
            // 기존 ID 매핑 구역
            else if ("album_art".equals(el.id)) targetView = containerAlbum;
            else if ("title".equals(el.id)) targetView = tvTitle;
            else if ("artist".equals(el.id)) targetView = tvArtist;
            else if ("album".equals(el.id) || "album_title".equals(el.id)) targetView = tvAlbum;
            else if ("progress_bar".equals(el.id)) targetView = containerProgress;
            else if ("track_count".equals(el.id)) targetView = tvTrackCount;
                // 🚀 [추가] 새로운 타겟 연결
            else if ("shuffle_icon".equals(el.id)) targetView = ivShuffle;
            else if ("repeat_icon".equals(el.id)) targetView = ivRepeat;
            else if ("quality_info".equals(el.id)) targetView = containerQuality;

            if (targetView != null) {
                // 🚀 [커스텀 테마 가사/스펙트럼 모드 연동]
                // 가사나 스펙트럼이 켜진 상태에서는 기본 테마 적용시처럼 프로그레시브 바를 하단에 배치하고,
                // 노래 정보(제목, 아티스트, 파일정보, 셔플/반복 아이콘, 커스텀 박스/선)는 안 보이게 처리합니다!
                if (isVisualizerShowing) {
                    if ("album_art".equals(el.id) || "title".equals(el.id) || "artist".equals(el.id) || "album".equals(el.id) || "album_title".equals(el.id)) {
                        targetView.setVisibility(View.INVISIBLE);
                        continue;
                    } else if ("progress_bar".equals(el.id)) {
                        targetView.setVisibility(View.VISIBLE);
                        // 커스텀 좌표(rootPlayerMode)로 이동하지 않고 기본 테마 구조(player_content_layout)에 유지!
                    } else {
                        targetView.setVisibility(View.GONE);
                        continue;
                    }
                } else {
                    targetView.setVisibility(View.VISIBLE);
                }

                if (!(isVisualizerShowing && "progress_bar".equals(el.id))) {
                    // 부품을 강제로 뜯어와 도화지에 붙이는 마법
                    ViewGroup currentParent = (ViewGroup) targetView.getParent();
                if (currentParent != null && currentParent != rootPlayerMode) {
                    if (targetView == containerAlbum && currentParent == findViewById(R.id.player_content_layout)) {
                        View placeholder = new View(this);
                        placeholder.setMinimumHeight((int)(190 * d));
                        LinearLayout.LayoutParams phLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(190 * d));
                        phLp.bottomMargin = (int)(8 * d);
                        int idx = currentParent.indexOfChild(targetView);
                        currentParent.removeView(targetView);
                        currentParent.addView(placeholder, idx);
                    } else {
                        currentParent.removeView(targetView);
                    }
                    rootPlayerMode.addView(targetView);
                } else if (currentParent == null) {
                    // 새로 만든 선(Line) 뷰처럼 부모가 아예 없으면 화면에 강제 등판!
                    rootPlayerMode.addView(targetView);
                }

                // 🚀 [지능형 크기 엔진] 폭/높이를 명시하지 않았을 때(-1),
                // 🚀 [지능형 크기 엔진] 폭/높이를 명시하지 않았을 때(-1),
                // 아이콘과 알약은 스스로 크기를 맞추고(WRAP), 진행바나 제목은 가로를 꽉 채우도록(MATCH) 똑똑하게 분기 처리!
                int w = el.width > 0 ? (int)(el.width * d) : ("progress_bar".equals(el.id) || "title".equals(el.id) || "artist".equals(el.id) || "album".equals(el.id) || "album_title".equals(el.id) ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT);
                int h = el.height > 0 ? (int)(el.height * d) : ViewGroup.LayoutParams.WRAP_CONTENT;

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);

                // =========================================================
                // 🚀 [신규 엔진] 플레이어 화면 그래비티(Anchor) 완벽 지원!
                // =========================================================
                int gravity = Gravity.TOP | Gravity.LEFT; // 기본값
                if (el.gravity != null && !el.gravity.trim().isEmpty()) {
                    gravity = 0;
                    String g = el.gravity.toLowerCase();
                    if (g.equals("center")) {
                        gravity = Gravity.CENTER;
                    } else {
                        // 세로(수직) 기준점 잡기
                        if (g.contains("bottom")) gravity |= Gravity.BOTTOM;
                        else if (g.contains("center_vertical")) gravity |= Gravity.CENTER_VERTICAL;
                        else gravity |= Gravity.TOP;

                        // 가로(수평) 기준점 잡기
                        if (g.contains("right")) gravity |= Gravity.RIGHT;
                        else if (g.contains("center_horizontal")) gravity |= Gravity.CENTER_HORIZONTAL;
                        else gravity |= Gravity.LEFT;
                    }
                }
                lp.gravity = gravity;

                // 🎯 [핵심] 그래비티 기준점에 맞춰 X, Y를 마진(여백)으로 영리하게 변환합니다!
                int marginX = (int)(el.x * d);
                int marginY = (int)(el.y * d);

                // 기준이 '오른쪽'이면 오른쪽 여백으로, 그 외엔 전부 왼쪽 여백으로 밀기
                if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) lp.rightMargin = marginX;
                else lp.leftMargin = marginX;

                // 기준이 '아래쪽'이면 아래쪽 여백으로, 그 외엔 전부 위쪽 여백으로 밀기
                if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) lp.bottomMargin = marginY;
                else lp.topMargin = marginY;
                // =========================================================
                targetView.setLayoutParams(lp);


                // =========================================================
                // 텍스트 관련 조작
                if (targetView instanceof TextView) {
                    TextView tv = (TextView) targetView;
                    if (el.textSize > 0) tv.setTextSize(el.textSize);

                    if ("left".equalsIgnoreCase(el.textAlign)) tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                    else if ("right".equalsIgnoreCase(el.textAlign)) tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    else tv.setGravity(Gravity.CENTER);

                    if (el.bgColor != null && !el.bgColor.isEmpty()) {
                        try { tv.setTextColor(android.graphics.Color.parseColor(el.bgColor)); } catch (Exception e) {}
                    }
                }

                // 🚀 [아이콘 색상 필터 적용] 셔플/반복 이미지뷰에 메인 테마 텍스트 색상(또는 커스텀 지정 색상) 적용
                if (targetView instanceof ImageView && (targetView == ivShuffle || targetView == ivRepeat)) {
                    ImageView iv = (ImageView) targetView;
                    int iconColor = ThemeManager.getTextColorPrimary();
                    if (el.bgColor != null && !el.bgColor.isEmpty()) {
                        try { iconColor = android.graphics.Color.parseColor(el.bgColor.trim()); } catch (Exception e) {}
                    }
                    iv.setColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_IN);
                }

                // 🚀 [특수 기믹] 음질 정보(알약) 컨테이너의 가로/세로 모드 변신!
                if (targetView == containerQuality) {
                    LinearLayout qLayout = (LinearLayout) targetView;
                    // json 에서 text_align: "horizontal" 로 설정하면 가로 배열!
                    if ("horizontal".equalsIgnoreCase(el.textAlign)) {
                        qLayout.setOrientation(LinearLayout.HORIZONTAL);
                        for (int i = 0; i < qLayout.getChildCount(); i++) {
                            View child = qLayout.getChildAt(i);
                            LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) child.getLayoutParams();
                            clp.bottomMargin = 0;
                            if (i < qLayout.getChildCount() - 1) clp.rightMargin = (int)(6 * d); // 알약 간 가로 간격
                            child.setLayoutParams(clp);
                        }
                    } else {
                        // 세로 배열 (기본값)
                        qLayout.setOrientation(LinearLayout.VERTICAL);
                        for (int i = 0; i < qLayout.getChildCount(); i++) {
                            View child = qLayout.getChildAt(i);
                            LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) child.getLayoutParams();
                            clp.rightMargin = 0;
                            if (i < qLayout.getChildCount() - 1) clp.bottomMargin = (int)(6 * d); // 알약 간 세로 간격
                            child.setLayoutParams(clp);
                        }
                    }
                }
                } // 🚀 !(isVisualizerShowing && "progress_bar".equals(el.id)) 닫기

                // =========================================================
                // 🚀 진행바(Progress Bar) 크기, 모서리, 커스텀 색상 100% 적용 엔진
                // =========================================================
                // =========================================================
                // 🚀 진행바(Progress Bar) 크기, 모서리, 커스텀 색상 100% 적용 엔진
                // =========================================================
                if ("progress_bar".equals(el.id)) {

                    // =========================================================
                    try {
                        float radius = (el.radius > 0 ? el.radius : 0) * d;

                        // 🚀 1. 기본값: 메인 테마의 포인트 색상을 먼저 장전합니다.
                        int barColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;

                        // 🚀 2. JSON에서 사용자가 'bg_color'를 직접 입력했다면 그걸 최우선으로 덮어치기!
                        if (el.bgColor != null && !el.bgColor.trim().isEmpty()) {
                            try { barColor = android.graphics.Color.parseColor(el.bgColor.trim()); } catch (Exception e) {}
                        }

                        // --- 진행바 배경 (반투명 덮개) ---
                        int trackBgColor = getPlayerProgressBarBgColor();
                        if (trackBgColor == 0) trackBgColor = 0x44FFFFFF;
                        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                        bg.setColor(trackBgColor);
                        bg.setCornerRadius(radius);

                        // --- 진행바 채워지는 부분 (barColor 적용!) ---
                        android.graphics.drawable.GradientDrawable progress = new android.graphics.drawable.GradientDrawable();
                        progress.setColor(barColor);
                        progress.setCornerRadius(radius);

                        android.graphics.drawable.ClipDrawable clipProgress = new android.graphics.drawable.ClipDrawable(progress, Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL);

                        android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bg, clipProgress});
                        layer.setId(0, android.R.id.background);
                        layer.setId(1, android.R.id.progress);

                        if (playerProgress != null) {
                            playerProgress.setProgressDrawable(layer);
                            if (playerProgress.getProgressDrawable() != null) {
                                playerProgress.getProgressDrawable().clearColorFilter();
                                if (playerProgress.getProgressDrawable() instanceof android.graphics.drawable.LayerDrawable) {
                                    android.graphics.drawable.Drawable p = ((android.graphics.drawable.LayerDrawable) playerProgress.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress);
                                    if (p != null) p.clearColorFilter();
                                }
                            }

                            if (playerProgress instanceof android.widget.SeekBar) {
                                android.widget.SeekBar seekBar = (android.widget.SeekBar) playerProgress;
                                if (seekBar.getThumb() != null) {
                                    seekBar.getThumb().setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN);
                                }
                            }
                        }
                    } catch (Exception e) {}

                    // =========================================================
                }
            }
        }

// =========================================================
        // 🚀 [추가] 스펙트럼 전용 하단 프로그레스 바 테마 도색
        // =========================================================
        if (visProgressBar != null) {
            int themeFocusColor = getPlayerProgressBarColor();
            int visBgColor = getPlayerProgressBarBgColor();
            if (visBgColor == 0) visBgColor = 0x44FFFFFF;
            float radiusVis = 10 * d;

            android.graphics.drawable.GradientDrawable bgVis = new android.graphics.drawable.GradientDrawable();
            bgVis.setColor(visBgColor);
            bgVis.setCornerRadius(radiusVis);

            android.graphics.drawable.GradientDrawable progressVis = new android.graphics.drawable.GradientDrawable();
            progressVis.setColor(themeFocusColor);
            progressVis.setCornerRadius(radiusVis);

            android.graphics.drawable.ClipDrawable clipProgressVis = new android.graphics.drawable.ClipDrawable(progressVis, Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL);

            android.graphics.drawable.LayerDrawable layerVis = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bgVis, clipProgressVis});
            layerVis.setId(0, android.R.id.background);
            layerVis.setId(1, android.R.id.progress);

            visProgressBar.setProgressDrawable(layerVis);
            if (visProgressBar.getProgressDrawable() != null) {
                visProgressBar.getProgressDrawable().clearColorFilter();
                if (visProgressBar.getProgressDrawable() instanceof android.graphics.drawable.LayerDrawable) {
                    android.graphics.drawable.Drawable p = ((android.graphics.drawable.LayerDrawable) visProgressBar.getProgressDrawable()).findDrawableByLayerId(android.R.id.progress);
                    if (p != null) p.clearColorFilter();
                }
            }

            if(tvVisTimeCurrent != null) tvVisTimeCurrent.setTextColor(ThemeManager.getTextColorPrimary());
            if(tvVisTimeTotal != null) tvVisTimeTotal.setTextColor(ThemeManager.getTextColorPrimary());
            if(tvPlayerTimeCurrent != null) tvPlayerTimeCurrent.setTextColor(ThemeManager.getTextColorPrimary());
            if(tvPlayerTimeTotal != null) tvPlayerTimeTotal.setTextColor(ThemeManager.getTextColorPrimary());
            if(tvLyrics != null) tvLyrics.setTextColor(isPlayerBackgroundLight() ? 0xFF111111 : 0xFFFFFFFF);
        }
// =========================================================
        updateVisualizerAndLyricsLayout();

        // 🚀 [Z-index 최상단 방어막] 사용자 커스텀 테마 적용 시 새로 추가되는 뷰들(앨범아트, 제목 등)보다
        // 셔플/반복 설정 팝업 오버레이가 뒤로 가려지지 않도록 최상단(Front)으로 끌어올립니다!
        if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getParent() != null) {
            layoutShuffleRepeatOverlay.bringToFront();
        }
        updatePlayerStatusIndicators(); // 🚀 [아이콘 색상 동기화] 테마 변경 시 셔플/반복 아이콘 색상 및 상태 즉시 반영!
        // =========================================================
    } // <-- applyThemeToPlayerUI() 끝나는 괄호!

    // 🚀 [요구사항] 사용자 커스텀 테마 디자인과 상관없이 노래 제목, 아티스트, 프로그레시브 바를 순정 음악 플레이어 화면처럼 원상 복구 및 유지!
    private void restoreDefaultPlayerLayoutStructure() {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout playerContentLayout = findViewById(R.id.player_content_layout);
        if (playerContentLayout == null) return;

        View containerAlbum = findViewById(R.id.container_album_art);
        View tvTitle = findViewById(R.id.tv_player_title);
        View tvArtist = findViewById(R.id.tv_player_artist);
        View tvAlbum = findViewById(R.id.tv_player_album);
        View containerProgress = findViewById(R.id.container_progress);

        for (int i = playerContentLayout.getChildCount() - 1; i >= 0; i--) {
            View child = playerContentLayout.getChildAt(i);
            if (child != containerAlbum && child != tvTitle && child != tvArtist && child != tvAlbum && child != containerProgress) {
                playerContentLayout.removeViewAt(i);
            }
        }

        FrameLayout rootPlayerMode = findViewById(R.id.layout_player_mode);
        if (rootPlayerMode != null) {
            for (int i = rootPlayerMode.getChildCount() - 1; i >= 0; i--) {
                View child = rootPlayerMode.getChildAt(i);
                if (child != null && child.getTag() != null && (child.getTag().toString().equals("player_line") || child.getTag().toString().startsWith("player_box_"))) {
                    rootPlayerMode.removeViewAt(i);
                }
            }
        }

        // 1. containerAlbum 복구
        if (containerAlbum != null) {
            if (containerAlbum.getParent() != playerContentLayout) {
                if (containerAlbum.getParent() instanceof ViewGroup) ((ViewGroup) containerAlbum.getParent()).removeView(containerAlbum);
                playerContentLayout.addView(containerAlbum, 0);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)(190*d), (int)(190*d));
            lp.bottomMargin = (int)(8*d);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            containerAlbum.setLayoutParams(lp);
            containerAlbum.setVisibility(View.VISIBLE);
        }

        // 2. tvTitle 복구
        if (tvTitle != null) {
            if (tvTitle.getParent() != playerContentLayout) {
                if (tvTitle.getParent() instanceof ViewGroup) ((ViewGroup) tvTitle.getParent()).removeView(tvTitle);
                int idx = playerContentLayout.indexOfChild(containerAlbum);
                playerContentLayout.addView(tvTitle, idx != -1 ? idx + 1 : 1);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            tvTitle.setLayoutParams(lp);
            if (tvTitle instanceof TextView) {
                ((TextView) tvTitle).setTextSize(22);
                ((TextView) tvTitle).setGravity(Gravity.CENTER);
            }
        }

        // 3. tvArtist 복구
        if (tvArtist != null) {
            if (tvArtist.getParent() != playerContentLayout) {
                if (tvArtist.getParent() instanceof ViewGroup) ((ViewGroup) tvArtist.getParent()).removeView(tvArtist);
                int idx = playerContentLayout.indexOfChild(tvTitle);
                playerContentLayout.addView(tvArtist, idx != -1 ? idx + 1 : 2);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int)(2*d);
            lp.bottomMargin = (int)(6*d);
            lp.gravity = Gravity.CENTER;
            tvArtist.setLayoutParams(lp);
            if (tvArtist instanceof TextView) {
                ((TextView) tvArtist).setTextSize(16);
                ((TextView) tvArtist).setGravity(Gravity.CENTER);
            }
        }

        // 3-2. tvAlbum 복구 (기본 레이아웃에서는 숨김 처리)
        if (tvAlbum != null) {
            if (tvAlbum.getParent() != playerContentLayout) {
                if (tvAlbum.getParent() instanceof ViewGroup) ((ViewGroup) tvAlbum.getParent()).removeView(tvAlbum);
                int idx = playerContentLayout.indexOfChild(tvArtist);
                playerContentLayout.addView(tvAlbum, idx != -1 ? idx + 1 : 3);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int)(6*d);
            lp.gravity = Gravity.CENTER;
            tvAlbum.setLayoutParams(lp);
            if (tvAlbum instanceof TextView) {
                ((TextView) tvAlbum).setTextSize(14);
                ((TextView) tvAlbum).setGravity(Gravity.CENTER);
            }
            tvAlbum.setVisibility(View.GONE);
        }

        // 4. containerProgress 복구
        if (containerProgress != null) {
            if (containerProgress.getParent() != playerContentLayout) {
                if (containerProgress.getParent() instanceof ViewGroup) ((ViewGroup) containerProgress.getParent()).removeView(containerProgress);
                int idx = playerContentLayout.indexOfChild(tvAlbum != null ? tvAlbum : tvArtist);
                playerContentLayout.addView(containerProgress, idx != -1 ? idx + 1 : (tvAlbum != null ? 4 : 3));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_VERTICAL;
            containerProgress.setLayoutParams(lp);
        }
    }

    // 🚀 [신규 엔진] 스펙트럼과 가사창이 프로그레시브 바 바로 위에 자연스럽게 위치하도록 자동 정렬!
    private void updateVisualizerAndLyricsLayout() {
        if (visualizerView == null || lyricScrollView == null) return;

        float d = getResources().getDisplayMetrics().density;
        FrameLayout rootPlayerMode = findViewById(R.id.layout_player_mode);
        if (rootPlayerMode == null) return;

        View progressContainer = findViewById(R.id.container_progress);
        int progressY = -1;

        if (progressContainer != null && progressContainer.getParent() != null && progressContainer.getVisibility() == View.VISIBLE) {
            int top = progressContainer.getTop();
            View parent = (View) progressContainer.getParent();
            while (parent != null && parent != rootPlayerMode) {
                top += parent.getTop();
                if (parent.getParent() instanceof View) {
                    parent = (View) parent.getParent();
                } else {
                    break;
                }
            }
            if (top > 0) {
                progressY = top;
            }
        }

        FrameLayout.LayoutParams lyricLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        lyricLp.topMargin = (int) (40 * d); // 상단 상태바 피하기
        lyricLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        if (progressY != -1) {
            // 프로그레스 바 바로 위(15dp 여백)까지만 스펙트럼/가사창의 높이를 제한하여 바로 위에 자연스럽게 위치!
            lyricLp.height = progressY - lyricLp.topMargin - (int) (15 * d);
        } else {
            lyricLp.bottomMargin = (int) (70 * d);
        }

        if (visualizerView != null) {
            if (visualizerView.getParent() == null && rootPlayerMode != null) {
                rootPlayerMode.addView(visualizerView, 0); // 뒷배경
            }
            visualizerView.setLayoutParams(lyricLp);
        }
        if (lyricScrollView != null) {
            if (lyricScrollView.getParent() == null && rootPlayerMode != null) {
                rootPlayerMode.addView(lyricScrollView);
            }
            lyricScrollView.setLayoutParams(lyricLp);
            if (tvLyrics != null) {
                tvLyrics.setTextColor(isPlayerBackgroundLight() ? 0xFF111111 : 0xFFFFFFFF);
            }
        }
        if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getParent() != null) {
            layoutShuffleRepeatOverlay.bringToFront();
        }
    }
    // 💡 [추가] 테마 리스트를 쫙 보여주고 사용자가 고를 수 있게 하는 전용 화면
    public void buildThemeSelectorUI() {
        currentSettingsDepth = 1; // 🚀 카테고리(0) → 서브 메뉴(1) → 이 화면(2)
        File themeFolder = StoragePaths.getThemesDir();
        ThemeManager.loadThemesFromStorage(themeFolder);

        containerSettingsItems.removeAllViews();
        com.themoon.y1.managers.SettingsMenuManager.getInstance(this).updateSettingsTitle(t("Theme"));
        // SD카드 폴더에서 읽어온 테마들을 하나씩 버튼으로 만듭니다.
        for (int i = 0; i < ThemeManager.availableThemes.size(); i++) {
            final int index = i;
            ThemeManager.ThemeData theme = ThemeManager.availableThemes.get(i);

            String prefix = (ThemeManager.getCurrentThemeIndex() == i) ? "✔ " : "   ";
            Button btn = createListButton(prefix + theme.name);

            if (ThemeManager.getCurrentThemeIndex() == i) {
                btn.setTypeface(null, Typeface.BOLD);
                btn.setTextColor(0xFF00FF00); // 현재 사용 중인 테마는 초록색으로 굵게 강조!
            }

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    ThemeManager.setThemeIndex(index);
                    try {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("app_theme_index", index);
                        editor.putBoolean("reboot_to_theme", true);

                        // 🚀 [지능형 룰 1] 새로 선택한 테마에 고유 배경화면이 있는지 3중으로 검사합니다.
                        ThemeManager.ThemeData selectedTheme = ThemeManager.availableThemes.get(index);
                        boolean hasThemeBg = false;

                        // 1. config.json 에 bg_image 가 선언되어 있고, 실제 파일이 존재하는지?
                        if (selectedTheme.bgImage != null && !selectedTheme.bgImage.isEmpty()) {
                            if (new File(selectedTheme.folderPath, selectedTheme.bgImage).exists())
                                hasThemeBg = true;
                        }
                        // 2. 파일 이름을 안 적었더라도 background.png 나 bg.png 가 존재하는지?
                        if (!hasThemeBg && new File(selectedTheme.folderPath, "background.png").exists())
                            hasThemeBg = true;
                        if (!hasThemeBg && new File(selectedTheme.folderPath, "bg.png").exists())
                            hasThemeBg = true;

                        // 🚀 [지능형 룰 2] 테마에 배경이 있다면, 사용자의 기존 배경이나 이전 설정을 무시하고 테마 배경으로 무조건 덮어씌웁니다!
                        // (테마에 배경이 없다면 기존 설정을 건드리지 않으므로, 사용자의 커스텀 배경이 그대로 유지됩니다)
                        if (hasThemeBg) {
                            editor.putString("bg_path", "THEME_DEFAULT");
                        }
                        // 🚀 [스마트 자동화] 선택한 테마의 부품(JSON)들을 스캔해서 위젯 스위치를 알아서 조작합니다!
                        boolean hasClock = false, hasAnalog = false, hasBattery = false, hasCircular = false,
                                hasAlbum = false, hasFocusImage = false; // 🚀 변수 추가

                        for (ThemeManager.MenuElement el : theme.menuElements) {
                            if ("widget_clock".equals(el.type))
                                hasClock = true;
                            if ("widget_analog_clock".equals(el.type))
                                hasAnalog = true;
                            if ("widget_battery".equals(el.type))
                                hasBattery = true;
                            if ("widget_circular_battery".equals(el.type))
                                hasCircular = true;
                            if ("widget_album".equals(el.type))
                                hasAlbum = true;
                            if ("widget_focus_image".equals(el.type))
                                hasFocusImage = true; // 🚀 검사 추가
                        }

                        // 테마에 포함된 위젯은 'ON', 없는 위젯은 'OFF'로 강제 동기화!
                        editor.putBoolean("widget_clock", hasClock);
                        editor.putBoolean("widget_analog_clock", hasAnalog);
                        editor.putBoolean("widget_battery", hasBattery);
                        editor.putBoolean("widget_circular_battery", hasCircular);
                        editor.putBoolean("widget_album", hasAlbum);
                        editor.putBoolean("widget_focus_image", hasFocusImage); // 🚀 저장 추가

                        editor.commit(); // 설정 저장 완료
                    } catch (Exception e) {
                    }

                    recreate(); // 화면 새로고침! (새로운 위젯 설정이 즉시 반영됩니다)
                }
            });
            containerSettingsItems.addView(btn);
        }

        containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 1번(두 번째)으로 도망가던 버그를 고치고, 한발 더 나아가 '내가 방금 선택한 테마'를 찾아 포커스를 꽂아줍니다!
                int selectedIdx = ThemeManager.getCurrentThemeIndex();

                if (containerSettingsItems.getChildCount() > selectedIdx && selectedIdx >= 0) {
                    containerSettingsItems.getChildAt(selectedIdx).requestFocus();
                }
                // 혹시라도 에러가 나면 무조건 0번(첫 번째)으로 안전하게 꽂아줍니다.
                else if (containerSettingsItems.getChildCount() > 0) {
                    containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        }, 50);
    }

    private void triggerAutoReconnect() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null && wm.isWifiEnabled()) {
                wm.reconnect();
            }
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba != null && ba.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
            }
        } catch (Exception e) {
        }
    }

    private void toggleWebServer() {
        if (isServerRunning) {
            if (webServer != null)
                webServer.stopServer();
            isServerRunning = false;

            // 🚀 [웹 서버 절전 해제] 서버가 꺼지면 자물쇠를 풀고 시스템을 놔줍니다.
            try {
                if (serverWakeLock != null && serverWakeLock.isHeld()) {
                    serverWakeLock.release();
                    serverWakeLock = null;
                }
                if (serverWifiLock != null && serverWifiLock.isHeld()) {
                    serverWifiLock.release();
                    serverWifiLock = null;
                }
                // 스캔 작업 중이 아닐 때만 화면 꺼짐 방지(FLAG) 해제
                if (!isCustomScanning && !isRadioScanning) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            } catch (Exception e) {
            }

            // 🎯 [신규 엔진 장착] PC에서 파일 업로드가 끝난 후 서버를 끄면, 즉시 백그라운드에서 자동 스캔을 돌려 새로 들어온 곡을 '최근
            // 추가된 곡' 1등으로 올립니다!
            startMediaLibraryScan();

        } else {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) {
                Toast.makeText(this, t("Please turn ON Wi-Fi first"), Toast.LENGTH_SHORT).show();
                return;
            }
            webServer = new Y1WebServer(getApplicationContext(), rootFolder);
            webServer.start();
            isServerRunning = true;

            // 🚀 [웹 서버 무중단 백그라운드 엔진 가동!]
            try {
                // 1. 시스템 타임아웃에 의해 화면이 물리적으로 꺼지는 것을 차단!
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // 2. CPU(WakeLock)와 Wi-Fi(WifiLock)가 딥슬립(절전 모드)에 빠지지 않도록 시스템 멱살 잡기!
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                serverWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Y1:WebServerLock");
                serverWakeLock.acquire();

                serverWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "Y1:WebServerWifiLock");
                serverWifiLock.acquire();
            } catch (Exception e) {
                // (앱 매니페스트에 권한이 없더라도 가상 암전 모드가 있기 때문에 2차 우회 방어가 완벽히 작동합니다)
            }
        }
    }

    private void updateWebServerUI() {
        if (tvServerStatus == null || tvServerIp == null || btnServerToggle == null) return;
        int primary = ThemeManager.getTextColorPrimary();
        int secondary = ThemeManager.getTextColorSecondary();

        if (isServerRunning) {
            // 💡 테마 스타일: 깔끔한 테마 주 색상(primary)으로 적용하여 밝은 배경에서도 완벽하게 가독성 확보!
            tvServerStatus.setText(t("SERVER RUNNING"));
            tvServerStatus.setTextColor(primary);
            tvServerIp.setText("http://" + webServer.getLocalIpAddress() + ":8080");
            tvServerIp.setTextColor(primary);
            btnServerToggle.setText(t("STOP SERVER"));

            // 🚀 [상태바 동기화] 서버가 켜지면 상단에 서버 아이콘 표시!
            if (ivStatusServer != null)
                ivStatusServer.setVisibility(View.VISIBLE);
        } else {
            // 💡 테마 스타일: 은은한 테마 보조 색상(secondary)으로 적용!
            tvServerStatus.setText(t("SERVER STOPPED"));
            tvServerStatus.setTextColor(secondary);
            tvServerIp.setText("http://---.---.---.---:8080");
            tvServerIp.setTextColor(secondary);
            btnServerToggle.setText(t("START SERVER"));

            // 🚀 [상태바 동기화] 서버가 꺼지면 상단 서버 아이콘 즉시 숨김!
            if (ivStatusServer != null)
                ivStatusServer.setVisibility(View.GONE);
        }
    }

    // 💡 [수정] 메인 화면 배경 자동 업데이트 (지능형 테마 배경 최우선 렌더링)
    public void updateMainMenuBackground() {
        try {
            String savedBgPath = prefs.getString("bg_path", null);
            ThemeManager.ThemeData currentTheme = ThemeManager.getCurrentTheme();

            // 🚀 1. 테마 기본 배경 고정 모드 ("THEME_DEFAULT")
            if ("THEME_DEFAULT".equals(savedBgPath)) {
                Bitmap themeBg = null;

                // 1순위: config.json 에서 "bg_image"로 지정한 파일
                if (currentTheme.bgImage != null && !currentTheme.bgImage.isEmpty()) {
                    File bgFile = new File(currentTheme.folderPath, currentTheme.bgImage);
                    if (bgFile.exists()) {
                        try {
                            themeBg = BitmapFactory.decodeFile(bgFile.getAbsolutePath());
                        } catch (Exception e) {
                        }
                    }
                }
                // 2순위: 이름이 background.png 또는 bg.png 인 녀석들
                if (themeBg == null)
                    themeBg = ThemeManager.getCustomIcon("background.png", this, 0);
                if (themeBg == null)
                    themeBg = ThemeManager.getCustomIcon("bg.png", this, 0);

                // 테마 이미지가 존재하면 블러 없이 쨍하게 화면에 띄우고 즉시 종료!
                if (themeBg != null) {
                    ivMainBg.setImageBitmap(themeBg);
                    return;
                } else {
                    // "THEME_DEFAULT" 명령을 받았지만 막상 테마에 이미지가 없으면?
                    // 변수를 조용히 비워주어 아래쪽의 '앨범 블러 모드'로 자연스럽게 통과시킵니다!
                    savedBgPath = null;
                }
            }

            // 🚀 2. 사용자가 갤러리에서 직접 지정한 커스텀 배경 이미지 (테마 배경이 없는 곳에서 돋보임)
            if (savedBgPath != null && !savedBgPath.isEmpty() && !savedBgPath.equals("THEME_DEFAULT")) {
                File bgFile = new File(savedBgPath);
                if (bgFile.exists()) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(savedBgPath, opts);

                    int scale = 1;
                    int maxDim = Math.max(opts.outWidth, opts.outHeight);
                    while (maxDim / scale > 800) {
                        scale *= 2;
                    }

                    opts.inJustDecodeBounds = false;
                    opts.inSampleSize = scale;
                    Bitmap customBg = BitmapFactory.decodeFile(savedBgPath, opts);

                    // 블러 없이 원본 띄우고 즉시 종료!
                    ivMainBg.setImageBitmap(customBg);
                    return;
                }
            }

            // 🚀 3. 커스텀 설정도 없고, 테마 배경도 없을 때 (앨범 아트 블러 모드 기본 작동)
            Bitmap sourceBitmap = null;
            if (lastAlbumArtBytes != null && lastAlbumArtBytes.length > 0) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;
                sourceBitmap = BitmapFactory.decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length, opts);
            } else {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 2;
                sourceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_back, opts);
            }

            if (sourceBitmap != null) {
                Bitmap blurredBitmap = applyGaussianBlur(sourceBitmap);
                ivMainBg.setImageBitmap(blurredBitmap);
                if (sourceBitmap != blurredBitmap) {
                    sourceBitmap.recycle();
                }
            } else {
                ivMainBg.setImageResource(R.drawable.default_back);
            }
        } catch (Throwable t) {
            ivMainBg.setImageResource(R.drawable.default_back);
        }
    }

    // 💡 [추가] 테마 색상과 '둥글기(Radius)'를 혼합해서 버튼의 배경 디자인을 찍어내는 도구
    public GradientDrawable createButtonBackground(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); // 테마 색상 주입
        // 테마에 설정된 둥글기(Radius) 값 주입 (dp 단위를 픽셀로 변환하여 적용)
        float radius = ThemeManager.getButtonRadius() * getResources().getDisplayMetrics().density;
        shape.setCornerRadius(radius);
        return shape;
    }

    // 💡 [수정] 메인 화면의 버튼들에 휠이 닿았을 때의 색상을 테마 엔진과 연결합니다!
    private void setupMenuButton(final Button btn, final int imageResId, final String iconFileName) {
        btn.setSoundEffectsEnabled(false);
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    btn.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg())); // 🚀 둥글기 적용
                    btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    // 🚀 [수정] 재생 상태를 확인하여 초기 아이콘(원형)과 빈 앨범 아이콘(사각형)을 완벽하게 구분합니다!
                    boolean anyWidgetActive = isWidgetClockOn || isWidgetBatteryOn || isWidgetAlbumOn;
                    if (!anyWidgetActive) {
                        if (btn.getId() == R.id.btn_now_playing) {

                            // 1. 노래가 아예 재생된 적이 없는 '초기 상태'일 때 -> 둥근 음표 아이콘(music_circle) 유지
                            if (currentPlaylist.isEmpty()) {
                                ivMenuPreview.setImageBitmap(
                                        ThemeManager.getCustomIcon(iconFileName, MainActivity.this, imageResId));
                                ivMenuPreview.setImageResource(imageResId);
                                if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                                    tvMenuPreviewTitle.setVisibility(View.GONE);
                                    tvMenuPreviewArtist.setVisibility(View.GONE);
                                }
                            }
                            // 2. 노래가 재생 중일 때
                            else {
                                if (lastAlbumArtBytes != null && lastAlbumArtBytes.length > 0) {
                                    try {
                                        BitmapFactory.Options opts = new BitmapFactory.Options();
                                        opts.inSampleSize = 2;
                                        Bitmap bmp = BitmapFactory
                                                .decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length, opts);
                                        ivMenuPreview.setImageBitmap(bmp);
                                    } catch (Exception e) {
                                        ivMenuPreview.setImageBitmap(ThemeManager.getCustomIcon(
                                                "icon_default_album.png", MainActivity.this, R.drawable.default_album));
                                        ivMenuPreview.setImageResource(R.drawable.default_album); // 에러 시 사각형 앨범
                                    }
                                } else {
                                    ivMenuPreview.setImageBitmap(ThemeManager.getCustomIcon("icon_default_album.png",
                                            MainActivity.this, R.drawable.default_album));
                                    ivMenuPreview.setImageResource(R.drawable.default_album); // 이미지가 없으면 사각형 앨범
                                }

                                // 재생 중이라면 정보 텍스트는 무조건 띄워줍니다.
                                if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                                    tvMenuPreviewTitle.setVisibility(View.VISIBLE);
                                    tvMenuPreviewArtist.setVisibility(View.VISIBLE);
                                    tvMenuPreviewTitle.setText(tvPlayerTitle.getText());
                                    tvMenuPreviewArtist.setText(tvPlayerArtist.getText());
                                }
                            }

                        } else {
                            // 다른 메뉴(Settings, Bluetooth 등)에 닿았을 때는 원래 아이콘만 보여주고 텍스트를 숨깁니다.
                            ivMenuPreview.setImageBitmap(
                                    ThemeManager.getCustomIcon(iconFileName, MainActivity.this, imageResId));
                            ivMenuPreview.setImageResource(imageResId);
                            if (tvMenuPreviewTitle != null && tvMenuPreviewArtist != null) {
                                tvMenuPreviewTitle.setVisibility(View.GONE);
                                tvMenuPreviewArtist.setVisibility(View.GONE);
                            }
                        }
                    }

                } else {
                    btn.setBackgroundColor(0x00000000);
                    btn.setTextColor(ThemeManager.getTextColorPrimary());
                }
            }
        });
    }

    public void changeScreen(int state) {
        int prevState = currentScreenState;
        // 🚀 [경로 추적 엔진] 화면이 바뀌기 직전에, 내가 어디서 출발했는지 정확하게 백미러에 기록합니다!
        if (state == STATE_PLAYER) {
            if (currentScreenState == STATE_MENU || currentScreenState == STATE_BROWSER
                    || currentScreenState == STATE_SETTINGS) {
                backTargetForPlayer = currentScreenState;
            }
            if (currentScreenState == STATE_BROWSER) {
                com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager
                        .getInstance();
                if (!am.isPlaying()) {
                    am.playOrPauseMusic();
                }
            }
        } else if (state == STATE_BLUETOOTH || state == STATE_WIFI || state == STATE_BRIGHTNESS
                || state == STATE_STORAGE || state == STATE_WEBSERVER) {
            if (currentScreenState == STATE_MENU || currentScreenState == STATE_BROWSER
                    || currentScreenState == STATE_SETTINGS) {
                backTargetForUtility = currentScreenState;
            }
        }

        // =======================================================
        // 🚀 [포커스 증발 완벽 방어막!]
        // 화면이 VISIBLE 될 때 안드로이드 시스템이 첫 번째 항목에 강제로 포커스를 주면서
        // 우리가 기억해둔 인덱스를 0으로 덮어씌워 버리는 '오토 포커스 오염' 현상을 막기 위해,
        // 화면이 바뀌기 직전에 진짜 인덱스를 SettingsMenuManager에서 빼와 안전한 금고에 백업해 둡니다!
        com.themoon.y1.managers.SettingsMenuManager smm = com.themoon.y1.managers.SettingsMenuManager.getInstance(this);
        int safeSettingsIndex = smm.lastSettingsFocusIndex;
        int safeSubMenuIndex = smm.lastSubMenuFocusIndex;
        // =======================================================

        int safeMenuIndex = lastMainMenuFocusIndex;

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

        View statusBar = findViewById(R.id.layout_status_bar);
        if (statusBar != null) {
            if (state == STATE_PLAYER || (state == STATE_BROWSER && currentBrowserMode == BROWSER_COVER_FLOW)) {
                statusBar.setBackgroundColor(0x00000000);
            } else {
                statusBar.setBackgroundColor(ThemeManager.getStatusBarBackgroundColor());
            }
        }

        if (state == STATE_MENU) {
            lastMainMenuFocusIndex = safeMenuIndex;
            isPickingBackground = false;

            int targetId = 10000 + safeMenuIndex;
            View dynamicBtn = findViewById(targetId);

            if (dynamicBtn != null && dynamicBtn.getVisibility() == View.VISIBLE) {
                dynamicBtn.requestFocus();
            } else {
                View dynamicFirstBtn = findViewById(10000);
                if (dynamicFirstBtn != null) {
                    dynamicFirstBtn.requestFocus();
                } else if (btnNowPlaying != null) {
                    btnNowPlaying.requestFocus();
                }
            }
            refreshNowPlayingPreview();

        } else if (state == STATE_BROWSER) {
            // ... (기존 BROWSER 로직 그대로 유지) ...
            if (currentBrowserMode == BROWSER_VIDEOS) {
                buildFolderBrowserUI();
            } else if (currentBrowserMode == BROWSER_ROOT || currentBrowserMode == BROWSER_FOLDER) {
                buildFileBrowserUI();
            } else if (currentBrowserMode == BROWSER_COVER_FLOW) {
                buildCoverFlowUI();
            } else if (currentBrowserMode == BROWSER_ARTISTS) {
                buildVirtualCategories("ARTIST");
            } else if (currentBrowserMode == BROWSER_ARTIST_ALBUMS) {
                buildVirtualCategories("ARTIST_ALBUM");
            } else if (currentBrowserMode == BROWSER_PLAYLISTS) {
                buildM3uPlaylistUI();
            } else if (currentBrowserMode == BROWSER_M3U_SONGS) {
                buildM3uSongsUI(currentM3uFile);
            } else if (currentBrowserMode == BROWSER_ALBUMS) {
                buildVirtualCategories("ALBUM");
            } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                buildVirtualSongs();
            } else if (currentBrowserMode == BROWSER_PODCAST_CHANNELS) {
                buildPodcastChannelsUI();
            } else {
                if (listVirtualSongs != null && listVirtualSongs.getAdapter() != null) {
                    ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
                }
            }

        } else if (state == STATE_SETTINGS) {
            // 🚀 [오염된 포커스 원상 복구]
            // 안드로이드에 의해 0으로 오염된 인덱스를 버리고, 아까 금고에 백업해둔 진짜 위치를 다시 주입합니다!
            smm.lastSettingsFocusIndex = safeSettingsIndex;
            smm.lastSubMenuFocusIndex = safeSubMenuIndex;

            if (!isNavigatingToSubMenu) {
                // 직전 화면(prevState)이 밝기(8), 와이파이(6) 등 유틸리티 화면이었다면?
                if (prevState == STATE_BRIGHTNESS || prevState == STATE_WIFI || prevState == STATE_WIFI_KEYBOARD ||
                        prevState == STATE_BLUETOOTH || prevState == STATE_STORAGE || prevState == STATE_WEBSERVER) {

                    if (currentSettingsDepth > 0) {
                        smm.restoreSubMenu(); // 🚀 복구된 진짜 인덱스로 해당 서브 메뉴 화면 렌더링!
                    } else {
                        smm.buildSettingsUI();
                    }
                } else {
                    currentSettingsDepth = 0;
                    smm.buildSettingsUI();
                }
            }

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

    // 💡 [수정] 스토리지 상세 정보 텍스트 적용 및 UI 크기/위치 최적화
    private void loadStorageUI() {
        try {
            android.os.StatFs stat = new android.os.StatFs(StoragePaths.getPrimaryRoot().getAbsolutePath());

            // 🚀 기기 용량이 클 때 숫자가 폭발(오버플로우)해서 에러가 나는 것을 막기 위해 (long)으로 강제 변환!
            long blockSize = (long) stat.getBlockSize();
            long total = ((long) stat.getBlockCount() * blockSize) / (1024 * 1024);
            long free = ((long) stat.getAvailableBlocks() * blockSize) / (1024 * 1024);
            long used = total - free;

            if (pbStorage != null)
                pbStorage.setVisibility(View.GONE);

            LinearLayout storageLayout = findViewById(R.id.layout_storage_mode);
            float density = getResources().getDisplayMetrics().density;

            // 🚀 [수술 1] 상단 타이틀이 상태바에 먹히지 않도록 아래로 20dp 끌어내리고 크기도 살짝 키웁니다!
            try {
                TextView tvHeader = (TextView) storageLayout.getChildAt(0);
                tvHeader.setTranslationY(30 * density); // 아래로 이동!
                tvHeader.setTextSize(22f); // 타이틀 글자 크기도 시원하게 22로!
                tvHeader.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
            } catch (Exception e) {}

            PieChartView pieChart = (PieChartView) storageLayout.findViewWithTag("pie_chart");

            if (pieChart == null) {
                pieChart = new PieChartView(this);
                pieChart.setTag("pie_chart");

                int size = (int) (140 * density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);

                // 🚀 [수술 2] 타이틀이 내려온 만큼, 차트도 겹치지 않게 위아래 마진(30dp)을 넉넉하게 줍니다!
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                lp.setMargins(0, (int) (30 * density), 0, (int) (30 * density));
                pieChart.setLayoutParams(lp);

                storageLayout.addView(pieChart, 1);
            }

            int themeColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            pieChart.setStorageData(used, total, themeColor);

            // 텍스트 정보 세팅 및 화면 강제 노출
            tvStorageDetails.setText(
                    t("Total Capacity") + " :  " + total + " MB\n" +
                            t("Used Space") + " :  " + used + " MB\n" +
                            t("Free Space") + " :  " + free + " MB");
            tvStorageDetails.setGravity(Gravity.CENTER);

            // 🚀 [수술 3] 글자 크기를 18f로 큼직하게 올리고, 줄 간격도 답답하지 않게 1.2배로 벌려줍니다!
            tvStorageDetails.setTextSize(18f);
            tvStorageDetails.setLineSpacing(12f, 0.8f);
            tvStorageDetails.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);

            tvStorageDetails.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            tvStorageDetails.setText(t("Storage Error: Failed to calculate space."));
            tvStorageDetails.setTextSize(18f); // 에러 메시지도 18f로 통일
            tvStorageDetails.setVisibility(View.VISIBLE);
        }
    }
    // 🚀 [직결 성공] 포커스 시스템을 우회하여 정중앙 앨범의 노래 리스트로 다이렉트 진입합니다!
    private void handleCenterShortClick() {
        if (currentScreenState == STATE_BROWSER && currentBrowserMode == BROWSER_COVER_FLOW) {
            if (uniqueAlbumList != null && !uniqueAlbumList.isEmpty() && currentCoverFlowIndex >= 0
                    && currentCoverFlowIndex < uniqueAlbumList.size()) {
                clickFeedback();
                SongItem chosen = uniqueAlbumList.get(currentCoverFlowIndex);
                currentBrowserMode = BROWSER_VIRTUAL_SONGS;
                virtualQueryType = "COVER_FLOW_ALBUM";
                virtualQueryValue = chosen.album;
                buildVirtualSongs();
            }
            return; // 💡 링크를 태웠으므로 아래의 낡은 포커스 클릭 루틴으로 내려가지 못하게 차단!
        }

        if (currentScreenState == STATE_PLAYER) {

            // 🚀 [가사 쏠림 버그 수리 2단계]
            // 가사창이 켜지는 순간, 잔상을 지우고 현재 가사 줄에 맞춰 즉시 스크롤을 0으로 강제 세팅합니다!
            lastLyricIndex = -1;
            toggleVisualizer();
            clickFeedback();
        } else if (currentScreenState == STATE_WIFI_KEYBOARD) {
            handleKeyboardInput();
        } else if (currentScreenState != STATE_BRIGHTNESS && currentScreenState != STATE_STORAGE
                && currentScreenState != STATE_PLAYER) {
            View c = getCurrentFocus();
            if (c != null)
                c.performClick();
        }
    }

    // 💡 [추가] 과부하 방지용 타이머 변수
    private long lastClickTime = 0;

    // 🚀 [원본 스크롤 위치 기억 금고] 클릭한 버튼의 텍스트를 열쇠로 삼아, 화면 상단으로부터의 정확한 거리(Offset)를 1px 단위로
    // 기억합니다!
    public java.util.HashMap<String, Integer> exactOffsetMemory = new java.util.HashMap<>();

    public void clickFeedback() {
        long now = System.currentTimeMillis();

        if (now - lastClickTime < 30)
            return;
        lastClickTime = now;

        // 🚀 [초정밀 스크롤 캡처 엔진 가동] 진동이 울리는 그 찰나의 순간, 현재 선택된 뷰의 화면상 Y좌표를 훔쳐와 금고에 저장합니다!
        try {
            View focused = getCurrentFocus();
            if (focused != null) {
                String keyText = "";
                if (focused instanceof Button) {
                    keyText = ((Button) focused).getText().toString();
                } else if (focused instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) focused;
                    if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof TextView) {
                        keyText = ((TextView) layout.getChildAt(1)).getText().toString();
                    } else if (layout.getChildCount() > 0 && layout.getChildAt(0) instanceof TextView) {
                        keyText = ((TextView) layout.getChildAt(0)).getText().toString();
                    }
                }

                if (!keyText.isEmpty()) {
                    if (keyText.contains("  ⏱"))
                        keyText = keyText.substring(0, keyText.indexOf("  ⏱")).trim();

                    android.view.ViewParent p = focused.getParent();
                    View childOfList = focused; // 🚀 [추가] 리스트뷰 자식 추적용 변수

                    while (p != null) {
                        if (p instanceof ScrollView) {
                            // 버튼의 절대 높이에서 현재 스크롤된 높이를 빼면 화면 상단으로부터의 실제 오프셋(거리)이 나옵니다.
                            int offset = focused.getTop() - ((ScrollView) p).getScrollY();
                            exactOffsetMemory.put(keyText, offset);

                            // 💡 설정창(Settings) 복원 전용 인덱스 오프셋도 보너스로 동시 저장!
                            if (focused.getParent() == containerSettingsItems) {
                                exactOffsetMemory.put("SETTINGS_" + containerSettingsItems.indexOfChild(focused), offset);
                            }
                            // 🚀 메인 화면(동적 메뉴) 복원 전용 인덱스 오프셋도 금고에 저장합니다!
                            if (focused.getId() >= 10000 && focused.getId() < 11000) {
                                exactOffsetMemory.put("MAIN_" + (focused.getId() - 10000), offset);
                            }
                            break;
                        }
                        // =========================================================
                        // 🚀 [여기에 신규 추가!] ListView 스크롤 캡처 엔진 장착!
                        // =========================================================
                        else if (p instanceof ListView) {
                            // 리스트뷰 화면 상단으로부터의 상대적 Y좌표(Offset)를 저장!
                            int offset = childOfList.getTop();
                            exactOffsetMemory.put(keyText, offset);

                            // 💡 텍스트 이모티콘(👤, 💿 등)을 제거한 '순수 이름'으로도
                            // 완벽하게 찾을 수 있게 금고에 백업용으로 하나 더 저장해둡니다!
                            String cleanKey = keyText.replace("📁 ", "").replace("👤 ", "").replace("💿 ", "").replace("🎵 ", "").trim();
                            exactOffsetMemory.put(cleanKey, offset);
                            break;
                        }
                        // =========================================================

                        childOfList = (View) p; // 부모로 타고 올라가기 전 현재 뷰 기억
                        p = p.getParent();
                    }
                }
            }
        } catch (Exception e) {
        }

        try {
            if (isVibrationEnabled) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null)
                    v.vibrate(VIBE_DURATIONS[vibrationStrengthLevel]);
            }
        } catch (Exception e) {
        }
    }

    private void openKeyboard() {
        typedPassword = "";
        keyboardIndex = 0;
        // 🚀 모드에 따라 상단 제목 다르게 표시!
        if (currentKeyboardMode == 1) {
            tvKeyboardSsid.setText("🔍 " + t("Search Podcast"));
        } else {
            tvKeyboardSsid.setText(t("Target") + ": " + targetWifiSsid);
        }
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

        // 🚀 와이파이 모드일 때만 '개방형 네트워크' 텍스트 표시
        if (isTargetWifiOpen && currentKeyboardMode == 0) {
            tvKeyboardInput.setText(t("Open Network (Direct Connect)"));
            keyboardIndex = len - 1;
            tvKeyCurrent.setText(KEYBOARD_CHARS[keyboardIndex]);
        } else {
            // 🚀 모드에 따라 힌트 텍스트 다르게 표시!
            if (currentKeyboardMode == 1) {
                tvKeyboardInput.setText(typedPassword.length() == 0 ? t("Enter keyword...") : typedPassword);
            } else {
                tvKeyboardInput.setText(typedPassword.length() == 0 ? t("Enter Password...") : typedPassword);
            }
        }
    }

    private void handleKeyboardInput() {
        String selectedChar = KEYBOARD_CHARS[keyboardIndex];
        clickFeedback();

        if (selectedChar.equals("[DEL]")) {
            if (typedPassword.length() > 0)
                typedPassword = typedPassword.substring(0, typedPassword.length() - 1);
        } else if (selectedChar.equals("[SPACE]")) {
            // 🚀 스페이스바 누르면 띄어쓰기 추가!
            typedPassword += " ";
        } else if (selectedChar.equals("[CONN]")) {
            // =======================================================
            // 🚀 [핵심 분기점] [CONN] 버튼을 눌렀을 때 발사되는 엔진 변경!
            // =======================================================
            if (currentKeyboardMode == 1) {
                if (typedPassword.trim().isEmpty()) {
                    Toast.makeText(this, t("Please enter a keyword."), Toast.LENGTH_SHORT).show();
                    return;
                }
                changeScreen(STATE_BROWSER); // 💡 검색 버튼을 누르면 키보드를 닫고 브라우저로 복귀!
                searchPodcastFromApple(typedPassword.trim()); // 🍏 애플 검색 엔진 발사!
            } else {
                connectToWifi(); // 기존 와이파이 접속 엔진 발사!
            }
        } else {
            typedPassword += selectedChar;
        }
        updateKeyboardUI();
    }

    private void connectToWifi() {
        Toast.makeText(this, t("Connecting to ") + targetWifiSsid + "...", Toast.LENGTH_SHORT).show();
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
    public void startBluetoothScan() {
        int currentFocusIndex = 0;
        if (containerBtItems != null) {
            for (int i = 0; i < containerBtItems.getChildCount(); i++) {
                if (containerBtItems.getChildAt(i).hasFocus()) {
                    currentFocusIndex = i;
                    break;
                }
            }
        }
        final int targetFocusIndex = currentFocusIndex;

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

        containerBtItems.removeAllViews();

        // 1. 전원 토글 버튼
        final LinearLayout btnToggle = createSettingRow(t("Bluetooth Power"), t(statusText));
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 🚀 [광클 폭주 방어막 1] 1.5초 이내의 중복 클릭은 완벽하게 씹어버립니다!
                long now = System.currentTimeMillis();
                if (now - lastBtToggleTime < 1500) {
                    Toast.makeText(MainActivity.this, t("Please wait a moment..."), Toast.LENGTH_SHORT).show();
                    return;
                }
                lastBtToggleTime = now;
                clickFeedback();

                if (ba != null) {
                    if (ba.isEnabled()) {
                        // 🚀 [무한루프 방어막 2] 의도적으로 끄는 것이므로 즉시 좀비 타겟을 삭제합니다!
                        targetDeviceForAudio = null;
                        ba.disable();
                    } else {
                        ba.enable();
                    }
                    ((TextView) btnToggle.getChildAt(1)).setText(t("Wait..."));
                }
            }
        });
        containerBtItems.addView(btnToggle);

        if (!isOn) {
            btnScanBt.setText(t("Bluetooth is OFF"));
            if (btnScanBt.getParent() != null)
                ((ViewGroup) btnScanBt.getParent()).removeView(btnScanBt);
            containerBtItems.addView(btnScanBt);
            restoreBluetoothFocus(targetFocusIndex);
            return;
        }

        // 🚀 2. 순정 런처 완벽 구현: 나의 기기 (PAIRED DEVICES) 목록
        TextView tvPaired = new TextView(this);
        tvPaired.setText("━ " + t("PAIRED DEVICES") + " ━");
        tvPaired.setTextColor(ThemeManager.getTextColorPrimary());
        tvPaired.setTextSize(14);
        tvPaired.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvPaired.setPadding(10, 30, 10, 5);
        containerBtItems.addView(tvPaired);

        try {
            Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
            if (pairedDevices != null && pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    addPairedBluetoothItemToUI(device); // 등록된 기기 전용 UI 호출
                }
            } else {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText(t("No paired devices."));
                tvEmpty.setTextColor(ThemeManager.getTextColorSecondary());
                tvEmpty.setPadding(10, 10, 10, 10);
                containerBtItems.addView(tvEmpty);
            }
        } catch (Exception e) {
        }

        // 🚀 3. 새로 찾은 기기 (AVAILABLE DEVICES) 목록
        TextView tvAvailable = new TextView(this);
        tvAvailable.setText("━ " + t("AVAILABLE DEVICES") + " ━");
        tvAvailable.setTextColor(ThemeManager.getTextColorPrimary());
        tvAvailable.setTextSize(14);
        tvAvailable.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvAvailable.setPadding(10, 30, 10, 5);
        containerBtItems.addView(tvAvailable);

        btnScanBt.setText(t("Scanning..."));
        foundBtDevices.clear();

        if (btnScanBt.getParent() != null)
            ((ViewGroup) btnScanBt.getParent()).removeView(btnScanBt);
        containerBtItems.addView(btnScanBt);

        if (ba.isDiscovering()) {
            ba.cancelDiscovery();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    ba.startDiscovery();
                }
            }, 500);
        } else {
            ba.startDiscovery();
        }

        restoreBluetoothFocus(targetFocusIndex);
    }

    // 🚀 [신규] 등록된 기기(Paired) 전용 리스트 및 삭제(Unpair) 메뉴
    private void addPairedBluetoothItemToUI(final BluetoothDevice device) {
        String name = (device.getName() != null && !device.getName().isEmpty()) ? device.getName()
                : "Unknown (" + device.getAddress() + ")";

        boolean isConnected = false;
        if (globalA2dp != null) {
            try {
                int state = (int) globalA2dp.getClass().getMethod("getConnectionState", BluetoothDevice.class)
                        .invoke(globalA2dp, device);
                isConnected = (state == BluetoothProfile.STATE_CONNECTED);
            } catch (Exception e) {
            }
        }

        String prefix = isConnected ? "((♪)) [CONNECTED] " : "✔ ";
        final Button btnDevice = createListButton(prefix + name);

        if (isConnected) {
            int themeColor = 0xFF00FFFF;
            try {
                themeColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            } catch (Exception e) {
            }
            btnDevice.setTextColor(themeColor);
            btnDevice.setTypeface(null, Typeface.BOLD);
        } else {
            btnDevice.setTextColor(ThemeManager.getTextColorPrimary());
        }

        // 🚀 [치명적 버그 수정] 서브 메뉴용 투명 폴더(LinearLayout)를 박살내고 리스트에 직속으로 붙입니다!

        // 🚀 [하이브리드 엔진 연동] 오디오 연결 유니코드("\uE1B1")와 흰색(0xFFFFFFFF)을 주입합니다.
        // (※ 주의: 리턴 타입이 Button에서 android.view.View로 변경됩니다)
        final View btnConnect = createListButtonWithIcon("\uE1B1", t("Connect Audio"), ThemeManager.getTextColorPrimary());

        btnConnect.setVisibility(View.GONE); // 초기엔 숨겨둡니다.
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                connectBluetoothAudio(device); // 🚀 중앙 엔진 호출!
            }
        });

        // 🚀 [하이브리드 엔진 연동] 쓰레기통 유니코드("\uE872")와 빨간색(0xFFFF5555)을 주입합니다.
        // (※ 주의: 리턴 타입이 Button에서 android.view.View로 변경됩니다)
        final View btnUnpair = createListButtonWithIcon("\uE872", t("Delete Device"), 0xFFFF5555);

        btnUnpair.setVisibility(View.GONE); // 초기엔 숨겨둡니다.
        btnUnpair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                try {
                    device.getClass().getMethod("removeBond").invoke(device);
                    Toast.makeText(MainActivity.this, t("Device Deleted."), Toast.LENGTH_SHORT).show();
                    startBluetoothScan(); // 삭제 후 화면 새로고침
                } catch (Exception e) {
                }
            }
        });

        // 부모 버튼 클릭 시 숨겨둔 서브 메뉴 버튼들의 투명 망토를 벗깁니다.
        btnDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                if (btnConnect.getVisibility() == View.GONE) {
                    btnConnect.setVisibility(View.VISIBLE);
                    btnUnpair.setVisibility(View.VISIBLE);

                    // 💡 서브 메뉴가 열리자마자 휠 커서가 'Connect Audio' 버튼으로 자연스럽게 빨려 들어가도록 유도합니다!
                    btnConnect.post(new Runnable() {
                        public void run() {
                            btnConnect.requestFocus();
                        }
                    });
                } else {
                    btnConnect.setVisibility(View.GONE);
                    btnUnpair.setVisibility(View.GONE);
                }
            }
        });

        // 🚀 인덱스 꼬임 방지: 화면 구조를 만들 때 그냥 차곡차곡 쌓아 올립니다.
        containerBtItems.addView(btnDevice);
        containerBtItems.addView(btnConnect);
        containerBtItems.addView(btnUnpair);
    }

    // 🚀 [신규] 새로 스캔된 기기(Available) 전용
    private void addBluetoothItemToUI(String name, final BluetoothDevice device, boolean isPaired) {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED)
            return; // 페어링된 기기는 위에서 그리므로 무시

        final Button btnDevice = createListButton("🔍 " + name);

        btnDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                connectBluetoothAudio(device); // 🚀 페어링 안 된 상태여도 중앙 엔진이 알아서 페어링부터 꽂아줍니다!
            }
        });

        containerBtItems.addView(btnDevice, containerBtItems.getChildCount() - 1);
    }

    // 🚀 [포커스 복구 전용 도구]
    private void restoreBluetoothFocus(final int targetFocusIndex) {
        containerBtItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (containerBtItems.getChildCount() > 0) {
                    if (targetFocusIndex >= 0 && targetFocusIndex < containerBtItems.getChildCount()) {
                        View target = containerBtItems.getChildAt(targetFocusIndex);
                        if (target.isFocusable()) {
                            target.requestFocus();
                            return;
                        }
                    }
                    containerBtItems.getChildAt(0).requestFocus();
                }
            }
        }, 50);
    }

    // 🚀 [가장 강력했던 연결 엔진 복구] 서브 메뉴, 매크로 모두 삭제!

    public void startWifiScan() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isOn = wm != null && wm.isWifiEnabled();
        updateWifiUI(null);

        if (isOn) {
            btnScanWifi.setText(t("Scanning..."));
            foundWifiNetworks.clear();
            // 💡 무조건 최상단 전원 버튼으로 포커스 강제 이동!
            if (containerWifiItems.getChildCount() > 0)
                containerWifiItems.getChildAt(0).requestFocus();
            wm.startScan();
        } else {
            btnScanWifi.setText(t("Wi-Fi is OFF"));
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
            final LinearLayout btnToggle = createSettingRow(t("Wi-Fi Power"), t(statusText));
            btnToggle.setId(999992);
            btnToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    if (wm != null) {
                        // 🚀 [광클 및 버그 방어막] Wait... 상태일 때는 클릭 무조건 무시!
                        int state = wm.getWifiState();
                        if (state == WifiManager.WIFI_STATE_ENABLING || state == WifiManager.WIFI_STATE_DISABLING) {
                            Toast.makeText(MainActivity.this, t("Please wait..."), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean isCurrentlyOn = wm.isWifiEnabled();
                        if (isCurrentlyOn) {
                            Toast.makeText(MainActivity.this, t("Turning Wi-Fi OFF..."), Toast.LENGTH_SHORT).show();
                            wm.setWifiEnabled(false);
                        } else {
                            Toast.makeText(MainActivity.this, t("Turning Wi-Fi ON..."), Toast.LENGTH_SHORT).show();
                            wm.setWifiEnabled(true);
                        }
                        TextView tvRight = (TextView) btnToggle.getChildAt(1);
                        tvRight.setText(t("Wait..."));
                        if (!btnToggle.hasFocus())
                            tvRight.setTextColor(0xFFFFFF00);
                    }
                }
            });
            containerWifiItems.addView(btnToggle, 0);

            if (btnScanWifi.getParent() != null) {
                ((ViewGroup) btnScanWifi.getParent()).removeView(btnScanWifi);
            }
            containerWifiItems.addView(btnScanWifi);
        } else {
            LinearLayout btnToggle = (LinearLayout) existingToggle;
            TextView tvRight = (TextView) btnToggle.getChildAt(1);
            tvRight.setText(t(statusText));
            if (!btnToggle.hasFocus()) {
                if (statusText.equals("ON"))
                    tvRight.setTextColor(ThemeManager.getTextColorPrimary());
                else if (statusText.equals("OFF"))
                    tvRight.setTextColor(ThemeManager.getTextColorSecondary());
                else
                    tvRight.setTextColor(ThemeManager.getTextColorPrimary());
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
            int themeColor = 0xFF00FF00;
            try {
                themeColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            } catch (Exception e) {
            }
            btnWifi.setTextColor(themeColor);
            btnWifi.setTypeface(null, Typeface.BOLD);
        }

        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

                // 🚀 [머티리얼 디자인 팝업 엔진 가동] 연결됐거나 저장된 와이파이를 눌렀을 때!
                if (isConnected || (isSaved && savedNetId != -1)) {
                    final int finalSavedNetId = savedNetId;

                    final android.app.Dialog dialog = new android.app.Dialog(MainActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                    float d = getResources().getDisplayMetrics().density;

                    // 1. 팝업창 뼈대와 배경 디자인 조립
                    final LinearLayout rootLayout = new LinearLayout(MainActivity.this);
                    rootLayout.setOrientation(LinearLayout.VERTICAL);
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0x88000000); // 메인 테마색 바탕
                    bg.setCornerRadius(15 * d);
                    bg.setStroke((int) (1 * d), 0x33FFFFFF); // 은은한 테두리
                    rootLayout.setBackground(bg);
                    rootLayout.setPadding((int) (15 * d), (int) (20 * d), (int) (15 * d), (int) (15 * d));

                    // 2. 상단 와이파이 이름(SSID) 타이틀
                    TextView tvTitle = new TextView(MainActivity.this);
                    tvTitle.setText("📶 " + ssid);
                    tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
                    tvTitle.setTextSize(18f);
                    tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
                    tvTitle.setGravity(Gravity.CENTER);
                    tvTitle.setPadding(0, 0, 0, (int) (15 * d));
                    rootLayout.addView(tvTitle);

                    // 3. 안내 메시지 (번역기 완벽 적용)
                    TextView tvMsg = new TextView(MainActivity.this);
                    tvMsg.setText(isConnected ? t("Do you want to disconnect and forget this network?") : t("This is a saved network."));
                    tvMsg.setTextColor(ThemeManager.getTextColorSecondary());
                    tvMsg.setTextSize(15f);
                    tvMsg.setGravity(Gravity.CENTER);
                    tvMsg.setPadding(0, 0, 0, (int) (20 * d));
                    rootLayout.addView(tvMsg);

                    // 🚀 [휠 조향 장치] 팝업창 안에서 휠을 돌릴 때 위아래로 움직이게 만듭니다!
                    View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                if (keyCode == 21 || keyCode == 19) { // 휠 위로 (UP)
                                    int idx = rootLayout.indexOfChild(v);
                                    for (int i = idx - 1; i >= 0; i--) {
                                        if (rootLayout.getChildAt(i).isFocusable()) {
                                            rootLayout.getChildAt(i).requestFocus();
                                            clickFeedback();
                                            return true;
                                        }
                                    }
                                    return true;
                                }
                                if (keyCode == 22 || keyCode == 20) { // 휠 아래로 (DOWN)
                                    int idx = rootLayout.indexOfChild(v);
                                    for (int i = idx + 1; i < rootLayout.getChildCount(); i++) {
                                        if (rootLayout.getChildAt(i).isFocusable()) {
                                            rootLayout.getChildAt(i).requestFocus();
                                            clickFeedback();
                                            return true;
                                        }
                                    }
                                    return true;
                                }
                            }
                            return false;
                        }
                    };

                    // 4. 버튼 조립 구역
                    // 현재 연결 안 된 저장된 와이파이라면 '연결(Connect)' 버튼 추가
                    if (!isConnected) {
                        View btnConnect = createListButtonWithIcon("\uE1B1", t("Connect"), 0xFF00FF00); // 🟢 초록색 연결 버튼
                        btnConnect.setOnKeyListener(dialogWheelListener);
                        btnConnect.setOnClickListener(v2 -> {
                            clickFeedback();
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, t("Connecting to saved network..."), Toast.LENGTH_SHORT).show();
                            manager.disconnect();
                            manager.enableNetwork(finalSavedNetId, true);
                            manager.reconnect();
                        });
                        rootLayout.addView(btnConnect);
                    }

                    // 삭제(Forget) 버튼
                    View btnForget = createListButtonWithIcon("\uE872", t("Forget"), 0xFFFF5555); // 🔴 빨간색 휴지통 아이콘
                    btnForget.setOnKeyListener(dialogWheelListener);
                    btnForget.setOnClickListener(v2 -> {
                        clickFeedback();
                        dialog.dismiss();
                        if (finalSavedNetId != -1) {
                            manager.removeNetwork(finalSavedNetId);
                            manager.saveConfiguration();
                        }
                        if (isConnected) manager.disconnect();
                        Toast.makeText(MainActivity.this, "🗑️ " + t("Network forgotten."), Toast.LENGTH_SHORT).show();
                        startWifiScan(); // 삭제 후 목록 새로고침
                    });
                    rootLayout.addView(btnForget);

                    // 취소(Cancel) 버튼
                    View btnCancel = createListButtonWithIcon("\uE5CD", t("Cancel"), ThemeManager.getTextColorPrimary()); // ⚪ 테마 기본색
                    btnCancel.setOnKeyListener(dialogWheelListener);
                    btnCancel.setOnClickListener(v2 -> {
                        clickFeedback();
                        dialog.dismiss();
                    });
                    rootLayout.addView(btnCancel);

                    dialog.setContentView(rootLayout);
                    Window window = dialog.getWindow();
                    if (window != null) window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.show();

                    // 🚀 팝업이 열리면 첫 번째 활성화된 버튼에 자석처럼 휠 포커스 강제 고정!
                    rootLayout.postDelayed(() -> {
                        for (int i = 0; i < rootLayout.getChildCount(); i++) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                break;
                            }
                        }
                    }, 50);

                    return; // 💡 팝업을 띄웠으니 여기서 즉시 함수 탈출!
                }

                // 🚀 아예 처음 보는 새로운 와이파이일 때 (비밀번호 입력 키보드 호출)
                targetWifiSsid = ssid;
                isTargetWifiOpen = isOpen;
                changeScreen(STATE_WIFI_KEYBOARD);
            }
        });
        containerWifiItems.addView(btnWifi);
    }

    private void createCategoryHeader(String title) {
        TextView tv = new TextView(this);
        tv.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tv.setText(t(title)); // 🚀 [수정] 들어온 title을 번역기에 한 번 돌려서 넣습니다!
        // 💡 하늘색을 빼고, 애플 스타일의 은은한 반투명 흰색 & 굵은 글씨로 변경!
        tv.setTextColor(ThemeManager.getTextColorPrimary());
        tv.setTextSize(14);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(10, 30, 10, 5);
        containerSettingsItems.addView(tv);
    }

    private LinearLayout createSettingRow(String leftText, String rightText) {
        final LinearLayout layout = new LinearLayout(this);
        layout.setSoundEffectsEnabled(false);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setFocusable(true);
        layout.setPadding(20, 15, 20, 15);
        layout.setOnLongClickListener(globalScreenOffLongClickListener);
        // 🚀 [수정 완료] 단색 덮어쓰기(setBackgroundColor)를 삭제하고, 둥글기가 적용된 배경만 입힙니다!
        layout.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));

        TextView tvLeft = new TextView(this);
        tvLeft.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
        tvLeft.setText(t(leftText));
        tvLeft.setTextColor(ThemeManager.getTextColorPrimary());
        tvLeft.setTextSize(18);
        tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvRight = new TextView(this);
        tvRight.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvRight.setText(rightText);
        tvRight.setTextSize(18);
        tvRight.setTypeface(null, Typeface.BOLD);
        tvRight.setGravity(Gravity.RIGHT);
        tvRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

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
                    layout.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    ((TextView) layout.getChildAt(0)).setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    ((TextView) layout.getChildAt(1)).setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    // 🚀 [버그 완벽 차단 및 라디오 호환] 깊이에 따라 알맞은 서랍에 포커스를 꼼꼼히 저장합니다!
                    // 🚀 [버그 완벽 차단 및 라디오 호환] 깊이에 따라 알맞은 서랍에 포커스를 꼼꼼히 저장합니다!
                    if (currentScreenState == STATE_SETTINGS) {
                        int idx = containerSettingsItems.indexOfChild(layout);
                        if (idx != -1) {
                            if (currentSettingsDepth == 0)
                                lastSettingsFocusIndex = idx;
                            else if (currentSettingsDepth == 1) {
                                // lastRadioFocusIndex = idx; // 💡 라디오 포커스 완벽 추적!

                                // 🚀 [주파수 전광판 시야 확보]
                                // 포커스가 상단 항목(Power=2, Tune=3)에 위치하면 안드로이드 스크롤뷰가
                                // 위의 전광판을 잘라버리는 문제를 막기 위해 최상단(0,0)으로 스크롤을 강제 고정합니다.
                                if (idx <= 2) {
                                    if (containerSettingsItems.getParent() instanceof ScrollView) {
                                        ((ScrollView) containerSettingsItems.getParent()).scrollTo(0, 0);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 🚀 포커스가 벗어나면 은은한 배경으로 복귀!
                    layout.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    ((TextView) layout.getChildAt(0)).setTextColor(ThemeManager.getTextColorPrimary());
                    TextView rightTv = (TextView) layout.getChildAt(1);
                    String currentText = rightTv.getText().toString();

                    // 🚀 [버그 수리] SHOW 상태인 글자도 회색으로 오염되지 않고 기본 주 글자색(흰색)을 유지하도록 방어막 체결!
                    // 💡 [수정] 아이콘(↕)이 붙어있어도 SHOW 글자가 '포함'되어 있다면 무조건 흰색을 유지하도록 .contains() 로 판별망을
                    // 넓힙니다!
                    if (currentText.equals("ON") || currentText.equals("ONE") || currentText.equals("ALL")
                            || currentText.contains(t("SHOW")))
                        rightTv.setTextColor(ThemeManager.getTextColorPrimary());
                    else
                        rightTv.setTextColor(ThemeManager.getTextColorSecondary());
                }
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 2, 0, 2);
        layout.setLayoutParams(lp);

        return layout;
    }

    // 🚀 [추가 엔진] 기존 코드들이 에러 나지 않도록, 색상 지정을 안 하면 자동으로 0(기본 테마색)을 넘겨주는 보조 함수입니다!
    public View createListButtonWithIcon(String iconUnicode, String textLabel) {
        return createListButtonWithIcon(iconUnicode, textLabel, 0);
    }

    // 🚀 [메인 엔진 업그레이드] 세 번째 파라미터로 'customColor(도색할 색상)'을 받도록 개조되었습니다.
    public View createListButtonWithIcon(String iconUnicode, String textLabel, final int customColor) {
        float d = getResources().getDisplayMetrics().density;

        final LinearLayout rowButton = new LinearLayout(this);
        rowButton.setOrientation(LinearLayout.HORIZONTAL);
        rowButton.setGravity(Gravity.CENTER_VERTICAL);
        rowButton.setFocusable(true);
        rowButton.setClickable(true);
        rowButton.setSoundEffectsEnabled(false);
        rowButton.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));

        int padLeft = (int) (25 * d);
        int padTopBottom = (int) (12 * d);
        int padRight = (int) (10 * d);
        rowButton.setPadding(padLeft, padTopBottom, padRight, padTopBottom);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 2, 0, 2);
        rowButton.setLayoutParams(rowLp);

        // 💡 [핵심 기술] 사용자가 색상을 지정했다면 그 색상을, 지정 안 했다면 테마 기본색을 변수에 장전합니다!
        final int normalColor = (customColor != 0) ? customColor : ThemeManager.getTextColorPrimary();

        final TextView tvIcon = new TextView(this);
        tvIcon.setText(iconUnicode);
        tvIcon.setTextSize(21f);
        tvIcon.setTextColor(normalColor); // 🚀 지정된 색상으로 도색!

        if (materialIconFont == null) {
            try {
                materialIconFont = Typeface.createFromAsset(getAssets(),
                        "fonts/MaterialIcons-Regular.ttf");
            } catch (Exception e) {
            }
        }
        if (materialIconFont != null)
            tvIcon.setTypeface(materialIconFont);

        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iconLp.rightMargin = (int) (15 * d);
        tvIcon.setLayoutParams(iconLp);

        final TextView tvText = new TextView(this);
        tvText.setText(textLabel);
        tvText.setTextSize(18f);
        tvText.setTextColor(normalColor); // 🚀 텍스트도 똑같이 도색!
        tvText.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);

        // 🚀 [마키(Marquee) 롤링 엔진 가동!] 무조건 한 줄로 고정하고, 휠이 닿으면 흐르게 만듭니다.
        tvText.setSingleLine(true);
        tvText.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        tvText.setMarqueeRepeatLimit(-1); // 무한 반복
        tvText.setHorizontalFadingEdgeEnabled(true); // 양끝을 부드럽게 페이드 처리

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvText.setLayoutParams(textLp);

        rowButton.addView(tvIcon);
        rowButton.addView(tvText);

        rowButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    rowButton.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    tvIcon.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvText.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvText.setSelected(true); // 🚀 휠이 닿으면 텍스트가 흐르기 시작!
                    showFastScrollLetter(tvText.getText().toString());
                } else {
                    // 🚀 [포커스 복구 버그 완전 해결] 포커스가 빠져나갈 때 흰색으로 리셋되지 않고, 처음에 칠했던 색상으로 정확히 복귀합니다!
                    rowButton.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    tvIcon.setTextColor(normalColor);
                    tvText.setTextColor(normalColor);
                    tvText.setSelected(false); // 🚀 휠이 벗어나면 텍스트 정지!
                }
            }
        });

        return rowButton;
    }

    public Button createListButton(String text) {
        final Button btn = new Button(this);

        // 🚀 [수정 완료] 단색 덮어쓰기(setBackgroundColor)를 삭제하고, 둥글기가 적용된 배경만 입힙니다!
        btn.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
        btn.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
        btn.setSoundEffectsEnabled(false);
        btn.setText(t(text));
        btn.setTextSize(18);
        btn.setTextColor(ThemeManager.getTextColorPrimary());

        btn.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

        // 🚀 [수정] 해상도(Density)에 맞춰서 여백(dp)을 넉넉하게 띄워줍니다!
        float density = getResources().getDisplayMetrics().density;
        int padLeft = (int) (25 * density); // 왼쪽 여백 25dp로 시원하게 띄우기!
        int padTopBottom = (int) (12 * density);
        int padRight = (int) (10 * density);
        btn.setPadding(padLeft, padTopBottom, padRight, padTopBottom);

        btn.setFocusable(true);
        btn.setSingleLine(true);

        // 🚀 [마키(Marquee) 롤링 엔진 가동!]
        btn.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
        btn.setMarqueeRepeatLimit(-1); // 무한 반복
        btn.setHorizontalFadingEdgeEnabled(true);

        btn.setOnLongClickListener(globalScreenOffLongClickListener);
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // 🚀 포커스 상태 둥근 배경 적용!
                    btn.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    btn.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    btn.setSelected(true); // 🚀 텍스트 흐르기 시작!
                    showFastScrollLetter(((Button) v).getText().toString());
                } else {
                    // 🚀 일반 상태 둥근 배경 적용!
                    btn.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    btn.setTextColor(ThemeManager.getTextColorPrimary());
                    btn.setSelected(false); // 🚀 텍스트 흐르기 정지!
                }
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 2, 0, 2);
        btn.setLayoutParams(lp);

        return btn;
    }

    private void buildSettingsUI() {
        // 🚨 이전에 추가했던 조건문들을 싹 다 지우고, 무조건 매니저의 메인 화면을 호출하도록 원상 복구합니다!
        com.themoon.y1.managers.SettingsMenuManager.getInstance(this).buildSettingsUI();
    }
      // 💡 [신규 추가] 언어팩 선택 전용 화면

    public void buildLanguageSelectorUI() {
        currentSettingsDepth = 2; // 🚀 카테고리(0) → 서브 메뉴(1) → 이 화면(2)
        containerSettingsItems.removeAllViews();

        final LanguageManager langMgr = LanguageManager
                .getInstance(this);
        langMgr.loadAvailableLanguages(); // 폴더에서 다시 스캔

        // 1. 기본 제공 영어 버튼
        String enPrefix = langMgr.currentLangFileName.equals("English (Default)") ? "✔ " : "   ";
        Button btnEng = createListButton(enPrefix + "English (Default)");
        if (langMgr.currentLangFileName.equals("English (Default)")) {
            btnEng.setTextColor(0xFF00FF00);
        }
        btnEng.setOnClickListener(v -> {
            clickFeedback();
            prefs.edit().putString("app_language", "English (Default)").commit();
            langMgr.applyLanguage("English (Default)");
            recreate(); // 화면 전체 새로고침하여 즉시 적용!
        });
        containerSettingsItems.addView(btnEng);

        // 2. 외부에서 읽어온 JSON 언어팩들 목록 나열
        for (final File f : langMgr.availableLangFiles) {
            String prefix = langMgr.currentLangFileName.equals(f.getName()) ? "✔ " : "   ";
            Button btnLang = createListButton(prefix + f.getName().replace(".json", ""));
            if (langMgr.currentLangFileName.equals(f.getName())) {
                btnLang.setTextColor(0xFF00FF00);
            }

            btnLang.setOnClickListener(v -> {
                clickFeedback();
                prefs.edit().putString("app_language", f.getName()).commit();
                langMgr.applyLanguage(f.getName());
                recreate(); // 언어 바꾸면 즉시 액티비티 재부팅!
            });
            containerSettingsItems.addView(btnLang);
        }

        if (containerSettingsItems.getChildCount() > 0)
            containerSettingsItems.getChildAt(0).requestFocus();
    }

    private void buildRadioUI() {
        currentSettingsDepth = 1;
        isRadioUIShowing = true; // 🚀 내가 지금 라디오 화면에 있다는 걸 시스템에 알림!
        containerSettingsItems.removeAllViews();

        // 🚀 상단 "Settings" 유령 타이틀 원천 차단 숨김 처리
        ViewGroup settingsGroup = (ViewGroup) layoutSettingsMode;
        if (settingsGroup != null && settingsGroup.getChildCount() > 0
                && settingsGroup.getChildAt(0) instanceof TextView) {
            settingsGroup.getChildAt(0).setVisibility(View.GONE);
        }

        final com.themoon.y1.managers.FmRadioManager fmManager = com.themoon.y1.managers.FmRadioManager
                .getInstance(this);

        if (savedRadioStations.isEmpty()) {
            try {
                String savedStationsStr = prefs.getString("radio_stations", "");
                if (!savedStationsStr.isEmpty()) {
                    for (String s : savedStationsStr.split(","))
                        savedRadioStations.add(Float.parseFloat(s));
                }
            } catch (Exception e) {
            }
        }

        final float density = getResources().getDisplayMetrics().density;

        // ==========================================================
        // 🎧 [모드 1] 기본 플레이어 모드 (🔮 네온 하이라이트 글로우 + 하단 정렬)
        // ==========================================================
        if (!isRadioSettingsMode) {

            // 고급 외곽 프레임 패널 세팅
            FrameLayout freqPanel = new FrameLayout(this);
            LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            panelLp.setMargins((int) (15 * density), (int) (30 * density), (int) (15 * density), (int) (15 * density));
            freqPanel.setLayoutParams(panelLp);

            GradientDrawable panelBg = new GradientDrawable();
            panelBg.setShape(GradientDrawable.RECTANGLE);
            panelBg.setCornerRadius(18 * density);

            int themeHighlightColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;

            if (fmManager.isPowerUp) {
                int backlitColor = (themeHighlightColor & 0x00FFFFFF) | 0x42000000;
                panelBg.setColor(backlitColor);
                panelBg.setStroke((int) (4 * density), themeHighlightColor);
            } else {
                panelBg.setColor(0x15FFFFFF);
                panelBg.setStroke((int) (1 * density), 0x33FFFFFF);
            }
            freqPanel.setBackground(panelBg);

            // 대형 디지털 주파수 텍스트 뷰
            final TextView tvFreq = new TextView(this);
            tvFreq.setTag("radio_main_freq_text");
            tvFreq.setText(String.format(Locale.US, "%.1f MHz", fmManager.currentFreq));
            tvFreq.setTextColor(fmManager.isPowerUp ? themeHighlightColor : 0xFF888888);
            tvFreq.setTextSize(54);
            tvFreq.setGravity(Gravity.CENTER);
            tvFreq.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
            tvFreq.setPadding(0, (int) (38 * density), 0, (int) (38 * density));

            freqPanel.addView(tvFreq);
            containerSettingsItems.addView(freqPanel);

            // 🍬 가로 스크롤형 알약 채널 컨테이너
            if (!savedRadioStations.isEmpty()) {
                android.widget.HorizontalScrollView hzScroll = new android.widget.HorizontalScrollView(this);
                hzScroll.setHorizontalScrollBarEnabled(false);
                hzScroll.setClipChildren(false);
                hzScroll.setClipToPadding(false);
                // 💡 이 옵션이 켜져 있어야 채널이 적을 때(1~3개) 예쁘게 중앙 정렬이 가능합니다!
                hzScroll.setFillViewport(true);
                hzScroll.setPadding(0, 15, 0, 15);

                LinearLayout candyContainer = new LinearLayout(this);
                candyContainer.setOrientation(LinearLayout.HORIZONTAL);

                // 🚀 [수리 1] CENTER_VERTICAL 대신 완벽한 CENTER 정렬을 허용합니다.
                candyContainer.setGravity(Gravity.CENTER);

                for (int i = 0; i < savedRadioStations.size(); i++) {
                    float stationFreq = savedRadioStations.get(i);

                    TextView tvCandy = new TextView(this);
                    tvCandy.setText(String.format(Locale.US, "%.1f", stationFreq));
                    tvCandy.setTextSize(18f);
                    tvCandy.setGravity(Gravity.CENTER);
                    tvCandy.setPadding((int) (14 * density), (int) (6 * density), (int) (14 * density),
                            (int) (6 * density));
                    tvCandy.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
                    tvCandy.setTag(stationFreq);

                    GradientDrawable candyBg = new GradientDrawable();
                    candyBg.setCornerRadius(20 * density);

                    if (Math.abs(fmManager.currentFreq - stationFreq) < 0.05f) {
                        candyBg.setColor(themeHighlightColor);
                        tvCandy.setTextColor(ThemeManager.getListButtonFocusedTextColor());

                        final View targetChild = tvCandy;
                        final android.widget.HorizontalScrollView finalHzScroll = hzScroll;
                        hzScroll.post(new Runnable() {
                            @Override
                            public void run() {
                                int scrollX = targetChild.getLeft() - (finalHzScroll.getWidth() / 2)
                                        + (targetChild.getWidth() / 2);
                                if (scrollX < 0)
                                    scrollX = 0; // 안전장치
                                finalHzScroll.scrollTo(scrollX, 0);
                            }
                        });
                    } else {
                        candyBg.setColor(ThemeManager.getListButtonNormalBg());
                        tvCandy.setTextColor(ThemeManager.getTextColorSecondary());
                    }

                    tvCandy.setBackground(candyBg);

                    LinearLayout.LayoutParams candyLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    candyLp.setMargins((int) (6 * density), 0, (int) (6 * density), 0);
                    tvCandy.setLayoutParams(candyLp);

                    candyContainer.addView(tvCandy);
                }

                // 🚀 [수리 2 핵심!] MATCH_PARENT와 CENTER_HORIZONTAL을 완전히 버리고 WRAP_CONTENT 로 변경!
                // 이렇게 하면 아이템이 많아져도 왼쪽 벽(0px 지점)이 무너지지 않고 정상적으로 우측으로만 스크롤이 확장됩니다.
                FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);

                // ❌ 절대 금지: containerLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;

                hzScroll.addView(candyContainer, containerLp);

                // 🚀 [버그 수리 완료] 조립이 끝난 가로 스크롤 주머니를 메인 화면에 찰칵! 부착합니다.
                containerSettingsItems.addView(hzScroll);

                layoutRadioCandyContainer = candyContainer;
            }

            // 하단 조작계 배정을 위한 가중치 스페이서
            View spacer = new View(this);
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0,
                    1.0f);
            spacer.setLayoutParams(spacerLp);
            containerSettingsItems.addView(spacer);

            // 3. 설정 모드 진입 버튼 (최하단 안착)
            Button btnSettings = createListButton(t("Radio Settings"));
            btnSettings.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams settingsLp = (LinearLayout.LayoutParams) btnSettings
                    .getLayoutParams();
            if (settingsLp != null) {
                settingsLp.bottomMargin = (int) (15 * density);
                btnSettings.setLayoutParams(settingsLp);
            }

            btnSettings.setOnClickListener(v -> {
                clickFeedback();
                isRadioSettingsMode = true;
                buildRadioUI();
            });
            containerSettingsItems.addView(btnSettings);

            containerSettingsItems.postDelayed(() -> {
                if (containerSettingsItems.getChildCount() > 0) {
                    containerSettingsItems.getChildAt(containerSettingsItems.getChildCount() - 1).requestFocus();
                }
            }, 50);

        }
        // ==========================================================
        // ⚙️ [모드 2] 설정 서브 페이지 모드 (기존 로직 완벽 유지)
        // ==========================================================
        else {
            Button btnClose = createListButton(t("Close Settings"));
            btnClose.setTextColor(0xFFFF8800);
            btnClose.setOnClickListener(v -> {
                clickFeedback();
                isRadioSettingsMode = false;
                isRadioAdjustingFreq = false;
                buildRadioUI();
            });
            containerSettingsItems.addView(btnClose);

            // 🚀 [신규 엔진 전역 변수] 라디오 광클 방지용 타이머 장착! (이 줄은 밖에서 전역으로 선언해도 되지만, 익명 클래스 내부에선 아래처럼
            // 처리합니다)
            final long[] lastRadioPowerToggleTime = { 0 };

            final LinearLayout btnPower = createSettingRow("Radio Power",
                    fmManager.isPowerUp ? t("ON") : t("OFF"));
            btnPower.setOnLongClickListener(globalScreenOffLongClickListener);
            btnPower.setOnClickListener(v -> {
                long now = System.currentTimeMillis();
                // 🚀 [1차 방어막] 전원 버튼을 누른 지 1.5초(1500ms)가 지나지 않았다면 명령을 무시합니다! (광클로 인한 하드웨어 꼬임 완벽
                // 차단)
                if (now - lastRadioPowerToggleTime[0] < 1500) {
                    Toast.makeText(MainActivity.this, t("Please wait a moment..."),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                lastRadioPowerToggleTime[0] = now;

                clickFeedback();
                if (fmManager.isPowerUp) {
                    fmManager.powerDown();
                    isRadioAdjustingFreq = false;
                    setVolumeControlStream(AudioManager.STREAM_MUSIC);
                } else {
                    com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager
                            .getInstance();
                    if (am.isPlaying())
                        am.playOrPauseMusic();
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    if (fmManager.powerUp(fmManager.currentFreq)) {
                        activePlayer = 1;
                        setVolumeControlStream(fmManager.getFmStreamType());
                    } else
                        Toast.makeText(MainActivity.this, "Radio Error: " + fmManager.lastError,
                                Toast.LENGTH_LONG).show();
                }
                updateGlobalStatusPlayIcon();
                buildRadioUI();
            });
            containerSettingsItems.addView(btnPower);

            String freqRightText = isRadioAdjustingFreq ? t("[ ADJUSTING ]") : t("Click to Tune");
            final LinearLayout btnTune = createSettingRow("Tune Frequency", freqRightText);
            if (isRadioAdjustingFreq)
                ((TextView) btnTune.getChildAt(1)).setTextColor(0xFFFF8800);
            btnTune.setOnLongClickListener(globalScreenOffLongClickListener);
            btnTune.setOnClickListener(v -> {
                clickFeedback();
                isRadioAdjustingFreq = !isRadioAdjustingFreq;
                buildRadioUI();
            });
            containerSettingsItems.addView(btnTune);

            boolean isSaved = savedRadioStations.contains(fmManager.currentFreq);
            final LinearLayout btnSaveFreq = createSettingRow("Save Channel",
                    isSaved ? "★ " + t("SAVED") : "☆ " + t("SAVE"));
            if (isSaved)
                ((TextView) btnSaveFreq.getChildAt(1)).setTextColor(0xFFFF8800);
            btnSaveFreq.setOnLongClickListener(globalScreenOffLongClickListener);
            btnSaveFreq.setOnClickListener(v -> {
                clickFeedback();
                if (isSaved) {
                    savedRadioStations.remove(Float.valueOf(fmManager.currentFreq));
                    Toast.makeText(MainActivity.this, t("Removed from saved channels."),
                            Toast.LENGTH_SHORT).show();
                } else {
                    savedRadioStations.add(fmManager.currentFreq);
                    java.util.Collections.sort(savedRadioStations);
                    Toast
                            .makeText(MainActivity.this, t("Channel saved!"), Toast.LENGTH_SHORT).show();
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < savedRadioStations.size(); i++) {
                    sb.append(savedRadioStations.get(i));
                    if (i < savedRadioStations.size() - 1)
                        sb.append(",");
                }
                prefs.edit().putString("radio_stations", sb.toString()).commit();
                buildRadioUI();
            });
            containerSettingsItems.addView(btnSaveFreq);
            // 🚀 [신규 추가] 저장된 채널들을 리스트로 보고 관리할 수 있는 진입 스위치!
            final LinearLayout btnManageChannels = createSettingRow("Saved Channels", t("Edit") + " 〉");
            btnManageChannels.setOnLongClickListener(globalScreenOffLongClickListener);
            btnManageChannels.setOnClickListener(v -> {
                clickFeedback();
                buildRadioSavedChannelsUI(); // 대망의 전용 채널 관리 스튜디오 팝업!
            });
            containerSettingsItems.addView(btnManageChannels);
            final LinearLayout btnAutoScan = createSettingRow("Auto Scan All", t("Start") + " >");
            btnAutoScan.setOnLongClickListener(globalScreenOffLongClickListener);
            btnAutoScan.setOnClickListener(v -> {
                clickFeedback();
                if (!fmManager.isPowerUp) {
                    Toast.makeText(MainActivity.this, t("Please turn on Radio Power first!"),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isRadioScanning)
                    return;

                isRadioScanning = true;
                showLoadingPopup();

                new Thread(() -> {
                    float fakeFreq = 87.5f;
                    int progress = 0;
                    while (isRadioScanning) {
                        final int p = progress;
                        final float f = fakeFreq;
                        runOnUiThread(() -> {
                            if (pbLoadingProgress != null) {
                                pbLoadingProgress.setIndeterminate(false);
                                pbLoadingProgress.setProgress(p);
                            }
                            if (tvLoadingProgress != null) {

                                tvLoadingProgress.setText(String.format(Locale.US,
                                        t("Scanning FM Frequencies...\nSearching around %.1f MHz"), f));
                            }
                        });
                        try {
                            Thread.sleep(70);
                        } catch (Exception e) {
                        }
                        progress += 1;
                        if (progress > 100)
                            progress = 0;
                        fakeFreq += 0.1f;
                        if (fakeFreq > 108.0f)
                            fakeFreq = 87.5f;
                    }
                }).start();

                new Thread(() -> {
                    final float[] foundStations = fmManager.autoScan();
                    isRadioScanning = false;

                    runOnUiThread(() -> {
                        if (layoutLoadingOverlay != null)
                            layoutLoadingOverlay.setVisibility(View.GONE);
                        if (tvLoadingProgress != null)
                            tvLoadingProgress.setText(t("Preparing to scan...\nPlease wait."));

                        if (foundStations != null && foundStations.length > 0) {
                            savedRadioStations.clear();
                            for (float f : foundStations)
                                savedRadioStations.add(f);
                            java.util.Collections.sort(savedRadioStations);

                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < savedRadioStations.size(); i++) {
                                sb.append(savedRadioStations.get(i));
                                if (i < savedRadioStations.size() - 1)
                                    sb.append(",");
                            }
                            prefs.edit().putString("radio_stations", sb.toString()).commit();
                            Toast.makeText(
                                    MainActivity.this, t("Scan Complete!\nFound") + " " + foundStations.length
                                            + t("channels.\nTuning to") + " " + foundStations[0] + "MHz",
                                    Toast.LENGTH_LONG).show();
                            fmManager.tune(foundStations[0]);
                        } else {
                            Toast.makeText(MainActivity.this,
                                    t("No stations found.") + " (" + fmManager.lastError + ")",
                                    Toast.LENGTH_LONG).show();
                        }
                        buildRadioUI();
                    });
                }).start();
            });
            containerSettingsItems.addView(btnAutoScan);

            final LinearLayout btnSpeaker = createSettingRow("Audio Output",
                    fmManager.isSpeakerOn ? t("Speaker") : t("Earphones"));
            btnSpeaker.setOnLongClickListener(globalScreenOffLongClickListener);
            btnSpeaker.setOnClickListener(v -> {
                clickFeedback();
                fmManager.setSpeaker(!fmManager.isSpeakerOn);
                setVolumeControlStream(fmManager.isPowerUp
                        ? fmManager.getFmStreamType()
                        : AudioManager.STREAM_MUSIC);
                buildRadioUI();
            });
            containerSettingsItems.addView(btnSpeaker);

            if (fmManager.isPowerUp) {
                setVolumeControlStream(fmManager.getFmStreamType());
            }

            containerSettingsItems.postDelayed(() -> {
                int targetIdx = lastRadioFocusIndex;
                if (isRadioAdjustingFreq) {
                    targetIdx = 2;
                }
                if (targetIdx >= 0 && targetIdx < containerSettingsItems.getChildCount()) {
                    containerSettingsItems.getChildAt(targetIdx).requestFocus();
                } else if (containerSettingsItems.getChildCount() > 0) {
                    containerSettingsItems.getChildAt(0).requestFocus();
                }
            }, 50);
        }
    }

    public void buildUpdateSettingsUI() {
        currentSettingsDepth = 2; // 🚀 카테고리(0) → 서브 메뉴(1) → 이 화면(2)
        containerSettingsItems.removeAllViews();
        com.themoon.y1.managers.SettingsMenuManager.getInstance(this).updateSettingsTitle(t("System Update"));
        // 1. 내 기기의 현재 버전 가져오기
        String myVersionName = "1.0";
        int tempCode = 1;
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            myVersionName = pInfo.versionName;
            tempCode = pInfo.versionCode;
        } catch (Exception e) {
        }

        final int myVersionCode = tempCode;

        // 2. 현재 버전 표시 줄
        LinearLayout rowCurrent = createSettingRow("Current Version", "v" + myVersionName);
        containerSettingsItems.addView(rowCurrent);

        // 3. 서버 버전 표시 줄 (처음엔 Checking... 으로 표시)
        final LinearLayout rowServer = createSettingRow("Latest Version", "Checking...");
        containerSettingsItems.addView(rowServer);

        createCategoryHeader("━━━━━━━━━━━━━━");

        // 4. 하단 업데이트 실행 버튼 (서버 확인 전까지는 숨겨둡니다)
        final Button btnExecuteUpdate = createListButton("🚀 " + t("DOWNLOAD & UPDATE"));
        ;
        btnExecuteUpdate.setVisibility(View.GONE);
        containerSettingsItems.addView(btnExecuteUpdate);

        // 🚀 5. 화면이 열리자마자 백그라운드에서 서버의 output-metadata.json을 읽어옵니다!
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.net.URL url = new java.net.URL(METADATA_URL);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

                    // 🚀 [필수 1] 깃허브 보안(TLS 1.2) 뚫기: 만들어둔 비밀 무기 장착!
                    if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                        try {
                            ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(new TLSSocketFactory());
                        } catch (Exception e) {
                        }
                    }

                    conn.setInstanceFollowRedirects(false); // 수동 추적을 위해 기본 기능 끄기
                    conn.setConnectTimeout(5000);

                    // 🚀 [필수 2] 깃허브 리다이렉트(주소 우회) 끝까지 쫓아가기!
                    int status = conn.getResponseCode();
                    if (status == 301 || status == 302 || status == 303) {
                        String newUrl = conn.getHeaderField("Location");
                        conn = (java.net.HttpURLConnection) new java.net.URL(newUrl).openConnection();
                        if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                            try {
                                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(new TLSSocketFactory());
                            } catch (Exception e) {
                            }
                        }
                    }

                    java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null)
                        sb.append(line);
                    in.close();

                    org.json.JSONObject root = new org.json.JSONObject(sb.toString());
                    org.json.JSONArray elements = root.getJSONArray("elements");
                    org.json.JSONObject element = elements.getJSONObject(0);

                    final int serverVersionCode = element.getInt("versionCode");
                    final String serverVersionName = element.getString("versionName");
                    final String apkFileName = element.getString("outputFile");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 서버 버전 텍스트를 업데이트 (예: Checking... -> v1.2)
                            TextView tvServer = (TextView) rowServer.getChildAt(1);
                            tvServer.setText("v" + serverVersionName);

                            // 🚀 [비교] 업데이트가 필요할 때
                            if (serverVersionCode > myVersionCode) {
                                tvServer.setTextColor(0xFF00FF00); // 서버 버전을 눈에 띄는 초록색으로!

                                btnExecuteUpdate.setVisibility(View.VISIBLE);
                                btnExecuteUpdate.setTextColor(0xFFFFFFFF);
                                btnExecuteUpdate.setTypeface(null, Typeface.BOLD);
                                btnExecuteUpdate.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        clickFeedback();
                                        String downloadUrl = SERVER_BASE_URL + apkFileName;
                                        downloadAndInstallApk(downloadUrl); // 다운로드 엔진 호출
                                    }
                                });
                            }
                            // 🚀 [비교] 이미 최신 버전일 때
                            else {
                                tvServer.setTextColor(ThemeManager.getTextColorSecondary());

                                btnExecuteUpdate.setText("✔ " + t("ALREADY UP TO DATE"));
                                btnExecuteUpdate.setVisibility(View.VISIBLE);
                                btnExecuteUpdate.setTextColor(ThemeManager.getTextColorSecondary());
                                btnExecuteUpdate.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        clickFeedback();
                                        Toast.makeText(MainActivity.this, t("You are using the latest version."),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView tvServer = (TextView) rowServer.getChildAt(1);
                            tvServer.setText(t("Network Error"));
                            tvServer.setTextColor(0xFFFF4444); // 빨간색 에러 표시
                        }
                    });
                }
            }
        }).start();

        // 진입 시 자동으로 두 번째 버튼(Current Version) 쪽에 포커스
        if (containerSettingsItems.getChildCount() > 0) {
            containerSettingsItems.getChildAt(0).requestFocus();
        }
    }

    // 💡 [신규 추가] 진동 ON/OFF와 세기 조절을 담당하는 전용 서브 메뉴!
    public void buildVibrationSettingsUI() {
        currentSettingsDepth = 2; // 메인 설정 밖으로 나왔음을 시스템에 알림
        containerSettingsItems.removeAllViews();
        com.themoon.y1.managers.SettingsMenuManager.getInstance(this).updateSettingsTitle(t("Vibration"));
        // 1. 진동 전원 스위치
        final LinearLayout btnToggle = createSettingRow("Vibration Power", isVibrationEnabled ? t("ON") : t("OFF"));
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVibrationEnabled = !isVibrationEnabled;
                clickFeedback();
                ((TextView) btnToggle.getChildAt(1)).setText(isVibrationEnabled ? t("ON") : t("OFF"));
                try {
                    prefs.edit().putBoolean("vibrate", isVibrationEnabled).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnToggle);

        // 2. 진동 세기 스위치 (Weak -> Normal -> Strong 순환)
        final LinearLayout btnStrength = createSettingRow("Vibration Strength",
                t(VIBE_STRENGTH_NAMES[vibrationStrengthLevel]));
        btnStrength.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibrationStrengthLevel = (vibrationStrengthLevel + 1) % 3; // 0, 1, 2 순환

                // 💡 누르는 즉시 바뀐 세기의 진동이 울리므로 손맛을 바로 확인할 수 있습니다!
                clickFeedback();

                // 🚀 [수정 완료] 버튼을 눌러서 텍스트가 바뀔 때도 번역기 t()를 무조건 통과하도록 씌워줍니다!
                ((TextView) btnStrength.getChildAt(1)).setText(t(VIBE_STRENGTH_NAMES[vibrationStrengthLevel]));
                try {
                    prefs.edit().putInt("vibrate_strength", vibrationStrengthLevel).commit();
                } catch (Exception e) {
                }
            }
        });
        containerSettingsItems.addView(btnStrength);

        // 메뉴 진입 시 첫 번째 버튼에 포커스!
        if (containerSettingsItems.getChildCount() > 0) {
            containerSettingsItems.getChildAt(0).requestFocus();
        }
    }

    // 💡 [추가] 배경화면 지정 및 삭제를 하나로 묶은 서브 메뉴 화면
    public void buildBackgroundSettingsUI() {
        currentSettingsDepth = 2; // 🚀 카테고리(0) → 서브 메뉴(1) → 이 화면(2)
        containerSettingsItems.removeAllViews();
        com.themoon.y1.managers.SettingsMenuManager.getInstance(this).updateSettingsTitle(t("Background"));
        // 1. 새로운 배경 지정 버튼
        LinearLayout btnSelectBg = createSettingRow("Select New Background", "〉 ");
        btnSelectBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isPickingBackground = true;
                currentFolder = StoragePaths.getPrimaryRoot();
                changeScreen(STATE_BROWSER);
                Toast.makeText(MainActivity.this, t("Select a JPG/PNG image"), Toast.LENGTH_SHORT).show();
            }
        });
        containerSettingsItems.addView(btnSelectBg);

        // 🚀 2. 테마 기본 배경 강제 고정 버튼!
        LinearLayout btnThemeBg = createSettingRow("Apply Theme Background", "〉 ");
        btnThemeBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                // 💡 현재 테마에 진짜 배경이 있는지 확인부터 합니다.
                ThemeManager.ThemeData currentTheme = ThemeManager.getCurrentTheme();
                boolean hasThemeBg = false;
                if (currentTheme.bgImage != null && !currentTheme.bgImage.isEmpty()
                        && new File(currentTheme.folderPath, currentTheme.bgImage).exists())
                    hasThemeBg = true;
                if (!hasThemeBg && new File(currentTheme.folderPath, "background.png").exists())
                    hasThemeBg = true;
                if (!hasThemeBg && new File(currentTheme.folderPath, "bg.png").exists())
                    hasThemeBg = true;

                if (hasThemeBg) {
                    prefs.edit().putString("bg_path", "THEME_DEFAULT").commit();
                    Toast.makeText(MainActivity.this, t("Theme background applied."), Toast.LENGTH_SHORT).show();
                } else {
                    // 테마에 배경이 없을 경우, 억지로 빈 이미지를 적용하지 않고 깔끔하게 설정을 지워 블러 모드로 돌려줍니다!
                    prefs.edit().remove("bg_path").commit();
                    Toast.makeText(MainActivity.this, t("This theme has no background. Switched to Album Blur."),
                            Toast.LENGTH_SHORT).show();
                }
                updateMainMenuBackground(); // 즉시 화면에 렌더링!
            }
        });
        containerSettingsItems.addView(btnThemeBg);

        // 3. 기존 배경 삭제 버튼 (앨범 아트 블러 모드로 복귀)
        LinearLayout btnClearBg = createSettingRow("Clear Custom Background", "〉 ");
        btnClearBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                if (prefs.contains("bg_path")) {
                    prefs.edit().remove("bg_path").commit();
                    Toast.makeText(MainActivity.this, t("Custom background cleared."), Toast.LENGTH_SHORT).show();
                    updateMainMenuBackground(); // 즉시 원래 테마 배경(앨범 아트 블러 모드)으로 복구!
                } else {
                    Toast.makeText(MainActivity.this, t("No custom background set."), Toast.LENGTH_SHORT).show();
                }
            }
        });
        containerSettingsItems.addView(btnClearBg);

        // 메뉴 진입 시 자동으로 첫 번째 버튼에 포커스!
        if (containerSettingsItems.getChildCount() > 0) {
            containerSettingsItems.getChildAt(0).requestFocus();
        }
    }

    // 💡 [개조 완료] 다운로드 진행률(%)과 용량(MB)을 실시간 팝업으로 보여주는 엔진!
    private void downloadAndInstallApk(final String apkUrl) {
        // 🚀 1. 화면에 띄울 '다운로드 진행률 팝업창'의 디자인을 자바 코드로 직접 조립합니다.
        final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);

        final TextView tvProgress = new TextView(this);
        tvProgress.setGravity(Gravity.CENTER);
        tvProgress.setPadding(0, 30, 0, 0);
        tvProgress.setTextSize(16);
        tvProgress.setText(t("Connecting to server..."));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.addView(progressBar);
        layout.addView(tvProgress);

        final AlertDialog progressDialog = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Downloading Update"))
                .setView(layout)
                .setCancelable(false) // 💡 다운로드 중에 다른 곳을 터치해도 창이 닫히지 않도록 잠급니다!
                .create();

        progressDialog.show();

        // 🚀 2. 백그라운드 다운로드 쓰레드 시작
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.net.URL url = new java.net.URL(apkUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

                    // 🚀 [필수 1] 깃허브 보안(TLS 1.2) 뚫기
                    if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                        try {
                            ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(new TLSSocketFactory());
                        } catch (Exception e) {
                        }
                    }

                    conn.setInstanceFollowRedirects(false);

                    // 🚀 [여기 추가!!] 안드로이드의 자동 압축(GZIP) 오지랖 끄기! (용량 뻥튀기 원천 차단)
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Cache-Control", "no-cache");
                    // 🚀 [필수 2] 깃허브 리다이렉트(주소 우회) 쫓아가서 파일 낚아채기!
                    int status = conn.getResponseCode();
                    if (status == 301 || status == 302 || status == 303) {
                        String newUrl = conn.getHeaderField("Location");
                        conn = (java.net.HttpURLConnection) new java.net.URL(newUrl).openConnection();
                        if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                            try {
                                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(new TLSSocketFactory());
                            } catch (Exception e) {
                            }
                        }

                        // 🚀 [여기 추가!!] 리다이렉트 된 진짜 다운로드 주소에서도 압축 금지 명령 다시 내리기!
                        conn.setRequestProperty("Accept-Encoding", "identity");
                    }

                    conn.connect();

                    // 서버로부터 파일의 전체 총 용량을 알아냅니다.
                    final int fileLength = conn.getContentLength();

                    // ❌ [기존 다운로드 경로 지정 코드를 전부 지워주세요]
                    // File sdcard = android.os.Environment.getExternalStorageDirectory();
                    // ...

                    // 🚀 ⭕ [새로운 코드로 덮어쓰기] SD카드의 간섭을 받지 않는 '앱 전용 내부 금고'를 생성합니다!
                    File dir = getDir("update", Context.MODE_PRIVATE);
                    final File updateFile = new File(dir, "Y1_Launcher_Update.apk");

                    FileOutputStream fos = new FileOutputStream(updateFile);
                    java.io.InputStream is = conn.getInputStream();

                    byte[] buffer = new byte[4096]; // 💡 다운로드 속도를 위해 버퍼를 4배 늘렸습니다.
                    int len;
                    long total = 0;

                    // 파일을 조각조각 다운로드 받으면서 동시에 화면에 퍼센트를 쏴줍니다.
                    while ((len = is.read(buffer)) != -1) {
                        total += len;
                        fos.write(buffer, 0, len);

                        if (fileLength > 0) {
                            final int progress = (int) (total * 100 / fileLength);
                            final long downloadedMB = total / (1024 * 1024);
                            final long totalMB = fileLength / (1024 * 1024);

                            // 화면(UI)을 조작하는 것은 반드시 메인 쓰레드에서 해야 합니다.
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(progress);
                                    tvProgress.setText(progress + "%   (" + downloadedMB + "MB / " + totalMB + "MB)");
                                }
                            });
                        }
                    }
                    fos.close();
                    is.close();
                    // (앞부분 생략) 루프가 끝난 직후 찌꺼기 검사 부분부터 덮어쓰기

                    if (fileLength > 0 && total != fileLength) {
                        if (updateFile.exists())
                            updateFile.delete();
                        throw new Exception("Incomplete Download: Size Mismatch");
                    }

                    // 🚀 [여기서부터 덮어쓰기!!] 다운로드가 끝나면 창을 바로 닫지 않고, 서버가 준 용량과 내가 받은 용량을 화면에 박제합니다!
                    final String debugMessage = "Server told: " + fileLength + " bytes\nActually got: " + total
                            + " bytes";

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 1. 프로그레스 바를 꽉 채우고, 우리가 확인해야 할 핵심 숫자를 화면에 띄웁니다.
                            progressBar.setProgress(100);
                            tvProgress.setText(t("Download Finished! Waiting 3 sec...\n\n") + debugMessage);
                            tvProgress.setTextColor(0xFF000000); // 눈에 확 띄게 노란색으로!

                            // 2. 정확히 3초(3000ms) 동안 화면을 멈춰둔 뒤에 팝업을 닫고 설치를 시도합니다.
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();

                                    // 🚀 [UI 멈춤 팝업 완벽 차단] 메인 화면 일꾼은 놔주고, 설치는 백그라운드 일꾼에게 조용히 시킵니다!
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            installApk(updateFile);
                                        }
                                    }).start();
                                }
                            }, 3000);
                        }
                    });

                    // 👆 [여기까지 덮어쓰기 끝] 👆
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, t("Download failed. Check your internet connection."),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private boolean isAudioFile(File f) {
        if (f == null || !f.isFile())
            return false;
        String name = f.getName().toLowerCase();
        // 🚀 [.m4b 오디오북 공식 지원 도어 오픈!]
        return name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".ogg")
                || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ape") || name.endsWith(".wma")
                || name.endsWith(".opus") || name.endsWith(".m4b");
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

    // 🚀 [신규 추가] '💖 My Favorites' 전용 곡 리스트 생성기
    private void buildVirtualSongsForFavorites() {
        if (isCustomScanning) {
            showLoadingPopup();
            currentBrowserMode = BROWSER_ROOT;
            buildFileBrowserUI();
            return;
        }
        scrollViewBrowser.setVisibility(View.GONE);
        listVirtualSongs.setVisibility(View.VISIBLE);

        tvBrowserPath.setText(t("Library") + ": " + t("My Favorites"));

        virtualSongList.clear();
        currentScrollIndexList.clear();
        List<SongItem> targetSongs = new ArrayList<>();

        for (SongItem song : customLibrary) {
            // 금고(favoritePaths)에 이 노래의 경로가 적혀있다면 리스트에 합류!
            if (favoritePaths.contains(song.file.getAbsolutePath())) {
                targetSongs.add(song);
            }
        }

        // 🚀 [수정] 즐겨찾기 목록도 대소문자 구분 없이 제목순으로 정렬!
        java.util.Collections.sort(targetSongs, new java.util.Comparator<SongItem>() {
            @Override
            public int compare(SongItem s1, SongItem s2) {
                return s1.title.compareToIgnoreCase(s2.title);
            }
        });

        // 정렬된 순서대로 재생 목록과 인덱스를 채웁니다.
        for (SongItem song : targetSongs) {
            virtualSongList.add(song.file);
            currentScrollIndexList.add(song.title);
        }

        if (targetSongs.isEmpty()) {
            Toast.makeText(this, t("No favorites added yet."), Toast.LENGTH_SHORT).show();
        }

        SongListAdapter adapter = new SongListAdapter(targetSongs);
        listVirtualSongs.setAdapter(adapter);

        int targetIndex = -1;
        if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentIndex >= 0
                && currentIndex < currentPlaylist.size()) {
            File playingFile = currentPlaylist.get(currentIndex);
            for (int i = 0; i < targetSongs.size(); i++) {
                if (targetSongs.get(i).file.getAbsolutePath().equals(playingFile.getAbsolutePath())) {
                    targetIndex = i;
                    break;
                }
            }
        }

        final int finalTargetIdx = targetIndex;
        listVirtualSongs.post(new Runnable() {
            @Override
            public void run() {
                if (finalTargetIdx >= 0 && finalTargetIdx < listVirtualSongs.getCount()) {
                    listVirtualSongs.setSelectionFromTop(finalTargetIdx, 0);
                    listVirtualSongs.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int visiblePos = finalTargetIdx - listVirtualSongs.getFirstVisiblePosition();
                            if (visiblePos >= 0 && visiblePos < listVirtualSongs.getChildCount()) {
                                listVirtualSongs.getChildAt(visiblePos).requestFocus();
                            }
                        }
                    }, 50);
                } else {
                    if (listVirtualSongs.getChildCount() > 0) {
                        listVirtualSongs.getChildAt(0).requestFocus();
                    }
                }
            }
        });
    }

    // 💡 2. 라이브러리 메인 라우터 (자체 스캔 버튼 적용)
    // 💡 기존 코드 수정
    private void buildFileBrowserUI() {
        View statusBar = findViewById(R.id.layout_status_bar);
        if (statusBar != null)
            statusBar.setBackgroundColor(ThemeManager.getStatusBarBackgroundColor());
        if (scrollViewBrowser != null)
            scrollViewBrowser.setVisibility(View.VISIBLE);
        if (listVirtualSongs != null)
            listVirtualSongs.setVisibility(View.GONE);
        containerBrowserItems.removeAllViews();

        // =======================================================
        // 🚀 [수정할 부분] 이 줄 끝에 '|| currentBrowserMode == BROWSER_VIDEOS' 를 꼭 추가해 줘야 폴더가
        // 화면에 나와!
        // =======================================================
        if (isPickingBackground || currentBrowserMode == BROWSER_FOLDER || currentBrowserMode == BROWSER_AUDIOBOOKS
                || currentBrowserMode == BROWSER_VIDEOS) {
            buildFolderBrowserUI();
            return;
        }

        if (currentBrowserMode == BROWSER_ROOT) {

            // 🎵 [뮤직 라이브러리 모드]
            if (!isAudiobookLibraryMode) {
                tvBrowserPath.setText(t("Library") + ": " + t("Music"));

                View btnCoverFlow = createListButtonWithIcon("\uE3B6", t("Cover Flow"));

                // 🚀 [지능형 부팅 스캔 추적 가동 - 라이브러리 선전환 + 투명 로딩 버전]
                btnCoverFlow.setOnClickListener(v -> {
                    clickFeedback();
                    lastBrowserFocusText = "FROM_LIBRARY";

                    if (isCustomScanning) {
                        currentBrowserMode = BROWSER_COVER_FLOW;
                        changeScreen(STATE_BROWSER);
                        showLoadingPopup();
                    } else if (customLibrary.isEmpty()) {
                        startMediaLibraryScan();
                        currentBrowserMode = BROWSER_COVER_FLOW;
                        changeScreen(STATE_BROWSER);
                    } else {
                        // 🚀 [라이브러리 내부용 선전환 공정]
                        currentBrowserMode = -1; // 빌더 간섭 차단
                        changeScreen(STATE_BROWSER); // 커버플로우 페이지로 선제 이동!

                        // 🚀 [깜빡임 버그 완벽 수리] 내부 진입 시에도 누르는 순간부터 즉시 완전 투명화 장전!
                        if (statusBar != null)
                            statusBar.setBackgroundColor(0x00000000);

                        if (tvBrowserPath != null)
                            tvBrowserPath.setText(t("Library") + ": " + t("Cover Flow"));
                        if (containerBrowserItems != null)
                            containerBrowserItems.removeAllViews();
                        if (listVirtualSongs != null)
                            listVirtualSongs.setVisibility(View.GONE);
                        if (scrollViewBrowser != null)
                            scrollViewBrowser.setVisibility(View.VISIBLE);

                        // ⚡ [기획 반영] 전체 화면 블랙 마스킹을 해제하고 상단 상태 바 영역을 완벽히 살린 채 글씨만 노출합니다.
                        if (layoutLoadingOverlay != null) {
                            layoutLoadingOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT); // 🚀 배경 투명화
                            if (pbLoadingProgress != null)
                                pbLoadingProgress.setVisibility(View.GONE); // 🚀 프로그레스 바 숨김
                            if (tvLoadingProgress != null) {
                                tvLoadingProgress.setTextSize(18f);
                                tvLoadingProgress.setTextColor(ThemeManager.getTextColorPrimary());
                                tvLoadingProgress.setText(t("Loading Cover Flow...\nPlease wait."));
                            }
                            layoutLoadingOverlay.setAlpha(1.0f);
                            layoutLoadingOverlay.setVisibility(View.VISIBLE);
                        }

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                currentBrowserMode = BROWSER_COVER_FLOW;
                                buildCoverFlowUI(); // 3D 레코드장 부드럽게 팝업 오픈!

                                if (layoutLoadingOverlay != null) {
                                    layoutLoadingOverlay.setVisibility(View.GONE);
                                    layoutLoadingOverlay.setBackgroundColor(0xDD000000); // 다른 스캔을 위해 원상복구
                                    if (tvLoadingProgress != null)
                                        tvLoadingProgress.setTextColor(0xFFFFFFFF);
                                    if (pbLoadingProgress != null)
                                        pbLoadingProgress.setVisibility(View.VISIBLE);
                                }
                            }
                        }, 150);
                    }
                });
                containerBrowserItems.addView(btnCoverFlow);
                View btnAll = createListButtonWithIcon("\uE03D", t("All Songs"));
                btnAll.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_VIRTUAL_SONGS;
                    virtualQueryType = "ALL";
                    buildVirtualSongs();
                });
                containerBrowserItems.addView(btnAll);
                View btnM3uPlaylist = createListButtonWithIcon("\uE05F", t("Playlists"));
                btnM3uPlaylist.setOnClickListener(v -> {
                    clickFeedback();
                    isPlaylistOpenedFromLibrary = true; // 🚀 확실하게 라이브러리를 거쳐왔음을 도장에 찍습니다!
                    lastBrowserFocusText = "FROM_LIBRARY";
                    currentBrowserMode = BROWSER_PLAYLISTS;
                    buildM3uPlaylistUI();
                });
                containerBrowserItems.addView(btnM3uPlaylist); // 🚀 [핵심 복구 완료] 만든 버튼을 라이브러리 화면 패널에 단단히 조립합니다!
                // Button btnFolder = createListButton("📁 " + t("Folders"));
                View btnFolder = createListButtonWithIcon("\uE2C7", t("Folders"));
                btnFolder.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_FOLDER;
                    currentFolder = rootFolder;
                    buildFileBrowserUI();
                });
                containerBrowserItems.addView(btnFolder);

                // =========================================================
                // 🚀 [메인 메뉴 통합] 가수 -> 앨범 -> 노래 3단 콤보 전용 버튼!
                // =========================================================
                View btnArtist = createListButtonWithIcon("\uE7FD", t("Artists"));
                btnArtist.setOnClickListener(v -> {
                    clickFeedback();
                    isArtistAlbumMode = true; // 💡 스위치 ON! (가수를 누르면 앨범 목록으로 100% 납치)
                    currentBrowserMode = BROWSER_ARTISTS;
                    virtualQueryValue = "";
                    lastBrowserFocusText = t("Artists"); // 🚀 [포커스 복구 엔진] 뒤로 돌아왔을 때 정확히 이 버튼에 록온!
                    buildVirtualCategories("ARTIST");
                });
                containerBrowserItems.addView(btnArtist);

                // Button btnAlbum = createListButton("💿 " + t("Albums"));
                View btnAlbum = createListButtonWithIcon("\uE019", t("Albums"));
                btnAlbum.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_ALBUMS;
                    virtualQueryValue = "";
                    buildVirtualCategories("ALBUM");
                });
                containerBrowserItems.addView(btnAlbum);

                View btnYear = createListButtonWithIcon("\uE916", t("Years"));
                btnYear.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_YEARS;
                    virtualQueryValue = "";
                    buildVirtualCategories("YEAR");
                });
                containerBrowserItems.addView(btnYear);

                View btnGenre = createListButtonWithIcon("\uE030", t("Genres"));
                btnGenre.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_GENRES;
                    virtualQueryValue = "";
                    buildVirtualCategories("GENRE");
                });
                containerBrowserItems.addView(btnGenre);
                // Button btnAll = createListButton("🎵 " + t("All Songs"));


                // 🚀 [신규 장착] 최근 추가된 곡 (시계 아이콘 유니코드 적용)
                View btnRecent = createListButtonWithIcon("\uE192", t("Recently Added"));
                btnRecent.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_RECENTLY_ADDED;
                    virtualQueryType = "RECENT";
                    buildVirtualSongs();
                });
                containerBrowserItems.addView(btnRecent);
                // 🚀 [팟캐스트 스튜디오 진입 스위치]
                View btnPodcast = createListButtonWithIcon("\uE03E", t("Podcasts")); // 🎙️ 라디오/팟캐스트 아이콘
                btnPodcast.setOnClickListener(v -> {
                    clickFeedback();

                    // 💡 라이브러리를 거쳐서 들어왔다고 깃발에 명확히 기록합니다!
                    isPodcastOpenedFromLibrary = true;
                    lastBrowserFocusText = "FROM_LIBRARY"; // (선택) 포커스 복귀용 메모

                    currentBrowserMode = BROWSER_PODCAST_CHANNELS;
                    buildPodcastChannelsUI(); // 대망의 채널 리스트 팝업!
                });
                containerBrowserItems.addView(btnPodcast);

                // =======================================================
                // 🚀 [신규 장착] 메인 메뉴/라이브러리 직속 비디오 브라우저 숏컷!
                // =======================================================
                View btnVideoBrowser = createListButtonWithIcon("\uE04B", t("Video Browser")); // 비디오 아이콘 유니코드 적용
                btnVideoBrowser.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_VIDEOS;
                    isVideoOpenedFromLibrary = true;
                    lastBrowserFocusText = "FROM_LIBRARY";
                    // SD카드의 비디오 전용 저장소나 전체 루트를 지정해 줍니다.
                    currentFolder = StoragePaths.getVideosDir();
                    if (!currentFolder.exists()) currentFolder.mkdirs();
                    buildFileBrowserUI(); // 📁 폴더 탐색기 엔진 가동!
                });
                containerBrowserItems.addView(btnVideoBrowser);

                View btnFav = createListButtonWithIcon("\uE87D", t("My Favorites"));

                // btnFav.setTextColor(0xFFFF8888);
                btnFav.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_FAVORITES;
                    buildVirtualSongsForFavorites();
                });
                containerBrowserItems.addView(btnFav);

                // 🎮 [몰래 숨겨놓은 게임 버튼 부활!]
                View btnGame = createListButtonWithIcon("\uE338", t("Game"));
                btnGame.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_GAMES;
                    buildGameListUI();
                });
                containerBrowserItems.addView(btnGame);

                // 🎧 오디오북 모드로 넘어가기 버튼
                // Button btnAudiobook = createListButton("🎧 " + t("Switch to Audiobooks"));
                View btnAudiobook = createListButtonWithIcon("\uE86D", t("Switch to Audiobooks"));
                // btnAudiobook.setTextColor(0xFF00FFFF);
                btnAudiobook.setOnClickListener(v -> {
                    clickFeedback();
                    isAudiobookLibraryMode = true;
                    buildFileBrowserUI();
                });
                containerBrowserItems.addView(btnAudiobook);
            }
            // 📚 [오디오북 라이브러리 모드]
            else {
                tvBrowserPath.setText(t("Library") + ": " + t("Audiobooks"));

                // Button btnFolder = createListButton("📁 " + t("Folders"));
                View btnFolder = createListButtonWithIcon("\uE2C7", t("Folders"));
                btnFolder.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_FOLDER;
                    currentFolder = audiobookRootFolder;
                    buildFileBrowserUI();
                });
                containerBrowserItems.addView(btnFolder);

                View btnAuthor = createListButtonWithIcon("\uE7FD", t("Authors"));
                btnAuthor.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_ARTISTS;
                    virtualQueryValue = "";
                    buildVirtualCategories("ARTIST");
                });
                containerBrowserItems.addView(btnAuthor);

                View btnBook = createListButtonWithIcon("\uE86D", t("Books"));
                btnBook.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_ALBUMS;
                    virtualQueryValue = "";
                    buildVirtualCategories("ALBUM");
                });
                containerBrowserItems.addView(btnBook);

                View btnAll = createListButtonWithIcon("\uE8FE", t("All Audiobooks"));

                btnAll.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_VIRTUAL_SONGS;
                    virtualQueryType = "ALL";
                    buildVirtualSongs();
                });
                containerBrowserItems.addView(btnAll);

                // 🎵 뮤직 모드로 돌아가기 버튼
                View btnMusic = createListButtonWithIcon("\uE03D", t("Switch to Music"));

                btnMusic.setOnClickListener(v -> {
                    clickFeedback();
                    isAudiobookLibraryMode = false;
                    buildFileBrowserUI();
                });
                containerBrowserItems.addView(btnMusic);
            }
            // 스캔 중일 때는 모래시계(\uE88B), 평소에는 동기화 화살표(\uE863) 유니코드를 적용합니다.
            String scanIcon = isCustomScanning ? "\uE88B" : "\uE863";
            String scanText = isCustomScanning ? t("Scanning Media...") : t("Scan Media Library");

            View btnScan = createListButtonWithIcon(scanIcon, scanText);

            btnScan.setOnClickListener(v -> {
                clickFeedback();
                startMediaLibraryScan();
            });
            containerBrowserItems.addView(btnScan);

            if (containerBrowserItems.getChildCount() > 0)
                containerBrowserItems.getChildAt(0).requestFocus();
        }
        // 🚀 [대개조 완료] ScrollView 본체에 리스너를 달아 스크롤 범위가 100% 확보된 순간 단번에 화면을 내립니다!
        final ScrollView sv = (ScrollView) containerBrowserItems.getParent();
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    sv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                boolean found = false;
                if (!lastBrowserFocusText.isEmpty()) {
                    for (int i = 0; i < containerBrowserItems.getChildCount(); i++) {
                        View v = containerBrowserItems.getChildAt(i);
                        String itemText = "";

                        if (v instanceof Button) {
                            itemText = ((Button) v).getText().toString();
                        } else if (v instanceof LinearLayout) {
                            LinearLayout layout = (LinearLayout) v;
                            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof TextView) {
                                itemText = ((TextView) layout.getChildAt(1)).getText().toString();
                            }
                        }

                        if (itemText.equals(lastBrowserFocusText)) {
                            // 🚀 [원본 위치 100% 복원]
                            int offset = (sv.getHeight() / 2) - (v.getHeight() / 2);
                            if (exactOffsetMemory.containsKey(itemText))
                                offset = exactOffsetMemory.get(itemText);

                            int targetY = v.getTop() - offset;
                            if (targetY < 0)
                                targetY = 0;

                            sv.scrollTo(0, targetY);
                            v.requestFocus();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found && containerBrowserItems.getChildCount() > 0) {
                    containerBrowserItems.getChildAt(0).requestFocus();
                }
                lastBrowserFocusText = "";
            }
        });
    }

    // =======================================================
    // 🚀 [팟캐스트 엔진 1단계] 메모장 자동 생성 및 채널 해독기
    // =======================================================
    private void buildPodcastChannelsUI() {
        if (scrollViewBrowser != null)
            scrollViewBrowser.setVisibility(View.VISIBLE);
        if (listVirtualSongs != null)
            listVirtualSongs.setVisibility(View.GONE);
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText(t("Library") + ": " + t("Podcasts"));
        // =======================================================
        // 🚀 [새 팟캐스트 검색 버튼 (휠 키보드 호출 버전)]
        // =======================================================
        View btnSearch = createListButtonWithIcon("\uE8B6", t("Search New Podcasts"));
        btnSearch.setOnClickListener(v -> {
            clickFeedback();
            currentKeyboardMode = 1; // 🚀 팟캐스트 검색 모드로 키보드 장전!
            changeScreen(STATE_WIFI_KEYBOARD); // 와이파이 키보드 화면으로 즉시 이동!
        });
        containerBrowserItems.addView(btnSearch);

        View btnManage = createListButtonWithIcon("\uE872", t("Manage Subscriptions")); // 빨간색 휴지통
        btnManage.setOnClickListener(v -> {
            clickFeedback();
            buildPodcastManageUI(); // 대망의 전용 삭제 스튜디오 오픈!
        });
        containerBrowserItems.addView(btnManage);

        // =======================================================
        // 1. 팟캐스트 전용 폴더가 없으면 뚫어줍니다.
        File podcastDir = StoragePaths.getPodcastsDir();
        if (!podcastDir.exists())
            podcastDir.mkdirs();

        File subFile = new File(podcastDir, "subscriptions.txt");

        // 🚀 [신규 장착] 파일이 없으면? 글로벌 베스트셀러 팟캐스트 주소로 자동 생성해 버립니다!
        if (!subFile.exists()) {
            try {
                java.io.BufferedWriter bw = new java.io.BufferedWriter(
                        new java.io.OutputStreamWriter(new FileOutputStream(subFile), "UTF-8"));
                bw.write("# Y1 Podcast Subscriptions\n");
                bw.write("# " + t("Format: Channel Name|RSS_URL") + "\n\n");

                bw.close();
            } catch (Exception e) {
            }
        }

        List<String[]> channels = new ArrayList<>();

        // 2. 텍스트 파일을 0.01초 만에 훑어서 배열 바구니에 담습니다.
        if (subFile.exists()) {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(subFile), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#"))
                        continue;

                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        channels.add(new String[] { parts[0].trim(), parts[1].trim() });
                    } else {
                        channels.add(new String[] { "Unknown Channel", parts[0].trim() });
                    }
                }
                br.close();
            } catch (Exception e) {
            }
        }

        // 3. 화면에 버튼들(또는 텅 빈 안내문)을 뿌려줍니다!
        if (channels.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("⚠️ " + t("No subscriptions found.") + "\n\n" +
                    t("Create a 'subscriptions.txt' file in the Podcasts folder.") + "\n" +
                    t("Format: Channel Name|RSS_URL"));
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(20, 100, 20, 50);
            tvEmpty.setLineSpacing(15f, 1.2f);
            containerBrowserItems.addView(tvEmpty);
        } else {
            // ... (기존 subscriptions.txt 파일 읽어오는 부분은 그대로 유지) ...

            for (final String[] channel : channels) {
                View btnCh = createListButtonWithIcon("\uE03E", channel[0]);

                // 🚀 [해결책 2-1] 나중에 포커스를 찾을 수 있도록 버튼에 채널 이름표를 몰래 달아둡니다.
                btnCh.setTag(channel[0]);

                btnCh.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_PODCAST_EPISODES;
                    currentPodcastUrl = channel[1];

                    // 🚀 [해결책 2-2] 방금 누른 채널 이름을 기억 장부(lastBrowserFocusText)에 적어둡니다!
                    lastBrowserFocusText = channel[0];

                    buildPodcastEpisodesUI(channel[0], currentPodcastUrl);
                    Toast.makeText(MainActivity.this, t("Connecting to server: ") + channel[0], Toast.LENGTH_SHORT)
                            .show();
                });
                containerBrowserItems.addView(btnCh);
            }
        }

        // 🚀 [해결책 2-3] 화면 로딩이 끝난 후, 기억해둔 채널을 찾아 포커스를 정확히 꽂아줍니다!
        if (containerBrowserItems.getChildCount() > 0) {
            boolean focused = false;
            if (lastBrowserFocusText != null && !lastBrowserFocusText.isEmpty()) {
                // 컨테이너 안의 버튼들을 훑어서 방금 저장한 이름표(Tag)와 똑같은 녀석을 찾습니다.
                for (int i = 0; i < containerBrowserItems.getChildCount(); i++) {
                    View child = containerBrowserItems.getChildAt(i);
                    if (lastBrowserFocusText.equals(child.getTag())) {
                        child.requestFocus(); // 🎯 찾았다! 포커스 록온!
                        focused = true;
                        break;
                    }
                }
            }
            // 만약 처음 들어왔거나 기억된 채널이 없다면 맨 위(0번)를 포커스합니다.
            if (!focused) {
                containerBrowserItems.getChildAt(0).requestFocus();
            }
        }
    }

    // =======================================================
    // =======================================================
    // 🚀 [팟캐스트 전용] Y1 독자적 OkHttp 백그라운드 다운로드 엔진!
    // =======================================================
    public void startPodcastDownload(final String title, final String audioUrl, final String channelName) {
        Toast.makeText(this, "⬇️ " + t("Downloading: ") + title, Toast.LENGTH_SHORT).show();

        // 🚀 2. 다운로드 폴더도 채널별로 완벽 격리!
        String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
        final File podcastDir = StoragePaths.getPodcastChannelDir(safeChannel);
        if (!podcastDir.exists())
            podcastDir.mkdirs();
        final File destFile = new File(podcastDir, safeTitle);

        // 🚀 1. 메모장에 주소를 적고 다운로드 준비 상태(0%)로 만듭니다.
        activePodcastDownloads.put(audioUrl, -1L); // 자체 엔진이므로 ID 대신 -1 사용
        podcastDownloadProgress.put(audioUrl, 0);

        // 🚀 2. 다운로드 도중 화면이 꺼져 와이파이가 날아가는 것을 철통 방어합니다!
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (listVirtualSongs != null && listVirtualSongs.getAdapter() != null) {
            ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
        }

        // 🚀 3. 무적의 백그라운드 다운로드 스레드 가동!
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();

                    // 💡 [핵심] 서버를 완벽하게 속이는 프리패스 보안 캡슐 장착!
                    javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                            new javax.net.ssl.X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new java.security.cert.X509Certificate[] {};
                                }

                                public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                        String authType) {
                                }

                                public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                        String authType) {
                                }
                            }
                    };
                    javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS", "Conscrypt");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    builder.sslSocketFactory(sslContext.getSocketFactory(),
                            (javax.net.ssl.X509TrustManager) trustAllCerts[0]);

                    builder.hostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                            return true;
                        }
                    });

                    okhttp3.OkHttpClient client = builder.build();

                    // 💡 위장 신분증과 함께 '압축 해제(identity)' 명령을 내려 퍼센트 오류를 막습니다!
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(audioUrl)
                            .header("User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/114.0.0.0 Safari/537.36")
                            .header("Accept-Encoding", "identity")
                            .build();

                    okhttp3.Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new java.io.IOException("Server rejected connection");

                    java.io.InputStream is = response.body().byteStream();
                    long totalLength = response.body().contentLength();
                    FileOutputStream fos = new FileOutputStream(destFile);

                    byte[] buffer = new byte[8192];
                    int len;
                    long downloaded = 0;
                    int lastProgress = 0;

                    // 🚀 파일을 쓰면서 동시에 퍼센트(%)를 계산하여 리스트를 실시간으로 그립니다!
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        downloaded += len;

                        if (totalLength > 0) {
                            int progress = (int) ((downloaded * 100L) / totalLength);

                            // 1%가 오를 때마다 화면 갱신 지시
                            if (progress > lastProgress) {
                                lastProgress = progress;
                                podcastDownloadProgress.put(audioUrl, progress);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (currentBrowserMode == 14 && listVirtualSongs != null
                                                && listVirtualSongs.getAdapter() != null) {
                                            ((android.widget.BaseAdapter) listVirtualSongs.getAdapter())
                                                    .notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                        }
                    }
                    fos.flush();
                    fos.close();
                    is.close();

                    // 🚀 4. 다운로드 100% 완료 처리
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activePodcastDownloads.remove(audioUrl);
                            podcastDownloadProgress.remove(audioUrl);
                            Toast.makeText(MainActivity.this, "✅ " + t("Download Complete!"), Toast.LENGTH_SHORT)
                                    .show();

                            if (currentBrowserMode == 14 && listVirtualSongs != null
                                    && listVirtualSongs.getAdapter() != null) {
                                ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
                            }

                            // 모든 다운로드가 끝났다면 화면 꺼짐 방지(Wakelock) 해제!
                            if (activePodcastDownloads.isEmpty() && !isCustomScanning && !isRadioScanning
                                    && !isServerRunning) {
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }
                    });

                } catch (final Exception e) {
                    if (destFile.exists())
                        destFile.delete(); // 에러 발생 시 찌꺼기 파일 폭파!

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activePodcastDownloads.remove(audioUrl);
                            podcastDownloadProgress.remove(audioUrl);
                            Toast.makeText(MainActivity.this, "🚨 Download Failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            if (currentBrowserMode == 14 && listVirtualSongs != null
                                    && listVirtualSongs.getAdapter() != null) {
                                ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
                            }
                            if (activePodcastDownloads.isEmpty() && !isCustomScanning && !isRadioScanning
                                    && !isServerRunning) {
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }
                    });
                }
            }
        }).start();
    }
    // =======================================================
    // 🚀 [팟캐스트 삭제 엔진] 머티리얼 디자인 + 휠 조향 지원 럭셔리 팝업!
    // =======================================================
    public void showDeletePodcastDialog(final File fileToDelete, final String title) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        float d = getResources().getDisplayMetrics().density;

        // 1. 예쁜 라운딩과 반투명 배경 조립
        final LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0x88000000);
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (15 * d), (int) (20 * d), (int) (15 * d), (int) (15 * d));

        // 2. 제목 & 메시지 세팅
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🗑️ " + t("Delete Episode"));
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (15 * d));
        rootLayout.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(t("Do you want to delete this downloaded episode?") + "\n\n" + title);
        tvMsg.setTextColor(ThemeManager.getTextColorSecondary());
        tvMsg.setTextSize(15f);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, 0, 0, (int) (20 * d));
        rootLayout.addView(tvMsg);

        // 3. 🚀 [휠 조향 장치] 팝업 안에서 휠(위/아래) 완벽 지원!
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21 || keyCode == 19) { // UP
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx - 1; i >= 0; i--) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                    if (keyCode == 22 || keyCode == 20) { // DOWN
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx + 1; i < rootLayout.getChildCount(); i++) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        // 4. 삭제 버튼 (빨간색)
        View btnDelete = createListButtonWithIcon("\uE872", t("Delete"), 0xFFFF5555);
        btnDelete.setOnKeyListener(dialogWheelListener);
        btnDelete.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
            if (fileToDelete.exists()) {
                fileToDelete.delete(); // 💥 즉시 파쇄!
                Toast.makeText(MainActivity.this, t("Episode deleted."), Toast.LENGTH_SHORT).show();
                if (currentBrowserMode == BROWSER_PODCAST_EPISODES && listVirtualSongs != null && listVirtualSongs.getAdapter() != null) {
                    ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
                }
            }
        });
        rootLayout.addView(btnDelete);

        // 5. 취소 버튼
        View btnCancel = createListButtonWithIcon("\uE5CD", t("Cancel"), ThemeManager.getTextColorPrimary());
        btnCancel.setOnKeyListener(dialogWheelListener);
        btnCancel.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
        });
        rootLayout.addView(btnCancel);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        // 🚀 열리자마자 '취소' 버튼(안전빵)에 자동 포커스 록온!
        rootLayout.postDelayed(() -> {
            if (rootLayout.getChildCount() > 3) {
                rootLayout.getChildAt(3).requestFocus();
            }
        }, 50);
    }
    // =======================================================
    // 🚀 [스마트 팝업 엔진] 휠 조작이 완벽하게 지원되는 머티리얼 팝업!
    // =======================================================
    public void showPodcastActionDialog(final String title, final String audioUrl, final String imageUrl,
            final String channelName) {
        // 🚀 1. 채널 이름으로 고유 폴더 경로를 생성합니다!
        String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
        final File localFile = new File(StoragePaths.getPodcastChannelDir(safeChannel), safeTitle);

        // =======================================================
        // 🚀 [오프라인 클릭 방어막] 기기에 파일이 없는데 인터넷도 끊겨 있다면 아예 팝업을 열지 않고 경고 메시지를 띄웁니다!
        // =======================================================
        if (!localFile.exists() || localFile.length() == 0) {
            boolean isOnline = false;
            try {
                android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
                isOnline = netInfo != null && netInfo.isConnected();
            } catch(Exception e) {}

            if (!isOnline) {
                clickFeedback();
                Toast.makeText(this, "📡 " + t("Offline Mode: This episode has not been downloaded yet."), Toast.LENGTH_LONG).show();
                return; // 💡 팝업을 그리지 않고 함수 즉시 탈출!
            }
        }
        // 1. 투명한 배경의 껍데기 다이얼로그 생성
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        float d = getResources().getDisplayMetrics().density;

        // 2. 예쁜 모서리 라운딩과 테마 색상이 들어간 알맹이 박스 조립
        final LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0x88000000); // 메인 테마색 바탕
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF); // 은은한 테두리
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (15 * d), (int) (20 * d), (int) (15 * d), (int) (15 * d));

        // 3. 상단 팟캐스트 제목
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (20 * d));
        rootLayout.addView(tvTitle);

        // 🚀 [휠 조향 장치 추가] 팝업창 안에서 휠(21, 22)을 돌릴 때 위아래로 움직이게 만듭니다!
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21) { // 휠 위로 (UP)
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx - 1; i >= 0; i--) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                    if (keyCode == 22) { // 휠 아래로 (DOWN)
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx + 1; i < rootLayout.getChildCount(); i++) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        // ... (showPodcastActionDialog 내부) ...
        // 4. 기기에 파일이 이미 존재한다면? (다운로드 완료 상태)
        if (localFile.exists() && localFile.length() > 0) {
            View btnPlay = createListButtonWithIcon("\uE037", t("Play (Downloaded)"), 0xFF00FF00); // 초록색 재생
            btnPlay.setOnKeyListener(dialogWheelListener);
            btnPlay.setOnClickListener(v -> {
                clickFeedback();
                dialog.dismiss();

                // 🚀 [이어서 듣기] 금고에서 기록을 꺼내옵니다!
                String streamKey = "/PODCAST_STREAM/" + safeChannel + "/" + safeTitle;
                int savedPos = prefs.getInt("book_pos_" + localFile.getAbsolutePath(), 0);
                if (savedPos == 0)
                    savedPos = prefs.getInt("book_pos_" + streamKey, 0); // 스트리밍 기록 백업

                List<File> playList = new ArrayList<>();
                playList.add(localFile);

                // 💡 기록이 있으면 해당 시간으로 점프(Offset)하고, 없으면 처음(0)부터 재생합니다!
                if (savedPos > 0) {
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().playTrackListWithOffset(playList, 0,
                            savedPos);
                } else {
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().playTrackList(playList, 0);
                }
                changeScreen(3);
            });
            rootLayout.addView(btnPlay);
            // ... (삭제 버튼 코드는 동일) ...

        }

        // 5. 파일이 없다면? (스트리밍 or 다운로드 대기 상태)
        else {
            View btnStream = createListButtonWithIcon("\uE037", t("Stream (Play)"));
            btnStream.setOnKeyListener(dialogWheelListener);
            btnStream.setOnClickListener(v -> {
                clickFeedback();
                dialog.dismiss();
                Toast.makeText(MainActivity.this, t("Connecting to server: ") + "\n" + title, Toast.LENGTH_SHORT)
                        .show();

                // 🚀 [이어서 듣기] 스트리밍용 기록 꺼내오기!
                String streamKey = "/PODCAST_STREAM/" + safeChannel + "/" + safeTitle;
                int savedPos = prefs.getInt("book_pos_" + streamKey, 0);
                if (savedPos == 0)
                    savedPos = prefs.getInt("book_pos_" + localFile.getAbsolutePath(), 0);

                // =======================================================
                // 🚀 [에러 원인 완벽 제거]
                // 가짜 파일(fakeStreamFile) 꼼수를 지우고, 우리가 완벽하게 고쳐둔
                // 팟캐스트 전용 스트리밍 함수(playPodcastStream)를 직접 호출합니다!
                // =======================================================
                com.themoon.y1.managers.AudioPlayerManager.getInstance().playPodcastStream(audioUrl, title, imageUrl,
                        channelName, savedPos);

                changeScreen(3); // 플레이어 화면으로 이동!
            });
            rootLayout.addView(btnStream);

            // 🚀 다운로드 중인지 검사 (containsKey로 변경!)
            if (activePodcastDownloads.containsKey(audioUrl)) {
                View btnDownloading = createListButtonWithIcon("\uE863", t("Downloading..."), 0xFFFF8800); // 오렌지색
                btnDownloading.setOnKeyListener(dialogWheelListener);
                rootLayout.addView(btnDownloading);
            } else {
                View btnDown = createListButtonWithIcon("\uE2C4", t("Download"));
                btnDown.setOnKeyListener(dialogWheelListener);
                btnDown.setOnClickListener(v -> {
                    clickFeedback();
                    dialog.dismiss();
                    startPodcastDownload(title, audioUrl, channelName); // 🎯 채널 이름 함께 전달!
                });
                rootLayout.addView(btnDown);
            }
        }

        dialog.setContentView(rootLayout);

        // 🚀 팝업창 크기를 스마트하게 조절
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();

        // 🚀 [포커스 자동 록온] 팝업이 열리면 첫 번째 버튼(인덱스 1)에 자석처럼 휠 포커스 강제 고정!
        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (rootLayout.getChildCount() > 1) {
                    rootLayout.getChildAt(1).requestFocus();
                }
            }
        }, 50);
    }

    // =======================================================
    // 🚀 [팟캐스트 엔진 2단계] 인터넷 RSS 연결 및 에피소드 정밀 파서
    // =======================================================
    private void buildPodcastEpisodesUI(final String channelName, final String rssUrl) {
        scrollViewBrowser.setVisibility(View.GONE);
        listVirtualSongs.setVisibility(View.VISIBLE);
        tvBrowserPath.setText(t("Podcast") + ": " + channelName);
        // 🚀 [해결책 1] 새 데이터를 긁어오기 전에, 화면에 남아있는 이전 에피소드 잔상을 즉시 폭파시킵니다!
        virtualSongList.clear();
        currentScrollIndexList.clear();
        if (listVirtualSongs.getAdapter() != null) {
            listVirtualSongs.setAdapter(null); // 리스트뷰를 백지로 만듭니다.
        }
        // 1. 인터넷에서 긁어올 동안 사용자가 지루하지 않게 로딩 팝업을 켭니다.
        showLoadingPopup();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<SongItem> episodes = new ArrayList<>();
                try {
                    okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();

                    javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                            new javax.net.ssl.X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new java.security.cert.X509Certificate[] {};
                                }

                                public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                        String authType) {
                                }

                                public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                        String authType) {
                                }
                            }
                    };
                    javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS", "Conscrypt");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                    builder.sslSocketFactory(sslContext.getSocketFactory(),
                            (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
                    builder.hostnameVerifier((hostname, session) -> true);

                    // 🚀 [해결 1] 컬투쇼 같은 거대 XML을 버텨내도록 타임아웃을 60초로 대폭 늘립니다!
                    okhttp3.OkHttpClient client = builder
                            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                            .build();

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(rssUrl)
                            .header("User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/114.0.0.0 Safari/537.36")
                            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                            .build();

                    okhttp3.Response response = client.newCall(request).execute();
                    if (!response.isSuccessful())
                        throw new java.io.IOException("HTTP Error Code: " + response.code());

                    java.io.InputStream is = response.body().byteStream();
                    org.xmlpull.v1.XmlPullParserFactory factory = org.xmlpull.v1.XmlPullParserFactory.newInstance();
                    org.xmlpull.v1.XmlPullParser parser = factory.newPullParser();
                    parser.setInput(is, "UTF-8");

                    int eventType = parser.getEventType();
                    boolean insideItem = false;
                    String title = "";
                    String pubDate = "";
                    String audioUrl = "";
                    String imageUrl = "";
                    String channelImageUrl = ""; // 🚀 간판 이미지 저장용 변수 추가

                    while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT && episodes.size() < 300) {
                        String name = parser.getName();
                        if (name == null)
                            name = "";

                        switch (eventType) {
                            case org.xmlpull.v1.XmlPullParser.START_TAG:
                                if (name.equalsIgnoreCase("item"))
                                    insideItem = true;
                                else if (insideItem && name.equalsIgnoreCase("title"))
                                    title = parser.nextText();
                                else if (insideItem && name.equalsIgnoreCase("pubDate")) {
                                    pubDate = parser.nextText();
                                    if (pubDate.length() > 16)
                                        pubDate = pubDate.substring(0, 16);
                                } else if (insideItem && name.equalsIgnoreCase("enclosure"))
                                    audioUrl = parser.getAttributeValue(null, "url");
                                // 🚀 [간판 훔치기 1] itunes:image 태그일 때
                                else if (name.equalsIgnoreCase("itunes:image")) {
                                    String href = parser.getAttributeValue(null, "href");
                                    if (insideItem)
                                        imageUrl = href;
                                    else if (channelImageUrl.isEmpty() && href != null)
                                        channelImageUrl = href;
                                }
                                // 🚀 [간판 훔치기 2] 기본 image > url 태그일 때
                                else if (!insideItem && name.equalsIgnoreCase("url")) {
                                    try {
                                        String urlText = parser.nextText();
                                        if (channelImageUrl.isEmpty() && urlText.startsWith("http"))
                                            channelImageUrl = urlText;
                                    } catch (Exception e) {
                                    }
                                }
                                break;
                            case org.xmlpull.v1.XmlPullParser.END_TAG:
                                if (name.equalsIgnoreCase("item")) {
                                    if (audioUrl != null && !audioUrl.isEmpty()) {
                                        episodes.add(new SongItem(new File("/PODCAST"), title, channelName, imageUrl,
                                                pubDate, audioUrl));
                                    }
                                    insideItem = false;
                                    title = "";
                                    pubDate = "";
                                    audioUrl = "";
                                    imageUrl = "";
                                }
                                break;
                        }
                        eventType = parser.next();
                    }
                    is.close();

                    // =======================================================
                    // 🚀 [간판 자동 저장 엔진] 채널 간판 이미지를 찾았다면 cover.jpg로 조용히 다운로드!
                    // =======================================================
                    if (!channelImageUrl.isEmpty()) {
                        String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
                        File podcastDir = StoragePaths.getPodcastChannelDir(safeChannel);
                        if (!podcastDir.exists())
                            podcastDir.mkdirs();

                        File coverFile = new File(podcastDir, "cover.jpg");

                        // 💡 이미 cover.jpg가 있으면 데이터 절약을 위해 다운로드 생략
                        if (!coverFile.exists()) {
                            try {
                                okhttp3.Request imgReq = new okhttp3.Request.Builder().url(channelImageUrl).build();
                                okhttp3.Response imgRes = client.newCall(imgReq).execute();
                                if (imgRes.isSuccessful() && imgRes.body() != null) {
                                    java.io.InputStream imgIs = imgRes.body().byteStream();
                                    FileOutputStream imgFos = new FileOutputStream(coverFile);
                                    byte[] buf = new byte[8192];
                                    int len;
                                    while ((len = imgIs.read(buf)) != -1) {
                                        imgFos.write(buf, 0, len);
                                    }
                                    imgFos.flush();
                                    imgFos.close();
                                    imgIs.close();
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    // 🚀 [여기 신규 추가!] 파싱이 완료된 최신 에피소드 리스트를 오프라인용 캐시로 영구 저장합니다!
                    if (!episodes.isEmpty()) {
                        savePodcastCache(channelName, episodes);
                    }
                } catch (final Exception e) {
                    // runOnUiThread(() -> android.widget.Toast.makeText(MainActivity.this, "🚨 통신
                    // 에러: " + e.toString(), android.widget.Toast.LENGTH_LONG).show());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (layoutLoadingOverlay != null)
                            layoutLoadingOverlay.setVisibility(View.GONE);

                        // 🚀 [해결 3] 오프라인 생존 엔진
                        if (episodes.isEmpty()) {
                            // 🚀 [신규 장착 1] 인터넷이 끊기면 가장 먼저 '캐시된 에피소드 전체 명단'을 불러옵니다!
                            episodes.addAll(loadPodcastCache(channelName));

                            String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
                            File podcastDir = StoragePaths.getPodcastChannelDir(safeChannel);

                            // 🚀 [신규 장착 2] 만약 캐시 메모장조차 없다면(완전 최초 접속 실패 시),
                            // 최후의 수단으로 다운로드된 로컬 파일만이라도 긁어옵니다.
                            if (episodes.isEmpty() && podcastDir.exists()) {
                                File[] localFiles = podcastDir.listFiles();
                                if (localFiles != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd", Locale.US);
                                    for (File f : localFiles) {
                                        if (f.isFile() && f.getName().toLowerCase().endsWith(".mp3")) {
                                            String localTitle = f.getName().substring(0, f.getName().length() - 4);
                                            String offlineDate = sdf.format(new Date(f.lastModified()));
                                            episodes.add(new SongItem(f, localTitle, channelName, "", offlineDate, ""));
                                        }
                                    }
                                }
                            }

                            if (episodes.isEmpty()) {
                                Toast.makeText(MainActivity.this, "📡 " + t("No internet connection and no downloaded files."), Toast.LENGTH_LONG).show();
                            } else {
                                // 💡 캐시로 전체 리스트를 띄워줬다는 안내 멘트!
                                Toast.makeText(MainActivity.this, "📡 " + t("Offline Mode: Showing cached episode list."), Toast.LENGTH_LONG).show();
                            }
                        }

                        virtualSongList.clear();
                        currentScrollIndexList.clear();
                        for (SongItem ep : episodes) {
                            virtualSongList.add(ep.file);
                            currentScrollIndexList.add(ep.title);
                        }

                        SongListAdapter adapter = new SongListAdapter(episodes);
                        listVirtualSongs.setAdapter(adapter);
                        if (listVirtualSongs.getChildCount() > 0) {
                            listVirtualSongs.getChildAt(0).requestFocus();
                        }

                        // =======================================================
                        // 🚀 [버그 해결] 팟캐스트 화면 세팅이 끝나면 알파벳 퀵 바도 새로 조립!
                        // (이 함수 안에 퀵 바를 다시 GONE으로 숨겨주는 초기화 기능이 포함되어 있음)
                        // =======================================================
                        buildAlphabetIndexBar();
                    }
                });
            }
        }).start();
    }
    private void buildGameListUI() {
        tvBrowserPath.setText(t("Games"));
        
        containerBrowserItems.removeAllViews();
        
        String[] games = new String[]{"2048", "BrickBreaker", "FlappyBird", "FreeCell", "SpaceShooter"};
        for (final String game : games) {
            View btn = createListButtonWithIcon("\uE338", game);
            btn.setOnClickListener(v -> {
                clickFeedback();
                try {
                    Intent intent = new Intent(MainActivity.this, GameActivity.class);
                    intent.putExtra("game_url", "file:///android_asset/games/" + game + "/index.html");
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            containerBrowserItems.addView(btn);
        }
        
        containerBrowserItems.post(() -> {
            if (containerBrowserItems.getChildCount() > 0) {
                containerBrowserItems.getChildAt(0).requestFocus();
            }
        });
    }

    private void buildVirtualCategories(final String type) {
        if (isCustomScanning) {
            showLoadingPopup();
            currentBrowserMode = BROWSER_ROOT;
            buildFileBrowserUI();
            return;
        }

        scrollViewBrowser.setVisibility(View.GONE);
        listVirtualSongs.setVisibility(View.VISIBLE);

        // 🚀 우리가 새로 만든 '가수의 앨범' 모드인지 확인
       // boolean isArtistAlbum = type.equals("ARTIST_ALBUM");

        // 🚀 스위치 상태에 맞춰서 상단 타이틀 텍스트를 완벽하게 분기 처리합니다!
        if (type.equals("YEAR_ARTIST") || type.equals("GENRE_ARTIST")) {
            // "Artists" -> "Album Artists" 로 텍스트만 변경!
            tvBrowserPath.setText(t("Library") + ": " + yearOrGenreFilter + " - " + t("Album Artists"));
        } else if (type.equals("YEAR_ARTIST_ALBUM") || type.equals("GENRE_ARTIST_ALBUM") || type.equals("ARTIST_ALBUM")) {
            tvBrowserPath.setText(t("Library") + ": " + artistForAlbumFilter + " - " + t("Albums"));
        } else if (isAudiobookLibraryMode) {
            tvBrowserPath.setText(t("Library") + ": " + (type.equals("ARTIST") ? (isArtistAlbumMode ? t("Authors & Books") : t("Authors")) : t("Books")));
        } else {
            tvBrowserPath.setText(t("Library") + ": " + (type.equals("ARTIST") ? (isArtistAlbumMode ? t("Artists") : t("Artists")) : (type.equals("YEAR") ? t("Years") : (type.equals("GENRE") ? t("Genres") : t("Albums")))));
        }

        List<SongItem> activeLibrary = isAudiobookLibraryMode ? audiobookLibrary : customLibrary;
        HashSet<String> uniqueCategories = new HashSet<>();

        for (SongItem song : activeLibrary) {
            String val = "Unknown";
            if (type.equals("ARTIST")) val = song.artist;
            else if (type.equals("ALBUM")) val = song.album;
            else if (type.equals("YEAR")) val = song.year;
            else if (type.equals("GENRE")) val = song.genre;
            else if (type.equals("ARTIST_ALBUM")) {
                if (song.artist.equals(artistForAlbumFilter)) val = song.album; else continue;
            }
            // 🚀 [신규 장착] 연도/장르 필터링 로직 추가!
            else if (type.equals("YEAR_ARTIST")) {
                if (song.year.equals(yearOrGenreFilter)) val = song.artist; else continue;
            } else if (type.equals("GENRE_ARTIST")) {
                if (song.genre.equals(yearOrGenreFilter)) val = song.artist; else continue;
            } else if (type.equals("YEAR_ARTIST_ALBUM")) {
                if (song.year.equals(yearOrGenreFilter) && song.artist.equals(artistForAlbumFilter)) val = song.album; else continue;
            } else if (type.equals("GENRE_ARTIST_ALBUM")) {
                if (song.genre.equals(yearOrGenreFilter) && song.artist.equals(artistForAlbumFilter)) val = song.album; else continue;
            }
            uniqueCategories.add(val);
        }

        List<String> categories = new ArrayList<>(uniqueCategories);
        java.util.Collections.sort(categories, String.CASE_INSENSITIVE_ORDER);

        currentScrollIndexList.clear();
        currentScrollIndexList.addAll(categories);

        /// 🚀 [마술 트릭 해제] 어댑터에게 더 이상 "ALBUM"이라고 속이지 않고 정직하게 원래 type을 넘깁니다!
        String adapterType = type; // (기존: isArtistAlbum ? "ALBUM" : type; 에서 원상복구!)
        CategoryListAdapter adapter = new CategoryListAdapter(categories, adapterType);
        listVirtualSongs.setAdapter(adapter);

        final int targetIndex = categories.indexOf(virtualQueryValue);

        listVirtualSongs.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (targetIndex >= 0) {
                    // =========================================================
                    // 🚀 [스크롤 복구 수리 완료]
                    // 무조건 0(맨 위)으로 고정하지 않고, 금고에 저장해 둔 원본 Y좌표를 꺼내서 완벽하게 복원합니다!
                    // =========================================================
                    int offset = 0;
                    if (exactOffsetMemory.containsKey(virtualQueryValue)) {
                        offset = exactOffsetMemory.get(virtualQueryValue);
                    }

                    listVirtualSongs.setSelectionFromTop(targetIndex, offset);
                    // =========================================================

                    listVirtualSongs.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int visiblePos = targetIndex - listVirtualSongs.getFirstVisiblePosition();
                            if (visiblePos >= 0 && visiblePos < listVirtualSongs.getChildCount()) {
                                listVirtualSongs.getChildAt(visiblePos).requestFocus();
                            }
                        }
                    }, 50);
                } else if (listVirtualSongs.getChildCount() > 0) {
                    listVirtualSongs.getChildAt(0).requestFocus();
                }
            }
        }, 50);
        buildAlphabetIndexBar();
    }
    // 💡 [지능형 다국어 엔진] 영어는 A~Z, 한글은 초성(ㄱ~ㅎ) 추출, 나머지는 전부 '#'으로 묶어버립니다!
    private char getInitialChar(String text) {
        if (text == null || text.isEmpty())
            return '#';

        // 이모지 및 불필요한 공백 제거
        String clean = text.replace("📁 ", "").replace("👤 ", "")
                .replace("💿 ", "").replace("🎵 ", "").trim().toUpperCase();
        if (clean.isEmpty())
            return '#';

        char c = clean.charAt(0);

        // 1. 영어인 경우 (A ~ Z)
        if (c >= 'A' && c <= 'Z') {
            return c;
        }

        // 2. 한글인 경우 (가 ~ 힣) -> 초성(ㄱ~ㅎ)으로 완벽하게 변환!
        if (c >= 0xAC00 && c <= 0xD7A3) {
            int base = c - 0xAC00;
            int initialIdx = base / (21 * 28); // 초성 인덱스 공식 계산

            // 쌍자음(ㄲ, ㄸ, ㅃ, ㅆ, ㅉ)은 기본 자음(ㄱ, ㄷ, ㅂ, ㅅ, ㅈ)으로 통합하여 깔끔하게 묶어줍니다!
            char[] initials = { 'ㄱ', 'ㄱ', 'ㄴ', 'ㄷ', 'ㄷ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅂ', 'ㅅ', 'ㅅ', 'ㅇ', 'ㅈ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ',
                    'ㅍ', 'ㅎ' };
            return initials[initialIdx];
        }

        // 3. 숫자, 특수문자, 일본어, 중국어 등 그 외 모든 문자는 찌꺼기가 남지 않게 '#' 방으로 보냅니다!
        return '#';
    }

    // 🚀 [신규 추가] 딜레이 제로! 초고속 앨범 아트 RAM 캐시 메모리
    private LruCache<String, Bitmap> albumArtCache;

    // 🚀 [최종 대개조] 음악과 오디오북 라이브러리를 하나로 통합하여 모든 앨범을 동시 출력하는 3D 엔진
    private void buildCoverFlowUI() {
        currentBrowserMode = BROWSER_COVER_FLOW;
        View statusBar = findViewById(R.id.layout_status_bar);
        if (statusBar != null)
            statusBar.setBackgroundColor(0x00000000);
        // 1. 기존 잔상 및 스크롤뷰 초기화
        if (scrollViewBrowser != null) {
            scrollViewBrowser.setVisibility(View.VISIBLE);
            if (scrollViewBrowser instanceof ScrollView) {
                ((ScrollView) scrollViewBrowser).scrollTo(0, 0);
            }
        }
        if (listVirtualSongs != null)
            listVirtualSongs.setVisibility(View.GONE);
        containerBrowserItems.removeAllViews();

        uniqueAlbumList.clear();

        // 🚀 [핵심 수정] 뮤직과 오디오북 바구니의 데이터 소스를 하나의 거대한 통합 주머니로 합쳐버립니다!
        List<SongItem> combinedLibrary = new ArrayList<>();
        combinedLibrary.addAll(customLibrary);
        combinedLibrary.addAll(audiobookLibrary);

        HashSet<String> checkedAlbums = new HashSet<>();

        // 3. 통합 주머니에서 중복 없이 온전한 데이터 수집
        for (SongItem song : combinedLibrary) {
            // 통합 모드이므로 /Music 폴더 계열과 /Audiobooks 폴더 계열을 모두 프리패스로 허용합니다.
            boolean isPathMatched = song.file.getAbsolutePath().contains("/Music")
                    || song.file.getAbsolutePath().contains("/Audiobooks");

            if (isPathMatched) {
                // 💡 [태그 복구 엔진] 앨범 이름이 없으면 음원이 든 폴더 이름으로 강제 변환!

                // 🚀 [중복 앨범 파쇄기] 아티스트 이름(피처링)이 다르더라도, 같은 '폴더' 안의 같은 '앨범 이름'이라면 무조건 1장의 카드로
                // 묶어버립니다!
                String key = song.file.getParentFile().getAbsolutePath() + " - " + song.album;

                if (!checkedAlbums.contains(key)) {
                    checkedAlbums.add(key);
                    uniqueAlbumList.add(song);
                }
            }
        }

        // =========================================================
        // 🚀 [커버 플로우 정렬 알고리즘 업그레이드!]
        // 4. '앨범 아티스트'를 기준으로 1차 정렬하여 같은 가수의 앨범들을 하나로 예쁘게 묶어줍니다!
        //    만약 같은 가수의 앨범이 여러 개라면, 그 안에서 '앨범 이름'으로 2차 정렬합니다.
        // =========================================================
        java.util.Collections.sort(uniqueAlbumList, new java.util.Comparator<SongItem>() {
            @Override
            public int compare(SongItem s1, SongItem s2) {
                // 1차: 아티스트 이름 비교
                int artistCompare = s1.artist.compareToIgnoreCase(s2.artist);

                // 아티스트가 다르면 아티스트 기준으로 줄 세우기!
                if (artistCompare != 0) {
                    return artistCompare;
                }

                // 2차: 아티스트가 똑같다면 앨범 이름 기준으로 줄 세우기!
                return s1.album.compareToIgnoreCase(s2.album);
            }
        });
        // =========================================================

        // 5. 해당 라이브러리에 앨범이 하나도 없다면 빈 화면 안내 메시지를 출력하고 탈출
        if (uniqueAlbumList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(t("No Albums Found"));
            tvEmpty.setTextSize(18f);
            tvEmpty.setTextColor(ThemeManager.getTextColorSecondary());
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(0, 50, 0, 0);
            containerBrowserItems.addView(tvEmpty);
            return;
        }

        // 🚀 [커버 플로우 포커스 록온]
        // 앨범 안쪽 곡 목록(COVER_FLOW_ALBUM)에서 뒤로 가기를 눌러 복귀한 경우인지 지능적으로 판별합니다.
        if ("COVER_FLOW_ALBUM".equals(virtualQueryType) && virtualQueryValue != null && !virtualQueryValue.isEmpty()) {
            int foundIdx = -1;
            // 정렬이 완료된 전체 명단에서 방금 보고 나왔던 앨범 이름과 똑같은 위치를 추적합니다!
            for (int i = 0; i < uniqueAlbumList.size(); i++) {
                if (uniqueAlbumList.get(i).album.equals(virtualQueryValue)) {
                    foundIdx = i;
                    break;
                }
            }
            // 일치하는 위치를 찾았다면 커버 플로우 바늘을 해당 인덱스로 즉시 동기화 고정합니다!
            if (foundIdx != -1) {
                currentCoverFlowIndex = foundIdx;
            } else {
                currentCoverFlowIndex = 0;
            }

            // 💡 다음번 일반 메뉴나 숏컷을 통해 커버 플로우에 완전 '처음' 진입할 때는
            // 0번부터 깔끔하게 열리도록 1회성 기억 변수를 사용 후 깨끗하게 비워줍니다.
            virtualQueryType = "";
            virtualQueryValue = "";
        } else {
            // 일반적인 최초 진입 시에는 원래 설계대로 0번 인덱스 장전
            currentCoverFlowIndex = 0;
        }

        // 6. 아티스트님의 '순정 고정형 배열 엔진(cfViews)'을 가동합니다.
        coverFlowContainer = new FrameLayout(this);
        int containerHeight = (int) (380 * getResources().getDisplayMetrics().density);
        containerBrowserItems.addView(coverFlowContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, containerHeight));

        cfViews = new View[visibleCoversCount];
        for (int i = 0; i < visibleCoversCount; i++) {
            cfViews[i] = createSingleCoverView(); // 💡 아래 개조된 카드 생성기 호출
            coverFlowContainer.addView(cfViews[i]);
        }

        // 7. 모든 카드가 배치되면, 기존에 있던 순정 알고리즘 엔진을 깨워 좌표와 각도를 잡아줍니다.
        initCoverFlowPositions();
    }

    // 🚀 [순정 3D 엔진 4] 초기 포지션 세팅 (알고리즘화 완료)
    private void initCoverFlowPositions() {
        int total = uniqueAlbumList.size();
        if (total == 0)
            return;

        int centerIdx = visibleCoversCount / 2;

        // 정중앙을 기준으로 앞뒤 인덱스를 계산하여 데이터 바인딩
        for (int i = 0; i < visibleCoversCount; i++) {
            int offsetFromCenter = i - centerIdx;
            int targetIdx = (currentCoverFlowIndex + offsetFromCenter + total * 3) % total;
            bindCoverData(cfViews[i], targetIdx);
        }

        float d = getResources().getDisplayMetrics().density;

        // 🚀 수학적 알고리즘 루프: 뷰의 개수에 상관없이 스케일/좌표 공식 자동 매핑
        for (int i = 0; i < visibleCoversCount; i++) {
            int dist = Math.abs(i - centerIdx);
            float sign = (i < centerIdx) ? -1f : 1f; // 왼쪽은 음수(-), 오른쪽은 양수(+)

            float transX = sign * getTransXForDist(dist, d);
            float rotY = -sign * getRotYForDist(dist);
            float scale = getScaleForDist(dist);
            float alpha = getAlphaForDist(dist);

            applyTransform(cfViews[i], transX, rotY, scale, alpha);
        }

        arrangeZIndex();

        for (int i = 0; i < visibleCoversCount; i++) {
            setCardTitleAlpha(cfViews[i], i == centerIdx, 0);
        }

        tvBrowserPath.setText(t("Cover Flow") + " (" + (currentCoverFlowIndex + 1) + "/" + total + ")");
    }
    // 🚀 [알고리즘 엔진] 중앙에서부터의 거리에 따른 X축 이동 거리 계산
    // private float getTransXForDist(int dist, float d) {
    // if (dist == 0) return 0f;
    // if (dist == 1) return 110 * d;
    // if (dist == 2) return 180 * d;
    // return 230 * d; // 거리가 3 이상일 때
    // }

    private float getTransXForDist(int dist, float d) {
        if (dist == 0)
            return 0f;
        if (dist == 1)
            return 130 * d;
        if (dist == 2)
            return 170 * d;
        return 220 * d; // 거리가 3 이상일 때
    }

    // 🚀 숫자를 높일수록 책장에 책을 비스듬히 꽂아둔 것처럼 각도가 팍 꺾입니다!
    // private float getRotYForDist(int dist) {
    // if (dist == 0) return 0f;
    // if (dist == 1) return 60f; // 💡 45도 -> 60도로 더 깊게 꺾기!
    // if (dist == 2) return 75f; // 💡 60도 -> 75도로 더 깊게 꺾기!
    // return 80f;
    // }
    private float getRotYForDist(int dist) {
        if (dist == 0)
            return 0f;
        if (dist == 1)
            return 65f; // 💡 45도 -> 60도로 더 깊게 꺾기!
        // if (dist == 2) return 75f; // 💡 60도 -> 75도로 더 깊게 꺾기!
        return 65f;
    }

    // 🚀 [알고리즘 엔진] 중앙에서부터의 거리에 따른 크기 축소 비율 계산
    private float getScaleForDist(int dist) {
        if (dist == 0)
            return 1.0f;
        if (dist == 1)
            return 0.8f;
        if (dist == 2)
            return 0.8f;
        return 0.8f;
    }

    // 🚀 [알고리즘 엔진] 중앙에서부터의 거리에 따른 투명도 계산
    // private float getAlphaForDist(int dist) {
    // if (dist == 0) return 1.0f;
    // if (dist == 1) return 0.8f;
    // if (dist == 2) return 0.5f;
    // return 0.1f;
    // }
    private float getAlphaForDist(int dist) {
        // if (dist == 0) return 1.0f;
        // if (dist == 1) return 0.8f;
        // if (dist == 2) return 0.5f;
        return 1f;
    }

    // 🚀 [순정 3D 엔진 3] 데이터 바인딩 및 캐시 폴더 강제 입체 역추적 엔진 장착!
    private void bindCoverData(View card, int dataIndex) {
        if (uniqueAlbumList.isEmpty() || dataIndex < 0 || dataIndex >= uniqueAlbumList.size())
            return;
        SongItem item = uniqueAlbumList.get(dataIndex);

        final ImageView ivCover = card.findViewById(1001);
        final ImageView ivReflection = card.findViewById(1004); // 🚀 반사판 레이어 획득
        TextView tvTitle = card.findViewById(1002);
        TextView tvArtist = card.findViewById(1003);

        tvTitle.setText(item.album);
        tvArtist.setText(item.artist);

        final String path = item.file.getAbsolutePath();
        ivCover.setTag(path); // 비동기 꼬임 완벽 차단

        // 1. 초고속 RAM 캐시 금고 수색 (원본 이미지와 반사판 세트 동시 수색)
        Bitmap cachedBmp = null;
        Bitmap cachedRef = null;
        if (albumArtCache != null) {
            cachedBmp = albumArtCache.get(path);
            cachedRef = albumArtCache.get("ref_" + path);
        }

        if (cachedBmp != null) {
            // 💡 램 금고에 둘 다 있다면? 즉시 0.0001초 만에 애니메이션 없이 즉각 바인딩!
            ivCover.animate().cancel(); // 🚀 이전 애니메이션 잔상 파쇄
            ivCover.setAlpha(1.0f);
            ivCover.setImageBitmap(cachedBmp);
            if (ivReflection != null) {
                ivReflection.animate().cancel();
                ivReflection.setAlpha(1.0f);
                ivReflection.setImageBitmap(cachedRef);
                ivReflection.setVisibility(cachedRef != null ? View.VISIBLE : View.INVISIBLE);
            }
            return;
        }

        // 2. 캐시에 없으면 기본 빈 도화지를 바인딩하고 일꾼(Thread) 발사
        ivCover.animate().cancel(); // 🚀 이전 애니메이션 잔상 파쇄
        ivCover.setAlpha(1.0f);
        ivCover.setImageBitmap(
                ThemeManager.getCustomIcon("icon_default_album.png", MainActivity.this, R.drawable.default_album));
        if (ivReflection != null) {
            ivReflection.animate().cancel();
            ivReflection.setAlpha(1.0f);
            ivReflection.setImageBitmap(null);
        }

        // 3. 백그라운드 로딩 엔진 발사
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
                        File fallbackFile = new File(StoragePaths.getCoversDir(), songName + ".jpg");
                        if (fallbackFile.exists()) {
                            bmp = decodeSampledBitmap(fallbackFile.getAbsolutePath(), 400, 400);
                        } else {
                            // 🚀 [신규 장착!] 2순위: 혹시 같은 폴더 안에 cover.jpg 가 있는지 탐색기 가동!
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
                }

                if (bmp == null) {
                    try {
                        byte[] embeddedArt = null;

                        if (path.toLowerCase().endsWith(".opus")) {
                            Object[] opusTags = com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .extractOpusMetadata(new File(path));
                            if (opusTags[5] != null)
                                embeddedArt = (byte[]) opusTags[5];
                        } else if (path.toLowerCase().endsWith(".flac")) {
                            Object[] flacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .extractFlacMetadata(new File(path));
                            // 🚨 배열 방 번호를 2에서 5로 변경!
                            if (flacTags[5] != null)
                                embeddedArt = (byte[]) flacTags[5];
                        } else {
                            // MP3, WAV 등 순정 부품 사용
                            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                            mmr.setDataSource(path);
                            embeddedArt = mmr.getEmbeddedPicture();
                            mmr.release();
                        }

                        // 🚀 빼온 사진 데이터(Byte)를 예쁜 비트맵(Bitmap)으로 구워냅니다.
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
                    }
                }

                final Bitmap finalBmp = bmp;

                // 메인 스레드가 아닌, 이 백그라운드 공간에서 반사판을 생성하므로 성능 과부하가 0%입니다!
                Bitmap tempRef = null;
                try {
                    tempRef = getReflectionBitmap(finalBmp);
                } catch (OutOfMemoryError e) {
                    tempRef = null;
                }
                final Bitmap finalRef = tempRef;

                // 다음번 조회를 위해 원본과 반사 이미지 나란히 RAM 금고에 입고
                if (finalBmp != null && albumArtCache != null) {
                    albumArtCache.put(path, finalBmp);
                    if (finalRef != null) {
                        albumArtCache.put("ref_" + path, finalRef);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 🚀 4. 휠 회전 중 다른 곡으로 타겟이 바뀌지 않았을 때만 화면에 렌더링!
                        if (path.equals(ivCover.getTag())) {
                            if (finalBmp != null) {
                                ivCover.setAlpha(0f); // 💡 투명 상태에서 시작
                                ivCover.setImageBitmap(finalBmp);
                                ivCover.animate().alpha(1.0f).setDuration(300).start(); // 🚀 0.3초 동안 스르륵! 순차적 페이드인 연출
                            }
                            if (ivReflection != null && finalRef != null) {
                                ivReflection.setAlpha(0f);
                                ivReflection.setImageBitmap(finalRef);
                                ivReflection.setVisibility(View.VISIBLE);
                                ivReflection.animate().alpha(1.0f).setDuration(300).start(); // 🚀 반사판도 동시 페이드인!
                            }
                        }
                    }
                });
            }
        });
    }

    // 🚀 [버그 완전 처치 & 비율 최적화] 커버 이미지 잘림을 막으면서도 전체를 위로 올리고, 텍스트 가독성을 극대화합니다!
    private View createSingleCoverView() {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                (int) (350 * d), (int) (320 * d));
        lp.gravity = Gravity.CENTER;

        // 🚀 [디테일 1] 카드 전체를 위로 살짝(-20dp) 끌어올려 화면 중앙보다 약간 높은 황금 비율에 안착시킵니다.
        lp.topMargin = (int) (-20 * d);

        card.setLayoutParams(lp);

        ImageView ivCover = new ImageView(this);
        ivCover.setId(1001);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams((int) (200 * d),
                (int) (200 * d));
        imgLp.gravity = Gravity.CENTER_HORIZONTAL;
        ivCover.setLayoutParams(imgLp);
        ivCover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivCover.setBackground(createButtonBackground(0x00000000));

        ImageView ivReflection = new ImageView(this);
        ivReflection.setId(1004);
        LinearLayout.LayoutParams refLp = new LinearLayout.LayoutParams((int) (200 * d),
                (int) (50 * d));
        refLp.gravity = Gravity.CENTER_HORIZONTAL;
        refLp.topMargin = (int) (2 * d);
        ivReflection.setLayoutParams(refLp);
        ivReflection.setScaleType(ImageView.ScaleType.CENTER_CROP);

        TextView tvTitle = new TextView(this);
        tvTitle.setId(1002);
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(18f); // 앨범 제목 크기 유지
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setSingleLine(true);
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvTitle.setAlpha(0f);

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // 🚀 [디테일 2] 앨범 제목을 위쪽(반사판 쪽)으로 훨씬 더 가깝게(-55dp) 바짝 끌어올립니다.
        titleLp.topMargin = (int) (-40 * d);
        titleLp.bottomMargin = (int) (0 * d);
        tvTitle.setLayoutParams(titleLp);

        TextView tvArtist = new TextView(this);
        tvArtist.setId(1003);
        tvArtist.setTextColor(ThemeManager.getTextColorSecondary());

        // 🚀 [디테일 3] 너무 작았던 가수 이름 글씨를 14f에서 16.5f로 시원하게 키웁니다!
        tvArtist.setTextSize(16.5f);
        tvArtist.setGravity(Gravity.CENTER);
        tvArtist.setSingleLine(true);
        tvArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvArtist.setAlpha(0f);

        LinearLayout.LayoutParams artistLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        artistLp.topMargin = (int) (2 * d);
        tvArtist.setLayoutParams(artistLp);

        card.addView(ivCover);
        card.addView(ivReflection);
        card.addView(tvTitle);
        card.addView(tvArtist);

        card.setOnClickListener(v -> {
            int centerIdx = visibleCoversCount / 2;
            if (v == cfViews[centerIdx] && currentCoverFlowIndex >= 0
                    && currentCoverFlowIndex < uniqueAlbumList.size()) {
                clickFeedback();
                SongItem chosen = uniqueAlbumList.get(currentCoverFlowIndex);

                // 🚀 [지능형 라이브러리 모드 전환 스위치]
                // 통합 커버플로우에서 앨범을 눌러 진입하는 순간, 선택한 곡의 파일 절대 경로를 실시간으로 분석합니다.
                // 오디오북 폴더(/Audiobooks) 소속이면 오디오북 모드를 키고, 음악 폴더 소속이면 음악 모드로 알아서 스위칭해줍니다!
                if (chosen.file.getAbsolutePath().contains("/Audiobooks")) {
                    isAudiobookLibraryMode = true;
                } else {
                    isAudiobookLibraryMode = false;
                }
                currentBrowserMode = BROWSER_VIRTUAL_SONGS;
                virtualQueryType = "COVER_FLOW_ALBUM";
                virtualQueryValue = chosen.album;
                buildVirtualSongs();
            }
        });

        return card;
    }

    // 🚀 [반사판 그래픽 파서] 원본을 상하 반전 후 그라데이션 마스크를 씌워 젤리빈 순정으로 렌더링합니다.
    private Bitmap getReflectionBitmap(Bitmap src) {
        if (src == null)
            return null;
        try {
            int w = src.getWidth();
            int h = src.getHeight();
            int reqH = h / 4; // 원본의 하단 25% 구역만 반사 영역으로 사용
            if (reqH <= 0)
                return null;

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.preScale(1, -1); // 상하 반전 행렬 주입

            // 하단 부분만 싹둑 잘라 뒤집은 비트맵 생성
            Bitmap flipped = Bitmap.createBitmap(src, 0, h - reqH, w, reqH, matrix,
                    false);
            Bitmap reflection = Bitmap.createBitmap(w, reqH,
                    Bitmap.Config.ARGB_8888);

            android.graphics.Canvas canvas = new android.graphics.Canvas(reflection);
            canvas.drawBitmap(flipped, 0, 0, null);
            flipped.recycle();

            // 밑으로 갈수록 스르륵 사라지는 그라데이션 마스크 도색
            android.graphics.Paint paint = new android.graphics.Paint();
            android.graphics.LinearGradient shader = new android.graphics.LinearGradient(
                    0, 0, 0, reqH,
                    0x44FFFFFF, 0x00FFFFFF, // 약 25%의 은은한 반사 시작 투명도 -> 0% 완전 투명
                    android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
            canvas.drawRect(0, 0, w, reqH, paint);

            return reflection;
        } catch (Exception e) {
            return null;
        }
    }

    // 🚀 [도우미 함수 4] 중앙에 온 카드만 제목을 보여주고, 옆으로 밀려난 카드는 제목을 숨깁니다!
    private void setCardTitleAlpha(View card, boolean isCenter, int duration) {
        View tvTitle = card.findViewById(1002);
        View tvArtist = card.findViewById(1003);
        if (tvTitle != null && tvArtist != null) {
            float targetAlpha = isCenter ? 1.0f : 0.0f; // 중앙이면 100% 켜고, 아니면 0% 끄기
            if (duration > 0) {
                tvTitle.animate().alpha(targetAlpha).setDuration(duration).start();
                tvArtist.animate().alpha(targetAlpha).setDuration(duration).start();
            } else {
                tvTitle.setAlpha(targetAlpha);
                tvArtist.setAlpha(targetAlpha);
            }
        }
    }

    // 🚀 [도우미 함수 1] 뷰를 순간 이동 및 변형시키는 함수
    private void applyTransform(View v, float transX, float rotY, float scale, float alpha) {
        v.setTranslationX(transX);
        v.setRotationY(rotY);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setAlpha(alpha);
    }

    private void animateTransform(View v, float transX, float rotY, float scale, float alpha, int duration) {
        v.animate().translationX(transX).rotationY(rotY).scaleX(scale).scaleY(scale).alpha(alpha).setDuration(duration)
                .start();
    }

    // 🚀 [뎁스 엔진] 설정된 개수에 맞추어 최외각 카드부터 정중앙 카드까지 순서대로 쌓아 올립니다.
    private void arrangeZIndex() {
        int centerIdx = visibleCoversCount / 2;

        // 가장 먼 거리부터 정중앙(0)까지 역순으로 앞면 배치(bringToFront) 처리
        for (int d = centerIdx; d >= 0; d--) {
            int leftViewIdx = centerIdx - d;
            int rightViewIdx = centerIdx + d;

            if (leftViewIdx >= 0)
                cfViews[leftViewIdx].bringToFront();
            if (rightViewIdx < visibleCoversCount)
                cfViews[rightViewIdx].bringToFront();
        }

        for (int i = 0; i < visibleCoversCount; i++)
            cfViews[i].invalidate();
        coverFlowContainer.invalidate();
    }

    private long lastCoverFlowTime = 0; // 🚀 스마트 변속용 타임머신 변수

    // 🚀 [순정 3D 엔진 5] 초고속 슬라이딩 엔진 (개수 가변형 연산 기하학 완비)
    private void scrollCoverFlow(boolean isNext) {
        int total = uniqueAlbumList.size();
        if (total == 0)
            return;

        float d = getResources().getDisplayMetrics().density;
        int centerIdx = visibleCoversCount / 2;

        long now = System.currentTimeMillis();
        long diff = now - lastCoverFlowTime;
        lastCoverFlowTime = now;
        int duration = (diff < 80) ? 30 : 180;

        if (isNext) {
            currentCoverFlowIndex = (currentCoverFlowIndex + 1) % total;
            View oldLeft = cfViews[0];

            // 🚀 동적 인덱스 셔플링
            for (int i = 0; i < visibleCoversCount - 1; i++)
                cfViews[i] = cfViews[i + 1];
            cfViews[visibleCoversCount - 1] = oldLeft;

            bindCoverData(cfViews[visibleCoversCount - 1], (currentCoverFlowIndex + centerIdx + total * 3) % total);

            float maxOff = getTransXForDist(centerIdx, d);
            float maxRot = getRotYForDist(centerIdx);
            float maxScale = getScaleForDist(centerIdx);
            applyTransform(cfViews[visibleCoversCount - 1], maxOff * 1.5f, -maxRot, maxScale, 0f);
        } else {
            currentCoverFlowIndex = (currentCoverFlowIndex - 1 + total) % total;
            View oldRight = cfViews[visibleCoversCount - 1];

            for (int i = visibleCoversCount - 1; i > 0; i--)
                cfViews[i] = cfViews[i - 1];
            cfViews[0] = oldRight;

            bindCoverData(cfViews[0], (currentCoverFlowIndex - centerIdx + total * 3) % total);

            float maxOff = getTransXForDist(centerIdx, d);
            float maxRot = getRotYForDist(centerIdx);
            float maxScale = getScaleForDist(centerIdx);
            applyTransform(cfViews[0], -maxOff * 1.5f, maxRot, maxScale, 0f);
        }

        arrangeZIndex();

        // 🚀 전체 동적 슬롯 애니메이션 폭격 루터 가동!
        for (int i = 0; i < visibleCoversCount; i++) {
            setCardTitleAlpha(cfViews[i], i == centerIdx, duration);

            int dist = Math.abs(i - centerIdx);
            float sign = (i < centerIdx) ? -1f : 1f;

            float transX = sign * getTransXForDist(dist, d);
            float rotY = -sign * getRotYForDist(dist);
            float scale = getScaleForDist(dist);
            float alpha = getAlphaForDist(dist);

            animateTransform(cfViews[i], transX, rotY, scale, alpha, duration);
        }

        tvBrowserPath.setText(t("Cover Flow") + " (" + (currentCoverFlowIndex + 1) + "/" + total + ")");
    }

    // 💡 4. 자체 DB에서 노래를 뽑아 '재활용 엔진'에 밀어넣는 함수 (뮤직/오디오북 완벽 격리 버전!)
    public void buildVirtualSongs() {
        // =========================================================
        // 🚀 [우회 라우터 엔진] 연도/장르 클릭 시 노래가 아닌 가수/앨범 목록으로 납치합니다!
        // =========================================================
        if ("YEAR".equals(virtualQueryType)) {
            yearOrGenreFilter = virtualQueryValue;
            currentBrowserMode = BROWSER_YEAR_ARTISTS;
            virtualQueryValue = "";
            buildVirtualCategories("YEAR_ARTIST");
            return;
        }
        if ("GENRE".equals(virtualQueryType)) {
            yearOrGenreFilter = virtualQueryValue;
            currentBrowserMode = BROWSER_GENRE_ARTISTS;
            virtualQueryValue = "";
            buildVirtualCategories("GENRE_ARTIST");
            return;
        }
        if ("YEAR_ARTIST".equals(virtualQueryType)) {
            artistForAlbumFilter = virtualQueryValue;
            currentBrowserMode = BROWSER_YEAR_ALBUMS;
            virtualQueryValue = "";
            buildVirtualCategories("YEAR_ARTIST_ALBUM");
            return;
        }
        if ("GENRE_ARTIST".equals(virtualQueryType)) {
            artistForAlbumFilter = virtualQueryValue;
            currentBrowserMode = BROWSER_GENRE_ALBUMS;
            virtualQueryValue = "";
            buildVirtualCategories("GENRE_ARTIST_ALBUM");
            return;
        }
        // 🚀 [가수 -> 앨범 직결 엔진]
        // 사용자가 가수를 선택(ARTIST)했다면, 노래 목록 대신 '그 가수의 앨범 목록'을 먼저 띄워줍니다!
        if ("ARTIST".equals(virtualQueryType) && isArtistAlbumMode) {
            artistForAlbumFilter = virtualQueryValue;
            currentBrowserMode = BROWSER_ARTIST_ALBUMS; // 17
            virtualQueryValue = "";
            buildVirtualCategories("ARTIST_ALBUM");
            return;
        }
        View statusBar = findViewById(R.id.layout_status_bar);
        if (statusBar != null)
            statusBar.setBackgroundColor(ThemeManager.getStatusBarBackgroundColor());

        if (isCustomScanning) {
            showLoadingPopup(); // 🚀 스캔 중이라면 대형 스피너 팝업을 띄웁니다!
            currentBrowserMode = BROWSER_ROOT;
            buildFileBrowserUI();
            return;
        }
        // 🚀 기존의 뚱뚱하고 느린 스크롤뷰를 끄고, 초고속 리스트뷰를 켭니다!
        scrollViewBrowser.setVisibility(View.GONE);
        listVirtualSongs.setVisibility(View.VISIBLE);

        // 🚀 [수정] RECENT 모드일 때 헤더 타이틀 대응
        if (virtualQueryType.equals("RECENT")) {
            tvBrowserPath.setText(t("Library") + ": " + t("Recently Added"));
        } else if (virtualQueryType.equals("ALL")) {
            tvBrowserPath
                    .setText(t("Library") + ": " + (isAudiobookLibraryMode ? t("All Audiobooks") : t("All Songs")));
        } else {
            tvBrowserPath.setText(t("Library") + ": " + virtualQueryValue);
        }
        virtualSongList.clear();
        currentScrollIndexList.clear(); // 🚀 기존 인덱스 초기화
        final List<SongItem> targetSongs = new ArrayList<>();

        // 🚀 [핵심 방어막] 현재 오디오북 모드 스위치 상태에 따라 뒤질 바구니를 명확히 고정합니다!
        List<SongItem> activeLibrary = isAudiobookLibraryMode ? audiobookLibrary : customLibrary;

        for (SongItem song : activeLibrary) {
            // 🚀 [대개조 완료] 가상 분류를 긁어올 때, 현재 노래 파일이 실제로 소속된 루트 폴더까지 2중으로 철저히 검사합니다!
            // 이렇게 격벽을 쳐주어야 'ALL'을 누르든 'ARTIST'를 누르든 뮤직 폴더의 파일이 오디오북 리스트로 절대 넘어오지 못합니다.
            boolean isPathMatched = false;
            if (isAudiobookLibraryMode) {
                // 오디오북 모드라면 파일 절대 경로에 'Audiobooks'가 포함되어 있어야만 패스!
                if (song.file.getAbsolutePath().contains("/Audiobooks"))
                    isPathMatched = true;
            } else {
                // 뮤직 모드라면 파일 절대 경로에 'Music'이 포함되어 있어야만 패스!
                if (song.file.getAbsolutePath().contains("/Music"))
                    isPathMatched = true;
            }

            // 경로 필터링 장벽을 통과한 파일들만 조건별 분류 공정에 진입시킵니다.
            if (isPathMatched) {

                // 🚀 [5일 커트라인 방어막] RECENT 모드일 때 5일(120시간)이 지난 곡은 가차 없이 컷!
                boolean isRecentPassed = true;
                if (virtualQueryType.equals("RECENT")) {
                    long now = System.currentTimeMillis();
                    long fiveDaysMs = 5L * 24 * 60 * 60 * 1000; // 5일을 밀리초(ms)로 환산
                    if (now - song.file.lastModified() > fiveDaysMs) {
                        isRecentPassed = false; // 5일 지났으면 불합격!
                    }
                }

                // 💡 방어막을 통과한(isRecentPassed == true) 신선한 곡들만 바구니에 담습니다.
                if (isRecentPassed && (virtualQueryType.equals("ALL") || virtualQueryType.equals("RECENT") ||
                        (virtualQueryType.equals("ARTIST") && song.artist.equals(virtualQueryValue)) ||
                        ((virtualQueryType.equals("ALBUM") || virtualQueryType.equals("ARTIST_ALBUM")) && song.album.equals(virtualQueryValue)) ||
                        (virtualQueryType.equals("COVER_FLOW_ALBUM") && song.album.equals(virtualQueryValue)) ||
                        (virtualQueryType.equals("YEAR") && song.year.equals(virtualQueryValue)) ||
                        (virtualQueryType.equals("GENRE") && song.genre.equals(virtualQueryValue)) ||
                        // =========================================================
                        // 🚀 [신규 콤보 추가] 연도+가수+앨범 & 장르+가수+앨범 모두 일치할 때만 통과!
                        // =========================================================
                        (virtualQueryType.equals("YEAR_ARTIST_ALBUM") && song.album.equals(virtualQueryValue) && song.artist.equals(artistForAlbumFilter) && song.year.equals(yearOrGenreFilter)) ||
                        (virtualQueryType.equals("GENRE_ARTIST_ALBUM") && song.album.equals(virtualQueryValue) && song.artist.equals(artistForAlbumFilter) && song.genre.equals(yearOrGenreFilter)))
                ) {
                    targetSongs.add(song);
                }
            }
        }

        // =======================================================
        // 🚀 1. [앨범 모드 전용] 트랙 번호순 정렬 엔진 가동!
        // =======================================================
        java.util.Collections.sort(targetSongs, new java.util.Comparator<SongItem>() {
            @Override
            public int compare(SongItem s1, SongItem s2) {
                // 앨범 모드이거나 커버 플로우에서 넘어왔을 때만 트랙 번호로 정렬!
                if ("ALBUM".equals(virtualQueryType) || "COVER_FLOW_ALBUM".equals(virtualQueryType)) {
                    int t1 = trackNumberMap.containsKey(s1.file.getAbsolutePath())
                            ? trackNumberMap.get(s1.file.getAbsolutePath())
                            : 0;
                    int t2 = trackNumberMap.containsKey(s2.file.getAbsolutePath())
                            ? trackNumberMap.get(s2.file.getAbsolutePath())
                            : 0;

                    if (t1 != t2) {
                        return Integer.compare(t1, t2); // 번호가 있으면 오름차순(1, 2, 3...) 정렬
                    }
                }
                // 트랙 번호가 아예 없거나, 앨범 모드가 아니면 기존처럼 제목 알파벳순 정렬
                return s1.title.compareToIgnoreCase(s2.title);
            }
        });

        // =======================================================
        // 🚀 2. 재생 목록 초기화 및 화면 표시용(Display) 바구니 생성
        // =======================================================
        virtualSongList.clear();
        currentScrollIndexList.clear();
        List<SongItem> displaySongs = new ArrayList<>();

        // 원본 라이브러리(DB)를 오염시키지 않기 위해, 화면에 던져줄 껍데기만 새로 포장합니다.
        for (SongItem song : targetSongs) {
            String displayTitle = song.title;

            // 앨범 모드일 때만 제목 앞에 "01. ", "02. " 형식으로 트랙 번호 훈장 달아주기!
            if ("ALBUM".equals(virtualQueryType) || "COVER_FLOW_ALBUM".equals(virtualQueryType)) {
                int tNum = trackNumberMap.containsKey(song.file.getAbsolutePath())
                        ? trackNumberMap.get(song.file.getAbsolutePath())
                        : 0;
                if (tNum > 0) {
                    displayTitle = String.format(Locale.US, "%02d. %s", tNum, song.title);
                }
            }

            // 예쁘게 포장된 제목으로 새 아이템을 만들어서 화면용 바구니에 쏙!
            displaySongs.add(new SongItem(song.file, displayTitle, song.artist, song.album, song.year, song.genre));

            // 실제 음악을 틀기 위한 플레이어용 주소와 인덱스는 그대로 세팅
            virtualSongList.add(song.file);
            currentScrollIndexList.add(displayTitle);
        }

        // =======================================================
        // 🚀 3. 완성된 바구니(displaySongs)를 리스트뷰 어댑터에 장착!
        // =======================================================
        SongListAdapter adapter = new SongListAdapter(displaySongs);
        listVirtualSongs.setAdapter(adapter);

        // 🚀 [스마트 포커스] 현재 재생 중인 곡이 리스트에 있다면 그 인덱스를 찾아냅니다!
        int targetIndex = -1;
        if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentIndex >= 0
                && currentIndex < currentPlaylist.size()) {
            File playingFile = currentPlaylist.get(currentIndex);
            for (int i = 0; i < targetSongs.size(); i++) {
                if (targetSongs.get(i).file.getAbsolutePath().equals(playingFile.getAbsolutePath())) {
                    targetIndex = i;
                    break;
                }
            }
        }

        final int finalTargetIdx = targetIndex;
        listVirtualSongs.post(new Runnable() {
            @Override
            public void run() {
                // 재생 중인 곡이 확인되면 해당 위치로 자동 스크롤 후 포커스 록온!
                if (finalTargetIdx >= 0 && finalTargetIdx < listVirtualSongs.getCount()) {
                    listVirtualSongs.setSelectionFromTop(finalTargetIdx, 0);
                    listVirtualSongs.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int visiblePos = finalTargetIdx - listVirtualSongs.getFirstVisiblePosition();
                            if (visiblePos >= 0 && visiblePos < listVirtualSongs.getChildCount()) {
                                listVirtualSongs.getChildAt(visiblePos).requestFocus();
                            }
                        }
                    }, 50);
                } else {
                    if (listVirtualSongs.getChildCount() > 0) {
                        listVirtualSongs.getChildAt(0).requestFocus();
                    }
                }
            }
        });
        buildAlphabetIndexBar();
    }
    // 🚀 [초고속 라이브러리 캐시 저장 엔진]
    private void saveLibraryToCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File cacheFile = new File(getFilesDir(), "library_cache.txt");
                    java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(cacheFile), "UTF-8"));

                    // 뮤직 바구니 저장
                    for (SongItem s : customLibrary) {
                        int tNum = trackNumberMap.containsKey(s.file.getAbsolutePath()) ? trackNumberMap.get(s.file.getAbsolutePath()) : 0;
                        bw.write("M|" + s.file.getAbsolutePath() + "|" + s.title + "|" + s.artist + "|" + s.album + "|" + s.year + "|" + s.genre + "|" + tNum + "\n");
                    }
                    // 오디오북 바구니 저장
                    for (SongItem s : audiobookLibrary) {
                        int tNum = trackNumberMap.containsKey(s.file.getAbsolutePath()) ? trackNumberMap.get(s.file.getAbsolutePath()) : 0;
                        bw.write("B|" + s.file.getAbsolutePath() + "|" + s.title + "|" + s.artist + "|" + s.album + "|" + s.year + "|" + s.genre + "|" + tNum + "\n");
                    }
                    bw.close();
                } catch (Exception e) {}
            }
        }).start();
    }

    // 🚀 [초고속 라이브러리 캐시 불러오기 엔진]
    private boolean loadLibraryFromCache() {
        File cacheFile = new File(getFilesDir(), "library_cache.txt");
        if (!cacheFile.exists()) return false;

        try {
            customLibrary.clear();
            audiobookLibrary.clear();
            trackNumberMap.clear();

            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(cacheFile), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length >= 8) {
                    File f = new File(p[1]);
                    if (!f.exists()) continue; // 💡 지워진 파일은 스킵하여 자동 청소!

                    SongItem item = new SongItem(f, p[2], p[3], p[4], p[5], p[6]);
                    int tNum = Integer.parseInt(p[7]);

                    if (p[0].equals("M")) customLibrary.add(item);
                    else audiobookLibrary.add(item);

                    if (tNum > 0) trackNumberMap.put(p[1], tNum);
                }
            }
            br.close();
            return (!customLibrary.isEmpty() || !audiobookLibrary.isEmpty());
        } catch (Exception e) {
            return false; // 에러 나면 false 반환해서 다시 스캔하게 만듦
        }
    }
    private void buildFolderBrowserUI() {
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText(t("Path") + ": " + StoragePaths.toDisplayPath(currentFolder.getAbsolutePath()));
        File[] files = currentFolder.listFiles();

        if (files == null || files.length == 0) {
            Button btnEmpty = createListButton(files == null ? "⚠️ " + t("USB Disconnect Required (Tap to go back)")
                    : "📂 " + t("Empty Folder (Tap to go back)"));
            btnEmpty.setTextColor(0xFFFF5555);
            btnEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();

                    // =======================================================
                    // 🚀 [수정할 부분] Movies 폴더나 Audiobooks 폴더가 텅 비어있어도 무사히 메인 화면으로 탈출하도록 방어막 추가!
                    // =======================================================
                    if (currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())
                            || StoragePaths.isAnyStorageRoot(currentFolder.getAbsolutePath())
                            || StoragePaths.isVideosRoot(currentFolder.getAbsolutePath())
                            || StoragePaths.isAudiobooksRoot(currentFolder.getAbsolutePath())) {

                        if (isPickingBackground) {
                            isPickingBackground = false;
                            isNavigatingToSubMenu = true;
                            changeScreen(STATE_SETTINGS);
                            buildBackgroundSettingsUI();
                        } else {
                            currentBrowserMode = BROWSER_ROOT;
                            buildFileBrowserUI();
                        }
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
        List<File> videoFiles = new ArrayList<File>(); // 🚀 [추가 1] 비디오 파일 전용 바구니 생성!

        for (File f : files) {
            String lowerName = f.getName().toLowerCase();

            if (f.isDirectory())
                folders.add(f);
            else if (isPickingBackground && isImageFile(f))
                imageFiles.add(f);
            else if (!isPickingBackground
                    && (lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".avi")))
                videoFiles.add(f); // 🚀 [추가 2] 비디오 확장자면 비디오 바구니에 쏙 담기!
            else if (!isPickingBackground && isAudioFile(f))
                audioFiles.add(f);
            else if (!isPickingBackground && isApkFile(f))
                apkFiles.add(f);
        }

        java.util.Comparator<File> fileSorter = new java.util.Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        };

        java.util.Collections.sort(folders, fileSorter);
        java.util.Collections.sort(videoFiles, fileSorter); // 🚀 [추가 3] 비디오도 알파벳순 정렬!
        java.util.Collections.sort(audioFiles, fileSorter);
        java.util.Collections.sort(apkFiles, fileSorter);
        java.util.Collections.sort(imageFiles, fileSorter);

        if (!isPickingBackground && (audioFiles.size() > 0 || folders.size() > 0)) {
            Button btnPlayAll = createListButton("▶ " + t("Play All"));
            btnPlayAll.setTextColor(0xFFFFFFFF);
            btnPlayAll.setTypeface(null, Typeface.BOLD);

            btnPlayAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    final List<File> allAudioInFolder = new ArrayList<>();
                    showLoadingPopup();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            collectAudioFilesAsFile(currentFolder, allAudioInFolder);
                            java.util.Collections.sort(allAudioInFolder, fileSorter);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (layoutLoadingOverlay != null)
                                        layoutLoadingOverlay.setVisibility(View.GONE);

                                    if (allAudioInFolder.isEmpty()) {
                                        Toast.makeText(MainActivity.this, t("No audio files found in subfolders."),
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                                .playTrackList(allAudioInFolder, 0);
                                        changeScreen(STATE_PLAYER);
                                    }
                                }
                            });
                        }
                    }).start();
                }
            });
            containerBrowserItems.addView(btnPlayAll);
        }

        for (final File folder : folders) {
            View b = createListButtonWithIcon("\uE2C7", folder.getName());
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
                View b = createListButtonWithIcon("\uE410", img.getName());
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        try {
                            prefs.edit().putString("bg_path", img.getAbsolutePath()).commit();
                        } catch (Exception e) {
                        }
                        updateMainMenuBackground();
                        Toast.makeText(MainActivity.this, t("Background Applied!"), Toast.LENGTH_SHORT).show();
                        isPickingBackground = false;
                        isNavigatingToSubMenu = true;
                        changeScreen(STATE_SETTINGS);
                        buildBackgroundSettingsUI();
                    }
                });
                containerBrowserItems.addView(b);
            }
        } else {
            for (final File apk : apkFiles) {
                Button b = createListButton("📦 [" + t("INSTALL") + "] " + apk.getName());
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

            // =======================================================
            // 🚀 [추가 4] 비디오 파일 버튼 렌더링 & 썸네일 이미지 변환!
            // =======================================================
            for (final File video : videoFiles) {
                View b = createListButtonWithIcon("\uE04B", video.getName());

                // 🚀 비디오 전용: 앞쪽 글자 아이콘(TextView)을 빼버리고, 썸네일 사진(ImageView)으로 교체 조립!
                LinearLayout row = (LinearLayout) b;
                TextView tvIcon = (TextView) row.getChildAt(0);
                float d = getResources().getDisplayMetrics().density;
                int iconWidth = (int) (80 * d);
                int iconHeight = (int) (50 * d);

                ImageView ivThumb = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(iconWidth, iconHeight);
                thumbLp.rightMargin = ((LinearLayout.LayoutParams) tvIcon.getLayoutParams()).rightMargin;
                ivThumb.setLayoutParams(thumbLp);
                ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

                row.removeViewAt(0);
                row.addView(ivThumb, 0);

                // 비동기 썸네일 엔진 발사!
                loadVideoThumbnailAsync(video.getAbsolutePath(), ivThumb);

                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                        intent.putExtra("VIDEO_PATH", video.getAbsolutePath());
                        startActivity(intent);
                    }
                });
                containerBrowserItems.addView(b);
            }

            for (final File audio : audioFiles) {
                String iconCode = (isAudiobookLibraryMode || currentBrowserMode == BROWSER_AUDIOBOOKS) ? "\uE310"
                        : "\uE405";
                View b = createListButtonWithIcon(iconCode, audio.getName());

                if (isAudiobookLibraryMode || currentBrowserMode == BROWSER_AUDIOBOOKS) {
                    int pos = prefs.getInt("book_pos_" + audio.getAbsolutePath(), 0);
                    int dur = prefs.getInt("book_dur_" + audio.getAbsolutePath(), 0);
                    if (pos > 0 && dur > 0) {
                        setupAudiobookProgress(b, pos, dur);
                    }
                }

                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        if (currentBrowserMode == BROWSER_AUDIOBOOKS) {
                            com.themoon.y1.managers.AudiobookManager.getInstance(MainActivity.this)
                                    .setupBookPlaylist(MainActivity.this, audio, currentFolder);
                        } else {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().setupFolderPlaylist(audio,
                                    currentFolder);
                        }
                        changeScreen(STATE_PLAYER);
                    }
                });

                b.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        clickFeedback();
                        isLongPressConsumed = true;
                        if (currentBrowserMode != BROWSER_M3U_SONGS) {
                            showAddToPlaylistDialog(audio);
                        }
                        return true;
                    }
                });
                containerBrowserItems.addView(b);
            }
        }
        if (containerBrowserItems.getChildCount() > 0)
            containerBrowserItems.getChildAt(0).requestFocus();

        // 🚀 [대개조 완료] ScrollView 자체의 준비 상태를 파악하여 애니메이션 없이 완벽히 스크롤 고정!
        final ScrollView sv = (ScrollView) containerBrowserItems.getParent();
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    sv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                boolean found = false;
                String playingFileName = "";
                if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentIndex >= 0
                        && currentIndex < currentPlaylist.size()) {
                    playingFileName = currentPlaylist.get(currentIndex).getName();
                }

                if (!lastBrowserFocusText.isEmpty() || !playingFileName.isEmpty()) {
                    for (int i = 0; i < containerBrowserItems.getChildCount(); i++) {
                        View v = containerBrowserItems.getChildAt(i);
                        String itemText = "";

                        if (v instanceof Button) {
                            itemText = ((Button) v).getText().toString();
                        } else if (v instanceof LinearLayout) {
                            LinearLayout layout = (LinearLayout) v;
                            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof TextView) {
                                itemText = ((TextView) layout.getChildAt(1)).getText().toString();
                            }
                        }

                        String cleanItemText = itemText;
                        if (cleanItemText.contains("  ⏱")) {
                            cleanItemText = cleanItemText.substring(0, cleanItemText.indexOf("  ⏱")).trim();
                        }

                        if (!lastBrowserFocusText.isEmpty() && cleanItemText.equals(lastBrowserFocusText)) {
                            int offset = (sv.getHeight() / 2) - (v.getHeight() / 2);
                            if (exactOffsetMemory.containsKey(cleanItemText))
                                offset = exactOffsetMemory.get(cleanItemText);

                            int targetY = v.getTop() - offset;
                            if (targetY < 0)
                                targetY = 0;

                            sv.scrollTo(0, targetY);
                            v.requestFocus();
                            found = true;
                            break;
                        } else if (lastBrowserFocusText.isEmpty() && !playingFileName.isEmpty()
                                && cleanItemText.equals(playingFileName)) {
                            int offset = (sv.getHeight() / 2) - (v.getHeight() / 2);
                            if (exactOffsetMemory.containsKey(cleanItemText))
                                offset = exactOffsetMemory.get(cleanItemText);

                            int targetY = v.getTop() - offset;
                            if (targetY < 0)
                                targetY = 0;

                            sv.scrollTo(0, targetY);
                            v.requestFocus();
                            found = true;
                            break;
                        }
                    }
                }

                if (!found && containerBrowserItems.getChildCount() > 0) {
                    containerBrowserItems.getChildAt(0).requestFocus();
                }
                lastBrowserFocusText = "";
            }
        });
    }

    // 🚀 [추가 도구 1] 영어로 된 정렬(Gravity) 텍스트를 안드로이드가 알아듣게 번역해 주는 함수
    private int parseGravity(String gravityStr) {
        int g = Gravity.TOP | Gravity.LEFT; // 기본값
        if (gravityStr == null || gravityStr.isEmpty())
            return g;
        gravityStr = gravityStr.toLowerCase();
        g = 0;
        if (gravityStr.contains("top"))
            g |= Gravity.TOP;
        if (gravityStr.contains("bottom"))
            g |= Gravity.BOTTOM;
        if (gravityStr.contains("center_vertical"))
            g |= Gravity.CENTER_VERTICAL;
        if (gravityStr.contains("left"))
            g |= Gravity.LEFT;
        if (gravityStr.contains("right"))
            g |= Gravity.RIGHT;
        if (gravityStr.contains("center_horizontal"))
            g |= Gravity.CENTER_HORIZONTAL;
        if (gravityStr.equals("center"))
            g = Gravity.CENTER; // 완벽한 정중앙

        if (g == 0)
            g = Gravity.TOP | Gravity.LEFT;
        return g;
    }

    // 🚀 [추가 도구 3] JSON의 X, Y, 너비, 높이, 정렬을 받아서 절대 좌표 세팅값으로 바꿔주는 공장!
    private FrameLayout.LayoutParams createDynamicLayoutParams(ThemeManager.MenuElement el,
            float density) {
        int w = el.width > 0 ? (int) (el.width * density) : FrameLayout.LayoutParams.WRAP_CONTENT;
        int h = el.height > 0 ? (int) (el.height * density) : FrameLayout.LayoutParams.WRAP_CONTENT;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.gravity = parseGravity(el.gravity);

        // 정렬(Gravity)에 맞추어 X, Y 마진을 지능적으로 부여합니다.
        if ((lp.gravity & Gravity.RIGHT) == Gravity.RIGHT)
            lp.rightMargin = (int) (el.x * density);
        else
            lp.leftMargin = (int) (el.x * density);

        if ((lp.gravity & Gravity.BOTTOM) == Gravity.BOTTOM)
            lp.bottomMargin = (int) (el.y * density);
        else
            lp.topMargin = (int) (el.y * density);

        return lp;
    }

    // 🚀 [추가 도구 2] 테마 기본 둥글기와 개별 버튼 둥글기를 똑똑하게 섞어주는 함수
    private GradientDrawable createDynamicButtonBackground(int color, int elementRadius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        // JSON에 개별 반경(-1이 아님)이 적혀있으면 그걸 쓰고, 없으면 테마 기본값을 씁니다!
        float r = (elementRadius == -1 ? ThemeManager.getButtonRadius() : elementRadius)
                * getResources().getDisplayMetrics().density;
        shape.setCornerRadius(r);
        return shape;
    }

    // 🚀 [추가 도구 4] 위젯 전용: 지정된 배경색과 둥글기로 예쁜 박스를 만들어냅니다.
    private GradientDrawable createWidgetBackground(String bgColorStr, int elementRadius) {
        if (bgColorStr == null || bgColorStr.trim().isEmpty())
            return null;
        int color;
        try {
            color = android.graphics.Color.parseColor(bgColorStr.trim());
        } catch (Exception e) {
            return null;
        }

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        // JSON에 둥글기를 안 적었으면 테마 기본 둥글기를 따라갑니다.
        float r = (elementRadius == -1 ? ThemeManager.getButtonRadius() : elementRadius)
                * getResources().getDisplayMetrics().density;
        shape.setCornerRadius(r);
        return shape;
    }

    // 🚀 [포커스 제어 완전 정복] 휠을 돌리면 공간을 무시하고 JSON 인덱스 순서대로 빙글빙글 돕니다!
    private void buildDynamicMainMenuUI() {

        final int safeMenuIndex = lastMainMenuFocusIndex;

        ViewGroup mainMenu = (ViewGroup) layoutMainMenu;
        // 🚀 [상태바 보호막 가동!!]
        // 기존 XML 뼈대에 있던 '상단 여백(상태바 높이)'을 알아냅니다.
        int safeTopPadding = mainMenu.getPaddingTop();

        // 만약 기존 여백을 못 불러왔다면, 안드로이드 기본 상태바 높이인 24dp로 강제 방어막을 칩니다!
        if (safeTopPadding == 0) {
            safeTopPadding = (int) (24 * getResources().getDisplayMetrics().density);
        }

        // 좌(0), 상단(보호막), 우(0), 하단(0) 으로 패딩을 다시 설정합니다.
        mainMenu.setPadding(0, safeTopPadding, 0, 0);

        for (int i = 0; i < mainMenu.getChildCount(); i++) {
            mainMenu.getChildAt(i).setVisibility(View.GONE);
        }

        // 🚀 [버그 수리: 고스트 뷰 잔상 박멸]
        View oldCanvas = mainMenu.findViewWithTag("dynamic_canvas");
        while (oldCanvas != null) {
            if (oldCanvas instanceof ViewGroup) {
                ((ViewGroup) oldCanvas).removeAllViews();
            }
            mainMenu.removeView(oldCanvas);
            oldCanvas = mainMenu.findViewWithTag("dynamic_canvas");
        }

        // 🚀 [타입 선언 복구] canvas 변수 선언을 다시 붙여줍니다!
        FrameLayout canvas = new FrameLayout(this);
        canvas.setTag("dynamic_canvas");
        canvas.setBackgroundColor(ThemeManager.getOverlayBackgroundColor());

        // 🚀 [핵심 해결 3] 캔버스 레벨에서도 아이콘이 크게 튀어나올 수 있도록 봉인 해제!
        canvas.setClipChildren(false);
        canvas.setClipToPadding(false);

        mainMenu.addView(canvas, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        tvWidgetClock = null;
        widgetBatteryView = null;
        ivWidgetAlbum = null;
        tvWidgetAlbumTitle = null;
        tvWidgetAlbumArtist = null;
        ivWidgetFocusImage = null; // 🚀 초기화 추가

        final float density = getResources().getDisplayMetrics().density;
        List<ThemeManager.MenuElement> elements = ThemeManager.getCurrentTheme().menuElements;

        List<ThemeManager.MenuElement> buttonElements = new ArrayList<>();
        List<ThemeManager.MenuElement> widgetElements = new ArrayList<>();

        for (ThemeManager.MenuElement el : elements) {
            if (el.type.equals("button"))
                buttonElements.add(el);
            else
                widgetElements.add(el);
        }

        sortMenuElements(buttonElements);

        // 🚀 [신규 통합 금고] 생성되는 모든 위젯들의 주소와 JSON 정보를 한곳에 보관하는 사령탑 메모리
        // 🚀 [버그 수리] 기존에 있던 전역 변수 금고를 초기화하여 재활용합니다! (final 지역 변수 선언 삭제)
        widgetViewRegistry.clear();
        final java.util.HashMap<String, LinearLayout> listContainers = new java.util.HashMap<>();

        // 💡 위젯 그리기
        for (ThemeManager.MenuElement el : widgetElements) {
            GradientDrawable widgetBg = createWidgetBackground(el.bgColor, el.radius);
            int p = (int) (el.padding * density);
            View createdWidgetView = null; // 🚀 위젯 참조 변수

            if (el.type.equals("list_box")) {
                final ScrollView sv = new ScrollView(this);
                sv.setLayoutParams(createDynamicLayoutParams(el, density));
                sv.setVerticalScrollBarEnabled(false);
                sv.setFocusable(false);
                sv.setFocusableInTouchMode(false);
                sv.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
                if (widgetBg != null)
                    sv.setBackground(widgetBg);
                sv.setVisibility(View.VISIBLE); // 🚀 껍데기(리스트 상자)는 항상 열어둡니다.
                sv.getViewTreeObserver()
                        .addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
                            @Override
                            public void onScrollChanged() {
                                android.view.ViewParent p = sv.getParent();
                                if (p instanceof View)
                                    ((View) p).invalidate();
                                sv.invalidate();
                            }
                        });

                LinearLayout innerLayout = new LinearLayout(this);
                innerLayout.setOrientation(LinearLayout.VERTICAL);
                innerLayout.setPadding(p, p, p, p);
                sv.addView(innerLayout, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                canvas.addView(sv);
                listContainers.put(el.id, innerLayout);
                createdWidgetView = sv;
            } else if (el.type.equals("box")) {
                ImageView boxView = new ImageView(this);
                boxView.setLayoutParams(createDynamicLayoutParams(el, density));
                if (widgetBg == null)
                    widgetBg = createWidgetBackground("#00000000", el.radius);
                boxView.setBackground(widgetBg);

                String imgName = (el.iconNormal != null && !el.iconNormal.isEmpty()) ? el.iconNormal : el.textNormal;
                if (imgName != null && !imgName.isEmpty() && !imgName.equals("New Item")) {
                    Bitmap bmp = ThemeManager.getCustomIcon(imgName, MainActivity.this, 0);
                    if (bmp != null) {
                        int maxTexSize = 2048;
                        if (bmp.getWidth() > maxTexSize || bmp.getHeight() > maxTexSize) {
                            float ratio = Math.min((float) maxTexSize / bmp.getWidth(),
                                    (float) maxTexSize / bmp.getHeight());
                            boxView.setImageBitmap(Bitmap.createScaledBitmap(bmp,
                                    (int) (bmp.getWidth() * ratio), (int) (bmp.getHeight() * ratio), true));
                        } else
                            boxView.setImageBitmap(bmp);
                        boxView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                }
                canvas.addView(boxView);
                createdWidgetView = boxView;
            } else if (el.type.equals("widget_clock")) {
                tvWidgetClock = new TextView(this);
                tvWidgetClock.setGravity(Gravity.CENTER);
                tvWidgetClock.setLayoutParams(createDynamicLayoutParams(el, density));
                tvWidgetClock.setTextColor(ThemeManager.getTextColorPrimary());
                tvWidgetClock.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
                currentClockSize = el.textSize > 0 ? el.textSize : 48f;
                tvWidgetClock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, currentClockSize * density);
                if (widgetBg != null)
                    tvWidgetClock.setBackground(widgetBg);
                tvWidgetClock.setPadding(p, p, p, p);

                canvas.addView(tvWidgetClock); // 🚀 캔버스 직속 복귀!
                createdWidgetView = tvWidgetClock;
            } else if (el.type.equals("widget_battery")) {
                widgetBatteryView = new WidgetBatteryBarView(this);
                widgetBatteryView.setLayoutParams(createDynamicLayoutParams(el, density));
                widgetBatteryView.setPadding(p, p, p, p);
                canvas.addView(widgetBatteryView);
                createdWidgetView = widgetBatteryView;
            } else if (el.type.equals("widget_album")) {
                LinearLayout albumContainer = new LinearLayout(this);
                layoutWidgetAlbumContainer = albumContainer;
                boolean isHorizontal = el.textPosition.equalsIgnoreCase("left")
                        || el.textPosition.equalsIgnoreCase("right");
                albumContainer.setOrientation(isHorizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
                albumContainer.setGravity(Gravity.CENTER);
                albumContainer.setLayoutParams(createDynamicLayoutParams(el, density));
                if (widgetBg != null)
                    albumContainer.setBackground(widgetBg);
                albumContainer.setPadding(p, p, p, p);

                ivWidgetAlbum = new ImageView(this);
                ivWidgetAlbum.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int pSubtract = el.padding * 2;
                int imgSize = isHorizontal ? (int) ((el.height - pSubtract) * density)
                        : (int) ((el.height - pSubtract) * 0.65f * density);
                if (imgSize <= 0)
                    imgSize = (int) (110 * density);
                LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(imgSize, imgSize);

                LinearLayout textContainer = new LinearLayout(this);
                textContainer.setOrientation(LinearLayout.VERTICAL);
                int textGravity = el.textAlign.equalsIgnoreCase("left")
                        ? (Gravity.LEFT | Gravity.CENTER_VERTICAL)
                        : (el.textAlign.equalsIgnoreCase("right")
                                ? (Gravity.RIGHT | Gravity.CENTER_VERTICAL)
                                : Gravity.CENTER);
                textContainer.setGravity(textGravity);
                LinearLayout.LayoutParams textContainerLp = isHorizontal
                        ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
                        : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);

                int safeWidth = el.width > 0 ? (int) (el.width * density) : (int) (200 * density);
                int availableWidth = isHorizontal ? (safeWidth - imgSize - (int) (15 * density) - (p * 2))
                        : (safeWidth - (p * 2));
                if (availableWidth <= 0)
                    availableWidth = (int) (150 * density);
                LinearLayout.LayoutParams textViewLp = new LinearLayout.LayoutParams(availableWidth,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                tvWidgetAlbumTitle = new TextView(this);
                tvWidgetAlbumTitle.setLayoutParams(textViewLp);
                tvWidgetAlbumTitle.setGravity(textGravity);
                tvWidgetAlbumTitle.setSingleLine(true);
                tvWidgetAlbumTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tvWidgetAlbumTitle.setTextColor(ThemeManager.getTextColorPrimary());
                tvWidgetAlbumTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
                tvWidgetAlbumTitle.setTextSize(el.textSize > 0 ? el.textSize : 16);
                textContainer.addView(tvWidgetAlbumTitle);

                tvWidgetAlbumArtist = new TextView(this);
                tvWidgetAlbumArtist.setLayoutParams(textViewLp);
                tvWidgetAlbumArtist.setGravity(textGravity);
                tvWidgetAlbumArtist.setSingleLine(true);
                tvWidgetAlbumArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tvWidgetAlbumArtist.setTextColor(ThemeManager.getTextColorSecondary());
                tvWidgetAlbumArtist.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
                tvWidgetAlbumArtist.setTextSize(el.textSecondarySize > 0 ? el.textSecondarySize : 12);
                textContainer.addView(tvWidgetAlbumArtist);

                if (el.textPosition.equalsIgnoreCase("left")) {
                    imgLp.leftMargin = (int) (15 * density);
                    albumContainer.addView(textContainer, textContainerLp);
                    albumContainer.addView(ivWidgetAlbum, imgLp);
                } else if (el.textPosition.equalsIgnoreCase("right")) {
                    textContainerLp.leftMargin = (int) (15 * density);
                    albumContainer.addView(ivWidgetAlbum, imgLp);
                    albumContainer.addView(textContainer, textContainerLp);
                } else if (el.textPosition.equalsIgnoreCase("top")) {
                    textContainerLp.bottomMargin = (int) (5 * density);
                    albumContainer.addView(textContainer, textContainerLp);
                    albumContainer.addView(ivWidgetAlbum, imgLp);
                } else {
                    textContainerLp.topMargin = (int) (5 * density);
                    albumContainer.addView(ivWidgetAlbum, imgLp);
                    albumContainer.addView(textContainer, textContainerLp);
                }

                canvas.addView(albumContainer); // 🚀 캔버스 직속 복귀!
                createdWidgetView = albumContainer;
            } else if (el.type.equals("widget_analog_clock")) {
                customAnalogClockView = new CustomAnalogClockView(this);
                customAnalogClockView.setLayoutParams(createDynamicLayoutParams(el, density));
                customAnalogClockView.setPadding(p, p, p, p);
                if (el.bgColor != null && !el.bgColor.trim().isEmpty()) {
                    try {
                        customAnalogClockView
                                .setClockBackgroundColor(android.graphics.Color.parseColor(el.bgColor.trim()));
                    } catch (Exception e) {
                    }
                }
                canvas.addView(customAnalogClockView);
                createdWidgetView = customAnalogClockView;
            } else if (el.type.equals("widget_circular_battery")) {
                customCircularBatteryView = new CircularBatteryView(this);
                customCircularBatteryView.setLayoutParams(createDynamicLayoutParams(el, density));
                customCircularBatteryView.setPadding(p, p, p, p);
                if (el.textSize > 0)
                    customCircularBatteryView.setCustomTextSize(el.textSize * density);
                canvas.addView(customCircularBatteryView);
                createdWidgetView = customCircularBatteryView;
            } else if (el.type.equals("widget_focus_image")) {
                ivWidgetFocusImage = new ImageView(this); // 🚀 하이브리드 통합을 위해 뼈대를 이미지뷰 원본 단독으로 슬림화!
                ivWidgetFocusImage.setLayoutParams(createDynamicLayoutParams(el, density));
                ivWidgetFocusImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (widgetBg != null)
                    ivWidgetFocusImage.setBackground(widgetBg);
                ivWidgetFocusImage.setPadding(p, p, p, p);

                canvas.addView(ivWidgetFocusImage);
                createdWidgetView = ivWidgetFocusImage;
            }

            // 🚀 주소록 대장에 이 위젯 객체와 테마 JSON 설계 정보를 꼼꼼히 입고합니다.
            if (createdWidgetView != null) {
                // ❌ [치명적 에러 원인 제거] 루프 도중 리스트를 수정하여 앱이 터지는 현상을 완벽 방지!
                widgetViewRegistry.put(createdWidgetView, el); // 올바른 금고에 보관합니다.

                // 🚀 [논리 수정] parentId가 아니라, 새로 만든 visibleOnFocus(감시 대상)가 적혀있다면 평소엔 숨겨둡니다!
                if (el.visibleOnFocus != null && !el.visibleOnFocus.trim().isEmpty()) {
                    createdWidgetView.setVisibility(View.GONE);
                }
            }
        }
        // 💡 버튼 그리기
        List<LinearLayout> createdButtons = new ArrayList<>(); // 🚀 Button에서 LinearLayout으로 업그레이드

        // 🚀 [숨김 필터링 엔진] 사용자가 설정창에서 숨기기로 한 버튼은 명단에서 아예 빼버립니다!
        List<ThemeManager.MenuElement> visibleButtonElements = new ArrayList<>();
        for (ThemeManager.MenuElement el : buttonElements) {
            if (!prefs.getBoolean("hide_btn_" + el.id, false)) {
                visibleButtonElements.add(el);
            }
        }

        // 전체 명단이 아닌, '보이기로 한 버튼들'만 가지고 UI 조립 및 포커스 고리(ID)를 엮습니다.
        for (int i = 0; i < visibleButtonElements.size(); i++) {
            final ThemeManager.MenuElement el = visibleButtonElements.get(i);
// =========================================================
            if ("line".equals(el.type)) {
                View lineView = new View(this);
                if (el.bgColor != null && !el.bgColor.isEmpty()) {
                    try { lineView.setBackgroundColor(android.graphics.Color.parseColor(el.bgColor)); } catch (Exception e) {}
                } else {
                    lineView.setBackgroundColor(0xFFFFFFFF);
                }

                // 💡 1. 리스트 상자 소속인지 확인
                if (el.parentId != null && !el.parentId.isEmpty() && listContainers.containsKey(el.parentId)) {
                    LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            el.height > 0 ? (int)(el.height * density) : (int)(2 * density) // 기본 두께 2dp
                    );
                    // 리스트 안에서는 Y값을 위쪽 마진, X값을 좌우 마진으로 사용!
                    lineLp.setMargins((int)(el.x * density), (int)(el.y * density), (int)(el.x * density), 0);
                    lineView.setLayoutParams(lineLp);
                    listContainers.get(el.parentId).addView(lineView);
                }
                // 💡 2. 소속이 없는 자유 캔버스 소속이라면!
                else {
                    FrameLayout.LayoutParams lineLp = new FrameLayout.LayoutParams(
                            el.width > 0 ? (int)(el.width * density) : ViewGroup.LayoutParams.MATCH_PARENT,
                            el.height > 0 ? (int)(el.height * density) : (int)(2 * density)
                    );
                    lineLp.leftMargin = (int)(el.x * density);
                    lineLp.topMargin = (int)(el.y * density);

                    String lineGravity = el.gravity != null ? el.gravity.toLowerCase() : "top|left";
                    if (lineGravity.contains("center_horizontal") || lineGravity.equals("center")) {
                        lineLp.gravity = Gravity.CENTER_HORIZONTAL;
                        lineLp.leftMargin = 0;
                    } else if (lineGravity.contains("right")) {
                        lineLp.gravity = Gravity.RIGHT;
                        lineLp.rightMargin = (int)(el.x * density);
                    } else {
                        lineLp.gravity = Gravity.LEFT;
                    }

                    if (lineGravity.contains("center_vertical") || lineGravity.equals("center")) {
                        lineLp.gravity |= Gravity.CENTER_VERTICAL;
                        lineLp.topMargin = 0;
                    } else if (lineGravity.contains("bottom")) {
                        lineLp.gravity |= Gravity.BOTTOM;
                        lineLp.bottomMargin = (int)(el.y * density);
                    } else {
                        lineLp.gravity |= Gravity.TOP;
                    }

                    lineView.setLayoutParams(lineLp);
                    canvas.addView(lineView); // 🚀 자유 캔버스에 추가!
                }
                continue; // 💡 선(Line) 그리기가 끝났으므로, 아래쪽의 일반 버튼 조립 코드는 건너뜁니다!
            }
            // 🚀 1. 버튼을 감싸는 전체 컨테이너 (LinearLayout)
            final LinearLayout btn = new LinearLayout(this);
            btn.setId(10000 + i);
            btn.setTag(el.action);
            btn.setSoundEffectsEnabled(false);
            btn.setFocusable(true);
            // 🚀 [포커스 증발 수리 3] 클릭 가능 속성이 빠지면 안드로이드 엔진이 버튼의 존재를 무시해버리므로 클릭 본능을 주입합니다!
            btn.setClickable(true);
            btn.setOrientation(LinearLayout.HORIZONTAL);
            btn.setOnLongClickListener(globalScreenOffLongClickListener);
            // 🚀 2. 좌측 메인 텍스트 및 아이콘 뷰
            final TextView tvMain = new TextView(this);
            tvMain.setSingleLine(true);
            tvMain.setEllipsize(android.text.TextUtils.TruncateAt.END);
            // 🚀 [안드로이드 버그 해결] 텍스트뷰 특유의 보이지 않는 유령 여백(약 5px)을 물리적으로 완벽하게 박살 냅니다!
            tvMain.setIncludeFontPadding(false);
            tvMain.setPadding(0, 0, 0, 0);
            tvMain.setMinimumWidth(0);
            tvMain.setMinimumHeight(0);

            // 🚀 3. 우측 화살표 및 포인트 텍스트 뷰
            final TextView tvRight = new TextView(this);
            tvRight.setSingleLine(true);
            tvRight.setIncludeFontPadding(false); // 여기도 일치시킵니다.
            tvRight.setPadding(0, 0, 0, 0);

            final boolean isIconOnly = (el.textNormal == null || el.textNormal.trim().isEmpty());

            // 🚀 [궁극의 공식] 패딩이 커져서 아이콘 크기가 마이너스가 되어 앱이 튕기는 에러까지 완벽하게 차단합니다!
            final int calculatedIconSize;
            if (isIconOnly) {
                int w = el.width > 0 ? el.width : 50;
                int h = el.height > 0 ? el.height : 50;
                int p = (int) (el.padding * density);
                int tempSize = (int) (Math.min(w, h) * density) - (p * 2);
                // 아이콘이 너무 작아지면 최소 10dp는 유지하도록 방어막을 칩니다.
                calculatedIconSize = tempSize > 0 ? tempSize : (int) (10 * density);
            } else {
                int h = el.height > 0 ? el.height : 50;
                calculatedIconSize = (int) (h * density * 0.5f);
            }

            int textGravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            if (el.textAlign != null && !el.textAlign.isEmpty()) {
                String ta = el.textAlign.toLowerCase();
                if (ta.equals("center"))
                    textGravity = Gravity.CENTER;
                else if (ta.equals("right"))
                    textGravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                else if (ta.equals("top"))
                    textGravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                else if (ta.equals("bottom"))
                    textGravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            } else {
                if (el.gravity.toLowerCase().contains("center"))
                    textGravity = Gravity.CENTER;
            }

            if (isIconOnly) {
                btn.setGravity(Gravity.CENTER);
                int p = (int) (el.padding * density);
                btn.setPadding(p, p, p, p);
                tvMain.setGravity(Gravity.CENTER);
            } else {
                btn.setGravity(Gravity.CENTER_VERTICAL);
                tvMain.setGravity(textGravity);
                tvRight.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

                // 🚀 [해결 1] 글씨가 있는 버튼도 에디터에서 설정한 패딩값을 완벽하게 챙겨줍니다!
                int customPad = (int) (el.padding * density);

                if (el.textAlign != null
                        && (el.textAlign.equalsIgnoreCase("top") || el.textAlign.equalsIgnoreCase("bottom"))) {
                    // 사용자가 값을 넣었으면 그 값을 쓰고, 안 넣었으면(0) 기본값 15 적용
                    int verticalPad = el.padding > 0 ? customPad : (int) (15 * density);
                    btn.setPadding(customPad, verticalPad, customPad, verticalPad);
                } else {
                    int horizontalPad = el.padding > 0 ? customPad : (int) (15 * density);
                    btn.setPadding(horizontalPad, customPad, horizontalPad, customPad);
                }
            }

            btn.setLayoutParams(createDynamicLayoutParams(el, density));
            tvMain.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
            tvRight.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);

            if (!isIconOnly) {
                // 🚀 [글자 크기 버그 해결] 안드로이드 기본(SP) 단위 대신 픽셀(PX) 단위를 사용하여 에디터와 100% 똑같은 크기로 강제
                // 고정합니다!
                float mainSize = el.textSize > 0 ? el.textSize : 16; // 에디터 기본값과 동일한 16px로 세팅
                float rightSize = el.textSecondarySize > 0 ? el.textSecondarySize : mainSize; // 우측 텍스트 독립 크기 지원

                tvMain.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, mainSize * density);
                tvRight.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, rightSize * density);
            }

            LinearLayout.LayoutParams lpMain;
            LinearLayout.LayoutParams lpRight;

            if (isIconOnly) {
                // 🚀 [핵심 해결 1] 아이콘 전용일 때는 우측 10dp 마진(도둑)을 완전히 없애고 자기 크기만 갖게 하여 정중앙에 고정!
                lpMain = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lpRight = new LinearLayout.LayoutParams(0, 0);
                tvRight.setVisibility(View.GONE); // 유령 텍스트 뷰 소멸

                // 🚀 [핵심 해결 2] 확대(Zoom) 애니메이션이 발동할 때 패딩선에 걸려 잘리지 않도록 봉인 해제!
                btn.setClipChildren(false);
                btn.setClipToPadding(false);
            } else {
                // 일반 버튼은 텍스트가 남은 공간을 밀어내도록 가중치 1.0f 유지
                lpMain = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                lpRight = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lpRight.leftMargin = (int) (10 * density);
            }

            btn.addView(tvMain, lpMain);
            btn.addView(tvRight, lpRight);

            final Runnable setNormalState = new Runnable() {
                public void run() {
                    // 🚀 [버그 해결 1] 테마 기본 배경색 대신, 에디터에서 개별 지정한 배경색(bgColor)이 있으면 최우선으로 가져옵니다!
                    int normalBgColor = ThemeManager.getListButtonNormalBg();
                    if (el.bgColor != null && !el.bgColor.trim().isEmpty()) {
                        try {
                            normalBgColor = android.graphics.Color.parseColor(el.bgColor.trim());
                        } catch (Exception e) {
                        }
                    }

                    // 🚀 [버그 해결 2] 아이콘 전용 버튼이든 일반 버튼이든 투명색 강제 할당을 없애고 무조건 배경색을 칠해줍니다!
                    btn.setBackground(createDynamicButtonBackground(normalBgColor, el.radius));

                    if (isIconOnly) {
                        tvMain.setText("");
                    } else {
                        tvMain.setText(t(el.textNormal));
                        tvMain.setTextColor(ThemeManager.getTextColorPrimary());
                        tvRight.setText(el.textRight != null ? t(el.textRight) : "");

                        if (el.textRightColor != null && !el.textRightColor.isEmpty()) {
                            try {
                                tvRight.setTextColor(android.graphics.Color.parseColor(el.textRightColor));
                            } catch (Exception e) {
                                tvRight.setTextColor(ThemeManager.getTextColorPrimary());
                            }
                        } else {
                            tvRight.setTextColor(ThemeManager.getTextColorPrimary());
                        }
                    }

                    if (el.iconNormal != null && !el.iconNormal.isEmpty()) {
                        Bitmap bmp = ThemeManager.getCustomIcon(el.iconNormal, MainActivity.this, 0);
                        if (bmp != null) {
                            // 🚀 [핵심 기술 1] 안드로이드가 원본 크기를 무시하지 못하도록, 비트맵 자체를 픽셀 단위로 물리적으로 깎아냅니다!
                            Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp,
                                    calculatedIconSize, calculatedIconSize, true);
                            android.graphics.drawable.BitmapDrawable d = new android.graphics.drawable.BitmapDrawable(
                                    getResources(), scaledBmp);

                            d.setBounds(0, 0, calculatedIconSize, calculatedIconSize);
                            tvMain.setCompoundDrawables(d, null, null, null);

                            tvMain.setCompoundDrawablePadding(isIconOnly ? 0 : (int) (10 * density));
                        } else {
                            tvMain.setCompoundDrawables(null, null, null, null);
                        }
                    } else {
                        tvMain.setCompoundDrawables(null, null, null, null);
                    }
                    tvMain.setTranslationX(0);
                    tvMain.setTranslationY(0);
                    tvMain.setScaleX(1.0f);
                    tvMain.setScaleY(1.0f);
                }
            };
            setNormalState.run();

            btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        btn.setBackground(
                                createDynamicButtonBackground(ThemeManager.getListButtonFocusedBg(), el.radius));

                        if (isIconOnly) {
                            tvMain.setText("");
                        } else {
                            // 🚀 포커스 시 메인 글자와 우측 화살표 색상 동시 변경!
                            tvMain.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                            // 🚀 우측 텍스트 전용 포커스 색상 적용
                            if (el.textRightFocusedColor != null && !el.textRightFocusedColor.isEmpty()) {
                                try {
                                    tvRight.setTextColor(android.graphics.Color.parseColor(el.textRightFocusedColor));
                                } catch (Exception e) {
                                    tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                                }
                            } else {
                                tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                            }
                            if (el.textFocused != null && !el.textFocused.isEmpty())
                                tvMain.setText(t(el.textFocused));
                            else
                                tvMain.setText(t(el.textNormal));
                        }

                        String targetIcon = (el.iconFocused != null && !el.iconFocused.isEmpty()) ? el.iconFocused
                                : el.iconNormal;
                        if (targetIcon != null && !targetIcon.isEmpty()) {
                            Bitmap bmpF = ThemeManager.getCustomIcon(targetIcon, MainActivity.this, 0);
                            if (bmpF != null) {
                                // 🚀 [핵심 기술 2] 포커스 시에도 똑같이 비트맵을 물리적으로 깎아서 끼웁니다!
                                Bitmap scaledBmpF = Bitmap.createScaledBitmap(bmpF,
                                        calculatedIconSize, calculatedIconSize, true);
                                android.graphics.drawable.BitmapDrawable d = new android.graphics.drawable.BitmapDrawable(
                                        getResources(), scaledBmpF);

                                d.setBounds(0, 0, calculatedIconSize, calculatedIconSize);
                                tvMain.setCompoundDrawables(d, null, null, null);
                            }
                        }
                        updateFocusPreviewLiveContent(el);

                        lastMainMenuFocusIndex = btn.getId() - 10000;
                        tvMain.animate()
                                .translationX(el.focusOffsetX * density)
                                .translationY(el.focusOffsetY * density)
                                .scaleX(el.focusScale).scaleY(el.focusScale)
                                .setDuration(150).start();

                    } else {
                        // 포커스 빠질 때 원상 복구
                        tvMain.animate()
                                .translationX(0).translationY(0)
                                .scaleX(1.0f).scaleY(1.0f)
                                .setDuration(150).start();
                        setNormalState.run();
                    }
                }
            });

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    switch (el.action) {
                        case "OPEN_PLAYER":
                            if (currentPlaylist.isEmpty())
                                Toast.makeText(MainActivity.this, "No music is currently playing.", Toast.LENGTH_SHORT)
                                        .show();
                            else
                                changeScreen(STATE_PLAYER);
                            break;
                        case "OPEN_COVER_FLOW":
                            // 🚀 [지능형 부팅 스캔 추적 가동]
                            if (isCustomScanning) {
                                currentBrowserMode = BROWSER_COVER_FLOW;
                                changeScreen(STATE_BROWSER);
                                showLoadingPopup();
                            } else if (customLibrary.isEmpty()) {
                                startMediaLibraryScan();
                                currentBrowserMode = BROWSER_COVER_FLOW;
                                changeScreen(STATE_BROWSER);
                            } else {
                                // 🚀 [궁극의 선전환 + 투명 로딩 연출 가동]
                                // 1. changeScreen 내부의 자동 빌더 간섭을 일시 차단하기 위해 모드를 -1로 숨깁니다.
                                currentBrowserMode = -1;
                                changeScreen(STATE_BROWSER); // 2. 대기 시간 없이 브라우저 창(커버플로우 페이지)으로 즉시 순간이동!

                                // 🚀 [깜빡임 버그 완벽 수리] 누르는 0.001초 그 순간부터 상태바를 미리 완전 투명하게 뚫어버립니다!
                                View statusBar = findViewById(R.id.layout_status_bar);
                                if (statusBar != null)
                                    statusBar.setBackgroundColor(0x00000000);

                                // 3. 이동한 화면의 기본 틀(상단 타이틀 등)을 선제 마킹하고 기존 잔상을 비웁니다.
                                if (tvBrowserPath != null)
                                    tvBrowserPath.setText(t("Library") + ": " + t("Cover Flow"));
                                if (containerBrowserItems != null)
                                    containerBrowserItems.removeAllViews();
                                if (listVirtualSongs != null)
                                    listVirtualSongs.setVisibility(View.GONE);
                                if (scrollViewBrowser != null)
                                    scrollViewBrowser.setVisibility(View.VISIBLE);

                                // 4. ⚡ [기획 반영] 화면 전체를 깜깜하게 가리지 않고, 상단 상태 바를 유지하며 중앙에 글씨만 띄웁니다!
                                if (layoutLoadingOverlay != null) {
                                    layoutLoadingOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT); // 🚀
                                                                                                                 // 배경을
                                                                                                                 // 투명하게!
                                    if (pbLoadingProgress != null)
                                        pbLoadingProgress.setVisibility(View.GONE); // 🚀 하단 프로그레스 바 숨김
                                    if (tvLoadingProgress != null) {
                                        tvLoadingProgress.setTextSize(18f);
                                        tvLoadingProgress.setTextColor(ThemeManager.getTextColorPrimary());
                                        tvLoadingProgress.setText(t("Loading Cover Flow...\nPlease wait."));
                                    }
                                    layoutLoadingOverlay.setAlpha(1.0f);
                                    layoutLoadingOverlay.setVisibility(View.VISIBLE); // 투명 레이어 업!
                                }

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        currentBrowserMode = BROWSER_COVER_FLOW; // 5. 150ms 숨통이 트이면 진짜 모드 장전!
                                        buildCoverFlowUI(); // 6. 무거운 3D 앨범장 카드를 부드럽게 렌더링 조립합니다.

                                        // 7. ⚡ 로딩 완료 후 오버레이를 닫고, 다음 공용 미디어 스캔 작업을 위해 원래 배경색과 바를 원상태로 리셋합니다!
                                        if (layoutLoadingOverlay != null) {
                                            layoutLoadingOverlay.setVisibility(View.GONE);
                                            layoutLoadingOverlay.setBackgroundColor(0xDD000000); // 원래 반투명 블랙 복구
                                            if (tvLoadingProgress != null)
                                                tvLoadingProgress.setTextColor(0xFFFFFFFF);
                                            if (pbLoadingProgress != null)
                                                pbLoadingProgress.setVisibility(View.VISIBLE); // 바 복구
                                        }
                                    }
                                }, 150);
                            }
                            break;
                        case "OPEN_BROWSER":
                            isAudiobookLibraryMode = false;
                            currentBrowserMode = BROWSER_ROOT;
                            if (customLibrary.isEmpty() && !isCustomScanning)
                                startMediaLibraryScan();
                            changeScreen(STATE_BROWSER);
                            if (isCustomScanning)
                                showLoadingPopup();
                            break;

                        // 📚 오디오북 라이브러리로 다이렉트 진입 (테마 설정에서 action을 "OPEN_AUDIOBOOKS"로 설정하시면 됩니다!)
                        case "OPEN_AUDIOBOOKS":
                            isAudiobookLibraryMode = true;
                            currentBrowserMode = BROWSER_ROOT;
                            if (audiobookLibrary.isEmpty() && !isCustomScanning)
                                startMediaLibraryScan();
                            changeScreen(STATE_BROWSER);
                            if (isCustomScanning)
                                showLoadingPopup();
                            break;
                        case "OPEN_BLUETOOTH":
                            changeScreen(STATE_BLUETOOTH);
                            break;
                        case "OPEN_SETTINGS":
                            changeScreen(STATE_SETTINGS);
                            break;
                        case "OPEN_WEBSERVER":
                            changeScreen(STATE_WEBSERVER);
                            break;
                        // 🚀 [라디오 부활] 테마에서 라디오 버튼을 누르면 안드로이드 내장 FM 라디오를 켭니다!
                        case "OPEN_RADIO":
                            clickFeedback();
                            // 🚀 투박한 순정 앱 대신, 우리가 만든 세련된 내장형 라디오 스튜디오로 직접 진입합니다!
                            isNavigatingToSubMenu = true;
                            changeScreen(STATE_SETTINGS);
                            buildRadioUI();
                            isNavigatingToSubMenu = false;
                            break;
                        // 🚀🚀🚀 [여기서부터 새로 추가된 다이렉트 숏컷 액션들!] 🚀🚀🚀
                        case "OPEN_ROOT_FOLDER":
                            currentBrowserMode = BROWSER_FOLDER;
                            currentFolder = StoragePaths.getPrimaryRoot(); // 최상위 루트 폴더로 강제 이동!
                            changeScreen(STATE_BROWSER);
                            break;
                        case "OPEN_PODCASTS":
                            isPodcastOpenedFromLibrary = false;
                            currentBrowserMode = BROWSER_PODCAST_CHANNELS;
                            changeScreen(STATE_BROWSER);
                            break;
                        case "OPEN_VIDEOS":
                            isVideoOpenedFromLibrary = false;
                            currentBrowserMode = BROWSER_VIDEOS;
                            currentFolder = StoragePaths.getVideosDir();
                            changeScreen(STATE_BROWSER);
                            break;
                        case "OPEN_WIFI":
                            changeScreen(STATE_WIFI);
                            break;
                        case "OPEN_BRIGHTNESS":
                            changeScreen(STATE_BRIGHTNESS);
                            break;
                        case "OPEN_STORAGE_INFO":
                            changeScreen(STATE_STORAGE);
                            break;
                        // 🚀 [신규 숏컷] 메인 화면에서 재생목록(Playlists) 화면으로 점프하는 직통 채널 개설!
                        case "OPEN_PLAYLISTS":
                            isPlaylistOpenedFromLibrary = false; // 🚀 메인 다이렉트 숏컷임을 도장에 찍습니다!
                            lastBrowserFocusText = "";
                            currentBrowserMode = BROWSER_PLAYLISTS;
                            changeScreen(STATE_BROWSER);
                            break;
                        case "OPEN_BACKGROUND_SETTINGS":
                            isNavigatingToSubMenu = true;
                            changeScreen(STATE_SETTINGS);
                            buildBackgroundSettingsUI();
                            isNavigatingToSubMenu = false;
                            break;
                        case "OPEN_THEME_SETTINGS":
                            isNavigatingToSubMenu = true;
                            changeScreen(STATE_SETTINGS);
                            buildThemeSelectorUI();
                            isNavigatingToSubMenu = false;
                            break;
                        case "OPEN_TIME_SETTINGS":
                            java.util.Calendar c = java.util.Calendar.getInstance();
                            dtYear = c.get(java.util.Calendar.YEAR);
                            dtMonth = c.get(java.util.Calendar.MONTH) + 1;
                            dtDay = c.get(java.util.Calendar.DAY_OF_MONTH);
                            dtHour = c.get(java.util.Calendar.HOUR_OF_DAY);
                            dtMinute = c.get(java.util.Calendar.MINUTE);
                            isNavigatingToSubMenu = true;
                            changeScreen(STATE_SETTINGS);
                            buildDateTimeUI();
                            isNavigatingToSubMenu = false;
                            break;
                        // 🚀🚀🚀 [추가 끝] 🚀🚀🚀

                        default:
                            break;
                    }
                }
            });

            if (el.parentId != null && !el.parentId.isEmpty() && listContainers.containsKey(el.parentId)) {
                // 💡 1. 리스트 상자 소속이라면: 세로 정렬(LinearLayout) 규칙에 맞게 속성을 바꿔서 넣습니다.
                LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        el.height > 0 ? (int) (el.height * density) : LinearLayout.LayoutParams.WRAP_CONTENT);
                // 리스트 안에서는 Y값을 Top Margin(위쪽 간격)으로, X값을 좌우 간격으로 똑똑하게 재활용합니다!
                listLp.setMargins((int) (el.x * density), (int) (el.y * density), (int) (el.x * density), 0);
                btn.setLayoutParams(listLp);

                // 캔버스가 아니라, 부모 그룹(리스트 상자) 안으로 쏙 들어갑니다!
                listContainers.get(el.parentId).addView(btn);
            } else {
                // 💡 2. 소속이 없다면: 기존처럼 X, Y 절대 좌표를 캔버스에 직접 꽂아 넣습니다.
                btn.setLayoutParams(createDynamicLayoutParams(el, density));
                canvas.addView(btn);
            }

            createdButtons.add(btn);

        }

        int totalBtns = createdButtons.size();
        for (int i = 0; i < totalBtns; i++) {
            LinearLayout currentBtn = createdButtons.get(i);
            // 🚀 [루프 조건 분기] 루프 스크롤 설정 상태에 따라 양 끝단 경계면의 무한 래핑을 허용하거나 단절(View.NO_ID)합니다.
            int prevId = (i == 0) ? (isLoopScrollOn ? 10000 + totalBtns - 1 : View.NO_ID) : 10000 + i - 1;
            int nextId = (i == totalBtns - 1) ? (isLoopScrollOn ? 10000 : View.NO_ID) : 10000 + i + 1;

            currentBtn.setNextFocusUpId(prevId);
            currentBtn.setNextFocusLeftId(prevId);

            currentBtn.setNextFocusDownId(nextId);
            currentBtn.setNextFocusRightId(nextId);
        }

        refreshWidgets();

        // 🚀 [대개조 완료] OnGlobalLayoutListener를 통해 메인 메뉴 복귀 시 딜레이와 이질감 없는 초고속 복원을 수행합니다!
        if (!createdButtons.isEmpty()) {
            final LinearLayout firstBtn = createdButtons.get(0);
            canvas.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (Build.VERSION.SDK_INT >= 16) {
                                canvas.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                canvas.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }

                            if (currentScreenState == STATE_MENU) {
                                int targetId = 10000 + safeMenuIndex;
                                View targetBtn = findViewById(targetId);

                                if (targetBtn != null && targetBtn.getVisibility() == View.VISIBLE) {
                                    android.view.ViewParent parent = targetBtn.getParent();
                                    if (parent != null && parent.getParent() instanceof ScrollView) {
                                        ScrollView sv = (ScrollView) parent.getParent();

                                        // 🚀 [메인 화면 초정밀 스크롤 복원] 메인 전용 금고 키를 불러와 1픽셀 오차 없이 복원합니다!
                                        int offset = (sv.getHeight() / 2) - (targetBtn.getHeight() / 2);
                                        if (exactOffsetMemory.containsKey("MAIN_" + safeMenuIndex)) {
                                            offset = exactOffsetMemory.get("MAIN_" + safeMenuIndex);
                                        }
                                        int targetY = targetBtn.getTop() - offset;
                                        if (targetY < 0)
                                            targetY = 0;

                                        sv.scrollTo(0, targetY);
                                    }
                                    targetBtn.requestFocus();
                                    lastMainMenuFocusIndex = safeMenuIndex;
                                } else {
                                    firstBtn.requestFocus();
                                    android.view.ViewParent parent = firstBtn.getParent();
                                    if (parent != null && parent.getParent() instanceof ScrollView) {
                                        ((ScrollView) parent.getParent()).scrollTo(0, 0);
                                    }
                                }
                            }
                        }
                    });
        }
    } // buildDynamicMainMenuUI 끝

    private void collectAudioFilesAsFile(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    collectAudioFilesAsFile(f, list); // 폴더를 발견하면 그 안으로 다시 파고듭니다!
                } else if (isAudioFile(f)) {
                    list.add(f); // 음악 파일이면 바구니에 쏙 담습니다.
                }
            }
        }
    }

    private void installApk(File apkFile) {
        try {
            // 🚀 [완벽한 해결책: 무음 백그라운드 설치(Silent Install) 통합 엔진]
            Process process = Runtime.getRuntime().exec("su");
            java.io.DataOutputStream os = new java.io.DataOutputStream(process.getOutputStream());

            // 1. [타이밍 버그 해결] 패키지 매니저가 접근할 수 있게 폴더와 파일의 권한을 엽니다.
            os.writeBytes("chmod 777 " + apkFile.getParentFile().getAbsolutePath() + "\n");
            os.writeBytes("chmod 777 " + apkFile.getAbsolutePath() + "\n");

            // 2. 안드로이드 시스템(디스크)이 권한 변경을 완전히 알아차릴 때까지 확실히 동기화(sync)시킵니다!
            os.writeBytes("sync\n");

            // 3. 백그라운드 설치 실행 (로그 기록 포함)
            os.writeBytes(
                    "pm install -r " + apkFile.getAbsolutePath() + " > " + StoragePaths.primaryFile("y1_update_log.txt").getAbsolutePath() + " 2>&1 \n");

            // 4. 설치가 끝날 때까지 넉넉하게 3초 대기 (이제 백그라운드 스레드라 화면이 안 멈춥니다!)
            os.writeBytes("sleep 3\n");

            // 5. 설치가 완료되면 런처(앱)를 곧바로 다시 실행시켜서 화면으로 복귀!
            os.writeBytes("am start -n " + getPackageName() + "/.MainActivity\n");

            os.writeBytes("exit\n");
            os.flush();
            os.close();

            // 프로세스가 완전히 끝날 때까지 대기
            process.waitFor();

            return;
        } catch (Exception e) {
            // 루트 권한 에러 시 플랜 B로 넘어감
        }

        // 6. [플랜 B] 루팅이 안 된 기기일 경우, 수동 설치 화면 띄우기
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = Uri.parse("file://" + apkFile.getAbsolutePath());
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, t("Install Failed."), Toast.LENGTH_SHORT).show();
        }
    }

    // 🚀 [신규 엔진] 스펙트럼과 가사의 상태를 실시간으로 판별해서 화면을 교대해주는 전담 일꾼!
    private void refreshVisualizerState() {
        View albumContainer = (View) ivAlbumArt.getParent();

        if (isVisualizerShowing) {
            if (ivAlbumArt != null) ivAlbumArt.setVisibility(View.INVISIBLE);
            if (albumContainer != null) albumContainer.setVisibility(View.VISIBLE);
            updateVisualizerAndLyricsLayout();

            // 현재 곡에 가사가 있다면? -> 스펙트럼을 끄고 가사창을 켭니다!
            if (!currentLyrics.isEmpty() || plainLyrics != null) {
                if (audioVisualizer != null)
                    audioVisualizer.setEnabled(false);
                visualizerView.setVisibility(View.GONE);
                visualizerView.clearAnimation(); // 잔상 제거

                lyricScrollView.setVisibility(View.VISIBLE);

                if (plainLyrics != null && currentLyrics.isEmpty()) {
                    lyricScrollView.post(new Runnable() {
                        public void run() {
                            lyricScrollView.scrollTo(0, 0);
                        }
                    });
                }
            }
            // 현재 곡에 가사가 없다면? -> 가사창을 끄고 화려한 스펙트럼을 켭니다!
            else {
                lyricScrollView.setVisibility(View.GONE);
                visualizerView.setVisibility(View.VISIBLE);
                visualizerView.invalidate();
                if (audioVisualizer != null)
                    audioVisualizer.setEnabled(true);
            }
        } else {
            // 시각화 모드가 아예 꺼져있다면 모두 숨기고 앨범 아트로 복귀
            visualizerView.setVisibility(View.GONE);
            lyricScrollView.setVisibility(View.GONE);
            if (albumContainer != null) albumContainer.setVisibility(View.VISIBLE);
            if (ivAlbumArt != null) ivAlbumArt.setVisibility(View.VISIBLE);
            if (audioVisualizer != null)
                audioVisualizer.setEnabled(false);
        }

        // 🚀 [커스텀 테마 가사/스펙트럼 모드 연동]
        if (!ThemeManager.availableThemes.isEmpty()) {
            ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
            if (theme != null && theme.playerElements != null && !theme.playerElements.isEmpty()) {
                applyThemeToPlayerUI();
            }
        }
    }
    // 🚀 [셔플/반복 설정 팝업 강제 소환 명령]
    public void showShuffleRepeatOverlay() {
        // 플레이어 화면이 아닐 땐 무시합니다.
        if (currentScreenState != STATE_PLAYER) return;

        // 창이 열릴 때 현재 상태(ON/OFF)를 읽어와서 텍스트를 최신화해 줍니다.
        btnOverlayShuffle.setText("🔀 " + t("Shuffle") + ": " + (isShuffleMode ? t("ON") : t("OFF")));
        String[] repText = {"OFF", "ONE", "ALL"};
        btnOverlayRepeat.setText("🔁 " + t("Repeat") + ": " + t(repText[repeatMode]));

        // 🚀 [그림자 및 테두리 입체 효과 갱신] 플레이어 배경 밝기에 맞춰 테두리 선명도를 조절하고 입체 그림자 적용
        if (layoutShuffleRepeatOverlay != null) {
            float dOver = getResources().getDisplayMetrics().density;
            android.graphics.drawable.GradientDrawable overlayBg = new android.graphics.drawable.GradientDrawable();
            int bgCol = ThemeManager.getOverlayBackgroundColor();
            if (bgCol == 0) bgCol = 0xFF1A1A24;
            overlayBg.setColor(bgCol | 0xEE000000);
            overlayBg.setCornerRadius(16 * dOver);
            int strokeColor = isPlayerBackgroundLight() ? 0x88000000 : 0x88FFFFFF;
            overlayBg.setStroke((int)(2 * dOver), strokeColor);
            layoutShuffleRepeatOverlay.setBackground(overlayBg);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                layoutShuffleRepeatOverlay.setElevation(35 * dOver); // 🚀 [API 21+ 그림자 추가, 구버전 크래시 방지]
            }
        }

        // 투명 망토를 벗기고 화면에 등장!
        layoutShuffleRepeatOverlay.setVisibility(View.VISIBLE);
        // 🚀 [해결] custom theme player 적용 시 앨범아트/노래제목 등이 layoutShuffleRepeatOverlay 보다 뒤에 추가되면서
        // Z-index 상 가려지는 문제를 해결하기 위해 최상단으로 끌어올림(bringToFront)!
        layoutShuffleRepeatOverlay.bringToFront();

        // 🚀 메뉴가 뜨자마자 첫 번째 줄(셔플 버튼)로 휠 커서를 자석처럼 쫙! 끌어당깁니다.
        layoutShuffleRepeatOverlay.post(() -> {
            btnOverlayShuffle.requestFocus();
        });
    }
    public void toggleVisualizer() {
        isVisualizerShowing = !isVisualizerShowing;

        // 🚀 [커스텀 테마 가사/스펙트럼 모드 연동]
        boolean isCustomTheme = false;
        if (!ThemeManager.availableThemes.isEmpty()) {
            ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
            if (theme != null && theme.playerElements != null && !theme.playerElements.isEmpty()) {
                isCustomTheme = true;
                applyThemeToPlayerUI();
            }
        }

        if (!isCustomTheme) {
            updateVisualizerAndLyricsLayout();
        }

        if (isVisualizerShowing) {
            // 1. 투명 사각형(DummyBox)이 공간을 지탱해주므로, 알맹이(ivAlbumArt)만 숨겨도 레이아웃이 절대 무너지지 않습니다!
            if (ivAlbumArt != null) ivAlbumArt.setVisibility(View.INVISIBLE);

            boolean hasLyrics = (currentLyrics != null && !currentLyrics.isEmpty()) || (plainLyrics != null && !plainLyrics.isEmpty());

            if (hasLyrics) {
                if (visualizerView != null) visualizerView.setVisibility(View.GONE);
                if (lyricScrollView != null) lyricScrollView.setVisibility(View.VISIBLE);
                try { if (audioVisualizer != null) audioVisualizer.setEnabled(false); } catch(Exception e){}
            } else {
                if (lyricScrollView != null) lyricScrollView.setVisibility(View.GONE);
                if (visualizerView != null) visualizerView.setVisibility(View.VISIBLE);
                try { if (audioVisualizer != null) audioVisualizer.setEnabled(true); } catch(Exception e){}
            }
            if (layoutVisProgress != null) layoutVisProgress.setVisibility(View.GONE);

        } else {
            // 2. 완벽 원상 복구! (앨범 이미지를 다시 켭니다)
            if (ivAlbumArt != null) ivAlbumArt.setVisibility(View.VISIBLE);

            if (visualizerView != null) visualizerView.setVisibility(View.GONE);
            if (lyricScrollView != null) lyricScrollView.setVisibility(View.GONE);
            try { if (audioVisualizer != null) audioVisualizer.setEnabled(false); } catch(Exception e){}

            if (layoutVisProgress != null) layoutVisProgress.setVisibility(View.GONE);
        }
    }
    // 💡 [수정] 오디오 엔진에 빨대를 꽂아 주파수 데이터를 빼오는 함수
    public void setupVisualizer() {
        try {
            // 🚀 [완벽 해결] 매번 새롭게 엔진을 만들어서 장착합니다! (메모리 누수 원천 차단)
            if (audioVisualizer != null) {
                audioVisualizer.setEnabled(false);
                audioVisualizer.release();
                audioVisualizer = null;
            }

            // ⭕ [아래 코드로 덮어쓰기]
            audioVisualizer = new android.media.audiofx.Visualizer(
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().getAudioSessionId());
            audioVisualizer.setCaptureSize(android.media.audiofx.Visualizer.getCaptureSizeRange()[1]);
            audioVisualizer.setDataCaptureListener(new android.media.audiofx.Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(android.media.audiofx.Visualizer visualizer, byte[] waveform,
                        int samplingRate) {
                }

                @Override
                public void onFftDataCapture(android.media.audiofx.Visualizer visualizer, byte[] fft,
                        int samplingRate) {
                    if (isVisualizerShowing && visualizerView != null
                            && visualizerView.getVisibility() == View.VISIBLE) {
                        visualizerView.updateVisualizer(fft, currentAlbumColor);
                    }
                }
            }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, false, true);

            if (isVisualizerShowing) {
                audioVisualizer.setEnabled(true);
            }
        } catch (Exception e) {
        }
    }

    // 🚀 [가사 엔진] .lrc 파일을 찾아 시간과 텍스트를 분리해 메모리에 담습니다.
    // 🚀 [가사 엔진 업그레이드] .lrc, USLT는 물론 m4b 챕터까지 싹 다 긁어오는 만능 파서!
    private void loadLyrics(File audioFile) {
        currentLyrics.clear();
        lyricTimestamps.clear();
        lastLyricIndex = -1;
        plainLyrics = null;
        if (tvLyrics != null)
            tvLyrics.setText("");

        if (audioFile == null)
            return;
        String path = audioFile.getAbsolutePath();

        // 🚀 [신규 장착 1] 파일 확장자가 .m4b 오디오북이라면 자체 챕터 해독기를 돌립니다!
        if (path.toLowerCase().endsWith(".m4b")) {
            com.themoon.y1.managers.M4bChapterParser.ChapterResult chResult = com.themoon.y1.managers.M4bChapterParser
                    .extractChapters(audioFile);

            // 챕터를 성공적으로 빼왔다면, 가사 대신 챕터 시간표를 가사 엔진에 쑤셔 넣습니다!
            if (!chResult.chaptersMap.isEmpty()) {
                currentLyrics.putAll(chResult.chaptersMap);
                lyricTimestamps.addAll(chResult.timestamps);
                return; // 💡 챕터를 세팅했으니 가사 찾기는 여기서 조기 종료!
            }
        }

        // (이 아래부터는 기존 코드와 100% 동일하게 유지하시면 됩니다!)
        int dotIdx = path.lastIndexOf(".");
        if (dotIdx > 0) {
            String lrcPath = path.substring(0, dotIdx) + ".lrc";
            File lrcFile = new File(lrcPath);

            // 1. 외부 .lrc 파일이 있는지 확인
            if (lrcFile.exists()) {
                try {
                    java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.InputStreamReader(new java.io.FileInputStream(lrcFile), "UTF-8"));
                    String line;
                    java.util.regex.Pattern pattern = java.util.regex.Pattern
                            .compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

                    while ((line = br.readLine()) != null) {
                        java.util.regex.Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            int min = Integer.parseInt(matcher.group(1));
                            int sec = Integer.parseInt(matcher.group(2));
                            int ms = Integer.parseInt(matcher.group(3));
                            if (matcher.group(3).length() == 2)
                                ms *= 10;

                            int totalMs = (min * 60 * 1000) + (sec * 1000) + ms;
                            String text = matcher.group(4).trim();

                            if (!text.isEmpty()) {
                                currentLyrics.put(totalMs, text);
                            }
                        }
                    }
                    br.close();
                    lyricTimestamps = new ArrayList<>(currentLyrics.keySet());
                    return; // 성공했으면 여기서 엔진 조기 종료!
                } catch (Exception e) {
                }
            }
        }

        // 2. 외부 .lrc 파일이 없다면 파일 내부에 박혀있는 가사 자체 채굴!
        if (path.toLowerCase().endsWith(".m4a") || path.toLowerCase().endsWith(".alac")) {
            Object[] alacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance().extractAlacMetadata(audioFile);
            if (alacTags.length > 7 && alacTags[7] != null) {
                plainLyrics = (String) alacTags[7];
            }
            // 🚀 [신규 호스 연결] FLAC 음원이면 FLAC 전용 6기통 채굴기를 돌려 가사를 뽑아옵니다!
        } else if (path.toLowerCase().endsWith(".flac")) {
            Object[] flacTags = com.themoon.y1.managers.AudioPlayerManager.getInstance().extractFlacMetadata(audioFile);
            if (flacTags.length > 7 && flacTags[7] != null) {
                plainLyrics = (String) flacTags[7];
            }
        } else {
            // 그 외(MP3 등)는 순정 ID3v2(USLT) 가사 파서 가동!
            plainLyrics = extractEmbeddedLyrics(audioFile);
        }

        // 🚀 [신규 장착] 3. 파일 내부에서 캔 가사(plainLyrics) 안에 타임라인 포맷이 있는지 정밀 검사합니다!
        if (plainLyrics != null && !plainLyrics.isEmpty()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern
                    .compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
            boolean isSyncedLrc = false;

            // 통짜 문자열을 줄바꿈(\n) 기준으로 쪼개서 한 줄씩 검사합니다.
            String[] lines = plainLyrics.split("\\r?\\n");
            for (String line : lines) {
                java.util.regex.Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    isSyncedLrc = true; // 💡 타임라인이 하나라도 발견되면 '싱크 가사'로 판정!

                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    int ms = Integer.parseInt(matcher.group(3));
                    if (matcher.group(3).length() == 2)
                        ms *= 10;

                    int totalMs = (min * 60 * 1000) + (sec * 1000) + ms;
                    String text = matcher.group(4).trim();

                    if (!text.isEmpty()) {
                        currentLyrics.put(totalMs, text);
                    }
                }
            }

            // 🎯 타임라인 해독에 성공했다면?
            if (isSyncedLrc) {
                lyricTimestamps = new ArrayList<>(currentLyrics.keySet());
                plainLyrics = null; // 💡 통짜 가사 취급을 막기 위해 변수를 깨끗하게 비워버립니다!
                return; // 🚀 스크롤 엔진이 알아서 작동하도록 여기서 함수를 우아하게 조기 종료!
            }
        }

        // 🎯 4. 타임라인이 없는 '진짜 순수 텍스트 가사'일 때만 화면에 통째로 뿌려줍니다!
        if (plainLyrics != null && !plainLyrics.isEmpty()) {
            if (tvLyrics != null) {
                tvLyrics.setTextColor(isPlayerBackgroundLight() ? 0xFF111111 : 0xFFFFFFFF);
                tvLyrics.setText(plainLyrics);
            }
        }
    }

    // 🚀 [신규 엔진] MP3 파일 내부의 ID3 태그를 직접 뜯어서 가사(USLT)를 추출하는 정밀 파서
    private String extractEmbeddedLyrics(File file) {
        try {
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
            byte[] header = new byte[10];
            raf.readFully(header);

            // ID3v2 태그가 존재하는지 확인 (MP3 파일의 시작점)
            if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                int majorVersion = header[3];
                // 태그 전체 크기 계산 (Syncsafe integer 방식)
                int tagSize = ((header[6] & 0x7F) << 21) | ((header[7] & 0x7F) << 14) | ((header[8] & 0x7F) << 7)
                        | (header[9] & 0x7F);

                // 가사가 보통 앞쪽에 있으므로 최대 512KB까지만 읽어서 메모리 폭발 완벽 방지!
                int readSize = Math.min(tagSize, 512 * 1024);
                byte[] tagData = new byte[readSize];
                int actualRead = raf.read(tagData);
                raf.close();

                int pos = 0;
                while (pos < actualRead - 10) {
                    String frameId = new String(tagData, pos, 4);
                    int frameSize;

                    // ID3v2.3과 ID3v2.4의 프레임 크기 계산 방식 차이 완벽 대응
                    if (majorVersion == 4) {
                        frameSize = ((tagData[pos + 4] & 0x7F) << 21) | ((tagData[pos + 5] & 0x7F) << 14)
                                | ((tagData[pos + 6] & 0x7F) << 7) | (tagData[pos + 7] & 0x7F);
                    } else {
                        frameSize = ((tagData[pos + 4] & 0xFF) << 24) | ((tagData[pos + 5] & 0xFF) << 16)
                                | ((tagData[pos + 6] & 0xFF) << 8) | (tagData[pos + 7] & 0xFF);
                    }

                    if (frameSize <= 0 || frameSize > actualRead - pos - 10)
                        break;

                    // 💡 USLT (Unsynchronized lyric/text transcription) 가사 프레임 발견!
                    if (frameId.equals("USLT")) {
                        int encoding = tagData[pos + 10]; // 인코딩 방식
                        int textPos = pos + 14; // 인코딩(1) + 언어코드(3) 건너뛰기

                        // Descriptor 문자열(가사 제목 등) 건너뛰기 (널 문자 0x00 찾기)
                        if (encoding == 1 || encoding == 2) { // UTF-16 (널 문자 2바이트)
                            while (textPos < pos + 10 + frameSize - 1) {
                                if (tagData[textPos] == 0 && tagData[textPos + 1] == 0) {
                                    textPos += 2;
                                    break;
                                }
                                textPos++;
                            }
                        } else { // ISO-8859-1 또는 UTF-8 (널 문자 1바이트)
                            while (textPos < pos + 10 + frameSize) {
                                if (tagData[textPos] == 0) {
                                    textPos += 1;
                                    break;
                                }
                                textPos++;
                            }
                        }

                        int lyricsLength = (pos + 10 + frameSize) - textPos;
                        if (lyricsLength > 0) {
                            String charset = "UTF-8";
                            if (encoding == 0)
                                charset = "ISO-8859-1";
                            else if (encoding == 1)
                                charset = "UTF-16"; // UTF-16 with BOM
                            else if (encoding == 2)
                                charset = "UTF-16BE"; // UTF-16 Big Endian

                            return new String(tagData, textPos, lyricsLength, charset).trim(); // 가사 텍스트 추출 완료!
                        }
                    }
                    pos += 10 + frameSize; // 다음 프레임으로 빠르게 건너뜁니다.
                }
            }
            raf.close();
        } catch (Exception e) {
        }
        return null;
    }

    public String getRepeatModeText(int mode) {
        switch (mode) {
            case 1:
                return "ONE";
            case 2:
                return "ALL";
            default:
                return "OFF";
        }
    }

    public void updatePlayerStatusIndicators() {
        try {
            boolean isCustomThemeVis = false;
            if (isVisualizerShowing && !ThemeManager.availableThemes.isEmpty()) {
                ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
                if (theme != null && theme.playerElements != null && !theme.playerElements.isEmpty()) {
                    isCustomThemeVis = true;
                }
            }

            // 1. 셔플 아이콘 세팅
            if (ivPlayerShuffleStatus != null) {
                ivPlayerShuffleStatus.setColorFilter(ThemeManager.getTextColorPrimary(), android.graphics.PorterDuff.Mode.SRC_IN);
                if (isShuffleMode && !isCustomThemeVis) {
                    ivPlayerShuffleStatus.setImageResource(R.drawable.ic_shuffle);
                    ivPlayerShuffleStatus.setVisibility(View.VISIBLE);
                } else {
                    ivPlayerShuffleStatus.setVisibility(View.GONE);
                }
            }
            if (ivPlayerRepeatStatus != null) {
                ivPlayerRepeatStatus.setColorFilter(ThemeManager.getTextColorPrimary(), android.graphics.PorterDuff.Mode.SRC_IN);
                if (!isCustomThemeVis && repeatMode == 1) { // 한 곡 반복
                    ivPlayerRepeatStatus.setImageResource(R.drawable.ic_repeat_one);
                    ivPlayerRepeatStatus.setVisibility(View.VISIBLE);
                } else if (!isCustomThemeVis && repeatMode == 2) { // 전곡 반복
                    ivPlayerRepeatStatus.setImageResource(R.drawable.ic_repeat);
                    ivPlayerRepeatStatus.setVisibility(View.VISIBLE);
                } else { // 반복 꺼짐
                    ivPlayerRepeatStatus.setVisibility(View.GONE);
                }
            }
            if (tvPlayerFavoriteStatus != null) {
                tvPlayerFavoriteStatus.setTextColor(ThemeManager.getTextColorPrimary());
                if (!isCustomThemeVis && !currentPlaylist.isEmpty()
                        && favoritePaths.contains(currentPlaylist.get(currentIndex).getAbsolutePath())) {
                    tvPlayerFavoriteStatus.setVisibility(View.VISIBLE);
                } else {
                    tvPlayerFavoriteStatus.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
        }
    }

    // 🚀 [신규 추가] 휠 버튼을 길게 누를 때 작동할 즐겨찾기 스위치 함수! (다른 함수들 사이에 넣으세요)
    private void toggleFavorite() {
        if (currentPlaylist.isEmpty())
            return;
        File currentSong = currentPlaylist.get(currentIndex);
        String path = currentSong.getAbsolutePath();

        if (favoritePaths.contains(path)) {
            favoritePaths.remove(path);
            Toast.makeText(this, t("Removed from Favorites."), Toast.LENGTH_SHORT).show();
        } else {
            favoritePaths.add(path);
            Toast.makeText(this, t("Added to Favorites."), Toast.LENGTH_SHORT).show();
        }

        try {
            prefs.edit().putStringSet("favorites", favoritePaths).commit(); // 즉시 영구 저장!
        } catch (Exception e) {
        }

        updatePlayerStatusIndicators(); // 💖 아이콘 새로고침
    }

    public void updatePlayerUI() {
        try {
            if (!currentPlaylist.isEmpty() && currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
                File currentFile = currentPlaylist.get(currentIndex);
                updateAudioQualityInfo(currentFile);

                // 🚀 곡이 바뀔 때마다 같은 이름의 .lrc 파일이 있는지 탐색합니다!
                loadLyrics(currentFile);
                refreshVisualizerState();
            }
            com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager.getInstance();

            if (am.isPlaying()) {
                ivAlbumArt.setAlpha(1.0f);
                ivPauseOverlay.setVisibility(View.GONE);
                progressHandler.post(updateProgressTask);
            } else {
                ivAlbumArt.setAlpha(0.4f);
                ivPauseOverlay.setVisibility(View.VISIBLE);
                progressHandler.removeCallbacks(updateProgressTask);
            }

            updateGlobalStatusPlayIcon();
            updatePlayerStatusIndicators(); // 💡 에러가 났던 그 함수! (이제 정상 작동합니다)
            // 🚀 [차량 블루투스 연동] 곡 정보와 재생/정지 상태를 실시간으로 차량에 쏴줍니다!
            sendBluetoothMetaToCar();
            updateBluetoothPlaybackState(am.isPlaying());

            // 🚀 [버그 완벽 픽스!] 하단 재생(Play) 버튼 아이콘 동기화!
            // 음악이 재생 중일 때는 '일시정지(pause)' 아이콘으로, 멈췄을 때는 '재생(play)' 아이콘으로 갈아끼웁니다.
            if (ivMenuPreview != null && btnNowPlaying != null && !btnNowPlaying.hasFocus()) {
                // 포커스가 안 가 있을 때만 아이콘(이미지)을 갈아끼웁니다.
                if (am.isPlaying()) {
                    ivMenuPreview.setImageResource(android.R.drawable.ic_media_pause); // ⏸ 정지 아이콘
                } else {
                    ivMenuPreview.setImageResource(android.R.drawable.ic_media_play); // ▶ 재생 아이콘
                }
            }

            // 🚀 [실시간 동기화] 메인 화면에서 나우 플레잉을 주시하고 있을 때 백그라운드에서 곡이 바뀌면 프리뷰 이미지도 실시간 리프레시!
            if (currentScreenState == STATE_MENU && ivWidgetFocusImage != null && tvFocusPreviewClock != null
                    && tvFocusPreviewClock.getVisibility() == View.VISIBLE) {
                for (ThemeManager.MenuElement el : ThemeManager.getCurrentTheme().menuElements) {
                    if ("OPEN_PLAYER".equals(el.action)) {
                        updateFocusPreviewLiveContent(el);
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    // 🚀 [버그 원인 제거 완료] 여기에 있던 불필요한 잉여 중괄호 '}' 하나를 완벽하게 삭제했습니다!
    private void adjustVolume(boolean up) {
        int stream = AudioManager.STREAM_MUSIC;
        try {
            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
            if (fm.isPowerUp) {
                // FM plays on STREAM_FM; MUSIC volume (esp. speaker device index) can be 0 on Y2.
                stream = fm.getFmStreamType();
            }
        } catch (Exception e) {
        }

        int currentVol = audioManager.getStreamVolume(stream);
        int maxVol = audioManager.getStreamMaxVolume(stream);
        if (up && currentVol < maxVol)
            currentVol++;
        else if (!up && currentVol > 0)
            currentVol--;
        audioManager.setStreamVolume(stream, currentVol, 0);

        // Keep MUSIC in sync for UI/widgets when adjusting FM.
        if (stream != AudioManager.STREAM_MUSIC) {
            try {
                int musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int musicVol = (int) (((float) currentVol / Math.max(1, maxVol)) * musicMax);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, musicVol, 0);
            } catch (Exception e) {
            }
        }

        showDynamicVolumeOverlay();
    }

    private void showDynamicVolumeOverlay() {
        int stream = AudioManager.STREAM_MUSIC;
        try {
            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
            if (fm.isPowerUp) {
                stream = fm.getFmStreamType();
            }
        } catch (Exception e) {
        }
        int currentVol = audioManager.getStreamVolume(stream);
        int maxVol = audioManager.getStreamMaxVolume(stream);
        layoutVolumeOverlay.setVisibility(View.VISIBLE);
        volumeProgress.setMax(maxVol);
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
        // [🟢 onKeyDown 함수 시작 부분에 최우선적으로 추가!]
        // 🚀 [휠 순서 이동 인터셉터] 메뉴 이동 모드가 켜져있을 때 휠을 돌리면 화면 스크롤 대신 '블록 자체'를 위아래로 끌고 다닙니다!
        if (currentScreenState == STATE_SETTINGS && settingsSubMode == 4 && isMenuReorderMode
                && currentReorderRow != null) {
            if (keyCode == 21) { // 휠 위로 (UP)
                int idx = containerSettingsItems.indexOfChild(currentReorderRow);
                if (idx > 0) {
                    containerSettingsItems.removeViewAt(idx);
                    containerSettingsItems.addView(currentReorderRow, idx - 1);
                    currentReorderRow.requestFocus();
                    clickFeedback();
                    if (containerSettingsItems.getParent() instanceof ScrollView) {
                        ((ScrollView) containerSettingsItems.getParent())
                                .requestChildFocus(containerSettingsItems, currentReorderRow);
                    }
                }
                return true;
            }
            if (keyCode == 22) { // 휠 아래로 (DOWN)
                int idx = containerSettingsItems.indexOfChild(currentReorderRow);
                if (idx < containerSettingsItems.getChildCount() - 1) {
                    containerSettingsItems.removeViewAt(idx);
                    containerSettingsItems.addView(currentReorderRow, idx + 1);
                    currentReorderRow.requestFocus();
                    clickFeedback();
                    if (containerSettingsItems.getParent() instanceof ScrollView) {
                        ((ScrollView) containerSettingsItems.getParent())
                                .requestChildFocus(containerSettingsItems, currentReorderRow);
                    }
                }
                return true;
            }
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = true;
        try {
            if (Build.VERSION.SDK_INT >= 20)
                isScreenOn = pm.isInteractive();
            else
                isScreenOn = pm.isScreenOn();
        } catch (Exception e) {
        }

        boolean isWakingUp = !isScreenOn || ((event.getFlags() & KeyEvent.FLAG_WOKE_HERE) != 0)
                || (System.currentTimeMillis() - lastScreenOnTime < 500);

        if (isWakingUp) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.getRepeatCount() == 0) {
                    event.startTracking();
                }
                return true;
            }

            // 🚀 [스크린 오프 컨트롤 라디오 인터셉터 삽입]
            if (isScreenOffControlEnabled && activePlayer == 1) {
                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
                    tuneToNextSavedRadioChannel(true);
                    clickFeedback();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
                    tuneToNextSavedRadioChannel(false);
                    clickFeedback();
                    return true;
                }
            }

            if (isScreenOffControlEnabled && currentScreenState == STATE_PLAYER) {
                if (keyCode == 21) {
                    // 🚀 방어막: 곡 넘김 직후 0.3초(300ms) 안에는 볼륨 조절을 차단합니다!
                    if (System.currentTimeMillis() - lastTrackChangeTime > 300) {
                        adjustVolume(false);
                        clickFeedback();
                    }
                    return true;
                }
                if (keyCode == 22) {
                    if (System.currentTimeMillis() - lastTrackChangeTime > 300) {
                        adjustVolume(true);
                        clickFeedback();
                    }
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
                    if (event.getRepeatCount() == 0) {
                        event.startTracking();
                        isSeekPerformed = false;
                    } else {
                        long now = System.currentTimeMillis();
                        if (now - lastSeekTime > 300) {
                            isSeekPerformed = true;
                            lastSeekTime = now;
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().seekRelative(-10000); // -10초 점프
                            clickFeedback();
                        }
                    }
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {

                    if (event.getRepeatCount() == 0) {
                        event.startTracking(); // 감시 시작
                        isSeekPerformed = false;
                    } else {
                        // 🚀 버튼을 떼지 않고 계속 꾹 누르고 있을 때 (0.3초마다 10초씩 계속 점프!)
                        long now = System.currentTimeMillis();
                        if (now - lastSeekTime > 300) {
                            isSeekPerformed = true;
                            lastSeekTime = now;
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().seekRelative(10000); // +10초 점프
                            clickFeedback(); // 드르륵 거리는 햅틱 반응
                        }
                    }
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == 86) {

                    // com.themoon.y1.managers.AudioPlayerManager.getInstance().playOrPauseMusic();
                    // clickFeedback();
                    return true;
                }
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.getRepeatCount() == 0) {
                event.startTracking(); // 🚀 [핵심 기술] 길게 누르는지 감시(추적)를 시작합니다!
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == 86 || keyCode == 126 || keyCode == 127) {
            // 🚀 [신경망 센서 작동] 누른 시간 기록 시작!
            if (event.getRepeatCount() == 0) {
                isBottomButtonDown = true;
                bottomButtonDownTime = System.currentTimeMillis();
                event.startTracking();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
            // 버튼을 처음 눌렀을 때
            if (event.getRepeatCount() == 0) {
                event.startTracking(); // 감시 시작
                isSeekPerformed = false;
            } else {
                // 🚀 버튼을 떼지 않고 계속 꾹 누르고 있을 때 (0.3초마다 10초씩 계속 점프!)
                long now = System.currentTimeMillis();
                if (now - lastSeekTime > 300) {
                    isSeekPerformed = true;
                    lastSeekTime = now;
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().seekRelative(10000); // +10초 점프
                    clickFeedback(); // 드르륵 거리는 햅틱 반응
                }
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
            if (event.getRepeatCount() == 0) {
                event.startTracking();
                isSeekPerformed = false;
            } else {
                long now = System.currentTimeMillis();
                if (now - lastSeekTime > 300) {
                    isSeekPerformed = true;
                    lastSeekTime = now;
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().seekRelative(-10000); // -10초 점프
                    clickFeedback();
                }
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
                if (currentKeyboardMode == 1) {
                    currentKeyboardMode = 0; // 모드 초기화
                    changeScreen(STATE_BROWSER); // 다시 팟캐스트 화면으로 복귀!
                    return true;
                }
                changeScreen(STATE_WIFI);
                clickFeedback();
                return true;
            }
            return true;
        }

        if (currentScreenState == STATE_PLAYER) {
            // 🚀 [셔플/반복 팝업 전용 조향 장치] 팝업창이 열려 있을 땐 휠과 백 버튼을 팝업이 최우선으로 제어합니다!
            if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getVisibility() == View.VISIBLE) {
                if (keyCode == 21 || keyCode == 19 || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (btnOverlayShuffle != null) {
                        btnOverlayShuffle.requestFocus();
                        clickFeedback();
                    }
                    return true;
                }
                if (keyCode == 22 || keyCode == 20 || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (btnOverlayRepeat != null) {
                        btnOverlayRepeat.requestFocus();
                        clickFeedback();
                    }
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                    layoutShuffleRepeatOverlay.setVisibility(View.GONE);
                    clickFeedback();
                    return true;
                }
                return true; // 팝업창이 떠 있을 땐 볼륨 조절 등 다른 키 조작 차단!
            }

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
                // 🚀 [복귀 경로 지정] 무조건 브라우저가 아니라, 방금 출발했던 화면으로 정확히 돌아갑니다!
                changeScreen(backTargetForPlayer);
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
                // 🚀 [복귀 경로 지정]
                changeScreen(backTargetForUtility);
                clickFeedback();
                return true;
            }
            return true;
        }

        if (currentScreenState == STATE_STORAGE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // 🚀 [복귀 경로 지정]
                changeScreen(backTargetForUtility);
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
                            .setTitle(t("Server is Running"))
                            .setMessage(
                                    t("The Web Server is still active. Do you want to shut it down completely and exit?"))
                            .setPositiveButton(t("Stop Server"), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    toggleWebServer();
                                    changeScreen(backTargetForUtility); // 🚀 복귀!
                                }
                            })
                            .setNegativeButton(t("Keep Running"), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    changeScreen(backTargetForUtility); // 🚀 복귀!
                                }
                            })
                            .show();
                } else {
                    changeScreen(backTargetForUtility); // 🚀 복귀!
                }
                return true;
            }
        }

        if (currentScreenState == STATE_MENU || currentScreenState == STATE_BROWSER
                || currentScreenState == STATE_SETTINGS || currentScreenState == STATE_BLUETOOTH
                || currentScreenState == STATE_WIFI) {

            // 🚀 [순정 커버 플로우 휠 조작 대개조 완료]
            if (currentScreenState == STATE_BROWSER && currentBrowserMode == BROWSER_COVER_FLOW) {
                if (keyCode == 21) { // 휠 위로(왼쪽) 돌릴 때
                    scrollCoverFlow(false);
                    clickFeedback();
                    return true;
                }
                if (keyCode == 22) { // 휠 아래로(오른쪽) 돌릴 때
                    scrollCoverFlow(true);
                    clickFeedback();
                    return true;
                }
            }

            // (기존 코드 유지) 🚀 기존 BACK키와 더불어 상단 버튼(19)을 누르면 무조건 한 단계 뒤로...
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 19) {
                clickFeedback();
                if (currentScreenState == STATE_BROWSER) {
                    if (isPickingBackground) {
                        // 🚀 [최상단 방어막] 배경 화면 선택 시, 음악 폴더뿐만 아니라 SD카드 최상단에서도 더 이상 못
                        // 올라가게 막습니다!
                        if (currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())
                                || StoragePaths.isAnyStorageRoot(currentFolder.getAbsolutePath())) {
                            isPickingBackground = false;
                            changeScreen(STATE_SETTINGS); // 💡 엉뚱한 메인 메뉴가 아니라, 배경 화면 설정창으로 안전하게 복귀!
                            buildBackgroundSettingsUI();
                        } else {
                            currentFolder = currentFolder.getParentFile();
                            // 🚀 [2중 방어] 혹시라도 시스템 에러로 경로가 null이 되더라도 앱이 터지지 않고 탈출하도록 보호
                            if (currentFolder == null) {
                                isPickingBackground = false;
                                isNavigatingToSubMenu = true;
                                changeScreen(STATE_SETTINGS);
                                buildBackgroundSettingsUI();
                            } else {
                                buildFileBrowserUI();
                            }
                        }
                    } else {
                        // 💡 [버그 완벽 수정] 내가 방금 어떤 방에서 나왔는지 텍스트를 정확히 기억(lastBrowserFocusText)해 둡니다!
                        if (currentBrowserMode == BROWSER_ROOT) {
                            changeScreen(STATE_MENU);
                        } else if (currentBrowserMode == BROWSER_COVER_FLOW) {
                            // 🚀 [지능형 다이렉트 퇴근 센서 장착]
                            // 직전 브라우저 메뉴의 기억(lastBrowserFocusText)이 없다면? 메인 메뉴에서 바로 들어온 것입니다!
                            if (lastBrowserFocusText == null || lastBrowserFocusText.trim().isEmpty()) {
                                changeScreen(STATE_MENU); // 🟢 다른 숏컷들처럼 메인 화면으로 즉시 다이렉트 복귀!
                            } else {
                                // 라이브러리 메뉴를 거쳐서 들어왔던 정석 경로라면 원래대로 부모 메뉴로 복귀
                                currentBrowserMode = BROWSER_ROOT;
                                buildFileBrowserUI();
                            }
                        } else if (currentBrowserMode == BROWSER_FOLDER) {
                            // 🚀 [버그 수정] 현재 모드(음악/오디오북)에 맞춰서 최상위 폴더에 도달했는지 지능적으로 체크합니다!
                            boolean isAtFolderRoot = false;
                            if (isAudiobookLibraryMode) {
                                if (currentFolder.getAbsolutePath().equals(audiobookRootFolder.getAbsolutePath())) {
                                    isAtFolderRoot = true;
                                }
                            } else {
                                if (currentFolder.getAbsolutePath().equals(rootFolder.getAbsolutePath())) {
                                    isAtFolderRoot = true;
                                }
                            }

                            // 모드별 최상위 폴더이거나 기기 전체 루트 폴더일 때 뒤로 가기를 누르면 라이브러리 메인(BROWSER_ROOT)으로 복귀!
                            if (isAtFolderRoot || StoragePaths.isAnyStorageRoot(currentFolder.getAbsolutePath())) {
                                currentBrowserMode = BROWSER_ROOT;
                                lastBrowserFocusText = t("Folders");
                                buildFileBrowserUI();
                            } else {
                                String exitedName = currentFolder.getName(); // 나온 폴더 이름 기억!
                                currentFolder = currentFolder.getParentFile();
                                if (currentFolder == null) {
                                    changeScreen(STATE_MENU);
                                } else {
                                    lastBrowserFocusText = exitedName;
                                    buildFileBrowserUI();
                                }
                            }
                        } else if (currentBrowserMode == BROWSER_VIRTUAL_SONGS) {
                            if (virtualQueryType.equals("COVER_FLOW_ALBUM")) {
                                currentBrowserMode = BROWSER_COVER_FLOW;
                                buildCoverFlowUI();
                            } else {
                                // 🚀 [뒤로가기 길 안내 완벽 수리!]
                                // 이전에는 ARTIST와 ALBUM만 있었지만, 이제 ARTIST_ALBUM 노선도 정확히 안내합니다.
                                if (virtualQueryType.equals("ALL") || virtualQueryType.equals("RECENT")) {
                                    currentBrowserMode = BROWSER_ROOT;
                                    lastBrowserFocusText = isAudiobookLibraryMode ? t("All Audiobooks") : t("All Songs");
                                    buildFileBrowserUI();
                                } else if (virtualQueryType.equals("ARTIST")) {
                                    currentBrowserMode = BROWSER_ARTISTS;
                                    buildVirtualCategories("ARTIST");
                                } else if (virtualQueryType.equals("ALBUM")) {
                                    currentBrowserMode = BROWSER_ALBUMS;
                                    buildVirtualCategories("ALBUM");
                                } else if (virtualQueryType.equals("ARTIST_ALBUM")) {
                                    currentBrowserMode = BROWSER_ARTIST_ALBUMS; // 17번
                                    buildVirtualCategories("ARTIST_ALBUM");
                                } else if (virtualQueryType.equals("YEAR_ARTIST_ALBUM")) {
                                    currentBrowserMode = BROWSER_YEAR_ALBUMS;
                                    virtualQueryValue = artistForAlbumFilter;
                                    buildVirtualCategories("YEAR_ARTIST_ALBUM");
                                } else if (virtualQueryType.equals("YEAR_ARTIST")) {
                                    currentBrowserMode = BROWSER_YEAR_ARTISTS;
                                    virtualQueryValue = yearOrGenreFilter;
                                    buildVirtualCategories("YEAR_ARTIST");
                                } else if (virtualQueryType.equals("GENRE_ARTIST_ALBUM")) {
                                    currentBrowserMode = BROWSER_GENRE_ALBUMS;
                                    virtualQueryValue = artistForAlbumFilter;
                                    buildVirtualCategories("GENRE_ARTIST_ALBUM");
                                } else if (virtualQueryType.equals("GENRE_ARTIST")) {
                                    currentBrowserMode = BROWSER_GENRE_ARTISTS;
                                    virtualQueryValue = yearOrGenreFilter;
                                    buildVirtualCategories("GENRE_ARTIST");
                                } else {
                                    currentBrowserMode = BROWSER_ROOT;
                                    buildFileBrowserUI();
                                }
                            }
                        } else if (currentBrowserMode == BROWSER_GAMES) {
                            currentBrowserMode = BROWSER_ROOT;
                            lastBrowserFocusText = t("Game");
                            buildFileBrowserUI();
                        } else if (currentBrowserMode == BROWSER_ARTISTS) {
                            currentBrowserMode = BROWSER_ROOT;
                            // 🚀 [오디오북 포커스 버그 수리]
                            lastBrowserFocusText = isAudiobookLibraryMode ? t("Authors") : t("Artists");
                            buildFileBrowserUI();
                        } else if (currentBrowserMode == BROWSER_ARTIST_ALBUMS) {
                            currentBrowserMode = BROWSER_ARTISTS;
                            // 🚀 [포커스 복구 핵심 수리]
                            // 리스트뷰(ListView)는 lastBrowserFocusText가 아니라 'virtualQueryValue'를 보고 포커스를 찾습니다!
                            virtualQueryValue = artistForAlbumFilter;
                            buildVirtualCategories("ARTIST");
                            // =========================================================
                        } else if (currentBrowserMode == BROWSER_YEAR_ALBUMS) {
                            currentBrowserMode = BROWSER_YEAR_ARTISTS;
                            virtualQueryValue = artistForAlbumFilter;
                            buildVirtualCategories("YEAR_ARTIST");
                        } else if (currentBrowserMode == BROWSER_YEAR_ARTISTS) {
                            currentBrowserMode = BROWSER_YEARS;
                            virtualQueryValue = yearOrGenreFilter;
                            buildVirtualCategories("YEAR");
                        } else if (currentBrowserMode == BROWSER_GENRE_ALBUMS) {
                            currentBrowserMode = BROWSER_GENRE_ARTISTS;
                            virtualQueryValue = artistForAlbumFilter;
                            buildVirtualCategories("GENRE_ARTIST");
                        } else if (currentBrowserMode == BROWSER_GENRE_ARTISTS) {
                            currentBrowserMode = BROWSER_GENRES;
                            virtualQueryValue = yearOrGenreFilter;
                            buildVirtualCategories("GENRE");
                        }else if (currentBrowserMode == BROWSER_FAVORITES) {
                            currentBrowserMode = BROWSER_ROOT;
                            lastBrowserFocusText = t("My Favorites");
                            buildFileBrowserUI();
                        } else if (currentBrowserMode == BROWSER_AUDIOBOOKS) {
                            currentBrowserMode = BROWSER_ROOT;
                            // 🚀 오디오북에서 뮤직으로 돌아갈 때의 방어막
                            lastBrowserFocusText = t("Switch to Audiobooks");
                            buildFileBrowserUI();
                        } else if (currentBrowserMode == BROWSER_PLAYLISTS) {
                            // 🚀 [플레이리스트 탈출 버그 완벽 수리]
                            // 꼬리표 텍스트에 의존하지 않고, 절대 지워지지 않는 '스위치 플래그'를 통해 정확한 경로로 퇴근시킵니다!
                            if (!isPlaylistOpenedFromLibrary) {
                                applyThemeToMainMenu();
                                changeScreen(STATE_MENU);
                            } else {
                                currentBrowserMode = BROWSER_ROOT;
                                lastBrowserFocusText = t("Playlists");
                                buildFileBrowserUI();
                            }
                        } else if (currentBrowserMode == BROWSER_M3U_SONGS) {
                            currentBrowserMode = BROWSER_PLAYLISTS;
                            if (currentM3uFile != null) {
                                lastBrowserFocusText = currentM3uFile.getName().substring(0,
                                        currentM3uFile.getName().lastIndexOf("."));
                            }
                            buildM3uPlaylistUI();
                        } else if (currentBrowserMode == BROWSER_ALBUMS) {
                            currentBrowserMode = BROWSER_ROOT;
                            // 🚀 [오디오북 포커스 버그 수리]
                            lastBrowserFocusText = isAudiobookLibraryMode ? t("Books") : t("Albums");
                            buildFileBrowserUI();
                        }
                        // 🚀 [여기에 아래 코드를 추가하여 퇴근 경로를 뚫어줍니다!]
                        else if (currentBrowserMode == BROWSER_YEARS) {
                            currentBrowserMode = BROWSER_ROOT;
                            lastBrowserFocusText = t("Years"); // 💡 나갔을 때 휠 포커스가 자동으로 'Years' 버튼에 록온되도록 세팅!
                            buildFileBrowserUI();
                        } else if (currentBrowserMode == BROWSER_GENRES) {
                            currentBrowserMode = BROWSER_ROOT;
                            lastBrowserFocusText = t("Genres"); // 💡 나갔을 때 휠 포커스가 자동으로 'Genres' 버튼에 록온되도록 세팅!
                            buildFileBrowserUI();
                        }
                        // 🚀 [신규 장착] 최근 추가된 곡 목록에서 나갈 때의 복귀 경로!
                        else if (currentBrowserMode == BROWSER_RECENTLY_ADDED) {
                            currentBrowserMode = BROWSER_ROOT;
                            lastBrowserFocusText = t("Recently Added");
                            buildFileBrowserUI();
                        }
                        // 🚀 [팟캐스트 탈출구 추가!]
                        else if (currentBrowserMode == BROWSER_PODCAST_CHANNELS) {
                            clickFeedback();
                            if (isPodcastOpenedFromLibrary) {
                                isPodcastOpenedFromLibrary = false;
                                lastBrowserFocusText = t("Podcasts");
                                currentBrowserMode = BROWSER_ROOT;
                                buildFileBrowserUI();
                            } else {
                                changeScreen(STATE_MENU);
                            }
                        } else if (currentBrowserMode == BROWSER_PODCAST_EPISODES) {
                            currentBrowserMode = BROWSER_PODCAST_CHANNELS;
                            buildPodcastChannelsUI();
                        } else if (currentBrowserMode == BROWSER_PODCAST_MANAGE) {
                            clickFeedback();
                            currentBrowserMode = BROWSER_PODCAST_CHANNELS;
                            buildPodcastChannelsUI();
                        }
                        // =======================================================
                        // 🚀 [여기서부터 덮어쓰기] 비디오 브라우저 지능형 탈출 & 포커스 복구 엔진!
                        // =======================================================
                        else if (currentBrowserMode == BROWSER_VIDEOS) {
                            clickFeedback();

                            // 💡 1. 비디오 폴더 내에서 하위 폴더에 들어와 있다면? 상위 폴더로 한 칸 올라갑니다!
                            if (!StoragePaths.isVideosRoot(currentFolder.getAbsolutePath()) &&
                                    !StoragePaths.isAnyStorageRoot(currentFolder.getAbsolutePath())) {
                                lastBrowserFocusText = currentFolder.getName();
                                currentFolder = currentFolder.getParentFile();
                                buildFileBrowserUI();
                            }
                            // 💡 2. 비디오 최상단 루트 폴더에서 뒤로 가기를 눌러 완전 탈출할 때!
                            else {
                                if (isVideoOpenedFromLibrary) {
                                    isVideoOpenedFromLibrary = false;
                                    currentBrowserMode = BROWSER_ROOT;
                                    // 🎯 [핵심 버그 해결] 버튼 이름과 100% 똑같이 "Video Browser"를 쥐여줍니다!
                                    lastBrowserFocusText = t("Video Browser");
                                    buildFileBrowserUI();
                                } else {
                                    // 메인 화면에서 다이렉트 숏컷으로 들어왔으면 메인으로 복귀!
                                    changeScreen(STATE_MENU);
                                }
                            }
                        }
                    }
                } else if (currentScreenState == STATE_BLUETOOTH || currentScreenState == STATE_WIFI) {
                    changeScreen(backTargetForUtility);
                } else if (currentScreenState == STATE_SETTINGS) {
                    if (isMenuReorderMode) {
                        toggleMenuReorderMode();
                        return true;
                    }
                    // 🚀 [1단계] 라디오 설정 서브 페이지 모드라면, 라디오 메인 플레이어 모드로 먼저 탈출!
                    if (isRadioUIShowing && isRadioSettingsMode) {
                        isRadioSettingsMode = false;
                        isRadioAdjustingFreq = false;
                        buildRadioUI();
                        clickFeedback();
                        return true;
                    }

                    // 🚀 [버그 완벽 해결] 라디오 메인 플레이어 화면에서 뒤로 가기를 누르면 홈(메인) 화면으로 즉시 순간 이동!
                    if (isRadioUIShowing) {
                        isRadioUIShowing = false;
                        isRadioSettingsMode = false;
                        isRadioAdjustingFreq = false;
                        applyThemeToMainMenu(); // 🚀 추가! 라디오에서 메인 복귀 시 갱신
                        changeScreen(STATE_MENU);
                        clickFeedback();
                        return true;
                    }

                    isRadioUIShowing = false;

                    // 🚀 [라우팅 정화 완벽 복구] 깊이(Depth)를 파악하여 알맞은 상위 메뉴로 완벽하게 돌아갑니다!
                    if (currentSettingsDepth == 0) {
                        applyThemeToMainMenu();
                        changeScreen(STATE_MENU);
                    } else if (currentSettingsDepth == 1) {
                        buildSettingsUI();
                    } else if (currentSettingsDepth == 2) {
                        if (isRadioUIShowing) {
                            buildRadioUI();
                        } else {
                            com.themoon.y1.managers.SettingsMenuManager.getInstance(MainActivity.this).restoreSubMenu();
                        }
                    } else if (currentSettingsDepth == 3) {
                        if (settingsSubMode == 2 || settingsSubMode == 3) {
                            buildEqualizerSettingsUI();
                        } else {
                            // 🚀 [수정] DateTimeSelector 등 depth 3 서브메뉴는 부모 화면(depth 2)으로 복귀
                            buildDateTimeUI();
                        }
                    }
                    clickFeedback();
                    return true;
                }
                return true;
            }
            // 🚀 [여기서부터 덮어쓰기!] 초고속 리스트뷰가 켜져있을 때는, 시스템 본연의 부드러운 스크롤 엔진에 휠 신호를 넘깁니다!
            if (currentScreenState == STATE_BROWSER && listVirtualSongs != null
                    && listVirtualSongs.getVisibility() == View.VISIBLE) {

                long now = System.currentTimeMillis();
                if (now - lastWheelTime < 40 && wheelFastCount < 2) {
                    lastWheelTime = now;
                    return true;
                }
                boolean isFastScroll = false;

                // 💡 [오토매틱 엔진] 0.05초(50ms) 이내에 연속으로 휠이 3칸 이상 돌아가면 '고속 점프 모드' 발동!
                if (now - lastWheelTime < 50) {
                    wheelFastCount++;
                    if (wheelFastCount >= 3)
                        isFastScroll = true;
                } else {
                    wheelFastCount = 0; // 천천히 돌리면 즉시 초기화
                }
                lastWheelTime = now;

                if (isFastScroll && !currentScrollIndexList.isEmpty()) {
                    // 🚀🚀 [고속 점프 모드] 알파벳(첫 글자) 단위로 뭉텅뭉텅 스크롤!
                    int currentPos = listVirtualSongs.getSelectedItemPosition();
                    if (currentPos < 0)
                        currentPos = 0;
                    char currentChar = getInitialChar(currentScrollIndexList.get(currentPos));
                    int targetPos = currentPos;

                    if (keyCode == 22) { // 휠 아래로 휙! 돌릴 때 (다음 알파벳 찾기)
                        for (int i = currentPos + 1; i < currentScrollIndexList.size(); i++) {
                            if (getInitialChar(currentScrollIndexList.get(i)) != currentChar) {
                                targetPos = i;
                                break;
                            }
                        }
                    } else if (keyCode == 21) { // 휠 위로 휙! 돌릴 때 (이전 알파벳 시작점 찾기)
                        char targetChar = currentChar;
                        boolean foundPrevChar = false;
                        for (int i = currentPos - 1; i >= 0; i--) {
                            char c = getInitialChar(currentScrollIndexList.get(i));
                            if (!foundPrevChar && c != currentChar) {
                                foundPrevChar = true;
                                targetChar = c;
                            }
                            if (foundPrevChar && c != targetChar) {
                                targetPos = i + 1;
                                break;
                            }
                            if (i == 0)
                                targetPos = 0;
                        }
                    }
                    listVirtualSongs.setSelection(targetPos);
                    clickFeedback();
                    return true;
                } else {
                    // 🐢🐢 [일반 주행 모드] 평소처럼 천천히 정확하게 1곡씩 이동!
                    if (keyCode == 21) {
                        int currentPos = listVirtualSongs.getSelectedItemPosition();
                        View focusedChild = listVirtualSongs.getFocusedChild();
                        if (focusedChild != null) currentPos = listVirtualSongs.getPositionForView(focusedChild);

                        if (currentPos <= 0) {
                            // 🚀 [루프 스크롤]
                            if (isLoopScrollOn) {
                                final int lastPos = listVirtualSongs.getCount() - 1;
                                listVirtualSongs.setSelection(lastPos);
                                listVirtualSongs.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int visiblePos = lastPos - listVirtualSongs.getFirstVisiblePosition();
                                        if (visiblePos >= 0 && visiblePos < listVirtualSongs.getChildCount()) {
                                            listVirtualSongs.getChildAt(visiblePos).requestFocus();
                                        }
                                    }
                                });
                            }
                        } else {
                            final int prevPos = currentPos - 1;
                            int vis = prevPos - listVirtualSongs.getFirstVisiblePosition();

                            // 💡 위에 있는 항목이 완전히 화면에 보일 때는 평범하게 포커스만!
                            if (vis >= 0 && vis < listVirtualSongs.getChildCount()) {
                                listVirtualSongs.getChildAt(vis).requestFocus();
                            } else {
                                // 🚀 [스크롤 널뛰기 버그 수리 완료!] 높이를 빼지 않고 현재 Y좌표를 그대로 물려줍니다.
                                int shiftOffset = (focusedChild != null) ? focusedChild.getTop() : 0;
                                listVirtualSongs.setSelectionFromTop(prevPos, shiftOffset);
                                listVirtualSongs.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int v = prevPos - listVirtualSongs.getFirstVisiblePosition();
                                        if (v >= 0 && v < listVirtualSongs.getChildCount()) {
                                            listVirtualSongs.getChildAt(v).requestFocus();
                                        }
                                    }
                                });
                            }
                        }
                        clickFeedback();
                        return true;
                    }

                    if (keyCode == 22) {
                        int currentPos = listVirtualSongs.getSelectedItemPosition();
                        View focusedChild = listVirtualSongs.getFocusedChild();
                        if (focusedChild != null) currentPos = listVirtualSongs.getPositionForView(focusedChild);

                        if (currentPos >= listVirtualSongs.getCount() - 1) {
                            // 🚀 [루프 스크롤]
                            if (isLoopScrollOn) {
                                listVirtualSongs.setSelection(0);
                                listVirtualSongs.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (listVirtualSongs.getChildCount() > 0)
                                            listVirtualSongs.getChildAt(0).requestFocus();
                                    }
                                });
                            }
                        } else {
                            final int nextPos = currentPos + 1;
                            int vis = nextPos - listVirtualSongs.getFirstVisiblePosition();

                            // 💡 다음 항목이 '완전히' 보일 때만 포커스 이동! (맨 밑에 반쯤 걸친 항목은 강제 스크롤시킴)
                            if (vis >= 0 && vis < listVirtualSongs.getChildCount() - 1) {
                                listVirtualSongs.getChildAt(vis).requestFocus();
                            } else {
                                // 🚀 [스크롤 널뛰기 버그 수리 완료!] 다음 곡이 현재 곡의 정확한 위치(Y좌표)를 뺏어오도록 고정!
                                int shiftOffset = (focusedChild != null) ? focusedChild.getTop() : 0;
                                listVirtualSongs.setSelectionFromTop(nextPos, shiftOffset);
                                listVirtualSongs.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int v = nextPos - listVirtualSongs.getFirstVisiblePosition();
                                        if (v >= 0 && v < listVirtualSongs.getChildCount()) {
                                            listVirtualSongs.getChildAt(v).requestFocus();
                                        }
                                    }
                                });
                            }
                        }
                        clickFeedback();
                        return true;
                    }
                }
            }
            View c = getCurrentFocus();
            if (c != null) {
                if (keyCode == 21) { // 휠 위로 돌릴 때 (UP)

                    // 🚀 [라디오 휠 조작] 깜빡임 완벽 제거 버전
                    if (currentScreenState == STATE_SETTINGS && isRadioUIShowing) {
                        if (!isRadioSettingsMode) {
                            adjustVolume(false);
                            return true;
                        } else if (isRadioAdjustingFreq) {
                            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager
                                    .getInstance(this);
                            float newFreq = fm.currentFreq - 0.1f;
                            if (newFreq < 87.5f)
                                newFreq = 108.0f;
                            if (fm.isPowerUp)
                                fm.tune(newFreq);
                            else
                                fm.currentFreq = newFreq;
                            showRadioFreqPopup(newFreq);
                            buildRadioUI();
                            return true;
                        }
                    }

                    // 🚀 [메인 메뉴 완벽 제어] 메인 화면에서는 무조건 우리가 조립한 인덱스 순서대로 포커스를 강제 이동시킵니다!
                    if (currentScreenState == STATE_MENU) {
                        int targetId = c.getNextFocusUpId();
                        if (targetId != View.NO_ID) {
                            View target = findViewById(targetId);
                            if (target != null) {
                                target.requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                    } else {
                        ViewGroup parent = (ViewGroup) c.getParent();
                        if (parent instanceof LinearLayout) {
                            int index = parent.indexOfChild(c);
                            boolean moved = false;
                            for (int i = index - 1; i >= 0; i--) {
                                View n = parent.getChildAt(i);
                                if (n != null && n.getVisibility() == View.VISIBLE && n.isFocusable()) {
                                    n.requestFocus();
                                    moved = true;
                                    break;
                                }
                            }
                            if (!moved && isLoopScrollOn) {
                                for (int i = parent.getChildCount() - 1; i > index; i--) {
                                    View n = parent.getChildAt(i);
                                    if (n != null && n.getVisibility() == View.VISIBLE && n.isFocusable()) {
                                        n.requestFocus();
                                        break;
                                    }
                                }
                            }
                        } else {
                            int targetId = c.getNextFocusUpId();
                            if (targetId != View.NO_ID) {
                                View target = findViewById(targetId);
                                if (target != null) {
                                    target.requestFocus();
                                    clickFeedback();
                                    return true;
                                }
                            }
                            View n = c.focusSearch(View.FOCUS_UP);
                            if (n != null)
                                n.requestFocus();
                        }
                    }
                    clickFeedback();
                    return true;
                }
                if (keyCode == 22) { // 휠 아래로 돌릴 때 (DOWN)

                    // 🚀 [라디오 휠 조작] 깜빡임 완벽 제거 버전
                    if (currentScreenState == STATE_SETTINGS && isRadioUIShowing) {
                        if (!isRadioSettingsMode) {
                            adjustVolume(true);
                            return true;
                        } else if (isRadioAdjustingFreq) {
                            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager
                                    .getInstance(this);
                            float newFreq = fm.currentFreq + 0.1f;
                            if (newFreq > 108.0f)
                                newFreq = 87.5f;
                            if (fm.isPowerUp)
                                fm.tune(newFreq);
                            else
                                fm.currentFreq = newFreq;
                            showRadioFreqPopup(newFreq);
                            buildRadioUI();
                            return true;
                        }
                    }

                    // 🚀 [메인 메뉴 완벽 제어] 메인 화면에서는 무조건 우리가 조립한 인덱스 순서대로 포커스를 강제 이동시킵니다!
                    if (currentScreenState == STATE_MENU) {
                        int targetId = c.getNextFocusDownId();
                        if (targetId != View.NO_ID) {
                            View target = findViewById(targetId);
                            if (target != null) {
                                target.requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                    } else {
                        ViewGroup parent = (ViewGroup) c.getParent();
                        if (parent instanceof LinearLayout) {
                            int index = parent.indexOfChild(c);
                            boolean moved = false;
                            for (int i = index + 1; i < parent.getChildCount(); i++) {
                                View n = parent.getChildAt(i);
                                if (n != null && n.getVisibility() == View.VISIBLE && n.isFocusable()) {
                                    n.requestFocus();
                                    moved = true;
                                    break;
                                }
                            }
                            if (!moved && isLoopScrollOn) {
                                for (int i = 0; i < index; i++) {
                                    View n = parent.getChildAt(i);
                                    if (n != null && n.getVisibility() == View.VISIBLE && n.isFocusable()) {
                                        n.requestFocus();
                                        break;
                                    }
                                }
                            }
                        } else {
                            int targetId = c.getNextFocusDownId();
                            if (targetId != View.NO_ID) {
                                View target = findViewById(targetId);
                                if (target != null) {
                                    target.requestFocus();
                                    clickFeedback();
                                    return true;
                                }
                            }
                            View n = c.focusSearch(View.FOCUS_DOWN);
                            if (n != null)
                                n.requestFocus();
                        }
                    }
                    clickFeedback();
                    return true;
                }
            } else {
                // 🚀 [포커스 증발 무한 루프 버그 완벽 수리!]
                // 블루투스 연결 등 시스템 팝업으로 인해 포커스가 일시적으로 증발(null)했을 때,
                // 보이지 않는 메인 메뉴 버튼(10000)에 갇히는 현상을 완벽하게 방어하고 현재 화면에 맞게 포커스를 살려냅니다!
                if (keyCode == 21 || keyCode == 22) {
                    if (currentScreenState == STATE_MENU) {
                        View firstBtn = findViewById(10000);
                        if (firstBtn != null && firstBtn.getVisibility() == View.VISIBLE) firstBtn.requestFocus();
                        else if (btnNowPlaying != null) btnNowPlaying.requestFocus();
                    } else if (currentScreenState == STATE_BROWSER) {
                        if (listVirtualSongs != null && listVirtualSongs.getVisibility() == View.VISIBLE && listVirtualSongs.getChildCount() > 0) {
                            listVirtualSongs.requestFocus();
                        } else if (containerBrowserItems != null && containerBrowserItems.getChildCount() > 0) {
                            containerBrowserItems.getChildAt(0).requestFocus();
                        }
                    } else if (currentScreenState == STATE_SETTINGS) {
                        if (containerSettingsItems != null && containerSettingsItems.getChildCount() > 0) {
                            containerSettingsItems.getChildAt(0).requestFocus();
                        }
                    } else if (currentScreenState == STATE_BLUETOOTH) {
                        if (containerBtItems != null && containerBtItems.getChildCount() > 0) {
                            containerBtItems.getChildAt(0).requestFocus();
                        }
                    } else if (currentScreenState == STATE_WIFI) {
                        if (containerWifiItems != null && containerWifiItems.getChildCount() > 0) {
                            containerWifiItems.getChildAt(0).requestFocus();
                        }
                    }

                    clickFeedback(); // 헛방을 쳤어도 조작감(사운드/진동)은 살려줍니다.
                    return true;
                }
            }
            return super.onKeyDown(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }

    // =======================================================
    // 🚀 [리플렉션 엔진] 시스템 최고 권한을 이용한 '진짜 깨우기'
    // =======================================================
    public void turnOnScreen() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                // 💡 꺼져있는 디스플레이 패널 강제 기상!
                pm.getClass().getMethod("wakeUp", long.class).invoke(pm, SystemClock.uptimeMillis());
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Runtime.getRuntime().exec(new String[] { "su", "-c", "input keyevent 224" });
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        // 💡 확인(Center) 버튼이거나 재생/정지 버튼일 때 감시 시작! (기기마다 번호가 달라서 다 넣어둠)
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == 23 || keyCode == 66 || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == 85 || keyCode == 126 || keyCode == 127) {

            if (action == KeyEvent.ACTION_DOWN) {
                // 버튼에 손가락이 처음 닿는 순간 (연속 눌림이 아닌 0회차일 때)
                if (event.getRepeatCount() == 0) {
                    // 💣 5초(5000ms) 짜리 타이머 폭탄을 장전하고 카운트다운 시작!
                    rebootHandler.removeCallbacks(rebootTask);
                    rebootHandler.postDelayed(rebootTask, 5000);
                }
            } else if (action == KeyEvent.ACTION_UP) {
                // 🛑 5초가 지나기 전에 손가락을 떼면? 타이머 폭탄 즉시 해체!
                rebootHandler.removeCallbacks(rebootTask);
            }

            // 💡 [핵심] 여기서 return true;를 해버리면 원래 하려던 짧은 클릭(음악 재생 등)이 먹통이 돼.
            // 그래서 타이머만 몰래 켜두고, 시스템은 아무 일 없었다는 듯 원래 로직을 타도록 밑으로 통과시켜야 해!
        }
        // =======================================================
        // 🎧 1. 하단 시작/정지 버튼 (Play/Pause) 제어 구역
        // =======================================================
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_STOP ||
                keyCode == 85 || keyCode == 86 || keyCode == 126 || keyCode == 127) {

            if (action == KeyEvent.ACTION_DOWN) {
                if (!isBottomButtonDown) {
                    isBottomButtonDown = true;
                    bottomButtonDownTime = System.currentTimeMillis();
                    isMediaLongPressConsumed = false;
                } else if (System.currentTimeMillis() - bottomButtonDownTime > 500 && !isMediaLongPressConsumed) {
                    isMediaLongPressConsumed = true;
                    clickFeedback();

                    if (currentScreenState == STATE_PLAYER) {
                        if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getVisibility() == View.VISIBLE) {
                            layoutShuffleRepeatOverlay.setVisibility(View.GONE);
                        } else {
                            showShuffleRepeatOverlay(); // 🚀 [완벽 해결] 플레이어 화면에서 재생/정지 버튼을 0.5초 이상 누르면 셔플/반복 팝업 소환!
                        }
                    } else if (currentScreenState == STATE_BROWSER && hzIndexScroll != null) {
                        if (currentScrollIndexList != null && !currentScrollIndexList.isEmpty()) {
                            if (hzIndexScroll.getVisibility() == View.VISIBLE) {
                                hzIndexScroll.setVisibility(View.GONE);
                                if (listVirtualSongs != null)
                                    listVirtualSongs.requestFocus();
                            } else {
                                hzIndexScroll.setVisibility(View.VISIBLE);
                                if (layoutIndexContainer != null && layoutIndexContainer.getChildCount() > 0) {
                                    layoutIndexContainer.getChildAt(0).requestFocus();
                                }
                            }
                        }
                    }
                }
                return true;
            } else if (action == KeyEvent.ACTION_UP) {
                // 🚀 [고아 신호 방어막] 화면 깨울 때 발생한 UP 찌꺼기 완벽 차단!
                if (!isBottomButtonDown)
                    return true;

                isBottomButtonDown = false;
                if (isMediaLongPressConsumed) {
                    isMediaLongPressConsumed = false;
                } else {
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().playOrPauseMusic();
                }
                return true;
            }
        }

        // =======================================================
        // 🔘 2. 가운데 휠 버튼 (Center / Enter) 제어 구역
        // =======================================================
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {

            if (action == KeyEvent.ACTION_DOWN) {
                if (!isCenterButtonDown) {
                    isCenterButtonDown = true;
                    centerButtonDownTime = System.currentTimeMillis();
                    isCenterLongPressed = false;
                } else if (System.currentTimeMillis() - centerButtonDownTime > 500 && !isCenterLongPressed) {
                    // 🚀 [가운데 버튼 길게 누름!]
                    isCenterLongPressed = true;
                    clickFeedback();

                    if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getVisibility() == View.VISIBLE) {
                        return true; // 🚀 팝업 떠있을 땐 길게 눌러도 화면 끄기 금지!
                    }

                    boolean isSongFocused = false;
                    if (currentScreenState == STATE_BROWSER && listVirtualSongs != null
                            && listVirtualSongs.getVisibility() == View.VISIBLE && listVirtualSongs.hasFocus()) {
                        int position = listVirtualSongs.getSelectedItemPosition();
                        if (position == ListView.INVALID_POSITION && listVirtualSongs.getFocusedChild() != null) {
                            position = listVirtualSongs.getPositionForView(listVirtualSongs.getFocusedChild());
                        }
                        if (position >= 0 && position < virtualSongList.size()) {
                            isSongFocused = true;
                            if (currentBrowserMode == 5)
                                showRemoveFromFavoritesDialog(virtualSongList.get(position));
                            else if (currentBrowserMode == 7)
                                showRemoveFromPlaylistDialog(virtualSongList.get(position));
                            else
                                showAddToPlaylistDialog(virtualSongList.get(position));
                        }
                    }

                    // 🎯 곡 목록이 아닐 때 -> 안드로이드 순정 롱클릭 후 화면 끄기 발동!
                    if (!isSongFocused) {
                        View focused = getCurrentFocus();
                        boolean handled = false;
                        if (focused != null)
                            handled = focused.performLongClick();

                        if (!handled) {
                            clickFeedback();
                            turnOffScreen(); // 💡 여기서 진짜 화면 끄기 정상 발동!
                        }
                    }
                }
                return true;

            } else if (action == KeyEvent.ACTION_UP) {
                // 🚀 [고아 신호 방어막] 화면이 꺼져있을 때 누른 버튼의 찌꺼기(UP) 신호를 원천 차단합니다!
                // (이 한 줄 덕분에 화면 켤 때 오작동하는 버그가 100% 사라집니다)
                if (!isCenterButtonDown) {
                    return true;
                }

                isCenterButtonDown = false;
                if (isCenterLongPressed) {
                    isCenterLongPressed = false;
                    return true;
                } else {
                    // 🚀 [가운데 버튼 짧게 누름 또는 더블클릭!]
                    if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getVisibility() == View.VISIBLE) {
                        clickFeedback();
                        View focused = layoutShuffleRepeatOverlay.findFocus();
                        if (focused instanceof Button) {
                            focused.performClick();
                        } else if (btnOverlayShuffle != null && btnOverlayShuffle.hasFocus()) {
                            btnOverlayShuffle.performClick();
                        } else if (btnOverlayRepeat != null) {
                            btnOverlayRepeat.performClick();
                        }
                        return true;
                    }

                    long now = System.currentTimeMillis();
                    if (currentScreenState == STATE_PLAYER) {
                        if (now - lastCenterUpTime < 300) {
                            doubleClickHandler.removeCallbacks(singleClickRunnable);
                            clickFeedback();
                            toggleFavorite();
                        } else {
                            doubleClickHandler.postDelayed(singleClickRunnable, 300);
                        }
                    } else {
                        handleCenterShortClick();
                    }
                    lastCenterUpTime = now;
                    return true;
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // if (ignoreNextKeyUp) {
        // ignoreNextKeyUp = false; // 방어막 해제
        // return true; // 💡 이벤트를 여기서 파쇄하여 아래의 handleCenterShortClick() 등으로 신호가 흘러가지 않게
        // 막습니다!
        // }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = true;
        try {
            if (Build.VERSION.SDK_INT >= 20)
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
            // 🚀 [방어막] 롱클릭(화면 끄기 또는 플레이리스트 팝업)이 이미 처리되었다면 숏클릭 루틴 파쇄
            if (isLongPressConsumed) {
                isLongPressConsumed = false;
                return true;
            }

            if ((event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
                // 🚀 [신규 조작계 장착] 메뉴 편집 모드에서 '순서 이동 중(빨간불)'일 때, 한 번만 짧게 딸깍! 누르면 그 자리에 확정/저장시킵니다!
                if (currentScreenState == STATE_SETTINGS && settingsSubMode == 4 && isMenuReorderMode) {
                    toggleMenuReorderMode(); // 💡 스위치를 끄고 저장 로직 실행
                    clickFeedback();
                    return true; // 💡 아래의 일반 숏클릭 동작으로 신호가 새어나가지 않게 여기서 완벽히 파쇄!
                }
                // 🚀 [지능형 숏클릭 분기] 플레이어 화면에서는 롱프레스를 화면 끄기에 양보했으므로,
                // 즐겨찾기(♥) 등록을 '더블 클릭(따닥!)' 엔진으로 우아하게 구출합니다!
                if (currentScreenState == STATE_PLAYER) {
                    long now = System.currentTimeMillis();
                    if (now - lastCenterUpTime < 300) {
                        doubleClickHandler.removeCallbacks(singleClickRunnable);
                        lastCenterUpTime = 0; // 타이머 초기화
                        clickFeedback();
                        toggleFavorite(); // 따닥 누르면 즐겨찾기 추가/해제!
                    } else {
                        lastCenterUpTime = now;
                        doubleClickHandler.postDelayed(singleClickRunnable, 300);
                    }
                } else {
                    // 🚀 그 외의 모든 화면(메인 메뉴 선택, 설정 로직, 라이브러리 목록 등)에서는
                    // 0.3초 대기 시간 없이 즉시 100% 원터치 광속 클릭이 작동하여 답답함을 완전히 없앱니다!
                    try {
                        handleCenterShortClick();
                    } catch (Exception e) {
                    }
                }
            }
            return true;
        }
        // 🚀 [버그 완벽 해결] 하단 하드웨어 재생/정지 버튼 지능형 완전 정화 조작계!
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == 86 || keyCode == 126 || keyCode == 127) {

            if (isBottomButtonDown) {
                long duration = System.currentTimeMillis() - bottomButtonDownTime;
                isBottomButtonDown = false; // 타이머 즉시 초기화

                // 💡 0.8초(800ms) 이상 꾹~ 눌렀을 때!
                if (duration >= 800) {
                    if (currentScreenState == STATE_PLAYER) {
                        clickFeedback();
                        showShuffleRepeatOverlay(); // 🚀 팝업 소환!
                        isMediaLongPressConsumed = true; // 롱클릭 깃발 꽂기
                    }
                    return true; // 🚨 여기서 무조건 탈출하여 노래 정지를 막습니다!
                }
            }

            if (event.getRepeatCount() == 0) {
                // 🚀 [방어막 가동] 롱클릭으로 팝업을 띄운 직후라면, 여기서 노래가 멈추는 걸 원천 차단!
                if (isMediaLongPressConsumed) {
                    isMediaLongPressConsumed = false;
                    return true;
                }

                com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);

                // 💡 [최우선 규칙] 사용자가 지금 '음악 플레이어 화면(STATE_PLAYER)'을 보고 있다면
                if (currentScreenState == STATE_PLAYER) {
                    if (fm.isPowerUp) fm.powerDown();
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().playOrPauseMusic();
                    activePlayer = 0;
                }
                else if (activePlayer == 1) {
                    com.themoon.y1.managers.AudioPlayerManager amInstance = com.themoon.y1.managers.AudioPlayerManager.getInstance();
                    if (amInstance.isPlaying()) {
                        amInstance.playOrPauseMusic();
                    }
                    try { Thread.sleep(50); } catch (Exception e) {}
                    if (!fm.powerUp(fm.currentFreq)) {
                        Toast.makeText(this, "Radio Error: " + fm.lastError, Toast.LENGTH_SHORT).show();
                    }
                    if (currentScreenState == STATE_SETTINGS) buildRadioUI();
                } else {
                    com.themoon.y1.managers.AudioPlayerManager.getInstance().playOrPauseMusic();
                }

                updateGlobalStatusPlayIcon();
                clickFeedback();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
            if (!isSeekPerformed) {
                if (activePlayer == 1) {
                    tuneToNextSavedRadioChannel(true);
                } else {
                    // 🚀 [챕터 스킵 닌자 가동] 챕터/가사 창이 열려있다면 곡 넘기기 대신 '다음 챕터 워프' 가동!
                    if (isVisualizerShowing && lyricTimestamps != null && !lyricTimestamps.isEmpty()) {
                        int currentPos = com.themoon.y1.managers.AudioPlayerManager.getInstance().getCurrentPosition();
                        int targetMs = -1;
                        for (int i = 0; i < lyricTimestamps.size(); i++) {
                            if (lyricTimestamps.get(i) > currentPos + 2000) { // 2초 여유 마진
                                targetMs = lyricTimestamps.get(i);
                                break;
                            }
                        }
                        if (targetMs != -1) {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .seekRelative(targetMs - currentPos);
                        } else {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().nextTrack(); // 마지막 챕터면 다음 곡으로
                        }
                    } else {
                        com.themoon.y1.managers.AudioPlayerManager.getInstance().nextTrack();
                    }
                }
                clickFeedback();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
            if (!isSeekPerformed) {
                if (activePlayer == 1) {
                    tuneToNextSavedRadioChannel(false);
                } else {
                    // 🚀 [챕터 스킵 닌자 가동] 챕터/가사 창이 열려있다면 '이전 챕터 워프' 가동!
                    if (isVisualizerShowing && lyricTimestamps != null && !lyricTimestamps.isEmpty()) {
                        int currentPos = com.themoon.y1.managers.AudioPlayerManager.getInstance().getCurrentPosition();
                        int targetMs = -1;
                        for (int i = lyricTimestamps.size() - 1; i >= 0; i--) {
                            if (lyricTimestamps.get(i) < currentPos - 3000) { // 3초 이전 챕터로 (현재 챕터 재시작 방지)
                                targetMs = lyricTimestamps.get(i);
                                break;
                            }
                        }
                        // 첫 번째 챕터보다 앞이면 맨 처음(0초)으로 점프
                        if (targetMs == -1 && !lyricTimestamps.isEmpty())
                            targetMs = lyricTimestamps.get(0);

                        if (targetMs != -1) {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance()
                                    .seekRelative(targetMs - currentPos);
                        } else {
                            com.themoon.y1.managers.AudioPlayerManager.getInstance().prevTrack();
                        }
                    } else {
                        com.themoon.y1.managers.AudioPlayerManager.getInstance().prevTrack();
                    }
                }
                clickFeedback();
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // 🚀 [기능 변경] 플레이어 화면에서 하단 정지/재생 버튼을 길게 누르면 셔플/반복 설정 팝업 가동 또는 닫기!
        if (currentScreenState == STATE_PLAYER && (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85
                || keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == 86 || keyCode == 126 || keyCode == 127)) {
            clickFeedback();
            isMediaLongPressConsumed = true; // 🚀 손 뗄 때 재생/정지 토글 차단 방어막 가동!
            if (layoutShuffleRepeatOverlay != null && layoutShuffleRepeatOverlay.getVisibility() == View.VISIBLE) {
                layoutShuffleRepeatOverlay.setVisibility(View.GONE);
            } else {
                showShuffleRepeatOverlay(); // 🚀 셔플/반복 재생 설정 팝업 호출!
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            clickFeedback();
            isLongPressConsumed = true; // 💡 손을 뗄 때 일반 숏클릭(메뉴 진입/스위치 토글)이 터지는 중복 버그 완벽 차단!

            // 🚀 [궁극의 철벽 방어막 장착]
            // 현재 메뉴 숨김/순서 편집창(settingsSubMode == 4)에 머물고 있다면,
            if (currentScreenState == STATE_SETTINGS && settingsSubMode == 4) {
                // 💡 [수정] 이미 순서 이동 모드(빨간불)일 때는 길게 눌러도 꺼지지 않게 막습니다! (오직 숏클릭으로만 내려놓기)
                if (!isMenuReorderMode) {
                    toggleMenuReorderMode(); // 지능형 순서 변경 엔진 기상!
                }
                return true; // 쇠말뚝을 박아 시스템 화면 끄기로 신호가 흘러가지 않게 원천 차단합니다.
            }

            // 🚀 [분기 1] 평상시 메인 화면, 플레이어 화면, 설정 화면 등에서는 원래대로 안전하게 화면 끄기 가동!
            if (currentScreenState == STATE_MENU || currentScreenState == STATE_PLAYER
                    || currentScreenState == STATE_SETTINGS
                    || currentScreenState == STATE_BLUETOOTH || currentScreenState == STATE_WIFI
                    || currentScreenState == STATE_BRIGHTNESS
                    || currentScreenState == STATE_STORAGE || currentScreenState == STATE_WEBSERVER) {

                turnOffScreen();
                return true;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    // ⭕ [아래 코드로 덮어쓰기]
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (externalSdMountMonitor != null) {
            externalSdMountMonitor.stop();
            externalSdMountMonitor = null;
        }
        clockHandler.removeCallbacks(clockTask);
        progressHandler.removeCallbacks(updateProgressTask);
        volumeHandler.removeCallbacks(hideVolumeTask);

        // 🚀 [웹 서버 자물쇠 해제] 앱이 종료될 때 시스템 잠금 좀비 버그가 걸리지 않도록 확실하게 풀어줍니다.
        try {
            if (serverWakeLock != null && serverWakeLock.isHeld())
                serverWakeLock.release();
            if (serverWifiLock != null && serverWifiLock.isHeld())
                serverWifiLock.release();
        } catch (Exception e) {
        }

        // 🚀 [2차 방어막] 앱이 완전히 죽기 직전, 라디오 칩셋이 켜져 있다면 강제로 모가지를 비틀어 전원을 뽑아버립니다! (좀비 라디오 원천
        // 차단)
        try {
            com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager.getInstance(this);
            if (fm.isPowerUp) {
                fm.powerDown();
            }
        } catch (Exception e) {
        }

        com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager.getInstance();

        if (currentBrowserMode == BROWSER_AUDIOBOOKS && !currentPlaylist.isEmpty()) {
            com.themoon.y1.managers.AudiobookManager.getInstance(this).saveBookmark(
                    currentPlaylist.get(currentIndex).getAbsolutePath(),
                    am.getCurrentPosition(),
                    currentIndex);
        }

        am.releasePlayer(); // 🚀 직접 끄지 않고 관리자에게 정중히 요청!

        if (currentFileInputStream != null) {
            try {
                currentFileInputStream.close();
            } catch (Exception e) {
            }
        }
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }

        unregisterReceiver(systemStatusReceiver);
    }

    // 💡 안드로이드 시스템 자체의 하드웨어 삑 소리 스트림을 직접 차단/허용하는 함수
    public void applySoundSetting() {
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
    public Bitmap applyGaussianBlur(Bitmap original) {
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
    public void buildDateTimeUI() {
        currentSettingsDepth = 2; // 🚀 카테고리(0) → 서브 메뉴(1) → 이 화면(2)
        containerSettingsItems.removeAllViews();
        // 🚀 [수정] 12시간/24시간 텍스트도 번역기를 거치도록 t()를 씌워줍니다!
        String formatRightText = is24HourFormat ? t("24 Hour") : t("12 Hour");
        final LinearLayout rowFormat = createSettingRow("Time Format", formatRightText);
        rowFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                is24HourFormat = !is24HourFormat; // 토글 변환
                prefs.edit().putBoolean("is_24h_format", is24HourFormat).commit(); // 영구 저장

                // 💡 변경 즉시 시계 침들이 돌아가도록 런타임 쓰레드 한 번 강제 찌르기
                clockHandler.removeCallbacks(clockTask);
                clockHandler.post(clockTask);

                buildDateTimeUI(); // 세팅 화면 새로고침
            }
        });
        containerSettingsItems.addView(rowFormat);
        final LinearLayout rowYear = createSettingRow("Year", String.valueOf(dtYear));
        rowYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Year", 2020, 2035, dtYear);
            }
        });
        containerSettingsItems.addView(rowYear);

        final LinearLayout rowMonth = createSettingRow("Month", String.format(Locale.US, "%02d", dtMonth));
        rowMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Month", 1, 12, dtMonth);
            }
        });
        containerSettingsItems.addView(rowMonth);

        final LinearLayout rowDay = createSettingRow("Day", String.format(Locale.US, "%02d", dtDay));
        rowDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Day", 1, 31, dtDay);
            }
        });
        containerSettingsItems.addView(rowDay);

        final LinearLayout rowHour = createSettingRow("Hour (24H)", String.format(Locale.US, "%02d", dtHour));
        rowHour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Hour", 0, 23, dtHour);
            }
        });
        containerSettingsItems.addView(rowHour);

        final LinearLayout rowMinute = createSettingRow("Minute", String.format(Locale.US, "%02d", dtMinute));
        rowMinute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildDateTimeSelectorUI("Minute", 0, 59, dtMinute);
            }
        });
        containerSettingsItems.addView(rowMinute);

        createCategoryHeader("━━━━━━━━━━━━━━");

        final Button btnApply = createListButton("✅ " + t("APPLY DATE & TIME"));
        btnApply.setTextColor(0xFFFFFFFF);
        btnApply.setTypeface(null, Typeface.BOLD);
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
                    String targetYMD = String.format(Locale.US, "%04d%02d%02d", dtYear, dtMonth, dtDay);

                    // 포맷 1: 구형 안드로이드(Toolbox) 전용 포맷 -> YYYYMMDD.HHmmss
                    String dateToolbox = String.format(Locale.US, "%04d%02d%02d.%02d%02d%02d", dtYear,
                            dtMonth, dtDay, dtHour, dtMinute, 0);
                    // 포맷 2: POSIX 국제 표준 포맷 (Toybox/Busybox 호환) -> MMDDhhmmYYYY.ss
                    String datePosix = String.format(Locale.US, "%02d%02d%02d%02d%04d.00", dtMonth, dtDay,
                            dtHour, dtMinute, dtYear);
                    // 포맷 3: 최신 안드로이드(Toybox) 문자열 포맷 -> YYYY-MM-DD HH:MM:SS
                    String dateString = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d:%02d", dtYear,
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

                // 🚀 [포커스 버그 해결 1] 날짜 적용 후 System 카테고리로 복귀
                com.themoon.y1.managers.SettingsMenuManager.getInstance(MainActivity.this).buildSystemSettingsUI();

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

        if (containerSettingsItems.getChildCount() > 0)
            containerSettingsItems.getChildAt(0).requestFocus();
    }

    // 💡 2. 숫자(년/월/일/시/분) 선택용 세로 리스트 화면
    private void buildDateTimeSelectorUI(final String type, int min, int max, int currentValue) {
        currentSettingsDepth = 3; // 🚀 카테고리(0) → 서브 메뉴(1) → DateTime(2) → 이 화면(3)
        containerSettingsItems.removeAllViews();

        Button focusBtn = null;
        for (int i = min; i <= max; i++) {
            final int val = i;
            String displayVal = (type.equals("Minute") || type.equals("Hour") || type.equals("Month")
                    || type.equals("Day")) ? String.format(Locale.US, "%02d", val) : String.valueOf(val);
            Button btn = createListButton(displayVal);
            btn.setGravity(Gravity.CENTER); // 가운데 정렬로 예쁘게!

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
        else if (containerSettingsItems.getChildCount() > 0)
            containerSettingsItems.getChildAt(0).requestFocus();
    }

    // 💡 [화면 꺼짐 전용 수신기] 화면이 꺼진 상태에서 시스템이 버튼 신호를 여기로 쏴줍니다!
    public static class MediaBtnReceiver extends BroadcastReceiver {
        private static long lastWakeUpTime = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()) || MainActivity.instance == null) {
                return;
            }

            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null)
                return;

            int keyCode = event.getKeyCode();
            int action = event.getAction();

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = false;
            try {
                if (android.os.Build.VERSION.SDK_INT >= 20) {
                    isScreenOn = pm.isInteractive();
                } else {
                    isScreenOn = pm.isScreenOn();
                }
            } catch (Exception e) {
            }

            // =======================================================
            // 🚀 1. 화면이 꺼져 있을 때의 지능형 통제 구역
            // =======================================================
            if (!isScreenOn) {
                // 💡 스크린 오프 컨트롤이 켜져 있고, 이전/다음 곡(또는 볼륨) 버튼을 눌렀다면? (ACTION_DOWN 한정)
                if (MainActivity.instance.isScreenOffControlEnabled && action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88 ||
                                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87 ||
                                keyCode == 21 || keyCode == 22)) {

                    // 화면을 켜지 않고 아래의 '3. 음악 제어 로직'으로 무사통과 시킵니다! (스텔스 조작)

                } else {
                    // 💡 가운데 버튼(재생/정지)이거나, 스크린 오프 컨트롤 자체가 꺼져있다면?
                    // 무조건 음악 제어는 무시하고 "화면만" 켭니다!
                    MainActivity.instance.turnOnScreen();
                    lastWakeUpTime = System.currentTimeMillis();
                    return; // 🛑 여기서 강제 종료!
                }
            }

            // =======================================================
            // 🚀 2. 찌꺼기 신호 (ACTION_UP) 완벽 무시 방어막
            // =======================================================
            if (System.currentTimeMillis() - lastWakeUpTime < 500) {
                return;
            }

            // =======================================================
            // 🚀 3. 실제 음악 제어 로직 (화면이 켜져 있거나, 스텔스 통과 시)
            // =======================================================
            if (action == KeyEvent.ACTION_DOWN) {

                // ⏮ 이전 곡 버튼
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88) {
                    if (MainActivity.instance.activePlayer == 1) {
                        MainActivity.instance.tuneToNextSavedRadioChannel(false);
                    } else {
                        com.themoon.y1.managers.AudioPlayerManager.getInstance().prevTrack();
                    }
                    MainActivity.instance.clickFeedback();
                }
                // ⏭ 다음 곡 버튼
                else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87) {
                    if (MainActivity.instance.activePlayer == 1) {
                        MainActivity.instance.tuneToNextSavedRadioChannel(true);
                    } else {
                        com.themoon.y1.managers.AudioPlayerManager.getInstance().nextTrack();
                    }
                    MainActivity.instance.clickFeedback();
                }
                // ⏯ 재생/일시정지 버튼
                else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == 85 || keyCode == 86) {
                    if (MainActivity.instance.activePlayer != 1) {
                        com.themoon.y1.managers.AudioPlayerManager.getInstance().playOrPauseMusic();
                        MainActivity.instance.clickFeedback();
                    }
                }
                // 🔊 휠 조작 볼륨 방어
                else if (keyCode == 21) {
                    MainActivity.instance.adjustVolume(false);
                    MainActivity.instance.clickFeedback();
                } else if (keyCode == 22) {
                    MainActivity.instance.adjustVolume(true);
                    MainActivity.instance.clickFeedback();
                }
            }
        }
    }

    // 💡 [수정] 동적 버튼의 꼬리표(Tag)를 읽어 앨범 아트를 똑똑하게 띄워줍니다!
    public void refreshNowPlayingPreview() {
        refreshWidgets();
    }

    // 💡 [추가] 1. 인터넷에서 받아온 커버 이미지를 캐시 폴더에서 불러와 화면에 띄우는 함수
    public void applyCachedCoverArt(String imagePath) {
        try {
            // 중앙의 선명한 앨범 아트
            BitmapFactory.Options optsCenter = new BitmapFactory.Options();
            optsCenter.inSampleSize = 2;
            Bitmap bmpCenter = BitmapFactory.decodeFile(imagePath, optsCenter);
            ivAlbumArt.setImageBitmap(bmpCenter);

            // 🚀 [완벽 수정] 앨범 아트의 하단 중앙 색상을 스포이드로 정확히 뽑아냅니다!
            try {
                int centerX = bmpCenter.getWidth() / 2;
                int centerY = (int) (bmpCenter.getHeight() * 0.8); // 정중앙보다 약간 아래의 포인트 색상
                currentAlbumColor = bmpCenter.getPixel(centerX, centerY) | 0xFF000000;
            } catch (Exception e) {
                currentAlbumColor = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
            }

            // 뒷배경 블러 처리
            BitmapFactory.Options optsBg = new BitmapFactory.Options();
            optsBg.inSampleSize = 4;
            Bitmap sourceBg = BitmapFactory.decodeFile(imagePath, optsBg);
//            Bitmap blurredBg = applyGaussianBlur(sourceBg);
//            ivPlayerBgBlur.setImageBitmap(blurredBg);
//            if (sourceBg != blurredBg)
//                sourceBg.recycle();
// 🚀 [배경 테마 엔진 적용 2: 일반 태그 음악]
            if ("clear".equals(currentPlayerBgMode)) {
                ivPlayerBgBlur.setImageBitmap(sourceBg);
            } else if ("blur".equals(currentPlayerBgMode)) {
                android.graphics.Bitmap blurredBg =applyGaussianBlur(sourceBg);
                ivPlayerBgBlur.setImageBitmap(blurredBg);
                if (sourceBg != blurredBg) sourceBg.recycle();
            } else {
                ivPlayerBgBlur.setImageBitmap(null);
                sourceBg.recycle();
            }
            updatePlayerBgOverlayVisibility();
            // 메인 메뉴 배경도 연동하기 위해 파일 데이터를 byte[]로 변환해서 lastAlbumArtBytes에 집어넣습니다!
            File file = new File(imagePath);
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            java.io.BufferedInputStream buf = new java.io.BufferedInputStream(new java.io.FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();

            lastAlbumArtBytes = bytes;
            updateMainMenuBackground();
            refreshNowPlayingPreview();
            // 🚀 앨범 아트가 새로 로딩되었으니 차량 화면에도 새로 쏴줍니다!
            sendBluetoothMetaToCar();

        } catch (Exception e) {
        }
    }

    public void fetchTrackInfoFromInternet(final File track, final String originalQuery, final boolean hasValidTags,
            final String origTitle, final String origArtist) {

        // 🚀 [추가] 오프라인 방어막: 와이파이나 데이터 연결이 없으면 조용히 뒤돌아 나갑니다!
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo == null || !netInfo.isConnected()) {
            return;
        }

        final String cleanQuery = originalQuery
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("^[0-9\\s\\-]+", "")
                .replaceAll("\\s[0-9]{2}\\s", " ")
                .trim();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "🔍 " + t("Searching: ") + cleanQuery, Toast.LENGTH_SHORT).show();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String query = java.net.URLEncoder.encode(cleanQuery, "UTF-8");
                    String urlString = "http://api.deezer.com/search?q=" + query;
                    java.net.URL url = new java.net.URL(urlString);

                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200)
                        throw new Exception("HTTP Response Code: " + responseCode);

                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        response.append(line);
                    reader.close();

                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                    org.json.JSONArray dataArray = jsonResponse.optJSONArray("data");

                    if (dataArray != null && dataArray.length() > 0) {
                        org.json.JSONObject trackInfo = dataArray.getJSONObject(0);

                        final String fetchedTitle = trackInfo.getString("title");
                        final String fetchedArtist = trackInfo.getJSONObject("artist").getString("name");

                        final String finalTitle = fetchedTitle;
                        final String finalArtist = fetchedArtist;

                        String coverUrl = trackInfo.getJSONObject("album").getString("cover_xl").replace("https://",
                                "http://");
                        java.net.URL imgUrl = new java.net.URL(coverUrl);
                        java.net.HttpURLConnection imgConn = (java.net.HttpURLConnection) imgUrl.openConnection();
                        java.io.InputStream in = imgConn.getInputStream();
                        final Bitmap coverBitmap = BitmapFactory.decodeStream(in);
                        in.close();

                        File coverFolder = StoragePaths.getCoversDir();
                        if (!coverFolder.exists())
                            coverFolder.mkdirs();

                        // 🚀 [.m4b 꼬리표 제거 추가!] 검색 시 파일명 기반으로 저장할 때 확장자를 깔끔하게 날립니다.
                        String safeFileName = track.getName().replace(".mp3", "").replace(".flac", "")
                                .replace(".m4a", "").replace(".m4b", "");

                        final File coverFile = new File(coverFolder, safeFileName + ".jpg");
                        FileOutputStream fos = new FileOutputStream(coverFile);
                        coverBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();

                        // 🚀 [추가] 앨범 커버 경로(album_art_)도 잊지 말고 저장소에 함께 덮어씌웁니다!
                        prefs.edit()
                                .putString("meta_title_" + track.getAbsolutePath(), finalTitle)
                                .putString("meta_artist_" + track.getAbsolutePath(), finalArtist)
                                .putString("album_art_" + track.getAbsolutePath(), coverFile.getAbsolutePath()) // 💡
                                                                                                                // 누락되었던
                                                                                                                // 핵심 코드
                                .commit();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, t("Album Art & Info Updated!"), Toast.LENGTH_SHORT)
                                        .show();
                                if (currentPlaylist.get(currentIndex).getAbsolutePath()
                                        .equals(track.getAbsolutePath())) {
                                    tvPlayerTitle.setText(finalTitle);
                                    tvPlayerArtist.setText(finalArtist);
                                    applyCachedCoverArt(coverFile.getAbsolutePath());
                                }
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, t("No results found."), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, t("Connection Error"), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    // 💡 [추가] 구형 안드로이드의 잠든 최신 보안(TLS 1.2)을 강제로 깨우는 전용 소켓 팩토리
    private static class TLSSocketFactory extends javax.net.ssl.SSLSocketFactory {
        private javax.net.ssl.SSLSocketFactory internalSSLSocketFactory;

        public TLSSocketFactory() throws java.security.KeyManagementException, java.security.NoSuchAlgorithmException {
            javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getInstance("TLS");
            context.init(null, null, null);
            internalSSLSocketFactory = context.getSocketFactory();
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return internalSSLSocketFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return internalSSLSocketFactory.getSupportedCipherSuites();
        }

        @Override
        public java.net.Socket createSocket() throws java.io.IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
        }

        @Override
        public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose)
                throws java.io.IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
        }

        @Override
        public java.net.Socket createSocket(String host, int port)
                throws java.io.IOException, java.net.UnknownHostException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
        }

        @Override
        public java.net.Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort)
                throws java.io.IOException, java.net.UnknownHostException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
        }

        @Override
        public java.net.Socket createSocket(java.net.InetAddress host, int port) throws java.io.IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
        }

        @Override
        public java.net.Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress,
                int localPort) throws java.io.IOException {
            return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
        }

        // 🚀 가장 핵심! 열리는 모든 소켓의 설정값을 TLSv1.2 로 강제 고정합니다.
        private java.net.Socket enableTLSOnSocket(java.net.Socket socket) {
            if (socket instanceof javax.net.ssl.SSLSocket) {
                ((javax.net.ssl.SSLSocket) socket).setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
            }
            return socket;
        }
    }

    // 💡 [수정] 속이 꽉 찬 리얼 파이(Pie) 차트 클래스

    // 💡 [추가] 화면에 존재하는 모든 글씨를 찾아내 테마 폰트로 갈아입히는 재귀 엔진!
    private void applyFontToAllViews(ViewGroup parent, Typeface font) {
        if (font == null)
            return;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            // 1. 만약 폴더(레이아웃)라면 안쪽으로 파고듭니다.
            if (child instanceof ViewGroup) {
                applyFontToAllViews((ViewGroup) child, font);
            }
            // 2. 만약 글씨(TextView, Button 등)라면 폰트를 즉시 교체합니다.
            else if (child instanceof TextView) {
                // 기존에 굵은 글씨(Bold) 설정이 되어있었다면 그 특성은 유지해 줍니다!
                Typeface current = ((TextView) child).getTypeface();
                int style = Typeface.NORMAL;
                if (current != null)
                    style = current.getStyle();

                ((TextView) child).setTypeface(font, style);
            }
        }
    }

    // 🚀 [신규 엔진] APK 내부에 탑재된 언어팩(assets/languages)을 기기 저장소로 자동 복사합니다!
    private void installBundledLanguages() {
        SharedPreferences prefs = getSharedPreferences("Y1_SETTINGS", MODE_PRIVATE);
        int lastInstalledVersion = prefs.getInt("last_lang_version", 0);

        int currentAppVersion = 1;
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentAppVersion = pInfo.versionCode;
        } catch (Exception e) {
        }

        // 🚀 이미 최신 버전의 기본 언어팩이 기기에 깔려있다면 중복 복사를 막아 속도를 높입니다.
        if (lastInstalledVersion >= currentAppVersion)
            return;

        // 💡 주의: 아래 경로는 현재 아티스트님의 LanguageManager가 파일을 읽어오는 폴더 경로로 맞춰주세요!
        // 보통 "/storage/sdcard0/Y1_Languages" 등으로 설정되어 있을 것입니다.
        File targetDir = StoragePaths.getLanguagesDir();
        if (!targetDir.exists())
            targetDir.mkdirs();

        try {
            android.content.res.AssetManager assetManager = getAssets();
            // assets/languages 폴더 안에 있는 파일 명단을 싹 긁어옵니다.
            String[] files = assetManager.list("languages");

            if (files != null) {
                for (String filename : files) {
                    if (filename.toLowerCase().endsWith(".json")) {
                        java.io.InputStream is = assetManager.open("languages/" + filename);
                        File outFile = new File(targetDir, filename);
                        FileOutputStream fout = new FileOutputStream(outFile);

                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = is.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }
                        fout.close();
                        is.close();
                    }
                }
            }
            // 🚀 성공적으로 복사했다면, 앱 버전을 금고에 기록해 다음 부팅 시 건너뛰도록 합니다.
            prefs.edit().putInt("last_lang_version", currentAppVersion).commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void installBundledThemes() {
        SharedPreferences prefs = getSharedPreferences("Y1_SETTINGS", MODE_PRIVATE);

        // 🚀 [개조] 단순한 true/false 대신, 마지막으로 테마를 설치했던 '앱의 버전 번호'를 읽어옵니다.
        int lastInstalledVersion = prefs.getInt("last_theme_version", 0);

        // 현재 실행된 이 앱의 진짜 버전 번호(versionCode)를 알아냅니다.
        int currentAppVersion = 1;
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentAppVersion = pInfo.versionCode; // 예: 1, 2, 3...
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🚀 [핵심 방어막] 이미 현재 버전과 같거나 더 높은 버전의 테마가 깔려있다면 건너뜁니다!
        // 만약 앱 버전이 올라갔다면(예: 1 -> 2) 이 조건문을 통과하여 테마를 새로 덮어씌웁니다.
        if (lastInstalledVersion >= currentAppVersion)
            return;

        File targetDir = StoragePaths.getThemesDir();
        if (!targetDir.exists())
            targetDir.mkdirs();

        try {
            android.content.res.AssetManager assetManager = getAssets();
            String[] files = assetManager.list("themes");

            if (files != null) {
                for (String filename : files) {
                    if (filename.toLowerCase().endsWith(".zip")) {
                        String folderName = filename.substring(0, filename.lastIndexOf("."));
                        File themeFolder = new File(targetDir, folderName);
                        if (!themeFolder.exists())
                            themeFolder.mkdirs();

                        java.io.InputStream is = assetManager.open("themes/" + filename);
                        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                                new java.io.BufferedInputStream(is));
                        java.util.zip.ZipEntry ze;

                        while ((ze = zis.getNextEntry()) != null) {
                            File extractFile = new File(themeFolder, ze.getName());
                            if (ze.isDirectory()) {
                                extractFile.mkdirs();
                            } else {
                                File parent = extractFile.getParentFile();
                                if (!parent.exists())
                                    parent.mkdirs();

                                FileOutputStream fout = new FileOutputStream(extractFile);
                                byte[] buffer = new byte[8192];
                                int count;
                                while ((count = zis.read(buffer)) != -1) {
                                    fout.write(buffer, 0, count);
                                }
                                fout.close();
                            }
                            zis.closeEntry();
                        }
                        zis.close();
                    }
                }
            }

            // 🚀 테마 덮어쓰기 조립이 완벽히 끝났다면, 현재 버전을 금고에 저장합니다.
            prefs.edit().putInt("last_theme_version", currentAppVersion).commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 고급 EQ 메인 서브 페이지 빌더
    public void buildEqualizerSettingsUI() {
        currentSettingsDepth = 2;
        settingsSubMode = 2; // EQ 서브 모드 활성화
        com.themoon.y1.managers.AudioEffectManager.getInstance().loadAndSyncExternalEqProfiles();
        com.themoon.y1.managers.AudioEffectManager.getInstance().ensureAudioEffectsReady();
        containerSettingsItems.removeAllViews();
        //createCategoryHeader(" " + t("EQUALIZER") + " ");
        // 🚀 [신규 장착] 하드웨어/소프트웨어 EQ 엔진 전환 스위치!
        // =======================================================
        // 🚀 [10-Band EQ 전원 스위치]
        // =======================================================
        final LinearLayout btnSoftwareEq = createSettingRow("10-Band Software EQ",
                isSoftwareEqEnabled ? t("ON") : t("OFF"));

        btnSoftwareEq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                if (isSoftwareEqEnabled) {
                    // 🚀 [기존 로직] 스위치 끄기
                    isSoftwareEqEnabled = false;
                    ((TextView) btnSoftwareEq.getChildAt(1)).setText(t("OFF"));
                    prefs.edit().putBoolean("software_eq_enabled", false).commit();

                    // =======================================================
                    // 🛡️ [추가된 방어막!] 5밴드로 돌아오면, 들고 있던 10밴드 프로필을 압수하고 5밴드 순정으로 강제 교체!
                    // =======================================================
                    currentEqProfile = "preset_0";
                    prefs.edit().putString("eq_profile_id", currentEqProfile).commit();

                    // 🚀 오디오 엔진 재부팅
                    rebootAudioEngine();

                    Toast.makeText(MainActivity.this, t("Switched to Hardware 5-Band EQ"), Toast.LENGTH_SHORT).show();
                    buildEqualizerSettingsUI();
                } else {
                    showSoftwareEqMaterialDialog(btnSoftwareEq);
                }
            }
        });
        containerSettingsItems.addView(btnSoftwareEq);
        // 🚀 2. 서브 설정창 EQ 표시
        String activeName = "Normal";
        if (currentEqProfile.startsWith("preset_")) {
            int pIdx = Integer.parseInt(currentEqProfile.replace("preset_", ""));
            if (pIdx < eqPresetNames.size())
                activeName = t(eqPresetNames.get(pIdx)); // 🚀 번역 적용
        } else {
            activeName = currentEqProfile.replace("custom_", ""); // 🚀 꼬리표 번역
        }
        LinearLayout rowSelect = createSettingRow("EQ Profile / Preset", activeName + " 〉");

        // 🚀 [버그 해결] 만들어둔 버튼에 클릭 이벤트를 달고, 화면에 찰칵! 추가해 줍니다.
        rowSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildEqProfileSelectorUI(); // 🚀 숨겨진 프리셋 선택 리스트 창 열기!
            }
        });
        containerSettingsItems.addView(rowSelect);

        // 2. 4구 베이스 부스터
        final String[] steps = { "OFF", "Weak", "Normal", "Strong" };
        final LinearLayout rowBass = createSettingRow("Bass Boost", t(steps[currentBassBoostStep]));
        rowBass.setOnClickListener(v -> {
            clickFeedback();
            currentBassBoostStep = (currentBassBoostStep + 1) % 4;
            ((TextView) rowBass.getChildAt(1)).setText(t(steps[currentBassBoostStep])); // 🚀 t() 장착 완료!
            com.themoon.y1.managers.AudioEffectManager.getInstance().applyAudioEffects();
            prefs.edit().putInt("bass_boost_step", currentBassBoostStep).commit();
        });
        containerSettingsItems.addView(rowBass);

        // 3. 4구 버츄얼라이저 (공간감)
        final LinearLayout rowVirt = createSettingRow("Virtualizer", t(steps[currentVirtualizerStep]));
        rowVirt.setOnClickListener(v -> {
            clickFeedback();
            currentVirtualizerStep = (currentVirtualizerStep + 1) % 4;
            ((TextView) rowVirt.getChildAt(1)).setText(t(steps[currentVirtualizerStep])); // 🚀 t() 장착 완료!
            com.themoon.y1.managers.AudioEffectManager.getInstance().applyAudioEffects();
            prefs.edit().putInt("virtualizer_step", currentVirtualizerStep).commit();
        });
        containerSettingsItems.addView(rowVirt);
        // 4. 🚀 [신규 장착] 크로스피드 (헤드파이 룸 어쿠스틱 공간감)
        final LinearLayout rowCrossfeed = createSettingRow("Crossfeed", t(steps[currentCrossfeedStep]));
        rowCrossfeed.setOnClickListener(v -> {
            clickFeedback();
            currentCrossfeedStep = (currentCrossfeedStep + 1) % 4;
            ((TextView) rowCrossfeed.getChildAt(1)).setText(t(steps[currentCrossfeedStep])); // 🚀 텍스트 즉시 변경!

            // 엔진에 즉시 새로운 강도(Intensity) 주입!
            com.themoon.y1.managers.AudioPlayerManager.getInstance().crossfeedProcessor
                    .setIntensity(currentCrossfeedStep);

            // 금고에 영구 저장
            prefs.edit().putInt("crossfeed_step", currentCrossfeedStep).commit();
        });
        containerSettingsItems.addView(rowCrossfeed);
        // 4. 커스텀 금고 관리 패널
        createCategoryHeader("━ " + t("PROFILE MANAGEMENT") + " ━");
        if (currentEqProfile.startsWith("custom_")) {
            View btnSave = createListButtonWithIcon("\uE161", t("Save Current Configuration"));
            btnSave.setOnClickListener(v -> {
                clickFeedback();
                com.themoon.y1.managers.AudioEffectManager.getInstance()
                        .saveCustomEqProfile(currentEqProfile.replace("custom_", ""));
                Toast.makeText(this, t("Configuration Saved!"), Toast.LENGTH_SHORT).show();
            });
            containerSettingsItems.addView(btnSave);

            View btnDel = createListButtonWithIcon("\uE872", t("Delete Current Profile"), 0xFFFF4444);

            btnDel.setOnClickListener(v -> {
                clickFeedback();
                com.themoon.y1.managers.AudioEffectManager.getInstance()
                        .deleteCustomEqProfile(currentEqProfile.replace("custom_", ""));
                currentEqProfile = "preset_0";
                com.themoon.y1.managers.AudioEffectManager.getInstance().applyEqProfile();
                buildEqualizerSettingsUI();
            });
            containerSettingsItems.addView(btnDel);
        }

        View btnCreate = createListButtonWithIcon("\uE145", t("Create New Profile"));
        btnCreate.setOnClickListener(v -> {
            clickFeedback();
            String listStr = prefs.getString("custom_eq_list", "");
            int count = 1;
            while (listStr.contains("User EQ " + count))
                count++;
            String newName = "User EQ " + count;
            if (!listStr.isEmpty())
                listStr += ",";
            listStr += newName;
            prefs.edit().putString("custom_eq_list", listStr).commit();

            currentEqProfile = "custom_" + newName;

            // 🚀 [버그 해결] 새 프로필 생성 시 기존에 조작했던 캐시 값이 그대로 상속되지 않도록,
            // 모든 주파수 대역(Band)을 깨끗하게 0 dB (Flat 기본값) 상태로 강제 포맷합니다!
            short bands = (equalizer != null) ? equalizer.getNumberOfBands() : 5;
            for (short i = 0; i < bands; i++) {
                customBandLevels[i] = 0;
                if (equalizer != null) {
                    try {
                        equalizer.setBandLevel(i, (short) 0);
                    } catch (Exception e) {
                    }
                }
            }

            com.themoon.y1.managers.AudioEffectManager.getInstance().saveCustomEqProfile(newName);
            com.themoon.y1.managers.AudioEffectManager.getInstance().exportEqProfileToFile(newName); // 생성과 동시에 공유용 단독
                                                                                                     // 파일로도 즉시 배출!
            com.themoon.y1.managers.AudioEffectManager.getInstance().applyEqProfile();

            // 🚀 [UX 혁신] 메인화면 리로드 단계를 과감히 생략하고 그래프 스튜디오로 바로 워프시킵니다!
            buildGraphicEqualizerUI();
        });
        containerSettingsItems.addView(btnCreate);

        // 🚀 5. 고급 그래픽 이퀄라이저 (Graphic EQ) 스튜디오 진입 버튼
        createCategoryHeader("━ " + t("GRAPHIC EQUALIZER") + " ━");
        LinearLayout btnGraphicEq = createSettingRow("Graphic Equalizer", t("Open Editor") + " 〉");
        btnGraphicEq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();

                if (currentEqProfile.startsWith("preset_")) {
                    Toast.makeText(MainActivity.this, t("Please create a Custom Profile to edit!"),
                            Toast.LENGTH_LONG).show();
                    return; // 🛑 여기서 함수를 완전히 종료시킵니다.
                }

                // =======================================================
                // 🛡️ [스튜디오 진입 방어막 & 자동 교환 엔진]
                // =======================================================
                boolean isProfile10Band = currentEqProfile.contains("_10B") || currentEqProfile.contains("10Band")
                        || currentEqProfile.contains("10.json");

                if (isSoftwareEqEnabled && !isProfile10Band) {
                    // 💡 10밴드 모드인데 5밴드를 들고 입장하려 할 때
                    currentEqProfile = "custom_Default_10B";

                    String listStr = prefs.getString("custom_eq_list", "");
                    if (!listStr.contains("Default_10B")) {
                        prefs.edit().putString("custom_eq_list", "Default_10B," + listStr).commit();
                    }
                    Toast.makeText(MainActivity.this, "🔄 " + t("Auto-switched to a 10-Band profile for editing."),
                            Toast.LENGTH_SHORT).show();
                } else if (!isSoftwareEqEnabled && isProfile10Band) {
                    // 💡 5밴드 모드인데 10밴드를 들고 입장하려 할 때 (수정 가능한 5밴드 커스텀으로 쥐여줍니다!)
                    currentEqProfile = "custom_Default_5B";

                    String listStr = prefs.getString("custom_eq_list", "");
                    if (!listStr.contains("Default_5B")) {
                        prefs.edit().putString("custom_eq_list", "Default_5B," + listStr).commit();
                    }
                    Toast.makeText(MainActivity.this, "🔄 " + t("Auto-switched to 5-Band default profile."),
                            Toast.LENGTH_SHORT).show();
                }

                // 🚀 자동 교환을 거쳤든, 정상 통과했든 최종적으로 무조건 스튜디오를 엽니다!
                buildGraphicEqualizerUI();
            }
        });
        containerSettingsItems.addView(btnGraphicEq);

        if (containerSettingsItems.getChildCount() > 0)
            containerSettingsItems.getChildAt(0).requestFocus();
    }

    // 프리셋 및 프로필 선택 창 (Depth 2)
    private void buildEqProfileSelectorUI() {
        currentSettingsDepth = 3;
        containerSettingsItems.removeAllViews();

        // 🚀 [포커스 유지용 금고] 현재 켜져 있는(방금 누른) EQ 버튼을 기억할 공간입니다!
        final View[] targetFocusView = { null };

        // 🚀 1. 시스템 프리셋 리스트 출력 (시스템은 무조건 5밴드 전용입니다!)
        if (equalizer != null) {
            for (int i = 0; i < eqPresetNames.size(); i++) {
                final int pIdx = i;
                final String pId = "preset_" + pIdx;
                String prefix = currentEqProfile.equals(pId) ? "✔ " : "   ";

                Button btn = createListButton(prefix + t(eqPresetNames.get(i)));

                if (currentEqProfile.equals(pId)) {
                    btn.setTextColor(0xFF00FF00);
                    btn.setTypeface(null, Typeface.BOLD);
                    targetFocusView[0] = btn; // 🎯 [핵심] 현재 활성화된 버튼 객체를 금고에 저장!
                }

                btn.setOnClickListener(v -> {
                    // =======================================================
                    // 🛡️ [시스템 프리셋 방어막] 시스템 EQ는 무조건 5밴드이므로 10밴드일 때 차단!
                    // =======================================================
                    if (isSoftwareEqEnabled) {
                        clickFeedback();
                        Toast.makeText(MainActivity.this, "⚠️ " + t("Cannot load a 5-Band profile in 10-Band mode."),
                                Toast.LENGTH_SHORT).show();
                        return; // 🛑 강제 차단!
                    }

                    clickFeedback();
                    currentEqProfile = pId;
                    com.themoon.y1.managers.AudioEffectManager.getInstance().applyEqProfile();
                    buildEqProfileSelectorUI(); // 화면 새로고침
                });
                containerSettingsItems.addView(btn);
            }
        }

        createCategoryHeader("━ " + t("SELECT USER PROFILES") + " ━");

        // 🚀 2. 커스텀 프리셋 리스트 출력
        String listStr = prefs.getString("custom_eq_list", "");
        if (!listStr.trim().isEmpty()) {
            for (final String prof : listStr.split(",")) {
                if (prof.trim().isEmpty())
                    continue;
                final String cId = "custom_" + prof;
                String prefix = currentEqProfile.equals(cId) ? "✔ " : "   ";

                Button btn = createListButton(prefix + prof);

                if (currentEqProfile.equals(cId)) {
                    btn.setTextColor(0xFF00FF00);
                    btn.setTypeface(null, Typeface.BOLD);
                    targetFocusView[0] = btn; // 🎯 [핵심] 현재 커스텀 EQ 버튼 객체도 금고에 저장!
                }

                btn.setOnClickListener(v -> {
                    // =======================================================
                    // 🛡️ [파일 이름 레이더망] 꼬리표(_10B)로 규격 완벽 스캔!
                    // =======================================================
                    // 💡 기존 파일(5.json, 10.json)과 신규 꼬리표(_5B, _10B)를 모두 스캔합니다!
                    boolean isProfile10Band = cId.contains("_10B") || cId.contains("10Band") || cId.contains("10.json");

                    if (isSoftwareEqEnabled && !isProfile10Band) {
                        clickFeedback();
                        Toast.makeText(MainActivity.this, "⚠️ " + t("Cannot load a 5-Band profile in 10-Band mode."),
                                Toast.LENGTH_SHORT).show();
                        return; // 🛑 5밴드를 10밴드에서 로드하는 것 강제 차단!
                    } else if (!isSoftwareEqEnabled && isProfile10Band) {
                        clickFeedback();
                        Toast.makeText(MainActivity.this, "⚠️ " + t("Cannot load a 10-Band profile in 5-Band mode."),
                                Toast.LENGTH_SHORT).show();
                        return; // 🛑 10밴드를 5밴드에서 로드하는 것 강제 차단!
                    }
                    // =======================================================

                    clickFeedback();
                    currentEqProfile = cId;
                    com.themoon.y1.managers.AudioEffectManager.getInstance().applyEqProfile();
                    buildEqProfileSelectorUI(); // 화면 새로고침
                });
                containerSettingsItems.addView(btn);
            }
        } else {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("   " + t("No custom profiles found."));
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setPadding(20, 10, 20, 10);
            containerSettingsItems.addView(tvEmpty);
        }

        // 🚀 3. [포커스 지능형 록온] 화면이 다 그려진 0.05초 뒤에, 금고에 넣어둔 그 버튼으로 포커스를 발사합니다!
        containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (targetFocusView[0] != null) {
                    targetFocusView[0].requestFocus(); // 🎯 누른 자리 그대로 유지!
                } else if (containerSettingsItems.getChildCount() > 0) {
                    containerSettingsItems.getChildAt(0).requestFocus(); // (에러 방지용 최상단 록온)
                }
            }
        }, 50);
    }

    // =========================================================================
    // 🚀 [완벽 수정] 1픽셀의 오차도 없고 포커스 가두리가 해결된 그래픽 EQ 스튜디오
    // =========================================================================
    private void buildGraphicEqualizerUI() {
        currentSettingsDepth = 3;
        settingsSubMode = 3;
        currentAdjustingBand = -1;

        containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("GRAPHIC EQUALIZER") + " ━");

        TextView tvTitle = new TextView(this);
        tvTitle.setText(t("Editing: ") + currentEqProfile.replace("custom_", ""));
        tvTitle.setTextColor(0xFFFF8800);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 10, 0, 20);
        containerSettingsItems.addView(tvTitle);

        // 🚀 [수정 1] 가로 스크롤을 유지하되, 안쪽 내용물이 화면 폭에 딱 맞게 강제 확장되도록 허용(fillViewport)합니다.
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(this);
        hsv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (280 * getResources().getDisplayMetrics().density)));
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setFillViewport(true);

        // 🚀 [수정 2] RelativeLayout을 버리고, 가로로 차곡차곡 쌓는 LinearLayout으로 엔진을 교체합니다!
        final LinearLayout eqContainer = new LinearLayout(this);
        eqContainer.setOrientation(LinearLayout.HORIZONTAL);
        eqContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        eqContainer.setGravity(Gravity.CENTER);

        final short bands;
        final short[] range;
        final int[] centerFreqs;

        if (isSoftwareEqEnabled) {
            bands = 10;
            range = new short[] { -1500, 1500 };
            centerFreqs = new int[] { 31000, 62000, 125000, 250000, 500000, 1000000, 2000000, 4000000, 8000000,
                    16000000 };
        } else {
            bands = (equalizer != null) ? equalizer.getNumberOfBands() : 5;
            range = (equalizer != null) ? equalizer.getBandLevelRange() : new short[] { -1500, 1500 };
            centerFreqs = new int[bands];
            for (short i = 0; i < bands; i++) {
                centerFreqs[i] = (equalizer != null) ? equalizer.getCenterFreq(i) : 0;
            }
        }

        for (short i = 0; i < bands; i++) {
            final short bandIdx = i;
            int freq = centerFreqs[i] / 1000;
            int currentLevel = customBandLevels[bandIdx];

            final LinearLayout bandLayout = new LinearLayout(this);
            bandLayout.setOrientation(LinearLayout.VERTICAL);
            bandLayout.setFocusable(true);
            bandLayout.setGravity(Gravity.CENTER);
            bandLayout.setId(8000 + i);

            int nextFocusId = (i == bands - 1) ? 8500 : (8000 + i + 1);
            int prevFocusId = (i == 0) ? 8500 : (8000 + i - 1);

            bandLayout.setNextFocusDownId(nextFocusId);
            bandLayout.setNextFocusUpId(prevFocusId);

            // 🚀 [수정 3] 핵심 기술! 폭(Width)을 '0'으로 주고 가중치(Weight=1.0f)를 주어 10개가 100% 꽉 맞물리게 자동
            // 압축합니다!
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);

            // 10밴드일 때는 간격을 2dp로 줄여서 오밀조밀하게, 5밴드일 때는 5dp로 넉넉하게 설정
            int marginDp = isSoftwareEqEnabled ? 2 : 5;
            if (i > 0)
                lp.leftMargin = (int) (marginDp * getResources().getDisplayMetrics().density);

            bandLayout.setLayoutParams(lp);

            final EqSliderView slider = new EqSliderView(this);
            slider.setRange(range[0], range[1]);
            slider.setLevel(currentLevel);
            slider.setLayoutParams(
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

            TextView tvFreq = new TextView(this);
            tvFreq.setText(freq >= 1000 ? (freq / 1000) + "k" : freq + "");
            tvFreq.setTextColor(ThemeManager.getTextColorPrimary());
            // 🚀 [수정 4] 글씨 크기를 시원하게 14.5f로 키우고, 굵은 글씨(BOLD)를 먹여 가독성을 극대화합니다!
            tvFreq.setTextSize(14.5f);
            tvFreq.setTypeface(null, Typeface.BOLD);
            tvFreq.setGravity(Gravity.CENTER);
            tvFreq.setPadding(0, 0, 0, 10);

            bandLayout.addView(slider);
            bandLayout.addView(tvFreq);

            bandLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        bandLayout.setBackground(
                                createButtonBackground(ThemeManager.getListButtonFocusedBg() & 0x66FFFFFF));
                    } else {
                        bandLayout.setBackgroundColor(0x00000000);
                        if (currentAdjustingBand == bandIdx) {
                            currentAdjustingBand = -1;
                            slider.setAdjusting(false);
                        }
                    }
                    slider.setFocused(hasFocus);
                }
            });

            bandLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    if (currentAdjustingBand == bandIdx) {
                        currentAdjustingBand = -1;
                        slider.setAdjusting(false);
                    } else {
                        if (currentAdjustingBand != -1) {
                            LinearLayout prevBand = (LinearLayout) eqContainer
                                    .findViewById(8000 + currentAdjustingBand);
                            if (prevBand != null)
                                ((EqSliderView) prevBand.getChildAt(0)).setAdjusting(false);
                        }
                        currentAdjustingBand = bandIdx;
                        slider.setAdjusting(true);
                    }
                }
            });

            bandLayout.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {

                        // 1. 🟢 이미 밴드를 클릭해서 '볼륨(dB) 조절 모드'일 때의 휠 동작
                        if (currentAdjustingBand == bandIdx) {
                            if (keyCode == 21 || keyCode == 22) {
                                int step = 100;
                                int level = customBandLevels[bandIdx];
                                if (keyCode == 21)
                                    level += step;
                                if (keyCode == 22)
                                    level -= step;

                                if (level > range[1])
                                    level = range[1];
                                if (level < range[0])
                                    level = range[0];

                                customBandLevels[bandIdx] = level;
                                slider.setLevel(level);
                                com.themoon.y1.managers.AudioEffectManager.getInstance()
                                        .saveCustomEqProfile(currentEqProfile.replace("custom_", ""));

                                // 🚀 [핵심 연결부] 하드웨어 모드와 소프트웨어 모드를 구별해서 명령을 쏩니다!
                                if (!isSoftwareEqEnabled && equalizer != null) {
                                    try {
                                        equalizer.setBandLevel(bandIdx, (short) level);
                                    } catch (Exception e) {
                                    }
                                } else if (isSoftwareEqEnabled) {
                                    // 🚀 소프트웨어 10밴드 수학 공식 필터에 실시간 파라미터 주입!
                                    com.themoon.y1.managers.AudioPlayerManager.getInstance().customEqProcessor
                                            .setBandLevel(bandIdx, level / 100.0f);
                                }

                                clickFeedback();
                                return true;
                            }
                        }
                        // 2. 🚀 [포커스 탈출 엔진 가동] 볼륨 조절 모드가 아닐 때, 양 끝단에서 휠을 돌리면 '저장 버튼(8500번)'으로 점프!
                        else {
                            if (keyCode == 21 && bandIdx == 0) { // 첫 번째 밴드에서 위로(왼쪽) 돌렸을 때 탈출!
                                View btnSave = containerSettingsItems.findViewById(8500);
                                if (btnSave != null)
                                    btnSave.requestFocus();
                                clickFeedback();
                                return true;
                            }
                            if (keyCode == 22 && bandIdx == bands - 1) { // 마지막 밴드에서 아래로(오른쪽) 돌렸을 때 탈출!
                                View btnSave = containerSettingsItems.findViewById(8500);
                                if (btnSave != null)
                                    btnSave.requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
            eqContainer.addView(bandLayout);
        } // 💡 10밴드 그리기 for문 끝!

        // 🚀 1. 슬라이더가 다 담긴 상자(eqContainer)를 가로 스크롤(hsv)에 넣고 메인 화면에 붙입니다.
        hsv.addView(eqContainer);
        containerSettingsItems.addView(hsv);

        // =========================================================
        // 🚀 2. [완벽 복구] 잃어버린 '저장(Save Profile)' 버튼 부활 및 부착!
        // =========================================================
        Button btnClose = createListButton(t("Save Profile"));
        btnClose.setId(8500);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                String name = currentEqProfile.replace("custom_", "");
                com.themoon.y1.managers.AudioEffectManager.getInstance().saveCustomEqProfile(name);
                com.themoon.y1.managers.AudioEffectManager.getInstance().exportEqProfileToFile(name);
                com.themoon.y1.managers.AudioEffectManager.getInstance().applyEqProfile();

                Toast
                        .makeText(MainActivity.this, t("File saved successfully!"), Toast.LENGTH_SHORT)
                        .show();
                buildEqualizerSettingsUI(); // 이전 설정 메뉴로 돌아가기
            }
        });

        // 🚀 3. 휠을 위아래로 돌렸을 때 첫 번째/마지막 밴드로 스르륵 점프하는 조향 장치
        btnClose.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    // 현재 활성화된 밴드 개수(5 or 10)를 파악
                    int targetBands = isSoftwareEqEnabled ? 10
                            : ((equalizer != null) ? equalizer.getNumberOfBands() : 5);

                    if (keyCode == 21) { // 휠 위로(UP)
                        View lastBand = eqContainer.findViewById(8000 + targetBands - 1);
                        if (lastBand != null)
                            lastBand.requestFocus();
                        clickFeedback();
                        return true;
                    }
                    if (keyCode == 22) { // 휠 아래로(DOWN)
                        View firstBand = eqContainer.findViewById(8000);
                        if (firstBand != null)
                            firstBand.requestFocus();
                        clickFeedback();
                        return true;
                    }
                }
                return false;
            }
        });

        // 🚀 4. 완성된 저장 버튼을 슬라이더 영역(hsv) 바로 아래에 철컥! 부착합니다.
        containerSettingsItems.addView(btnClose);

        // 🚀 5. 렌더링 직후 첫 번째 슬라이더로 포커스 강제 록온!
        containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                View firstBand = eqContainer.findViewById(8000);
                if (firstBand != null) {
                    firstBand.requestFocus();
                } else if (containerSettingsItems.getChildCount() > 2) {
                    containerSettingsItems.getChildAt(2).requestFocus();
                }
            }
        }, 50);
    }

    // 🚀 [네이티브 엔진 1] M3U 리스트 화면 구축
    private void buildM3uPlaylistUI() {
        if (scrollViewBrowser != null)
            scrollViewBrowser.setVisibility(View.VISIBLE);
        if (listVirtualSongs != null)
            listVirtualSongs.setVisibility(View.GONE);
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText(t("Library") + ": " + t("Playlists"));

        // 전용 재생목록 보관함 개설
        File playlistDir = StoragePaths.getPlaylistsDir();
        if (!playlistDir.exists())
            playlistDir.mkdirs();

        File[] files = playlistDir.listFiles();
        List<File> m3uFiles = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                // 🚀 [해결] 파일 이름이 .m3u 이거나(OR) .m3u8 로 끝나는 파일들을 모두 수집합니다!
                String name = f.getName().toLowerCase();
                if (f.isFile() && (name.endsWith(".m3u") || name.endsWith(".m3u8"))) {
                    m3uFiles.add(f);
                }
            }
        }

        // 알파벳 대소문자 구분 없이 깔끔하게 정렬
        java.util.Collections.sort(m3uFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        if (m3uFiles.isEmpty()) {
            View btnEmpty = createListButtonWithIcon("\uE05F", t("No .m3u files found in Y1_Playlists"),
                    ThemeManager.getTextColorSecondary());
            containerBrowserItems.addView(btnEmpty);
        } else {
            for (final File m3u : m3uFiles) {
                // 확장자를 떼고 순수 재생목록 이름만 추출하여 리스트업
                String cleanName = m3u.getName().substring(0, m3u.getName().lastIndexOf("."));
                // Button b = createListButton("📝 " + cleanName);
                View b = createListButtonWithIcon("\uE05F", cleanName);

                // 1. 기존 동작: 짧게 누르면 플레이리스트 내부로 진입
                b.setOnClickListener(v -> {
                    clickFeedback();
                    currentBrowserMode = BROWSER_M3U_SONGS;
                    currentM3uFile = m3u;
                    buildM3uSongsUI(m3u);
                });

                // 🚀 2. 신규 동작: 길게 누르면 플레이리스트 파일 자체를 물리적으로 삭제!
                b.setLongClickable(true); // 길게 누르기 허용
                b.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        clickFeedback();
                        isLongPressConsumed = true; // 🚀 [방어막 장착] 창 닫힐 때 튕기는 휠 클릭 오작동 완벽 방어!

                        // 🚀 [새로운 머티리얼 팝업 호출]
                        // 방금 우리가 만든 예쁘고 둥근 팝업 함수로 파일(m3u)을 던져줍니다!
                        showDeletePlaylistDialog(m3u);

                        return true; // 🚀 true를 반환해야 짧은 클릭(진입) 이벤트가 중복으로 실행되지 않습니다.
                    }
                });

                containerBrowserItems.addView(b);
            }
        }
        // 🚀 [플레이리스트 전용 스크롤 추적기 장착]
        final ScrollView sv = (ScrollView) containerBrowserItems.getParent();
        sv.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    sv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    sv.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                boolean found = false;
                if (!lastBrowserFocusText.isEmpty()) {
                    for (int i = 0; i < containerBrowserItems.getChildCount(); i++) {
                        View v = containerBrowserItems.getChildAt(i);
                        String itemText = "";

                        if (v instanceof Button) {
                            itemText = ((Button) v).getText().toString();
                        } else if (v instanceof LinearLayout) {
                            LinearLayout layout = (LinearLayout) v;
                            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof TextView) {
                                itemText = ((TextView) layout.getChildAt(1)).getText().toString();
                            }
                        }

                        if (itemText.equals(lastBrowserFocusText)) {
                            // 🚀 [원본 위치 100% 복원]
                            int offset = (sv.getHeight() / 2) - (v.getHeight() / 2);
                            if (exactOffsetMemory.containsKey(itemText))
                                offset = exactOffsetMemory.get(itemText);

                            int targetY = v.getTop() - offset;
                            if (targetY < 0)
                                targetY = 0;

                            sv.scrollTo(0, targetY);
                            v.requestFocus();
                            found = true;
                            break;
                        }
                    }
                }
                if (!found && containerBrowserItems.getChildCount() > 0) {
                    containerBrowserItems.getChildAt(0).requestFocus();
                }
                lastBrowserFocusText = "";
            }
        });
    }

    // =======================================================
    // 🚀 [팝업 2] 플레이리스트 목록에서 '플레이리스트 파일 자체'를 지울 때 뜨는 커스텀 팝업
    // =======================================================
    public void showDeletePlaylistDialog(final File m3uFile) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        float d = getResources().getDisplayMetrics().density;

        final LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0x88000000);
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (15 * d), (int) (20 * d), (int) (15 * d), (int) (15 * d));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(t("Delete Playlist"));
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (10 * d));
        rootLayout.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(t("Do you want to permanently delete this playlist?"));
        tvMsg.setTextColor(ThemeManager.getTextColorSecondary());
        tvMsg.setTextSize(14f);
        tvMsg.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, 0, 0, (int) (20 * d));
        tvMsg.setLineSpacing(10f, 1.2f);
        rootLayout.addView(tvMsg);

        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 19 || keyCode == 21) {
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx - 1; i >= 0; i--) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                    if (keyCode == 20 || keyCode == 22) {
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx + 1; i < rootLayout.getChildCount(); i++) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        View btnDelete = createListButtonWithIcon("\uE872", t("Delete"), 0xFFFF5555);
        btnDelete.setOnKeyListener(dialogWheelListener);
        btnDelete.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
            if (m3uFile.exists() && m3uFile.delete()) {
                Toast.makeText(MainActivity.this, t("Playlist deleted."), Toast.LENGTH_SHORT).show();
                // 🚀 삭제 후 리스트 갱신 함수 (자기 환경에 맞춰 호출!)
                buildM3uPlaylistUI();
            } else {
                Toast.makeText(MainActivity.this, t("Failed to delete playlist."), Toast.LENGTH_SHORT).show();
            }
        });
        rootLayout.addView(btnDelete);

        View btnCancel = createListButtonWithIcon("\uE5CD", t("Cancel"));
        btnCancel.setOnKeyListener(dialogWheelListener);
        btnCancel.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
        });
        rootLayout.addView(btnCancel);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        // 팝업이 뜨면 '취소' 버튼(인덱스 3)에 휠 포커스 고정!
        rootLayout.postDelayed(() -> {
            if (rootLayout.getChildCount() > 3) {
                rootLayout.getChildAt(3).requestFocus();
            }
        }, 50);
    }

    // 🚀 [네이티브 엔진 2] M3U 실시간 텍스트 경로 파서 (핵심 디테일 공정)
    private List<SongItem> parseM3uFile(File m3uFile) {
        List<SongItem> songs = new ArrayList<>();
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(m3uFile), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // 빈 줄이거나 주석(#EXTINF 등) 라인은 매끄럽게 통과
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                // 윈도우 스타일 역슬래시(\)가 섞여있다면 리눅스/안드로이드용 슬래시(/)로 자동 치환 보정!
                line = line.replace("\\", "/");

                File audioFile = new File(line);
                // 만약 PC용 상대 경로 파일 형태라면 기본 음악 폴더(Music) 기준으로 강제 맵핑 보정!
                if (!audioFile.isAbsolute()) {
                    audioFile = new File(rootFolder, line);
                }

                // 해당 경로에 진짜 음원이 살아숨쉬고 있는지 물리적 최종 검증
                if (audioFile.exists() && isAudioFile(audioFile)) {
                    String title = audioFile.getName();
                    // 확장자 제거
                    int dotIdx = title.lastIndexOf(".");
                    if (dotIdx > 0)
                        title = title.substring(0, dotIdx);

                    // 네이티브 구동 속도를 위해 무거운 태그 조회는 생략하고 제목만 들고 광속 조립!
                    songs.add(new SongItem(audioFile, title, "M3U Playlist", ""));
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songs;
    }

    // 🚀 [네이티브 엔진 3] 추출된 곡들을 초고속 재활용 엔진(ListView)에 직결 송출
    private void buildM3uSongsUI(File m3uFile) {
        scrollViewBrowser.setVisibility(View.GONE);
        listVirtualSongs.setVisibility(View.VISIBLE);
        tvBrowserPath
                .setText(t("Playlist") + ": " + m3uFile.getName().substring(0, m3uFile.getName().lastIndexOf(".")));

        virtualSongList.clear();
        currentScrollIndexList.clear();

        List<SongItem> songs = parseM3uFile(m3uFile);

        // 정렬하지 않고 사용자가 .m3u 파일 안에 수동으로 배열해둔 순서 "그대로" 보존하여 장전합니다!
        for (SongItem song : songs) {
            virtualSongList.add(song.file);
            currentScrollIndexList.add(song.title);
        }

        if (songs.isEmpty()) {
            Toast.makeText(this, "No valid tracks found in this playlist.", Toast.LENGTH_SHORT).show();
        }

        SongListAdapter adapter = new SongListAdapter(songs);
        listVirtualSongs.setAdapter(adapter);

        // =======================================================
        int targetIndex = -1;
        if (currentPlaylist != null && !currentPlaylist.isEmpty() && currentIndex >= 0
                && currentIndex < currentPlaylist.size()) {
            File playingFile = currentPlaylist.get(currentIndex);
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).file.getAbsolutePath().equals(playingFile.getAbsolutePath())) {
                    targetIndex = i;
                    break;
                }
            }
        }

        final int finalTargetIdx = targetIndex;
        listVirtualSongs.post(new Runnable() {
            @Override
            public void run() {
                if (finalTargetIdx >= 0 && finalTargetIdx < listVirtualSongs.getCount()) {
                    listVirtualSongs.setSelectionFromTop(finalTargetIdx, 0);
                    listVirtualSongs.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int visiblePos = finalTargetIdx - listVirtualSongs.getFirstVisiblePosition();
                            if (visiblePos >= 0 && visiblePos < listVirtualSongs.getChildCount()) {
                                listVirtualSongs.getChildAt(visiblePos).requestFocus();
                            }
                        }
                    }, 50);
                } else {
                    if (listVirtualSongs.getChildCount() > 0) {
                        listVirtualSongs.getChildAt(0).requestFocus();
                    }
                }
            }
        });
        buildAlphabetIndexBar();
    }

    // 🚀 [디자인 개조 및 휠 버그 완벽 해결]
    public void showAddToPlaylistDialog(final File songFile) {
        final File playlistDir = StoragePaths.getPlaylistsDir();        // =======================================================
        // 🚀 [팟캐스트 플레이리스트 납치 차단 방어막]
        // 물리 버튼 롱클릭 시 어댑터를 무시하고 여기로 다이렉트로 꽂히는 현상을 원천 차단합니다!
        // =======================================================
        boolean isPodcast = (currentBrowserMode == 14) ||
                (songFile != null && songFile.getAbsolutePath().contains("/Podcasts/")) ||
                (songFile != null && "/PODCAST".equals(songFile.getAbsolutePath()));

        if (isPodcast) {
            clickFeedback();
            try {
                // 1. 팟캐스트 에피소드 리스트(가상 리스트)에서 눌렀을 때
                if (listVirtualSongs != null && listVirtualSongs.getVisibility() == View.VISIBLE) {
                    int pos = listVirtualSongs.getSelectedItemPosition();
                    View focusedChild = listVirtualSongs.getFocusedChild();
                    if (focusedChild != null) pos = listVirtualSongs.getPositionForView(focusedChild);

                    if (pos >= 0 && listVirtualSongs.getAdapter() != null) {
                        com.themoon.y1.models.SongItem song = (com.themoon.y1.models.SongItem) listVirtualSongs.getAdapter().getItem(pos);

                        String safeChannel = song.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
                        String safeTitle = song.title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp3";
                        File localFile = new File("/storage/sdcard0/Podcasts/" + safeChannel, safeTitle);

                        // 만약 전체 곡, 최근 추가된 곡에서 실제 다운로드된 파일을 눌렀다면 타겟 재조정
                        if (song.file != null && song.file.getAbsolutePath().contains("/Podcasts/") && song.file.exists()) {
                            localFile = song.file;
                        }

                        if (localFile.exists() && localFile.length() > 0) {
                            showDeletePodcastDialog(localFile, song.title); // 🎯 삭제 팝업 소환!
                        } else {
                            android.widget.Toast.makeText(this, "📡 " + t("Offline Mode: This episode has not been downloaded yet."), android.widget.Toast.LENGTH_SHORT).show();
                        }
                        return; // 🚨 가로채기 완료! 여기서 즉시 함수를 빠져나가서 아래 코드를 씹어버립니다.
                    }
                }
                // 2. 폴더 탐색기 등에서 실제 다운로드된 MP3를 다이렉트로 길게 눌렀을 때
                else if (songFile != null && songFile.exists() && songFile.getAbsolutePath().contains("/Podcasts/")) {
                    String title = songFile.getName().replace(".mp3", "");
                    showDeletePodcastDialog(songFile, title); // 🎯 삭제 팝업 소환!
                    return; // 🚨 가로채기 완료!
                }
            } catch (Exception e) {}

            return; // 🚨 에러가 나더라도 팟캐스트면 무조건 일반 플레이리스트에 등록되는 걸 차단합니다!
        }
      //  final File playlistDir = new File("/storage/sdcard0/Y1_Playlists");
        if (!playlistDir.exists())
            playlistDir.mkdirs();

        File[] files = playlistDir.listFiles();
        final List<File> playlistFiles = new ArrayList<>();

        if (files != null) {
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (f.isFile() && (name.endsWith(".m3u") || name.endsWith(".m3u8"))) {
                    playlistFiles.add(f);
                }
            }
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF222222);

        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        scrollView.addView(layout);

        // 🚀 1. 촌스러운 시스템 타이틀 대신, 메인 화면과 똑같은 '커스텀 타이틀'을 우리가 직접 그립니다!
        TextView tvTitle = new TextView(this);
        tvTitle.setText("━ " + t("ADD TO PLAYLIST") + " ━");
        tvTitle.setTextColor(0xFFFFFFFF); // 하늘색으로 예쁘게!
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 10, 0, 30);
        tvTitle.setTextSize(16);
        layout.addView(tvTitle);

        // 🚀 2. 팝업창 안쪽에서도 휠(21, 22)을 인식하게 만드는 '팝업 전용 조향 장치(Listener)'
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21) { // 휠 위로 (UP)
                        int idx = layout.indexOfChild(v);
                        for (int i = idx - 1; i >= 0; i--) {
                            if (layout.getChildAt(i).isFocusable()) {
                                layout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true; // 위가 막히면 정지
                    }
                    if (keyCode == 22) { // 휠 아래로 (DOWN)
                        int idx = layout.indexOfChild(v);
                        for (int i = idx + 1; i < layout.getChildCount(); i++) {
                            if (layout.getChildAt(i).isFocusable()) {
                                layout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true; // 아래가 막히면 정지
                    }
                }
                return false;
            }
        };

        // 🚀 3. 시스템 팝업을 만들 때, 순정 타이틀(.setTitle)을 빼버려서 보이지 않게 만듭니다!
        final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(scrollView)
                .create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 4. [첫 번째 버튼] 새 플레이리스트 만들기
        View btnNew = createListButtonWithIcon("\uE145", t("Create New Playlist"));
        btnNew.setOnKeyListener(dialogWheelListener); // 🚀 버튼에 팝업 전용 조향 장치 연결!
        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                int count = 1;
                File newPlaylistFile;
                do {
                    newPlaylistFile = new File(playlistDir, t("Playlist ") + count + ".m3u8");
                    count++;
                } while (newPlaylistFile.exists());

                writeSongToM3uFile(newPlaylistFile, songFile, false);
                Toast.makeText(MainActivity.instance, t("Created Playlist ") + (count - 1) + " " + t("successfully!"),
                        Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        layout.addView(btnNew);

        // 5. [나머지 버튼들] 기존 플레이리스트 파일들 목록
        for (final File targetM3u : playlistFiles) {
            String cleanName = targetM3u.getName().substring(0, targetM3u.getName().lastIndexOf("."));

            View btnExisting = createListButtonWithIcon("\uE05F", cleanName);

            btnExisting.setOnKeyListener(dialogWheelListener); // 🚀 버튼에 팝업 전용 조향 장치 연결!
            btnExisting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickFeedback();
                    writeSongToM3uFile(targetM3u, songFile, true);
                    Toast.makeText(MainActivity.instance, t("Added to playlist successfully!"), Toast.LENGTH_SHORT)
                            .show();
                    dialog.dismiss();
                }
            });
            layout.addView(btnExisting);
        }

        dialog.show();

        // 6. 팝업창이 열리면 자동으로 '첫 번째 버튼'에 포커스 꽂아주기
        layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 레이아웃의 0번 인덱스는 '커스텀 타이틀 텍스트'이므로, 포커스는 1번(btnNew)에 줍니다!
                if (layout.getChildCount() > 1)
                    layout.getChildAt(1).requestFocus();
            }
        }, 50);
    }

    // 🚀 [자체 플레이리스트 엔진 4단계] 실시간 하드디스크 물리 레코딩 스트림
    private void writeSongToM3uFile(File m3uFile, File songFile, boolean append) {
        try {
            java.io.BufferedWriter bw = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new FileOutputStream(m3uFile, append), "UTF-8"));

            // 새 파일인 경우 표준 재생목록 헤더 규격을 명시해 줍니다.
            if (!append) {
                bw.write("#EXTM3U\n");
            }

            // 곡의 절대 경로 주소를 안전하게 마킹한 뒤 줄 바꿈 처리
            bw.write(songFile.getAbsolutePath() + "\n");
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =======================================================
    // 🚀 [팝업 1] 플레이리스트 안에서 '특정 노래'를 지울 때 뜨는 커스텀 팝업
    // =======================================================
    public void showRemoveFromPlaylistDialog(final File audioFile) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        float d = getResources().getDisplayMetrics().density;

        final LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0x88000000);
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (15 * d), (int) (20 * d), (int) (15 * d), (int) (15 * d));

        // 제목
        TextView tvTitle = new TextView(this);
        tvTitle.setText(t("Remove from Playlist"));
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (10 * d));
        rootLayout.addView(tvTitle);

        // 설명 메시지
        TextView tvMsg = new TextView(this);
        tvMsg.setText(t("Do you want to remove this track from the playlist?"));
        tvMsg.setTextColor(ThemeManager.getTextColorSecondary());
        tvMsg.setTextSize(14f);
        tvMsg.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, 0, 0, (int) (20 * d));
        tvMsg.setLineSpacing(10f, 1.2f);
        rootLayout.addView(tvMsg);

        // 🚀 팝업 내 휠 조작 리스너
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 19 || keyCode == 21) { // 휠 위로(UP)
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx - 1; i >= 0; i--) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                    if (keyCode == 20 || keyCode == 22) { // 휠 아래로(DOWN)
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx + 1; i < rootLayout.getChildCount(); i++) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        // 삭제 버튼 (빨간색)
        View btnRemove = createListButtonWithIcon("\uE872", t("Remove"), 0xFFFF5555);
        btnRemove.setOnKeyListener(dialogWheelListener);
        btnRemove.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
            removeSongFromM3uFile(currentM3uFile, audioFile);
            buildM3uSongsUI(currentM3uFile);
            Toast.makeText(MainActivity.this, t("Track removed."), Toast.LENGTH_SHORT).show();
        });
        rootLayout.addView(btnRemove);

        // 취소 버튼
        View btnCancel = createListButtonWithIcon("\uE5CD", t("Cancel"));
        btnCancel.setOnKeyListener(dialogWheelListener);
        btnCancel.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
        });
        rootLayout.addView(btnCancel);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        // 팝업이 뜨면 '취소' 버튼(인덱스 3)에 휠 포커스 강제 고정!
        rootLayout.postDelayed(() -> {
            if (rootLayout.getChildCount() > 3) {
                rootLayout.getChildAt(3).requestFocus();
            }
        }, 50);
    }

    // 🚀 [자체 플레이리스트 엔진 6단계] M3U 파일에서 해당 곡의 텍스트 줄만 찾아 지우는 기능
    public void removeSongFromM3uFile(File m3uFile, File songFile) {
        if (m3uFile == null || !m3uFile.exists())
            return;
        try {
            List<String> lines = new ArrayList<>();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(m3uFile), "UTF-8"));
            String line;
            boolean isRemoved = false; // 💡 동일한 곡이 여러 개 담겼을 경우, 한 번에 하나씩만 지우도록 방어

            while ((line = br.readLine()) != null) {
                String cleanLine = line.replace("\\", "/").trim();

                // 주석이나 빈 줄은 삭제하지 않고 그대로 통과시켜 M3U 본연의 형식을 보존합니다.
                if (cleanLine.isEmpty() || cleanLine.startsWith("#")) {
                    lines.add(line);
                    continue;
                }

                // 지울 노래와 파일명이 일치하고, 아직 이번 타임에 지운 적이 없다면 (리스트에 넣지 않고 스킵 = 삭제)
                if (!isRemoved && cleanLine.endsWith(songFile.getName())) {
                    isRemoved = true;
                    continue;
                }

                lines.add(line); // 삭제 대상이 아닌 곡들은 그대로 유지
            }
            br.close();

            // 갱신된(한 곡이 빠진) 리스트를 원본 M3U 파일에 덮어쓰기 (Append = false)
            java.io.BufferedWriter bw = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new FileOutputStream(m3uFile, false), "UTF-8"));
            for (String l : lines) {
                bw.write(l + "\n");
            }
            bw.close();

            Toast.makeText(this, t("Removed successfully."), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🚀 [보너스] 즐겨찾기(Favorites) 내부에서도 꾹 누르면 바로 해제할 수 있는 전용 팝업!
    public void showRemoveFromFavoritesDialog(final File songFile) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(t("Remove from Favorites"))
                .setMessage(t("Remove this song from your favorites list?"))
                .setPositiveButton(t("Remove"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        clickFeedback();
                        if (favoritePaths.contains(songFile.getAbsolutePath())) {
                            favoritePaths.remove(songFile.getAbsolutePath());
                            try {
                                prefs.edit().putStringSet("favorites", favoritePaths).commit();
                            } catch (Exception e) {
                            }
                            buildVirtualSongsForFavorites(); // 화면 즉시 갱신
                            Toast.makeText(MainActivity.instance, t("Removed from Favorites."), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                })
                .setNegativeButton(t("Cancel"), null)
                .show();
    }

    public void updateAudioQualityInfo(File audioFile) {
        if (isVisualizerShowing && !ThemeManager.availableThemes.isEmpty()) {
            ThemeManager.ThemeData theme = ThemeManager.getCurrentTheme();
            if (theme != null && theme.playerElements != null && !theme.playerElements.isEmpty()) {
                if (layoutAudioQualityContainer != null) layoutAudioQualityContainer.setVisibility(View.GONE);
                return;
            }
        }
        if (layoutAudioQualityContainer == null || audioFile == null || !audioFile.exists()) {
            if (layoutAudioQualityContainer != null)
                layoutAudioQualityContainer.setVisibility(View.GONE);
            return;
        }

        String ext = "";
        String name = audioFile.getName().toLowerCase();
        int dotIdx = name.lastIndexOf(".");
        if (dotIdx > 0)
            ext = name.substring(dotIdx + 1).toUpperCase();

        String bitrateStr = "";
        long durationMs = 0;
        int kbpsNum = 0; // 🚀 비트레이트 숫자 저장용 변수 추가

        // 🚀 1. 순정 부품으로 비트레이트와 곡 길이를 먼저 빼옵니다.
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(audioFile.getAbsolutePath());
            String br = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (br != null && !br.isEmpty()) {
                kbpsNum = Integer.parseInt(br) / 1000;
                bitrateStr = kbpsNum + " kbps";
            }

            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (dur != null && !dur.isEmpty())
                durationMs = Long.parseLong(dur);
            mmr.release();
        } catch (Exception e) {
        }

        // 🚀 2. 순정 부품이 기절했다면, 물리적인 파일 용량과 시간으로 '절대 속일 수 없는 평균 비트레이트' 역산!
        if (bitrateStr.isEmpty() || bitrateStr.equals("0 kbps")) {
            if (durationMs <= 0) {
                durationMs = com.themoon.y1.managers.AudioPlayerManager.getInstance().getDuration();
            }
            if (durationMs > 0) {
                long fileSize = audioFile.length();
                kbpsNum = (int) ((fileSize * 8) / durationMs); // (바이트 * 8) / 밀리초 = kbps
                bitrateStr = kbpsNum + " kbps";
            }
        }

        // 🚀 [신규 엔진] 3. 비트레이트 계산이 끝난 후, 물리적 용량을 바탕으로 무손실 여부를 깐깐하게 판독합니다!
        boolean isLossless = ext.equals("FLAC") || ext.equals("WAV") || ext.equals("APE") || ext.equals("ALAC");

        // 💡 애플의 M4A 껍데기 속 알맹이 판별 로직: M4A인데 비트레이트가 400 kbps를 넘으면 무조건 ALAC(무손실)입니다!
        if (ext.equals("M4A") && kbpsNum > 400) {
            isLossless = true;
            ext = "ALAC"; // 🎯 화면에도 M4A 대신 'ALAC'라는 영광스러운 이름을 달아줍니다!
        }

        String formatTag = isLossless ? "LOSSLESS" : "LOSSY";
        if (ext.equals("WAV"))
            formatTag = "UNCOMPRESSED";

        tvQualityExt.setText(ext);
        tvQualityFormat.setText(formatTag);

        if (!bitrateStr.isEmpty() && !bitrateStr.equals("0 kbps")) {
            tvQualityBitrate.setText(bitrateStr);
            tvQualityBitrate.setVisibility(View.VISIBLE);
        } else {
            tvQualityBitrate.setVisibility(View.GONE);
        }

        layoutAudioQualityContainer.setVisibility(View.VISIBLE);
        qualityInfoHandler.removeCallbacks(hideQualityInfoTask);
        qualityInfoHandler.postDelayed(hideQualityInfoTask, 3000);
    }

    // 🚀 [신규 1] 오디오북 버튼 전용 포커스 유지 리스너 조립기 (하이브리드 유니코드 뷰 대응 완료!)
    public void setupAudiobookProgress(final View btn, final int pos, final int dur) {
        // 처음 화면에 나타날 때 그리기
        applyProgressBackground(btn, pos, dur, btn.hasFocus());

        // 💡 버튼이 원래 가지고 있던 단순 단색 포커스 리스너를 '프로그레스 전용 리스너'로 덮어씌웁니다!
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (v instanceof Button) {
                        ((Button) v).setTextColor(ThemeManager.getListButtonFocusedTextColor());
                        showFastScrollLetter(((Button) v).getText().toString());
                    } else if (v instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) v;
                        if (row.getChildCount() > 1) {
                            ((TextView) row.getChildAt(0))
                                    .setTextColor(ThemeManager.getListButtonFocusedTextColor());
                            ((TextView) row.getChildAt(1))
                                    .setTextColor(ThemeManager.getListButtonFocusedTextColor());
                            showFastScrollLetter(((TextView) row.getChildAt(1)).getText().toString());
                        }
                    }
                    applyProgressBackground(btn, pos, dur, true);
                } else {
                    if (v instanceof Button) {
                        ((Button) v).setTextColor(ThemeManager.getTextColorPrimary());
                    } else if (v instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) v;
                        if (row.getChildCount() > 1) {
                            ((TextView) row.getChildAt(0))
                                    .setTextColor(ThemeManager.getTextColorPrimary());
                            ((TextView) row.getChildAt(1))
                                    .setTextColor(ThemeManager.getTextColorPrimary());
                        }
                    }
                    applyProgressBackground(btn, pos, dur, false);
                }
            }
        });
    }

    // 🚀 [신규 2] 포커스 상태(isFocused)에 따라 색상을 똑똑하게 조절하는 프로그레스 렌더링 함수 (유니코드 뷰 대응 완료!)
    public void applyProgressBackground(View btn, int currentMs, int totalMs, boolean isFocused) {
        if (currentMs <= 0 || totalMs <= 0)
            return;

        int progressPercent = (int) (((float) currentMs / totalMs) * 10000);
        if (progressPercent > 10000)
            progressPercent = 10000;

        int baseColor = isFocused ? ThemeManager.getListButtonFocusedBg() : ThemeManager.getListButtonNormalBg();
        android.graphics.drawable.Drawable baseBg = createButtonBackground(baseColor);

        int progressColor;
        if (isFocused) {
            progressColor = 0x66FFFFFF; // 휠이 닿았을 때: 눈에 확 띄는 반투명 화이트
        } else {
            progressColor = (ThemeManager.getListButtonFocusedBg() & 0x00FFFFFF) | 0x44000000; // 평소: 테마색 반투명
        }
        android.graphics.drawable.Drawable progressBg = createButtonBackground(progressColor);

        android.graphics.drawable.ClipDrawable clipProgress = new android.graphics.drawable.ClipDrawable(progressBg,
                Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL);
        clipProgress.setLevel(progressPercent);

        android.graphics.drawable.LayerDrawable layerBg = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[] { baseBg, clipProgress });

        // 🚀 [여백 증발 버그 완벽 차단!] 배경을 바꾸기 전에 기존 여백(Padding)을 안전하게 기억해 둡니다.
        int pLeft = btn.getPaddingLeft();
        int pTop = btn.getPaddingTop();
        int pRight = btn.getPaddingRight();
        int pBottom = btn.getPaddingBottom();

        btn.setBackground(layerBg);
        btn.setPadding(pLeft, pTop, pRight, pBottom); // 💡 날아간 여백을 즉시 100% 복구합니다!

        // 🚀 유니코드 뷰 내부에서 텍스트(제목)를 가지고 있는 객체를 정확히 찾아내어 업데이트합니다.
        TextView targetTv = null;
        if (btn instanceof Button) {
            targetTv = (TextView) btn;
        } else if (btn instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) btn;
            if (row.getChildCount() > 1) {
                targetTv = (TextView) row.getChildAt(1); // 💡 두 번째 자식이 바로 텍스트뷰!
            }
        }

        if (targetTv != null) {
            String originalText = targetTv.getText().toString();
            if (originalText.contains("  ⏱")) {
                originalText = originalText.substring(0, originalText.indexOf("  ⏱"));
            }

            // 🚀 [말줄임표 지능형 보호] 글자 수를 억지로 자르던 낡은 코드를 삭제하고,
            // 텍스트의 '가운데'를 줄여주는 MIDDLE 옵션을 가동하여 끝에 달린 재생 시간(⏱)을 절대 잘리지 않게 방어합니다!
            targetTv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);

            int min = (currentMs / 1000) / 60;
            int maxMin = (totalMs / 1000) / 60;
            targetTv.setText(originalText + "  ⏱ [" + min + "m / " + maxMin + "m]");
        }
    }

    // 🚀 [신규 엔진] 휠 조작 주파수 실시간 전체 화면 팝업 제어기
    private Handler radioFreqHandler = new Handler();
    private Runnable hideRadioFreqTask = new Runnable() {
        @Override
        public void run() {
            if (layoutLoadingOverlay != null) {
                layoutLoadingOverlay.setVisibility(View.GONE); // 팝업 닫기
                if (pbLoadingProgress != null)
                    pbLoadingProgress.setVisibility(View.VISIBLE); // 프로그레스 바 상태 원상 복구
            }
        }
    };

    private void showRadioFreqPopup(float freq) {
        if (layoutLoadingOverlay != null) {
            radioFreqHandler.removeCallbacks(hideRadioFreqTask);

            // 🚀 [수리 1] 투명인간 버그 해결: 가상 암전 모드가 0%로 만들어버린 투명도를 다시 100%로 강제 복구합니다!
            layoutLoadingOverlay.setAlpha(1.0f);
            layoutLoadingOverlay.setVisibility(View.VISIBLE);

            if (pbLoadingProgress != null) {
                pbLoadingProgress.setVisibility(View.VISIBLE);
                int progress = (int) (((freq - 87.5f) / 20.5f) * 100);
                pbLoadingProgress.setProgress(progress);
            }

            if (tvLoadingProgress != null) {
                tvLoadingProgress.setTextSize(24f);
                // 🚀 [수리 2] 실수로 주석(//) 처리되어 잠들어 있던 텍스트 출력 엔진을 다시 살려냅니다!
                tvLoadingProgress
                        .setText(String.format(Locale.US, t("Tuning Frequency...\n\n%.1f MHz"), freq));
            }

            radioFreqHandler.postDelayed(hideRadioFreqTask, 1500);
        }
    }

    // 🚀 [궁극의 아키텍처 완료] 하드코딩 용어 없이 오직 객체간의 parent_id 결합 구조만 보고 실시간으로 스위칭하는 동적 레이아웃
    // 엔진
    private void updateFocusPreviewLiveContent(ThemeManager.MenuElement focusedElement) {
        FrameLayout canvas = (FrameLayout) layoutMainMenu.findViewWithTag("dynamic_canvas");
        if (canvas == null)
            return;

        // 현재 테마에 등록된 모든 메뉴 요소 명단을 꺼내옵니다.
        List<ThemeManager.MenuElement> allElements = ThemeManager.getCurrentTheme().menuElements;

        boolean hasLiveWidgetActivated = false;

        // 🚀 [1차 스캔] 도화지에 깔려있는 모든 위젯들을 전수 조사하여 분기 필터링을 가합니다!
        for (ThemeManager.MenuElement el : allElements) {
            if (el.type.equals("button") || el.type.equals("box") || el.type.equals("list_box"))
                continue;

            // 해당 위젯의 실제 객체 주소를 캔버스 도화지 안에서 수소문해 옵니다.
            View widgetView = null;
            if (el.type.equals("widget_clock"))
                widgetView = tvWidgetClock;
            else if (el.type.equals("widget_battery"))
                widgetView = widgetBatteryView;
            else if (el.type.equals("widget_album"))
                widgetView = layoutWidgetAlbumContainer;
            else if (el.type.equals("widget_analog_clock"))
                widgetView = customAnalogClockView;
            else if (el.type.equals("widget_circular_battery"))
                widgetView = customCircularBatteryView;
            else if (el.type.equals("widget_focus_image"))
                widgetView = ivWidgetFocusImage;

            if (widgetView == null)
                continue;

            // 💡 [핵심 조건문] 이 위젯이 "현재 포커스된 버튼의 ID"를 자기의 감시 대상(visibleOnFocus)으로 명시해 두었는가?!
            if (focusedElement.id.equals(el.visibleOnFocus)) {

                // 👉 매칭 성공! 사용자가 원하는 위치(JSON에 적어둔 x, y)에서 진짜 위젯의 전원을 켜줍니다!
                widgetView.setVisibility(View.VISIBLE);
                hasLiveWidgetActivated = true;

                // 특별히 앨범 정보 위젯의 경우, 진짜 나우플레잉 연동 데이터를 실시간 주입 새로고침해 줍니다.
                if (el.type.equals("widget_album")) {
                    if (tvWidgetAlbumTitle != null && tvPlayerTitle != null)
                        tvWidgetAlbumTitle.setText(tvPlayerTitle.getText());
                    if (tvWidgetAlbumArtist != null && tvPlayerArtist != null)
                        tvWidgetAlbumArtist.setText(tvPlayerArtist.getText());
                    if (ivWidgetAlbum != null) {
                        if (lastAlbumArtBytes != null && lastAlbumArtBytes.length > 0) {
                            try {
                                BitmapFactory.Options opts = new BitmapFactory.Options();
                                opts.inSampleSize = 2;
                                ivWidgetAlbum.setImageBitmap(BitmapFactory
                                        .decodeByteArray(lastAlbumArtBytes, 0, lastAlbumArtBytes.length, opts));
                            } catch (Exception e) {
                                ivWidgetAlbum.setImageBitmap(ThemeManager.getCustomIcon("icon_default_album.png", this,
                                        R.drawable.default_album));
                            }
                        } else {
                            ivWidgetAlbum.setImageBitmap(ThemeManager.getCustomIcon("icon_default_album.png", this,
                                    R.drawable.default_album));
                        }
                    }
                }
            }
            // 💡 만약 이 위젯의 감시 대상(visibleOnFocus)이 비어있다면, 사용자가 테마 설정에서 지정한 전역 스위치(isWidgetOn)
            // 규칙에 따릅니다!
            else if (el.visibleOnFocus == null || el.visibleOnFocus.trim().isEmpty()) {
                if (el.type.equals("widget_clock"))
                    widgetView.setVisibility(isWidgetClockOn ? View.VISIBLE : View.GONE);
                else if (el.type.equals("widget_battery"))
                    widgetView.setVisibility(isWidgetBatteryOn ? View.VISIBLE : View.GONE);
                else if (el.type.equals("widget_album"))
                    widgetView.setVisibility(isWidgetAlbumOn ? View.VISIBLE : View.GONE);
                else if (el.type.equals("widget_analog_clock"))
                    widgetView.setVisibility(isWidgetAnalogClockOn ? View.VISIBLE : View.GONE);
                else if (el.type.equals("widget_circular_battery"))
                    widgetView.setVisibility(isWidgetCircularBatteryOn ? View.VISIBLE : View.GONE);
                else if (el.type.equals("widget_focus_image"))
                    widgetView.setVisibility(isWidgetFocusImageOn ? View.VISIBLE : View.GONE);
            }
            // 💡 현재 포커스된 버튼과 상관없는 다른 버튼의 전용 종속 위젯이라면 전원을 차단합니다.
            else {
                widgetView.setVisibility(View.GONE);
            }
        }

        // 🚀 [2차 스캔] 만약 어떤 버튼에 포커스가 갔는데, 그 버튼을 부모로 삼는 특수 위젯이 JSON 상에 단 하나도 없다면?
        // 기존 100% 미니멀 설계 사명에 충족하도록 버튼의 고유 preview_image 이미지를 메인 전체 화면
        // 뷰포트(ivWidgetFocusImage)에 가득 채워 띄워줍니다!
        if (ivWidgetFocusImage != null) {
            if (!hasLiveWidgetActivated && focusedElement.previewImage != null
                    && !focusedElement.previewImage.isEmpty()) {
                ivWidgetFocusImage.setVisibility(View.VISIBLE);
                Bitmap bmpPreview = ThemeManager.getCustomIcon(focusedElement.previewImage, this, 0);
                if (bmpPreview != null)
                    ivWidgetFocusImage.setImageBitmap(bmpPreview);
                else
                    ivWidgetFocusImage.setImageDrawable(null);
            } else if (!hasLiveWidgetActivated) {
                // 보여줄 라이브 위젯도 없고, 이미지도 지정 안 된 일반 버튼이면 프리뷰 공간을 깔끔하게 비웁니다.
                ivWidgetFocusImage.setImageDrawable(null);
            } else {
                // 🚀 [초기화 처리 완벽 장착] 라이브 위젯(앨범/시계)이 활성화된 버튼(Now Playing 등)일 때는
                // 이전 버튼이 남긴 프리뷰 이미지 잔상이 절대로 겹치지 않도록 내부 비트맵 이미지를 깨끗이 파쇄합니다!
                ivWidgetFocusImage.setImageDrawable(null);

                // 💡 만약 프리뷰 이미지 위젯 자체가 현재 버튼의 전용 라이브 위젯으로 등록된 게 아니라면 안전하게 GONE 가림막을 칩니다.
                boolean isFocusImageLiveForCurrentBtn = false;
                for (ThemeManager.MenuElement el : allElements) {
                    if (el.type.equals("widget_focus_image") && focusedElement.id.equals(el.visibleOnFocus)) {
                        isFocusImageLiveForCurrentBtn = true;
                        break;
                    }
                }
                if (!isFocusImageLiveForCurrentBtn) {
                    ivWidgetFocusImage.setVisibility(View.GONE);
                }
            }
        }
    }

    // 💡 [지능형 버그 수리 완결] 상단 헤더를 제거하고, 중간 메뉴 제어 시 포커스 락을 완벽하게 유지하는 에디터 서브 메뉴
    public void buildMainMenuVisibilitySettingsUI() {
        currentSettingsDepth = 2; // 🚀 카테고리(0) → 서브 메뉴(1) → 이 화면(2)
        containerSettingsItems.removeAllViews();
        com.themoon.y1.managers.SettingsMenuManager.getInstance(this).updateSettingsTitle(t("Main Menu Items"));
        // ❌ 아티스트님의 요청에 따라 상단 카테고리 헤더 텍스트("━ SHOW / HIDE MENUS ━")를 흔적도 없이 완전히 삭제했습니다!

        // 1. 현재 테마의 메인 메뉴 버튼들을 순서대로 정렬하여 가져옵니다.
        List<ThemeManager.MenuElement> buttons = new ArrayList<>();
        for (ThemeManager.MenuElement el : ThemeManager.getCurrentTheme().menuElements) {
            if (el.type.equals("button"))
                buttons.add(el);
        }
        settingsSubMode = 4; // 🚀 우리가 이 메뉴 편집창에 있다는 것을 시스템에 알리는 플래그!
        sortMenuElements(buttons);

        // 2. 각 버튼마다 숨김/표시(HIDDEN/SHOW) 스위치를 달아줍니다.
        for (int i = 0; i < buttons.size(); i++) {
            final ThemeManager.MenuElement el = buttons.get(i);
            final int currentItemIndex = i; // 🚀 현재 이 버튼이 몇 번째 줄인지 인덱스 박제

            boolean isHidden = prefs.getBoolean("hide_btn_" + el.id, false);
            String btnName = (el.textNormal != null && !el.textNormal.trim().isEmpty()) ? el.textNormal : el.id;

            // 🚀 [OS 호환성 버그 완벽 해결]
            // 구형 기기에서는 최신 이모지(👁)를 인식하지 못해 증발합니다.
            // 이를 막기 위해 앱에 내장된 '머티리얼 아이콘 폰트'를 직접 로드하여 그려냅니다!
            if (materialIconFont == null) {
                try {
                    materialIconFont = Typeface.createFromAsset(getAssets(),
                            "fonts/MaterialIcons-Regular.ttf");
                } catch (Exception e) {
                }
            }

            // 💡 머티리얼 아이콘 전용 유니코드 (표시: \uE8F4 / 숨김: \uE5CD)
            String stateIcon = isHidden ? "\uE5CD" : "\uE8F4";

            final LinearLayout row = createSettingRow(btnName, stateIcon);
            row.setTag(el.id);
            row.setFocusable(true);

            // 💡 [핵심 기술] 우측 아이콘 전용 텍스트뷰(TextView)의 폰트만 머티리얼 폰트로 싹 갈아입힙니다!
            final TextView tvRight = (TextView) row.getChildAt(1);
            if (materialIconFont != null) {
                tvRight.setTypeface(materialIconFont);
                tvRight.setTextSize(22f); // 아이콘이 예쁘게 보이도록 사이즈 살짝 업!
            }

            if (row.hasFocus()) {
                row.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                ((TextView) row.getChildAt(0)).setTextColor(ThemeManager.getListButtonFocusedTextColor());
                tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
            } else {
                row.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                ((TextView) row.getChildAt(0)).setTextColor(ThemeManager.getTextColorPrimary());
                if (isHidden)
                    tvRight.setTextColor(ThemeManager.getTextColorSecondary());
                else
                    tvRight.setTextColor(ThemeManager.getTextColorPrimary());
            }

            row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (isMenuReorderMode)
                        return;

                    boolean currHidden = prefs.getBoolean("hide_btn_" + el.id, false);
                    if (hasFocus) {
                        row.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                        ((TextView) row.getChildAt(0)).setTextColor(ThemeManager.getListButtonFocusedTextColor());
                        tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    } else {
                        row.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                        ((TextView) row.getChildAt(0)).setTextColor(ThemeManager.getTextColorPrimary());
                        if (currHidden)
                            tvRight.setTextColor(ThemeManager.getTextColorSecondary());
                        else
                            tvRight.setTextColor(ThemeManager.getTextColorPrimary());
                    }
                }
            });

            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    clickFeedback();
                    isLongPressConsumed = true;
                    if (!isMenuReorderMode)
                        toggleMenuReorderMode();
                    return true;
                }
            });

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isMenuReorderMode) {
                        clickFeedback();
                        toggleMenuReorderMode();
                        return;
                    }

                    clickFeedback();
                    boolean newState = !prefs.getBoolean("hide_btn_" + el.id, false);
                    prefs.edit().putBoolean("hide_btn_" + el.id, newState).commit();

                    // 💡 [수정] 토글 시 머티리얼 유니코드로 즉시 전환!
                    tvRight.setText(newState ? "\uE5CD" : "\uE8F4");

                    if (row.hasFocus()) {
                        tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    } else {
                        if (newState)
                            tvRight.setTextColor(ThemeManager.getTextColorSecondary());
                        else
                            tvRight.setTextColor(ThemeManager.getTextColorPrimary());
                    }
                }
            });

            containerSettingsItems.addView(row);
        }
        containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (containerSettingsItems.getChildCount() > 0) {
                    containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        }, 50);
    }

    // 🚀 [신규 엔진] 테마별 커스텀 순서를 금고에서 읽어와 적용하는 정렬 도우미
    private void sortMenuElements(List<ThemeManager.MenuElement> buttons) {
        String savedOrder = prefs.getString("custom_menu_order_" + ThemeManager.getCurrentTheme().name, "");
        if (!savedOrder.isEmpty()) {
            final List<String> orderList = java.util.Arrays.asList(savedOrder.split(","));
            java.util.Collections.sort(buttons, new java.util.Comparator<ThemeManager.MenuElement>() {
                @Override
                public int compare(ThemeManager.MenuElement e1, ThemeManager.MenuElement e2) {
                    int idx1 = orderList.indexOf(e1.id);
                    int idx2 = orderList.indexOf(e2.id);
                    if (idx1 != -1 && idx2 != -1)
                        return idx1 - idx2;
                    if (idx1 != -1)
                        return -1;
                    if (idx2 != -1)
                        return 1;
                    return e1.focusIndex - e2.focusIndex;
                }
            });
        } else {
            java.util.Collections.sort(buttons, new java.util.Comparator<ThemeManager.MenuElement>() {
                @Override
                public int compare(ThemeManager.MenuElement e1, ThemeManager.MenuElement e2) {
                    return e1.focusIndex - e2.focusIndex;
                }
            });
        }
    }

    // 🚀 [지능형 순서 변경 컨트롤러]
    private void toggleMenuReorderMode() {
        if (!isMenuReorderMode) {
            // 🔴 이동 모드 켜기 (집어 들기)
            View focused = getCurrentFocus();
            if (focused != null && focused.getParent() == containerSettingsItems) {
                isMenuReorderMode = true;
                currentReorderRow = focused;

                // 눈에 확 띄도록 강렬한 레드 계열로 색상 반전!
                currentReorderRow.setBackground(createButtonBackground(0xFFD32F2F));

                // 💡 [신규 추가] 빨간색으로 활성화된 순간에만 눈 아이콘 뒤에 " ↕"를 수술집도하듯 붙여줍니다!
                TextView tvRight = (TextView) ((LinearLayout) currentReorderRow).getChildAt(1);
                String currentIcon = tvRight.getText().toString().replace(" ↕", ""); // 방어 코드
                tvRight.setText(currentIcon + " ↕");
                tvRight.setTextColor(0xFFFFFFFF);
            }
        } else {
            // ⚪ 이동 모드 끄기 & 순서 영구 저장 (내려놓기)
            isMenuReorderMode = false;
            if (currentReorderRow != null) {
                currentReorderRow.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));

                // 💡 [신규 추가] 내려놓는 순간, 붙어있던 위아래 화살표(" ↕")를 깔끔하게 지워 원래대로 되돌립니다!
                TextView tvRight = (TextView) ((LinearLayout) currentReorderRow).getChildAt(1);
                String currentIcon = tvRight.getText().toString().replace(" ↕", "");
                tvRight.setText(currentIcon);
                tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());

                currentReorderRow = null;
            }

            // 변경된 순서를 수집해서 텍스트로 합친 뒤 금고에 박제!
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < containerSettingsItems.getChildCount(); i++) {
                View child = containerSettingsItems.getChildAt(i);
                if (child.getTag() != null && child.getTag() instanceof String) {
                    sb.append((String) child.getTag()).append(",");
                }
            }
            prefs.edit().putString("custom_menu_order_" + ThemeManager.getCurrentTheme().name, sb.toString()).commit();
            Toast.makeText(this, t("Menu order saved!"), Toast.LENGTH_SHORT).show();
        }
    }

    // 🚀 [신규 엔진] 저장된 라디오 채널들을 한눈에 보고 삭제/이동할 수 있는 전용 관리 스튜디오!
    private void buildRadioSavedChannelsUI() {
        currentSettingsDepth = 2; // 💡 깊이(Depth) 2로 설정하여 뒤로 가기 시 라디오 설정창으로 복귀하게 만듭니다.
        containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("SAVED CHANNELS") + " ━");

        if (savedRadioStations.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("   " + t("No saved channels."));
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setPadding(20, 10, 20, 10);
            containerSettingsItems.addView(tvEmpty);
        } else {
            for (int i = 0; i < savedRadioStations.size(); i++) {
                final Float freq = savedRadioStations.get(i);

                // CH 1: 89.1 MHz 형식으로 예쁘게 렌더링
                String label = "CH " + (i + 1) + " :  " + String.format(Locale.US, "%.1f MHz", freq);
                View btnFreq = createListButtonWithIcon("\uE03E", label); // 📻 라디오 유니코드 아이콘

                // 🚀 [숏클릭 액션] 짧게 누르면: 해당 주파수로 즉시 튜닝하고 라디오 메인 플레이어 화면으로 복귀!
                btnFreq.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickFeedback();
                        com.themoon.y1.managers.FmRadioManager fm = com.themoon.y1.managers.FmRadioManager
                                .getInstance(MainActivity.this);
                        if (fm.isPowerUp)
                            fm.tune(freq);
                        else
                            fm.currentFreq = freq;

                        isRadioSettingsMode = false; // 메인 플레이어로 상태 전환
                        isRadioAdjustingFreq = false;
                        buildRadioUI(); // 화면 갱신
                    }
                });

                // 🚀 [롱클릭 액션] 길게 누르면: 즉시 삭제 팝업 가동!
                btnFreq.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        clickFeedback();
                        isLongPressConsumed = true; // 손 뗄 때 클릭되는 안드로이드 고질병 방어막

                        new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                                .setTitle(t("Delete Channel"))
                                .setMessage(String.format(Locale.US,
                                        t("Remove %.1f MHz from saved channels?"), freq))
                                .setPositiveButton(t("Delete"), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        clickFeedback();
                                        savedRadioStations.remove(freq); // 리스트에서 삭제

                                        // 💡 금고(SharedPreferences) 동기화 업데이트
                                        StringBuilder sb = new StringBuilder();
                                        for (int j = 0; j < savedRadioStations.size(); j++) {
                                            sb.append(savedRadioStations.get(j));
                                            if (j < savedRadioStations.size() - 1)
                                                sb.append(",");
                                        }
                                        prefs.edit().putString("radio_stations", sb.toString()).commit();
                                        Toast.makeText(MainActivity.this, t("Channel deleted."), Toast.LENGTH_SHORT)
                                                .show();

                                        buildRadioSavedChannelsUI(); // 리스트 갱신 (지운 항목 화면에서 즉시 증발)
                                    }
                                })
                                .setNegativeButton(t("Cancel"), null)
                                .show();
                        return true;
                    }
                });
                containerSettingsItems.addView(btnFreq);
            }
        }

        // 🚀 화면 렌더링 직후 가장 첫 번째 채널에 자석처럼 휠 포커스 록온!
        containerSettingsItems.postDelayed(new Runnable() {
            public void run() {
                if (containerSettingsItems.getChildCount() > 1) { // 0번은 헤더 텍스트이므로 1번을 타겟팅
                    containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        }, 50);
    }

    // =======================================================
    // 🚀 [팟캐스트 전용] 실시간 다운로드 퍼센트(%) 추적 및 화면 유지 엔진
    // =======================================================
    private Handler podcastProgressHandler = new Handler();
    private Runnable podcastProgressTask = new Runnable() {
        @Override
        public void run() {
            if (activePodcastDownloads.isEmpty()) {
                // 💡 다운로드가 모두 끝나면 화면 꺼짐 방지를 해제하여 배터리를 아낍니다!
                if (!isCustomScanning && !isRadioScanning && !isServerRunning) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                return;
            }

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            boolean needsListUpdate = false;

            java.util.Iterator<java.util.Map.Entry<String, Long>> it = activePodcastDownloads.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<String, Long> entry = it.next();
                String url = entry.getKey();
                long downloadId = entry.getValue();

                DownloadManager.Query q = new DownloadManager.Query();
                q.setFilterById(downloadId);
                android.database.Cursor cursor = manager.query(q);

                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    // 다운로드 완료 또는 실패 시 레이더에서 삭제
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        it.remove();
                        podcastDownloadProgress.remove(url);
                        needsListUpdate = true;
                    } else {
                        // 🚀 진행 중일 때 전체 바이트와 현재 바이트를 나눠 퍼센트(%)를 구합니다!
                        int bytesDownloaded = cursor
                                .getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (bytesTotal > 0) {
                            int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            Integer oldProgress = podcastDownloadProgress.get(url);
                            if (oldProgress == null || oldProgress != progress) {
                                podcastDownloadProgress.put(url, progress);
                                needsListUpdate = true;
                            }
                        }
                    }
                    cursor.close();
                } else {
                    it.remove();
                    podcastDownloadProgress.remove(url);
                    needsListUpdate = true;
                    if (cursor != null)
                        cursor.close();
                }
            }

            // 퍼센트가 1%라도 오르면 팟캐스트 리스트 화면을 즉시 새로고침!
            if (needsListUpdate && currentBrowserMode == 14 && listVirtualSongs != null
                    && listVirtualSongs.getAdapter() != null) {
                ((android.widget.BaseAdapter) listVirtualSongs.getAdapter()).notifyDataSetChanged();
            }

            if (!activePodcastDownloads.isEmpty()) {
                podcastProgressHandler.postDelayed(this, 1000); // 1초마다 반복 검사
            } else {
                // 다운로드 끝! 화면 잠금 해제
                if (!isCustomScanning && !isRadioScanning && !isServerRunning) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        }
    };

    // =======================================================
    // 🚀 [이퀄라이저 엔진] 10밴드 전환 전용 머티리얼 경고 팝업!
    // =======================================================
    public void showSoftwareEqMaterialDialog(final LinearLayout btnSoftwareEq) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        float d = getResources().getDisplayMetrics().density;

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0xFF000000);
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (20 * d), (int) (25 * d), (int) (20 * d), (int) (20 * d));

        // 1. 타이틀 (오렌지색 경고)
        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚠️ 10-Band Software EQ");
        tvTitle.setTextColor(0xFFFF8800);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (15 * d));
        rootLayout.addView(tvTitle);

        // 2. 설명 텍스트
        TextView tvDesc = new TextView(this);
        tvDesc.setText(t(
                "This engine provides precise 10-band tuning, but uses more CPU.\n\nOld devices may experience audio stuttering. Proceed?"));
        tvDesc.setTextColor(ThemeManager.getTextColorPrimary());
        tvDesc.setTextSize(15f);
        tvDesc.setLineSpacing(12f, 1.1f);
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 0, 0, (int) (25 * d));
        rootLayout.addView(tvDesc);

        // 3. 버튼 배치를 위한 가로 레이아웃
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        // ❌ 취소 버튼
        final View btnCancel = createListButtonWithIcon("\uE14C", t("Cancel"));
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cancelLp.rightMargin = (int) (4 * d);
        btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
        });

        // 🟢 승인(ON) 버튼
        final View btnConfirm = createListButtonWithIcon("\uE876", t("Turn ON"), 0xFF00FF00);
        LinearLayout.LayoutParams confirmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        confirmLp.leftMargin = (int) (4 * d);
        btnConfirm.setLayoutParams(confirmLp);
        btnConfirm.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();

            // 🚀 [기존 로직] 스위치 켜기
            isSoftwareEqEnabled = true;
            prefs.edit().putBoolean("software_eq_enabled", true).commit();

            // =======================================================
            // 🛡️ [추가된 방어막!] 10밴드로 넘어가면, 들고 있던 5밴드 프로필을 뺏고 10밴드 기본 프로필 장착!
            // =======================================================
            currentEqProfile = "custom_Default_10B";

            String listStr = prefs.getString("custom_eq_list", "");
            if (!listStr.contains("Default_10B")) {
                prefs.edit().putString("custom_eq_list", "Default_10B," + listStr).commit();
            }
            prefs.edit().putString("eq_profile_id", currentEqProfile).commit();

            // 🚀 오디오 엔진 재부팅
            rebootAudioEngine();

            Toast.makeText(MainActivity.this, t("10-Band EQ Engine Activated!"), Toast.LENGTH_SHORT).show();
            buildEqualizerSettingsUI();
        });

        // 🚀 좌우 휠 조향 장치 (21: 왼쪽/취소, 22: 오른쪽/승인)
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21) {
                        btnCancel.requestFocus();
                        clickFeedback();
                        return true;
                    }
                    if (keyCode == 22) {
                        btnConfirm.requestFocus();
                        clickFeedback();
                        return true;
                    }
                }
                return false;
            }
        };
        btnCancel.setOnKeyListener(dialogWheelListener);
        btnConfirm.setOnKeyListener(dialogWheelListener);

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnConfirm);
        rootLayout.addView(btnLayout);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (350 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        // 팝업이 뜨면 안전하게 취소 버튼에 포커스!
        rootLayout.postDelayed(() -> btnCancel.requestFocus(), 50);
    }

    // =======================================================
    // 🚀 [오디오 배관 재조립 엔진] EQ 모드 변경 시 소리 먹통/폭주 완벽 방지
    // =======================================================
    public void rebootAudioEngine() {
        com.themoon.y1.managers.AudioPlayerManager am = com.themoon.y1.managers.AudioPlayerManager.getInstance();
        if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
            boolean wasPlaying = am.isPlaying();
            int currentPos = am.getCurrentPosition();

            am.releasePlayer(); // 💥 기존에 꼬여버린 낡은 배관을 완전히 박살냅니다!

            am.prepareMusicTrack(currentIndex); // 🚀 새 규격(5밴드/10밴드)에 맞춰 배관 100% 깨끗하게 재조립!
            if (currentPos > 0)
                am.seekRelative(currentPos); // 듣던 위치로 복귀
            if (wasPlaying)
                am.playOrPauseMusic(); // 재생 중이었다면 다시 재생!
        }
    }

    // =======================================================
    // 🚀 [애플 팟캐스트 검색 엔진] 리미트 200개 해제 & 썸네일 아트 추출!
    // =======================================================
    private void searchPodcastFromApple(final String keyword) {
        showLoadingPopup();

        new Thread(() -> {
            try {
                String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
                // 🚀 [리미트 해제] 200개까지 검색 결과를 미친 듯이 긁어옵니다!
                String urlString = "https://itunes.apple.com/search?term=" + encodedKeyword
                        + "&entity=podcast&limit=200";

                okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[] {};
                            }

                            public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                    String authType) {
                            }

                            public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                    String authType) {
                            }
                        }
                };
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS", "Conscrypt");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                builder.sslSocketFactory(sslContext.getSocketFactory(),
                        (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
                builder.hostnameVerifier((hostname, session) -> true);

                okhttp3.OkHttpClient client = builder.build();
                okhttp3.Request request = new okhttp3.Request.Builder().url(urlString).build();
                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful())
                    throw new java.io.IOException("Apple API Error");

                String jsonResponse = response.body().string();
                org.json.JSONObject root = new org.json.JSONObject(jsonResponse);
                org.json.JSONArray results = root.getJSONArray("results");

                final List<String[]> searchResults = new ArrayList<>();

                for (int i = 0; i < results.length(); i++) {
                    org.json.JSONObject item = results.getJSONObject(i);
                    if (item.has("feedUrl") && item.has("collectionName")) {
                        String name = item.getString("collectionName");
                        String author = item.has("artistName") ? item.getString("artistName") : "Unknown";
                        String rss = item.getString("feedUrl");

                        // 🚀 애플 서버가 제공하는 100x100 픽셀 썸네일 주소 획득!
                        String artUrl = item.has("artworkUrl100") ? item.getString("artworkUrl100") : "";

                        searchResults.add(new String[] { name, author, rss, artUrl });
                    }
                }

                runOnUiThread(() -> {
                    if (layoutLoadingOverlay != null)
                        layoutLoadingOverlay.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    if (searchResults.isEmpty()) {
                        Toast.makeText(MainActivity.this, t("No podcasts found for: ") + keyword, Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        // 대망의 무한 스크롤 팝업창 호출
                        showPodcastSearchResultsDialog(searchResults);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (layoutLoadingOverlay != null)
                        layoutLoadingOverlay.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    Toast.makeText(MainActivity.this, "Search Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // =======================================================
    // 🚀 [애플 팟캐스트 검색 결과 창] 썸네일 보안 우회 + ListView 폴링 엔진!
    // =======================================================
    private void showPodcastSearchResultsDialog(final List<String[]> results) {
        final float d = getResources().getDisplayMetrics().density;

        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0xFF000000);
        bg.setCornerRadius(15 * d);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (10 * d), (int) (20 * d), (int) (10 * d), (int) (10 * d));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🔍 " + t("Search Results") + " (" + results.size() + ")");
        tvTitle.setTextColor(0xFF00FFFF);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (15 * d));
        rootLayout.addView(tvTitle);

        // =======================================================
        // 🚀 [핵심 추가] 이미지 전용 '무적의 우회 통신망' 1개 개통! (메모리 절약)
        // =======================================================
        okhttp3.OkHttpClient tempClient = null;
        try {
            okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[] {};
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS", "Conscrypt");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            tempClient = builder.build();
        } catch (Exception e) {
        }

        final okhttp3.OkHttpClient imageClient = tempClient;

        // 🚀 ListView 조립
        final ListView listView = new ListView(this);
        listView.setDivider(new ColorDrawable(0x00000000));
        listView.setDividerHeight((int) (4 * d));
        listView.setSelector(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        listView.setScrollbarFadingEnabled(false);
        listView.setItemsCanFocus(true);

        android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
            @Override
            public int getCount() {
                return results.size();
            }

            @Override
            public Object getItem(int position) {
                return results.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                LinearLayout itemBtn;
                ImageView ivThumb;
                TextView t1, t2;

                if (convertView == null) {
                    itemBtn = new LinearLayout(MainActivity.this);
                    itemBtn.setOrientation(LinearLayout.HORIZONTAL);
                    itemBtn.setGravity(Gravity.CENTER_VERTICAL);
                    itemBtn.setFocusable(true);
                    itemBtn.setClickable(true);
                    itemBtn.setSoundEffectsEnabled(false);
                    itemBtn.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    itemBtn.setPadding((int) (10 * d), (int) (10 * d), (int) (10 * d), (int) (10 * d));

                    android.widget.AbsListView.LayoutParams btnLp = new android.widget.AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    itemBtn.setLayoutParams(btnLp);

                    ivThumb = new ImageView(MainActivity.this);
                    LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams((int) (55 * d), (int) (55 * d));
                    thumbLp.rightMargin = (int) (15 * d);
                    ivThumb.setLayoutParams(thumbLp);
                    ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivThumb.setBackgroundColor(0x22FFFFFF);

                    LinearLayout textContainer = new LinearLayout(MainActivity.this);
                    textContainer.setOrientation(LinearLayout.VERTICAL);
                    textContainer.setLayoutParams(
                            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                    t1 = new TextView(MainActivity.this);
                    t1.setTextColor(ThemeManager.getTextColorPrimary());
                    t1.setTextSize(16f);
                    t1.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
                    t1.setSingleLine(true);
                    t1.setEllipsize(android.text.TextUtils.TruncateAt.END);

                    t2 = new TextView(MainActivity.this);
                    t2.setTextColor(ThemeManager.getTextColorSecondary());
                    t2.setTextSize(13f);
                    t2.setTypeface(ThemeManager.getCustomFont(), Typeface.NORMAL);
                    t2.setSingleLine(true);
                    t2.setEllipsize(android.text.TextUtils.TruncateAt.END);

                    textContainer.addView(t1);
                    textContainer.addView(t2);

                    itemBtn.addView(ivThumb);
                    itemBtn.addView(textContainer);

                    itemBtn.setTag(new Object[] { ivThumb, t1, t2 });
                } else {
                    itemBtn = (LinearLayout) convertView;
                    Object[] holder = (Object[]) itemBtn.getTag();
                    ivThumb = (ImageView) holder[0];
                    t1 = (TextView) holder[1];
                    t2 = (TextView) holder[2];
                }

                final String[] selected = results.get(position);
                final String channelName = selected[0];
                final String author = selected[1];
                final String rssUrl = selected[2];
                final String artUrl = selected[3];

                t1.setText(channelName);
                t2.setText(author);

                ivThumb.setImageBitmap(null); // 이전 껍데기 잔상 초기화
                ivThumb.setTag(artUrl); // 목표 주소 각인

                // =======================================================
                // 🚀 [이미지 다운로드] 무적 통신망(OkHttp)으로 애플 보안 서버 박살내기!
                // =======================================================
                if (!artUrl.isEmpty() && imageClient != null) {
                    new Thread(() -> {
                        try {
                            okhttp3.Request request = new okhttp3.Request.Builder().url(artUrl).build();
                            okhttp3.Response response = imageClient.newCall(request).execute();

                            if (response.isSuccessful() && response.body() != null) {
                                java.io.InputStream is = response.body().byteStream();
                                final Bitmap bmp = BitmapFactory.decodeStream(is);
                                is.close();

                                runOnUiThread(() -> {
                                    // 휠을 돌려 뷰가 재활용되었어도 원래 주소와 일치할 때만 그림 그리기!
                                    if (artUrl.equals(ivThumb.getTag())) {
                                        ivThumb.setImageBitmap(bmp);
                                    }
                                });
                            }
                        } catch (Exception e) {
                        }
                    }).start();
                }

                // 포커스 처리
                final TextView finalT1 = t1;
                final TextView finalT2 = t2;
                itemBtn.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        v.setBackground(createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                        finalT1.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                        finalT2.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    } else {
                        v.setBackground(createButtonBackground(ThemeManager.getListButtonNormalBg()));
                        finalT1.setTextColor(ThemeManager.getTextColorPrimary());
                        finalT2.setTextColor(ThemeManager.getTextColorSecondary());
                    }
                });

                // 특수 휠 우회 엔진
                itemBtn.setOnKeyListener((v, keyCode, event) -> {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == 21 || keyCode == 19) {
                            int targetPos = position - 1;
                            if (targetPos >= 0) {
                                listView.setSelectionFromTop(targetPos, (int) (10 * d));
                                listView.post(() -> {
                                    int visibleIdx = targetPos - listView.getFirstVisiblePosition();
                                    if (visibleIdx >= 0 && visibleIdx < listView.getChildCount()) {
                                        listView.getChildAt(visibleIdx).requestFocus();
                                    }
                                });
                                clickFeedback();
                            }
                            return true;
                        }
                        if (keyCode == 22 || keyCode == 20) {
                            int targetPos = position + 1;
                            if (targetPos < results.size()) {
                                listView.setSelectionFromTop(targetPos, (int) (10 * d));
                                listView.post(() -> {
                                    int visibleIdx = targetPos - listView.getFirstVisiblePosition();
                                    if (visibleIdx >= 0 && visibleIdx < listView.getChildCount()) {
                                        listView.getChildAt(visibleIdx).requestFocus();
                                    }
                                });
                                clickFeedback();
                            }
                            return true;
                        }
                    }
                    return false;
                });

                // 클릭 (구독)
                itemBtn.setOnClickListener(v -> {
                    clickFeedback();
                    try {
                        File podcastDir = StoragePaths.getPodcastsDir();
                        if (!podcastDir.exists())
                            podcastDir.mkdirs();
                        File subFile = new File(podcastDir, "subscriptions.txt");

                        FileOutputStream fos = new FileOutputStream(subFile, true);
                        java.io.BufferedWriter bw = new java.io.BufferedWriter(
                                new java.io.OutputStreamWriter(fos, "UTF-8"));

                        String safeName = channelName.replace("|", "-");
                        bw.write("\n" + safeName + "|" + rssUrl);
                        bw.close();

                        Toast.makeText(MainActivity.this, "✅ " + t("Subscribed: ") + safeName, Toast.LENGTH_SHORT)
                                .show();
                        dialog.dismiss();

                        buildPodcastChannelsUI();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                return itemBtn;
            }
        };
        listView.setAdapter(adapter);

        LinearLayout listWrapper = new LinearLayout(this);
        listWrapper.setPadding(0, 0, 0, 0);
        listWrapper.addView(listView,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (320 * d)));

        rootLayout.addView(listWrapper);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (340 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        listView.postDelayed(() -> {
            if (listView.getChildCount() > 0) {
                listView.getChildAt(0).requestFocus();
            }
        }, 100);
    }

    // =======================================================
    // 🚀 [팟캐스트 삭제 엔진] 구독 해지와 동시에 다운로드된 파일까지 싹 다 날립니다!
    // =======================================================
    private void removePodcastSubscription(String channelName, String targetRssUrl) {
        try {
            File podcastDir = StoragePaths.getPodcastsDir();
            File subFile = new File(podcastDir, "subscriptions.txt");
            if (!subFile.exists())
                return;

            // 1. 메모장(subscriptions.txt)에서 해당 채널 줄 지우기
            List<String> lines = new ArrayList<>();
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(subFile), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    lines.add(line);
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 2 && parts[1].trim().equals(targetRssUrl.trim()))
                    continue;
                lines.add(line);
            }
            br.close();

            java.io.BufferedWriter bw = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new FileOutputStream(subFile), "UTF-8"));
            for (String l : lines)
                bw.write(l + "\n");
            bw.close();

            // 🚀 2. [신규 장착] 폴더 및 내부 파일 (음원, cover.jpg) 싹 다 폭파!
            String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File channelFolder = new File(podcastDir, safeChannel);
            if (channelFolder.exists()) {
                File[] files = channelFolder.listFiles();
                if (files != null) {
                    for (File f : files)
                        f.delete(); // 폴더 안의 파일들 모두 삭제
                }
                channelFolder.delete(); // 텅 빈 폴더 최종 삭제
            }

            Toast.makeText(this, "🗑️ " + t("Channel deleted."), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =======================================================
    // 🚀 [팟캐스트 삭제 머티리얼 팝업] 휠 포커스 완벽 지원!
    // =======================================================
    private void showDeletePodcastMaterialDialog(final String channelName, final String rssUrl) {
        float d = getResources().getDisplayMetrics().density;
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0xFF000000); // 메인 테마색 바탕
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF); // 은은한 테두리
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (15 * d), (int) (20 * d), (int) (15 * d), (int) (15 * d));

        // 1. 제목 텍스트 (빨간색 강조)
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🗑️ " + t("Delete Channel"));
        tvTitle.setTextColor(0xFFFF5555);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (10 * d));
        rootLayout.addView(tvTitle);

        // 2. 메시지 텍스트
        TextView tvMsg = new TextView(this);
        tvMsg.setText(t("Do you want to delete '") + channelName + "'?");
        tvMsg.setTextColor(ThemeManager.getTextColorPrimary());
        tvMsg.setTextSize(16f);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, 0, 0, (int) (20 * d));
        tvMsg.setLineSpacing(5f, 1.2f);
        rootLayout.addView(tvMsg);

        // 3. 휠 조향 센서 (위아래 완벽 이동)
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21 || keyCode == 19) {
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx - 1; i >= 0; i--) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                    if (keyCode == 22 || keyCode == 20) {
                        int idx = rootLayout.indexOfChild(v);
                        for (int i = idx + 1; i < rootLayout.getChildCount(); i++) {
                            if (rootLayout.getChildAt(i).isFocusable()) {
                                rootLayout.getChildAt(i).requestFocus();
                                clickFeedback();
                                return true;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        // 4. [삭제] 버튼
        View btnDelete = createListButtonWithIcon("\uE872", t("Delete"), 0xFFFF5555);
        btnDelete.setOnKeyListener(dialogWheelListener);
        btnDelete.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
            removePodcastSubscription(channelName, rssUrl);
            buildPodcastManageUI(); // 화면 새로고침
        });
        rootLayout.addView(btnDelete);

        // 5. [취소] 버튼
        View btnCancel = createListButtonWithIcon("\uE14C", t("Cancel"), ThemeManager.getTextColorSecondary());
        btnCancel.setOnKeyListener(dialogWheelListener);
        btnCancel.setOnClickListener(v -> {
            clickFeedback();
            dialog.dismiss();
        });
        rootLayout.addView(btnCancel);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout((int) (320 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();

        // 🚀 팝업이 열리면 안전하게 '취소' 버튼에 먼저 포커스! (실수 방지)
        rootLayout.postDelayed(() -> btnCancel.requestFocus(), 50);
    }

    // =======================================================
    // 🚀 [팟캐스트 구독 관리 화면] 휠로 돌려서 삭제할 채널을 고르는 스튜디오!
    // =======================================================
    private void buildPodcastManageUI() {
        currentBrowserMode = BROWSER_PODCAST_MANAGE; // 🚀 [핵심] 나 지금 구독 관리 화면에 있다고 시스템에 신고!
        if (scrollViewBrowser != null)
            scrollViewBrowser.setVisibility(View.VISIBLE);
        if (listVirtualSongs != null)
            listVirtualSongs.setVisibility(View.GONE);
        containerBrowserItems.removeAllViews();
        tvBrowserPath.setText(t("Podcasts") + ": " + t("Manage Subscriptions"));

        // 🚀 1. 뒤로 가기 버튼 (가장 위에 배치)
        View btnBack = createListButtonWithIcon("\uE5C4", t("Back to Podcasts")); // 뒤로가기 화살표 아이콘
        btnBack.setOnClickListener(v -> {
            clickFeedback();
            buildPodcastChannelsUI(); // 원래의 팟캐스트 메인 화면으로 복귀!
        });
        containerBrowserItems.addView(btnBack);

        // 메모장에서 채널 목록 읽어오기
        File podcastDir = StoragePaths.getPodcastsDir();
        File subFile = new File(podcastDir, "subscriptions.txt");
        List<String[]> channels = new ArrayList<>();

        if (subFile.exists()) {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(subFile), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#"))
                        continue;
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        channels.add(new String[] { parts[0].trim(), parts[1].trim() });
                    }
                }
                br.close();
            } catch (Exception e) {
            }
        }

        // 🚀 2. 삭제할 채널 목록 그리기
        if (channels.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("⚠️ " + t("No subscriptions found."));
            tvEmpty.setTextColor(0xFF888888);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(20, 100, 20, 50);
            containerBrowserItems.addView(tvEmpty);
        } else {
            for (final String[] channel : channels) {
                // 💡 빨간색(0xFFFF5555) 휴지통 아이콘으로 '삭제용 버튼'임을 명확히 보여줍니다!
                View btnDel = createListButtonWithIcon("\uE872", channel[0], 0xFFFF5555);

                // 기존의 new AlertDialog.Builder(...) 부분을 통째로 지우고 이 한 줄로 교체하세요!
                btnDel.setOnClickListener(v -> {
                    clickFeedback();
                    showDeletePodcastMaterialDialog(channel[0], channel[1]); // 🚀 머티리얼 팝업 호출
                });
                containerBrowserItems.addView(btnDel);
            }
        }

        // 🚀 3. 화면이 열리면 첫 번째 채널(인덱스 1번)에 자동으로 휠 포커스 고정!
        if (containerBrowserItems.getChildCount() > 1) {
            containerBrowserItems.postDelayed(() -> {
                containerBrowserItems.getChildAt(1).requestFocus();
            }, 50);
        }
    }

    // =======================================================
    // 🚀 4번 기능: 초고속 비디오 썸네일 캐시 렌더러 (렉 방지 큐잉 엔진 탑재!)
    // =======================================================
    public void loadVideoThumbnailAsync(final String path, final ImageView iv) {
        iv.setTag(path); // 💡 꼬임 방지: 이미지 뷰에 현재 비디오 경로 이름표를 단단히 붙여둠

        Bitmap cached = albumArtCache.get("vid_" + path);
        if (cached != null) {
            iv.setImageBitmap(cached);
            return;
        }

        // 🚀 핵심: 화면 로딩 중일 때는 시스템 기본 비디오 아이콘(회색)을 0.01초 만에 먼저 띄워놓고 대기!
        iv.setImageResource(android.R.drawable.presence_video_online);

        if (thumbnailExecutor == null) {
            thumbnailExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        }

        // 💡 수십 개의 스레드가 터지지 않도록 전담 일꾼 1명이 대기표를 뽑고 순서대로 작업합니다!
        thumbnailExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bmp = albumArtCache.get("vid_" + path);
                    if (bmp == null) {
                        bmp = android.media.ThumbnailUtils.createVideoThumbnail(path,
                                android.provider.MediaStore.Video.Thumbnails.MINI_KIND);
                        if (bmp != null) {
                            albumArtCache.put("vid_" + path, bmp); // 완성된 사진 금고에 저장
                        }
                    }

                    final Bitmap finalBmp = bmp;
                    if (finalBmp != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 💡 다른 화면으로 넘어가거나 뷰가 꼬이지 않도록 이름표가 일치할 때만 쏙! 끼워 넣기
                                if (path.equals(iv.getTag())) {
                                    iv.setImageBitmap(finalBmp);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    // =======================================================
    // 🚀 [신규 엔진] 팟캐스트 오프라인 전체 리스트 캐시 저장 및 불러오기
    // =======================================================
    private void savePodcastCache(String channelName, List<SongItem> episodes) {
        try {
            String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File podcastDir = new File("/storage/sdcard0/Podcasts/" + safeChannel);
            if (!podcastDir.exists()) podcastDir.mkdirs();
            File cacheFile = new File(podcastDir, "episode_cache.txt");
            java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(cacheFile), "UTF-8"));
            for (SongItem ep : episodes) {
                // 제목, 앨범아트(album), 날짜(year), 오디오주소(genre) 순으로 저장
                bw.write(ep.title + "|_|" + ep.album + "|_|" + ep.year + "|_|" + ep.genre + "\n");
            }
            bw.close();
        } catch (Exception e) {}
    }

    private List<SongItem> loadPodcastCache(String channelName) {
        List<SongItem> cached = new ArrayList<>();
        try {
            String safeChannel = channelName.replaceAll("[\\\\/:*?\"<>|]", "_");
            File cacheFile = new File("/storage/sdcard0/Podcasts/" + safeChannel, "episode_cache.txt");
            if (cacheFile.exists()) {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(cacheFile), "UTF-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|_\\|");
                    if (parts.length >= 4) {
                        cached.add(new SongItem(new File("/PODCAST"), parts[0], channelName, parts[1], parts[2], parts[3]));
                    }
                }
                br.close();
            }
        } catch (Exception e) {}
        return cached;
    }

    // =======================================================
    // 🚀 [스크린 필터 엔진] 화면 전체 색상 및 채도 실시간 렌더링
    // =======================================================
    public void applyScreenFilter() {
        try {
            boolean enabled = prefs.getBoolean("filter_enabled", false);
            View rootView = getWindow().getDecorView().getRootView();

            // 필터가 꺼져 있으면 투명하게 원상복구!
            if (!enabled) {
                rootView.setLayerType(View.LAYER_TYPE_NONE, null);
                return;
            }

            // 저장된 슬라이더 값 불러오기 (채도는 0~100, RGB는 0~255)
            int satProgress = prefs.getInt("filter_saturation", 0);
            int r = prefs.getInt("filter_r", 136); // 기본값 녹색톤(136)
            int g = prefs.getInt("filter_g", 204); // 기본값 녹색톤(204)
            int b = prefs.getInt("filter_b", 136); // 기본값 녹색톤(136)

            android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
            matrix.setSaturation(satProgress / 100f); // 0이면 흑백, 1이면 원본 채도

            android.graphics.ColorMatrix tintMatrix = new android.graphics.ColorMatrix();
            tintMatrix.setScale(r / 255f, g / 255f, b / 255f, 1.0f); // RGB 색상 필터 덧씌우기
            matrix.postConcat(tintMatrix);

            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
            rootView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } catch (Exception e) {}
    }

}
