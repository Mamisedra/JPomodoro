package com.jpomodoro.model;

import java.time.Instant;

public record Feedback(
        long id,
        long summaryId,
        boolean isPositive,
        Instant createdAt
) {}
