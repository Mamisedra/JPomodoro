package com.jpomodoro.ui;

import com.jpomodoro.ui.palettes.DefaultPalette;
import com.jpomodoro.ui.palettes.MonoPalette;
import com.jpomodoro.ui.palettes.SynthwavePalette;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaletteFactoryTest {

    @Test
    void mapsKnownNames() {
        assertThat(PaletteFactory.forName("default")).isInstanceOf(DefaultPalette.class);
        assertThat(PaletteFactory.forName("mono")).isInstanceOf(MonoPalette.class);
        assertThat(PaletteFactory.forName("synthwave")).isInstanceOf(SynthwavePalette.class);
    }

    @Test
    void unknownFallsBackToDefault() {
        assertThat(PaletteFactory.forName("zzz")).isInstanceOf(DefaultPalette.class);
        assertThat(PaletteFactory.forName(null)).isInstanceOf(DefaultPalette.class);
    }

    @Test
    void namesExposedForSettingsPicker() {
        assertThat(PaletteFactory.NAMES).containsExactly("default", "mono", "synthwave");
    }
}
