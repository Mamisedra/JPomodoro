package com.jpomodoro.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.jpomodoro.config.AppConfig;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.db.TaskRepository;
import com.jpomodoro.model.Priority;
import com.jpomodoro.model.Task;
import com.jpomodoro.notify.SoundPlayer;
import com.jpomodoro.schedule.ScheduleMode;
import com.jpomodoro.timer.PomodoroTimer;
import com.jpomodoro.timer.SessionType;
import com.jpomodoro.timer.TimerListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWindow implements TimerListener {

    private final Screen screen;
    private final PomodoroTimer timer;
    private final TaskRepository taskRepo;
    private final SessionRepository sessionRepo;
    private final ConfigService config;

    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private volatile boolean running = true;

    private List<Task> tasks;
    private int selectedIndex = 0;
    private int settingIndex = 0;
    private Long activeTaskId = null;
    private Tab currentTab = Tab.TIMER;
    private String statusMessage = "Bienvenue. Appuie sur [s] pour démarrer.";

    public MainWindow(Screen screen, PomodoroTimer timer,
                      TaskRepository taskRepo, SessionRepository sessionRepo,
                      ConfigService config) {
        this.screen = screen;
        this.timer = timer;
        this.taskRepo = taskRepo;
        this.sessionRepo = sessionRepo;
        this.config = config;
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
            switch (ch) {
                case '1' -> { currentTab = Tab.TIMER; return; }
                case '2' -> { currentTab = Tab.HISTORY; return; }
                case '3' -> { currentTab = Tab.SETTINGS; return; }
                default -> {}
            }
            switch (Character.toLowerCase(ch)) {
                case 'q' -> { running = false; return; }
                case 's' -> { timer.start(); statusMessage = "Timer démarré."; return; }
                case 'p' -> { timer.pause(); statusMessage = "Timer en pause."; return; }
                case 'r' -> { timer.reset(); statusMessage = "Timer réinitialisé."; return; }
                case 'n' -> { timer.skip(); statusMessage = "Session passée."; return; }
                case 'm' -> { toggleScheduleMode(); return; }
                default -> {}
            }
            if (currentTab == Tab.TIMER) {
                switch (Character.toLowerCase(ch)) {
                    case 'a' -> { promptAddTask(); return; }
                    case 'd' -> { deleteSelectedTask(); return; }
                    case ' ' -> { setActiveSelectedTask(); return; }
                    case '!' -> { cyclePrioritySelected(); return; }
                    case 'e' -> { editEstimateSelected(); return; }
                    default -> {}
                }
            }
        }

        if (key.getKeyType() == KeyType.Tab) {
            currentTab = currentTab.next();
        } else if (currentTab == Tab.TIMER) {
            if (key.getKeyType() == KeyType.ArrowUp) {
                if (!tasks.isEmpty()) selectedIndex = Math.max(0, selectedIndex - 1);
            } else if (key.getKeyType() == KeyType.ArrowDown) {
                if (!tasks.isEmpty()) selectedIndex = Math.min(tasks.size() - 1, selectedIndex + 1);
            } else if (key.getKeyType() == KeyType.Enter) {
                toggleSelectedTask();
            }
        } else if (currentTab == Tab.SETTINGS) {
            int rows = settingsRowCount();
            if (key.getKeyType() == KeyType.ArrowUp) {
                settingIndex = (settingIndex - 1 + rows) % rows;
            } else if (key.getKeyType() == KeyType.ArrowDown) {
                settingIndex = (settingIndex + 1) % rows;
            } else if (key.getKeyType() == KeyType.Enter) {
                editSetting(settingIndex);
            }
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

    private void cyclePrioritySelected() {
        Task t = currentTask();
        if (t == null) return;
        Priority p = taskRepo.cyclePriority(t.id());
        tasks = taskRepo.listOpen();
        reSelectById(t.id());
        statusMessage = "Priorité : " + p.name();
    }

    private void editEstimateSelected() throws IOException {
        Task t = currentTask();
        if (t == null) return;
        String raw = readLineModal("Estimation (pomodoros, vide pour effacer) : ");
        if (raw == null) return;
        Integer value = null;
        if (!raw.isBlank()) {
            try {
                int n = Integer.parseInt(raw.trim());
                if (n > 0) value = n;
            } catch (NumberFormatException ignore) {
                statusMessage = "Estimation invalide.";
                return;
            }
        }
        taskRepo.setEstimate(t.id(), value);
        tasks = taskRepo.listOpen();
        reSelectById(t.id());
        statusMessage = value == null ? "Estimation effacée." : ("Estimation : " + value);
    }

    private void reSelectById(long id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id() == id) { selectedIndex = i; return; }
        }
        selectedIndex = Math.min(selectedIndex, Math.max(0, tasks.size() - 1));
    }

    private void toggleScheduleMode() {
        ScheduleMode next = (timer.mode() == ScheduleMode.AUTO) ? ScheduleMode.MANUAL : ScheduleMode.AUTO;
        timer.setMode(next);
        statusMessage = "Mode " + (next == ScheduleMode.AUTO ? "AUTO" : "MANUEL") + ".";
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

        int bannerHeight = Banner.HEIGHT + 1;
        int tabsHeight = 1;
        int statsHeight = 3;
        int helpHeight = 4;

        int bannerY = 1;
        int sep1Y = bannerY + bannerHeight;
        int tabsY = sep1Y + 1;
        int sep2Y = tabsY + tabsHeight;
        int contentY = sep2Y + 1;
        int sep4Y = height - helpHeight - 1;
        int statsY = sep4Y - statsHeight;
        int sep3Y = statsY - 1;
        int contentHeight = sep3Y - contentY;

        renderBanner(g, 1, bannerY, width - 2);
        drawHorizontal(g, 0, sep1Y, width);
        renderTabs(g, 1, tabsY, width - 2);
        drawHorizontal(g, 0, sep2Y, width);

        switch (currentTab) {
            case TIMER -> renderTimerTab(g, 1, contentY, width - 2, contentHeight);
            case HISTORY -> renderHistoryPlaceholder(g, 1, contentY, width - 2, contentHeight);
            case SETTINGS -> renderSettingsPlaceholder(g, 1, contentY, width - 2, contentHeight);
        }

        drawHorizontal(g, 0, sep3Y, width);
        renderStats(g, 1, statsY, width - 2, statsHeight);
        drawHorizontal(g, 0, sep4Y, width);
        renderHelp(g, 1, height - helpHeight, width - 2);

        screen.refresh();
    }

    private void renderTabs(TextGraphics g, int x, int y, int w) {
        StringBuilder sb = new StringBuilder();
        for (Tab t : Tab.values()) {
            sb.append("  ");
            sb.append(t == currentTab ? "▶ " : "  ");
            sb.append("[").append(t.ordinal() + 1).append("] ").append(t.label());
        }
        String line = sb.toString();
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(centerX(x, w, line.length()), y, line);
    }

    private void renderTimerTab(TextGraphics g, int x, int y, int w, int h) {
        int timerHeight = Math.min(11, h - 4);
        renderTimer(g, x, y, w, timerHeight);
        int sepY = y + timerHeight;
        drawHorizontal(g, x - 1, sepY, w + 2);
        renderTasks(g, x, sepY + 1, w, h - timerHeight - 1);
    }

    private void renderHistoryPlaceholder(TextGraphics g, int x, int y, int w, int h) {
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, y, "Historique", SGR.BOLD);
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x, y + 2, "(à venir — Phase 7 : sessions + résumés + sparkline 7 jours + streak)");
    }

    private void renderSettingsPlaceholder(TextGraphics g, int x, int y, int w, int h) {
        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, y, "Réglages", SGR.BOLD);
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x, y + 1, "[↑↓] naviguer · [Enter] éditer");

        AppConfig c = config.get();
        SettingRow[] rows = settingsRows(c);
        int startY = y + 3;
        int max = Math.min(rows.length, h - 4);
        for (int i = 0; i < max; i++) {
            SettingRow row = rows[i];
            String line = String.format(" %-32s %s", row.label, row.value);
            if (i == settingIndex) {
                g.setForegroundColor(TextColor.ANSI.BLACK);
                g.setBackgroundColor(TextColor.ANSI.WHITE);
            } else if (row.section) {
                g.setForegroundColor(TextColor.ANSI.YELLOW);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            } else {
                g.setForegroundColor(TextColor.ANSI.WHITE);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            }
            g.putString(x, startY + i, padRight(line, w));
        }
        g.setBackgroundColor(TextColor.ANSI.DEFAULT);
    }

    private record SettingRow(String label, String value, boolean section) {}

    private SettingRow[] settingsRows(AppConfig c) {
        return new SettingRow[] {
                new SettingRow("Durée focus (min)",        String.valueOf(c.timer().focusMinutes()), false),
                new SettingRow("Pause courte (min)",       String.valueOf(c.timer().shortBreakMinutes()), false),
                new SettingRow("Pause longue (min)",       String.valueOf(c.timer().longBreakMinutes()), false),
                new SettingRow("Cycles avant pause longue", String.valueOf(c.timer().cyclesBeforeLongBreak()), false),
                new SettingRow("Matin début",              c.schedule().morningStart(), false),
                new SettingRow("Matin fin",                c.schedule().morningEnd(), false),
                new SettingRow("Après-midi début",         c.schedule().afternoonStart(), false),
                new SettingRow("Après-midi fin",           c.schedule().afternoonEnd(), false),
                new SettingRow("IA activée",               c.ai().enabled() ? "oui" : "non", false),
                new SettingRow("Modèle IA",                c.ai().model(), false),
                new SettingRow("Endpoint Ollama",          c.ai().endpoint(), false),
                new SettingRow("Notifications activées",   c.notification().enabled() ? "oui" : "non", false),
                new SettingRow("Son",                      c.notification().sound(), false),
                new SettingRow("Webhook URL",              c.webhook().url().isEmpty() ? "(vide)" : c.webhook().url(), false),
                new SettingRow("Webhook activé",           c.webhook().enabled() ? "oui" : "non", false),
                new SettingRow("Palette",                  c.appearance().palette(), false),
        };
    }

    private int settingsRowCount() {
        return settingsRows(config.get()).length;
    }

    private void editSetting(int index) throws IOException {
        switch (index) {
            case 0 -> editTimerMinutes("Durée focus (min)", c -> c.timer().focusMinutes(),
                    (c, v) -> withFocus(c, v));
            case 1 -> editTimerMinutes("Pause courte (min)", c -> c.timer().shortBreakMinutes(),
                    (c, v) -> withShortBreak(c, v));
            case 2 -> editTimerMinutes("Pause longue (min)", c -> c.timer().longBreakMinutes(),
                    (c, v) -> withLongBreak(c, v));
            case 3 -> editTimerMinutes("Cycles avant pause longue", c -> c.timer().cyclesBeforeLongBreak(),
                    (c, v) -> withCycles(c, v));
            case 4 -> editTime("Matin début (HH:mm)", c -> c.schedule().morningStart(),
                    (c, v) -> withSchedule(c, v, c.schedule().morningEnd(), c.schedule().afternoonStart(), c.schedule().afternoonEnd()));
            case 5 -> editTime("Matin fin (HH:mm)", c -> c.schedule().morningEnd(),
                    (c, v) -> withSchedule(c, c.schedule().morningStart(), v, c.schedule().afternoonStart(), c.schedule().afternoonEnd()));
            case 6 -> editTime("Après-midi début (HH:mm)", c -> c.schedule().afternoonStart(),
                    (c, v) -> withSchedule(c, c.schedule().morningStart(), c.schedule().morningEnd(), v, c.schedule().afternoonEnd()));
            case 7 -> editTime("Après-midi fin (HH:mm)", c -> c.schedule().afternoonEnd(),
                    (c, v) -> withSchedule(c, c.schedule().morningStart(), c.schedule().morningEnd(), c.schedule().afternoonStart(), v));
            case 8 -> toggleAiEnabled();
            case 9 -> editText("Modèle IA", config.get().ai().model(), this::withAiModel);
            case 10 -> editText("Endpoint Ollama", config.get().ai().endpoint(), this::withAiEndpoint);
            case 11 -> toggleNotificationsEnabled();
            case 12 -> chooseSound();
            case 13 -> editText("Webhook URL (vide pour désactiver)", config.get().webhook().url(), this::withWebhookUrl);
            case 14 -> toggleWebhookEnabled();
            case 15 -> editText("Palette (default/mono/synthwave)", config.get().appearance().palette(), this::withPalette);
            default -> {}
        }
    }

    @FunctionalInterface
    private interface IntGetter { int get(AppConfig c); }
    @FunctionalInterface
    private interface StringGetter { String get(AppConfig c); }
    @FunctionalInterface
    private interface IntApplier { AppConfig apply(AppConfig c, int v); }
    @FunctionalInterface
    private interface StringApplier { AppConfig apply(AppConfig c, String v); }

    private void editTimerMinutes(String label, IntGetter getter, IntApplier applier) throws IOException {
        String raw = Modal.readLine(screen, label + " : ", String.valueOf(getter.get(config.get())));
        if (raw == null) return;
        int v;
        try {
            v = Integer.parseInt(raw.trim());
            if (v <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusMessage = "Valeur invalide.";
            return;
        }
        config.update(c -> applier.apply(c, v));
        statusMessage = label + " = " + v;
    }

    private void editTime(String label, StringGetter getter, StringApplier applier) throws IOException {
        String raw = Modal.readLine(screen, label + " : ", getter.get(config.get()));
        if (raw == null || raw.isBlank()) return;
        try {
            java.time.LocalTime.parse(raw.trim());
        } catch (Exception e) {
            statusMessage = "Format invalide (HH:mm attendu).";
            return;
        }
        config.update(c -> applier.apply(c, raw.trim()));
        statusMessage = label + " = " + raw.trim();
    }

    private void editText(String label, String current, StringApplier applier) throws IOException {
        String raw = Modal.readLine(screen, label + " : ", current);
        if (raw == null) return;
        config.update(c -> applier.apply(c, raw));
        statusMessage = label + " mis à jour.";
    }

    private void toggleAiEnabled() {
        config.update(c -> new AppConfig(c.timer(), c.schedule(),
                new AppConfig.AiSettings(!c.ai().enabled(), c.ai().model(), c.ai().endpoint(), c.ai().timeoutSeconds()),
                c.notification(), c.webhook(), c.appearance(), c.onboarded()));
        statusMessage = "IA " + (config.get().ai().enabled() ? "activée" : "désactivée") + ".";
    }

    private void toggleNotificationsEnabled() {
        config.update(c -> new AppConfig(c.timer(), c.schedule(), c.ai(),
                new AppConfig.NotificationSettings(!c.notification().enabled(), c.notification().sound()),
                c.webhook(), c.appearance(), c.onboarded()));
        statusMessage = "Notifications " + (config.get().notification().enabled() ? "activées" : "désactivées") + ".";
    }

    private void toggleWebhookEnabled() {
        config.update(c -> new AppConfig(c.timer(), c.schedule(), c.ai(), c.notification(),
                new AppConfig.WebhookSettings(c.webhook().url(), !c.webhook().enabled()),
                c.appearance(), c.onboarded()));
        statusMessage = "Webhook " + (config.get().webhook().enabled() ? "activé" : "désactivé") + ".";
    }

    private void chooseSound() throws IOException {
        java.util.List<String> options = new java.util.ArrayList<>(SoundPlayer.presets());
        java.util.Collections.sort(options);
        options.add("none");
        java.util.Optional<Integer> picked = Modal.choose(screen, "Son notification", options);
        if (picked.isEmpty()) return;
        String value = options.get(picked.get());
        config.update(c -> new AppConfig(c.timer(), c.schedule(), c.ai(),
                new AppConfig.NotificationSettings(c.notification().enabled(), value),
                c.webhook(), c.appearance(), c.onboarded()));
        statusMessage = "Son : " + value;
    }

    private AppConfig withFocus(AppConfig c, int v) {
        return withTimer(c, new AppConfig.TimerSettings(v, c.timer().shortBreakMinutes(), c.timer().longBreakMinutes(), c.timer().cyclesBeforeLongBreak()));
    }
    private AppConfig withShortBreak(AppConfig c, int v) {
        return withTimer(c, new AppConfig.TimerSettings(c.timer().focusMinutes(), v, c.timer().longBreakMinutes(), c.timer().cyclesBeforeLongBreak()));
    }
    private AppConfig withLongBreak(AppConfig c, int v) {
        return withTimer(c, new AppConfig.TimerSettings(c.timer().focusMinutes(), c.timer().shortBreakMinutes(), v, c.timer().cyclesBeforeLongBreak()));
    }
    private AppConfig withCycles(AppConfig c, int v) {
        return withTimer(c, new AppConfig.TimerSettings(c.timer().focusMinutes(), c.timer().shortBreakMinutes(), c.timer().longBreakMinutes(), v));
    }
    private AppConfig withTimer(AppConfig c, AppConfig.TimerSettings t) {
        return new AppConfig(t, c.schedule(), c.ai(), c.notification(), c.webhook(), c.appearance(), c.onboarded());
    }
    private AppConfig withSchedule(AppConfig c, String ms, String me, String as, String ae) {
        return new AppConfig(c.timer(),
                new AppConfig.ScheduleSettings(ms, me, as, ae, c.schedule().mode()),
                c.ai(), c.notification(), c.webhook(), c.appearance(), c.onboarded());
    }
    private AppConfig withAiModel(AppConfig c, String v) {
        return new AppConfig(c.timer(), c.schedule(),
                new AppConfig.AiSettings(c.ai().enabled(), v, c.ai().endpoint(), c.ai().timeoutSeconds()),
                c.notification(), c.webhook(), c.appearance(), c.onboarded());
    }
    private AppConfig withAiEndpoint(AppConfig c, String v) {
        return new AppConfig(c.timer(), c.schedule(),
                new AppConfig.AiSettings(c.ai().enabled(), c.ai().model(), v, c.ai().timeoutSeconds()),
                c.notification(), c.webhook(), c.appearance(), c.onboarded());
    }
    private AppConfig withWebhookUrl(AppConfig c, String v) {
        return new AppConfig(c.timer(), c.schedule(), c.ai(), c.notification(),
                new AppConfig.WebhookSettings(v, c.webhook().enabled()),
                c.appearance(), c.onboarded());
    }
    private AppConfig withPalette(AppConfig c, String v) {
        return new AppConfig(c.timer(), c.schedule(), c.ai(), c.notification(), c.webhook(),
                new AppConfig.AppearanceSettings(v), c.onboarded());
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

        String cycle = "Cycle " + timer.cycleSlot() + "/" + timer.cyclesBeforeLongBreak()
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
        g.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
        g.putString(x, y, "Tâches", SGR.BOLD);

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
            String priorityTag = "[" + t.priority().marker() + "] ";
            String activeTag = isActive ? "  ← active" : "";
            String estimateTag = estimateSuffix(t);
            String line = marker + box + " " + priorityTag
                    + truncate(t.title(), Math.max(4, w - 24))
                    + estimateTag + activeTag;

            if (isSelected) {
                g.setForegroundColor(TextColor.ANSI.BLACK);
                g.setBackgroundColor(TextColor.ANSI.WHITE);
            } else if (t.done()) {
                g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            } else if (isActive) {
                g.setForegroundColor(TextColor.ANSI.YELLOW);
                g.setBackgroundColor(TextColor.ANSI.DEFAULT);
            } else {
                g.setForegroundColor(priorityColor(t.priority()));
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

        String modeBadge = (timer.mode() == ScheduleMode.AUTO) ? "[AUTO]" : "[MANUEL]";
        String boundary = timer.nextBoundary()
                .map(b -> "  →  " + formatBoundary(b))
                .orElse("");
        String holdMark = timer.autoHold() ? "  ⏸ hors créneau" : "";
        String scheduleLine = modeBadge + boundary + holdMark;
        g.setForegroundColor(timer.autoHold() ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
        g.putString(x, y + 1, truncate(scheduleLine, w));

        g.setForegroundColor(TextColor.ANSI.WHITE);
        g.putString(x, y + 2, truncate(statusMessage, w));
    }

    private TextColor priorityColor(Priority p) {
        return switch (p) {
            case HIGH -> TextColor.ANSI.RED_BRIGHT;
            case NORMAL -> TextColor.ANSI.WHITE;
            case LOW -> TextColor.ANSI.BLACK_BRIGHT;
        };
    }

    private String estimateSuffix(Task t) {
        if (t.estimatedPomodoros() == null) return "";
        int consumed = sessionRepo.countFocusForTask(t.id());
        return "  (" + consumed + "/" + t.estimatedPomodoros() + ")";
    }

    private String formatBoundary(LocalDateTime boundary) {
        LocalDateTime now = LocalDateTime.now();
        if (boundary.toLocalDate().equals(now.toLocalDate())) {
            return boundary.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return boundary.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    private void renderHelp(TextGraphics g, int x, int y, int w) {
        g.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
        g.putString(x, y,     "[s] start  [p] pause  [r] reset  [n] skip  [m] auto/manuel");
        g.putString(x, y + 1, "[a] add  [enter] toggle  [d] del  [space] active  [!] prio  [e] est");
        g.putString(x, y + 2, "[↑↓] nav  [tab/1·2·3] vue  [q] quit");
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
        return Modal.readLine(screen, prompt, "");
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
