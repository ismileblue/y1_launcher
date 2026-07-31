import sys
import re

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace hardcoded white text color for frequency text in buildGraphicEqualizerUI
pattern = r'(tvFreq\.setTextColor\()0xFFFFFFFF(\);)'
replacement = r'\1ThemeManager.getTextColorPrimary()\2'
content = re.sub(pattern, replacement, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainActivity patched.")
