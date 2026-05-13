package com.jpomodoro.notify;

import com.jpomodoro.TestSupport;
import com.jpomodoro.config.ConfigService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotifierFactoryTest {

    private SoundPlayer newPlayer() throws Exception {
        return new SoundPlayer(new ConfigService(TestSupport.tempPaths()));
    }

    @Test
    void macReturnsMacNotifier() throws Exception {
        SoundPlayer p = newPlayer();
        assertThat(NotifierFactory.forOs("Mac OS X", p)).isInstanceOf(MacNotifier.class);
        assertThat(NotifierFactory.forOs("Darwin", p)).isInstanceOf(MacNotifier.class);
    }

    @Test
    void linuxReturnsLinuxNotifier() throws Exception {
        SoundPlayer p = newPlayer();
        assertThat(NotifierFactory.forOs("Linux", p)).isInstanceOf(LinuxNotifier.class);
        assertThat(NotifierFactory.forOs("GNU/Linux", p)).isInstanceOf(LinuxNotifier.class);
    }

    @Test
    void unknownReturnsNoop() throws Exception {
        SoundPlayer p = newPlayer();
        assertThat(NotifierFactory.forOs("Windows 11", p)).isInstanceOf(NoopNotifier.class);
        assertThat(NotifierFactory.forOs(null, p)).isInstanceOf(NoopNotifier.class);
        assertThat(NotifierFactory.forOs("", p)).isInstanceOf(NoopNotifier.class);
    }

    @Test
    void noopDoesNotThrow() {
        Notifier n = new NoopNotifier();
        n.notifyFocusEnded();
        n.notifyBreakEnded();
        n.notifyCustom("x", "y");
    }
}
