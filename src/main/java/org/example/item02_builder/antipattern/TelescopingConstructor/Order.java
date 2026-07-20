package org.example.item02_builder.antipattern.TelescopingConstructor;

public class Order {

    private final String customerId;
    private final String productId;
    private final int quantity;
    private final String coupon;
    private final String shippingAddress;

    public Order(
            String customerId,
            String productId) {

        this(customerId, productId, 1);
    }

    public Order(
            String customerId,
            String productId,
            int quantity) {

        this(customerId, productId, quantity, null);
    }

    public Order(
            String customerId,
            String productId,
            int quantity,
            String coupon) {

        this(customerId,
                productId,
                quantity,
                coupon,
                null);
    }

    public Order(
            String customerId,
            String productId,
            int quantity,
            String coupon,
            String shippingAddress) {

        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.coupon = coupon;
        this.shippingAddress = shippingAddress;
    }
}