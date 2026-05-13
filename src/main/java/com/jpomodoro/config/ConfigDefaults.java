package com.jpomodoro.config;

public final class ConfigDefaults {

    private ConfigDefaults() {}

    public static AppConfig defaults() {
        return new AppConfig(
                new AppConfig.TimerSettings(25, 5, 15, 4),
                new AppConfig.ScheduleSettings("10:00", "13:00", "14:00", "19:00", "MANUAL"),
                new AppConfig.AiSettings(true, "qwen2.5:7b", "http://localhost:11434", 55),
                new AppConfig.NotificationSettings(true, "default"),
                new AppConfig.WebhookSettings("", false),
                new AppConfig.AppearanceSettings("default"),
                false
        );
    }
}
