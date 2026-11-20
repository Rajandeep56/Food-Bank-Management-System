package Services;

import Repository.UserRepository;
import model.Donor;
import model.Recipient;
import model.User;

public class AuthService {

    private final UserRepository userRepository;
    private User currentUser;

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

        user.updateProfile(name, user.getEmail(), phone);
    }

   
    
    public void changePassword(
        String oldPassword,
        String newPassword
    ) {
        User user = getCurrentUser();

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