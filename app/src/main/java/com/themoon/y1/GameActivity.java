package com.themoon.y1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GameActivity extends Activity {
    private WebView webView;

    public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) { mContext = c; }

        @JavascriptInterface
        public void exitGame() {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((Activity) mContext).finish();
                }
            });
        }
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 🚀 [패치 1] 게임 중 화면 꺼짐 방지! (FLAG_KEEP_SCREEN_ON)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        webView.addJavascriptInterface(new WebAppInterface(this), "Y1App");

        String gameUrl = getIntent().getStringExtra("game_url");
        if (gameUrl != null) {
            webView.loadUrl(gameUrl);
        }
    }

    // 🚀 [패치 2] 화면이 꺼지거나 백그라운드로 갈 때 게임 엔진 강제 정지!
    @Override
    protected void onPause() {
        super.onPause();
        com.themoon.y1.managers.BatteryStatsManager.getInstance(this).setMode(com.themoon.y1.managers.BatteryStatsManager.MODE_OTHER);
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers(); // JS setTimeout, setInterval 완벽 차단!
        }
    }

    // 🚀 [패치 3] 화면을 다시 켜면 게임 엔진 재가동!
    @Override
    protected void onResume() {
        super.onResume();
        com.themoon.y1.managers.BatteryStatsManager.getInstance(this).setMode(com.themoon.y1.managers.BatteryStatsManager.MODE_GAME);
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    // 🚀 [패치 4] 게임 종료 시 메모리 깔끔하게 정리!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        final int action = event.getAction();

        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 4 ||
                keyCode == 85 || keyCode == 126 || keyCode == 127 ||
                keyCode == 88 || keyCode == 87 ||
                keyCode == 23 || keyCode == 66 ||
                keyCode == 19 || keyCode == 20 || keyCode == 21 || keyCode == 22) {

            if (webView != null) {
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        String jsCode = "if(typeof window.onDeviceKey === 'function') { window.onDeviceKey(" + keyCode + ", " + action + "); }";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            webView.evaluateJavascript(jsCode, null);
                        } else {
                            webView.loadUrl("javascript:" + jsCode);
                        }
                    }
                });
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}