package org.example.item03_SingletonPattern.dependencyinjection.bestpractice;

public class LoggerService
        implements Logger {

    private static final LoggerService INSTANCE =
            new LoggerService();

    private LoggerService() {
    }

    public static LoggerService getInstance() {

        return INSTANCE;
    }

    @Override
    public void log(String message) {

        System.out.println(message);
    }
}