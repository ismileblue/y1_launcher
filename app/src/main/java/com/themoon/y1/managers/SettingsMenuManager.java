package com.themoon.y1.managers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import com.themoon.y1.MainActivity;
import com.themoon.y1.R;
import com.themoon.y1.ThemeManager;
import com.themoon.y1.managers.AudioPlayerManager;
import java.io.File;

public class SettingsMenuManager {
    private static SettingsMenuManager instance;
    private MainActivity main;
    public int lastSettingsFocusIndex = 0;
    public int lastSubMenuFocusIndex = 0;

    private SettingsMenuManager(MainActivity main) {
        this.main = main;
    }

    public static SettingsMenuManager getInstance(MainActivity main) {
        if (instance == null) {
            instance = new SettingsMenuManager(main);
        } else {
            // 🚀 [유령 화면 버그 완벽 수리!]
            // 앱이 새로고침(recreate)되어 새로운 화면이 전달되면,
            // 죽은 화면을 버리고 새 화면으로 조종석(main)을 즉시 갈아끼워줍니다!
            instance.main = main;
        }
        return instance;
    }

    private String t(String text) {
        return main.t(text);
    }

    private void clickFeedback() {
        main.clickFeedback();
    }

    private void clearSettingsUI() {
        main.containerSettingsItems.removeAllViews();
        // Remove any fixed UI injected before the ScrollView
        ViewGroup layoutSettingsMode = (ViewGroup) main.layoutSettingsMode;
        for (int i = layoutSettingsMode.getChildCount() - 1; i >= 0; i--) {
            View child = layoutSettingsMode.getChildAt(i);
            if ("battery_fixed".equals(child.getTag())) {
                layoutSettingsMode.removeViewAt(i);
            }
        }
    }

    public void buildSettingsUI() {
        main.currentSettingsDepth = 0;
        main.isRadioUIShowing = false;
        main.isRadioSettingsMode = false;

        updateSettingsTitle(null);

        clearSettingsUI();

        // 🚀 스크롤을 최상단으로 리셋 (카테고리 목록은 짧으므로)
        android.view.ViewParent parent = main.containerSettingsItems.getParent();
        if (parent instanceof android.widget.ScrollView) {
            ((android.widget.ScrollView) parent).scrollTo(0, 0);
        }

        main.containerSettingsItems.addView(createCategoryButton(t("Audio & Playback"), 0, new Runnable() {
            public void run() { buildAudioSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Display & Menu"), 1, new Runnable() {
            public void run() { buildDisplaySettingsUI(); }
        }));
        // 🚀 [수정] 테마가 세 번째 자리이므로 번호표를 '2'로 줍니다!
        main.containerSettingsItems.addView(createCategoryButton(t("Theme"), 2, new Runnable() {
            public void run() { main.buildThemeSelectorUI(); }
        }));
        // 🚀 [수정] 그 아래부터는 하나씩 밀려서 3, 4, 5, 6이 됩니다!
        main.containerSettingsItems.addView(createCategoryButton(t("Control & Feedback"), 3, new Runnable() {
            public void run() { buildControlSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Network & Connections"), 4, new Runnable() {
            public void run() { buildNetworkSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Data & Storage"), 5, new Runnable() {
            public void run() { buildDataSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("System"), 6, new Runnable() {
            public void run() { buildSystemSettingsUI(); }
        }));

        main.containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (lastSettingsFocusIndex >= 0 && main.containerSettingsItems.getChildCount() > lastSettingsFocusIndex) {
                    main.containerSettingsItems.getChildAt(lastSettingsFocusIndex).requestFocus();
                } else if (main.containerSettingsItems.getChildCount() > 0) {
                    main.containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        }, 50);
    }

    public void buildAudioSettingsUI() {
        main.currentSettingsDepth = 1;
        clearSettingsUI();
        updateSettingsTitle(t("Audio & Playback"));

        final LinearLayout btnShuffle = createSettingRow(t("Shuffle"), main.isShuffleMode ? t("ON") : t("OFF"));
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.isShuffleMode = !main.isShuffleMode;
                TextView tvStatus = (TextView) btnShuffle.getChildAt(1);
                tvStatus.setText(main.isShuffleMode ? t("ON") : t("OFF"));
                main.updatePlayerStatusIndicators();
                try {
                    main.prefs.edit().putBoolean("shuffle", main.isShuffleMode).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnShuffle);

        final LinearLayout btnRepeat = createSettingRow(t("Repeat"), t(main.getRepeatModeText(main.repeatMode)));
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.repeatMode = (main.repeatMode + 1) % 3;
                TextView tvStatus = (TextView) btnRepeat.getChildAt(1);
                tvStatus.setText(t(main.getRepeatModeText(main.repeatMode)));
                main.updatePlayerStatusIndicators();
                try {
                    main.prefs.edit().putInt("repeat", main.repeatMode).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnRepeat);

        String eqDisplayName = "";
        if (main.currentEqProfile.startsWith("preset_")) {
            try {
                int presetIdx = Integer.parseInt(main.currentEqProfile.replace("preset_", ""));
                if (main.eqPresetNames != null && presetIdx < main.eqPresetNames.size()) {
                    eqDisplayName = t(main.eqPresetNames.get(presetIdx));
                }
            } catch (Exception e) {}
        } else {
            eqDisplayName = main.currentEqProfile.replace("custom_", "");
        }
        final LinearLayout btnEq = createSettingRow(t("Equalizer & Audio Effects"), eqDisplayName + " 〉");
        btnEq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildEqualizerSettingsUI();
            }
        });
        main.containerSettingsItems.addView(btnEq);

        final String[] speedLabels = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"};
        final float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        AudioPlayerManager am = AudioPlayerManager.getInstance();
        float currentSpeed = am.getCurrentSpeed();
        int spdIdx = 2;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - currentSpeed) < 0.01f) spdIdx = i;
        }
        final LinearLayout btnSpeed = createSettingRow(t("Playback Speed"), t(speedLabels[spdIdx]));
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                float currentSpeed = AudioPlayerManager.getInstance().getCurrentSpeed();
                int idx = 2;
                for (int i = 0; i < speedValues.length; i++) {
                    if (Math.abs(speedValues[i] - currentSpeed) < 0.01f) idx = i;
                }
                int nextIdx = (idx + 1) % speedValues.length;
                AudioPlayerManager.getInstance().setPlaybackSpeed(speedValues[nextIdx]);
                TextView tvStatus = (TextView) btnSpeed.getChildAt(1);
                tvStatus.setText(t(speedLabels[nextIdx]));
                Toast.makeText(main, t("Speed set to ") + t(speedLabels[nextIdx]), Toast.LENGTH_SHORT).show();
            }
        });
        main.containerSettingsItems.addView(btnSpeed);
        focusFirstItem();
    }

    public void buildDisplaySettingsUI() {
        main.currentSettingsDepth = 1;
        clearSettingsUI();
        updateSettingsTitle(t("Display & Menu"));



        LinearLayout btnBgMenu = createSettingRow(t("Background"), "〉 ");
        btnBgMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildBackgroundSettingsUI();
            }
        });
        main.containerSettingsItems.addView(btnBgMenu);

        final LinearLayout btnMenuVisibility = createSettingRow(t("Main Menu Items"), t("Edit") + " 〉");
        btnMenuVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildMainMenuVisibilitySettingsUI();
            }
        });
        main.containerSettingsItems.addView(btnMenuVisibility);

        LinearLayout btnBrightMenu = createSettingRow(t("Display Brightness"), "〉 ");
        btnBrightMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeScreen(8); // STATE_BRIGHTNESS
                clickFeedback();
            }
        });
        main.containerSettingsItems.addView(btnBrightMenu);
