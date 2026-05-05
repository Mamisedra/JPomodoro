package com.jpomodoro.timer;

public interface TimerListener {
    void onTick(SessionType type, int remainingSeconds, int totalSeconds, int cycleCount);
    void onTransition(SessionType from, SessionType to, int cycleCount);
    void onStateChanged();
}
