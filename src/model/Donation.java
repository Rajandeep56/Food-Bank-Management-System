package model;

import java.time.Instant;
import common.DonationStatus;

public class Donation {
    private final int donationId;
    private final int donorId;
    private final int recordedBy;
    private int categoryId;
    private int quantity;
    private String unit;
    private final Instant donationDate;
    private DonationStatus status;

    public Donation(int donationId, int donorId, int recordedBy, String itemName, int categoryId, int quantity, String unit) {
        if(donationId <= 0 || donorId <= 0 || recordedBy <= 0 || itemName == null || itemName.strip().isEmpty() || categoryId <= 0 || quantity <= 0 || unit == null || unit.strip().isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters for Donation constructor");
        }

        this.donationId = donationId;
        this.donorId = donorId;
        this.recordedBy = recordedBy;
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.unit = unit;
        this.donationDate = Instant.now();
        this.status = DonationStatus.CREATED;
    }
    
    public int getDonationId() {
        return donationId;
    }

    public int getDonorId() {
        return donorId;
    }

    public int getRecordedBy() {
        return recordedBy;
    }
    public Instant getDonationDate() {
        return donationDate;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getUnit() {
        return unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setStatus(DonationStatus status) {
        DonationStatus currentStatus = getStatus();

        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
       
        if((currentStatus == DonationStatus.PENDING && status == DonationStatus.COMPLETED)
            ||(currentStatus == DonationStatus.CREATED && status == DonationStatus.PENDING)
            || (currentStatus == DonationStatus.PENDING && status == DonationStatus.FAILED)
            || (currentStatus == DonationStatus.FAILED && status == DonationStatus.PENDING)
            ) {
            this.status = status;
        }else {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + status);
        }
    }

    public void updateDonation(String itemName, int categoryId, int quantity, String unit) {
        if (status != DonationStatus.CREATED) {
            throw new IllegalStateException(
                "Only newly created donations can be modified"
            );
        }

        if (itemName == null || itemName.isBlank()
            || categoryId <= 0 || quantity <= 0
            || unit == null || unit.isBlank()) {

            throw new IllegalArgumentException("Invalid donation details");
        }

        itemName.trim();
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.unit = unit.trim();
    }
}