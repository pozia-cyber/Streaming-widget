package dev.remixwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 지금 곡 + 전체 목록(지금 곡 강조)을 보여 주는 큰 위젯 */
public class BigWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, android.os.Bundle options) {
        WidgetUpdater.updateAll(context);
    }
}
