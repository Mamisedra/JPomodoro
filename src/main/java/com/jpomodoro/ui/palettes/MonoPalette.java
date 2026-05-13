package com.jpomodoro.ui.palettes;

import com.googlecode.lanterna.TextColor;
import com.jpomodoro.ui.Palette;

public final class MonoPalette implements Palette {
    @Override public TextColor focus() { return TextColor.ANSI.WHITE_BRIGHT; }
    @Override public TextColor shortBreak() { return TextColor.ANSI.WHITE; }
    @Override public TextColor longBreak() { return TextColor.ANSI.WHITE; }
    @Override public TextColor accent() { return TextColor.ANSI.WHITE_BRIGHT; }
    @Override public TextColor dim() { return TextColor.ANSI.BLACK_BRIGHT; }
    @Override public TextColor highlight() { return TextColor.ANSI.WHITE_BRIGHT; }
    @Override public String name() { return "mono"; }
}
