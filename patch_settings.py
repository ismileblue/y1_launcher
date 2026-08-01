import sys
import re

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\managers\SettingsMenuManager.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target = r'final LinearLayout btnTimeout = createSettingRow\(t\("Screen Timeout"\), t\(main\.TIMEOUT_NAMES\[main\.currentTimeoutIndex\]\)\);'

replacement = r'''final LinearLayout btnBatteryIndicator = createSettingRow(t("Battery Indicator"), t(main.BATTERY_STYLE_NAMES[main.currentBatteryStyleIndex]));
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

        final LinearLayout btnTimeout = createSettingRow(t("Screen Timeout"), t(main.TIMEOUT_NAMES[main.currentTimeoutIndex]));'''

if 'Battery Indicator' not in content:
    content = re.sub(target, replacement, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("SettingsMenuManager patched.")
