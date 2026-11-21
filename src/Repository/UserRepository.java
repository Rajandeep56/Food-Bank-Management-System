package Repository;

import java.util.*;

import common.RecipientApprovalStatus;
import model.Coordinator;
import model.Donor;
import model.Recipient;
import model.User;
import model.Volunteer;
import database.DatabaseConnection;
import java.sql.*;


public class UserRepository {
    private final Map<Integer, User> users = new HashMap<>();
    private static final String USER_QUERY =
        """
        SELECT u.*,
            r.household_size,
            r.dietary_restrictions,
            r.emergency_contact,
            r.approval_status,
            v.availability
        FROM users u
        LEFT JOIN recipients r
            ON u.user_id = r.recipient_id
        LEFT JOIN volunteers v
            ON u.user_id = v.volunteer_id
        """;
    
    public User mapUser(ResultSet rs) throws SQLException {

        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password_hash");
        String phone = rs.getString("phone");
        String role = rs.getString("role");

        User user;
        String approvalStatus = rs.getString("approval_status");
        switch (role) {
            case "Donor":
                user = new Donor(id, name, email, password, phone);
                break;
            case "Volunteer":
                user = new Volunteer(
                    id, name, email, password, phone,
                    rs.getString("availability")
                );
                break;
            case "Recipient":
                user = new Recipient(
                    id, name, email, password, phone,
                    rs.getInt("household_size"),
                    rs.getString("dietary_restrictions"),
                    rs.getString("emergency_contact")
                );
                break;
            case "Coordinator":
                user = new Coordinator(
                    id, name, email, password, phone
                );
                break;

            default:
                throw new SQLException("Invalid user role");
        }
        if (!rs.getBoolean("is_active")) {
            user.deactivate();
        }
        if (user instanceof Recipient) {
            ((Recipient) user).updateApprovalStatus(RecipientApprovalStatus.valueOf(rs.getString("approval_status").toUpperCase() ) );
        }

        Timestamp lock = rs.getTimestamp("locked_until");
        user.restoreLoginState(rs.getInt("failed_logins"), lock == null ? null : lock.toInstant());
        return user;
    }

    public void validateNewUser(User user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (users.containsKey(user.getUserId())) throw new IllegalArgumentException("User ID already exists");
        if (findByEmailOrNull(user.getEmail()) != null) throw new IllegalArgumentException("Email already registered");
    }

    public User findByEmailOrNull(String email) {
        if (email == null) return null;
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(email.trim())) return user;
        }
        return null;
    }

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

    public User findByEmail(String email) {

        for (User user : users.values()) {

            if (user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }

        throw new IllegalArgumentException("User not found");
    }
}
