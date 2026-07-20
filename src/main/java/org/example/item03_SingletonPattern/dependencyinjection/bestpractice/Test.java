package org.example.item03_SingletonPattern.dependencyinjection.bestpractice;

public class Test {
    static void main() {
        OrderService service =
                new OrderService(
                        new FakeLogger());
    }
}
