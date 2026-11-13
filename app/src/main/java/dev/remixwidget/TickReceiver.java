package dev.remixwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 곡이 바뀔 때, 1분 안전장치, 부팅·시간 변경·앱 업데이트 후에 위젯을 다시 그립니다. */
public class TickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent == null ? null : intent.getAction();
        if (WidgetUpdater.ACTION_HEARTBEAT.equals(a)) {
            WidgetUpdater.updateIfStale(context);
        } else {
            WidgetUpdater.updateAll(context);
        }
    }
}
