package org.example.item03_singleton_pattern.enumsingleton.bestpractice;

public enum MetricsRegistry {

    INSTANCE;

    public void increment(String metric) {

        System.out.println(metric);
    }
}