package com.jpomodoro.notify;

public interface Notifier {
    void notifyFocusEnded();
    void notifyBreakEnded();
    void notifyCustom(String title, String body);
}
