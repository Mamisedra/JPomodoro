package com.jpomodoro.schedule;

import com.jpomodoro.config.AppConfig;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public record WorkHours(
        LocalTime morningStart,
        LocalTime morningEnd,
        LocalTime afternoonStart,
        LocalTime afternoonEnd
) {

    public static WorkHours from(AppConfig.ScheduleSettings s) {
        return new WorkHours(
                parse(s.morningStart(), LocalTime.of(10, 0)),
                parse(s.morningEnd(), LocalTime.of(13, 0)),
                parse(s.afternoonStart(), LocalTime.of(14, 0)),
                parse(s.afternoonEnd(), LocalTime.of(19, 0))
        );
    }

    private static LocalTime parse(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
