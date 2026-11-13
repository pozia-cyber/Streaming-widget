package dev.remixwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/** 모든 위젯 종류의 공통 동작: 추가·갱신·크기 변경 때 다시 그립니다. */
public abstract class BaseWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        WidgetUpdater.updateAll(context);
    }
}
