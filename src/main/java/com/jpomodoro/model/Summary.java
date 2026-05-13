package com.jpomodoro.model;

import java.time.Instant;

public record Summary(
        long id,
        long sessionId,
        String content,
        String model,
        Instant createdAt
) {}
