package org.example.item02_builder.bestpractice;

public class User {

    private final String username;
    private final String email;
    private final int age;

    private User(Builder builder) {

        this.username = builder.username;
        this.email = builder.email;
        this.age = builder.age;
    }

    public static class Builder {

        private final String username;

        private String email;

        private int age = 18;

        public Builder(String username) {

            if (username == null || username.isBlank()) {

                throw new IllegalArgumentException(
                        "username required");
            }

            this.username = username;
        }

        public Builder email(String email) {

            this.email = email;
            return this;
        }

        public Builder age(int age) {

            if (age <= 0) {

                throw new IllegalArgumentException();
            }

            this.age = age;

            return this;
        }

        public User build() {

            return new User(this);
        }
    }
}