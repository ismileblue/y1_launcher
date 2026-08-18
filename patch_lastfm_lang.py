import json
import os

translations = {
    "Last.fm Login Guide": {
        "english": "Last.fm Login Guide",
        "korean": "Last.fm 로그인 안내",
        "japanese": "Last.fm ログイン案内",
        "chinese": "Last.fm 登录指南",
        "spanish": "Guía de inicio de sesión de Last.fm",
        "russian": "Вход в Last.fm"
    },
    "Please start Wireless PC Upload (Web Server) and log in to Last.fm from your PC or smartphone browser.": {
        "english": "Please start Wireless PC Upload (Web Server) and log in to Last.fm from your PC or smartphone browser.",
        "korean": "무선 PC 업로드(웹 서버)를 실행한 후, PC 또는 스마트폰 브라우저에서 Last.fm에 로그인해 주세요.",
        "japanese": "ワイヤレスPCアップロード（Webサーバー）を起動し、PCまたはスマートフォンのブラウザからLast.fmにログインしてください。",
        "chinese": "请启动无线PC上传（Web服务器），并在电脑或手机浏览器中登录Last.fm。",
        "spanish": "Inicie la carga inalámbrica para PC (servidor web) e inicie sesión en Last.fm desde el navegador de su PC o teléfono inteligente.",
        "russian": "Запустите беспроводную загрузку (веб-сервер) и войдите в Last.fm через браузер ПК или смартфона."
    },
    "Start Web Server": {
        "english": "Start Web Server",
        "korean": "웹 서버 실행",
        "japanese": "Webサーバー起動",
        "chinese": "启动Web服务器",
        "spanish": "Iniciar servidor web",
        "russian": "Запустить веб-сервер"
    },
    "Last.fm Logout": {
        "english": "Last.fm Logout",
        "korean": "Last.fm 로그아웃",
        "japanese": "Last.fm ログアウト",
        "chinese": "Last.fm 退出登录",
        "spanish": "Cerrar sesión de Last.fm",
        "russian": "Выйти из Last.fm"
    },
    "Are you sure you want to log out from Last.fm?": {
        "english": "Are you sure you want to log out from Last.fm?",
        "korean": "Last.fm 계정에서 로그아웃하시겠습니까?",
        "japanese": "Last.fmアカウントからログアウトしますか？",
        "chinese": "确定要退出Last.fm账号吗？",
        "spanish": "¿Seguro que desea cerrar sesión en Last.fm?",
        "russian": "Вы действительно хотите выйти из Last.fm?"
    },
    "Logout": {
        "english": "Logout",
        "korean": "로그아웃",
        "japanese": "ログアウト",
        "chinese": "退出登录",
        "spanish": "Cerrar sesión",
        "russian": "Выйти"
    },
    "Logged out": {
        "english": "Logged out",
        "korean": "로그아웃되었습니다.",
        "japanese": "ログアウトしました",
        "chinese": "已退出登录",
        "spanish": "Sesión cerrada",
        "russian": "Выход выполнен"
    },
    "Not logged in": {
        "english": "Not logged in",
        "korean": "로그인 안 됨",
        "japanese": "未ログイン",
        "chinese": "未登录",
        "spanish": "No conectado",
        "russian": "Не выполнен вход"
    }
}

base_dir = r"c:\Users\blue\Documents\Flutter_project\Y1\app\src\main\assets\languages"

for fname in os.listdir(base_dir):
    if not fname.endswith('.json'):
        continue
    lang_key = fname.split('.')[0].lower()
    filepath = os.path.join(base_dir, fname)
    
    with open(filepath, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except Exception as e:
            print(f"Error loading {fname}: {e}")
            continue
            
    added = 0
    for key, lang_map in translations.items():
        if lang_key in lang_map:
            data[key] = lang_map[lang_key]
            added += 1
            
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
        
    print(f"Updated {fname} with {added} translations.")

print("All language files successfully updated.")
