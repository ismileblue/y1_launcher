package com.themoon.y1;

import android.content.Context;
import android.net.wifi.WifiManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Locale;

public class Y1WebServer extends Thread {
    private ServerSocket serverSocket;
    private boolean running = true;
    private File rootFolder;
    private Context context;

    public Y1WebServer(Context context, File originalRootFolder) {
        this.context = context;
        // Y1: /storage/sdcard0. Y2 with readable SD: /storage (sdcard0 + sdcard1).
        this.rootFolder = StoragePaths.getWebServerRoot();
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(8080);
            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(new RequestHandler(socket)).start();
            }
        } catch (Exception e) {}
    }

    public void stopServer() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch(Exception e){}
    }

    public String getLocalIpAddress() {
        try {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wm.getConnectionInfo().getIpAddress();
            return String.format(Locale.US, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        } catch (Exception ex) { return "Unknown IP"; }
    }

    private void deleteFileOrFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileOrFolder(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    private class RequestHandler implements Runnable {
        private Socket socket;
        public RequestHandler(Socket socket) { this.socket = socket; }

        private String readHeaderLine(InputStream is) throws java.io.IOException {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = is.read()) != -1) {
                if (c == '\r') continue;
                if (c == '\n') break;
                sb.append((char) c);
            }
            return sb.toString();
        }

        public void run() {
            try {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                String requestLine = readHeaderLine(is);
                if (requestLine == null || requestLine.isEmpty()) return;

                String[] parts = requestLine.split(" ");
                String method = parts[0];
                String path = parts[1];

                int contentLength = 0;
                String line;
                while (!(line = readHeaderLine(is)).isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }

                // 1️⃣ 화면 UI 전송 (프론트엔드 - 인라인 플레이어 + 🚀 텍스트 에디터 탑재 + 🚀 드래그 앤 드롭 지원)
                if (method.equals("GET") && path.equals("/")) {
                    String html = "<!DOCTYPE html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'>" +
                            "<title>Y1 File Manager</title><style>" +
                            // 🚀 [디자인 수정] 바탕색 및 기본 글꼴 (Material Dark Theme 기반)
                            "body{font-family:'Roboto', 'Segoe UI', sans-serif; background:#1E1E24; color:#E0E0E0; padding:20px; text-align:center; max-width:800px; margin:0 auto; padding-bottom:120px;} " +
                            "input, select, button{font-size:14px; padding:10px 16px; margin:5px; border-radius:20px; border:none; outline:none; transition:0.2s;} " +
                            "input[type=text]{width:calc(100% - 120px); background:#2A2A35; color:#E0E0E0; border-radius:12px; padding:12px;} " +

                            // 🚀 [디자인 수정] 버튼 색상 (보라색 포인트 & 모던 톤)
                            "button{background:#B39DDB; color:#121212; font-weight:600; cursor:pointer;} " + // 기본 버튼 (연한 보라)
                            "button:hover{background:#9575CD;} " +
                            "button.danger{background:#424250; color:#E57373;} " + // 삭제/취소 버튼 (어두운 회색 바탕, 빨간 글씨)
                            "button.danger:hover{background:#EF5350; color:#fff;} " +
                            "button.action{background:#383848; color:#B39DDB;} " + // 서브 액션 버튼 (어두운 바탕, 보라색 글씨)
                            "button.action:hover{background:#454555;} " +

                            // 🚀 [디자인 수정] 박스 및 리스트 아이템
                            ".box{background:#252530; padding:20px; border-radius:16px; margin:15px 0; text-align:left; box-shadow:0 4px 6px rgba(0,0,0,0.3);} " +
                            ".item{display:flex; justify-content:space-between; align-items:center; padding:12px; border-bottom:1px solid #333340; cursor:pointer; transition:0.2s;} " +
                            ".item:last-child{border-bottom:none;} " +
                            ".item:hover{background:#2D2D3A; border-radius:8px;} " +
                            ".item-left{display:flex; align-items:center; flex-grow:1; overflow:hidden; gap:12px;} " +
                            ".item-name{white-space:nowrap; overflow:hidden; text-overflow:ellipsis; font-weight:500;} " +
                            ".thumb{width:40px; height:40px; object-fit:cover; border-radius:8px; background:#1A1A20;} " +
                            ".icon{font-size:22px; width:40px; text-align:center; color:#B39DDB;} " + // 아이콘도 연보라 톤으로 통일
                            ".btn-group{display:flex; gap:6px;} " +

                            // 오디오 플레이어
                            "#audioBox{position:fixed; bottom:0; left:0; right:0; background:#252530; border-top:1px solid #454555; padding:15px; display:none; z-index:100; box-shadow:0 -2px 10px rgba(0,0,0,0.5);} " +
                            "audio{width:100%; max-width:800px; margin:0 auto; display:block; outline:none; border-radius:24px;} " +

                            // 드래그 앤 드롭 애니메이션
                            "#uploadBox{transition:0.3s; border:2px dashed #454555;} " +
                            "#uploadBox.dragover{background:#2D2D3A; border:2px dashed #B39DDB;} " +

                            // 텍스트 에디터 모달
                            "#editorBox{position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(18,18,22,0.95); z-index:200; padding:20px; box-sizing:border-box; display:none;} " +
                            "#editorArea{width:100%; height:calc(100% - 120px); background:#1E1E24; color:#E0E0E0; font-family:'Courier New', monospace; font-size:15px; border:1px solid #454555; border-radius:12px; padding:15px; resize:none; box-shadow:inset 0 2px 5px rgba(0,0,0,0.3);} " +
                            "#editorTitle{color:#B39DDB; margin-top:0; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; font-weight:600;}" +
                            "</style></head><body>" +

                            "<h2 style='color:#E0E0E0; font-weight:600; letter-spacing:0.5px;'>📁 Y1 File Manager</h2>" +

                            // 업로드/폴더생성 박스
                            "<div class='box' id='uploadBox'>" +
                            "<div style='font-size:16px; margin-bottom:12px; font-weight:500; color:#B39DDB;'>📍 <span id='currentPathText'>/</span></div>" +
                            "<div style='text-align:center; color:#9E9E9E; font-size:13px; margin-bottom:20px; padding-bottom:15px; border-bottom:1px solid #333340;'>💡 <b>Drag & Drop</b> files anywhere in this box to upload instantly!</div>" +
                            "<div style='display:flex; gap:8px; margin-bottom:15px;'>" +
                            "<input type='text' id='fName' placeholder='New folder name...'>" +
                            "<button onclick='createFolder()'>Create</button></div>" +
                            "<div style='display:flex; gap:8px; align-items:center;'>" +
                            "<input type='file' id='fInput' multiple accept='*/*' style='flex-grow:1; color:#9E9E9E;'>" +
                            "<button onclick='uploadAll()' class='action'>Upload Here</button></div>" +
                            "<div id='status' style='margin-top:12px; color:#81C784; font-weight:600; font-size:14px;'></div>" +
                            "</div>" +

                            // 파일 리스트 박스
                            "<div class='box' id='fileList'>Loading...</div>" +

                            // 플로팅 오디오 플레이어
                            "<div id='audioBox'>" +
                            "<div id='audioTitle' style='max-width:800px; margin:0 auto 12px; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; color:#B39DDB;'></div>" +
                            "<audio id='audioPlayer' controls controlsList='nodownload'></audio>" +
                            "</div>" +

                            // 전체화면 텍스트 에디터
                            "<div id='editorBox'>" +
                            "<h3 id='editorTitle'>📝 Edit File</h3>" +
                            "<textarea id='editorArea' spellcheck='false' wrap='off'></textarea>" +
                            "<div style='display:flex; gap:12px; margin-top:15px;'>" +
                            "<button class='action' style='flex:1; background:#B39DDB; color:#121212;' onclick='saveFile()'>💾 Save Settings</button>" +
                            "<button class='danger' style='flex:1;' onclick='closeEditor()'>Cancel</button>" +
                            "</div></div>" +

                            "<script>" +
                            "let currentPath = '';" +
                            "function loadList() {" +
                            "  fetch('/api/list?dir=' + encodeURIComponent(currentPath)).then(r=>r.json()).then(data => {" +
                            "    document.getElementById('currentPathText').innerText = '/' + currentPath;" +
                            "    let html = '';" +
                            "    if(currentPath !== '') html += `<div class='item' onclick='goUp()'><div class='item-left'><div class='icon'>🔙</div><b style='color:#B39DDB;'>[Go Back]</b></div></div>`;" +
                            "    data.forEach(f => {" +
                            "      let ext = f.name.split('.').pop().toLowerCase();" +
                            "      let isImg = ['jpg','jpeg','png','webp','gif'].includes(ext);" +
                            "      let isAudio = ['mp3','flac','wav','ogg','m4a','aac'].includes(ext);" +
                            "      let isText = ['json','txt','xml','ini','md','m3u','m3u8','eq'].includes(ext);" +
                            "      let fullPath = currentPath ? currentPath + '/' + f.name : f.name;" +
                            "      let safePath = encodeURIComponent(fullPath);" +

                            "      let iconHtml = f.isDir ? `<div class='icon'>📁</div>` : " +
                            "                     isImg ? `<img src='/api/file?path=${safePath}' class='thumb' loading='lazy'>` : " +
                            "                     isAudio ? `<div class='icon'>🎵</div>` : " +
                            "                     isText ? `<div class='icon'>📝</div>` : `<div class='icon'>📄</div>`;" +

                            "      let rowAction = f.isDir ? `onclick=\"goInto('${f.name.replace(/'/g, \"\\\\'\")}')\"` : " +
                            "                      isAudio ? `onclick=\"playAudio('${safePath}', '${f.name.replace(/'/g, \"\\\\'\")}')\"` : " +
                            "                      isText ? `onclick=\"openEditor(event, '${safePath}', '${f.name.replace(/'/g, \"\\\\'\")}')\"` : " +
                            "                      isImg ? `onclick=\"window.open('/api/file?path=${safePath}', '_blank')\"` : '';" +

                            // (기존 코드)
                            "      let editBtn = isText ? `<button class='action' onclick=\"openEditor(event, '${safePath}', '${f.name.replace(/'/g, \"\\\\'\")}')\">Edit</button>` : '';" +
                            "      let renameBtn = `<button class='action' onclick=\"renameItem(event, '${f.name.replace(/'/g, \"\\\\'\")}')\">Rename</button>`;" +

                            // 🚀 [여기서부터 수정!] 다운로드 버튼 변수를 새로 만들고, html += 조립 부분에 ${downloadBtn}을 끼워 넣습니다.
                            "      let downloadBtn = f.isDir ? '' : `<button class='action' style='background:#81C784; color:#121212;' onclick=\"event.stopPropagation(); window.location.href='/api/download?path=${safePath}';\">⬇️ Down</button>`;" +

                            "      html += `<div class='item' ${rowAction}>` +" +
                            "              `<div class='item-left'>${iconHtml}<span class='item-name'>${f.name}</span></div>` +" +
                            "              `<div class='btn-group'>${downloadBtn}${editBtn}${renameBtn}<button class='danger' onclick=\"deleteItem(event, '${f.name.replace(/'/g, \"\\\\'\")}')\">Delete</button></div>` +" +
                            "              `</div>`;" +
                            "    });" +
                            "    if(data.length===0 && currentPath === '') html += '<div style=\"padding:15px; color:#9E9E9E;\">No files found.</div>';" +
                            "    document.getElementById('fileList').innerHTML = html;" +
                            "  });" +
                            "}" +
                            "function goInto(dirName) { currentPath = currentPath ? currentPath + '/' + dirName : dirName; loadList(); }" +
                            "function goUp() { let parts = currentPath.split('/'); parts.pop(); currentPath = parts.join('/'); loadList(); }" +

                            "function playAudio(path, name) {" +
                            "  document.getElementById('audioBox').style.display = 'block';" +
                            "  document.getElementById('audioTitle').innerText = '▶ ' + name;" +
                            "  let player = document.getElementById('audioPlayer');" +
                            "  player.src = '/api/file?path=' + path;" +
                            "  player.play();" +
                            "}" +

                            "let editingPath = '';" +
                            "function openEditor(e, path, name) {" +
                            "  if(e) e.stopPropagation();" +
                            "  editingPath = path;" +
                            "  document.getElementById('editorTitle').innerText = '📝 Editing: ' + name;" +
                            "  document.getElementById('editorArea').value = 'Loading content...';" +
                            "  document.getElementById('editorBox').style.display = 'block';" +
                            "  fetch('/api/file?path=' + path).then(r => r.text()).then(txt => {" +
                            "    document.getElementById('editorArea').value = txt;" +
                            "  });" +
                            "}" +
                            "function closeEditor() { document.getElementById('editorBox').style.display = 'none'; editingPath=''; }" +
                            "function saveFile() {" +
                            "  let content = document.getElementById('editorArea').value;" +
                            "  fetch('/api/save?path=' + editingPath, { method: 'POST', body: content }).then(() => {" +
                            "    alert('✅ File saved successfully!'); closeEditor();" +
                            "  }).catch(e => alert('Failed to save.'));" +
                            "}" +

                            "function createFolder() { " +
                            "  var n = document.getElementById('fName').value; if(!n) return;" +
                            "  fetch('/api/create?dir=' + encodeURIComponent(currentPath) + '&name=' + encodeURIComponent(n), {method:'POST'}).then(()=>{ document.getElementById('fName').value=''; loadList();});" +
                            "}" +
                            "function renameItem(e, oldName) { " +
                            "  e.stopPropagation();" +
                            "  var newName = prompt('Enter new name for: ' + oldName, oldName);" +
                            "  if(!newName || newName === oldName) return;" +
                            "  fetch('/api/rename?dir=' + encodeURIComponent(currentPath) + '&old=' + encodeURIComponent(oldName) + '&new=' + encodeURIComponent(newName), {method:'POST'}).then(()=>loadList());" +
                            "}" +
                            "function deleteItem(e, name) { " +
                            "  e.stopPropagation();" +
                            "  if(!confirm('Delete ' + name + '?')) return;" +
                            "  fetch('/api/delete?path=' + encodeURIComponent(currentPath ? currentPath + '/' + name : name), {method:'POST'}).then(()=>loadList());" +
                            "}" +

                            "async function uploadAll(droppedFiles) { " +
                            "  var files = droppedFiles || document.getElementById('fInput').files; var st = document.getElementById('status'); " +
                            "  if(files.length === 0) return;" +
                            "  for(var i=0; i<files.length; i++) { " +
                            "    st.innerText = 'Uploading: ' + files[i].name + ' (' + (i+1) + '/' + files.length + ')'; " +
                            "    await fetch('/api/upload?dir=' + encodeURIComponent(currentPath) + '&name=' + encodeURIComponent(files[i].name), {method:'POST', body:files[i]}); " +
                            "  } " +
                            "  st.innerText = '✅ Upload Complete!'; document.getElementById('fInput').value=''; loadList();" +
                            "}" +

                            "async function uploadFolderItems(fileList) { " +
                            "  var st = document.getElementById('status'); " +
                            "  for(var i=0; i<fileList.length; i++) { " +
                            "    let item = fileList[i]; " +
                            "    let displayPath = item.path ? item.path + item.file.name : item.file.name; " +
                            "    st.innerText = 'Uploading: ' + displayPath + ' (' + (i+1) + '/' + fileList.length + ')'; " +
                            "    let targetDir = currentPath; " +
                            "    if(item.path) { " +
                            "       let subDir = item.path.replace(/\\/$/, ''); " +
                            "       targetDir = currentPath ? currentPath + '/' + subDir : subDir; " +
                            "    } " +
                            "    await fetch('/api/upload?dir=' + encodeURIComponent(targetDir) + '&name=' + encodeURIComponent(item.file.name), {method:'POST', body:item.file}); " +
                            "  } " +
                            "  st.innerText = '✅ Folder Upload Complete!'; loadList();" +
                            "}" +

                            "let dropZone = document.getElementById('uploadBox');" +
                            "dropZone.addEventListener('dragover', function(e) { e.preventDefault(); dropZone.classList.add('dragover'); });" +
                            "dropZone.addEventListener('dragleave', function(e) { e.preventDefault(); dropZone.classList.remove('dragover'); });" +
                            "dropZone.addEventListener('drop', function(e) { " +
                            "  e.preventDefault(); dropZone.classList.remove('dragover'); " +
                            "  let items = e.dataTransfer.items; " +
                            "  if(!items) { if(e.dataTransfer.files.length > 0) uploadAll(e.dataTransfer.files); return; } " +
                            "  " +
                            "  document.getElementById('status').innerText = 'Scanning dropped items...'; " +
                            "  let filesToUpload = []; " +
                            "  let pending = 0; " +
                            "  " +
                            "  function scanEntry(item, path) { " +
                            "    if(item.isFile) { " +
                            "      pending++; " +
                            "      item.file(f => { filesToUpload.push({file: f, path: path}); pending--; checkDone(); }); " +
                            "    } else if(item.isDirectory) { " +
                            "      let dirReader = item.createReader(); " +
                            "      pending++; " +
                            "      dirReader.readEntries(entries => { " +
                            "        entries.forEach(entry => scanEntry(entry, path + item.name + '/')); " +
                            "        pending--; checkDone(); " +
                            "      }); " +
                            "    } " +
                            "  } " +
                            "  function checkDone() { " +
                            "    if(pending === 0) { " +
                            "      if(filesToUpload.length > 0) uploadFolderItems(filesToUpload); " +
                            "      else document.getElementById('status').innerText = 'No files found.'; " +
                            "    } " +
                            "  } " +
                            "  for(let i=0; i<items.length; i++) { " +
                            "    let entry = items[i].webkitGetAsEntry(); " +
                            "    if(entry) scanEntry(entry, ''); " +
                            "  } " +
                            "});" +

                            "window.onload = loadList;" +
                            "</script></body></html>";

                    os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n" + html).getBytes("UTF-8"));
                }
                // 2️⃣ [API] 파일 및 폴더 리스트 응답 (JSON 형식)
                else if (method.equals("GET") && path.startsWith("/api/list")) {
                    String q = path.contains("?") ? path.split("\\?")[1] : "";
                    String dirStr = "";
                    if (q.startsWith("dir=")) dirStr = URLDecoder.decode(q.substring(4), "UTF-8");

                    File targetDir = dirStr.isEmpty() ? rootFolder : new File(rootFolder, dirStr);
                    StringBuilder json = new StringBuilder("[");

                    if (targetDir.exists() && targetDir.isDirectory()) {
                        File[] files = targetDir.listFiles();
                        if (files != null) {
                            for (int i=0; i<2; i++) {
                                for (File f : files) {
                                    boolean isDir = f.isDirectory();
                                    if ((i == 0 && isDir) || (i == 1 && !isDir)) {
                                        if (json.length() > 1) json.append(",");
                                        json.append("{\"name\":\"").append(f.getName().replace("\"", "\\\"")).append("\",\"isDir\":").append(isDir).append("}");
                                    }
                                }
                            }
                        }
                    }
                    json.append("]");
                    os.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" + json.toString()).getBytes("UTF-8"));
                }

                // 3️⃣ [API] 폴더 생성
                else if (method.equals("POST") && path.startsWith("/api/create")) {
                    String q = path.split("\\?")[1];
                    String[] params = q.split("&");
                    String dirStr = "", name = "";
                    for (String p : params) {
                        if (p.startsWith("dir=")) dirStr = URLDecoder.decode(p.substring(4), "UTF-8");
                        if (p.startsWith("name=")) name = URLDecoder.decode(p.substring(5), "UTF-8");
                    }
                    File targetDir = dirStr.isEmpty() ? rootFolder : new File(rootFolder, dirStr);
                    File newDir = new File(targetDir, name);
                    newDir.mkdirs();
                    os.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes("UTF-8"));
                }

                // 4️⃣ [API] 파일 및 폴더 삭제
                else if (method.equals("POST") && path.startsWith("/api/delete")) {
                    String q = path.split("\\?")[1];
                    String targetPath = URLDecoder.decode(q.substring(5), "UTF-8");
                    File targetFile = new File(rootFolder, targetPath);
                    if (targetFile.exists()) {
                        deleteFileOrFolder(targetFile);
                    }
                    os.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes("UTF-8"));
                }

                // 🚀 [수정 5] [API] 파일 및 폴더 이름 변경 (Rename) 엔진 장착!
                else if (method.equals("POST") && path.startsWith("/api/rename")) {
                    String q = path.split("\\?")[1];
                    String[] params = q.split("&");
                    String dirStr = "", oldName = "", newName = "";
                    for (String p : params) {
                        if (p.startsWith("dir=")) dirStr = URLDecoder.decode(p.substring(4), "UTF-8");
                        if (p.startsWith("old=")) oldName = URLDecoder.decode(p.substring(4), "UTF-8");
                        if (p.startsWith("new=")) newName = URLDecoder.decode(p.substring(4), "UTF-8");
                    }

                    File targetDir = dirStr.isEmpty() ? rootFolder : new File(rootFolder, dirStr);
                    File oldFile = new File(targetDir, oldName);
                    File newFile = new File(targetDir, newName);

                    // 기존 파일이 존재하고 새 이름의 파일이 없을 때만 안전하게 이름 변경 실행
                    if (oldFile.exists() && !newFile.exists()) {
                        oldFile.renameTo(newFile);
                    }
                    os.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes("UTF-8"));
                }

                // 5️⃣ [API] 파일 읽기 (스트리밍, 다운로드, 코드 불러오기)

                // 5️⃣ [API] 파일 읽기 (스트리밍, 다운로드, 코드 불러오기)
                else if (method.equals("GET") && path.startsWith("/api/file")) {
                    String q = path.split("\\?")[1];
                    String targetPath = URLDecoder.decode(q.substring(5), "UTF-8");
                    File targetFile = new File(rootFolder, targetPath);

                    if (!targetFile.exists() || targetFile.isDirectory()) {
                        os.write("HTTP/1.1 404 Not Found\r\n\r\nNot Found".getBytes("UTF-8"));
                    } else {
                        String mimeType = "application/octet-stream";
                        String lowerName = targetFile.getName().toLowerCase();
                        if (lowerName.endsWith(".mp3")) mimeType = "audio/mpeg";
                        else if (lowerName.endsWith(".flac")) mimeType = "audio/flac";
                        else if (lowerName.endsWith(".wav")) mimeType = "audio/wav";
                        else if (lowerName.endsWith(".ogg")) mimeType = "audio/ogg";
                        else if (lowerName.endsWith(".m4a") || lowerName.endsWith(".aac")) mimeType = "audio/mp4";
                        else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) mimeType = "image/jpeg";
                        else if (lowerName.endsWith(".png")) mimeType = "image/png";
                        else if (lowerName.endsWith(".json")) mimeType = "application/json";
                            // 🚀 [수정 2] 방금 추가한 파일들도 브라우저가 순수한 '글자'로 인식해서 에디터 창에 띄우도록 명시!
                        else if (lowerName.endsWith(".txt") || lowerName.endsWith(".m3u") || lowerName.endsWith(".m3u8") || lowerName.endsWith(".eq")) mimeType = "text/plain";

                        String header = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + targetFile.length() + "\r\n" +
                                "Accept-Ranges: bytes\r\n\r\n";
                        os.write(header.getBytes("UTF-8"));

                        FileInputStream fis = new FileInputStream(targetFile);
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        fis.close();
                    }
                }

                // 6️⃣ [API] 파일 업로드
                else if (method.equals("POST") && path.startsWith("/api/upload")) {
                    String q = path.split("\\?")[1];
                    String[] params = q.split("&");
                    String dirStr = "", name = "unnamed.file";
                    for (String p : params) {
                        if (p.startsWith("dir=")) dirStr = URLDecoder.decode(p.substring(4), "UTF-8");
                        if (p.startsWith("name=")) name = URLDecoder.decode(p.substring(5), "UTF-8");
                    }

                    File targetDir = dirStr.isEmpty() ? rootFolder : new File(rootFolder, dirStr);
                    if (!targetDir.exists()) targetDir.mkdirs();
                    File outFile = new File(targetDir, name);

                    FileOutputStream fos = new FileOutputStream(outFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int totalRead = 0;
                    while (totalRead < contentLength && (bytesRead = is.read(buffer, 0, Math.min(buffer.length, contentLength - totalRead))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                    fos.flush();
                    try { fos.getFD().sync(); } catch(Exception e){}
                    fos.close();

                    os.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes("UTF-8"));
                }

                // 7️⃣ [API] 🚀 텍스트 파일 저장 (코드 에디터에서 전송된 텍스트를 기기에 덮어쓰기)
                else if (method.equals("POST") && path.startsWith("/api/save")) {
                    String q = path.split("\\?")[1];
                    String targetPath = URLDecoder.decode(q.substring(5), "UTF-8");
                    File targetFile = new File(rootFolder, targetPath);

                    FileOutputStream fos = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    int totalRead = 0;
                    // 전달받은 텍스트 몸통(Body)을 파일로 그대로 쭉 밀어 넣습니다.
                    while (totalRead < contentLength && (bytesRead = is.read(buffer, 0, Math.min(buffer.length, contentLength - totalRead))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                    fos.flush();
                    try { fos.getFD().sync(); } catch(Exception e){}
                    fos.close();

                    os.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes("UTF-8"));
                }
                else if (method.equals("GET") && path.startsWith("/api/download")) {
                    String q = path.split("\\?")[1];
                    String targetPath = URLDecoder.decode(q.substring(5), "UTF-8");
                    File targetFile = new File(rootFolder, targetPath);

                    if (!targetFile.exists() || targetFile.isDirectory()) {
                        os.write("HTTP/1.1 404 Not Found\r\n\r\nNot Found".getBytes("UTF-8"));
                    } else {
                        // 💡 브라우저가 화면에 재생하지 않고 "무조건 파일로 저장"하게 만드는 Content-Disposition 헤더!
                        String header = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/octet-stream\r\n" +
                                "Content-Disposition: attachment; filename=\"" + targetFile.getName() + "\"\r\n" +
                                "Content-Length: " + targetFile.length() + "\r\n" +
                                "Accept-Ranges: bytes\r\n\r\n";
                        os.write(header.getBytes("UTF-8"));

                        FileInputStream fis = new FileInputStream(targetFile);
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        fis.close();
                    }
                }
                os.flush();
            } catch (Exception e) {}
            finally {
                try { socket.close(); } catch (Exception e) {}
            }
        }
    }
}