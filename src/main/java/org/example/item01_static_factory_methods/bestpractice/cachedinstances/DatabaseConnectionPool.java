package org.example.item01_static_factory_methods.bestpractice.cachedinstances;

public final class DatabaseConnectionPool {

    private static final DatabaseConnectionPool INSTANCE =
            new DatabaseConnectionPool();

    private DatabaseConnectionPool() {
    }

    public static DatabaseConnectionPool getInstance() {
        return INSTANCE;
    }
}