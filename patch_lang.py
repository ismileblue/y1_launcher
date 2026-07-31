import json
import os

langs = {
    "English.json": {
        "Battery Indicator": "Battery Indicator",
        "Icon Only": "Icon Only",
        "Percent Only": "Percent Only",
        "Icon + Percent": "Icon + Percent"
    },
    "Korean.json": {
        "Battery Indicator": "배터리 표시",
        "Icon Only": "그림만",
        "Percent Only": "숫자만",
        "Icon + Percent": "그림 + 숫자"
    },
    "Japanese.json": {
        "Battery Indicator": "バッテリー表示",
        "Icon Only": "アイコンのみ",
        "Percent Only": "パーセントのみ",
        "Icon + Percent": "アイコン + パーセント"
    },
    "Chinese.json": {
        "Battery Indicator": "电池显示",
        "Icon Only": "仅图标",
        "Percent Only": "仅百分比",
        "Icon + Percent": "图标 + 百分比"
    },
    "Spanish.json": {
        "Battery Indicator": "Indicador de batería",
        "Icon Only": "Solo icono",
        "Percent Only": "Solo porcentaje",
        "Icon + Percent": "Icono + Porcentaje"
    },
    "Russian.json": {
        "Battery Indicator": "Индикатор батареи",
        "Icon Only": "Только значок",
        "Percent Only": "Только процент",
        "Icon + Percent": "Значок + Процент"
    }
}

base_dir = r"c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\assets\languages"

for filename, trans in langs.items():
    filepath = os.path.join(base_dir, filename)
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            try:
                data = json.load(f)
            except:
                data = {}
        
        for k, v in trans.items():
            if k not in data:
                data[k] = v
                
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"Updated {filename}")

print("All language files patched.")
