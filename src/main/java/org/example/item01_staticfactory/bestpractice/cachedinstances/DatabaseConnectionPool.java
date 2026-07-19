package org.example.item01_staticfactory.bestpractice.cachedinstances;

public final class DatabaseConnectionPool {

    private static final DatabaseConnectionPool INSTANCE =
            new DatabaseConnectionPool();

    private DatabaseConnectionPool() {
    }

    public static DatabaseConnectionPool getInstance() {
        return INSTANCE;
    }
}