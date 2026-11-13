package dev.remixwidget;

import android.content.Context;
import android.content.SharedPreferences;

/** 위젯 모양 설정과 "내 목록 사용" 여부를 저장합니다. */
public final class Prefs {
    private static final String FILE = "appearance";
    /** 글자 크기 단계 (0=작게, 1=보통, 2=크게, 3=아주 크게) */
    private static final float[] SCALES = {0.85f, 1.0f, 1.2f, 1.4f};
    public static final String[] SCALE_NAMES = {"작게", "보통", "크게", "아주 크게"};

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static float scale(int step) {
        return SCALES[Math.max(0, Math.min(SCALES.length - 1, step))];
    }

    public static int theme(Context c) {
        int t = sp(c).getInt("theme", 0);
        return (t < 0 || t >= Themes.ALL.length) ? 0 : t;
    }
    public static void setTheme(Context c, int v) { sp(c).edit().putInt("theme", v).apply(); }

    public static int titleScale(Context c) { return sp(c).getInt("title_scale", 1); }
    public static void setTitleScale(Context c, int v) { sp(c).edit().putInt("title_scale", v).apply(); }

    public static int timeScale(Context c) { return sp(c).getInt("time_scale", 1); }
    public static void setTimeScale(Context c, int v) { sp(c).edit().putInt("time_scale", v).apply(); }

    public static boolean showNext(Context c) { return sp(c).getBoolean("show_next", true); }
    public static void setShowNext(Context c, boolean v) { sp(c).edit().putBoolean("show_next", v).apply(); }

    public static boolean showSub(Context c) { return sp(c).getBoolean("show_sub", true); }
    public static void setShowSub(Context c, boolean v) { sp(c).edit().putBoolean("show_sub", v).apply(); }

    public static boolean showIdx(Context c) { return sp(c).getBoolean("show_idx", true); }
    public static void setShowIdx(Context c, boolean v) { sp(c).edit().putBoolean("show_idx", v).apply(); }

    public static boolean useCustom(Context c) { return sp(c).getBoolean("use_custom", false); }
    public static void setUseCustom(Context c, boolean v) { sp(c).edit().putBoolean("use_custom", v).apply(); }

    /** 모양 설정만 기본값으로 되돌립니다 (내 목록 설정은 건드리지 않음). */
    public static void resetAppearance(Context c) {
        sp(c).edit().remove("theme").remove("title_scale").remove("time_scale")
                .remove("show_next").remove("show_sub").remove("show_idx").apply();
    }
}
