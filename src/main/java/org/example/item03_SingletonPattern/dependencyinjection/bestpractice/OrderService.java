package org.example.item03_SingletonPattern.dependencyinjection.bestpractice;

public class OrderService {

    private final Logger logger;

    public OrderService(Logger logger) {

        this.logger = logger;
    }

    public void createOrder() {

        logger.log("created");
    }
}