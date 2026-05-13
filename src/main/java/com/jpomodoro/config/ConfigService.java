package com.jpomodoro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

public final class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    private final AppPaths paths;
    private volatile AppConfig current;

    public ConfigService(AppPaths paths) {
        this.paths = paths;
        this.current = loadOrCreate();
    }

    public AppConfig get() {
        return current;
    }

    public synchronized void update(UnaryOperator<AppConfig> mutator) {
        AppConfig next = mutator.apply(current);
        save(next);
        current = next;
    }

    private AppConfig loadOrCreate() {
        paths.ensureDirectories();
        Path file = paths.config();
        if (!Files.exists(file)) {
            AppConfig defaults = ConfigDefaults.defaults();
            save(defaults);
            log.info("Config créée : {}", file);
            return defaults;
        }
        try {
            return parse(file);
        } catch (Exception e) {
            log.warn("Config illisible {}, retour aux défauts : {}", file, e.toString());
            return ConfigDefaults.defaults();
        }
    }

    private void save(AppConfig cfg) {
        paths.ensureDirectories();
        String content = TomlWriter.serialize(cfg);
        try {
            Files.writeString(paths.config(), content);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'écrire la config : " + paths.config(), e);
        }
    }

    private AppConfig parse(Path file) throws IOException {
        TomlParseResult toml = Toml.parse(file);
        if (toml.hasErrors()) {
            throw new IOException("TOML invalide : " + toml.errors().get(0).getMessage());
        }
        AppConfig d = ConfigDefaults.defaults();

        boolean onboarded = toml.getBoolean("onboarded", () -> d.onboarded());

        TomlTable t = toml.getTable("timer");
        AppConfig.TimerSettings timer = new AppConfig.TimerSettings(
                intOr(t, "focus_minutes", d.timer().focusMinutes()),
                intOr(t, "short_break_minutes", d.timer().shortBreakMinutes()),
                intOr(t, "long_break_minutes", d.timer().longBreakMinutes()),
                intOr(t, "cycles_before_long_break", d.timer().cyclesBeforeLongBreak())
        );

        TomlTable s = toml.getTable("schedule");
        AppConfig.ScheduleSettings schedule = new AppConfig.ScheduleSettings(
                strOr(s, "morning_start", d.schedule().morningStart()),
                strOr(s, "morning_end", d.schedule().morningEnd()),
                strOr(s, "afternoon_start", d.schedule().afternoonStart()),
                strOr(s, "afternoon_end", d.schedule().afternoonEnd()),
                strOr(s, "mode", d.schedule().mode())
        );

        TomlTable a = toml.getTable("ai");
        AppConfig.AiSettings ai = new AppConfig.AiSettings(
                boolOr(a, "enabled", d.ai().enabled()),
                strOr(a, "model", d.ai().model()),
                strOr(a, "endpoint", d.ai().endpoint()),
                intOr(a, "timeout_seconds", d.ai().timeoutSeconds())
        );

        TomlTable n = toml.getTable("notification");
        AppConfig.NotificationSettings notif = new AppConfig.NotificationSettings(
                boolOr(n, "enabled", d.notification().enabled()),
                strOr(n, "sound", d.notification().sound())
        );

        TomlTable w = toml.getTable("webhook");
        AppConfig.WebhookSettings webhook = new AppConfig.WebhookSettings(
                strOr(w, "url", d.webhook().url()),
                boolOr(w, "enabled", d.webhook().enabled())
        );

        TomlTable ap = toml.getTable("appearance");
        AppConfig.AppearanceSettings appearance = new AppConfig.AppearanceSettings(
                strOr(ap, "palette", d.appearance().palette())
        );

        return new AppConfig(timer, schedule, ai, notif, webhook, appearance, onboarded);
    }

    private static int intOr(TomlTable t, String key, int fallback) {
        if (t == null) return fallback;
        Long v = t.getLong(key);
        return v == null ? fallback : v.intValue();
    }

    private static String strOr(TomlTable t, String key, String fallback) {
        if (t == null) return fallback;
        String v = t.getString(key);
        return v == null ? fallback : v;
    }

    private static boolean boolOr(TomlTable t, String key, boolean fallback) {
        if (t == null) return fallback;
        Boolean v = t.getBoolean(key);
        return v == null ? fallback : v;
    }
}
