package com.jpomodoro.timer;

import com.jpomodoro.TestSupport;
import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.AppPaths;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.Database;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.notify.NoopNotifier;
import com.jpomodoro.schedule.ScheduleMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PomodoroTimerScheduleTest {

    private ConfigService config;

    @BeforeEach
    void setup() throws Exception {
        resetDb();
        AppPaths paths = TestSupport.tempPaths();
        Database.open(paths.database());
        config = new ConfigService(paths);
    }

    @AfterEach
    void teardown() throws Exception {
        resetDb();
    }

    @Test
    void autoModeOutsideWorkHoursPreventsStart() {
        config.update(c -> withAutoMode(c));
        PomodoroTimer timer = newTimer(at("2026-05-13T20:00:00"));

        timer.start();

        assertThat(timer.state()).isEqualTo(PomodoroTimer.State.IDLE);
        assertThat(timer.autoHold()).isTrue();
    }

    @Test
    void autoModeInsideWorkHoursStartsNormally() {
        config.update(c -> withAutoMode(c));
        PomodoroTimer timer = newTimer(at("2026-05-13T11:30:00"));

        timer.start();

        assertThat(timer.state()).isEqualTo(PomodoroTimer.State.RUNNING);
        assertThat(timer.autoHold()).isFalse();
    }

    @Test
    void manualModeIgnoresSchedule() {
        PomodoroTimer timer = newTimer(at("2026-05-13T20:00:00"));

        timer.start();

        assertThat(timer.state()).isEqualTo(PomodoroTimer.State.RUNNING);
    }

    @Test
    void toggleAutoToManualClearsHold() {
        config.update(c -> withAutoMode(c));
        PomodoroTimer timer = newTimer(at("2026-05-13T20:00:00"));
        timer.start();
        assertThat(timer.autoHold()).isTrue();

        timer.setMode(ScheduleMode.MANUAL);

        assertThat(timer.autoHold()).isFalse();
        timer.start();
        assertThat(timer.state()).isEqualTo(PomodoroTimer.State.RUNNING);
    }

    @Test
    void nextBoundaryReflectsConfig() {
        PomodoroTimer timer = newTimer(at("2026-05-13T11:30:00"));

        assertThat(timer.nextBoundary())
                .contains(LocalDateTime.of(2026, 5, 13, 13, 0));
    }

    private PomodoroTimer newTimer(Clock clock) {
        PomodoroTimer t = new PomodoroTimer(new SessionRepository(), config, new NoopNotifier());
        t.setClock(clock);
        return t;
    }

    private static Clock at(String iso) {
        return Clock.fixed(LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }

    private static AppConfig withAutoMode(AppConfig c) {
        return new AppConfig(
                c.timer(),
                new AppConfig.ScheduleSettings(
                        c.schedule().morningStart(), c.schedule().morningEnd(),
                        c.schedule().afternoonStart(), c.schedule().afternoonEnd(),
                        "AUTO"),
                c.ai(), c.notification(), c.webhook(), c.appearance(), c.onboarded());
    }

    private static void resetDb() throws Exception {
        Method m = Database.class.getDeclaredMethod("resetForTests");
        m.setAccessible(true);
        m.invoke(null);
    }
}
