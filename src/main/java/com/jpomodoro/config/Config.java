package com.jpomodoro.config;

/**
 * Valeurs par défaut historiques (référencées par {@link ConfigDefaults}).
 * À runtime, les durées proviennent de {@link ConfigService}.
 */
public final class Config {
    public static final int FOCUS_MINUTES = 25;
    public static final int SHORT_BREAK_MINUTES = 5;
    public static final int LONG_BREAK_MINUTES = 15;
    public static final int CYCLES_BEFORE_LONG_BREAK = 4;

    private Config() {}
}
