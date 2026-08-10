# 🎮 Y1 Launcher - Custom Mini-Game Development Guide

Y1 Launcher supports lightweight **HTML5 + WebView-based** mini-games. 
To build games that run smoothly under Android JellyBean (API 16) with Y1's physical Wheel/D-Pad controller, please follow the guidelines below.

---

## ⚠️ 3 Critical Development Rules (JellyBean Constraints)

Android JellyBean's stock WebView uses an older WebKit engine. Adhering to these rules prevents crashes and black screens.

### 1. Strictly Use ES5 (Vanilla JavaScript)
* ❌ **DO NOT USE (Causes Crashes):** `const`, `let`, Arrow functions (`() => {}`), `async/await`, `Promise`, `class`.
* ⭕ **SUPPORTED:** `var`, `function() {}`, standard `for`/`while` loops, HTML5 Canvas 2D.
> **Why?** JellyBean's WebKit engine cannot parse ES6+ syntax and will immediately fail with a `SyntaxError`, resulting in a blank black screen.

### 2. Map Key Inputs to the Physical Wheel (D-Pad)
The Y1 device relies on a **physical wheel (D-Pad + Center Key)** instead of touch controls.
Map your `keydown` event listeners to these key codes:

* ⬆️ **Wheel Up / DPAD Up:** `ArrowUp` (keyCode: `38` or `19`)
* ⬇️ **Wheel Down / DPAD Down:** `ArrowDown` (keyCode: `40` or `20`)
* ⬅️ **Left:** `ArrowLeft` (keyCode: `37` or `21`)
* ➡️ **Right:** `ArrowRight` (keyCode: `39` or `22`)
* 🔘 **Center Button:** `Enter` / `Space` (keyCode: `13` or `23`)
* 🔙 **Back Button:** `Escape` / `Back` (keyCode: `27` or `4`)

### 3. Target 2D Canvas & 320x240 Resolution
* ❌ **NOT SUPPORTED:** WebGL, Heavy 3D rendering, large MP4 video textures.
* ⭕ **RECOMMENDED:** HTML5 `<canvas>` 2D context, pixel art, lightweight PNG/GIF assets.

---

## 📂 File Directory Structure

Drop your game folder into the `Y1_Games` directory on the SD card. The launcher will automatically detect and display it in the **Game** menu.

```text
/storage/sdcard0/Y1_Games/
  └── MyGame/             <-- Your Game Folder Name
        ├── index.html    <-- (Required) Main entry file
        ├── icon.png      <-- (Optional) 64x64 pixel game icon
        └── assets/       <-- (Optional) Images & audio files
