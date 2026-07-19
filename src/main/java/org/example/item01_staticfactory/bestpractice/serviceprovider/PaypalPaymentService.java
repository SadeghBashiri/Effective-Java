package org.example.item01_staticfactory.bestpractice.serviceprovider;

public class PaypalPaymentService
        implements PaymentService {

    @Override
    public void pay(long amount) {

        System.out.println(
                "Paypal payment: " + amount
        );
    }
}