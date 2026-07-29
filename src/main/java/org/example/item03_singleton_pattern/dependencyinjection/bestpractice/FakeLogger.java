package org.example.item03_singleton_pattern.dependencyinjection.bestpractice;

public class FakeLogger implements Logger{
    @Override
    public void log(String message) {
        System.out.println("FakeLogger");
    }
}
