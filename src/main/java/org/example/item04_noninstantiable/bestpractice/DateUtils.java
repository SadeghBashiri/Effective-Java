package org.example.item04_noninstantiable.bestpractice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;

public final class DateUtils {

    private DateUtils() {
        throw new AssertionError(
                "Utility class");
    }

    public static boolean isWeekend(LocalDate date) {

        DayOfWeek day = date.getDayOfWeek();

        return day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY;
    }

    public static int age(LocalDate birthDate) {

        return Period.between(
                birthDate,
                LocalDate.now())
                .getYears();
    }
}