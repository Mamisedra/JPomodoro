package com.jpomodoro.config;

final class TomlWriter {

    private TomlWriter() {}

    static String serialize(AppConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Configuration JPomodoro — éditable à la main\n\n");

        sb.append("onboarded = ").append(c.onboarded()).append("\n\n");

        sb.append("[timer]\n");
        sb.append("focus_minutes = ").append(c.timer().focusMinutes()).append("\n");
        sb.append("short_break_minutes = ").append(c.timer().shortBreakMinutes()).append("\n");
        sb.append("long_break_minutes = ").append(c.timer().longBreakMinutes()).append("\n");
        sb.append("cycles_before_long_break = ").append(c.timer().cyclesBeforeLongBreak()).append("\n\n");

        sb.append("[schedule]\n");
        sb.append("morning_start = ").append(quote(c.schedule().morningStart())).append("\n");
        sb.append("morning_end = ").append(quote(c.schedule().morningEnd())).append("\n");
        sb.append("afternoon_start = ").append(quote(c.schedule().afternoonStart())).append("\n");
        sb.append("afternoon_end = ").append(quote(c.schedule().afternoonEnd())).append("\n");
        sb.append("mode = ").append(quote(c.schedule().mode())).append("\n\n");

        sb.append("[ai]\n");
        sb.append("enabled = ").append(c.ai().enabled()).append("\n");
        sb.append("model = ").append(quote(c.ai().model())).append("\n");
        sb.append("endpoint = ").append(quote(c.ai().endpoint())).append("\n");
        sb.append("timeout_seconds = ").append(c.ai().timeoutSeconds()).append("\n\n");

        sb.append("[notification]\n");
        sb.append("enabled = ").append(c.notification().enabled()).append("\n");
        sb.append("sound = ").append(quote(c.notification().sound())).append("\n\n");

        sb.append("[webhook]\n");
        sb.append("url = ").append(quote(c.webhook().url())).append("\n");
        sb.append("enabled = ").append(c.webhook().enabled()).append("\n\n");

        sb.append("[appearance]\n");
        sb.append("palette = ").append(quote(c.appearance().palette())).append("\n");

        return sb.toString();
    }

    private static String quote(String s) {
        if (s == null) s = "";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
