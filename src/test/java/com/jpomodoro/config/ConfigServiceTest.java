package com.jpomodoro.config;

import com.jpomodoro.TestSupport;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServiceTest {

    @Test
    void createsDefaultConfigOnFirstRun() throws Exception {
        AppPaths paths = TestSupport.tempPaths();

        ConfigService service = new ConfigService(paths);

        assertThat(Files.exists(paths.config())).isTrue();
        assertThat(service.get().timer().focusMinutes()).isEqualTo(25);
        assertThat(service.get().timer().cyclesBeforeLongBreak()).isEqualTo(4);
        assertThat(service.get().ai().enabled()).isTrue();
        assertThat(service.get().onboarded()).isFalse();
    }

    @Test
    void roundTripPreservesValues() throws Exception {
        AppPaths paths = TestSupport.tempPaths();

        ConfigService first = new ConfigService(paths);
        first.update(c -> new AppConfig(
                new AppConfig.TimerSettings(45, 10, 20, 3),
                c.schedule(),
                c.ai(),
                c.notification(),
                c.webhook(),
                c.appearance(),
                true
        ));

        ConfigService reloaded = new ConfigService(paths);

        assertThat(reloaded.get().timer().focusMinutes()).isEqualTo(45);
        assertThat(reloaded.get().timer().shortBreakMinutes()).isEqualTo(10);
        assertThat(reloaded.get().timer().longBreakMinutes()).isEqualTo(20);
        assertThat(reloaded.get().timer().cyclesBeforeLongBreak()).isEqualTo(3);
        assertThat(reloaded.get().onboarded()).isTrue();
    }

    @Test
    void invalidTomlFallsBackToDefaults() throws Exception {
        AppPaths paths = TestSupport.tempPaths();
        paths.ensureDirectories();
        Files.writeString(paths.config(), "this is not valid = toml = [");

        ConfigService service = new ConfigService(paths);

        assertThat(service.get().timer().focusMinutes()).isEqualTo(25);
    }

    @Test
    void partialTomlMergesWithDefaults() throws Exception {
        AppPaths paths = TestSupport.tempPaths();
        paths.ensureDirectories();
        Files.writeString(paths.config(), """
                [timer]
                focus_minutes = 50
                """);

        ConfigService service = new ConfigService(paths);

        assertThat(service.get().timer().focusMinutes()).isEqualTo(50);
        assertThat(service.get().timer().shortBreakMinutes()).isEqualTo(5);
        assertThat(service.get().ai().model()).isEqualTo("qwen2.5:7b");
    }
}
