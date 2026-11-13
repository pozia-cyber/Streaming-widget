package dev.remixwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.TimeZone;

/** 홈 화면 위젯을 그리고, 곡이 바뀌는 시각에 다시 그리도록 알람을 예약합니다. */
public final class WidgetUpdater {
    public static final String ACTION_TICK = "dev.remixwidget.TICK";
    static final int MAX_ROWS = 30;

    static final int KIND_MINI = 0;
    static final int KIND_SMALL = 1;
    static final int KIND_BIG = 2;

    private WidgetUpdater() {}

    public static void updateAll(Context context) {
        Context c = context.getApplicationContext();
        Schedule s = ListStore.load(c);
        long now = System.currentTimeMillis();
        Schedule.State st = s.at(now, TimeZone.getDefault());

        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        int[] mini = mgr.getAppWidgetIds(new ComponentName(c, MiniWidget.class));
        int[] small = mgr.getAppWidgetIds(new ComponentName(c, SmallWidget.class));
        int[] big = mgr.getAppWidgetIds(new ComponentName(c, BigWidget.class));
        if (mini.length > 0) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_mini);
            fillHeader(c, rv, s, st, KIND_MINI);
            applyStyle(c, rv, KIND_MINI);
            mgr.updateAppWidget(mini, rv);
        }
        if (small.length > 0) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_small);
            fillHeader(c, rv, s, st, KIND_SMALL);
            applyStyle(c, rv, KIND_SMALL);
            mgr.updateAppWidget(small, rv);
        }
        if (big.length > 0) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_big);
            fillHeader(c, rv, s, st, KIND_BIG);
            applyStyle(c, rv, KIND_BIG);
            fillRows(c, rv, s, st);
            mgr.updateAppWidget(big, rv);
        }
        if (small.length > 0 || big.length > 0 || mini.length > 0) {
            // 곡이 끝나는 순간 + 0.3초 뒤에 다시 그립니다 (경계에서 살짝 이르게 깨어나는 경우 대비)
            scheduleNext(c, st.trackEndMs + 300);
        }
    }

    private static void fillHeader(Context c, RemoteViews rv, Schedule s, Schedule.State st, int kind) {
        long elapsed = SystemClock.elapsedRealtime();
        boolean hasSub;
        if (st.gap) {
            rv.setTextViewText(R.id.w_idx, "WAIT");
            rv.setTextViewText(R.id.w_title, "다음 정각을 기다리는 중");
            rv.setTextViewText(R.id.w_sub, "");
            hasSub = false;
        } else {
            rv.setTextViewText(R.id.w_idx, kind == KIND_MINI
                    ? String.format("%d/%d", st.index + 1, s.count)
                    : String.format("NOW  %02d / %02d", st.index + 1, s.count));
            rv.setTextViewText(R.id.w_title, s.titles[st.index]);
            rv.setTextViewText(R.id.w_sub, s.subs[st.index]);
            hasSub = !s.subs[st.index].isEmpty();
        }
        if (kind != KIND_MINI) {
            rv.setViewVisibility(R.id.w_sub, hasSub && Prefs.showSub(c) ? View.VISIBLE : View.GONE);
        }
        // 위로 올라가는 타이머(현재 재생 위치). 시스템이 직접 세므로 위젯을 갱신하지 않아도 초가 올라갑니다.
        rv.setChronometer(R.id.w_pos, elapsed - st.withinMs, null, !st.gap);
        // 아래로 내려가는 타이머(곡 종료까지)
        rv.setChronometerCountDown(R.id.w_remain, true);
        rv.setChronometer(R.id.w_remain, elapsed + st.remainMs, null, true);
        String nextSub = s.subs[st.next];
        rv.setTextViewText(R.id.w_next, "NEXT  " + s.titles[st.next] + (nextSub.isEmpty() ? "" : " (" + nextSub + ")"));

        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.w_root, pi);
    }

    /** 설정(테마, 글자 크기, 표시 여부)을 위젯에 적용합니다. */
    private static void applyStyle(Context c, RemoteViews rv, int kind) {
        Themes.T t = Themes.get(Prefs.theme(c));
        float ts = Prefs.scale(Prefs.titleScale(c));   // 제목 · 부제 · 목록 글자
        float ms = Prefs.scale(Prefs.timeScale(c));    // 시간 글자
        boolean idx = Prefs.showIdx(c);

        rv.setInt(R.id.w_root, "setBackgroundResource", t.bg);
        rv.setInt(R.id.w_idx, "setBackgroundResource", t.chip);
        rv.setTextColor(R.id.w_idx, t.accent);
        rv.setViewVisibility(R.id.w_idx, idx ? View.VISIBLE : View.GONE);
        rv.setTextColor(R.id.w_title, t.ink);
        rv.setTextColor(R.id.w_pos, t.ink);
        rv.setTextColor(R.id.w_remain, t.accent);

        float titleBase, posBase, remainBase;
        if (kind == KIND_MINI) { titleBase = 14; posBase = 22; remainBase = 12; }
        else if (kind == KIND_SMALL) { titleBase = 20; posBase = 44; remainBase = 16; }
        else { titleBase = 18; posBase = 34; remainBase = 11; }
        rv.setTextViewTextSize(R.id.w_title, TypedValue.COMPLEX_UNIT_SP, titleBase * ts);
        rv.setTextViewTextSize(R.id.w_pos, TypedValue.COMPLEX_UNIT_SP, posBase * ms);
        rv.setTextViewTextSize(R.id.w_remain, TypedValue.COMPLEX_UNIT_SP, remainBase * ms);

        if (kind != KIND_MINI) {
            rv.setTextColor(R.id.w_sub, t.mute);
            rv.setTextViewTextSize(R.id.w_sub, TypedValue.COMPLEX_UNIT_SP, 11 * ts);
            rv.setTextColor(R.id.w_lbl, t.mute);
            rv.setInt(R.id.w_div, "setBackgroundColor", t.div);
        }
        if (kind == KIND_SMALL) {
            boolean next = Prefs.showNext(c);
            rv.setTextColor(R.id.w_next, t.mute);
            rv.setTextViewTextSize(R.id.w_next, TypedValue.COMPLEX_UNIT_SP, 12 * ts);
            rv.setViewVisibility(R.id.w_next, next ? View.VISIBLE : View.GONE);
            rv.setViewVisibility(R.id.w_div, next ? View.VISIBLE : View.GONE);
        }
    }

    private static void fillRows(Context c, RemoteViews rv, Schedule s, Schedule.State st) {
        Themes.T t = Themes.get(Prefs.theme(c));
        float ts = Prefs.scale(Prefs.titleScale(c));
        int n = s.titles.length;
        for (int i = 0; i < MAX_ROWS; i++) {
            int id = c.getResources().getIdentifier("r" + i, "id", c.getPackageName());
            if (id == 0) continue;
            if (i >= n) {
                rv.setViewVisibility(id, View.GONE);
                continue;
            }
            rv.setViewVisibility(id, View.VISIBLE);
            String sub = s.subs[i];
            String label = String.format("%02d  %s%s", i + 1, s.titles[i], sub.isEmpty() ? "" : " (" + sub + ")");
            rv.setTextViewText(id, label);
            rv.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, 11 * ts);
            boolean cur = !st.gap && i == st.index;
            boolean past = !st.gap ? i < st.index : true;
            rv.setTextColor(id, cur ? Color.WHITE : (past ? t.past : t.ink));
            rv.setInt(id, "setBackgroundResource", cur ? t.row : R.drawable.row_none);
        }
    }

    static void scheduleNext(Context c, long atMs) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, TickReceiver.class).setAction(ACTION_TICK);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi);
        } else {
            // 정확한 알람 권한이 없으면 몇 분 늦을 수 있습니다. 앱 화면에서 권한을 허용하세요.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi);
        }
    }

    static boolean canScheduleExact(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am.canScheduleExactAlarms();
    }
}
