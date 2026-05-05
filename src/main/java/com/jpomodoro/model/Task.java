package com.jpomodoro.model;

import java.time.Instant;

public record Task(
        long id,
        String title,
        boolean done,
        Instant createdAt,
        Instant completedAt
) {
    public Task withDone(boolean newDone, Instant when) {
        return new Task(id, title, newDone, createdAt, newDone ? when : null);
    }
}
