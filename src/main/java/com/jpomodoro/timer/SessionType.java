package com.jpomodoro.timer;

public enum SessionType {
    FOCUS("FOCUS"),
    SHORT_BREAK("PAUSE"),
    LONG_BREAK("PAUSE LONGUE");

    private final String label;

    SessionType(String label) {
        this.label = label;
    }

    public String label() { return label; }
    public boolean isBreak() { return this != FOCUS; }
}
