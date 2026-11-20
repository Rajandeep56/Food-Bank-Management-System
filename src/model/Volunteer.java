package model;

public class Volunteer extends User {
    private String availability;
    public Volunteer(int userId, String name, String email, String passwordHash, String phone, String availability) {
        super(userId, name, email, passwordHash, phone, "Volunteer");
        this.availability = availability;
    }
    public String getAvailability() {
        return availability;
    }
    public void updateAvailability(String availability) {
        if(availability == null || availability.trim().isEmpty()) {
            throw new IllegalArgumentException("Availability cannot be null or empty");
        }
        this.availability = availability;
    }
    public void setAvailability(String availability) {

        if (availability == null || availability.isBlank()) {
            throw new IllegalArgumentException(
                "Availability cannot be empty"
            );
        }

        this.availability = availability.trim();
    }
}
