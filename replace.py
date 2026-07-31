import sys

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# turnOnScreen (11489 - 11503) -> 11488 to 11503 in 0-indexed
replace2 = """    public void turnOnScreen() {
        try {
            if (isFakeScreenOff) {
                isFakeScreenOff = false;
                if (layoutLoadingOverlay != null) {
                    layoutLoadingOverlay.setBackgroundColor(com.themoon.y1.managers.ThemeManager.getOverlayBackgroundColor() | 0xFF000000);
                    pbLoadingProgress.setVisibility(View.VISIBLE);
                    tvLoadingProgress.setVisibility(View.VISIBLE);
                    layoutLoadingOverlay.setVisibility(View.GONE);
                }
                updateBrightness(currentSystemBrightness);
                return;
            }

            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                pm.getClass().getMethod("wakeUp", long.class).invoke(pm, SystemClock.uptimeMillis());
                Runtime.getRuntime().exec(new String[] { "su", "-c", "input keyevent 26" }); // 🚀 Simulate Power Button to wake
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Runtime.getRuntime().exec(new String[] { "su", "-c", "input keyevent 224" });
                Runtime.getRuntime().exec(new String[] { "su", "-c", "input keyevent 26" });
            } catch (Exception ex) {
            }
        }
    }
"""

lines[11488:11503] = [replace2]

# After this replacement, the length of lines changes because we replace 15 lines with 1 line (which contains the multiline string).
# So next line numbers will be shifted by -14.
# ignoreNextKeyUp was 11685 - 11689. Now it's 11671 - 11675
replace4 = """        if (ignoreNextKeyUp) {
            ignoreNextKeyUp = false; // 플래그 초기화
            return true; // 🚀 이벤트를 여기서 무시하여 아래의 handleCenterShortClick() 이 호출되지 않게 합니다!
        }
"""
lines[11670:11675] = [replace4]

# Shifted again by -4.
# MediaBtnReceiver was 12225 - 12241 (17 lines). Now it's 12207 - 12223.
replace5 = """            if (!isScreenOn || (MainActivity.instance != null && MainActivity.instance.isFakeScreenOff)) {
                // 💡 스크린 컨트롤이 켜져있고, 이전/다음 곡(또는 상/하) 버튼이 눌렸다면? (ACTION_DOWN 시)
                if (MainActivity.instance.isScreenOffControlEnabled && action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyCode == 88 ||
                                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT || keyCode == 87 ||
                                keyCode == 21 || keyCode == 22)) {

                    // 화면을 켜지 않고 아래 '3. 재생 제어'로 진입합니다! (백그라운드 제어)

                } else {
                    // 💡 그외 버튼(좌/우)이거나, 스크린 컨트롤 옵션이 꺼져 있다면?
                    // 화면을 깨우고 "화면만" 켭니다!
                    MainActivity.instance.turnOnScreen();
                    lastWakeUpTime = System.currentTimeMillis();
                    if (action == KeyEvent.ACTION_DOWN) {
                        MainActivity.instance.ignoreNextKeyUp = true;
                    }
                    return; // 🚀 여기서 동작 종료!
                }
            }
"""
lines[12206:12223] = [replace5]

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('Done!')
