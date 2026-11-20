package Services;

import java.util.*;

import Repository.VolunteerShiftRepository;
import model.VolunteerShift;

public class VolunteerShiftService {

    private final VolunteerShiftRepository shiftRepository;

    public VolunteerShiftService(
            VolunteerShiftRepository shiftRepository) {

        if (shiftRepository == null) {
            throw new IllegalArgumentException(
                "Repository cannot be null"
            );
        }

        this.shiftRepository = shiftRepository;
    }

    public void assignShift(VolunteerShift newShift) {

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
        shiftRepository.save(newShift);
    }
    public List<VolunteerShift> getVolunteerShifts(int volunteerId){
        if(volunteerId<=0){
            throw new IllegalArgumentException("volunteer id should be > 0");

        }
        List<VolunteerShift> existingShifts = shiftRepository.findByVolunteerId(volunteerId);
        return existingShifts;

    }
}