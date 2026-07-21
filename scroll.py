import codecs
import re

with codecs.open('app/src/main/res/layout/activity_main.xml', 'r', 'utf-8') as f:
    text = f.read()

start = text.find('android:id="@+id/container_settings_items"')
if start != -1:
    print(text[start-200:start+100])
else:
    print('Not found')
