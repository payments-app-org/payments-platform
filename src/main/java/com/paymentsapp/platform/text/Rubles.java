package com.paymentsapp.platform.text;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

// Сумма в рублях для текстов пользователю: «50 000 ₽», но «33 739,30 ₽»
public final class Rubles {

    private static final Locale RUSSIAN = Locale.forLanguageTag("ru");
    private static final String SIGN = " ₽";
    private static final int KOPECKS_DIGITS = 2;

    private Rubles() {
    }

    public static String format(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(RUSSIAN);
        boolean whole = value.stripTrailingZeros().scale() <= 0;
        format.setMinimumFractionDigits(whole ? 0 : KOPECKS_DIGITS);
        format.setMaximumFractionDigits(KOPECKS_DIGITS);
        return format.format(value) + SIGN;
    }
}
