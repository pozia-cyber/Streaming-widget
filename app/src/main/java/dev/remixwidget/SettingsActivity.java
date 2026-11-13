package dev.remixwidget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** 위젯 모양 설정: 색 테마, 제목·시간 글자 크기, 표시할 줄. 바꾸는 즉시 홈 화면 위젯에 적용됩니다. */
public class SettingsActivity extends Activity {
    // 미리보기(작은 위젯 모양)
    private LinearLayout card;
    private TextView pvChip, pvTitle, pvSub, pvPos, pvLbl, pvRemain, pvNext;
    private View pvDiv;

    private TextView[] themeTiles;
    private TextView[] titleBtns;
    private TextView[] timeBtns;

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
        help.setText("바꾸는 즉시 홈 화면 위젯에 적용돼요. 위쪽 미리보기는 '작게' 위젯 기준입니다.");
        help.setTextSize(13);
        help.setTextColor(Color.parseColor("#566B7F"));
        help.setPadding(0, dp(6), 0, dp(10));
        root.addView(help);

        // 미리보기: 어두운 배경 위에 올려서 반투명 테마도 보이게
        LinearLayout stage = new LinearLayout(this);
        stage.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable stageBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#2B5C84"), Color.parseColor("#0F2236")});
        stageBg.setCornerRadius(dp(16));
        stage.setBackground(stageBg);
        buildPreview();
        stage.addView(card, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(stage);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);

