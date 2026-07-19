package org.example.item01_staticfactory.antipattern;

public class BadBooleanInstantiation {

    public static void main(String[] args) {

        Boolean a = new Boolean(true);
        Boolean b = new Boolean(true);

        System.out.println(a == b); // false
    }
}