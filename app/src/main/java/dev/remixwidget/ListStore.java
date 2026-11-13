package dev.remixwidget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 곡 목록(list.json)을 읽고 새로고침합니다.
 * 순서: 새로고침으로 받아 둔 파일 → 앱에 들어 있는 기본 파일.
 */
public final class ListStore {
    private static final String FILE = "list.json";
    private static final String PREFS = "list_prefs";
    private static final int MAX_BYTES = 256 * 1024;

    private ListStore() {}

    public static Schedule load(Context c) {
        try {
            File f = new File(c.getFilesDir(), FILE);
            if (f.exists()) return parse(readAll(new java.io.FileInputStream(f)));
        } catch (Exception ignored) {
            // 받아 둔 파일이 깨졌으면 기본 목록을 씁니다.
        }
        try {
            return parse(readAll(c.getAssets().open(FILE)));
        } catch (Exception e) {
            throw new IllegalStateException("기본 목록을 읽을 수 없습니다", e);
        }
    }

    /** list.json 내용을 검사하고 Schedule 로 바꿉니다. 형식이 틀리면 예외(메시지에 이유). */
    public static Schedule parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        String name = o.optString("name", "REMIX");
        int cycle = o.optInt("cycleSeconds", 3600);
        JSONArray a = o.getJSONArray("tracks");
        int n = a.length();
        if (n == 0) throw new JSONException("tracks 가 비어 있습니다");
        String[] titles = new String[n];
        String[] subs = new String[n];
        int[] secs = new int[n];
        for (int i = 0; i < n; i++) {
            JSONObject t = a.getJSONObject(i);
            titles[i] = t.getString("title");
            subs[i] = t.optString("sub", "");
            if (t.has("seconds")) {
                secs[i] = t.getInt("seconds");
            } else if (t.has("length")) {
                secs[i] = parseLength(t.getString("length"));
            } else {
                throw new JSONException((i + 1) + "번 곡에 length(예: \"2:59\") 또는 seconds 가 없습니다");
            }
            if (secs[i] <= 0) throw new JSONException((i + 1) + "번 곡 길이가 올바르지 않습니다");
        }
        try {
            return new Schedule(name, titles, subs, secs, cycle);
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
    }

    /** "2:59" 또는 "1:02:03" → 초 */
    static int parseLength(String s) throws JSONException {
        String[] p = s.trim().split(":");
        if (p.length < 2 || p.length > 3) throw new JSONException("길이 형식이 올바르지 않습니다: " + s);
        int v = 0;
        try {
            for (String x : p) v = v * 60 + Integer.parseInt(x.trim());
        } catch (NumberFormatException e) {
            throw new JSONException("길이 형식이 올바르지 않습니다: " + s);
        }
        return v;
    }

    /** 인터넷에서 최신 목록을 받아 저장합니다. 반드시 백그라운드 스레드에서 호출하세요. 성공하면 null, 실패하면 이유를 돌려줍니다. */
    public static String refresh(Context c) {
        String url = BuildConfig.LIST_URL;
        if (url.contains("REPLACE_")) return "목록 주소가 아직 설정되지 않았습니다 (build.gradle 의 LIST_URL)";
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url + "?t=" + System.currentTimeMillis()).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setUseCaches(false);
            int code = con.getResponseCode();
            if (code != 200) return "서버 응답 오류 (" + code + ")";
            String body = readAll(con.getInputStream());
            Schedule s = parse(body);              // 형식 검사. 틀리면 기존 목록을 그대로 유지
            File tmp = new File(c.getFilesDir(), FILE + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            File dst = new File(c.getFilesDir(), FILE);
            if (!tmp.renameTo(dst)) {
                dst.delete();
                if (!tmp.renameTo(dst)) return "목록 파일을 저장하지 못했습니다";
            }
            String ver = "";
            try { ver = new JSONObject(body).optString("version", ""); } catch (JSONException ignored) {}
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong("last_ok", System.currentTimeMillis())
                    .putString("version", ver)
                    .putInt("count", s.titles.length)
                    .apply();
            return null;
        } catch (JSONException e) {
            return "목록 파일 형식 오류: " + e.getMessage();
        } catch (IOException e) {
            return "인터넷에 연결할 수 없습니다";
        } finally {
            if (con != null) con.disconnect();
        }
    }

    public static long lastRefreshMs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_ok", 0L);
    }

    public static String lastVersion(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("version", "");
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n, total = 0;
            while ((n = is.read(buf)) > 0) {
                total += n;
                if (total > MAX_BYTES) throw new IOException("목록 파일이 너무 큽니다");
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