// 🚀 [신규 장착] 스크린 필터 메뉴
        LinearLayout btnFilterMenu = createSettingRow(t("Screen Filter"), "〉 ");
        btnFilterMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildScreenFilterSettingsUI(); // 전용 스튜디오 오픈!
            }
        });
        main.containerSettingsItems.addView(btnFilterMenu);

        // (기존 코드) LinearLayout btnBrightMenu = createSettingRow(t("Display Brightness"), "〉 ");
        final LinearLayout btnBatteryIndicator = createSettingRow(t("Battery Indicator"), t(main.BATTERY_STYLE_NAMES[main.currentBatteryStyleIndex]));
        btnBatteryIndicator.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                clickFeedback();
                main.currentBatteryStyleIndex = (main.currentBatteryStyleIndex + 1) % main.BATTERY_STYLE_NAMES.length;
                android.widget.TextView tvStatus = (android.widget.TextView) btnBatteryIndicator.getChildAt(1);
                tvStatus.setText(t(main.BATTERY_STYLE_NAMES[main.currentBatteryStyleIndex]));
                try {
                    main.prefs.edit().putInt("battery_indicator_style", main.currentBatteryStyleIndex).commit();
                    
                    if (main.currentBatteryStyleIndex == 0) { // Icon Only
                        if (main.batteryIconView != null) main.batteryIconView.setVisibility(android.view.View.VISIBLE);
                        main.findViewById(com.themoon.y1.R.id.tv_status_battery).setVisibility(android.view.View.GONE);
                    } else if (main.currentBatteryStyleIndex == 1) { // Percent Only
                        if (main.batteryIconView != null) main.batteryIconView.setVisibility(android.view.View.GONE);
                        main.findViewById(com.themoon.y1.R.id.tv_status_battery).setVisibility(android.view.View.VISIBLE);
                    } else { // Icon + Percent
                        if (main.batteryIconView != null) main.batteryIconView.setVisibility(android.view.View.VISIBLE);
                        main.findViewById(com.themoon.y1.R.id.tv_status_battery).setVisibility(android.view.View.VISIBLE);
                    }
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnBatteryIndicator);

        final LinearLayout btnTimeout = createSettingRow(t("Screen Timeout"), t(main.TIMEOUT_NAMES[main.currentTimeoutIndex]));
        btnTimeout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.currentTimeoutIndex = (main.currentTimeoutIndex + 1) % main.TIMEOUT_VALUES.length;
                TextView tvStatus = (TextView) btnTimeout.getChildAt(1);
                tvStatus.setText(t(main.TIMEOUT_NAMES[main.currentTimeoutIndex]));
                try {
                    android.provider.Settings.System.putInt(main.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, main.TIMEOUT_VALUES[main.currentTimeoutIndex]);
                    main.prefs.edit().putInt("screen_timeout_index", main.currentTimeoutIndex).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnTimeout);
        focusFirstItem();
    }

    public void buildControlSettingsUI() {
        main.currentSettingsDepth = 1;
        clearSettingsUI();
        updateSettingsTitle(t("Control & Feedback"));

        final LinearLayout btnSound = createSettingRow(t("Button Sound"), main.isSoundEffectEnabled ? t("ON") : t("OFF"));
        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.isSoundEffectEnabled = !main.isSoundEffectEnabled;
                main.applySoundSetting();
                clickFeedback();
                TextView tvStatus = (TextView) btnSound.getChildAt(1);
                tvStatus.setText(main.isSoundEffectEnabled ? t("ON") : t("OFF"));
                try {
                    main.prefs.edit().putBoolean("sound", main.isSoundEffectEnabled).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnSound);

        LinearLayout btnVibrateMenu = createSettingRow(t("Vibration"), "〉 ");
        btnVibrateMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildVibrationSettingsUI();
            }
        });
        main.containerSettingsItems.addView(btnVibrateMenu);

        final LinearLayout btnLoopScrollToggle = createSettingRow(t("Wheel Loop Scroll"), main.isLoopScrollOn ? t("ON") : t("OFF"));
        btnLoopScrollToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.isLoopScrollOn = !main.isLoopScrollOn;
                TextView tvStatus = (TextView) btnLoopScrollToggle.getChildAt(1);
                tvStatus.setText(main.isLoopScrollOn ? t("ON") : t("OFF"));
                main.updateMainMenuBackground();
                try {
                    main.prefs.edit().putBoolean("loop_scroll", main.isLoopScrollOn).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnLoopScrollToggle);

        final LinearLayout btnScreenOffCtrl = createSettingRow(t("Screen-Off Control"), main.isScreenOffControlEnabled ? t("ON") : t("OFF"));
        btnScreenOffCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.isScreenOffControlEnabled = !main.isScreenOffControlEnabled;
                TextView tvStatus = (TextView) btnScreenOffCtrl.getChildAt(1);
                tvStatus.setText(main.isScreenOffControlEnabled ? t("ON") : t("OFF"));
                try {
                    main.prefs.edit().putBoolean("screen_off_control", main.isScreenOffControlEnabled).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnScreenOffCtrl);
        focusFirstItem();
    }

    public void buildNetworkSettingsUI() {
        main.currentSettingsDepth = 1;
        clearSettingsUI();
        updateSettingsTitle(t("Network & Connections"));

        LinearLayout btnWifiMenu = createSettingRow(t("Wi-Fi"), "〉 ");
        btnWifiMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeScreen(6); // STATE_WIFI
                main.startWifiScan();
                clickFeedback();
            }
        });
        main.containerSettingsItems.addView(btnWifiMenu);

        LinearLayout btnBtMenu = createSettingRow(t("Bluetooth"), "〉 ");
        btnBtMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeScreen(5); // STATE_BLUETOOTH
                main.startBluetoothScan();
                clickFeedback();
            }
        });
        main.containerSettingsItems.addView(btnBtMenu);

        LinearLayout btnServerMenu = createSettingRow(t("Web Server"), "〉 ");
        btnServerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeScreen(10); // STATE_WEBSERVER
                clickFeedback();
            }
        });
        main.containerSettingsItems.addView(btnServerMenu);

        final LastFmManager lfm = LastFmManager.getInstance(main);
        final String statusText = lfm.isEnabled() ? lfm.getUsername() : t("Not logged in");
        final LinearLayout btnLastFm = createSettingRow(t("Last.fm Scrobbling"), statusText + " 〉");
        btnLastFm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                if (lfm.isEnabled()) {
                    showLastFmLogoutDialog(btnLastFm);
                } else {
                    showLastFmLoginGuideDialog();
                }
            }
        });
        main.containerSettingsItems.addView(btnLastFm);

        focusFirstItem();
    }

    private void showLastFmLoginGuideDialog() {
        final Dialog dialog = new Dialog(main);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        float d = main.getResources().getDisplayMetrics().density;

        final LinearLayout rootLayout = new LinearLayout(main);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0xEE000000);
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (16 * d), (int) (18 * d), (int) (16 * d), (int) (16 * d));

        // 1. 타이틀
        TextView tvTitle = new TextView(main);
        tvTitle.setText("🎵 " + t("Last.fm Login Guide"));
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (12 * d));
        rootLayout.addView(tvTitle);

        // 2. 설명 메시지 (다국어 지원)
        TextView tvMsg = new TextView(main);
        tvMsg.setText(t("Please start Wireless PC Upload (Web Server) and log in to Last.fm from your PC or smartphone browser."));
        tvMsg.setTextColor(ThemeManager.getTextColorSecondary());
        tvMsg.setTextSize(14f);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding((int) (4 * d), 0, (int) (4 * d), (int) (16 * d));
        rootLayout.addView(tvMsg);

        // 휠 조향 장치
        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21 || keyCode == 19 || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
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
                    if (keyCode == 22 || keyCode == 20 || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
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

        // 3. 웹서버 바로가기 버튼
        View btnGoServer = main.createListButtonWithIcon("\uE2C6", t("Start Web Server"), 0xFF81C784);
        btnGoServer.setOnKeyListener(dialogWheelListener);
        btnGoServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                dialog.dismiss();
                main.changeScreen(10); // 10: STATE_WEBSERVER
            }
        });
        rootLayout.addView(btnGoServer);

        // 4. 닫기 버튼
        View btnClose = main.createListButtonWithIcon("\uE5CD", t("Cancel"), ThemeManager.getTextColorPrimary());
        btnClose.setOnKeyListener(dialogWheelListener);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                dialog.dismiss();
            }
        });
        rootLayout.addView(btnClose);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (btnGoServer.isFocusable()) {
                    btnGoServer.requestFocus();
                }
            }
        }, 50);
    }

    private void showLastFmLogoutDialog(final LinearLayout btnLastFm) {
        final Dialog dialog = new Dialog(main);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        float d = main.getResources().getDisplayMetrics().density;

        final LinearLayout rootLayout = new LinearLayout(main);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ThemeManager.getOverlayBackgroundColor() | 0xEE000000);
        bg.setCornerRadius(15 * d);
        bg.setStroke((int) (1 * d), 0x33FFFFFF);
        rootLayout.setBackground(bg);
        rootLayout.setPadding((int) (16 * d), (int) (18 * d), (int) (16 * d), (int) (16 * d));

        TextView tvTitle = new TextView(main);
        tvTitle.setText("🎵 " + t("Last.fm Logout"));
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (12 * d));
        rootLayout.addView(tvTitle);

        TextView tvMsg = new TextView(main);
        tvMsg.setText(t("Are you sure you want to log out from Last.fm?"));
        tvMsg.setTextColor(ThemeManager.getTextColorSecondary());
        tvMsg.setTextSize(14f);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding((int) (4 * d), 0, (int) (4 * d), (int) (16 * d));
        rootLayout.addView(tvMsg);

        View.OnKeyListener dialogWheelListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == 21 || keyCode == 19 || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
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
                    if (keyCode == 22 || keyCode == 20 || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
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

        View btnLogout = main.createListButtonWithIcon("\uE872", t("Logout"), 0xFFFF5555);
        btnLogout.setOnKeyListener(dialogWheelListener);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                dialog.dismiss();
                LastFmManager.getInstance(main).logout();
                ((TextView) btnLastFm.getChildAt(1)).setText(t("Not logged in") + " 〉");
                main.updatePlayerStatusIndicators();
                Toast.makeText(main, t("Logged out"), Toast.LENGTH_SHORT).show();
            }
        });
        rootLayout.addView(btnLogout);

        View btnCancel = main.createListButtonWithIcon("\uE5CD", t("Cancel"), ThemeManager.getTextColorPrimary());
        btnCancel.setOnKeyListener(dialogWheelListener);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                dialog.dismiss();
            }
        });
        rootLayout.addView(btnCancel);

        dialog.setContentView(rootLayout);
        Window window = dialog.getWindow();
        if (window != null) window.setLayout((int) (300 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();

        rootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (btnLogout.isFocusable()) {
                    btnLogout.requestFocus();
                }
            }
        }, 50);
    }

    public void buildDataSettingsUI() {
        main.currentSettingsDepth = 1;
        clearSettingsUI();
        updateSettingsTitle(t("Data & Storage"));

        LinearLayout btnStorageMenu = createSettingRow(t("Storage"), "〉 ");
        btnStorageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.changeScreen(9); // STATE_STORAGE
                clickFeedback();
            }
        });
        main.containerSettingsItems.addView(btnStorageMenu);

        final LinearLayout btnAutoFetch = createSettingRow(t("Auto Fetch Album Art"), main.isAutoFetchEnabled ? t("ON") : t("OFF"));
        btnAutoFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.isAutoFetchEnabled = !main.isAutoFetchEnabled;
                ((TextView) btnAutoFetch.getChildAt(1)).setText(main.isAutoFetchEnabled ? t("ON") : t("OFF"));
                try {
                    main.prefs.edit().putBoolean("auto_fetch", main.isAutoFetchEnabled).commit();
                } catch (Exception e) {}
            }
        });
        main.containerSettingsItems.addView(btnAutoFetch);

        LinearLayout btnClearCache = createSettingRow(t("Clear Album Art & Info"), "〉 ");
        btnClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                new AlertDialog.Builder(main, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle(t("Clear Cache"))
                        .setMessage(t("Clear all cached album arts and song information?"))
                        .setPositiveButton(t("Clear"), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    File cacheDir = main.getCacheDir();
                                    File[] files = cacheDir.listFiles();
                                    if (files != null) {
                                        for (File f : files) f.delete();
                                    }
                                    main.prefs.edit().remove("saved_song_info").commit();
                                    Toast.makeText(main, t("Cache cleared."), Toast.LENGTH_SHORT).show();
                                    main.refreshNowPlayingPreview();
                                } catch (Exception e) {}
                            }
                        })
                        .setNegativeButton(t("Cancel"), null)
                        .show();
            }
        });
        main.containerSettingsItems.addView(btnClearCache);
        focusFirstItem();
    }

    public void buildSystemSettingsUI() {
        main.currentSettingsDepth = 1;
        clearSettingsUI();
        updateSettingsTitle(t("System"));

        LinearLayout btnTime = createSettingRow(t("Date & Time"), "〉");
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                // 🚀 시스템 시간을 먼저 읽어와서 임시 변수에 저장합니다.
                java.util.Calendar c = java.util.Calendar.getInstance();
                main.dtYear = c.get(java.util.Calendar.YEAR);
                main.dtMonth = c.get(java.util.Calendar.MONTH) + 1;
                main.dtDay = c.get(java.util.Calendar.DAY_OF_MONTH);
                main.dtHour = c.get(java.util.Calendar.HOUR_OF_DAY);
                main.dtMinute = c.get(java.util.Calendar.MINUTE);
                main.buildDateTimeUI();
            }
        });
        main.containerSettingsItems.addView(btnTime);

        String displayLang = LanguageManager.getInstance(main).currentLangFileName.replace(".json", "");
        LinearLayout btnLangMenu = createSettingRow(t("Language"), displayLang);
        btnLangMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildLanguageSelectorUI();
            }
        });
        main.containerSettingsItems.addView(btnLangMenu);

        String myVersionName = "1.0";
        try {
            myVersionName = main.getPackageManager().getPackageInfo(main.getPackageName(), 0).versionName;
        } catch (Exception e) {}
        LinearLayout btnUpdateCheck = createSettingRow(t("System Update"), "v" + myVersionName);
        btnUpdateCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildUpdateSettingsUI();
            }
        });
        main.containerSettingsItems.addView(btnUpdateCheck);

        LinearLayout btnPowerOff = createSettingRow(t("Power Off"), "〉 ");
        btnPowerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                new AlertDialog.Builder(main, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle(t("Power Off"))
                        .setMessage(t("Do you want to shut down the device?"))
                        .setPositiveButton(t("Shut Down"), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot -p"});
                                    proc.waitFor();
                                } catch (Exception e) {
                                    try {
                                        Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
                                        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        main.startActivity(intent);
                                    } catch (Exception ex) {
                                        Toast.makeText(main, t("System security prevents powering off directly from the app."), Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        })
                        .setNegativeButton(t("Cancel"), null)
                        .show();
            }
        });
        main.containerSettingsItems.addView(btnPowerOff);
        LinearLayout btnBatteryTime = createSettingRow(t("Battery Time"), "〉 ");
        btnBatteryTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                buildBatteryStatsUI();
            }
        });
        main.containerSettingsItems.addView(btnBatteryTime);

        focusFirstItem();
    }

    public void buildBatteryStatsUI() {
        main.currentSettingsDepth = 2; // Inside System
        clearSettingsUI();
        updateSettingsTitle(t("Battery Time"));

        com.themoon.y1.managers.BatteryStatsManager bsm = com.themoon.y1.managers.BatteryStatsManager.getInstance(main);

        // Header: Last Charged Time
        long lastReset = bsm.getLastResetTime();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        TextView tvHeader = new TextView(main);
        tvHeader.setTag("battery_fixed");
        tvHeader.setText(t("Last Charged") + ": " + sdf.format(new java.util.Date(lastReset)));
        tvHeader.setTextColor(com.themoon.y1.ThemeManager.getTextColorPrimary());
        tvHeader.setTextSize(18f);
        tvHeader.setPadding(20, 20, 20, 10);
        ((ViewGroup) main.layoutSettingsMode).addView(tvHeader, 1);

        // Graph Container
        LinearLayout graphLayout = new LinearLayout(main);
        graphLayout.setTag("battery_fixed");
        graphLayout.setOrientation(LinearLayout.HORIZONTAL);
        graphLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 40));
        graphLayout.setPadding(20, 0, 20, 20);

        int dropM = bsm.getDrop(com.themoon.y1.managers.BatteryStatsManager.MODE_MUSIC);
        int dropV = bsm.getDrop(com.themoon.y1.managers.BatteryStatsManager.MODE_VIDEO);
        int dropR = bsm.getDrop(com.themoon.y1.managers.BatteryStatsManager.MODE_RADIO);
        int dropG = bsm.getDrop(com.themoon.y1.managers.BatteryStatsManager.MODE_GAME);
        int dropS = bsm.getDrop(com.themoon.y1.managers.BatteryStatsManager.MODE_STANDBY);
        int dropO = bsm.getDrop(com.themoon.y1.managers.BatteryStatsManager.MODE_OTHER);
        int totalDrop = dropM + dropV + dropR + dropG + dropS + dropO;
        if (totalDrop == 0) totalDrop = 1; // Prevent div by zero

        // Add Segments
        addSegment(graphLayout, dropM, 0xFF42A5F5); // Music - Blue
        addSegment(graphLayout, dropV, 0xFFEF5350); // Video - Red
        addSegment(graphLayout, dropR, 0xFFFFA726); // Radio - Orange
        addSegment(graphLayout, dropG, 0xFFAB47BC); // Game - Purple
        addSegment(graphLayout, dropO, 0xFF26A69A); // Other - Teal
        addSegment(graphLayout, dropS, 0xFF9E9E9E); // Standby - Gray

        // If no usage, show empty gray bar
        if (graphLayout.getChildCount() == 0) {
            View segment = new View(main);
            segment.setBackgroundColor(0xFF424242);
            segment.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            graphLayout.addView(segment);
        }

        ((ViewGroup) main.layoutSettingsMode).addView(graphLayout, 2);

        // Details List
        addDetail(main.containerSettingsItems, bsm, com.themoon.y1.managers.BatteryStatsManager.MODE_MUSIC);
        addDetail(main.containerSettingsItems, bsm, com.themoon.y1.managers.BatteryStatsManager.MODE_VIDEO);
        addDetail(main.containerSettingsItems, bsm, com.themoon.y1.managers.BatteryStatsManager.MODE_RADIO);
        addDetail(main.containerSettingsItems, bsm, com.themoon.y1.managers.BatteryStatsManager.MODE_GAME);
        addDetail(main.containerSettingsItems, bsm, com.themoon.y1.managers.BatteryStatsManager.MODE_OTHER);
        addDetail(main.containerSettingsItems, bsm, com.themoon.y1.managers.BatteryStatsManager.MODE_STANDBY);

        main.containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 스크롤을 최상단으로 리셋
                android.view.ViewParent parent = main.containerSettingsItems.getParent();
                if (parent instanceof android.widget.ScrollView) {
                    ((android.widget.ScrollView) parent).scrollTo(0, 0);
                }
                // Now that Header and Graph are out of containerSettingsItems, the first row is at index 0!
                if (main.containerSettingsItems.getChildCount() > 0) {
                    main.containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        }, 50);
    }

    private void addSegment(LinearLayout graphLayout, int drop, int color) {
        if (drop > 0) {
            View segment = new View(main);
            segment.setBackgroundColor(color);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) drop);
            segment.setLayoutParams(lp);
            graphLayout.addView(segment);
        }
    }

    private void addDetail(LinearLayout parent, com.themoon.y1.managers.BatteryStatsManager bsm, int mode) {
        int drop = bsm.getDrop(mode);
        long timeMs = bsm.getTime(mode);
        String title = "";
        int color = 0;
        switch(mode) {
            case com.themoon.y1.managers.BatteryStatsManager.MODE_MUSIC: title = t("Music"); color = 0xFF42A5F5; break;
            case com.themoon.y1.managers.BatteryStatsManager.MODE_VIDEO: title = t("Video"); color = 0xFFEF5350; break;
            case com.themoon.y1.managers.BatteryStatsManager.MODE_RADIO: title = t("Radio"); color = 0xFFFFA726; break;
            case com.themoon.y1.managers.BatteryStatsManager.MODE_GAME: title = t("Game"); color = 0xFFAB47BC; break;
            case com.themoon.y1.managers.BatteryStatsManager.MODE_OTHER: title = t("Other"); color = 0xFF26A69A; break;
            case com.themoon.y1.managers.BatteryStatsManager.MODE_STANDBY: title = t("Standby"); color = 0xFF9E9E9E; break;
        }
        long hours = timeMs / 3600000;
        long mins = (timeMs % 3600000) / 60000;
        String timeStr = hours + "h " + mins + "m";
        
        LinearLayout row = createSettingRow(title, timeStr + " (" + drop + "%)");
        
        final TextView tvTitle = (TextView) row.getChildAt(0);
        final TextView tvRight = (TextView) row.getChildAt(1);
        
        // Add a color dot
        String dot = "● ";
        android.text.SpannableString sp = new android.text.SpannableString(dot + title);
        sp.setSpan(new android.text.style.ForegroundColorSpan(color), 0, dot.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTitle.setText(sp);
        
        // Overwrite focus listener to fix right text color
        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    row.setBackground(main.createButtonBackground(com.themoon.y1.ThemeManager.getListButtonFocusedBg()));
                    tvTitle.setTextColor(com.themoon.y1.ThemeManager.getListButtonFocusedTextColor());
                    tvRight.setTextColor(com.themoon.y1.ThemeManager.getListButtonFocusedTextColor());
                } else {
                    row.setBackground(main.createButtonBackground(com.themoon.y1.ThemeManager.getListButtonNormalBg()));
                    tvTitle.setTextColor(com.themoon.y1.ThemeManager.getTextColorPrimary());
                    tvRight.setTextColor(com.themoon.y1.ThemeManager.getTextColorPrimary()); // ALWAYS PRIMARY
                }
            }
        });
        
        // Trigger initial color
        tvRight.setTextColor(com.themoon.y1.ThemeManager.getTextColorPrimary());
        
        row.setFocusable(true);
        parent.addView(row);
    }


    public void restoreSubMenu() {
        switch (lastSettingsFocusIndex) {
            case 0: buildAudioSettingsUI(); break;
            case 1: buildDisplaySettingsUI(); break;
            case 2: main.buildThemeSelectorUI(); break; // 🚀 테마 (2번)
            case 3: buildControlSettingsUI(); break;    // 🚀 컨트롤 (3번)
            case 4: buildNetworkSettingsUI(); break;    // 🚀 네트워크 (4번)
            case 5: buildDataSettingsUI(); break;       // 🚀 데이터 (5번)
            case 6: buildSystemSettingsUI(); break;     // 🚀 시스템 (6번)
            default: buildSettingsUI(); break;
        }
    }

    public void updateSettingsTitle(String subCategory) {
        ViewGroup settingsGroup = (ViewGroup) main.layoutSettingsMode;
        if (settingsGroup != null && settingsGroup.getChildCount() > 0 && settingsGroup.getChildAt(0) instanceof TextView) {
            TextView header = (TextView) settingsGroup.getChildAt(0);
            header.setVisibility(View.VISIBLE);
            header.setTextColor(ThemeManager.getTextColorPrimary());
            if (subCategory == null) {
                header.setText(t("Settings"));
            } else {
                header.setText(t("Settings") + " > " + subCategory);
            }
        }
    }

    public void focusFirstItem() {
        main.containerSettingsItems.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 🚀 스크롤을 최상단으로 리셋
                android.view.ViewParent parent = main.containerSettingsItems.getParent();
                if (parent instanceof android.widget.ScrollView) {
                    ((android.widget.ScrollView) parent).scrollTo(0, 0);
                }

                if (main.currentSettingsDepth == 1 && lastSubMenuFocusIndex >= 0
                        && main.containerSettingsItems.getChildCount() > lastSubMenuFocusIndex) {
                    main.containerSettingsItems.getChildAt(lastSubMenuFocusIndex).requestFocus();
                } else if (main.containerSettingsItems.getChildCount() > 0) {
                    main.containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        }, 50);
    }

    public LinearLayout createCategoryButton(String title, final int index, final Runnable onClick) {
        final LinearLayout btn = createSettingRow(title, "〉");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                lastSettingsFocusIndex = index;
                if (main.currentSettingsDepth == 0) {
                    lastSubMenuFocusIndex = 0;
                }
                onClick.run();
            }
        });
        btn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                TextView tvLeft = (TextView) btn.getChildAt(0);
                TextView tvRight = (TextView) btn.getChildAt(1);
                if (hasFocus) {
                    btn.setBackground(main.createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    tvLeft.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    lastSettingsFocusIndex = index;
                } else {
                    btn.setBackground(main.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    tvLeft.setTextColor(ThemeManager.getTextColorPrimary());
                    tvRight.setTextColor(ThemeManager.getTextColorSecondary());
                }
            }
        });
        return btn;
    }

    public LinearLayout createSettingRow(String leftText, String rightText) {
        final LinearLayout row = new LinearLayout(main);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setSoundEffectsEnabled(false);
        
        final GradientDrawable normalBg = main.createButtonBackground(ThemeManager.getListButtonNormalBg());
        final GradientDrawable focusedBg = main.createButtonBackground(ThemeManager.getListButtonFocusedBg());
        row.setBackground(normalBg);
        
        row.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(rowLp);

        final TextView tvLeft = new TextView(main);
        tvLeft.setText(leftText);
        tvLeft.setTextColor(ThemeManager.getTextColorPrimary());
        tvLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView tvRight = new TextView(main);
        tvRight.setText(rightText);
        tvRight.setTextColor(ThemeManager.getTextColorPrimary());
        tvRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvRight.setGravity(Gravity.RIGHT);

        row.addView(tvLeft);
        row.addView(tvRight);

        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (main.currentSettingsDepth == 1) {
                        lastSubMenuFocusIndex = main.containerSettingsItems.indexOfChild(row);
                    }
                    row.setBackground(focusedBg);
                    tvLeft.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                } else {
                    row.setBackground(normalBg);
                    tvLeft.setTextColor(ThemeManager.getTextColorPrimary());
                    tvRight.setTextColor(ThemeManager.getTextColorPrimary());
                }
            }
        });

        return row;
    }

    // =======================================================
    // 🚀 [스크린 필터 스튜디오] 채도 & RGB 슬라이더 팝업
    // =======================================================
    public void buildScreenFilterSettingsUI() {
        main.currentSettingsDepth = 2;
        clearSettingsUI();
        updateSettingsTitle(t("Screen Filter"));

        final boolean[] isEnabled = {main.prefs.getBoolean("filter_enabled", false)};

        // 1. 필터 전원 스위치
        final LinearLayout btnToggle = createSettingRow(t("Filter Power"), isEnabled[0] ? t("ON") : t("OFF"));
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                isEnabled[0] = !isEnabled[0];
                ((TextView) btnToggle.getChildAt(1)).setText(isEnabled[0] ? t("ON") : t("OFF"));
                main.prefs.edit().putBoolean("filter_enabled", isEnabled[0]).commit();
                main.applyScreenFilter(); // 💡 켜고 끌 때 즉시 적용!
            }
        });
        main.containerSettingsItems.addView(btnToggle);

        // =======================================================
        // 🎨 2. [신규 장착] 실시간 색상 팔레트 (Current Color) 표시기!
        // =======================================================
        float d = main.getResources().getDisplayMetrics().density;
        final TextView paletteView = new TextView(main);
        paletteView.setText(t("Current Color"));
        paletteView.setTextColor(0xFFFFFFFF);
        paletteView.setTextSize(16f);
        paletteView.setTypeface(ThemeManager.getCustomFont(), android.graphics.Typeface.BOLD);
        paletteView.setGravity(Gravity.CENTER);
        paletteView.setShadowLayer(4, 0, 0, 0xFF000000); // 💡 밝은 색상에서도 글씨가 잘 보이게 진한 그림자!

        LinearLayout.LayoutParams palLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(60 * d));
        palLp.setMargins((int)(20*d), (int)(10*d), (int)(20*d), (int)(15*d));
        paletteView.setLayoutParams(palLp);

        final GradientDrawable paletteBg = new GradientDrawable();
        paletteBg.setCornerRadius(15 * d);
        paletteBg.setStroke((int)(2*d), 0x33FFFFFF);
        paletteView.setBackground(paletteBg);
        main.containerSettingsItems.addView(paletteView);

        // 💡 팔레트 색상 실시간 갱신용 도우미
        final Runnable updatePalette = new Runnable() {
            @Override
            public void run() {
                int r = main.prefs.getInt("filter_r", 136);
                int g = main.prefs.getInt("filter_g", 204);
                int b = main.prefs.getInt("filter_b", 136);
                paletteBg.setColor(android.graphics.Color.rgb(r, g, b));
                paletteView.invalidate();
            }
        };
        updatePalette.run(); // 진입 시 1회 색상 적용
        // =======================================================

        // 3. 현재 저장된 설정값 불러오기
        int sat = main.prefs.getInt("filter_saturation", 0);
        int r = main.prefs.getInt("filter_r", 136);
        int g = main.prefs.getInt("filter_g", 204);
        int b = main.prefs.getInt("filter_b", 136);

        // 4. 채도(Saturation) 슬라이더 (0~100)
        main.containerSettingsItems.addView(createSliderRow(t("Saturation"), 100, sat, new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean fromUser) {
                main.prefs.edit().putInt("filter_saturation", p).commit();
                main.applyScreenFilter();
                updatePalette.run(); // 🎨 슬라이더 움직일 때마다 팔레트 갱신!
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        }));

        // 5. Red 슬라이더 (0~255)
        main.containerSettingsItems.addView(createSliderRow(t("Red"), 255, r, new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean fromUser) {
                main.prefs.edit().putInt("filter_r", p).commit();
                main.applyScreenFilter();
                updatePalette.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        }));

        // 6. Green 슬라이더 (0~255)
        main.containerSettingsItems.addView(createSliderRow(t("Green"), 255, g, new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean fromUser) {
                main.prefs.edit().putInt("filter_g", p).commit();
                main.applyScreenFilter();
                updatePalette.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        }));

        // 7. Blue 슬라이더 (0~255)
        main.containerSettingsItems.addView(createSliderRow(t("Blue"), 255, b, new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int p, boolean fromUser) {
                main.prefs.edit().putInt("filter_b", p).commit();
                main.applyScreenFilter();
                updatePalette.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        }));

        // 8. 기본값 리셋 버튼
        Button btnReset = main.createListButton("🔄 " + t("Reset to Default"));
        btnReset.setTextColor(0xFFFF8800);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.prefs.edit()
                        .putInt("filter_saturation", 0)
                        .putInt("filter_r", 136)
                        .putInt("filter_g", 204)
                        .putInt("filter_b", 136)
                        .commit();
                main.applyScreenFilter();
                buildScreenFilterSettingsUI(); // 화면 리로드
                Toast.makeText(main, t("Reset to Default"), Toast.LENGTH_SHORT).show();
            }
        });
        main.containerSettingsItems.addView(btnReset);

        if (main.containerSettingsItems.getChildCount() > 0) {
            main.containerSettingsItems.getChildAt(0).requestFocus();
        }
    }

    // 🚀 [슬라이더 UI 생성기] 가운데 버튼 클릭으로 포커스를 풀고 잠그는 스위치 엔진 장착!
    private LinearLayout createSliderRow(final String titleText, final int max, int progress, final android.widget.SeekBar.OnSeekBarChangeListener listener) {
        float d = main.getResources().getDisplayMetrics().density;
        final LinearLayout row = new LinearLayout(main);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(main.createButtonBackground(ThemeManager.getListButtonNormalBg()));
        row.setPadding((int)(20*d), (int)(15*d), (int)(20*d), (int)(15*d));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(rowLp);
        row.setFocusable(true);
        row.setClickable(true);
        row.setSoundEffectsEnabled(false);

        // =======================================================
        // 🎯 [핵심 엔진] 편집 모드(슬라이더 휠 조작 상태)인지 기억하는 변수!
        // =======================================================
        final boolean[] isEditing = {false};

        LinearLayout header = new LinearLayout(main);
        header.setOrientation(LinearLayout.HORIZONTAL);
        final TextView tvTitle = new TextView(main);
        tvTitle.setText(titleText);
        tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(ThemeManager.getCustomFont(), android.graphics.Typeface.NORMAL);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView tvVal = new TextView(main);
        tvVal.setText(String.valueOf(progress));
        tvVal.setTextColor(ThemeManager.getTextColorSecondary());
        tvVal.setTextSize(18f);
        tvVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVal.setGravity(Gravity.RIGHT);

        header.addView(tvTitle);
        header.addView(tvVal);
        row.addView(header);

        final android.widget.SeekBar seekBar = new android.widget.SeekBar(main);
        seekBar.setMax(max);
        seekBar.setProgress(progress);
        // 💡 0이나 최대값(255)에서 동그란 썸(Thumb)이 잘리지 않도록 좌우 패딩을 18dp로 여유있게 설정!
        seekBar.setPadding((int)(18*d), (int)(15*d), (int)(18*d), (int)(5*d));
        seekBar.setFocusable(false); // 💡 슬라이더 자체는 포커스를 받지 않음 (부모 row가 다 통제함)

        // 🚀 슬라이더 바 색상 실시간 적응 엔진 (포커스/편집 상태에 따라 동적 변경)
        final Runnable updateSliderColor = new Runnable() {
            @Override
            public void run() {
                int color;
                if (isEditing[0]) {
                    color = 0xFFFF8800; // ✏️ 편집 모드: 주황색 하이라이트!
                } else if (row.hasFocus()) {
                    // 🚀 선택(포커스) 시 포커스 배경색과 겹쳐 안보이는 문제 해결을 위해 고대비 선택 글자색 적용!
                    color = ThemeManager.getListButtonFocusedTextColor();
                    if (color == 0) color = 0xFFFFFFFF; // 안전장치
                } else {
                    // 평소 (비선택) 상태: 테마 강조색
                    color = ThemeManager.getListButtonFocusedBg() | 0xFF000000;
                }
                try {
                    seekBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                    if (android.os.Build.VERSION.SDK_INT >= 16) {
                        seekBar.getThumb().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                } catch (Exception e) {}
            }
        };
        updateSliderColor.run();

        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar sb, int p, boolean fromUser) {
                tvVal.setText(String.valueOf(p));
                if (listener != null) listener.onProgressChanged(sb, p, fromUser);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });
        row.addView(seekBar);

        // =======================================================
        // 🚀 가운데 클릭 시 편집 모드(Edit Mode) ON / OFF 전환 스위치!
        // =======================================================
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.clickFeedback();
                isEditing[0] = !isEditing[0]; // 모드 전환!

                if (isEditing[0]) {
                    // 편집 모드 ON: 주황색으로 반짝이고 텍스트 옆에 연필 아이콘!
                    tvVal.setTextColor(0xFFFF8800);
                    tvTitle.setText(titleText + " ✏️");
                } else {
                    // 편집 모드 OFF: 원래대로 복구
                    tvVal.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvTitle.setText(titleText);
                }
                updateSliderColor.run(); // 💡 편집 모드 전환 시 슬라이더 바 색상도 변경!
            }
        });

        // 🚀 포커스 하이라이트 효과 (편집 모드가 아닐 때 위아래로 빠져나가면 편집 강제 종료)
        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    row.setBackground(main.createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    tvTitle.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvVal.setTextColor(isEditing[0] ? 0xFFFF8800 : ThemeManager.getListButtonFocusedTextColor());
                } else {
                    // 포커스가 빠져나가면 강제로 편집 모드 해제!
                    isEditing[0] = false;
                    tvTitle.setText(titleText);
                    row.setBackground(main.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    tvTitle.setTextColor(ThemeManager.getTextColorPrimary());
                    tvVal.setTextColor(ThemeManager.getTextColorSecondary());
                }
                updateSliderColor.run(); // 💡 포커스 변경 시 슬라이더 바 색상 자동 대비 변환!
            }
        });

        // =======================================================
        // 🚀 물리 버튼(휠) 가로채기 엔진
        // =======================================================
        row.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                // 편집 모드일 때만 휠의 모든(상하좌우) 이벤트를 가로채서 슬라이더를 움직입니다!
                if (isEditing[0] && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    int step = max > 100 ? 5 : 2;
                    if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT || keyCode == 21 || keyCode == 19) {
                        seekBar.setProgress(Math.max(0, seekBar.getProgress() - step));
                        return true; // 🚨 여기서 이벤트를 먹어버려서 리스트 스크롤이 잠깁니다!
                    } else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == 22 || keyCode == 20) {
                        seekBar.setProgress(Math.min(seekBar.getMax(), seekBar.getProgress() + step));
                        return true;
                    }
                }
                // 편집 모드가 아니면(false), 위/아래 휠 신호를 시스템에 그대로 반환해서 다른 항목으로 스크롤이 되게 합니다!
                return false;
            }
        });

        return row;
    }

}