package org.openmrs.module.bahmnioha.util;

import org.joda.time.LocalDate;
import org.joda.time.Years;

import java.math.BigDecimal;
import java.util.Date;

public class BahmniMath {
    public static double getTwoDigitRoundUpValue(double value) {
        return new BigDecimal(value).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static Integer ageInYears(Date birthDate, Date asOfDate) {
        return Years.yearsBetween(new LocalDate(birthDate), new LocalDate(asOfDate)).getYears();
    }
}
