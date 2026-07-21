import codecs

with codecs.open(r'C:\Users\blue\.gemini\antigravity-ide\brain\2753e663-6468-4a6f-8093-b33150b03c7d\.system_generated\logs\transcript_full.jsonl', 'r', 'utf-8') as f:
    text = f.read()

idx = text.find('class SettingsMenuManager')
if idx != -1:
    # search backwards for python script start
    start = text.rfind('python -c', 0, idx)
    if start != -1:
        end = text.find('"}', idx)
        print('Found python script length:', end - start)
        with codecs.open('extract_script.txt', 'w', 'utf-8') as fw:
            fw.write(text[start:end])
        print('Wrote to extract_script.txt')
