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

    public void buildSettingsUI() {
        main.currentSettingsDepth = 0;
        main.isRadioUIShowing = false;
        main.isRadioSettingsMode = false;

        updateSettingsTitle(null);

        main.containerSettingsItems.removeAllViews();

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
        main.containerSettingsItems.removeAllViews();
        updateSettingsTitle(t("Audio & Playback"));

        final LinearLayout btnShuffle = createSettingRow(t("Shuffle Mode"), main.isShuffleMode ? t("ON") : t("OFF"));
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

        final LinearLayout btnRepeat = createSettingRow(t("Repeat Mode"), t(main.getRepeatModeText(main.repeatMode)));
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
        main.containerSettingsItems.removeAllViews();
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
        main.containerSettingsItems.removeAllViews();
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
        main.containerSettingsItems.removeAllViews();
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
        focusFirstItem();
    }

    public void buildDataSettingsUI() {
        main.currentSettingsDepth = 1;
        main.containerSettingsItems.removeAllViews();
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
        main.containerSettingsItems.removeAllViews();
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
        focusFirstItem();
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
        tvLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView tvRight = new TextView(main);
        tvRight.setText(rightText);
        tvRight.setTextColor(ThemeManager.getTextColorSecondary());
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