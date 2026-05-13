package com.jpomodoro.stats;

public final class Sparkline {

    private static final char[] BARS = {'▁','▂','▃','▄','▅','▆','▇','█'};

    private Sparkline() {}

    public static String render(int[] values) {
        if (values == null || values.length == 0) return "";
        int max = 0;
        for (int v : values) if (v > max) max = v;
        StringBuilder sb = new StringBuilder(values.length);
        for (int v : values) {
            if (max == 0) {
                sb.append(BARS[0]);
            } else {
                int level = (int) Math.round((double) v / max * (BARS.length - 1));
                sb.append(BARS[Math.max(0, Math.min(BARS.length - 1, level))]);
            }
        }
        return sb.toString();
    }
}
