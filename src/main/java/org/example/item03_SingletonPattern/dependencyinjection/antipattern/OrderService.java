package org.example.item03_SingletonPattern.dependencyinjection.antipattern;

import org.example.item03_SingletonPattern.staticfactory.antipattern.LoggerService;

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