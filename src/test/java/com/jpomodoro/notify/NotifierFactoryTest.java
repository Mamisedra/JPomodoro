package com.jpomodoro.notify;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotifierFactoryTest {

    @Test
    void macReturnsMacNotifier() {
        assertThat(NotifierFactory.forOs("Mac OS X")).isInstanceOf(MacNotifier.class);
        assertThat(NotifierFactory.forOs("Darwin")).isInstanceOf(MacNotifier.class);
    }

    @Test
    void linuxReturnsLinuxNotifier() {
        assertThat(NotifierFactory.forOs("Linux")).isInstanceOf(LinuxNotifier.class);
        assertThat(NotifierFactory.forOs("GNU/Linux")).isInstanceOf(LinuxNotifier.class);
    }

    @Test
    void unknownReturnsNoop() {
        assertThat(NotifierFactory.forOs("Windows 11")).isInstanceOf(NoopNotifier.class);
        assertThat(NotifierFactory.forOs(null)).isInstanceOf(NoopNotifier.class);
        assertThat(NotifierFactory.forOs("")).isInstanceOf(NoopNotifier.class);
    }

    @Test
    void noopDoesNotThrow() {
        Notifier n = new NoopNotifier();
        n.notifyFocusEnded();
        n.notifyBreakEnded();
        n.notifyCustom("x", "y");
    }
}
