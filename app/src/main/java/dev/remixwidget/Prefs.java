package dev.remixwidget;

import android.content.Context;
import android.content.SharedPreferences;

/** 위젯 모양 설정과 "내 목록 사용" 여부를 저장합니다. 크기·배치는 위젯 종류(KIND)마다 따로 저장합니다. */
public final class Prefs {
    public static final int KIND_MINI = 0;   // 가로 2칸
    public static final int KIND_SMALL = 1;  // 작게
    public static final int KIND_BIG = 2;    // 목록 포함
    public static final int KIND_WIDE = 3;   // 가로 4칸
    public static final int KIND_COUNT = 4;
    public static final String[] KIND_NAMES = {"가로 2칸", "작게", "목록 포함", "가로 4칸"};

    public static final int PCT_MIN = 60;
    public static final int PCT_MAX = 260;

    private static final String FILE = "appearance";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static int clampPct(int v) {
        return Math.max(PCT_MIN, Math.min(PCT_MAX, v));
    }

    // ---- 모든 위젯 공통 ----
    public static int theme(Context c) {
        int t = sp(c).getInt("theme", 0);
        return (t < 0 || t >= Themes.ALL.length) ? 0 : t;
    }
    public static void setTheme(Context c, int v) { sp(c).edit().putInt("theme", v).apply(); }

    public static boolean showNext(Context c) { return sp(c).getBoolean("show_next", true); }
    public static void setShowNext(Context c, boolean v) { sp(c).edit().putBoolean("show_next", v).apply(); }

    public static boolean showSub(Context c) { return sp(c).getBoolean("show_sub", true); }
    public static void setShowSub(Context c, boolean v) { sp(c).edit().putBoolean("show_sub", v).apply(); }

    public static boolean showIdx(Context c) { return sp(c).getBoolean("show_idx", true); }
    public static void setShowIdx(Context c, boolean v) { sp(c).edit().putBoolean("show_idx", v).apply(); }

    // ---- 위젯 종류별 (퍼센트: 100 = 기본 크기) ----
    public static int titlePct(Context c, int kind) { return clampPct(sp(c).getInt("title_pct_" + kind, 100)); }
    public static void setTitlePct(Context c, int kind, int v) { sp(c).edit().putInt("title_pct_" + kind, clampPct(v)).apply(); }

    public static int timePct(Context c, int kind) { return clampPct(sp(c).getInt("time_pct_" + kind, 100)); }
    public static void setTimePct(Context c, int kind, int v) { sp(c).edit().putInt("time_pct_" + kind, clampPct(v)).apply(); }

    /** 종료까지·NEXT·부제·번호 칩 같은 보조 글자 */
    public static int subPct(Context c, int kind) { return clampPct(sp(c).getInt("sub_pct_" + kind, 100)); }
    public static void setSubPct(Context c, int kind, int v) { sp(c).edit().putInt("sub_pct_" + kind, clampPct(v)).apply(); }

    /** 목록 위젯의 곡 줄 글자 */
    public static int listPct(Context c) { return clampPct(sp(c).getInt("list_pct", 100)); }
    public static void setListPct(Context c, int v) { sp(c).edit().putInt("list_pct", clampPct(v)).apply(); }

    /** 배치 변형 번호 (Layouts.NAMES 참고) */
    public static int variant(Context c, int kind) {
        int v = sp(c).getInt("variant_" + kind, 0);
        return (v < 0 || v >= Layouts.count(kind)) ? 0 : v;
    }
    public static void setVariant(Context c, int kind, int v) { sp(c).edit().putInt("variant_" + kind, v).apply(); }

    // ---- 내 목록 ----
    public static boolean useCustom(Context c) { return sp(c).getBoolean("use_custom", false); }
    public static void setUseCustom(Context c, boolean v) { sp(c).edit().putBoolean("use_custom", v).apply(); }

    // ---- 마지막으로 그린 곡의 끝 시각 (이 시각이 지났는데 아직 안 바뀌었으면 '낡은' 상태) ----
    public static long drawnEnd(Context c) { return sp(c).getLong("drawn_end", 0L); }
    public static void setDrawnEnd(Context c, long v) { sp(c).edit().putLong("drawn_end", v).apply(); }

    /** 위젯 하나의 크기·배치를 기본값으로 */
    public static void resetKind(Context c, int kind) {
        SharedPreferences.Editor e = sp(c).edit();
        e.remove("title_pct_" + kind).remove("time_pct_" + kind).remove("sub_pct_" + kind).remove("variant_" + kind);
        if (kind == KIND_BIG) e.remove("list_pct");
        e.apply();
    }

    /** 모양 설정 전체를 기본값으로 (내 목록 설정은 건드리지 않음) */
    public static void resetAppearance(Context c) {
        SharedPreferences.Editor e = sp(c).edit();
        e.remove("theme").remove("show_next").remove("show_sub").remove("show_idx").remove("list_pct");
        for (int k = 0; k < KIND_COUNT; k++) {
            e.remove("title_pct_" + k).remove("time_pct_" + k).remove("sub_pct_" + k).remove("variant_" + k);
        }
        e.apply();
    }
}
