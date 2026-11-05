package com.paymentsapp.platform.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RublesTest {

    @Test
    void wholeRublesWithoutKopecks() {
        assertThat(Rubles.format(new BigDecimal("50000.00"))).isEqualTo("50 000 ₽");
    }

    @Test
    void kopecksWithTwoDigits() {
        assertThat(Rubles.format(new BigDecimal("33739.3"))).isEqualTo("33 739,30 ₽");
    }
}
