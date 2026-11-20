package Services;

import Repository.UserRepository;
import database.DatabaseWriter;
import model.Donor;
import model.Recipient;
import model.User;

public class AuthService {

    private final UserRepository userRepository;
    private User currentUser;
    private DatabaseWriter dbWriter = new DatabaseWriter();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String email, String password) {

        User user = userRepository.findByEmail(email);

        if (!user.isActive()) {
            throw new IllegalStateException("Account is inactive");
        }

        if (!user.verifyPassword(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        this.currentUser = user;
        return user;
    }

    public void register(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!(user instanceof Donor) &&
            !(user instanceof Recipient)) {

            throw new SecurityException(
                "Only donors and recipients can register"
            );
        }
        dbWriter.addUser(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getPhone(),
            user.getPasswordHash(),
            "Donor"
        );
        userRepository.save(user);
    }
    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {

        if (currentUser == null || !currentUser.isActive()) {
            throw new SecurityException("Please login first");
        }

        return currentUser;
    }
    public boolean isLoggedIn() {
        return currentUser != null && currentUser.isActive();
    }

    public void updateProfile(String name, String phone) {

        User user = getCurrentUser();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        dbWriter.updateUser(user.getUserId(), name, phone);

        user.updateProfile(name, user.getEmail(), phone);
    }

   
    
    public void changePassword(
        String oldPassword,
        String newPassword
    ) {
        User user = getCurrentUser();
        dbWriter.changePassword(user.getUserId(), newPassword);
        user.changePassword(oldPassword, newPassword);
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