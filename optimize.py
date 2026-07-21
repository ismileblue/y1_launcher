import codecs

with codecs.open('app/src/main/res/layout/activity_main.xml', 'r', 'utf-8') as f:
    text = f.read()

start = text.find('android:id="@+id/layout_storage_mode"')
end = text.find('</LinearLayout>', start)
section = text[start:end]

# 1. Title margin bottom 30dp -> 10dp, textSize 20sp -> 16sp
section = section.replace('android:layout_marginBottom="30dp"', 'android:layout_marginBottom="10dp"')
section = section.replace('text="STORAGE INFO" android:textColor="#BBFFFFFF" android:textSize="20sp"', 'text="STORAGE INFO" android:textColor="#BBFFFFFF" android:textSize="16sp"')

# 2. Details text size 18sp -> 14sp
section = section.replace('android:id="@+id/tv_storage_details" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Used: 0MB / Total: 0MB" android:textColor="#FFFFFF" android:textSize="18sp"', 'android:id="@+id/tv_storage_details" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Used: 0MB / Total: 0MB" android:textColor="#FFFFFF" android:textSize="14sp"')

# 3. Back instruction margin top 30dp -> 10dp, textSize 14sp -> 12sp
section = section.replace('android:text="Press [Back] to return." android:textColor="#888888" android:textSize="14sp" android:layout_marginTop="30dp"', 'android:text="Press [Back] to return." android:textColor="#888888" android:textSize="12sp" android:layout_marginTop="10dp"')

text = text[:start] + section + text[end:]

with codecs.open('app/src/main/res/layout/activity_main.xml', 'w', 'utf-8') as fw:
    fw.write(text)
print('Layout optimized!')
