package dev.remixwidget;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.Chronometer;
import android.widget.RemoteViews;
import android.widget.TextView;

/**
 * 위젯(RemoteViews)과 설정 화면 미리보기(진짜 View)를 같은 코드로 채웁니다.
 * 그래서 미리보기가 실제 위젯과 같은 모양·같은 글자 크기로 나옵니다.
 */
public final class Renderer {
    private Renderer() {}

    /** 값을 어디에 쓸지(위젯 또는 화면) 추상화한 출력 대상 */
    public interface Sink {
        void text(int id, CharSequence t);
        void size(int id, float sp);
        void color(int id, int c);
        void bgRes(int id, int res);
        void bgColor(int id, int c);
        void show(int id, boolean visible);
        void chrono(int id, long base, boolean running, boolean countDown);
    }

    public static final class RvSink implements Sink {
        private final RemoteViews rv;
        public RvSink(RemoteViews rv) { this.rv = rv; }
        @Override public void text(int id, CharSequence t) { rv.setTextViewText(id, t); }
        @Override public void size(int id, float sp) { rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp); }
        @Override public void color(int id, int c) { rv.setTextColor(id, c); }
        @Override public void bgRes(int id, int res) { rv.setInt(id, "setBackgroundResource", res); }
        @Override public void bgColor(int id, int c) { rv.setInt(id, "setBackgroundColor", c); }
        @Override public void show(int id, boolean v) { rv.setViewVisibility(id, v ? View.VISIBLE : View.GONE); }
        @Override public void chrono(int id, long base, boolean running, boolean countDown) {
            rv.setChronometerCountDown(id, countDown);
            rv.setChronometer(id, base, null, running);
        }
    }

    public static final class ViewSink implements Sink {
        private final View root;
        public ViewSink(View root) { this.root = root; }
        private View v(int id) { return root.findViewById(id); }
        @Override public void text(int id, CharSequence t) { View x = v(id); if (x instanceof TextView) ((TextView) x).setText(t); }
        @Override public void size(int id, float sp) { View x = v(id); if (x instanceof TextView) ((TextView) x).setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); }
        @Override public void color(int id, int c) { View x = v(id); if (x instanceof TextView) ((TextView) x).setTextColor(c); }
        @Override public void bgRes(int id, int res) { View x = v(id); if (x != null) x.setBackgroundResource(res); }
        @Override public void bgColor(int id, int c) { View x = v(id); if (x != null) x.setBackgroundColor(c); }
        @Override public void show(int id, boolean vis) { View x = v(id); if (x != null) x.setVisibility(vis ? View.VISIBLE : View.GONE); }
        @Override public void chrono(int id, long base, boolean running, boolean countDown) {
            View x = v(id);
            if (x instanceof Chronometer) {
                Chronometer ch = (Chronometer) x;
                ch.setCountDown(countDown);
                ch.setBase(base);
                if (running) ch.start(); else ch.stop();
            }
        }
    }

    // 기본 글자 크기(sp). 설정의 퍼센트가 여기에 곱해집니다.
    // 순서: 제목, 시간, 종료까지, 부제, 번호 칩, 종료까지 라벨, NEXT 줄
    private static final float[][] BASE = {
            /* 가로 2칸 */ {18, 30, 16, 12, 11, 11, 12},
            /* 작게     */ {24, 52, 20, 14, 11, 12, 15},
            /* 목록 포함 */ {22, 40, 15, 13, 11, 12, 13},
            /* 가로 4칸 */ {20, 32, 16, 12, 11, 11, 12},
    };
    private static final float LIST_BASE = 14f;

    /** 지금 상태(st)를 kind 종류의 위젯에 그립니다. elapsed 는 SystemClock.elapsedRealtime() */
    public static void render(Context c, Sink s, int kind, Schedule sch, Schedule.State st, long elapsed) {
        Themes.T t = Themes.get(Prefs.theme(c));
        float tp = Prefs.titlePct(c, kind) / 100f;
        float mp = Prefs.timePct(c, kind) / 100f;
        float sp = Prefs.subPct(c, kind) / 100f;
        float[] b = BASE[kind];
        boolean shortChip = kind == Prefs.KIND_MINI || kind == Prefs.KIND_WIDE;
        boolean hasLbl = kind == Prefs.KIND_SMALL || kind == Prefs.KIND_BIG;

        // ---- 내용 ----
        boolean hasSub;
        if (st.gap) {
            s.text(R.id.w_idx, "WAIT");
            s.text(R.id.w_title, "다음 정각을 기다리는 중");
            s.text(R.id.w_sub, "");
            hasSub = false;
        } else {
            s.text(R.id.w_idx, shortChip
                    ? String.format("%d/%d", st.index + 1, sch.count)
                    : String.format("NOW  %02d / %02d", st.index + 1, sch.count));
            s.text(R.id.w_title, sch.titles[st.index]);
            s.text(R.id.w_sub, sch.subs[st.index]);
            hasSub = !sch.subs[st.index].isEmpty();
        }
        String nextSub = sch.subs[st.next];
        s.text(R.id.w_next, "NEXT  " + sch.titles[st.next] + (nextSub.isEmpty() ? "" : " (" + nextSub + ")"));
        // 위로 올라가는 타이머(현재 재생 위치)와 아래로 내려가는 타이머(곡 종료까지). 시스템이 직접 세므로 초가 알아서 올라갑니다.
        s.chrono(R.id.w_pos, elapsed - st.withinMs, !st.gap, false);
        s.chrono(R.id.w_remain, elapsed + st.remainMs, true, true);

        // ---- 모양 ----
        s.bgRes(R.id.w_root, t.bg);
        s.bgRes(R.id.w_idx, t.chip);
        s.color(R.id.w_idx, t.accent);
        s.size(R.id.w_idx, b[4] * sp);
        s.show(R.id.w_idx, Prefs.showIdx(c));
        s.color(R.id.w_title, t.ink);
        s.size(R.id.w_title, b[0] * tp);
        s.color(R.id.w_pos, t.ink);
        s.size(R.id.w_pos, b[1] * mp);
        s.color(R.id.w_remain, t.accent);
        s.size(R.id.w_remain, b[2] * mp);

        if (hasLbl) {
            s.color(R.id.w_sub, t.mute);
            s.size(R.id.w_sub, b[3] * sp);
            s.show(R.id.w_sub, hasSub && Prefs.showSub(c));
            s.color(R.id.w_lbl, t.mute);
            s.size(R.id.w_lbl, b[5] * sp);
            s.bgColor(R.id.w_div, t.div);
        }
        if (kind == Prefs.KIND_SMALL) {
            boolean next = Prefs.showNext(c);
            s.color(R.id.w_next, t.mute);
            s.size(R.id.w_next, b[6] * sp);
            s.show(R.id.w_next, next);
            s.show(R.id.w_div, next);
        }
        if (kind == Prefs.KIND_BIG) {
            float lp = Prefs.listPct(c) / 100f;
            int n = sch.titles.length;
            for (int i = 0; i < WidgetUpdater.MAX_ROWS; i++) {
                int id = c.getResources().getIdentifier("r" + i, "id", c.getPackageName());
                if (id == 0) continue;
                if (i >= n) {
                    s.show(id, false);
                    continue;
                }
                s.show(id, true);
                String sub = sch.subs[i];
                s.text(id, String.format("%02d  %s%s", i + 1, sch.titles[i], sub.isEmpty() ? "" : " (" + sub + ")"));
                s.size(id, LIST_BASE * lp);
                boolean cur = !st.gap && i == st.index;
                boolean past = !st.gap ? i < st.index : true;
                s.color(id, cur ? Color.WHITE : (past ? t.past : t.ink));
                s.bgRes(id, cur ? t.row : R.drawable.row_none);
            }
        }
    }
}
