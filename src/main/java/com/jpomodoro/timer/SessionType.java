package com.jpomodoro.timer;

import com.jpomodoro.config.Config;

public enum SessionType {
    FOCUS(Config.FOCUS_SECONDS, "FOCUS"),
    SHORT_BREAK(Config.SHORT_BREAK_SECONDS, "PAUSE"),
    LONG_BREAK(Config.LONG_BREAK_SECONDS, "PAUSE LONGUE");

    private final int durationSeconds;
    private final String label;

    SessionType(int durationSeconds, String label) {
        this.durationSeconds = durationSeconds;
        this.label = label;
    }

    public int durationSeconds() { return durationSeconds; }
    public String label() { return label; }
    public boolean isBreak() { return this != FOCUS; }
}
