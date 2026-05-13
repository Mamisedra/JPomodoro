package com.jpomodoro.notify;

import com.jpomodoro.TestSupport;
import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.ConfigService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SoundPlayerTest {

    @Test
    void exposesFourPresets() {
        assertThat(SoundPlayer.presets()).containsExactlyInAnyOrder("bell", "chime", "ding", "soft");
    }

    @Test
    void presetsResourcesArePresent() {
        for (String name : SoundPlayer.presets()) {
            assertThat(SoundPlayerTest.class.getResource("/sounds/" + name + ".wav"))
                    .as("preset %s.wav présent dans le classpath", name)
                    .isNotNull();
        }
    }

    @Test
    void noneSoundIsSilentAndDoesNotThrow() throws Exception {
        ConfigService cfg = new ConfigService(TestSupport.tempPaths());
        cfg.update(c -> new AppConfig(
                c.timer(), c.schedule(), c.ai(),
                new AppConfig.NotificationSettings(true, "none"),
                c.webhook(), c.appearance(), c.onboarded()
        ));
        SoundPlayer player = new SoundPlayer(cfg);
        assertThatCode(player::play).doesNotThrowAnyException();
    }

    @Test
    void unknownNameFallsBackWithoutThrowing() throws Exception {
        ConfigService cfg = new ConfigService(TestSupport.tempPaths());
        SoundPlayer player = new SoundPlayer(cfg);
        assertThatCode(() -> player.play("does-not-exist")).doesNotThrowAnyException();
    }
}
