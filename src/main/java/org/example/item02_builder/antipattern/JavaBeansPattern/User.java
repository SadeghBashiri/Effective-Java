package org.example.item02_builder.antipattern.JavaBeansPattern;

public class User {

    private String username;
    private String email;
    private String phone;

    public User() {
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}