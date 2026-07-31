import sys
import re

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove permanentWakeLock variable
content = re.sub(r'private android\.os\.PowerManager\.WakeLock permanentWakeLock = null;\s*', '', content)

# Remove permanentWakeLock.acquire() in onCreate
content = re.sub(r'// 전원 버튼이 없는 기기를 위해 앱 전체 생명주기 동안 CPU가 절대 잠들지 못하게 하는 영구 WakeLock\s*try \{\s*android\.os\.PowerManager pm = \(android\.os\.PowerManager\) getSystemService\(android\.content\.Context\.POWER_SERVICE\);\s*permanentWakeLock = pm\.newWakeLock\(android\.os\.PowerManager\.PARTIAL_WAKE_LOCK, "Y1:PermanentWakeLock"\);\s*permanentWakeLock\.acquire\(\);\s*\} catch \(Exception e\) \{\}', '', content)

# Remove permanentWakeLock.release() in onDestroy
content = re.sub(r'try \{\s*if \(permanentWakeLock != null && permanentWakeLock\.isHeld\(\)\) \{\s*permanentWakeLock\.release\(\);\s*\}\s*\} catch \(Exception e\) \{\}', '', content)

# Add wakeUp to MediaBtnReceiver
# Find:
# MainActivity.instance.turnOnScreen();
# lastWakeUpTime = System.currentTimeMillis();
target = r'MainActivity\.instance\.turnOnScreen\(\);\s*lastWakeUpTime = System\.currentTimeMillis\(\);'
replacement = r'''MainActivity.instance.turnOnScreen();
                    lastWakeUpTime = System.currentTimeMillis();
                    // 🚀 [절전 모드 해제] AutoShutdownReceiver에서 발견한 시스템 API로 물리 화면 강제 기상!
                    try {
                        pm.getClass().getMethod("wakeUp", Long.TYPE).invoke(pm, Long.valueOf(android.os.SystemClock.uptimeMillis()));
                    } catch (Exception e) {}'''
if 'wakeUp' not in content[content.find('MediaBtnReceiver'):]:
    content = re.sub(target, replacement, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainActivity patched successfully.")
