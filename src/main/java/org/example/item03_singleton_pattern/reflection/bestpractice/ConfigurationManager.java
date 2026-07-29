package org.example.item03_singleton_pattern.reflection.bestpractice;

public final class ConfigurationManager {

    private static boolean created;

    public static final ConfigurationManager INSTANCE = new ConfigurationManager();

    private ConfigurationManager() {

        if (created) {

            throw new RuntimeException("Singleton already created");
        }

        created = true;
    }
}