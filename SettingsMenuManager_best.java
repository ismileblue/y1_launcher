package com.themoon.y1.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
    private int lastSettingsFocusIndex = 0;

    private SettingsMenuManager(MainActivity main) {
        this.main = main;
    }

    public static SettingsMenuManager getInstance(MainActivity main) {
        if (instance == null) {
            instance = new SettingsMenuManager(main);
        }
        return instance;
    }

    private String t(String text) {
        return main.t(text);
    }

    private void clickFeedback() {
        main.clickFeedback();
    }

    public void buildSettingsUI() {
        main.currentSettingsDepth = 0;
        main.isRadioUIShowing = false;
        main.isRadioSettingsMode = false;

        ViewGroup settingsGroup = (ViewGroup) main.layoutSettingsMode;
        if (settingsGroup != null && settingsGroup.getChildCount() > 0 && settingsGroup.getChildAt(0) instanceof TextView) {
            settingsGroup.getChildAt(0).setVisibility(View.VISIBLE);
        }

        main.containerSettingsItems.removeAllViews();

        main.containerSettingsItems.addView(createCategoryButton(t("Audio & Playback"), 0, new Runnable() {
            public void run() { buildAudioSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Display & Personalization"), 1, new Runnable() {
            public void run() { buildDisplaySettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Control & Feedback"), 2, new Runnable() {
            public void run() { buildControlSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Network & Connections"), 3, new Runnable() {
            public void run() { buildNetworkSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("Data & Storage"), 4, new Runnable() {
            public void run() { buildDataSettingsUI(); }
        }));
        main.containerSettingsItems.addView(createCategoryButton(t("System"), 5, new Runnable() {
            public void run() { buildSystemSettingsUI(); }
        }));

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > lastSettingsFocusIndex) {
                    main.containerSettingsItems.getChildAt(lastSettingsFocusIndex).requestFocus();
                } else if (main.containerSettingsItems.getChildCount() > 0) {
                    main.containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        });
    }

    public void buildAudioSettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("Audio & Playback") + " ━");

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

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 1) {
                    main.containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        });
    }

    public void buildDisplaySettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("Display & Personalization") + " ━");

        final LinearLayout btnTheme = createSettingRow(t("Theme"), ThemeManager.getCurrentTheme().name);
        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
                main.buildThemeSelectorUI();
            }
        });
        main.containerSettingsItems.addView(btnTheme);

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

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 1) {
                    main.containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        });
    }

    public void buildControlSettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("Control & Feedback") + " ━");

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

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 1) {
                    main.containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        });
    }

    public void buildNetworkSettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("Network & Connections") + " ━");

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

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 1) {
                    main.containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        });
    }

    public void buildDataSettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("Data & Storage") + " ━");

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

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 1) {
                    main.containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        });
    }

    public void buildSystemSettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
        createCategoryHeader("━ " + t("System") + " ━");

        LinearLayout btnTime = createSettingRow(t("Date & Time"), "〉");
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
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

        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 1) {
                    main.containerSettingsItems.getChildAt(1).requestFocus();
                }
            }
        });
    }

    public void createCategoryHeader(String title) {
        TextView header = new TextView(main);
        header.setText(title);
        header.setTextColor(ThemeManager.getTextColorSecondary());
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 8);
        header.setLayoutParams(lp);

        main.containerSettingsItems.addView(header);
    }

    public LinearLayout createCategoryButton(String title, final int index, final Runnable onClick) {
        final LinearLayout btn = createSettingRow(title, "〉");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFeedback();
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
        
        GradientDrawable normalBg = main.createButtonBackground(ThemeManager.getListButtonNormalBg());
        row.setBackground(normalBg);
        
        row.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(rowLp);

        final TextView tvLeft = new TextView(main);
        tvLeft.setText(leftText);
        tvLeft.setTextColor(ThemeManager.getTextColorPrimary());
        tvLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView tvRight = new TextView(main);
        tvRight.setText(rightText);
        tvRight.setTextColor(ThemeManager.getTextColorSecondary());
        tvRight.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvRight.setGravity(Gravity.RIGHT);

        row.addView(tvLeft);
        row.addView(tvRight);

        row.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    row.setBackground(main.createButtonBackground(ThemeManager.getListButtonFocusedBg()));
                    tvLeft.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                    tvRight.setTextColor(ThemeManager.getListButtonFocusedTextColor());
                } else {
                    row.setBackground(main.createButtonBackground(ThemeManager.getListButtonNormalBg()));
                    tvLeft.setTextColor(ThemeManager.getTextColorPrimary());
                    tvRight.setTextColor(ThemeManager.getTextColorSecondary());
                }
            }
        });

        return row;
    }
}