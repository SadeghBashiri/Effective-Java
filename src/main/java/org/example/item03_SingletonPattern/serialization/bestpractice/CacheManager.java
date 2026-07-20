package org.example.item03_SingletonPattern.serialization.bestpractice;

import java.io.Serializable;

public class CacheManager implements Serializable {

    public static final CacheManager INSTANCE =
            new CacheManager();

    private CacheManager() {
    }

    private Object readResolve() {

        return INSTANCE;
    }
}