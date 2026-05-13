package com.jpomodoro.notify;

public final class NoopNotifier implements Notifier {

    @Override public void notifyFocusEnded() {}
    @Override public void notifyBreakEnded() {}
    @Override public void notifyCustom(String title, String body) {}
}
