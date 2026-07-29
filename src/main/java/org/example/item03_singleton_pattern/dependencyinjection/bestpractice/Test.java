package org.example.item03_singleton_pattern.dependencyinjection.bestpractice;

public class Test {
    static void main() {
        OrderService service =
                new OrderService(
                        new FakeLogger());
    }
}
