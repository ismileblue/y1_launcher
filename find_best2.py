import codecs

with codecs.open(r'C:\Users\blue\.gemini\antigravity-ide\brain\2753e663-6468-4a6f-8093-b33150b03c7d\.system_generated\logs\transcript_full.jsonl', 'r', 'utf-8') as f:
    text = f.read()

# Replace escaped newlines so we can parse it as normal code
unescaped = text.replace('\\n', '\n').replace('\\"', '"').replace('\\\\', '\\')

start_idx = 0
found = []
while True:
    idx = unescaped.find('package com.themoon.y1.managers;\n', start_idx)
    if idx == -1: break
    
    # Extract the block
    # Start counting braces from idx
    brace_count = 0
    in_class = False
    
    for i in range(idx, len(unescaped)):
        if unescaped[i] == '{':
            brace_count += 1
            in_class = True
        elif unescaped[i] == '}':
            brace_count -= 1
            if in_class and brace_count == 0:
                found.append(unescaped[idx:i+1])
                break
                
    start_idx = idx + 10

if found:
    # Get the largest one, that's probably the most complete version before truncation
    best = max(found, key=len)
    with codecs.open('SettingsMenuManager_best.java', 'w', 'utf-8') as fw:
        fw.write(best)
    print('Found best length:', len(best))
else:
    print('Not found')
