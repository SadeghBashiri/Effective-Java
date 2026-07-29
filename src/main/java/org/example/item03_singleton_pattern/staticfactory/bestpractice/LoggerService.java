package org.example.item03_singleton_pattern.staticfactory.bestpractice;

public final class LoggerService {

    private static final LoggerService INSTANCE =
            new LoggerService();

    private LoggerService() {
    }

    public static LoggerService getInstance() {
        return INSTANCE;
    }

    public void log(String message) {

        System.out.println(message);
    }
}