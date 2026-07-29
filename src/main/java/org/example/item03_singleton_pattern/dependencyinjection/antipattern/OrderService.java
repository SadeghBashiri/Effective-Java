package org.example.item03_singleton_pattern.dependencyinjection.antipattern;

import org.example.item03_singleton_pattern.staticfactory.antipattern.LoggerService;

/**
 * مشکل
 *
 * OrderService
 *
 * کاملاً به
 *
 * LoggerService
 *
 * وابسته شده است.
 *
 * Test کردن تقریباً غیرممکن می‌شود.
 */
public class OrderService {

    public void createOrder() {

        LoggerService
                .getInstance()
                .log("created");
    }
}