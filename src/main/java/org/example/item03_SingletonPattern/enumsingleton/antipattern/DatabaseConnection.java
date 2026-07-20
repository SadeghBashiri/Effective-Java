package org.example.item03_SingletonPattern.enumsingleton.antipattern;

public enum DatabaseConnection {

    INSTANCE;

    /**
     * Connection عمر مشخصی دارد.
     *
     * Reconnect دارد.
     *
     * Pool دارد.
     *
     * Singleton مناسبی نیست.
     */
    // private Connection connection;

    //...
}