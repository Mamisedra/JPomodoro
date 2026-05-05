package com.jpomodoro.model;

import com.jpomodoro.timer.SessionType;

import java.time.Instant;

public record Session(
        long id,
        SessionType type,
        Long taskId,
        Instant startedAt,
        Instant endedAt,
        boolean completed
) {}
