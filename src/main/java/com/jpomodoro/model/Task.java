package com.jpomodoro.model;

import java.time.Instant;

public record Task(
        long id,
        String title,
        boolean done,
        Priority priority,
        Integer estimatedPomodoros,
        Instant createdAt,
        Instant completedAt,
        Instant deletedAt
) {
    public Task withDone(boolean newDone, Instant when) {
        return new Task(id, title, newDone, priority, estimatedPomodoros, createdAt,
                newDone ? when : null, deletedAt);
    }

    public Task withPriority(Priority newPriority) {
        return new Task(id, title, done, newPriority, estimatedPomodoros, createdAt, completedAt, deletedAt);
    }

    public Task withEstimate(Integer estimate) {
        return new Task(id, title, done, priority, estimate, createdAt, completedAt, deletedAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
