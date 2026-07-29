package org.example.item03_singleton_pattern.enumsingleton.bestpractice;

public class Main {
    static void main() {
        MetricsRegistry.INSTANCE.increment(
                "login.success");
    }
}
