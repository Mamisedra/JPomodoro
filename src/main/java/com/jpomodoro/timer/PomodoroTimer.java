package com.jpomodoro.timer;

import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.notify.Notifier;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PomodoroTimer {

    public enum State { IDLE, RUNNING, PAUSED }

    private final SessionRepository sessions;
    private final ConfigService config;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pomodoro-timer");
                t.setDaemon(true);
                return t;
            });

    private TimerListener listener;
    private SessionType currentType = SessionType.FOCUS;
    private int remainingSeconds;
    private int focusCyclesCompleted = 0;
    private State state = State.IDLE;
    private Long currentSessionId = null;
    private Long activeTaskId = null;
    private ScheduledFuture<?> ticker;

    public PomodoroTimer(SessionRepository sessions, ConfigService config) {
        this.sessions = sessions;
        this.config = config;
        this.remainingSeconds = timerSettings().secondsFor(SessionType.FOCUS);
    }

    public void setListener(TimerListener listener) {
        this.listener = listener;
    }

    public void setActiveTaskId(Long taskId) {
        this.activeTaskId = taskId;
    }

    public synchronized void start() {
        if (state == State.RUNNING) return;
        if (state == State.IDLE) {
            currentSessionId = sessions.start(currentType, currentType == SessionType.FOCUS ? activeTaskId : null);
        }
        state = State.RUNNING;
        scheduleTicker();
        fireStateChanged();
    }

    public synchronized void pause() {
        if (state != State.RUNNING) return;
        state = State.PAUSED;
        cancelTicker();
        fireStateChanged();
    }

    public synchronized void reset() {
        cancelTicker();
        if (currentSessionId != null) {
            sessions.cancel(currentSessionId);
            currentSessionId = null;
        }
        currentType = SessionType.FOCUS;
        remainingSeconds = timerSettings().secondsFor(currentType);
        focusCyclesCompleted = 0;
        state = State.IDLE;
        fireStateChanged();
    }

    public synchronized void skip() {
        if (state == State.IDLE) return;
        cancelTicker();
        completeCurrent(false);
    }

    public synchronized State state() { return state; }
    public synchronized SessionType currentType() { return currentType; }
    public synchronized int remainingSeconds() { return remainingSeconds; }
    public synchronized int totalSeconds() { return timerSettings().secondsFor(currentType); }
    public synchronized int focusCyclesCompleted() { return focusCyclesCompleted; }
    public synchronized int cycleSlot() {
        return (focusCyclesCompleted % cyclesBeforeLongBreak()) + 1;
    }
    public int cyclesBeforeLongBreak() {
        return timerSettings().cyclesBeforeLongBreak();
    }

    public void shutdown() {
        cancelTicker();
        scheduler.shutdownNow();
    }

    private void scheduleTicker() {
        ticker = scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    private void cancelTicker() {
        if (ticker != null) {
            ticker.cancel(false);
            ticker = null;
        }
    }

    private void tick() {
        SessionType type;
        int remaining;
        int total;
        int cycle;
        synchronized (this) {
            if (state != State.RUNNING) return;
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                completeCurrent(true);
                return;
            }
            type = currentType;
            remaining = remainingSeconds;
            total = timerSettings().secondsFor(currentType);
            cycle = cycleSlot();
        }
        if (listener != null) listener.onTick(type, remaining, total, cycle);
    }

    private void completeCurrent(boolean naturalEnd) {
        SessionType from;
        SessionType to;
        int cycle;
        synchronized (this) {
            from = currentType;
            if (currentSessionId != null) {
                if (naturalEnd) sessions.complete(currentSessionId);
                else sessions.cancel(currentSessionId);
                currentSessionId = null;
            }
            if (from == SessionType.FOCUS) {
                focusCyclesCompleted++;
                to = (focusCyclesCompleted % cyclesBeforeLongBreak() == 0)
                        ? SessionType.LONG_BREAK
                        : SessionType.SHORT_BREAK;
            } else {
                to = SessionType.FOCUS;
            }
            currentType = to;
            remainingSeconds = timerSettings().secondsFor(to);
            cycle = cycleSlot();
            currentSessionId = sessions.start(to, to == SessionType.FOCUS ? activeTaskId : null);
            state = State.RUNNING;
            scheduleTicker();
        }
        if (naturalEnd) {
            if (from == SessionType.FOCUS) Notifier.notifyFocusEnded();
            else Notifier.notifyBreakEnded();
        }
        if (listener != null) {
            listener.onTransition(from, to, cycle);
            listener.onStateChanged();
        }
    }

    private AppConfig.TimerSettings timerSettings() {
        return config.get().timer();
    }

    private void fireStateChanged() {
        if (listener != null) listener.onStateChanged();
    }
}
