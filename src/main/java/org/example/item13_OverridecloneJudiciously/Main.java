package org.example.item13_OverridecloneJudiciously;

public class Main {
    static void main() {
        Employee e1 = new Employee("Ali", new Address("Berlin"));
        Employee e2 = e1.clone();

        e2.getAddress().setCity("Munich");

        System.out.println(e1.getAddress().getCity()); // ❌ Munich
    }
}
