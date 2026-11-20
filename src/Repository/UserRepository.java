package Repository;

import java.util.*;

import model.User;


public class UserRepository {
    private final Map<Integer, User> users = new HashMap<>();

    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (users.containsKey(user.getUserId())) {
            throw new IllegalArgumentException("User with ID " + user.getUserId() + " already exists");
        }
        for (User existing : users.values()) {
            if (existing.getEmail().equalsIgnoreCase(user.getEmail())) {
                throw new IllegalArgumentException("A user with email " + user.getEmail() + " already exists");
            }
        }
        users.put(user.getUserId(), user);
    }

    public User findById(int userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }


    public List<User> findByRole(String role) {
        List<User> result = new ArrayList<>();
        for (User user : users.values()) {
            if (user.isActive() && user.getRole().equalsIgnoreCase(role)) {
                result.add(user);
            }
        }
        return result;
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }


   public void delete(int userId) {
        User user = findById(userId);
        user.deactivate();
    }
}
