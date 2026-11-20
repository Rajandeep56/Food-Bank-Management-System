package model;

import java.time.LocalDate;

import common.HamperRequestStatus;

public class FoodHamperRequest {
    private final int requestId;
    private final int recipientId;
    private final LocalDate requestDate;
    private LocalDate pickupDate;
    private HamperRequestStatus status;

    public FoodHamperRequest(int requestId, int recipientId) {
        if (requestId <= 0 || recipientId <= 0) {
            throw new IllegalArgumentException("Invalid input parameters for FoodHamperRequest constructor");
        }

        this.requestId = requestId;
        this.recipientId = recipientId;
        this.requestDate = LocalDate.now();
        this.status = HamperRequestStatus.PENDING;
    }
    public int getRequestId() {
        return requestId;
    }
    public int getRecipientId() {
        return recipientId;
    }
    public LocalDate getPickupDate() {
        return pickupDate;
    }
    public HamperRequestStatus getStatus() {
        return status;
    }
    
    public void schedulePickup(LocalDate pickupDate) {
        if (pickupDate == null || pickupDate.isBefore(requestDate)) {
            throw new IllegalArgumentException("Pickup date cannot be null or before the request date");
        }
        if (status != HamperRequestStatus.PENDING) {
            throw new IllegalStateException("Pickup can only be scheduled for pending requests");
        }
         this.pickupDate = pickupDate;
        this.status = HamperRequestStatus.SCHEDULED;
    }
    public void cancel() {
        if (status == HamperRequestStatus.FULFILLED) {
            throw new IllegalStateException("Cannot cancel a fulfilled request");
        }
        
        this.status = HamperRequestStatus.CANCELLED;
    }

    public void markFulfilled(){
        if (status != HamperRequestStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled hamper requests can be fulfilled");
        }
        status = HamperRequestStatus.FULFILLED;
    }
}
