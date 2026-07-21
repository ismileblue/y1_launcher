import codecs

with codecs.open('app/src/main/java/com/themoon/y1/managers/SettingsMenuManager.java', 'r', 'utf-8') as f:
    content = f.read()

# 1. Replace createCategoryHeader with updateSettingsTitle
content = content.replace('createCategoryHeader("━ " + t("Audio & Playback") + " ━");', 'updateSettingsTitle(t("Audio & Playback"));')
content = content.replace('createCategoryHeader("━ " + t("Display & Personalization") + " ━");', 'updateSettingsTitle(t("Display & Theme"));')
content = content.replace('createCategoryHeader("━ " + t("Control & Feedback") + " ━");', 'updateSettingsTitle(t("Control & Feedback"));')
content = content.replace('createCategoryHeader("━ " + t("Network & Connections") + " ━");', 'updateSettingsTitle(t("Network & Connections"));')
content = content.replace('createCategoryHeader("━ " + t("Data & Storage") + " ━");', 'updateSettingsTitle(t("Data & Storage"));')
content = content.replace('createCategoryHeader("━ " + t("System") + " ━");', 'updateSettingsTitle(t("System"));')

# 2. Add updateSettingsTitle(null)
target_null = '''        ViewGroup settingsGroup = (ViewGroup) main.layoutSettingsMode;
        if (settingsGroup != null && settingsGroup.getChildCount() > 0 && settingsGroup.getChildAt(0) instanceof TextView) {
            settingsGroup.getChildAt(0).setVisibility(View.VISIBLE);
        }'''
content = content.replace(target_null, '        updateSettingsTitle(null);')

# 3, 4, 5, 9. Replace createCategoryHeader method with new methods
header_func = '''    public void createCategoryHeader(String title) {
        TextView header = new TextView(main);
        header.setText(title);
        header.setTextColor(ThemeManager.getTextColorSecondary());
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 8);
        header.setLayoutParams(lp);

        main.containerSettingsItems.addView(header);
    }'''

new_methods = '''    public void restoreSubMenu() {
        switch (lastSettingsFocusIndex) {
            case 0: buildAudioSettingsUI(); break;
            case 1: buildDisplaySettingsUI(); break;
            case 2: buildControlSettingsUI(); break;
            case 3: buildNetworkSettingsUI(); break;
            case 4: buildDataSettingsUI(); break;
            case 5: buildSystemSettingsUI(); break;
            default: buildSettingsUI(); break;
        }
    }

    public void updateSettingsTitle(String subCategory) {
        ViewGroup settingsGroup = (ViewGroup) main.layoutSettingsMode;
        if (settingsGroup != null && settingsGroup.getChildCount() > 0 && settingsGroup.getChildAt(0) instanceof TextView) {
            TextView header = (TextView) settingsGroup.getChildAt(0);
            header.setVisibility(View.VISIBLE);
            if (subCategory == null) {
                header.setText(t("Settings"));
            } else {
                header.setText(t("Settings") + " > " + subCategory);
            }
        }
    }

    public void focusFirstItem() {
        main.containerSettingsItems.post(new Runnable() {
            @Override
            public void run() {
                if (main.containerSettingsItems.getChildCount() > 0) {
                    android.view.ViewParent parent = main.containerSettingsItems.getParent();
                    if (parent instanceof android.widget.ScrollView) {
                        ((android.widget.ScrollView) parent).scrollTo(0, 0);
                    }
                    main.containerSettingsItems.getChildAt(0).requestFocus();
                }
            }
        });
    }'''
content = content.replace(header_func, new_methods)

# 6. Change font size
content = content.replace('tvLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);', 'tvLeft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);')

# 7. Rename Display & Personalization everywhere
content = content.replace('Display & Personalization', 'Display & Theme')

# 10. Add sound effects disabled
target_sound = '''        row.setClickable(true);
        row.setFocusable(true);'''
replacement_sound = '''        row.setClickable(true);
        row.setFocusable(true);
        row.setSoundEffectsEnabled(false);'''
content = content.replace(target_sound, replacement_sound)


def insert_before_closing_brace_of_methods(code, method_names, insertion):
    lines = code.split('\\n')
    new_lines = []
    current_method = None
    brace_level = 0
    
    for line in lines:
        if current_method is None:
            for m in method_names:
                if 'public void ' + m + '()' in line:
                    current_method = m
                    brace_level = 0
                    break
        
        if current_method:
            brace_level += line.count('{')
            brace_level -= line.count('}')
            
            if brace_level == 0 and '}' in line:
                new_lines.append(insertion)
                current_method = None
                
        new_lines.append(line)
        
    return '\\n'.join(new_lines)

builders = [
    'buildAudioSettingsUI',
    'buildDisplaySettingsUI',
    'buildControlSettingsUI',
    'buildNetworkSettingsUI',
    'buildDataSettingsUI',
    'buildSystemSettingsUI'
]
content = insert_before_closing_brace_of_methods(content, builders, '        focusFirstItem();')


with codecs.open('app/src/main/java/com/themoon/y1/managers/SettingsMenuManager.java', 'w', 'utf-8') as f:
    f.write(content)
print('Done!')
