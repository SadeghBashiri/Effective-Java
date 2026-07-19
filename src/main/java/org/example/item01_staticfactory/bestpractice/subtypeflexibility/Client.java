package org.example.item01_staticfactory.bestpractice.subtypeflexibility;

public class Client {

    public static void main(String[] args) {

        NotificationService service =
                NotificationFactory.of("EMAIL");

        service.send("Hello");
    }
}