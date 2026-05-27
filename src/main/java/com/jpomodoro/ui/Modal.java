package com.jpomodoro.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class Modal {

    private Modal() {}

    public static String readLine(Screen screen, String prompt, String initial) throws IOException {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int boxW = Math.min(size.getColumns() - 4, Math.max(60, prompt.length() + 30));
        int boxH = 5;
        int x = (size.getColumns() - boxW) / 2;
        int y = (size.getRows() - boxH) / 2;

        TextGraphics g = screen.newTextGraphics();
        clear(g, x, y, boxW, boxH);
        drawBorder(g, x, y, boxW, boxH, " Saisie ");
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x + 2, y + 1, prompt);
        screen.refresh();

        StringBuilder sb = new StringBuilder(initial == null ? "" : initial);
        int inputY = y + 2;
        int visibleLen = boxW - 4;
        screen.setCursorPosition(new TerminalPosition(x + 2, inputY));

        while (true) {
            String value = sb.toString();
            String visible = value.length() > visibleLen
                    ? value.substring(value.length() - visibleLen)
                    : value;
            redrawInput(g, x, inputY, visible, visibleLen);
            int cursorOffset = Math.min(visible.length(), visibleLen - 1);
            screen.setCursorPosition(new TerminalPosition(x + 2 + cursorOffset, inputY));
            screen.refresh();
            KeyStroke k = screen.readInput();
            if (k.getKeyType() == KeyType.Escape) {
                screen.setCursorPosition(null);
                return null;
            }
            if (k.getKeyType() == KeyType.Enter) {
                screen.setCursorPosition(null);
                return sb.toString();
            }
            if (k.getKeyType() == KeyType.Backspace && sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            } else if (k.getCharacter() != null) {
                char c = k.getCharacter();
                if (c >= 0x20 && c != 0x7F) {
                    sb.append(c);
                }
            }
        }
    }

    public static boolean confirm(Screen screen, String prompt) throws IOException {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int boxW = Math.min(size.getColumns() - 4, Math.max(50, prompt.length() + 10));
        int boxH = 5;
        int x = (size.getColumns() - boxW) / 2;
        int y = (size.getRows() - boxH) / 2;

        TextGraphics g = screen.newTextGraphics();
        clear(g, x, y, boxW, boxH);
        drawBorder(g, x, y, boxW, boxH, " Confirmation ");
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x + 2, y + 1, prompt);
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x + 2, y + 3, "[y] oui   [n] non   [Esc] annuler");
        screen.refresh();

        while (true) {
            KeyStroke k = screen.readInput();
            if (k.getKeyType() == KeyType.Escape) return false;
            Character ch = k.getCharacter();
            if (ch == null) continue;
            char c = Character.toLowerCase(ch);
            if (c == 'y' || c == 'o') return true;
            if (c == 'n') return false;
        }
    }

    public static Optional<Integer> choose(Screen screen, String title, List<String> options) throws IOException {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int boxW = Math.min(size.getColumns() - 4, Math.max(40, title.length() + 10));
        int boxH = Math.min(size.getRows() - 4, options.size() + 4);
        int x = (size.getColumns() - boxW) / 2;
        int y = (size.getRows() - boxH) / 2;

        TextGraphics g = screen.newTextGraphics();
        int idx = 0;

        while (true) {
            screen.clear();
            clear(g, x, y, boxW, boxH);
            drawBorder(g, x, y, boxW, boxH, " " + title + " ");
            for (int i = 0; i < options.size() && i < boxH - 3; i++) {
                String line = (i == idx ? "▶ " : "  ") + options.get(i);
                if (i == idx) {
                    g.setForegroundColor(TextColor.ANSI.BLACK);
                    g.setBackgroundColor(TextColor.ANSI.WHITE);
                } else {
                    g.setForegroundColor(TextColor.ANSI.WHITE);
                    g.setBackgroundColor(TextColor.ANSI.BLACK);
                }
                g.putString(x + 2, y + 1 + i, padRight(line, boxW - 4));
            }
            g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
            g.putString(x + 2, y + boxH - 2, "[↑↓] nav  [Enter] valider  [Esc] annuler");
            screen.refresh();

            KeyStroke k = screen.readInput();
            if (k.getKeyType() == KeyType.Escape) return Optional.empty();
            if (k.getKeyType() == KeyType.Enter) return Optional.of(idx);
            if (k.getKeyType() == KeyType.ArrowUp) idx = (idx - 1 + options.size()) % options.size();
            else if (k.getKeyType() == KeyType.ArrowDown) idx = (idx + 1) % options.size();
        }
    }

    public static void info(Screen screen, String title, List<String> lines) throws IOException {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int width = lines.stream().mapToInt(String::length).max().orElse(40);
        int boxW = Math.min(size.getColumns() - 4, Math.max(50, width + 6));
        int boxH = Math.min(size.getRows() - 4, lines.size() + 4);
        int x = (size.getColumns() - boxW) / 2;
        int y = (size.getRows() - boxH) / 2;

        TextGraphics g = screen.newTextGraphics();
        clear(g, x, y, boxW, boxH);
        drawBorder(g, x, y, boxW, boxH, " " + title + " ");
        g.setForegroundColor(TextColor.ANSI.WHITE);
        for (int i = 0; i < lines.size() && i < boxH - 3; i++) {
            g.putString(x + 2, y + 1 + i, lines.get(i));
        }
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x + 2, y + boxH - 2, "[Entrée/Esc] fermer");
        screen.refresh();

        while (true) {
            KeyStroke k = screen.readInput();
            KeyType type = k.getKeyType();
            if (type == KeyType.Escape || type == KeyType.Enter) return;
        }
    }

    private static void redrawInput(TextGraphics g, int x, int y, String value, int maxLen) {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x + 2, y, padRight(value, maxLen));
    }

    private static void clear(TextGraphics g, int x, int y, int w, int h) {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        for (int i = 0; i < h; i++) {
            g.putString(x, y + i, padRight("", w));
        }
    }

    private static void drawBorder(TextGraphics g, int x, int y, int w, int h, String title) {
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.drawLine(x, y, x + w - 1, y, '─');
        g.drawLine(x, y + h - 1, x + w - 1, y + h - 1, '─');
        g.drawLine(x, y, x, y + h - 1, '│');
        g.drawLine(x + w - 1, y, x + w - 1, y + h - 1, '│');
        g.setCharacter(x, y, '┌');
        g.setCharacter(x + w - 1, y, '┐');
        g.setCharacter(x, y + h - 1, '└');
        g.setCharacter(x + w - 1, y + h - 1, '┘');
        if (!title.isEmpty()) g.putString(x + 2, y, title, SGR.BOLD);
    }

    private static String padRight(String s, int w) {
        if (s.length() >= w) return s.substring(0, w);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }
}
