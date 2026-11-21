package database;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import model.DistributionItem;
import model.Donation;
import model.InventoryHistory;



public class DatabaseWriter {
    public static boolean enabled = false;

    public void addCategory(int id, String name) {
        if (!enabled) {
            return;
        }

        String sql = "INSERT INTO categories (category_id, name) VALUES (?, ?)";

        try {
            Connection conn = DatabaseConnection.getConnection();

            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, id);
            stmt.setString(2, name);

            stmt.executeUpdate();

            stmt.close();
            conn.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void updateCategory(int id, String name) {
        if (!enabled) {
            return;
        }

    String sql = "UPDATE categories SET name = ? WHERE category_id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, name);
        stmt.setInt(2, id);

        stmt.executeUpdate();

    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}


    public void deleteCategory(int id) {
        if (!enabled) {
            return;
        }

        String sql = "DELETE FROM categories WHERE category_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUser(int id, String name, String email,
        String phone, String password, String role) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO users
            (user_id, name, email, phone, password_hash, role)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, password);
            stmt.setString(6, role);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUser(int id, String name, String phone) {

    if (!enabled) {
        return;
    }

    String sql = """
        UPDATE users
        SET name = ?, phone = ?
        WHERE user_id = ?
        """;

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, name);
        stmt.setString(2, phone);
        stmt.setInt(3, id);

