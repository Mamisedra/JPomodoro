package com.jpomodoro.stats;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SparklineTest {

    @Test
    void rendersProgression() {
        String s = Sparkline.render(new int[]{0, 1, 2, 4, 8});
        assertThat(s).hasSize(5);
        assertThat(s.charAt(0)).isEqualTo('▁');
        assertThat(s.charAt(4)).isEqualTo('█');
    }

    @Test
    void allZeroesYieldsLowestBar() {
        String s = Sparkline.render(new int[]{0, 0, 0});
        assertThat(s).isEqualTo("▁▁▁");
    }

    @Test
    void emptyReturnsEmpty() {
        assertThat(Sparkline.render(new int[]{})).isEmpty();
        assertThat(Sparkline.render(null)).isEmpty();
    }
}
