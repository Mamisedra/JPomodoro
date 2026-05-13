package com.jpomodoro.ui;

import com.jpomodoro.ui.palettes.DefaultPalette;
import com.jpomodoro.ui.palettes.MonoPalette;
import com.jpomodoro.ui.palettes.SynthwavePalette;

import java.util.List;

public final class PaletteFactory {

    public static final List<String> NAMES = List.of("default", "mono", "synthwave");

    private PaletteFactory() {}

    public static Palette forName(String name) {
        if (name == null) return new DefaultPalette();
        return switch (name.trim().toLowerCase()) {
            case "mono" -> new MonoPalette();
            case "synthwave" -> new SynthwavePalette();
            default -> new DefaultPalette();
        };
    }
}
