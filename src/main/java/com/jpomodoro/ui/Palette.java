package com.jpomodoro.ui;

import com.googlecode.lanterna.TextColor;

public interface Palette {
    TextColor focus();
    TextColor shortBreak();
    TextColor longBreak();
    TextColor accent();
    TextColor dim();
    TextColor highlight();
    String name();
}
