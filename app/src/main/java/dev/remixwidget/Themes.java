package dev.remixwidget;

import android.graphics.Color;

/** 위젯 색 테마. drawable 이름은 res/drawable 의 widget_bg*, chip_bg*, row_cur* 와 짝입니다. */
public final class Themes {
    public static final class T {
        public final String name;
        public final int bg, chip, row;          // drawable 리소스
        public final int ink, mute, past, accent, div;
        T(String name, int bg, int chip, int row, String ink, String mute, String past, String accent, String div) {
            this.name = name; this.bg = bg; this.chip = chip; this.row = row;
            this.ink = Color.parseColor(ink); this.mute = Color.parseColor(mute);
            this.past = Color.parseColor(past); this.accent = Color.parseColor(accent);
            this.div = Color.parseColor(div);
        }
    }

    public static final T[] ALL = {
            new T("하늘", R.drawable.widget_bg, R.drawable.chip_bg, R.drawable.row_cur,
                    "#0D1A26", "#566B7F", "#8DA0B3", "#0670A8", "#D6E4EE"),
            new T("화이트", R.drawable.widget_bg_white, R.drawable.chip_bg_white, R.drawable.row_cur_white,
                    "#111827", "#6B7280", "#9CA3AF", "#0B86C6", "#E5E7EB"),
            new T("다크", R.drawable.widget_bg_dark, R.drawable.chip_bg_dark, R.drawable.row_cur_dark,
                    "#F1F5F9", "#9FB0C3", "#62758A", "#63C5F7", "#2D3C4D"),
            new T("반투명", R.drawable.widget_bg_glass, R.drawable.chip_bg_glass, R.drawable.row_cur_glass,
                    "#FFFFFF", "#C9D6E3", "#8FA3B8", "#8FD6FF", "#44FFFFFF"),
            new T("핑크", R.drawable.widget_bg_pink, R.drawable.chip_bg_pink, R.drawable.row_cur_pink,
                    "#2A1620", "#7D5C6C", "#BFA3B1", "#C2185B", "#F3D3E0"),
    };

    private Themes() {}

    public static T get(int i) {
        return ALL[(i < 0 || i >= ALL.length) ? 0 : i];
    }
}
