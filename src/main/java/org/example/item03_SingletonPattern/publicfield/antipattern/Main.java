package org.example.item03_SingletonPattern.publicfield.antipattern;

public class Main {
    static void main() {
        ConfigurationManager c1 =
                new ConfigurationManager();

        ConfigurationManager c2 =
                new ConfigurationManager();

        // c1 != c2 دیگر Singleton نیست.
    }
}
