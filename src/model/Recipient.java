package model;

import common.RecipientApprovalStatus;

public class Recipient extends User {
    private int householdSize;
    private String dietaryRestrictions;
    private String emergencyContact;
    private RecipientApprovalStatus approvalStatus;

    public Recipient(int userId, String name, String email, String password, String phone , int householdSize, String dietaryRestrictions, String emergencyContact) {
        super(userId, name, email, password, phone, "Recipient");
        if(householdSize <= 0) {
            throw new IllegalArgumentException("Household size must be positive");
        }
        if(emergencyContact == null || emergencyContact.trim().isEmpty()) {
            throw new IllegalArgumentException("Emergency contact cannot be null or empty");
        }
        this.householdSize = householdSize;
        this.dietaryRestrictions = dietaryRestrictions;
        this.emergencyContact = emergencyContact;
        this.approvalStatus = RecipientApprovalStatus.PENDING;
    }
    public static void validateDetails(int householdSize, String emergencyContact) {
        if (householdSize <= 0 || emergencyContact == null || emergencyContact.isBlank())
            throw new IllegalArgumentException("Household size and emergency contact are required");
    }
    public void updateDetails(int householdSize, String dietaryRestrictions, String emergencyContact) {
        validateDetails(householdSize, emergencyContact);
        this.householdSize = householdSize;
        this.dietaryRestrictions = dietaryRestrictions;
        this.emergencyContact = emergencyContact.trim();
    }
    public int getHouseholdSize() {
        return householdSize;
    }
    public String getDietaryRestrictions() {
        return dietaryRestrictions; 
    }
    public String getEmergencyContact() {
        return emergencyContact;
    }
    public RecipientApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }
    public void updateApprovalStatus(RecipientApprovalStatus newStatus) {
        if(newStatus == null) {
            throw new IllegalArgumentException("Approval status cannot be null");
        }
        this.approvalStatus = newStatus;
    }
}
