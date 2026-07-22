package org.example.item04_noninstantiable.antipattern;

public class DateUtils {
    /**
     * کامپایلر خودش اضافه میکند
     * در نتیجه
     * DateUtils utils = new DateUtils();
     * @return
     */
//    public DateUtils() {
//    }

    public static boolean isWeekend() {
        return false;
    }

}