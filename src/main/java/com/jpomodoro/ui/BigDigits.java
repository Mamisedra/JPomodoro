package com.jpomodoro.ui;

import com.googlecode.lanterna.graphics.TextGraphics;

public final class BigDigits {

    public static final int HEIGHT = 5;

    private static final String[][] GLYPHS = new String[11][];
    static {
        GLYPHS[0] = new String[]{
                "█████",
                "█   █",
                "█   █",
                "█   █",
                "█████"
        };
        GLYPHS[1] = new String[]{
                "  █  ",
                " ██  ",
                "  █  ",
                "  █  ",
                "█████"
        };
        GLYPHS[2] = new String[]{
                "█████",
                "    █",
                "█████",
                "█    ",
                "█████"
        };
        GLYPHS[3] = new String[]{
                "█████",
                "    █",
                " ████",
                "    █",
                "█████"
        };
        GLYPHS[4] = new String[]{
                "█   █",
                "█   █",
                "█████",
                "    █",
                "    █"
        };
        GLYPHS[5] = new String[]{
                "█████",
                "█    ",
                "█████",
                "    █",
                "█████"
        };
        GLYPHS[6] = new String[]{
                "█████",
                "█    ",
                "█████",
                "█   █",
                "█████"
        };
        GLYPHS[7] = new String[]{
                "█████",
                "    █",
                "   █ ",
                "  █  ",
                "  █  "
        };
        GLYPHS[8] = new String[]{
                "█████",
                "█   █",
                "█████",
                "█   █",
                "█████"
        };
        GLYPHS[9] = new String[]{
                "█████",
                "█   █",
                "█████",
                "    █",
                "█████"
        };
        // colon (index 10) — 2 cols wide, padded to keep alignment helper simple
        GLYPHS[10] = new String[]{
                "  ",
                "██",
                "  ",
                "██",
                "  "
        };
    }

    private static final int DIGIT_WIDTH = 5;
    private static final int COLON_WIDTH = 2;
    private static final int SPACING = 1;

    /** Width in columns of the rendered string "MM:SS" (5 chars). */
    public static int widthForMmSs() {
        // 4 digits + 1 colon + 4 spacings between them
        return DIGIT_WIDTH * 4 + COLON_WIDTH + SPACING * 4;
    }

    /** Draws a "MM:SS" big-font time at (x, y). The string must be 5 chars long. */
    public static void drawMmSs(TextGraphics g, int x, int y, String mmss) {
        if (mmss.length() != 5) return;
        int cursor = x;
        for (int i = 0; i < mmss.length(); i++) {
            char c = mmss.charAt(i);
            int idx = (c == ':') ? 10 : (c - '0');
            if (idx < 0 || idx > 10) continue;
            String[] glyph = GLYPHS[idx];
            int w = (idx == 10) ? COLON_WIDTH : DIGIT_WIDTH;
            for (int row = 0; row < HEIGHT; row++) {
                g.putString(cursor, y + row, glyph[row]);
            }
            cursor += w + SPACING;
        }
    }

    private BigDigits() {}
}
