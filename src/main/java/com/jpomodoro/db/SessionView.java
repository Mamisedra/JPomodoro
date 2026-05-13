package com.jpomodoro.db;

import com.jpomodoro.timer.SessionType;

import java.time.Instant;

public record SessionView(
        long id,
        SessionType type,
        Instant startedAt,
        Instant endedAt,
        boolean completed,
        Long taskId,
        String taskTitle,
        String summary,
        Boolean feedback
) {
    public long durationSeconds() {
        if (startedAt == null || endedAt == null) return 0;
        return Math.max(0, endedAt.getEpochSecond() - startedAt.getEpochSecond());
    }
}
