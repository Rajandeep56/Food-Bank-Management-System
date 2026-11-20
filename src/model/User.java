package model;

import java.time.*;
// abstract class to rpevent direct instantiation of User, as we will have different types of users
public abstract class User {
    private final int userId;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private final String role;
    private final Instant createdAt;
    private boolean active = true;

    protected User(int userId, String name, String email, String passwordHash, String phone, String role) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        this.userId = userId;
        this.role = role;
        this.createdAt = Instant.now();

        updateProfile(name, email, phone);

        if(passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be null or empty");
        }
        this.passwordHash = passwordHash;

    }

    public void updateProfile(String name, String email, String phone) {
         if(name == null || name.trim().isEmpty()|| email == null || email.trim().isEmpty()|| phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Name, email, and phone cannot be null or empty");
        }
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public int getUserId() {
        return userId;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;   
    }
    public String getRole() {
        return role;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

}
