package dev.remixwidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 사람이 편집하기 쉬운 목록 텍스트 형식을 읽고 씁니다. 안드로이드에 의존하지 않습니다.
 *
 * 한 줄에 한 곡:   제목 | 길이 | 부제(생략 가능)
 *   Star Trace | 2:59
 *   Star Trace | 2:44 | Future House Remix by jeonghyeon
 * 길이는 2:59 처럼 분:초 (또는 179 처럼 초). 빈 줄과 # 로 시작하는 줄은 무시합니다.
 */
public final class ListText {
    private ListText() {}

    public static Schedule parse(String text, String name, int cycle) {
        List<String> titles = new ArrayList<>();
        List<String> subs = new ArrayList<>();
        List<Integer> secs = new ArrayList<>();
        String[] lines = text.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\|", 3);
            int ln = i + 1;
            if (p.length < 2) throw new IllegalArgumentException(ln + "번째 줄: '제목 | 2:59' 형식으로 입력하세요");
            String title = p[0].trim();
            if (title.isEmpty()) throw new IllegalArgumentException(ln + "번째 줄: 제목이 비어 있습니다");
            int sec;
            try {
                sec = parseSeconds(p[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(ln + "번째 줄: 길이는 2:59 처럼 입력하세요 (입력한 값: " + p[1].trim() + ")");
            }
            if (sec <= 0) throw new IllegalArgumentException(ln + "번째 줄: 길이가 0보다 커야 합니다");
            titles.add(title);
            subs.add(p.length > 2 ? p[2].trim() : "");
            secs.add(sec);
        }
        if (titles.isEmpty()) throw new IllegalArgumentException("곡을 한 곡 이상 입력하세요");
        int n = titles.size();
        int[] s = new int[n];
        for (int i = 0; i < n; i++) s[i] = secs.get(i);
        return new Schedule(name, titles.toArray(new String[0]), subs.toArray(new String[0]), s, cycle);
    }

    /** "2:59", "1:02:03", "179" → 초. 형식이 틀리면 NumberFormatException */
    public static int parseSeconds(String v) {
        String[] p = v.split(":", -1);
        if (p.length > 3) throw new NumberFormatException(v);
        int total = 0;
        for (int i = 0; i < p.length; i++) {
            String x = p[i].trim();
            if (x.isEmpty()) throw new NumberFormatException(v);
            int n = Integer.parseInt(x);
            if (n < 0) throw new NumberFormatException(v);
            if (i > 0 && n >= 60) throw new NumberFormatException(v);
            total = total * 60 + n;
        }
        return total;
    }

    public static String formatSeconds(int s) {
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    public static String toText(Schedule s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.titles.length; i++) {
            sb.append(s.titles[i]).append(" | ").append(formatSeconds(s.seconds[i]));
            if (!s.subs[i].isEmpty()) sb.append(" | ").append(s.subs[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    public static int totalSeconds(Schedule s) {
        int t = 0;
        for (int v : s.seconds) t += v;
        return t;
    }
}
