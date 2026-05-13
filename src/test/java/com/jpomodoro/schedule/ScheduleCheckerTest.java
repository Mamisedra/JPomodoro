package com.jpomodoro.schedule;

import com.jpomodoro.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleCheckerTest {

    private final WorkHours hours = WorkHours.from(new AppConfig.ScheduleSettings(
            "10:00", "13:00", "14:00", "19:00", "MANUAL"));

    @Test
    void inWorkHoursMorning() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(11, 30))).isTrue();
    }

    @Test
    void inWorkHoursAfternoon() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(15, 0))).isTrue();
    }

    @Test
    void outsideWorkHoursLunch() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(13, 30))).isFalse();
    }

    @Test
    void outsideWorkHoursMorning() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(9, 0))).isFalse();
    }

    @Test
    void outsideWorkHoursEvening() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(20, 0))).isFalse();
    }

    @Test
    void boundaryStartIsInclusive() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(10, 0))).isTrue();
    }

    @Test
    void boundaryEndIsExclusive() {
        assertThat(ScheduleChecker.inWorkHours(hours, LocalTime.of(13, 0))).isFalse();
    }

    @Test
    void nextBoundaryDuringMorning() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 13, 11, 30);
        Optional<LocalDateTime> next = ScheduleChecker.nextBoundary(hours, now);
        assertThat(next).contains(LocalDateTime.of(2026, 5, 13, 13, 0));
    }

    @Test
    void nextBoundaryDuringLunch() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 13, 13, 30);
        Optional<LocalDateTime> next = ScheduleChecker.nextBoundary(hours, now);
        assertThat(next).contains(LocalDateTime.of(2026, 5, 13, 14, 0));
    }

    @Test
    void nextBoundaryAfterWorkWrapsToTomorrow() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 13, 20, 0);
        Optional<LocalDateTime> next = ScheduleChecker.nextBoundary(hours, now);
        assertThat(next).contains(LocalDateTime.of(2026, 5, 14, 10, 0));
    }

    @Test
    void scheduleModeParse() {
        assertThat(ScheduleMode.parse("AUTO")).isEqualTo(ScheduleMode.AUTO);
        assertThat(ScheduleMode.parse("auto")).isEqualTo(ScheduleMode.AUTO);
        assertThat(ScheduleMode.parse("manual")).isEqualTo(ScheduleMode.MANUAL);
        assertThat(ScheduleMode.parse("xxx")).isEqualTo(ScheduleMode.MANUAL);
        assertThat(ScheduleMode.parse(null)).isEqualTo(ScheduleMode.MANUAL);
    }
}
