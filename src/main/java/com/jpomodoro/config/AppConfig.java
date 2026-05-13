package com.jpomodoro.config;

import com.jpomodoro.timer.SessionType;

public record AppConfig(
        TimerSettings timer,
        ScheduleSettings schedule,
        AiSettings ai,
        NotificationSettings notification,
        WebhookSettings webhook,
        AppearanceSettings appearance,
        boolean onboarded
) {

    public record TimerSettings(
            int focusMinutes,
            int shortBreakMinutes,
            int longBreakMinutes,
            int cyclesBeforeLongBreak
    ) {
        public int focusSeconds() { return focusMinutes * 60; }
        public int shortBreakSeconds() { return shortBreakMinutes * 60; }
        public int longBreakSeconds() { return longBreakMinutes * 60; }

        public int secondsFor(SessionType type) {
            return switch (type) {
                case FOCUS -> focusSeconds();
                case SHORT_BREAK -> shortBreakSeconds();
                case LONG_BREAK -> longBreakSeconds();
            };
        }
    }

    public record ScheduleSettings(
            String morningStart,
            String morningEnd,
            String afternoonStart,
            String afternoonEnd,
            String mode
    ) {}

    public record AiSettings(
            boolean enabled,
            String model,
            String endpoint,
            int timeoutSeconds
    ) {}

    public record NotificationSettings(
            boolean enabled,
            String sound
    ) {}

    public record WebhookSettings(
            String url,
            boolean enabled
    ) {}

    public record AppearanceSettings(
            String palette
    ) {}
}
