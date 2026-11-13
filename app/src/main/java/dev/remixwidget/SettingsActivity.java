package dev.remixwidget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.TimeZone;

/**
 * 위젯 모양 설정. 위젯 종류(가로 2칸 / 작게 / 목록 포함 / 가로 4칸)를 고르면
 * 그 위젯의 배치와 글자 크기를 따로 바꿀 수 있고, 위쪽 미리보기는 실제 위젯과 같은 코드로 그려집니다.
 * 바꾸는 즉시(슬라이더는 손을 뗄 때) 홈 화면 위젯에 적용됩니다.
 */
public class SettingsActivity extends Activity {
    private static final int[] PREVIEW_H_DP = {84, 190, 250, 84};   // 위젯 종류별 미리보기 높이

    private int kind = Prefs.KIND_SMALL;
    private FrameLayout previewBox;
    private LinearLayout controls;
    private TextView[] kindBtns;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad + dp(8), pad, pad);
        root.setBackgroundColor(Color.parseColor("#F3F8FC"));

        TextView title = new TextView(this);
        title.setText("위젯 모양 설정");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#0D1A26"));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("고칠 위젯을 고르면 그 위젯의 배치와 글자 크기를 따로 바꿀 수 있어요.");
        help.setTextSize(13);
        help.setTextColor(Color.parseColor("#566B7F"));
        help.setPadding(0, dp(6), 0, dp(8));
        root.addView(help);

        // 위젯 종류 선택
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout kindRow = new LinearLayout(this);
        kindRow.setOrientation(LinearLayout.HORIZONTAL);
        kindBtns = new TextView[Prefs.KIND_COUNT];
        for (int i = 0; i < Prefs.KIND_COUNT; i++) {
            final int k = i;
            TextView tv = new TextView(this);
            tv.setText(Prefs.KIND_NAMES[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { kind = k; rebuildAll(); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
            lp.rightMargin = dp(8);
            kindRow.addView(tv, lp);
            kindBtns[i] = tv;
        }
        hs.addView(kindRow);
        root.addView(hs);

        // 미리보기 무대: 어두운 배경 위에 올려서 반투명 테마도 보이게
        LinearLayout stage = new LinearLayout(this);
        stage.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable stageBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#2B5C84"), Color.parseColor("#0F2236")});
        stageBg.setCornerRadius(dp(16));
        stage.setBackground(stageBg);
        previewBox = new FrameLayout(this);
        stage.addView(previewBox, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stageLp.topMargin = dp(10);
        root.addView(stage, stageLp);

        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(controls);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        rebuildAll();
    }

    private void rebuildAll() {
        styleKinds();
        rebuildPreview();
        buildControls();
    }

    private void styleKinds() {
        for (int i = 0; i < kindBtns.length; i++) styleChoice(kindBtns[i], i == kind);
    }

    /** 선택된 위젯 종류·배치로 실제 레이아웃을 불러와 지금 곡을 채워 보여 줍니다. */
    private void rebuildPreview() {
        previewBox.removeAllViews();
        View v = LayoutInflater.from(this).inflate(Layouts.layout(kind, Prefs.variant(this, kind)), previewBox, false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(PREVIEW_H_DP[kind]));
        previewBox.addView(v, lp);
        repaintPreview();
    }

    private void repaintPreview() {
        if (previewBox.getChildCount() == 0) return;
        Schedule s = ListStore.load(this);
        Schedule.State st = s.at(System.currentTimeMillis(), TimeZone.getDefault());
        Renderer.render(this, new Renderer.ViewSink(previewBox.getChildAt(0)), kind, s, st, SystemClock.elapsedRealtime());
    }

    /** 설정이 바뀌었을 때: 미리보기 갱신 + 홈 화면 위젯 갱신 */
    private void changed() {
        repaintPreview();
        WidgetUpdater.updateAll(this);
    }

    private void buildControls() {
        controls.removeAllViews();

        controls.addView(label("배치 — " + Prefs.KIND_NAMES[kind] + " 위젯"));
        int cur = Prefs.variant(this, kind);
        final TextView[] vb = new TextView[Layouts.count(kind)];
        for (int i = 0; i < vb.length; i++) {
            final int v = i;
            TextView tv = new TextView(this);
            tv.setText(Layouts.NAMES[kind][i]);
            tv.setTextSize(14);
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View x) {
                    Prefs.setVariant(SettingsActivity.this, kind, v);
                    for (int j = 0; j < vb.length; j++) styleChoice(vb[j], j == v);
                    rebuildPreview();
                    WidgetUpdater.updateAll(SettingsActivity.this);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
            lp.bottomMargin = dp(6);
            controls.addView(tv, lp);
            vb[i] = tv;
            styleChoice(tv, i == cur);
        }

        controls.addView(label("글자 크기 — " + Prefs.KIND_NAMES[kind] + " 위젯"));
        controls.addView(slider("제목", Prefs.titlePct(this, kind), new PctSetter() {
            @Override public void set(int pct) { Prefs.setTitlePct(SettingsActivity.this, kind, pct); }
        }));
        controls.addView(slider("시간 (재생 위치·종료까지)", Prefs.timePct(this, kind), new PctSetter() {
            @Override public void set(int pct) { Prefs.setTimePct(SettingsActivity.this, kind, pct); }
        }));
        controls.addView(slider("보조 글자 (부제·NEXT·번호·'종료까지')", Prefs.subPct(this, kind), new PctSetter() {
            @Override public void set(int pct) { Prefs.setSubPct(SettingsActivity.this, kind, pct); }
        }));
        if (kind == Prefs.KIND_BIG) {
            controls.addView(slider("곡 목록 줄", Prefs.listPct(this), new PctSetter() {
                @Override public void set(int pct) { Prefs.setListPct(SettingsActivity.this, pct); }
            }));
        }
        Button resetKind = new Button(this);
        resetKind.setText("이 위젯 배치·크기를 기본값으로");
        resetKind.setAllCaps(false);
        resetKind.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.resetKind(SettingsActivity.this, kind);
                rebuildPreview();
                buildControls();
                WidgetUpdater.updateAll(SettingsActivity.this);
            }
        });
        controls.addView(resetKind);

        // ---- 모든 위젯 공통 ----
        controls.addView(label("색 테마 (모든 위젯)"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] tiles = new TextView[Themes.ALL.length];
        int curTheme = Prefs.theme(this);
        for (int i = 0; i < Themes.ALL.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackgroundResource(Themes.ALL[i].bg);
            tv.setTextColor(Themes.ALL[i].ink);
            tv.setText((i == curTheme ? "✓ " : "") + Themes.ALL[i].name);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.setTheme(SettingsActivity.this, idx);
                    for (int j = 0; j < tiles.length; j++) tiles[j].setText((j == idx ? "✓ " : "") + Themes.ALL[j].name);
                    changed();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(84), dp(56));
            lp.rightMargin = dp(8);
            themeRow.addView(tv, lp);
            tiles[i] = tv;
        }
        hs.addView(themeRow);
        controls.addView(hs);

        controls.addView(label("표시할 내용 (모든 위젯)"));
        controls.addView(toggle("NEXT(다음 곡) 줄 보이기 — 작게 위젯", Prefs.showNext(this), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { Prefs.setShowNext(SettingsActivity.this, on); changed(); }
        }));
        controls.addView(toggle("곡 부제(리믹스 정보) 보이기 — 작게·목록 위젯", Prefs.showSub(this), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { Prefs.setShowSub(SettingsActivity.this, on); changed(); }
        }));
        controls.addView(toggle("곡 번호 보이기 (NOW 19 / 20, 19/20)", Prefs.showIdx(this), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { Prefs.setShowIdx(SettingsActivity.this, on); changed(); }
        }));

        TextView note = new TextView(this);
        note.setText("미리보기는 실제 위젯과 같은 코드로 그리지만, 위젯을 홈 화면에서 늘리거나 줄인 크기와는 다를 수 있어요. 글자를 키웠는데 잘리면 위젯을 길게 눌러 크기를 늘려 주세요.");
        note.setTextSize(12);
        note.setTextColor(Color.parseColor("#566B7F"));
        note.setPadding(0, dp(10), 0, dp(6));
        controls.addView(note);

        Button resetAll = new Button(this);
        resetAll.setText("모든 모양 설정을 기본값으로");
        resetAll.setAllCaps(false);
        resetAll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.resetAppearance(SettingsActivity.this);
                rebuildPreview();
                buildControls();
                WidgetUpdater.updateAll(SettingsActivity.this);
            }
        });
        controls.addView(resetAll);
    }

    private interface PctSetter { void set(int pct); }

    /** 60%~260% 슬라이더. 움직이는 동안 미리보기만, 손을 떼면 홈 화면 위젯까지 반영합니다. */
    private View slider(String name, int pct, final PctSetter setter) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(4), 0, dp(4));
        final String nm = name;
        final TextView lab = new TextView(this);
        lab.setText(nm + "  " + pct + "%");
        lab.setTextSize(14);
        lab.setTextColor(Color.parseColor("#0D1A26"));
        box.addView(lab);
        SeekBar sb = new SeekBar(this);
        sb.setMax(Prefs.PCT_MAX - Prefs.PCT_MIN);
        sb.setProgress(pct - Prefs.PCT_MIN);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int v = (Math.round((p + Prefs.PCT_MIN) / 5f)) * 5;     // 5% 단위
                lab.setText(nm + "  " + v + "%");
                if (fromUser) { setter.set(v); repaintPreview(); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { WidgetUpdater.updateAll(SettingsActivity.this); }
        });
        box.addView(sb);
        return box;
    }

    private void styleChoice(TextView tv, boolean on) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(12));
        d.setColor(on ? Color.parseColor("#0B86C6") : Color.parseColor("#E4E8EC"));
        tv.setBackground(d);
        tv.setTextColor(on ? Color.WHITE : Color.parseColor("#0D1A26"));
        tv.setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor("#566B7F"));
        tv.setPadding(0, dp(16), 0, dp(6));
        return tv;
    }

    private Switch toggle(String text, boolean on, CompoundButton.OnCheckedChangeListener l) {
        Switch sw = new Switch(this);
        sw.setText(text);
        sw.setTextSize(14);
        sw.setTextColor(Color.parseColor("#0D1A26"));
        sw.setChecked(on);
        sw.setPadding(0, dp(8), 0, dp(8));
        sw.setOnCheckedChangeListener(l);
        return sw;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
