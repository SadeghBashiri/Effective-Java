package org.example.item02_builder.antipattern.BuilderPattern;

public class User {

    private final String username;
    private final int age;

    private User(Builder builder) {

        username = builder.username;
        age = builder.age;
    }

    public static class Builder {

        private String username;
        private int age;

        public Builder username(String username) {

            this.username = username;
            return this;
        }

        public Builder age(int age) {

            this.age = age;
            return this;
        }

        public User build() {

            return new User(this);
        }
    }
}