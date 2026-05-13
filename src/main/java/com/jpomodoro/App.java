package com.jpomodoro;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.jpomodoro.config.AppPaths;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.Database;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.db.TaskRepository;
import com.jpomodoro.notify.Notifier;
import com.jpomodoro.notify.NotifierFactory;
import com.jpomodoro.timer.PomodoroTimer;
import com.jpomodoro.ui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        AppPaths paths = new AppPaths();
        paths.ensureDirectories();

        ConfigService config = new ConfigService(paths);
        log.info("JPomodoro démarrage — config : {}", paths.config());

        Database.open(paths.database());

        TaskRepository taskRepo = new TaskRepository();
        SessionRepository sessionRepo = new SessionRepository();
        Notifier notifier = NotifierFactory.current();
        PomodoroTimer timer = new PomodoroTimer(sessionRepo, config, notifier);

        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);

        MainWindow window = new MainWindow(screen, timer, taskRepo, sessionRepo);
        try {
            window.run();
        } finally {
            timer.shutdown();
            screen.close();
            log.info("JPomodoro arrêt propre");
        }
    }
}
