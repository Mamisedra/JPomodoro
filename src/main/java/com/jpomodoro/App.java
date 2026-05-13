package com.jpomodoro;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.jpomodoro.ai.JsonlParser;
import com.jpomodoro.ai.SummaryService;
import com.jpomodoro.config.AppPaths;
import com.jpomodoro.config.ConfigService;
import com.jpomodoro.db.Database;
import com.jpomodoro.db.FeedbackRepository;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.db.SummaryRepository;
import com.jpomodoro.db.TaskRepository;
import com.jpomodoro.notify.Notifier;
import com.jpomodoro.notify.NotifierFactory;
import com.jpomodoro.notify.SoundPlayer;
import com.jpomodoro.timer.PomodoroTimer;
import com.jpomodoro.ui.MainWindow;
import com.jpomodoro.ui.onboarding.OnboardingFlow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        SummaryRepository summaryRepo = new SummaryRepository();
        FeedbackRepository feedbackRepo = new FeedbackRepository();
        SoundPlayer soundPlayer = new SoundPlayer(config);
        Notifier notifier = NotifierFactory.current(soundPlayer);
        PomodoroTimer timer = new PomodoroTimer(sessionRepo, config, notifier);

        ExecutorService aiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ai-worker");
            t.setDaemon(true);
            return t;
        });
        SummaryService summaryService = new SummaryService(
                config, summaryRepo, aiExecutor, JsonlParser.defaultClaudeProjectsDir());

        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);

        if (!config.get().onboarded()) {
            log.info("Onboarding (premier lancement)");
            OnboardingFlow.run(screen, config);
        }

        MainWindow window = new MainWindow(screen, timer, taskRepo, sessionRepo,
                config, summaryService, feedbackRepo);
        try {
            window.run();
        } finally {
            timer.shutdown();
            aiExecutor.shutdownNow();
            screen.close();
            log.info("JPomodoro arrêt propre");
        }
    }
}
