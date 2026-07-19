package org.example.item01_staticfactory.bestpractice.subtypeflexibility;

public class SmsNotificationService
        implements NotificationService {

    @Override
    public void send(String message) {
        System.out.println("Sending SMS: " + message);
    }
}