package org.example.item01_static_factory_methods.bestpractice.subtypeflexibility;

public final class NotificationFactory {

    private NotificationFactory() {
    }

    public static NotificationService of(String type) {

        return switch (type.toUpperCase()) {

            case "EMAIL" -> new EmailNotificationService();

            case "SMS" -> new SmsNotificationService();

            default -> throw new IllegalArgumentException(
                    "Unsupported type: " + type
            );
        };
    }
}