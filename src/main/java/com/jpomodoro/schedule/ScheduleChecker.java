package com.jpomodoro.schedule;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public final class ScheduleChecker {

    private ScheduleChecker() {}

    public static boolean inWorkHours(WorkHours hours, LocalTime now) {
        return inRange(now, hours.morningStart(), hours.morningEnd())
                || inRange(now, hours.afternoonStart(), hours.afternoonEnd());
    }

    private static boolean inRange(LocalTime t, LocalTime start, LocalTime end) {
        return !t.isBefore(start) && t.isBefore(end);
    }

    /**
     * Prochaine frontière (début ou fin de créneau) à partir de {@code now}.
     * Renvoie la première limite future parmi les 4 horaires, en gérant le wrap minuit.
     */
    public static Optional<LocalDateTime> nextBoundary(WorkHours hours, LocalDateTime now) {
        LocalDateTime[] candidates = {
                now.toLocalDate().atTime(hours.morningStart()),
                now.toLocalDate().atTime(hours.morningEnd()),
                now.toLocalDate().atTime(hours.afternoonStart()),
                now.toLocalDate().atTime(hours.afternoonEnd())
        };
        LocalDateTime best = null;
        for (LocalDateTime c : candidates) {
            LocalDateTime effective = c.isAfter(now) ? c : c.plusDays(1);
            if (best == null || effective.isBefore(best)) best = effective;
        }
        return Optional.ofNullable(best);
    }
}
