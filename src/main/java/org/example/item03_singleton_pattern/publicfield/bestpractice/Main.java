package org.example.item03_singleton_pattern.publicfield.bestpractice;

public class Main {

    public static void main(String[] args) {

        ConfigurationManager manager =
                ConfigurationManager.INSTANCE;

        System.out.println(
                manager.getProperty("app.name"));
    }
}