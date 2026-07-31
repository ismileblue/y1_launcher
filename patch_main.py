import sys
import re

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add currentBatteryStyleIndex and BATTERY_STYLE_NAMES
target1 = r'public int currentTimeoutIndex = 1;'
replacement1 = r'''public int currentBatteryStyleIndex = 0;
    public final String[] BATTERY_STYLE_NAMES = { "Icon Only", "Percent Only", "Icon + Percent" };
    public int currentTimeoutIndex = 1;'''
if 'currentBatteryStyleIndex' not in content:
    content = content.replace(target1, replacement1)

# 2. Load from prefs
target2 = r'currentTimeoutIndex = prefs.getInt("timeout_idx", 1);'
replacement2 = r'''currentTimeoutIndex = prefs.getInt("timeout_idx", 1);
            currentBatteryStyleIndex = prefs.getInt("battery_indicator_style", 0);'''
if 'battery_indicator_style' not in content:
    content = content.replace(target2, replacement2)

# 3. Update ACTION_BATTERY_CHANGED logic
# Find:
#                 if (batteryIconView != null) {
#                     batteryIconView.setBatteryLevel(batteryPct, isCharging);
#                 }
target3 = r'''if \(batteryIconView != null\) \{
                    batteryIconView\.setBatteryLevel\(batteryPct, isCharging\);
                \}'''
replacement3 = r'''if (batteryIconView != null) {
                    batteryIconView.setBatteryLevel(batteryPct, isCharging);
                }
                if (currentBatteryStyleIndex == 0) { // Icon Only
                    if (batteryIconView != null) batteryIconView.setVisibility(View.VISIBLE);
                    tvStatusBattery.setVisibility(View.GONE);
                } else if (currentBatteryStyleIndex == 1) { // Percent Only
                    if (batteryIconView != null) batteryIconView.setVisibility(View.GONE);
                    tvStatusBattery.setVisibility(View.VISIBLE);
                } else { // Icon + Percent
                    if (batteryIconView != null) batteryIconView.setVisibility(View.VISIBLE);
                    tvStatusBattery.setVisibility(View.VISIBLE);
                }'''
if 'currentBatteryStyleIndex == 0' not in content:
    content = re.sub(target3, replacement3, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainActivity patched.")
