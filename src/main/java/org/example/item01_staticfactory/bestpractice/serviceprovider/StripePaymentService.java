package org.example.item01_staticfactory.bestpractice.serviceprovider;

public class StripePaymentService
        implements PaymentService {

    @Override
    public void pay(long amount) {

        System.out.println(
                "Stripe payment: " + amount
        );
    }
}