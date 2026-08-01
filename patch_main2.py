import sys
import re

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("private BatteryIconView batteryIconView;", "public BatteryIconView batteryIconView;")

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("batteryIconView made public")
