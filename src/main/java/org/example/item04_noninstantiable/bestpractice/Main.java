package org.example.item04_noninstantiable.bestpractice;

import java.time.LocalDate;

public class Main {
    static void main() {
        boolean weekend =
                DateUtils.isWeekend(LocalDate.now());
    }
}
