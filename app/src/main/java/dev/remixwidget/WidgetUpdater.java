package dev.remixwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.TimeZone;

/** 홈 화면 위젯을 그리고, 곡이 바뀌는 시각에 다시 그리도록 알람을 예약합니다. */
public final class WidgetUpdater {
    public static final String ACTION_TICK = "dev.remixwidget.TICK";
    public static final String ACTION_HEARTBEAT = "dev.remixwidget.HEARTBEAT";
    static final int MAX_ROWS = 30;
    private static final long HEARTBEAT_MS = 60_000L;

    private static final Class<?>[] PROVIDERS = {MiniWidget.class, SmallWidget.class, BigWidget.class, WideWidget.class};

    private WidgetUpdater() {}

    /** 모든 위젯을 지금 상태로 다시 그리고, 다음 곡 전환 알람을 예약합니다. */
    public static void updateAll(Context context) {
        Context c = context.getApplicationContext();
        Schedule s = ListStore.load(c);
        long now = System.currentTimeMillis();
        Schedule.State st = s.at(now, TimeZone.getDefault());
        long elapsed = SystemClock.elapsedRealtime();

        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        boolean any = false;
        for (int kind = 0; kind < PROVIDERS.length; kind++) {
            int[] ids = mgr.getAppWidgetIds(new ComponentName(c, PROVIDERS[kind]));
            if (ids.length == 0) continue;
            any = true;
            RemoteViews rv = new RemoteViews(c.getPackageName(), Layouts.layout(kind, Prefs.variant(c, kind)));
            Renderer.render(c, new Renderer.RvSink(rv), kind, s, st, elapsed);
            Intent open = new Intent(c, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.w_root, pi);
            mgr.updateAppWidget(ids, rv);
        }
        if (any) {
            Prefs.setDrawnEnd(c, st.trackEndMs);
            // 곡이 끝나는 순간 + 0.3초 뒤에 다시 그립니다. 그리고 1분마다 '낡았는지' 확인하는 안전장치도 겁니다.
            scheduleNext(c, st.trackEndMs + 300);
            scheduleHeartbeat(c);
        } else {
            cancelAlarms(c);
        }
    }

    /**
     * 안전장치: 곡이 바뀔 시각이 지났는데 아직 이전 곡이 그려져 있으면 다시 그립니다.
     * (알람이 늦게 울렸거나 폰이 절전 중이었을 때, 화면이 켜지면 곧바로 따라잡기 위함)
     */
    public static void updateIfStale(Context context) {
        Context c = context.getApplicationContext();
        if (System.currentTimeMillis() >= Prefs.drawnEnd(c) - 100) {
            updateAll(c);
        } else {
            scheduleNext(c, Prefs.drawnEnd(c) + 300);   // 곡 전환 알람이 사라졌을 경우를 대비해 다시 겁니다
        }
    }

    static void scheduleNext(Context c, long atMs) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c, 1, ACTION_TICK);
        try {
            if (canScheduleExact(c)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi);   // 몇 분 늦을 수 있음
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pi);
        }
    }

    /** 1분마다 깨어 있을 때만 확인하는 알람(기기를 깨우지 않음). 화면을 켜면 바로 도착해서 위젯이 따라잡습니다. */
    private static void scheduleHeartbeat(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + HEARTBEAT_MS, HEARTBEAT_MS,
                alarmIntent(c, 2, ACTION_HEARTBEAT));
    }

    private static void cancelAlarms(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(alarmIntent(c, 1, ACTION_TICK));
        am.cancel(alarmIntent(c, 2, ACTION_HEARTBEAT));
    }

    private static PendingIntent alarmIntent(Context c, int code, String action) {
        Intent i = new Intent(c, TickReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static boolean canScheduleExact(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am.canScheduleExactAlarms();
    }
}
