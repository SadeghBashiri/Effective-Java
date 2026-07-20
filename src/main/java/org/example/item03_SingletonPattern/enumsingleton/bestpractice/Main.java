package org.example.item03_SingletonPattern.enumsingleton.bestpractice;

public class Main {
    static void main() {
        MetricsRegistry.INSTANCE.increment(
                "login.success");
    }
}
