package org.example.item03_singleton_pattern.dependencyinjection.bestpractice;

public class Main {
    static void main() {
        OrderService service =
                new OrderService(
                        LoggerService.getInstance());
    }
}
