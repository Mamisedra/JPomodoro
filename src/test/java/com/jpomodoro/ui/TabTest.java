package com.jpomodoro.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TabTest {

    @Test
    void nextCyclesAndWraps() {
        assertThat(Tab.TIMER.next()).isEqualTo(Tab.HISTORY);
        assertThat(Tab.HISTORY.next()).isEqualTo(Tab.SETTINGS);
        assertThat(Tab.SETTINGS.next()).isEqualTo(Tab.TIMER);
    }

    @Test
    void labelsExposedInFrench() {
        assertThat(Tab.TIMER.label()).isEqualTo("Timer");
        assertThat(Tab.HISTORY.label()).isEqualTo("Historique");
        assertThat(Tab.SETTINGS.label()).isEqualTo("Réglages");
    }
}
