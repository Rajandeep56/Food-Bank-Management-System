package model;

import java.time.*;
public abstract class User {
    private final int userId;
    private String name;
    private String email;
    private String password;
    private String phone;
    private final String role;
    private final Instant createdAt;
    private boolean active = true;
    private int failedLogins;
    private Instant lockedUntil;
    public int getFailedLogins() {
        return failedLogins;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void restoreLoginState(int attempts, Instant until) {
        failedLogins = attempts;
        lockedUntil = until;
    }


    protected User(int userId, String name, String email, String password, String phone, String role) {
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

        if(password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;

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
    public String getPassword() {
        return password;
    }
    @Deprecated
    public String getPasswordHash() {
        return getPassword();
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
    public boolean verifyPassword(String password) {
        return this.password.equals(password);
    }
    public void setPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = password;
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (!verifyPassword(oldPassword)) {
            throw new IllegalArgumentException(
                "Incorrect old password"
            );
        }

        setPassword(newPassword);
    }

}
