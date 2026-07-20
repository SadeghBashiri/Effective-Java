package org.example.item03_SingletonPattern.dependencyinjection.bestpractice;

public class Main {
    static void main() {
        OrderService service =
                new OrderService(
                        LoggerService.getInstance());
    }
}
