package com.jpomodoro.schedule;

public enum ScheduleMode {
    AUTO,
    MANUAL;

    public static ScheduleMode parse(String value) {
        if (value == null) return MANUAL;
        try {
            return ScheduleMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MANUAL;
        }
    }
}
