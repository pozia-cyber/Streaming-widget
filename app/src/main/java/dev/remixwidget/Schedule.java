package dev.remixwidget;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * 매 정각(00분 00초)에 1번 곡이 시작된다는 기준으로, 지금 어떤 곡이 나와야 하는지 계산합니다.
 * 안드로이드에 의존하지 않아서 PC에서도 테스트할 수 있습니다.
 */
public final class Schedule {

    /** 지금 시각의 계산 결과 */
    public static final class State {
        public int index;        // 현재 곡 번호(0부터)
        public int next;         // 다음 곡 번호(0부터)
        public long withinMs;    // 현재 곡 재생 위치
        public long lengthMs;    // 현재 곡 길이(정각에 잘리는 경우 줄어든 길이)
        public long remainMs;    // 곡 종료(또는 다음 시작)까지 남은 시간
        public long trackEndMs;  // 곡이 끝나는 실제 시각(epoch ms)
        public boolean gap;      // 목록이 한 시간보다 짧아서 다음 정각을 기다리는 중
    }

    public final String name;
    public final String[] titles;
    public final String[] subs;
    public final int[] seconds;   // 원래 곡 길이
    public final int cycle;       // 한 바퀴 길이(초). 보통 3600
    public final int[] starts;    // 각 곡 시작 위치(초)
    public final int[] eff;       // 한 바퀴 안에서 실제로 재생되는 길이(초)
    public final int count;       // 한 바퀴 안에서 시작하는 곡 수

    public Schedule(String name, String[] titles, String[] subs, int[] seconds, int cycle) {
        if (titles.length == 0) throw new IllegalArgumentException("곡이 하나도 없습니다");
        if (cycle <= 0) throw new IllegalArgumentException("cycleSeconds 는 0보다 커야 합니다");
        this.name = name;
        this.titles = titles;
        this.subs = subs;
        this.seconds = seconds;
        this.cycle = cycle;
        int n = titles.length;
        starts = new int[n];
        eff = new int[n];
        int acc = 0, c = 0;
        for (int i = 0; i < n; i++) {
            if (seconds[i] <= 0) throw new IllegalArgumentException((i + 1) + "번 곡 길이가 0 이하입니다");
            starts[i] = acc;
            eff[i] = Math.max(0, Math.min(seconds[i], cycle - acc));
            if (acc < cycle) c = i + 1;
            acc += seconds[i];
        }
        count = c;
    }

    public State at(long nowMs, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(nowMs);
        long posMs = ((cal.get(Calendar.MINUTE) * 60L) + cal.get(Calendar.SECOND)) * 1000L
                + cal.get(Calendar.MILLISECOND);
        long cycleMs = cycle * 1000L;
        posMs %= cycleMs;

        State st = new State();
        int idx = 0;
        while (idx < count - 1 && posMs >= (starts[idx] + (long) eff[idx]) * 1000L) idx++;
        long endOfIdx = (starts[idx] + (long) eff[idx]) * 1000L;
        st.index = idx;
        st.next = (idx + 1) % count;
        st.lengthMs = eff[idx] * 1000L;
        if (posMs >= endOfIdx) {                      // 마지막 곡이 끝났고 다음 정각 전
            st.gap = true;
            st.withinMs = st.lengthMs;
            st.remainMs = cycleMs - posMs;
            st.next = 0;
        } else {
            st.withinMs = posMs - starts[idx] * 1000L;
            st.remainMs = st.lengthMs - st.withinMs;
        }
        st.trackEndMs = nowMs + st.remainMs;
        return st;
    }
}
