package com.jpomodoro;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.jpomodoro.db.SessionRepository;
import com.jpomodoro.db.TaskRepository;
import com.jpomodoro.timer.PomodoroTimer;
import com.jpomodoro.ui.MainWindow;

public class App {
    public static void main(String[] args) throws Exception {
        TaskRepository taskRepo = new TaskRepository();
        SessionRepository sessionRepo = new SessionRepository();
        PomodoroTimer timer = new PomodoroTimer(sessionRepo);

        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);

        MainWindow window = new MainWindow(screen, timer, taskRepo, sessionRepo);
        try {
            window.run();
        } finally {
            timer.shutdown();
            screen.close();
        }
    }
}
