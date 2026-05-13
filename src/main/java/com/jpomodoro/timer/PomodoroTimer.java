package com.jpomodoro.timer;

import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.notify.Notifier;
import com.jpomodoro.schedule.ScheduleChecker;
import com.jpomodoro.schedule.ScheduleMode;
import com.jpomodoro.schedule.WorkHours;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PomodoroTimer {

    public enum State { IDLE, RUNNING, PAUSED }

    private final SessionRepository sessions;
    private final ConfigService config;
    private final Notifier notifier;
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

    private ScheduleMode mode;
    private boolean autoHold = false;
    private Clock clock = Clock.systemDefaultZone();

    public PomodoroTimer(SessionRepository sessions, ConfigService config, Notifier notifier) {
        this.sessions = sessions;
        this.config = config;
        this.notifier = notifier;
        this.mode = ScheduleMode.parse(config.get().schedule().mode());
        this.remainingSeconds = timerSettings().secondsFor(SessionType.FOCUS);
    }

    public void setListener(TimerListener listener) {
        this.listener = listener;
    }

    public void setActiveTaskId(Long taskId) {
        this.activeTaskId = taskId;
    }

    public synchronized void setClock(Clock clock) {
        this.clock = clock;
    }

    public synchronized ScheduleMode mode() {
        return mode;
    }

    public synchronized void setMode(ScheduleMode newMode) {
        this.mode = newMode;
        if (newMode == ScheduleMode.MANUAL) autoHold = false;
        fireStateChanged();
    }

    public synchronized boolean autoHold() {
        return autoHold;
    }

    public synchronized Optional<LocalDateTime> nextBoundary() {
        WorkHours hours = WorkHours.from(config.get().schedule());
        return ScheduleChecker.nextBoundary(hours, LocalDateTime.now(clock));
    }

    public synchronized void start() {
        if (state == State.RUNNING) return;
        if (state == State.IDLE && currentType == SessionType.FOCUS && shouldAutoPause()) {
            autoHold = true;
            fireStateChanged();
            return;
        }
        autoHold = false;
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
        autoHold = false;
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
        boolean held;
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

            if (to == SessionType.FOCUS && shouldAutoPause()) {
                autoHold = true;
                state = State.IDLE;
                held = true;
            } else {
                autoHold = false;
                currentSessionId = sessions.start(to, to == SessionType.FOCUS ? activeTaskId : null);
                state = State.RUNNING;
                scheduleTicker();
                held = false;
            }
        }
        if (naturalEnd) {
            if (from == SessionType.FOCUS) notifier.notifyFocusEnded();
            else notifier.notifyBreakEnded();
        }
        if (listener != null) {
            listener.onTransition(from, to, cycle);
            listener.onStateChanged();
        }
    }

    private boolean shouldAutoPause() {
        if (mode != ScheduleMode.AUTO) return false;
        WorkHours hours = WorkHours.from(config.get().schedule());
        return !ScheduleChecker.inWorkHours(hours, LocalDateTime.now(clock).toLocalTime());
    }

    private AppConfig.TimerSettings timerSettings() {
        return config.get().timer();
    }

    private void fireStateChanged() {
        if (listener != null) listener.onStateChanged();
    }
}
