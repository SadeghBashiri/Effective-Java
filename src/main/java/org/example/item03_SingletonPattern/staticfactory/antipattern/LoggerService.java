package org.example.item03_SingletonPattern.staticfactory.antipattern;

public final class LoggerService {

    private static final LoggerService INSTANCE =
            new LoggerService();

    private LoggerService() {
    }

    public static LoggerService getInstance() {

        return new LoggerService();
    }

    public void log(String message) {

        System.out.println(message);
    }
}