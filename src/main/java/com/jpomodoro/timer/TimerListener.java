package com.jpomodoro.timer;

import java.time.Instant;

public interface TimerListener {
    void onTick(SessionType type, int remainingSeconds, int totalSeconds, int cycleCount);

    /**
     * @param completedSessionId id de la session terminée (focus ou pause), null si reset/skip pré-IDLE
     * @param completedStart timestamp de début (approximatif si reconstruit côté timer)
     * @param completedEnd timestamp de fin (now)
     */
    void onTransition(SessionType from, SessionType to, int cycleCount,
                      Long completedSessionId, Instant completedStart, Instant completedEnd);

    void onStateChanged();
}
