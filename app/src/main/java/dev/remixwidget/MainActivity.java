package dev.remixwidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** 설정 화면: 목록 새로고침, 새 앱 받기, 정확한 알람 허용, 현재 목록 보기 */
public class MainActivity extends Activity {
    private TextView status;
    private TextView listView;
    private Button refreshBtn;
    private Button alarmBtn;
    private Button batteryBtn;
    private TextView hint;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad + dp(8), pad, pad);
        root.setBackgroundColor(Color.parseColor("#F3F8FC"));

        TextView title = new TextView(this);
        title.setText("REMIX 스밍 위젯");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#0D1A26"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("홈 화면을 길게 눌러 위젯에서 'REMIX 스밍'을 추가하세요. 매 정각에 1번 곡이 시작된다고 보고 지금 곡을 자동으로 보여 줍니다. 폰의 날짜 및 시간은 '자동'으로 두세요.");
        help.setTextSize(14);
        help.setTextColor(Color.parseColor("#566B7F"));
        help.setPadding(0, dp(8), 0, dp(12));
        root.addView(help);

        status = new TextView(this);
        status.setTextSize(14);
        status.setTextColor(Color.parseColor("#0D1A26"));
        status.setPadding(0, 0, 0, dp(8));
        root.addView(status);

        root.addView(button("곡 목록 직접 편집", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, ListEditActivity.class)); }
        }));

        root.addView(button("위젯 모양 설정 (색·글자 크기)", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        }));

        refreshBtn = button("목록 새로고침", new View.OnClickListener() {
            @Override public void onClick(View v) { refresh(true); }
        });
        root.addView(refreshBtn);

        root.addView(button("새 버전 앱 받기 (다시 다운로드)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.APK_URL)));
                } catch (Exception e) {
                    status.setText("브라우저를 열 수 없습니다");
                }
            }
        }));

        alarmBtn = button("정확한 알람 허용하기 (곡 전환이 늦을 때)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                    }
                }
            }
        });
        root.addView(alarmBtn);

        batteryBtn = button("배터리 사용 제한 해제 (곡이 안 넘어갈 때)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    } catch (Exception e2) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                    }
                }
            }
        });
        root.addView(batteryBtn);

        hint = new TextView(this);
        hint.setTextSize(12);
        hint.setTextColor(Color.parseColor("#566B7F"));
        hint.setPadding(0, dp(6), 0, 0);
        root.addView(hint);

        TextView listTitle = new TextView(this);
        listTitle.setText("현재 목록 (위젯이 쓰는 목록)");
        listTitle.setTextSize(13);
        listTitle.setTextColor(Color.parseColor("#566B7F"));
        listTitle.setPadding(0, dp(12), 0, dp(4));
        root.addView(listTitle);

        listView = new TextView(this);
        listView.setTextSize(14);
        listView.setTextColor(Color.parseColor("#0D1A26"));
        listView.setLineSpacing(0, 1.15f);
        ScrollView sv = new ScrollView(this);
        sv.addView(listView);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showList();
        boolean exact = WidgetUpdater.canScheduleExact(this);
        boolean batteryOk = isIgnoringBatteryOptimizations();
        alarmBtn.setVisibility(exact ? View.GONE : View.VISIBLE);
        batteryBtn.setVisibility(batteryOk ? View.GONE : View.VISIBLE);
        if (exact && batteryOk) {
            hint.setVisibility(View.GONE);
        } else {
            hint.setVisibility(View.VISIBLE);
            hint.setText("곡이 바뀌는데 위젯이 늦게 따라오면 위 버튼으로 허용해 주세요."
                    + "\n삼성 폰: 설정 → 배터리 → 백그라운드 사용 제한 에서 '사용하지 않는 앱을 절전 상태로 전환'을 끄고, REMIX 스밍을 '절전 모드 안 함' 목록에 넣어 주세요.");
        }
        long last = ListStore.lastRefreshMs(this);
        // 열 때마다, 마지막 새로고침이 1시간 이상 지났으면 조용히 새로 받아 봅니다
        if (System.currentTimeMillis() - last > 3600_000L) refresh(false);
        WidgetUpdater.updateAll(this);
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void refresh(final boolean verbose) {
        refreshBtn.setEnabled(false);
        if (verbose) status.setText("목록을 받는 중…");
        new Thread(new Runnable() {
            @Override public void run() {
                final String err = ListStore.refresh(MainActivity.this);
                ui.post(new Runnable() {
                    @Override public void run() {
                        refreshBtn.setEnabled(true);
                        if (err == null) {
                            WidgetUpdater.updateAll(MainActivity.this);
                            showList();
                        } else if (verbose) {
                            status.setText("새로고침 실패: " + err + "\n(기존 목록을 그대로 쓰고 있어요)");
                        }
                    }
                });
            }
        }).start();
    }

    private void showList() {
        Schedule s = ListStore.load(this);
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (int i = 0; i < s.titles.length; i++) {
            total += s.seconds[i];
            sb.append(String.format(Locale.KOREA, "%02d  %s%s  %d:%02d\n", i + 1, s.titles[i],
                    s.subs[i].isEmpty() ? "" : " (" + s.subs[i] + ")", s.seconds[i] / 60, s.seconds[i] % 60));
        }
        listView.setText(sb.toString());
        long last = ListStore.lastRefreshMs(this);
        String when = last == 0 ? "받은 적 없음(앱에 들어 있는 기본 목록)"
                : new SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(new Date(last));
        String ver = ListStore.lastVersion(this);
        if (ListStore.isCustom(this)) {
            status.setText("내 목록(직접 편집) · " + s.titles.length + "곡 · 총 " + (total / 60) + "분 " + (total % 60) + "초"
                    + "\n내 목록을 쓰는 중이라 '목록 새로고침'을 해도 바뀌지 않아요. 공유 목록으로 돌아가려면 '곡 목록 직접 편집'에서 되돌리기를 누르세요.");
        } else {
            status.setText(s.name + " · " + s.titles.length + "곡 · 총 " + (total / 60) + "분 " + (total % 60) + "초"
                    + "\n마지막 새로고침: " + when + (ver.isEmpty() ? "" : " (목록 버전 " + ver + ")"));
        }
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        b.setLayoutParams(lp);
        b.setGravity(Gravity.CENTER);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
