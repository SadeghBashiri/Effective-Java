package org.example.item01_static_factory_methods.bestpractice.serviceprovider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PaymentServiceRegistry {

    private static final Map<String, Supplier<PaymentService>> PROVIDERS = new HashMap<>(); // Supplier-based Registry
//    private static Map<String, Class<? extends PaymentService>> PROVIDERS = new HashMap<>(); // Class-based Registry

    static {

        PROVIDERS.put(
                "STRIPE",
                StripePaymentService::new
        );

        PROVIDERS.put(
                "PAYPAL",
                PaypalPaymentService::new
        );
    }

    private PaymentServiceRegistry() {
    }

    public static PaymentService getProvider(String name) {

        Supplier<PaymentService> supplier =
                PROVIDERS.get(name.toUpperCase());

        if (supplier == null) {
            throw new IllegalArgumentException(
                    "Unknown provider: " + name
            );
        }

        return supplier.get();
    }
}