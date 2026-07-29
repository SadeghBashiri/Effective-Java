package org.example.item03_singleton_pattern.publicfield.bestpractice;

public final class ConfigurationManager {

    public static final ConfigurationManager INSTANCE =
            new ConfigurationManager();

    private ConfigurationManager() {
    }

    public String getProperty(String key) {

        return switch (key) {
            case "app.name" -> "Effective Java";
            case "timeout" -> "30";
            default -> "";
        };
    }
}