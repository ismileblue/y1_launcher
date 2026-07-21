import codecs

with codecs.open('app/src/main/res/layout/activity_main.xml', 'r', 'utf-8') as f:
    text = f.read()

start = text.find('android:id="@+id/layout_storage_mode"')
end = text.find('>', start)
section = text[start:end]

if 'android:paddingTop="36dp"' in section:
    new_section = section.replace('android:paddingTop="36dp"', '')
    text = text[:start] + new_section + text[end:]
    with codecs.open('app/src/main/res/layout/activity_main.xml', 'w', 'utf-8') as fw:
        fw.write(text)
    print('Padding removed!')
else:
    print('Padding not found in layout_storage_mode')
    print(section)
