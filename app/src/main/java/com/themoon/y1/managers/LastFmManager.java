package com.themoon.y1.managers;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LastFmManager {
    private static final String TAG = "LastFmManager";
    // 💡 공개 API Key (웹 브라우저 로그인 창 띄우기용 - 공개 식별자이므로 안전합니다)
    private static final String API_KEY = "2165415dd222229ee5bcfb7f93145fe1";
    // 🚀 모든 통신 및 보안 서명(API_SECRET) 처리를 전담하는 Vercel 프록시 URL
    private static final String PROXY_URL = "https://lastfm-proxy-livid.vercel.app/api";
    private static final String CALLBACK_URL = "y1://lastfm-callback";

    private static LastFmManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    private final ScrobbleDatabaseHelper dbHelper;

    private String sessionKey;
    private String username;
    private String appToken;
    
    private boolean isScrobblingEnabled = false;

    private LastFmManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("lastfm_prefs", Context.MODE_PRIVATE);
        this.httpClient = getUnsafeOkHttpClient();
        this.dbHelper = new ScrobbleDatabaseHelper(this.context);
        this.sessionKey = prefs.getString("session_key", null);
        this.username = prefs.getString("username", null);
        this.appToken = prefs.getString("app_token", null);
        this.isScrobblingEnabled = (this.sessionKey != null);
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslContext.getSocketFactory(), (javax.net.ssl.X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                    return true;
                }
            });
            return builder.build();
        } catch (Exception e) {
            return new OkHttpClient();
        }
    }

    public static LastFmManager getInstance(Context context) {
        if (instance == null) {
            instance = new LastFmManager(context);
        }
        return instance;
    }

    public boolean isEnabled() {
        return isScrobblingEnabled && sessionKey != null;
    }

    public String getUsername() {
        return username;
    }

    public String getAppToken() {
        return appToken != null ? appToken : prefs.getString("app_token", null);
    }

    /**
     * 🚀 Last.fm 공식 웹 인증 브라우저 창 열기
     * 사용자 비밀번호 입력 없이 1회 브라우저 승인 방식으로 안전하게 진행됩니다.
     */
    public void startWebAuth(Context ctx) {
        try {
            String authUrl = "http://www.last.fm/api/auth/?api_key=" + API_KEY + "&cb=" + android.net.Uri.encode(CALLBACK_URL);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Last.fm web auth: " + e.getMessage());
        }
    }

    /**
     * 🚀 웹 인증 후 수신한 Token을 Vercel 백엔드 프록시로 전달하여
     * Last.fm Session Key와 서비스 전용 JWT 토큰을 동시에 발급받습니다.
     */
    public void exchangeTokenWithBackend(String token, final LoginCallback callback) {
        String url = PROXY_URL + "?method=auth.getSession&token=" + token;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String bodyStr = response.body().string();
                        JSONObject json = new JSONObject(bodyStr);

                        String key = null;
                        String name = null;

                        if (json.has("session")) {
                            JSONObject session = json.getJSONObject("session");
                            key = session.getString("key");
                            name = session.getString("name");
                        } else if (json.has("sessionKey")) {
                            key = json.getString("sessionKey");
                            name = json.optString("username", "");
                        } else if (json.has("error")) {
                            if (callback != null) callback.onError(json.getString("message"));
                            return;
                        }

                        if (key != null) {
                            sessionKey = key;
                            username = name;
                            if (json.has("appToken")) {
                                appToken = json.getString("appToken");
                            } else if (json.has("token")) {
                                appToken = json.getString("token");
                            }
                            isScrobblingEnabled = true;

                            SharedPreferences.Editor editor = prefs.edit()
                                    .putString("session_key", sessionKey)
                                    .putString("username", username);
                            if (appToken != null) {
                                editor.putString("app_token", appToken);
                            }
                            editor.apply();

                            if (callback != null) callback.onSuccess();
                        } else {
                            if (callback != null) callback.onError("Invalid session response from server");
                        }
                    } else {
                        if (callback != null) callback.onError("HTTP Error: " + response.code());
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                }
            }
        });
    }

    public void login(final String user, String password, final LoginCallback callback) {
        FormBody formBody = new FormBody.Builder()
                .add("method", "auth.getMobileSession")
                .add("username", user)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(PROXY_URL)
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String bodyStr = response.body().string();
                        JSONObject json = new JSONObject(bodyStr);
                        if (json.has("session")) {
                            JSONObject session = json.getJSONObject("session");
                            sessionKey = session.getString("key");
                            username = session.getString("name");
                            isScrobblingEnabled = true;

                            prefs.edit()
                                    .putString("session_key", sessionKey)
                                    .putString("username", username)
                                    .apply();
                            
                            if (callback != null) callback.onSuccess();
                        } else if (json.has("error")) {
                            if (callback != null) callback.onError(json.getString("message"));
                        } else {
                            if (callback != null) callback.onError("Unknown error");
                        }
                    } else {
                        if (callback != null) callback.onError("HTTP Error: " + response.code());
                    }
                } catch (Exception e) {
                    if (callback != null) callback.onError(e.getMessage());
                }
            }
        });
    }

    public void logout() {
        sessionKey = null;
        username = null;
        appToken = null;
        isScrobblingEnabled = false;
        prefs.edit().clear().apply();
        dbHelper.clearQueue();
    }

    public void updateNowPlaying(String track, String artist) {
        if (!isEnabled()) return;
        
        FormBody formBody = new FormBody.Builder()
                .add("method", "track.updateNowPlaying")
                .add("track", track)
                .add("artist", artist)
                .add("sk", sessionKey)
                .build();

        Request request = new Request.Builder()
                .url(PROXY_URL)
                .post(formBody)
                .build();

        // Fire and forget
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "updateNowPlaying failed: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
            }
        });
    }

    // Queues a scrobble in SQLite
    public void scrobble(String track, String artist, long timestampUnix) {
        if (!isEnabled()) return;
        dbHelper.addScrobble(track, artist, timestampUnix);
        processQueue(); // Attempt to process immediately
    }

    private final java.util.concurrent.atomic.AtomicBoolean isProcessingQueue = new java.util.concurrent.atomic.AtomicBoolean(false);

    // Process the queue (up to 50 at a time per Last.fm batch limits, looping until empty)
    public void processQueue() {
        if (!isEnabled()) return;
        if (!isProcessingQueue.compareAndSet(false, true)) {
            return; // 이미 다른 스레드에서 전송 작업 중이면 중복 실행 방지
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        List<ScrobbleItem> batch = dbHelper.getBatch(50);
                        if (batch.isEmpty()) break;

                        FormBody.Builder formBuilder = new FormBody.Builder()
                                .add("method", "track.scrobble")
                                .add("sk", sessionKey);

                        for (int i = 0; i < batch.size(); i++) {
                            ScrobbleItem item = batch.get(i);
                            formBuilder.add("track[" + i + "]", item.track);
                            formBuilder.add("artist[" + i + "]", item.artist);
                            formBuilder.add("timestamp[" + i + "]", String.valueOf(item.timestamp));
                        }

                        Request request = new Request.Builder()
                                .url(PROXY_URL)
                                .post(formBuilder.build())
                                .build();

                        Response response = httpClient.newCall(request).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            String bodyStr = response.body().string();
                            JSONObject json = new JSONObject(bodyStr);
                            if (json.has("scrobbles")) {
                                for (ScrobbleItem item : batch) {
                                    dbHelper.removeScrobble(item.id);
                                }
                                Log.d(TAG, "Scrobbled " + batch.size() + " tracks successfully. Remaining in queue: " + dbHelper.getBatch(1).size());
                            } else if (json.has("error")) {
                                Log.e(TAG, "Scrobble error: " + json.getString("message"));
                                break; // Last.fm 에러 응답 시 루프 중단
                            } else {
                                break;
                            }
                        } else {
                            break; // HTTP 에러 또는 네트워크 단절 시 루프 중단 (DB에 보존)
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Scrobble exception: " + e.getMessage());
                } finally {
                    isProcessingQueue.set(false);
                }
            }
        }).start();
    }

    public interface LoginCallback {
        void onSuccess();
        void onError(String errorMsg);
    }

    // SQLite Queue Helper
    private static class ScrobbleItem {
        long id;
        String track;
        String artist;
        long timestamp;
    }

    private static class ScrobbleDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "scrobbles.db";
        private static final int DATABASE_VERSION = 1;
        private static final String TABLE_SCROBBLES = "scrobbles";

        public ScrobbleDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String CREATE_TABLE = "CREATE TABLE " + TABLE_SCROBBLES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "track TEXT," +
                    "artist TEXT," +
                    "timestamp INTEGER)";
            db.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCROBBLES);
            onCreate(db);
        }

        public void addScrobble(String track, String artist, long timestamp) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("track", track);
            values.put("artist", artist);
            values.put("timestamp", timestamp);
            db.insert(TABLE_SCROBBLES, null, values);
            db.close();
        }

        public List<ScrobbleItem> getBatch(int limit) {
            List<ScrobbleItem> list = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SCROBBLES + " ORDER BY id ASC LIMIT " + limit, null);
            if (cursor.moveToFirst()) {
                do {
                    ScrobbleItem item = new ScrobbleItem();
                    item.id = cursor.getLong(0);
                    item.track = cursor.getString(1);
                    item.artist = cursor.getString(2);
                    item.timestamp = cursor.getLong(3);
                    list.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
            return list;
        }

        public void removeScrobble(long id) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_SCROBBLES, "id=?", new String[]{String.valueOf(id)});
            db.close();
        }

        public void clearQueue() {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_SCROBBLES, null, null);
            db.close();
        }
    }
}
