package Services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import Repository.NotificationRepository;
import Repository.UserRepository;
import Repository.VolunteerShiftRepository;
import database.DatabaseLoader;
import database.DatabaseWriter;
import model.User;
import model.Volunteer;
import model.VolunteerShift;

public class VolunteerShiftService {
    private VolunteerShiftRepository shiftRepository;
    private UserRepository userRepository;
    private final DatabaseWriter dbWriter = new DatabaseWriter();

    public VolunteerShiftService(VolunteerShiftRepository shiftRepository) {
        setShiftRepository(shiftRepository);
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setShiftRepository(VolunteerShiftRepository shiftRepository) {
        if (shiftRepository == null) {
            throw new IllegalArgumentException("Shift repository cannot be null");
        }
        this.shiftRepository = shiftRepository;
    }

    public void assignShift(VolunteerShift shift, AuthService auth) {
        auth.requireRole("Coordinator");
        if (shift == null) {
            throw new IllegalArgumentException("Shift cannot be null");
        }
        if (shiftRepository.findById(shift.getShiftId()) != null) {
            throw new IllegalArgumentException("Shift ID already exists");
        }
        if (shift.getAssignedBy() != auth.getCurrentUserId()) {
            throw new SecurityException("Invalid assigning coordinator");
        }
        validateAssignment(shift);

        dbWriter.addVolunteerShift(shift.getShiftId(), shift.getVolunteerId(),
            shift.getAssignedBy(), shift.getShiftDate(), shift.getStartTime(),
            shift.getEndTime(), shift.getAssignedTask());
        shiftRepository.save(shift);
    }

    private void validateAssignment(VolunteerShift shift) {
        if (userRepository == null) {
            throw new IllegalStateException("User repository must be configured");
        }
        User user = userRepository.findById(shift.getVolunteerId());
        if (!(user instanceof Volunteer) || !user.isActive()) {
            throw new IllegalArgumentException("Active volunteer required");
        }
        Volunteer volunteer = (Volunteer) user;
        checkAvailability(volunteer, shift.getShiftDate());

        for (VolunteerShift existing : shiftRepository.findByVolunteerId(shift.getVolunteerId())) {
            boolean sameShift = existing.getShiftId() == shift.getShiftId();
            boolean sameDate = existing.getShiftDate().equals(shift.getShiftDate());
            boolean overlaps = shift.getStartTime().isBefore(existing.getEndTime())
                && shift.getEndTime().isAfter(existing.getStartTime());
            if (!sameShift && sameDate && overlaps) {
                throw new IllegalStateException("Overlapping volunteer shift");
            }
        }
    }

    private void checkAvailability(Volunteer volunteer, LocalDate date) {
        String availability = volunteer.getAvailability();
        if (availability == null) {
            return;
        }
        String days = availability.trim().toLowerCase(Locale.ROOT);
        boolean weekdaysOnly = days.equals("weekdays") || days.equals("monday to friday");
        boolean weekendsOnly = days.equals("weekends") || days.equals("saturday and sunday");
        boolean weekend = date.getDayOfWeek().getValue() >= 6;

        if ((weekdaysOnly && weekend) || (weekendsOnly && !weekend)) {
            throw new IllegalArgumentException("Volunteer unavailable on this day");
        }
    }

    public void updateShift(int id, LocalDate date, LocalTime start, LocalTime end,
            String task, AuthService auth) {
        auth.requireRole("Coordinator");
        VolunteerShift old = findShift(id);
        VolunteerShift updated = new VolunteerShift(id, old.getVolunteerId(), date,
            start, end, task, old.getAssignedBy());
        validateAssignment(updated);

        dbWriter.updateVolunteerShift(id, date, start, end, task);
        shiftRepository.update(updated);
    }

    public void deleteShift(int id, AuthService auth) {
        auth.requireRole("Coordinator");
        findShift(id);
        dbWriter.deleteVolunteerShift(id);
        shiftRepository.delete(id);
    }

    private VolunteerShift findShift(int id) {
        VolunteerShift shift = shiftRepository.findById(id);
        if (shift == null) {
            throw new IllegalArgumentException("Shift not found");
        }
        return shift;
    }

    public List<VolunteerShift> getVolunteerShifts(AuthService auth) {
        User volunteer = auth.requireRole("Volunteer");
        return shiftRepository.findByVolunteerId(volunteer.getUserId());
    }

    // Existing entry points reuse the database loader instead of repeating SQL.
    public void loadVolunteerShifts(VolunteerShiftRepository repo) {
        new DatabaseLoader().loadVolunteerShifts(repo);
    }

    public void loadNotifications(NotificationRepository repo) {
        new DatabaseLoader().loadNotifications(repo);
    }
}
