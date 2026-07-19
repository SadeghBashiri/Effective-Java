package org.example.item01_staticfactory.bestpractice.cachedinstances;

public class CachedBooleanExample {

    public static void main(String[] args) {

        Boolean a = Boolean.valueOf(true);
        Boolean b = Boolean.valueOf(true);

        System.out.println(a == b); // true
    }
}