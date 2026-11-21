package Services;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import Repository.UserRepository;
import database.DatabaseWriter;
import model.Donor;
import model.Recipient;
import model.User;

public class AuthService {

    private final UserRepository userRepository;
    private User currentUser;
    private final Clock clock;
    private Instant lastActivity;
    private DatabaseWriter dbWriter = new DatabaseWriter();

    public AuthService(UserRepository userRepository) {
        this(userRepository, Clock.systemUTC());
    }

    public AuthService(UserRepository users, Clock clock) {
        if (users == null || clock == null) {
            throw new IllegalArgumentException("User repository and clock are required");
        }
        this.userRepository = users;
        this.clock = clock;
    }

    public User login(String email, String password) {

        currentUser = null;
        User user = userRepository.findByEmailOrNull(email);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        Instant now = clock.instant();
        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            throw new SecurityException("Account temporarily locked");
        }
        int failed = user.getFailedLogins();
        if (user.getLockedUntil() != null) {
            failed = 0; // A previous lock has expired.
        }
        if (!user.verifyPassword(password)) {
            failed++;
            Instant until = null;
            if (failed >= 5) {
                until = now.plusSeconds(15 * 60);
            }
            dbWriter.updateLoginState(user.getUserId(), failed, until);
            user.restoreLoginState(failed, until);
            throw new IllegalArgumentException("Invalid credentials");
        }
        dbWriter.updateLoginState(user.getUserId(), 0, null);
        user.restoreLoginState(0, null);
        currentUser = user;
        lastActivity = now;
        return user;
    }

    public void register(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!(user instanceof Donor) && !(user instanceof Recipient)) {
            throw new SecurityException(
                "Only donors and recipients can self-register"
            );
        }

        userRepository.validateNewUser(user);
        if (user instanceof Recipient recipient) {

            dbWriter.registerRecipient(
                recipient.getUserId(),
                recipient.getName(),
                recipient.getEmail(),
                recipient.getPhone(),
                recipient.getPassword(),
                recipient.getHouseholdSize(),
                recipient.getDietaryRestrictions(),
                recipient.getEmergencyContact()
            );

        } else if (user instanceof Donor) {

            dbWriter.addUser(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getPassword(),
                "Donor"
            );
        }
        userRepository.save(user);
    }
    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {

        if (!isLoggedIn()) {
            currentUser = null;
            throw new SecurityException("Please login first");
        }
        lastActivity = clock.instant();
        return currentUser;
    }

    public boolean isLoggedIn() {
        if (currentUser == null || !currentUser.isActive() || lastActivity == null) {
            return false;
        }
        Instant expiresAt = lastActivity.plusSeconds(30 * 60);
        return clock.instant().isBefore(expiresAt);
    }

    public void updateProfile(
        String name, String email, String phone) {

        User user = getCurrentUser();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (email == null ||
            !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid email");
        }

        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be empty");
        }

        email = email.trim().toLowerCase(Locale.ROOT);
        name = name.trim();
        phone = phone.trim();

        User existingUser = userRepository.findByEmailOrNull(email);

        if (existingUser != null &&
            existingUser.getUserId() != user.getUserId()) {

            throw new IllegalArgumentException(
                "Email already registered"
            );
        }

        dbWriter.updateUser(
            user.getUserId(), name, email, phone
        );

        user.updateProfile(name, email, phone);
    }

   
    
    public void changePassword(
        String oldPassword,
        String newPassword
    ) {
        User user = getCurrentUser();
        if (!user.verifyPassword(oldPassword)) {
            throw new IllegalArgumentException("Incorrect old password");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        dbWriter.changePassword(user.getUserId(), newPassword);
        user.setPassword(newPassword);
    }

    public void updateRecipientProfile(int householdSize, String dietaryRestrictions, String emergencyContact) {
        Recipient recipient = (Recipient) requireRole("Recipient");
        Recipient.validateDetails(householdSize, emergencyContact);
        dbWriter.updateRecipientProfile(recipient.getUserId(), householdSize, dietaryRestrictions, emergencyContact);
        recipient.updateDetails(householdSize, dietaryRestrictions, emergencyContact);
    }

    public User requireRole(String role) {

        User user = getCurrentUser();

        if (!user.getRole().equalsIgnoreCase(role)) {
            throw new SecurityException("Access denied");
        }

        return user;
    }
    public int getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}