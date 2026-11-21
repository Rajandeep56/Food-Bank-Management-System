
package model;

import java.time.LocalDate;
import java.time.LocalTime;

import Repository.UserRepository;

public class VolunteerShift {

    private final int shiftId;
    private final int volunteerId;
    private final LocalDate shiftDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String assignedTask;
    private final int assignedBy;
    private UserRepository userRepository;

    public VolunteerShift(
            int shiftId,
            int volunteerId,
            LocalDate shiftDate,
            LocalTime startTime,
            LocalTime endTime,
            String assignedTask,
            int assignedBy) {

        if (shiftId <= 0 || volunteerId <= 0 || assignedBy <= 0) {
            throw new IllegalArgumentException(
                "IDs must be positive"
            );
        }

        if (shiftDate == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                "Shift date and times cannot be null"
            );
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                "End time must be after start time"
            );
        }

        if (assignedTask == null || assignedTask.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Assigned task cannot be empty"
            );
        }

        this.shiftId = shiftId;
        this.volunteerId = volunteerId;
        this.shiftDate = shiftDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.assignedTask = assignedTask.trim();
        this.assignedBy = assignedBy;
    }

    public int getShiftId() {
        return shiftId;
    }

    public int getVolunteerId() {
        return volunteerId;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getAssignedTask() {
        return assignedTask;
    }

    public int getAssignedBy() {
        return assignedBy;
    }
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
