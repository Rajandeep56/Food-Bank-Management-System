package database;


import java.sql.*;
import java.time.*;

import Repository.*;
import model.*;
import common.*;
import Services.InventoryService;

public class DatabaseLoader {
    private static int wholeQuantity(ResultSet rs, String column) throws SQLException {
        java.math.BigDecimal value = rs.getBigDecimal(column);
        try {
            if (value == null) throw new ArithmeticException("Null quantity");
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw new SQLException("Unsupported fractional or out-of-range quantity in " + column, e);
        }
    }


    public void loadUsers(UserRepository userRepository) {

        String sql = """
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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                userRepository.save(userRepository.mapUser(rs));
            }

            System.out.println("Users loaded successfully");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load users", e);
        }
    }

    public void loadCategories(CategoryRepository repo) {

        String sql = "SELECT * FROM categories";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                repo.save(new Category(
                    rs.getInt("category_id"),
                    rs.getString("name")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadDonations(DonationRepository repo) {

        String sql = "SELECT * FROM donations";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

               Donation donation = new Donation(
                rs.getInt("donation_id"),
                rs.getInt("donor_id"),
                rs.getInt("recorded_by"),
                rs.getString("item_name"),
                rs.getInt("category_id"),
                wholeQuantity(rs, "quantity"),
                rs.getString("unit")
            );

            donation.restoreFromDatabase(
                rs.getTimestamp("donation_date").toInstant(),
                DonationStatus.valueOf(rs.getString("status"))
            );

            repo.save(donation);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadInventory(InventoryRepository repo) {

        String sql = "SELECT * FROM inventory";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                InventoryItem item = new InventoryItem(
                    rs.getInt("inventory_id"),
                    rs.getString("item_name"),
                    rs.getInt("category_id"),
                    wholeQuantity(rs, "quantity"),
                    rs.getString("unit"),
                    rs.getObject("expiry_date", java.time.LocalDate.class),
                    rs.getInt("donation_id")
                );

                item.restoreFromDatabase(rs.getDate("received_date").toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    wholeQuantity(rs, "quantity_received"), rs.getString("status"), rs.getObject("waste_date", LocalDate.class));
                repo.save(item);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadHamperRequests(FoodHamperRequestRepository repo) {

        String sql = "SELECT * FROM hamper_requests";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                FoodHamperRequest request = new FoodHamperRequest(
                    rs.getInt("request_id"),
                    rs.getInt("recipient_id")
                );

                request.restoreFromDatabase(rs.getObject("request_date", LocalDate.class),
                    rs.getObject("pickup_date", LocalDate.class), HamperRequestStatus.valueOf(rs.getString("status")));
                repo.save(request);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadDistributions(DistributionRepository repo) {

        String sql = "SELECT * FROM distributions";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Distribution distribution = new Distribution(
                    rs.getInt("distribution_id"),
                    rs.getInt("request_id"),
                    rs.getObject("distribution_date", LocalDate.class)
                );

                LocalTime time = rs.getObject(
                    "pickup_time", LocalTime.class
                );

                if (time != null) {
                    distribution.setPickupTime(time);
                }

                distribution.setVolunteerId((Integer) rs.getObject("volunteer_id"));
                repo.save(distribution);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadDistributionItems(DistributionRepository repo) {

        String sql = "SELECT * FROM distribution_items";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Distribution distribution = repo.findById(
                    rs.getInt("distribution_id")
                );

                DistributionItem item = new DistributionItem(
                    rs.getInt("distribution_item_id"),
                    rs.getInt("inventory_id"),
                    wholeQuantity(rs, "quantity"),
                    rs.getString("unit")
                );

                distribution.addItem(item);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadVolunteerShifts(VolunteerShiftRepository repo) {

        String sql = "SELECT * FROM volunteer_shifts";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                VolunteerShift shift = new VolunteerShift(
                    rs.getInt("shift_id"),
                    rs.getInt("volunteer_id"),
                    rs.getObject("shift_date", LocalDate.class),
                    rs.getObject("start_time", LocalTime.class),
                    rs.getObject("end_time", LocalTime.class),
                    rs.getString("task"),
                    rs.getInt("coordinator_id")
                );

                repo.save(shift);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load shifts", e);
        }
    }


    public void loadNotifications(NotificationRepository repo) {

        String sql = "SELECT * FROM notifications";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                model.Notification notification = new model.Notification(
                    rs.getInt("notification_id"),
                    common.NotificationType.valueOf(
                        rs.getString("notification_type")
                    ),
                    rs.getInt("recipient_user_id"),
                    rs.getString("recipient_email"),
                    rs.getString("message")
                );

                notification.setReason(rs.getString("reason"));
                notification.restoreFromDatabase( rs.getTimestamp("created_at").toInstant(), rs.getBoolean("is_sent"));
                repo.save(notification);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load notifications", e);
        }
    }


    public void loadDistributionStatuses(DistributionRepository repo) {

        String sql = "SELECT distribution_id, pickup_status FROM distributions";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Distribution distribution = repo.findById(
                    rs.getInt("distribution_id")
                );

                PickupStatus status = PickupStatus.valueOf(
                    rs.getString("pickup_status")
                );

                if (status != PickupStatus.SCHEDULED) {
                    distribution.markPickupStatus(status);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load distribution statuses", e);
        }
    }


    public void loadInventoryHistory(InventoryService service) {

        String sql = "SELECT * FROM inventory_history ORDER BY occurred_at";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                 InventoryHistory record = new InventoryHistory(
                        rs.getInt("inventory_id"),
                        wholeQuantity(rs, "quantity_change"),
                        InventoryChangeType.valueOf(rs.getString("change_type")),
                        rs.getInt("category_id"),
                        rs.getString("unit")
                    );

                record.restoreFromDatabase( rs.getTimestamp("occurred_at").toInstant());

                service.loadHistory(record);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load inventory history", e);
        }
}
}