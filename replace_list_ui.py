import sys

file_path = r'c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\java\com\themoon\y1\MainActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target = '''            for (final File video : videoFiles) {
                View b = createListButtonWithIcon("\\uE04B", video.getName(), 0xFF00FFFF);

                // ?? 비디???용: ?쪽 글???이?TextView)??빼버리고, ?네???진(ImageView)?로 교체 조립!
                LinearLayout row = (LinearLayout) b;
                TextView tvIcon = (TextView) row.getChildAt(0);
                float d = getResources().getDisplayMetrics().density;
                int iconSize = (int) (40 * d);

                ImageView ivThumb = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(iconSize, iconSize);'''

replacement = '''            for (final File video : videoFiles) {
                View b = createListButtonWithIcon("\\uE04B", video.getName());

                // Thumbnail replacement
                LinearLayout row = (LinearLayout) b;
                TextView tvIcon = (TextView) row.getChildAt(0);
                float d = getResources().getDisplayMetrics().density;
                int iconWidth = (int) (80 * d);
                int iconHeight = (int) (50 * d);

                ImageView ivThumb = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(iconWidth, iconHeight);'''

# Simple regex or string replace to handle encoding quirks safely
import re
pattern = re.compile(r'View b = createListButtonWithIcon\("\\uE04B", video.getName\(\), 0xFF00FFFF\);.*?int iconSize = \(int\) \(40 \* d\);.*?LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams\(iconSize, iconSize\);', re.DOTALL)

new_content = pattern.sub(r'''View b = createListButtonWithIcon("\\uE04B", video.getName());
                
                LinearLayout row = (LinearLayout) b;
                TextView tvIcon = (TextView) row.getChildAt(0);
                float d = getResources().getDisplayMetrics().density;
                int iconWidth = (int) (80 * d);
                int iconHeight = (int) (50 * d);

                ImageView ivThumb = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(iconWidth, iconHeight);''', content)


with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)
    
print("Done!")
