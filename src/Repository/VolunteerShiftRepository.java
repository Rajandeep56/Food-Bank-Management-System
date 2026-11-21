package Repository;

import java.util.*;

import model.VolunteerShift;

public class VolunteerShiftRepository {
    private final Map<Integer, VolunteerShift> shifts = new HashMap<>();

    public void save(VolunteerShift shift){
        if(shift == null){
            throw new IllegalArgumentException("shift cant be null");
        }
        if (shifts.containsKey(shift.getShiftId())) {
            throw new IllegalStateException("Shift ID already exists");
        }
        shifts.put(shift.getShiftId(), shift);
    }
    public VolunteerShift findById(int shiftId){
        return shifts.get(shiftId);

    }
    public List<VolunteerShift> findByVolunteerId(int volunteerId){
        List<VolunteerShift> result = new ArrayList<>();
        for(VolunteerShift shift: shifts.values()){
            if(shift.getVolunteerId()==volunteerId){
                result.add(shift);
            }
        }
        return result;

    }
    public void update(VolunteerShift shift) {
        if (shift == null || !shifts.containsKey(shift.getShiftId())) throw new IllegalArgumentException("Shift not found");
        shifts.put(shift.getShiftId(), shift);
    }
    public void delete(int id) {
        if (shifts.remove(id) == null) throw new IllegalArgumentException("Shift not found");
    }
    public List<VolunteerShift> findAll() {
        return new ArrayList<>(shifts.values());
    }
}
