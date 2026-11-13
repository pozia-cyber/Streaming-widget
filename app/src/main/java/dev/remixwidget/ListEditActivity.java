package dev.remixwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 곡 목록을 직접 편집하는 화면. 한 줄에 한 곡: 제목 | 길이 | 부제(생략 가능) */
public class ListEditActivity extends Activity {
    private static final int CYCLE = 3600;

    private EditText input;
    private TextView summary;
    private Button saveBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad + dp(8), pad, pad);
        root.setBackgroundColor(Color.parseColor("#F3F8FC"));

        TextView title = new TextView(this);
        title.setText("곡 목록 직접 편집");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#0D1A26"));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("한 줄에 한 곡씩  제목 | 길이 | 부제  로 적으세요. 부제는 생략할 수 있어요.\n"
                + "예)  Star Trace | 2:59\n예)  Star Trace | 2:44 | Future House Remix\n"
                + "순서를 바꾸려면 줄을 잘라 붙이고, 지우려면 줄을 지우세요. 매 정각에 맨 위 곡부터 시작합니다.");
        help.setTextSize(13);
        help.setTextColor(Color.parseColor("#566B7F"));
        help.setPadding(0, dp(6), 0, dp(8));
        root.addView(help);

        input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setHorizontallyScrolling(false);
        input.setTextSize(14);
        input.setTextColor(Color.parseColor("#0D1A26"));
        input.setBackgroundColor(Color.WHITE);
        input.setPadding(dp(10), dp(10), dp(10), dp(10));
        input.setText(ListText.toText(ListStore.load(this)));
        root.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        summary = new TextView(this);
        summary.setTextSize(13);
        summary.setPadding(0, dp(8), 0, dp(4));
        root.addView(summary);

        saveBtn = button("저장하고 위젯에 적용", new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        root.addView(saveBtn);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button revert = button("공유 목록으로 되돌리기", new View.OnClickListener() {
            @Override public void onClick(View v) { confirmRevert(); }
        });
        Button copy = button("list.json 복사", new View.OnClickListener() {
            @Override public void onClick(View v) { copyJson(); }
        });
        row.addView(revert, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(row);

        setContentView(root);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable e) { validate(); }
        });
        validate();
    }

    /** 입력을 해석합니다. 틀리면 예외(메시지에 몇 번째 줄인지) */
    private Schedule parseInput() {
        return ListText.parse(input.getText().toString(), "내 목록", CYCLE);
    }

    private void validate() {
        try {
            Schedule s = parseInput();
            int total = ListText.totalSeconds(s);
            StringBuilder sb = new StringBuilder();
            sb.append(s.titles.length).append("곡 · 합계 ").append(ListText.formatSeconds(total));
            if (total == CYCLE) {
                sb.append(" · 정확히 1시간입니다");
            } else if (total < CYCLE) {
                sb.append(" · 정각 전 ").append(ListText.formatSeconds(CYCLE - total)).append(" 동안은 '대기'로 표시돼요");
            } else {
                sb.append(" · 마지막 곡이 정각에 ").append(total - CYCLE).append("초 잘려요");
                if (s.count < s.titles.length) {
                    sb.append(" (뒤의 ").append(s.titles.length - s.count).append("곡은 1시간 안에 시작하지 못해 위젯에서 제외)");
                }
            }
            summary.setText(sb.toString());
            summary.setTextColor(Color.parseColor("#0670A8"));
            saveBtn.setEnabled(true);
        } catch (IllegalArgumentException e) {
            summary.setText(e.getMessage());
            summary.setTextColor(Color.parseColor("#C62828"));
            saveBtn.setEnabled(false);
        }
    }

    private void save() {
        Schedule s;
        try {
            s = parseInput();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        String err = ListStore.saveCustom(this, s);
        if (err != null) {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            return;
        }
        WidgetUpdater.updateAll(this);
        Toast.makeText(this, "저장했어요. 홈 화면 위젯에 바로 반영됩니다", Toast.LENGTH_LONG).show();
        finish();
    }

    private void confirmRevert() {
        new AlertDialog.Builder(this)
                .setMessage("직접 편집한 목록을 지우고 공유 목록(저장소의 list.json)으로 되돌릴까요?")
                .setPositiveButton("되돌리기", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        ListStore.clearCustom(ListEditActivity.this);
                        WidgetUpdater.updateAll(ListEditActivity.this);
                        Toast.makeText(ListEditActivity.this, "공유 목록으로 되돌렸어요", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void copyJson() {
        try {
            String json = ListStore.toJson(parseInput());
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("list.json", json));
            Toast.makeText(this, "복사했어요. GitHub 의 list.json 에 붙여 넣으면 모두에게 적용돼요", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "먼저 입력 오류를 고쳐 주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
