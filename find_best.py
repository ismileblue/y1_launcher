import codecs

with codecs.open(r'C:\Users\blue\.gemini\antigravity-ide\brain\2753e663-6468-4a6f-8093-b33150b03c7d\.system_generated\logs\transcript_full.jsonl', 'r', 'utf-8') as f:
    text = f.read()

start_idx = 0
found = []
while True:
    idx = text.find('package com.themoon.y1.managers;', start_idx)
    if idx == -1: break
    
    end_idx = text.find('}\n}', idx)
    if end_idx != -1:
        found.append(text[idx:end_idx+3])
    else:
        # try escaping
        end_idx = text.find('}\\n}', idx)
        if end_idx != -1:
            found.append(text[idx:end_idx+4])
            
    start_idx = idx + 10

best = ''
for f in found:
    unescaped = f.replace('\\n', '\n').replace('\\"', '"').replace('\\\\', '\\')
    if len(unescaped) > len(best):
        best = unescaped

print('Found best length:', len(best))
with codecs.open('SettingsMenuManager_best.java', 'w', 'utf-8') as fw:
    fw.write(best)
