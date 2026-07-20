package org.example.item03_SingletonPattern.staticfactory.bestpractice;

public class Main {
    static void main() {
        LoggerService logger =
                LoggerService.getInstance();

        logger.log("Application started");
    }
}
