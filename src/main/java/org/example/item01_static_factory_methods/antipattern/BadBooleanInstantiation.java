package org.example.item01_static_factory_methods.antipattern;

public class BadBooleanInstantiation {

    public static void main(String[] args) {

        Boolean a = new Boolean(true);
        Boolean b = new Boolean(true);

        System.out.println(a == b); // false
    }
}