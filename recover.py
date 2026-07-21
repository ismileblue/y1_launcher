import codecs
import json

with codecs.open('found_lines.txt', 'r', 'utf-8') as f:
    text = f.read()

try:
    args = json.loads(text)
    cmd = args.get('CommandLine', '')
    if 'codecs.open' in cmd:
        start = cmd.find('"""package')
        end = cmd.find('"""', start + 3)
        if start != -1:
            java_code = cmd[start+3:end]
            with codecs.open('app/src/main/java/com/themoon/y1/managers/SettingsMenuManager.java', 'w', 'utf-8') as fw:
                fw.write(java_code)
            print('Recovered successfully from CommandLine!')
            exit(0)
except Exception as e:
    print(e)

print('Not a JSON string or CommandLine not found. Check first 500 chars:')
print(text[:500])
