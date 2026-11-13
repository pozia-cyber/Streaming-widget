package dev.remixwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 지금 곡만 보여 주는 작은 위젯 */
public class SmallWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUpdater.updateAll(context);
    }
}
