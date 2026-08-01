import sys
import re

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add permanentWakeLock declaration
content = re.sub(
    r'(public class MainActivity extends Activity \{)',
    r'\1\n    private android.os.PowerManager.WakeLock permanentWakeLock = null;',
    content,
    count=1
)

# Add lock acquisition in onCreate
on_create_pattern = r'(protected void onCreate\(Bundle savedInstanceState\) \{\s*try \{\s*Security\.insertProviderAt\(Conscrypt\.newProvider\(\), 1\);\s*\} catch \(Throwable e\) \{\s*e\.printStackTrace\(\);\s*\}\s*super\.onCreate\(savedInstanceState\);)'

on_create_replacement = r'''\1
        // 전원 버튼이 없는 기기를 위해 앱 전체 생명주기 동안 CPU가 절대 잠들지 못하게 하는 영구 WakeLock
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
            permanentWakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Y1:PermanentWakeLock");
            permanentWakeLock.acquire();
        } catch (Exception e) {}'''

content = re.sub(on_create_pattern, on_create_replacement, content, count=1)

# Add lock release in onDestroy
on_destroy_pattern = r'(protected void onDestroy\(\) \{\s*super\.onDestroy\(\);)'
on_destroy_replacement = r'''\1
        try {
            if (permanentWakeLock != null && permanentWakeLock.isHeld()) {
                permanentWakeLock.release();
            }
        } catch (Exception e) {}'''
content = re.sub(on_destroy_pattern, on_destroy_replacement, content, count=1)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Patch applied.")
