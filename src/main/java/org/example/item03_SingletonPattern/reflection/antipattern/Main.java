package org.example.item03_SingletonPattern.reflection.antipattern;

import org.example.item03_SingletonPattern.publicfield.antipattern.ConfigurationManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Main {
    static void main() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Constructor<ConfigurationManager> c = ConfigurationManager.class.getDeclaredConstructor();

        c.setAccessible(true);

        ConfigurationManager second = c.newInstance();
    }
}
