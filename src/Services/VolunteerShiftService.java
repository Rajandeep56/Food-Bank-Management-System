package Services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


import Repository.NotificationRepository;
import Repository.VolunteerShiftRepository;
import common.NotificationType;
import database.DatabaseConnection;
import database.DatabaseWriter;
import model.Notification;
import model.User;
import model.VolunteerShift;

public class VolunteerShiftService {

    private final VolunteerShiftRepository shiftRepository;
    private DatabaseWriter dbWriter = new DatabaseWriter();

    public VolunteerShiftService(
            VolunteerShiftRepository shiftRepository) {

        if (shiftRepository == null) {
            throw new IllegalArgumentException(
                "Repository cannot be null"
            );
        }

        this.shiftRepository = shiftRepository;
    }

    public void assignShift(VolunteerShift newShift, AuthService authService) {
        authService.requireRole("Coordinator");
        if (newShift == null) {
            throw new IllegalArgumentException("Shift cannot be null");
        }

        List<VolunteerShift> existingShifts =
            shiftRepository.findByVolunteerId(newShift.getVolunteerId());

        for (VolunteerShift existing : existingShifts) {

            if (existing.getShiftDate().equals(newShift.getShiftDate())
                && newShift.getStartTime().isBefore(existing.getEndTime())
                && newShift.getEndTime().isAfter(existing.getStartTime())) {

                throw new IllegalStateException("Overlapping volunteer shift");
            }
        }
        dbWriter.addVolunteerShift(
            newShift.getShiftId(),
            newShift.getVolunteerId(),
            newShift.getAssignedBy(),
            newShift.getShiftDate(),
            newShift.getStartTime(),
            newShift.getEndTime(),
            newShift.getAssignedTask()
        );
        shiftRepository.save(newShift);
    }
    public List<VolunteerShift> getVolunteerShifts(AuthService authService){
        User volunteer = authService.requireRole("Volunteer");

        return shiftRepository.findByVolunteerId(
            volunteer.getUserId()
        );

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
            throw new RuntimeException(e);
        }
    }

    public void loadNotifications(NotificationRepository repo) {

        String sql = "SELECT * FROM notifications";

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Notification notification = new Notification(
                    rs.getInt("notification_id"),
                    NotificationType.valueOf(
                        rs.getString("notification_type")
                    ),
                    rs.getInt("recipient_user_id"),
                    rs.getString("recipient_email"),
                    rs.getString("message")
                );

                if (rs.getBoolean("is_sent")) {
                    notification.markSent();
                }

                repo.save(notification);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}