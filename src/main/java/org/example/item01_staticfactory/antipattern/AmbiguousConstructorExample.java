package org.example.item01_staticfactory.antipattern;

import java.math.BigInteger;
import java.util.Random;

public class AmbiguousConstructorExample {

    void main() {

        BigInteger prime = new BigInteger(128, 64, new Random());

        System.out.println(prime);
    }
}