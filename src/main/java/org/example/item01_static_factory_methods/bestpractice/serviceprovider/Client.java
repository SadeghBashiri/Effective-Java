package org.example.item01_static_factory_methods.bestpractice.serviceprovider;

public class Client {

    public static void main(String[] args) {

        PaymentService paymentService =
                PaymentServiceRegistry
                        .getProvider("STRIPE");

        paymentService.pay(1000);
    }
}