package org.example.item01_static_factory_methods.bestpractice.namedfactory;

import java.math.BigInteger;
import java.util.Random;

public class NamedFactoryMethodExample {

    public static void main(String[] args) {

        BigInteger prime =
                BigInteger.probablePrime(128, new Random());

        System.out.println(prime);
    }
}