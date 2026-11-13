package dev.remixwidget;

/** 위젯 종류(KIND)별 배치 변형 목록. 0번이 기본 배치입니다. */
public final class Layouts {
    private static final int[][] IDS = {
            {R.layout.widget_mini, R.layout.widget_mini_v1, R.layout.widget_mini_v2, R.layout.widget_mini_v3},
            {R.layout.widget_small, R.layout.widget_small_v1, R.layout.widget_small_v2, R.layout.widget_small_v3},
            {R.layout.widget_big, R.layout.widget_big_v1, R.layout.widget_big_v2},
            {R.layout.widget_wide, R.layout.widget_wide_v1},
    };

    public static final String[][] NAMES = {
            {"제목 위 · 시간 아래 (왼쪽 정렬)", "시간 위 · 제목 아래 (왼쪽 정렬)", "제목 위 · 시간 아래 (가운데)", "시간 위 · 제목 아래 (가운데)"},
            {"제목 위 · 시간 아래 (왼쪽 정렬)", "시간 위 · 제목 아래 (왼쪽 정렬)", "제목 위 · 시간 아래 (가운데)", "시간 위 · 제목 아래 (가운데)"},
            {"좌우로 나눔 (제목 왼쪽 · 시간 오른쪽)", "가운데 쌓기 (제목 위)", "가운데 쌓기 (시간 위)"},
            {"번호 · 제목 · 시간 · 종료까지", "시간 · 제목 · 종료까지 · 번호"},
    };

    private Layouts() {}

    public static int count(int kind) {
        return IDS[kind].length;
    }

    public static int layout(int kind, int variant) {
        int[] a = IDS[kind];
        return a[(variant < 0 || variant >= a.length) ? 0 : variant];
    }
}
