import codecs

with codecs.open('app/src/main/java/com/themoon/y1/MainActivity.java', 'r', 'utf-8') as f:
    text = f.read()

start_idx = text.find('private void buildSettingsUI() {')
if start_idx == -1:
    print('Start not found')
    exit(1)

# Find the end of buildSettingsUI by matching braces
brace_count = 0
in_string = False
end_idx = -1

for i in range(start_idx + text[start_idx:].find('{'), len(text)):
    c = text[i]
    if c == '"' and text[i-1] != '\\':
        in_string = not in_string
    if not in_string:
        if c == '{':
            brace_count += 1
        elif c == '}':
            brace_count -= 1
            if brace_count == 0:
                end_idx = i + 1
                break

if end_idx != -1:
    old_method = text[start_idx:end_idx]
    print('Found method of length:', len(old_method))
    
    new_method = '''public void buildSettingsUI() {
        currentSettingsDepth = 0; // Depth 0 for main categorized menu
        isRadioUIShowing = false;
        isRadioSettingsMode = false;
        com.themoon.y1.managers.SettingsMenuManager.getInstance(MainActivity.this).buildSettingsUI();
    }'''
    
    new_text = text[:start_idx] + new_method + text[end_idx:]
    with codecs.open('app/src/main/java/com/themoon/y1/MainActivity.java', 'w', 'utf-8') as f:
        f.write(new_text)
    print('Successfully replaced buildSettingsUI()!')
else:
    print('End not found')
