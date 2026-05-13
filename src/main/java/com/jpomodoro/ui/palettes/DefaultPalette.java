package com.jpomodoro.ui.palettes;

import com.googlecode.lanterna.TextColor;
import com.jpomodoro.ui.Palette;

public final class DefaultPalette implements Palette {
    @Override public TextColor focus() { return TextColor.ANSI.RED_BRIGHT; }
    @Override public TextColor shortBreak() { return TextColor.ANSI.GREEN_BRIGHT; }
    @Override public TextColor longBreak() { return TextColor.ANSI.CYAN_BRIGHT; }
    @Override public TextColor accent() { return TextColor.ANSI.YELLOW; }
    @Override public TextColor dim() { return TextColor.ANSI.BLACK_BRIGHT; }
    @Override public TextColor highlight() { return TextColor.ANSI.WHITE_BRIGHT; }
    @Override public String name() { return "default"; }
}
