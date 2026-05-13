package com.jpomodoro.ui.modal;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.jpomodoro.db.FeedbackRepository;
import com.jpomodoro.model.Summary;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class BreakModal {

    private static final String[] SPINNER = {"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};

    private BreakModal() {}

    public static void open(Screen screen,
                             CompletableFuture<Optional<Summary>> future,
                             FeedbackRepository feedbackRepo) throws IOException {
        int frame = 0;
        Optional<Summary> summary = Optional.empty();
        boolean done = false;

        while (true) {
            screen.clear();
            TerminalSize size = screen.getTerminalSize();
            int boxW = Math.min(size.getColumns() - 4, 80);
            int boxH = Math.min(size.getRows() - 4, 14);
            if (boxW < 30 || boxH < 6) return;
            int x = (size.getColumns() - boxW) / 2;
            int y = (size.getRows() - boxH) / 2;

            TextGraphics g = screen.newTextGraphics();
            clear(g, x, y, boxW, boxH);
            drawBorder(g, x, y, boxW, boxH, " Pause méritée ");

            g.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
            g.putString(x + 2, y + 1, "Souffle un peu — éloigne-toi de l'écran.", SGR.BOLD);

            if (!done && future.isDone()) {
                try { summary = future.get(); } catch (Exception ignored) { summary = Optional.empty(); }
                done = true;
            }

            if (!done) {
                g.setForegroundColor(TextColor.ANSI.YELLOW);
                g.putString(x + 2, y + 3, SPINNER[frame] + "  Génération du résumé via Ollama…");
                frame = (frame + 1) % SPINNER.length;
            } else if (summary.isEmpty()) {
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.putString(x + 2, y + 3, "(IA indisponible — repose-toi tranquillement.)");
            } else {
                g.setForegroundColor(TextColor.ANSI.WHITE);
                renderWrapped(g, x + 2, y + 3, boxW - 4, boxH - 5, summary.get().content());
            }

            g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
            String footer = (done && summary.isPresent())
                    ? "[y] 👍   [n] 👎   [Esc/Entrée] fermer"
                    : "[Esc/Entrée] fermer";
            g.putString(x + 2, y + boxH - 2, footer);

            screen.refresh();

            KeyStroke k = screen.pollInput();
            if (k != null) {
                KeyType type = k.getKeyType();
                if (type == KeyType.Escape || type == KeyType.Enter) return;
                Character ch = k.getCharacter();
                if (ch != null && done && summary.isPresent()) {
                    char c = Character.toLowerCase(ch);
                    if (c == 'y' || c == 'o') {
                        feedbackRepo.save(summary.get().id(), true);
                        return;
                    }
                    if (c == 'n') {
                        feedbackRepo.save(summary.get().id(), false);
                        return;
                    }
                }
            }
            try { Thread.sleep(150); } catch (InterruptedException ignored) { return; }
        }
    }

    private static void renderWrapped(TextGraphics g, int x, int y, int w, int h, String text) {
        String[] paragraphs = text.split("\n");
        int row = 0;
        for (String p : paragraphs) {
            if (row >= h) break;
            String remaining = p;
            while (!remaining.isEmpty() && row < h) {
                int len = Math.min(w, remaining.length());
                if (len < remaining.length()) {
                    int spaceAt = remaining.lastIndexOf(' ', len);
                    if (spaceAt > w / 2) len = spaceAt;
                }
                g.putString(x, y + row, remaining.substring(0, len));
                remaining = remaining.substring(len).stripLeading();
                row++;
            }
        }
    }

    private static void clear(TextGraphics g, int x, int y, int w, int h) {
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        for (int i = 0; i < h; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < w; j++) sb.append(' ');
            g.putString(x, y + i, sb.toString());
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
        g.putString(x + 2, y, title, SGR.BOLD);
    }
}