        if (stmt.executeUpdate() != 1) {
            throw new SQLException("User not found");
        }

    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}

    public void updateUser(
            int id, String name, String email, String phone) {

        if (!enabled) {
            return;
        }

        String sql = """
            UPDATE users
            SET name = ?, email = ?, phone = ?
            WHERE user_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setInt(4, id);

            if (stmt.executeUpdate() != 1) {
                throw new SQLException("User not found");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void deactivateUser(int id) {
        if (!enabled) {
            return;
        }

        String sql = "UPDATE users SET is_active = false WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void changePassword(int id, String password) {
        if (!enabled) {
            return;
        }

        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, password);
            stmt.setInt(2, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void addDonation(Donation donation) {
        if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO donations
            (donation_id, donor_id, recorded_by, item_name,
            category_id, quantity, unit, donation_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donation.getDonationId());
            stmt.setInt(2, donation.getDonorId());
            stmt.setInt(3, donation.getRecordedBy());
            stmt.setString(4, donation.getItemName());
            stmt.setInt(5, donation.getCategoryId());
            stmt.setInt(6, donation.getQuantity());
            stmt.setString(7, donation.getUnit());
            stmt.setTimestamp(8, Timestamp.from(donation.getDonationDate()));
            stmt.setString(9, donation.getStatus().name());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void updateDonation(Donation donation) {
        if (!enabled) {
            return;
        }
        String sql = """
            UPDATE donations
            SET item_name = ?, category_id = ?,
                quantity = ?, unit = ?, status = ?
            WHERE donation_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, donation.getItemName());
            stmt.setInt(2, donation.getCategoryId());
            stmt.setInt(3, donation.getQuantity());
            stmt.setString(4, donation.getUnit());
            stmt.setString(5, donation.getStatus().name());
            stmt.setInt(6, donation.getDonationId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateInventory(int id, int quantity) {
        if (!enabled) {
            return;
        }

        String sql = "UPDATE inventory SET quantity = ? WHERE inventory_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addInventory(int id, Integer donationId,
        String name, int categoryId, int quantity,
        String unit, LocalDate expiryDate) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO inventory
            (inventory_id, donation_id, item_name, category_id,
            quantity, quantity_received, unit, expiry_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setObject(2, donationId);
            stmt.setString(3, name);
            stmt.setInt(4, categoryId);
            stmt.setInt(5, quantity);
            stmt.setInt(6, quantity);
            stmt.setString(7, unit);
            stmt.setObject(8, expiryDate);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void discardInventory(int id) {
        if (!enabled) {
            return;
        }

        String sql = """
            UPDATE inventory
            SET quantity = 0, status = 'Discarded',
                waste_date = CURRENT_DATE
            WHERE inventory_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void addRecipient(int id, int householdSize,
        String dietaryRestrictions, String emergencyContact) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO recipients
            (recipient_id, household_size,
            dietary_restrictions, emergency_contact, approval_status)
            VALUES (?, ?, ?, ?, 'Pending')
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, householdSize);
            stmt.setString(3, dietaryRestrictions);
            stmt.setString(4, emergencyContact);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateRecipientStatus(int id, String status,
        int coordinatorId) {
            if (!enabled) {
            return;
        }
        String sql = """
            UPDATE recipients
            SET approval_status = ?, approved_by = ?,
                approved_at = CURRENT_TIMESTAMP
            WHERE recipient_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, coordinatorId);
            stmt.setInt(3, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void addVolunteer(int id, String availability,
        int coordinatorId) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO volunteers
            (volunteer_id, availability, created_by)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setString(2, availability);
            stmt.setInt(3, coordinatorId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateVolunteer(int id, String availability) {
        if (!enabled) {
            return;
        }

        String sql = """
            UPDATE volunteers SET availability = ?
            WHERE volunteer_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, availability);
            stmt.setInt(2, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addHamperRequest(int id, int recipientId,
        LocalDate pickupDate) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO hamper_requests
            (request_id, recipient_id, pickup_date, status)
            VALUES (?, ?, ?, 'PENDING')
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, recipientId);
            stmt.setObject(3, pickupDate);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateHamperStatus(int id, String status) {
        if (!enabled) {
            return;
        }

        String sql = "UPDATE hamper_requests SET status = ? WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addDistribution(int id, int requestId,
        LocalDate distributionDate) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO distributions
            (distribution_id, request_id, distribution_date, pickup_status)
            VALUES (?, ?, ?, 'SCHEDULED')
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, requestId);
            stmt.setObject(3, distributionDate);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDistribution(int id, String status,
            LocalTime pickupTime, Integer volunteerId) {
                if (!enabled) {
            return;
        }

        String sql = """
            UPDATE distributions
            SET pickup_status = ?, pickup_time = ?, volunteer_id = ?
            WHERE distribution_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setObject(2, pickupTime);
            stmt.setObject(3, volunteerId);
            stmt.setInt(4, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteDistribution(int id) {
        if (!enabled) {
            return;
        }

        String sql = "DELETE FROM distributions WHERE distribution_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void addDistributionItem(int id, int distributionId,
        int inventoryId, int quantity, String unit) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO distribution_items
            (distribution_item_id, distribution_id,
            inventory_id, quantity, unit)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, distributionId);
            stmt.setInt(3, inventoryId);
            stmt.setInt(4, quantity);
            stmt.setString(5, unit);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDistributionItem(int id, int quantity) {
        if (!enabled) {
            return;
        }

        String sql = """
            UPDATE distribution_items SET quantity = ?
            WHERE distribution_item_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteDistributionItem(int id) {
        if (!enabled) {
            return;
        }

        String sql = """
            DELETE FROM distribution_items WHERE distribution_item_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addVolunteerShift(int id, int volunteerId,
        int coordinatorId, LocalDate date,
        LocalTime start, LocalTime end, String task) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO volunteer_shifts
            (shift_id, volunteer_id, coordinator_id,
            shift_date, start_time, end_time, task)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, volunteerId);
            stmt.setInt(3, coordinatorId);
            stmt.setObject(4, date);
            stmt.setObject(5, start);
            stmt.setObject(6, end);
            stmt.setString(7, task);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateVolunteerShift(int id, LocalDate date,
            LocalTime start, LocalTime end, String task) {
                if (!enabled) {
            return;
        }

        String sql = """
            UPDATE volunteer_shifts
            SET shift_date = ?, start_time = ?, end_time = ?, task = ?
            WHERE shift_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, date);
            stmt.setObject(2, start);
            stmt.setObject(3, end);
            stmt.setString(4, task);
            stmt.setInt(5, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteVolunteerShift(int id) {
        if (!enabled) {
            return;
        }

        String sql = "DELETE FROM volunteer_shifts WHERE shift_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void addNotification(int id, int userId,
        String email, String type, String message) {
            if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO notifications
            (notification_id, recipient_user_id,
            recipient_email, notification_type, message)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            stmt.setString(3, email);
            stmt.setString(4, type);
            stmt.setString(5, message);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void markNotificationSent(int id) {
        if (!enabled) {
            return;
        }

        String sql = """
            UPDATE notifications
            SET is_sent = true, sent_at = CURRENT_TIMESTAMP
            WHERE notification_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void addInventoryHistory(InventoryHistory record) {
        if (!enabled) {
            return;
        }

        String sql = """
            INSERT INTO inventory_history
            (inventory_id, category_id, unit,
            quantity_change, change_type, occurred_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, record.getInventoryId());
            stmt.setInt(2, record.getCategoryId());
            stmt.setString(3, record.getUnit());
            stmt.setInt(4, record.getQuantityChange());
            stmt.setString(5, record.getChangeType().name());
            stmt.setTimestamp(6, Timestamp.from(record.getOccurredAt()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void registerRecipient(int id, String name, String email,
        String phone, String password, int householdSize,
        String dietaryRestrictions, String emergencyContact) {
            if (!enabled) {
            return;
        }

        String userSql = """
            INSERT INTO users
            (user_id, name, email, phone, password_hash, role)
            VALUES (?, ?, ?, ?, ?, 'Recipient')
            """;

        String recipientSql = """
            INSERT INTO recipients
            (recipient_id, household_size, dietary_restrictions,
            emergency_contact, approval_status)
            VALUES (?, ?, ?, ?, 'Pending')
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement userStmt = conn.prepareStatement(userSql);
                PreparedStatement recipientStmt = conn.prepareStatement(recipientSql)) {

                userStmt.setInt(1, id);
                userStmt.setString(2, name);
                userStmt.setString(3, email);
                userStmt.setString(4, phone);
                userStmt.setString(5, password);
                userStmt.executeUpdate();

                recipientStmt.setInt(1, id);
                recipientStmt.setInt(2, householdSize);
                recipientStmt.setString(3, dietaryRestrictions);
                recipientStmt.setString(4, emergencyContact);
                recipientStmt.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerVolunteer(int id, String name,
        String email, String password, String phone,
        String availability, int coordinatorId) {
            if (!enabled) {
            return;
        }

        String userSql = """
            INSERT INTO users
            (user_id, name, email, password_hash, phone, role)
            VALUES (?, ?, ?, ?, ?, 'Volunteer')
            """;

        String volunteerSql = """
            INSERT INTO volunteers
            (volunteer_id, availability, created_by)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement userStmt = conn.prepareStatement(userSql);
                PreparedStatement volunteerStmt = conn.prepareStatement(volunteerSql)) {

                userStmt.setInt(1, id);
                userStmt.setString(2, name);
                userStmt.setString(3, email);
                userStmt.setString(4, password);
                userStmt.setString(5, phone);
                userStmt.executeUpdate();

                volunteerStmt.setInt(1, id);
                volunteerStmt.setString(2, availability);
                volunteerStmt.setInt(3, coordinatorId);
                volunteerStmt.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void completeDonation(int donationId, int inventoryId,
        String itemName, int categoryId, int quantity,
        String unit, LocalDate expiryDate) {
            if (!enabled) {
            return;
        }

        String donationSql = """
            UPDATE donations SET status = 'COMPLETED'
            WHERE donation_id = ? AND status = 'PENDING'
            """;

        String inventorySql = """
            INSERT INTO inventory
            (inventory_id, donation_id, item_name, category_id,
            quantity, quantity_received, unit, expiry_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String historySql = """
            INSERT INTO inventory_history
            (inventory_id, category_id, unit,
            quantity_change, change_type)
            VALUES (?, ?, ?, ?, 'DONATION_RECEIVED')
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement donationStmt = conn.prepareStatement(donationSql);
                PreparedStatement inventoryStmt = conn.prepareStatement(inventorySql);
                PreparedStatement historyStmt = conn.prepareStatement(historySql)) {

                // Update donation
                donationStmt.setInt(1, donationId);

                if (donationStmt.executeUpdate() != 1) {
                    throw new SQLException("Donation is not pending");
                }

                // Add inventory
                inventoryStmt.setInt(1, inventoryId);
                inventoryStmt.setInt(2, donationId);
                inventoryStmt.setString(3, itemName);
                inventoryStmt.setInt(4, categoryId);
                inventoryStmt.setInt(5, quantity);
                inventoryStmt.setInt(6, quantity);
                inventoryStmt.setString(7, unit);
                inventoryStmt.setObject(8, expiryDate);

                inventoryStmt.executeUpdate();

                // Add inventory history
                historyStmt.setInt(1, inventoryId);
                historyStmt.setInt(2, categoryId);
                historyStmt.setString(3, unit);
                historyStmt.setInt(4, quantity);

                historyStmt.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void changeInventory(int id, int quantity,
        InventoryHistory record) {

        if (!enabled) return;

        String update = """
            UPDATE inventory SET quantity = ?
            WHERE inventory_id = ?
            """;

        String history = """
            INSERT INTO inventory_history
            (inventory_id, category_id, unit,
            quantity_change, change_type, occurred_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement a = conn.prepareStatement(update);
                PreparedStatement b = conn.prepareStatement(history)) {

                a.setInt(1, quantity);
                a.setInt(2, id);

                if (a.executeUpdate() != 1) {
                    throw new SQLException("Inventory not found");
                }

                b.setInt(1, record.getInventoryId());
                b.setInt(2, record.getCategoryId());
                b.setString(3, record.getUnit());
                b.setInt(4, record.getQuantityChange());
                b.setString(5, record.getChangeType().name());
                b.setTimestamp(6, Timestamp.from(record.getOccurredAt()));
                b.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void updatePickupStatus(int distributionId, String status) {

        if (!enabled) return;

        String sql = """
            UPDATE distributions
            SET pickup_status = ?
            WHERE distribution_id = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, distributionId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void fulfillDistribution(int distributionId,
        int requestId, int volunteerId,
        List<DistributionItem> items) {

        if (!enabled) return;

        String distributionSql = """
            UPDATE distributions
            SET pickup_status = 'FULFILLED', volunteer_id = ?
            WHERE distribution_id = ?
            AND request_id = ?
            AND pickup_status IN ('SCHEDULED', 'DELAYED')
            """;

        String inventorySql = """
            UPDATE inventory
            SET quantity = quantity - ?
            WHERE inventory_id = ?
            AND quantity >= ?
            """;

        String historySql = """
            INSERT INTO inventory_history
            (inventory_id, category_id, unit,
            quantity_change, change_type)
            SELECT inventory_id, category_id, unit,
                ?, 'DISTRIBUTED'
            FROM inventory WHERE inventory_id = ?
            """;

        String hamperSql = """
            UPDATE hamper_requests SET status = 'FULFILLED'
            WHERE request_id = ? AND status = 'SCHEDULED'
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement a = conn.prepareStatement(distributionSql);
                PreparedStatement b = conn.prepareStatement(inventorySql);
                PreparedStatement c = conn.prepareStatement(historySql);
                PreparedStatement d = conn.prepareStatement(hamperSql)) {

                a.setInt(1, volunteerId);
                a.setInt(2, distributionId);
                a.setInt(3, requestId);

                if (a.executeUpdate() != 1) {
                    throw new SQLException("Invalid distribution status");
                }

                for (DistributionItem item : items) {

                    b.setInt(1, item.getQuantity());
                    b.setInt(2, item.getInventoryId());
                    b.setInt(3, item.getQuantity());

                    if (b.executeUpdate() != 1) {
                        throw new SQLException("Insufficient inventory");
                    }

                    c.setInt(1, -item.getQuantity());
                    c.setInt(2, item.getInventoryId());
                    c.executeUpdate();
                }

                d.setInt(1, requestId);

                if (d.executeUpdate() != 1) {
                    throw new SQLException("Invalid hamper request");
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createHamperRequest(int requestId, int recipientId, LocalDate pickupDate, LocalTime pickupTime) {

        if (!enabled) return;

        String requestSql = """
            INSERT INTO hamper_requests
            (request_id, recipient_id, pickup_date, status)
            VALUES (?, ?, ?, 'SCHEDULED')
            """;

        String distributionSql = """
            INSERT INTO distributions
            (distribution_id, request_id, distribution_date, pickup_status)
            VALUES (?, ?, ?, 'SCHEDULED')
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement a = conn.prepareStatement(requestSql);
                PreparedStatement b = conn.prepareStatement(distributionSql)) {

                a.setInt(1, requestId);
                a.setInt(2, recipientId);
                a.setObject(3, pickupDate);
                a.executeUpdate();

                b.setInt(1, requestId);
                b.setInt(2, requestId);
                b.setObject(3, pickupDate);
                b.setObject(4, pickupTime);
                b.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createHamperRequest(int requestId, int recipientId, LocalDate pickupDate) {

        createHamperRequest(requestId, recipientId, pickupDate, LocalTime.of(10, 0));
    }
    
    public void cancelHamperRequest(int requestId) {

        if (!enabled) return;

        String update = """
            UPDATE hamper_requests SET status = 'CANCELLED'
            WHERE request_id = ?
            AND status IN ('PENDING', 'SCHEDULED')
            """;

        String delete = """
            DELETE FROM distributions
            WHERE request_id = ?
            AND pickup_status IN ('SCHEDULED', 'DELAYED')
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement a = conn.prepareStatement(update);
                PreparedStatement b = conn.prepareStatement(delete)) {

                a.setInt(1, requestId);

                if (a.executeUpdate() != 1) {
                    throw new SQLException("Request cannot be cancelled");
                }

                b.setInt(1, requestId);
                b.executeUpdate();

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}