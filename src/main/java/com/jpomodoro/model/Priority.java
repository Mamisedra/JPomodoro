package com.jpomodoro.model;

public enum Priority {
    LOW("L"),
    NORMAL("N"),
    HIGH("H");

    private final String marker;

    Priority(String marker) {
        this.marker = marker;
    }

    public String marker() { return marker; }

    public Priority next() {
        return switch (this) {
            case LOW -> NORMAL;
            case NORMAL -> HIGH;
            case HIGH -> LOW;
        };
    }

    public static Priority parse(String value) {
        if (value == null) return NORMAL;
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
