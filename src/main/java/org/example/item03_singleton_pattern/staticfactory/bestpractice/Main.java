package org.example.item03_singleton_pattern.staticfactory.bestpractice;

public class Main {
    static void main() {
        LoggerService logger =
                LoggerService.getInstance();

        logger.log("Application started");
    }
}
