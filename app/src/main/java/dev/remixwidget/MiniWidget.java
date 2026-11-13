package dev.remixwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** 가로 2칸(2x1) 초소형 위젯: 곡 제목 + 재생 위치 + 종료까지 */
public class MiniWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUpdater.updateAll(context);
    }
}
