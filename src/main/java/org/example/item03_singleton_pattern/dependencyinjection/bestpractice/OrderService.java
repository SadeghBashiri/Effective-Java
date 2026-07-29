package org.example.item03_singleton_pattern.dependencyinjection.bestpractice;

public class OrderService {

    private final Logger logger;

    public OrderService(Logger logger) {

        this.logger = logger;
    }

    public void createOrder() {

        logger.log("created");
    }
}