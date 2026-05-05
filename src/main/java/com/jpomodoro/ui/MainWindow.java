package com.jpomodoro.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.jpomodoro.config.Config;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.db.TaskRepository;
import com.jpomodoro.model.Task;
import com.jpomodoro.timer.PomodoroTimer;
import com.jpomodoro.timer.SessionType;
import com.jpomodoro.timer.TimerListener;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWindow implements TimerListener {

    private enum FocusZone { TIMER, TASKS }

    private final Screen screen;
    private final PomodoroTimer timer;
    private final TaskRepository taskRepo;
    private final SessionRepository sessionRepo;

    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private volatile boolean running = true;

    private List<Task> tasks;
    private int selectedIndex = 0;
    private Long activeTaskId = null;
    private FocusZone focusZone = FocusZone.TIMER;
    private String statusMessage = "Bienvenue. Appuie sur [s] pour démarrer.";

    public MainWindow(Screen screen, PomodoroTimer timer,
                      TaskRepository taskRepo, SessionRepository sessionRepo) {
        this.screen = screen;
        this.timer = timer;
        this.taskRepo = taskRepo;
        this.sessionRepo = sessionRepo;
        this.tasks = taskRepo.listOpen();
        this.timer.setListener(this);
    }

    public void run() throws IOException {
        screen.startScreen();
        screen.setCursorPosition(null);
        try {
            while (running) {
                if (dirty.compareAndSet(true, false)) {
                    render();
                }
                KeyStroke key = screen.pollInput();
                if (key != null) {
                    handleKey(key);
                    dirty.set(true);
                } else {
                    try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                }
            }
        } finally {
            screen.stopScreen();
        }
    }

    private void handleKey(KeyStroke key) throws IOException {
        if (key.getKeyType() == KeyType.EOF) { running = false; return; }
        Character ch = key.getCharacter();

        if (ch != null) {
            switch (Character.toLowerCase(ch)) {
                case 'q' -> { running = false; return; }
                case 's' -> { timer.start(); statusMessage = "Timer démarré."; return; }
                case 'p' -> { timer.pause(); statusMessage = "Timer en pause."; return; }
                case 'r' -> { timer.reset(); statusMessage = "Timer réinitialisé."; return; }
                case 'n' -> { timer.skip(); statusMessage = "Session passée."; return; }
                case 'a' -> { promptAddTask(); return; }
                case 'd' -> { deleteSelectedTask(); return; }
                case ' ' -> { setActiveSelectedTask(); return; }
                default -> {}
            }
        }

        if (key.getKeyType() == KeyType.Tab) {
            focusZone = (focusZone == FocusZone.TIMER) ? FocusZone.TASKS : FocusZone.TIMER;
        } else if (key.getKeyType() == KeyType.ArrowUp) {
            if (!tasks.isEmpty()) selectedIndex = Math.max(0, selectedIndex - 1);
        } else if (key.getKeyType() == KeyType.ArrowDown) {
            if (!tasks.isEmpty()) selectedIndex = Math.min(tasks.size() - 1, selectedIndex + 1);
        } else if (key.getKeyType() == KeyType.Enter) {
            toggleSelectedTask();
        }
    }

    private void promptAddTask() throws IOException {
        String title = readLineModal("Nouvelle tâche : ");
        if (title != null && !title.isBlank()) {
            taskRepo.add(title.trim());
            tasks = taskRepo.listOpen();
            statusMessage = "Tâche ajoutée.";
        }
    }

    private void toggleSelectedTask() {
        Task t = currentTask();
        if (t == null) return;
        boolean newDone = !t.done();
        taskRepo.toggleDone(t.id(), newDone);
        if (newDone && activeTaskId != null && activeTaskId == t.id()) {
            activeTaskId = null;
            timer.setActiveTaskId(null);
        }
        tasks = taskRepo.listOpen();
        if (selectedIndex >= tasks.size()) selectedIndex = Math.max(0, tasks.size() - 1);
        statusMessage = newDone ? "Tâche cochée." : "Tâche dé-cochée.";
    }

    private void deleteSelectedTask() {
        Task t = currentTask();
        if (t == null) return;
        taskRepo.delete(t.id());
        if (activeTaskId != null && activeTaskId == t.id()) {
            activeTaskId = null;
            timer.setActiveTaskId(null);
        }
        tasks = taskRepo.listOpen();
        if (selectedIndex >= tasks.size()) selectedIndex = Math.max(0, tasks.size() - 1);
        statusMessage = "Tâche supprimée.";
    }

    private void setActiveSelectedTask() {
        Task t = currentTask();
        if (t == null) return;
        if (activeTaskId != null && activeTaskId == t.id()) {
            activeTaskId = null;
            timer.setActiveTaskId(null);
            statusMessage = "Aucune tâche active.";
        } else {
            activeTaskId = t.id();
            timer.setActiveTaskId(t.id());
            statusMessage = "Tâche active : " + t.title();
        }
    }

    private Task currentTask() {
        if (tasks.isEmpty()) return null;
        if (selectedIndex < 0 || selectedIndex >= tasks.size()) return null;
        return tasks.get(selectedIndex);
    }

    // --- Rendering ---

    private void render() throws IOException {
        screen.doResizeIfNecessary();
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        TextGraphics g = screen.newTextGraphics();

        int width = size.getColumns();
        int height = size.getRows();

        drawBorder(g, 0, 0, width, height, "");

        int bannerHeight = Banner.HEIGHT + 1; // 3 banner lines + 1 tagline
        int timerHeight = 11;
        int statsHeight = 3;
        int helpHeight = 4;

        int bannerY = 1;
        int sep1Y = bannerY + bannerHeight;
        int timerY = sep1Y + 1;
        int sep2Y = timerY + timerHeight;
        int tasksY = sep2Y + 1;
        int sep4Y = height - helpHeight - 1;
        int statsY = sep4Y - statsHeight;
        int sep3Y = statsY - 1;
        int tasksHeight = sep3Y - tasksY;

        renderBanner(g, 1, bannerY, width - 2);
        drawHorizontal(g, 0, sep1Y, width);
        renderTimer(g, 1, timerY, width - 2, timerHeight);
        drawHorizontal(g, 0, sep2Y, width);
        renderTasks(g, 1, tasksY, width - 2, tasksHeight);
        drawHorizontal(g, 0, sep3Y, width);
        renderStats(g, 1, statsY, width - 2, statsHeight);
        drawHorizontal(g, 0, sep4Y, width);
        renderHelp(g, 1, height - helpHeight, width - 2);

        screen.refresh();
    }

    private void renderBanner(TextGraphics g, int x, int y, int w) {
        int bx = centerX(x, w, Banner.width());
        g.setForegroundColor(TextColor.ANSI.RED_BRIGHT);
        Banner.draw(g, bx, y);
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        String tag = Banner.tagline();
        g.putString(centerX(x, w, tag.length()), y + Banner.HEIGHT, tag);
    }

    private void renderTimer(TextGraphics g, int x, int y, int w, int h) {
        SessionType type = timer.currentType();
        int remaining = timer.remainingSeconds();
        int total = timer.totalSeconds();
        PomodoroTimer.State state = timer.state();

        TextColor color = switch (type) {
            case FOCUS -> TextColor.ANSI.RED_BRIGHT;
            case SHORT_BREAK -> TextColor.ANSI.GREEN_BRIGHT;
            case LONG_BREAK -> TextColor.ANSI.CYAN_BRIGHT;
        };

        String time = formatTime(remaining);
        int bigW = BigDigits.widthForMmSs();
        int bigX = centerX(x, w, bigW);
        g.setForegroundColor(color);
        BigDigits.drawMmSs(g, bigX, y + 1, time);

        String label = type.label();
        if (state == PomodoroTimer.State.PAUSED) label += " (pause)";
        else if (state == PomodoroTimer.State.IDLE) label += " (arrêté)";
        g.setForegroundColor(color);
        g.putString(centerX(x, w, label.length()), y + BigDigits.HEIGHT + 1, label, SGR.BOLD);

        int barWidth = Math.min(w - 4, 50);
        int filled = total > 0 ? (int) Math.round((1.0 - (double) remaining / total) * barWidth) : 0;
        int barX = centerX(x, w, barWidth);
        g.setForegroundColor(color);
        for (int i = 0; i < barWidth; i++) {
            g.setCharacter(barX + i, y + BigDigits.HEIGHT + 2, i < filled ? '▓' : '░');
        }

        String cycle = "Cycle " + timer.cycleSlot() + "/" + Config.CYCLES_BEFORE_LONG_BREAK
                + "  ·  Pomodoros : " + timer.focusCyclesCompleted();
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(centerX(x, w, cycle.length()), y + BigDigits.HEIGHT + 3, cycle);

        String activeLine = activeTaskId == null
                ? "Aucune tâche active (Espace pour assigner)"
                : "Tâche active : " + activeTaskTitle();
        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(centerX(x, w, activeLine.length()), y + BigDigits.HEIGHT + 4, activeLine);
    }

    private String activeTaskTitle() {
        if (activeTaskId == null) return "";
        for (Task t : tasks) if (t.id() == activeTaskId) return t.title();
        return "(supprimée)";
    }

    private void renderTasks(TextGraphics g, int x, int y, int w, int h) {
        boolean focused = focusZone == FocusZone.TASKS;
        g.setForegroundColor(focused ? TextColor.ANSI.WHITE_BRIGHT : TextColor.ANSI.WHITE);
        String title = "Tâches" + (focused ? " ◀" : "");
        g.putString(x, y, title, SGR.BOLD);

        if (tasks.isEmpty()) {
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(x, y + 2, "(aucune tâche — appuie sur [a] pour en ajouter)");
            return;
        }

        int max = h - 1;
        int start = Math.max(0, Math.min(selectedIndex - max / 2, tasks.size() - max));
        if (start < 0) start = 0;
        for (int i = 0; i < max && start + i < tasks.size(); i++) {
            Task t = tasks.get(start + i);
            boolean isSelected = (start + i) == selectedIndex;
            boolean isActive = activeTaskId != null && activeTaskId == t.id();

            String box = t.done() ? "[✓]" : "[ ]";
            String marker = isSelected ? "▶ " : "  ";
            String activeTag = isActive ? "  ← active" : "";
            String line = marker + box + " " + truncate(t.title(), w - 16) + activeTag;

            if (isSelected && focused) {
                g.setForegroundColor(TextColor.ANSI.BLACK);
                g.setBackgroundColor(TextColor.ANSI.WHITE);
            } else if (t.done()) {
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            } else if (isActive) {
                g.setForegroundColor(TextColor.ANSI.YELLOW);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            } else {
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            }
            String padded = padRight(line, w);
            g.putString(x, y + 1 + i, padded);
        }
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
    }

    private void renderStats(TextGraphics g, int x, int y, int w, int h) {
        SessionRepository.DailyStats stats = sessionRepo.todayStats();
        long minutes = stats.focusSeconds() / 60;
        String human = (minutes >= 60)
                ? String.format("%dh%02d", minutes / 60, minutes % 60)
                : minutes + " min";
        String line = "Aujourd'hui : " + stats.focusCount() + " pomodoros · " + human + " de focus";
        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(x, y, line);

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, y + 1, truncate(statusMessage, w));
    }

    private void renderHelp(TextGraphics g, int x, int y, int w) {
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x, y,     "[s] start   [p] pause   [r] reset   [n] skip");
        g.putString(x, y + 1, "[a] add     [enter] toggle   [d] del   [space] active");
        g.putString(x, y + 2, "[↑↓] nav    [tab] zone   [q] quit");
    }

    // --- helpers ---

    private void drawBorder(TextGraphics g, int x, int y, int w, int h, String title) {
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

    private void drawHorizontal(TextGraphics g, int x, int y, int w) {
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.drawLine(x + 1, y, x + w - 2, y, '─');
        g.setCharacter(x, y, '├');
        g.setCharacter(x + w - 1, y, '┤');
    }

    private int centerX(int x, int w, int strLen) {
        return x + Math.max(0, (w - strLen) / 2);
    }

    private String formatTime(int totalSeconds) {
        int s = Math.max(0, totalSeconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private String truncate(String s, int max) {
        if (max <= 0) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String padRight(String s, int w) {
        if (s.length() >= w) return s.substring(0, w);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }

    private String readLineModal(String prompt) throws IOException {
        TerminalSize size = screen.getTerminalSize();
        int boxW = Math.min(size.getColumns() - 4, 60);
        int boxH = 5;
        int x = (size.getColumns() - boxW) / 2;
        int y = (size.getRows() - boxH) / 2;

        TextGraphics g = screen.newTextGraphics();
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        for (int i = 0; i < boxH; i++) {
            g.putString(x, y + i, padRight("", boxW));
        }
        drawBorder(g, x, y, boxW, boxH, " Saisie ");
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x + 2, y + 1, prompt);
        screen.refresh();

        StringBuilder sb = new StringBuilder();
        int inputX = x + 2 + prompt.length();
        int inputY = y + 2;
        int maxLen = boxW - 4;
        screen.setCursorPosition(new TerminalPosition(inputX, inputY));

        while (true) {
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
            } else if (k.getCharacter() != null && sb.length() < maxLen) {
                sb.append(k.getCharacter());
            }
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(x + 2, inputY, padRight(sb.toString(), maxLen));
            screen.setCursorPosition(new TerminalPosition(inputX + Math.min(sb.length(), maxLen - prompt.length()), inputY));
            screen.refresh();
        }
    }

    // --- TimerListener ---

    @Override
    public void onTick(SessionType type, int remainingSeconds, int totalSeconds, int cycleCount) {
        dirty.set(true);
    }

    @Override
    public void onTransition(SessionType from, SessionType to, int cycleCount) {
        statusMessage = from.label() + " terminé → " + to.label();
        dirty.set(true);
    }

    @Override
    public void onStateChanged() {
        dirty.set(true);
    }
}
