package com.jpomodoro.ui.palettes;

import com.googlecode.lanterna.TextColor;
import com.jpomodoro.ui.Palette;

public final class SynthwavePalette implements Palette {
    @Override public TextColor focus() { return TextColor.ANSI.MAGENTA_BRIGHT; }
    @Override public TextColor shortBreak() { return TextColor.ANSI.CYAN_BRIGHT; }
    @Override public TextColor longBreak() { return TextColor.ANSI.BLUE_BRIGHT; }
    @Override public TextColor accent() { return TextColor.ANSI.YELLOW_BRIGHT; }
    @Override public TextColor dim() { return TextColor.ANSI.BLACK_BRIGHT; }
    @Override public TextColor highlight() { return TextColor.ANSI.MAGENTA_BRIGHT; }
    @Override public String name() { return "synthwave"; }
}