        body.addView(label("색 테마"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeTiles = new TextView[Themes.ALL.length];
        for (int i = 0; i < Themes.ALL.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackgroundResource(Themes.ALL[i].bg);
            tv.setTextColor(Themes.ALL[i].ink);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { Prefs.setTheme(SettingsActivity.this, idx); changed(); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(84), dp(56));
            lp.rightMargin = dp(8);
            themeRow.addView(tv, lp);
            themeTiles[i] = tv;
        }
        hs.addView(themeRow);
        body.addView(hs);

        body.addView(label("제목 글자 크기 (제목·부제·목록)"));
        titleBtns = scaleRow(body, true);
        body.addView(label("시간 글자 크기 (재생 위치·종료까지)"));
        timeBtns = scaleRow(body, false);

        body.addView(label("표시할 내용"));
        body.addView(toggle("NEXT(다음 곡) 줄 보이기 — 작게 위젯", Prefs.showNext(this), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { Prefs.setShowNext(SettingsActivity.this, on); changed(); }
        }));
        body.addView(toggle("곡 부제(리믹스 정보) 보이기 — 작게·목록 위젯", Prefs.showSub(this), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { Prefs.setShowSub(SettingsActivity.this, on); changed(); }
        }));
        body.addView(toggle("곡 번호 표시(NOW 19 / 20) 보이기", Prefs.showIdx(this), new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton v, boolean on) { Prefs.setShowIdx(SettingsActivity.this, on); changed(); }
        }));

        TextView note = new TextView(this);
        note.setText("글자를 '크게' 이상으로 키우면 가로 2칸 위젯은 높이가 모자라 잘릴 수 있어요. 그땐 위젯을 길게 눌러 세로로 늘려 주세요.");
        note.setTextSize(12);
        note.setTextColor(Color.parseColor("#566B7F"));
        note.setPadding(0, dp(10), 0, dp(6));
        body.addView(note);

        Button reset = new Button(this);
        reset.setText("기본 모양으로 되돌리기");
        reset.setAllCaps(false);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.resetAppearance(SettingsActivity.this);
                recreate();
                WidgetUpdater.updateAll(SettingsActivity.this);
            }
        });
        body.addView(reset);

        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        refreshAll();
    }

    private void buildPreview() {
        card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));

        pvChip = new TextView(this);
        pvChip.setText("NOW  19 / 20");
        pvChip.setTextSize(10);
        pvChip.setTypeface(null, Typeface.BOLD);
        pvChip.setPadding(dp(9), dp(2), dp(9), dp(2));
        card.addView(pvChip, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        pvTitle = new TextView(this);
        pvTitle.setText("푸른 하늘에 단비");
        pvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pvTitle.setPadding(0, dp(7), 0, 0);
        card.addView(pvTitle);

        pvSub = new TextView(this);
        pvSub.setText("Future House Remix by jeonghyeon");
        card.addView(pvSub);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.BOTTOM);
        pvPos = new TextView(this);
        pvPos.setText("01:23");
        pvPos.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        timeRow.addView(pvPos, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        right.setPadding(0, 0, 0, dp(8));
        pvLbl = new TextView(this);
        pvLbl.setText("종료까지");
        pvLbl.setTextSize(10);
        right.addView(pvLbl);
        pvRemain = new TextView(this);
        pvRemain.setText("02:15");
        pvRemain.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        right.addView(pvRemain);
        timeRow.addView(right);
        card.addView(timeRow);

        pvDiv = new View(this);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dl.topMargin = dp(2);
        dl.bottomMargin = dp(7);
        card.addView(pvDiv, dl);

        pvNext = new TextView(this);
        pvNext.setText("NEXT  꾼");
        card.addView(pvNext);
    }

    private void refreshAll() {
        Themes.T t = Themes.get(Prefs.theme(this));
        float ts = Prefs.scale(Prefs.titleScale(this));
        float ms = Prefs.scale(Prefs.timeScale(this));

        card.setBackgroundResource(t.bg);
        pvChip.setBackgroundResource(t.chip);
        pvChip.setTextColor(t.accent);
        pvChip.setVisibility(Prefs.showIdx(this) ? View.VISIBLE : View.GONE);
        pvTitle.setTextColor(t.ink);
        pvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20 * ts);
        pvSub.setTextColor(t.mute);
        pvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11 * ts);
        pvSub.setVisibility(Prefs.showSub(this) ? View.VISIBLE : View.GONE);
        pvPos.setTextColor(t.ink);
        pvPos.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44 * ms);
        pvLbl.setTextColor(t.mute);
        pvRemain.setTextColor(t.accent);
        pvRemain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * ms);
        boolean next = Prefs.showNext(this);
        pvDiv.setBackgroundColor(t.div);
        pvDiv.setVisibility(next ? View.VISIBLE : View.GONE);
        pvNext.setTextColor(t.mute);
        pvNext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * ts);
        pvNext.setVisibility(next ? View.VISIBLE : View.GONE);

        int cur = Prefs.theme(this);
        for (int i = 0; i < themeTiles.length; i++) {
            themeTiles[i].setText((i == cur ? "✓ " : "") + Themes.ALL[i].name);
        }
        styleScale(titleBtns, Prefs.titleScale(this));
        styleScale(timeBtns, Prefs.timeScale(this));
    }

    private void changed() {
        refreshAll();
        WidgetUpdater.updateAll(this);
    }

    private TextView[] scaleRow(LinearLayout parent, final boolean forTitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView[] out = new TextView[Prefs.SCALE_NAMES.length];
        for (int i = 0; i < out.length; i++) {
            final int step = i;
            TextView tv = new TextView(this);
            tv.setText(Prefs.SCALE_NAMES[i]);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(13);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (forTitle) Prefs.setTitleScale(SettingsActivity.this, step);
                    else Prefs.setTimeScale(SettingsActivity.this, step);
                    changed();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.rightMargin = dp(6);
            row.addView(tv, lp);
            out[i] = tv;
        }
        parent.addView(row);
        return out;
    }

    private void styleScale(TextView[] btns, int selected) {
        for (int i = 0; i < btns.length; i++) {
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(dp(12));
            boolean on = i == selected;
            d.setColor(on ? Color.parseColor("#0B86C6") : Color.parseColor("#E4E8EC"));
            btns[i].setBackground(d);
            btns[i].setTextColor(on ? Color.WHITE : Color.parseColor("#0D1A26"));
            btns[i].setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
        }
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
