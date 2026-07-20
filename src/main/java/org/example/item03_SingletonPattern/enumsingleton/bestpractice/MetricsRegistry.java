package org.example.item03_SingletonPattern.enumsingleton.bestpractice;

public enum MetricsRegistry {

    INSTANCE;

    public void increment(String metric) {

        System.out.println(metric);
    }
}