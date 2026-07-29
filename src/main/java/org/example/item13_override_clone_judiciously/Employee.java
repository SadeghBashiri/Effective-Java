package org.example.item13_override_clone_judiciously;

public class Employee implements Cloneable {

    private String name;
    private Address address;

//    @Override
//    public Employee clone() {
//        try {
//            return (Employee) super.clone();  // ❌ Shallow Copy
//        } catch (CloneNotSupportedException e) {
//            throw new AssertionError();
//        }
//    }

    @Override
    public Employee clone() {
        return new Employee(name, address);  // ❌ super.clone() را رعایت نکرده
    }

    public Employee(String name, Address address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}