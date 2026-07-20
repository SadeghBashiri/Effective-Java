package org.example.item03_SingletonPattern.dependencyinjection.bestpractice;

public class FakeLogger implements Logger{
    @Override
    public void log(String message) {
        System.out.println("FakeLogger");
    }
}
