package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import common.PickupStatus;

public class Distribution {
    private final int distributionId;
    private final int requestId;
    private final LocalDate distributionDate;
    private final List<DistributionItem> items = new ArrayList<>();
    private PickupStatus pickupStatus;
    private LocalTime pickupTime;
    
    public Distribution(int distributionId, int requestId, LocalDate distributionDate) {
        if (distributionId <= 0 || requestId <= 0) {
            throw new IllegalArgumentException("Invalid input parameters for Distribution constructor");
        }
        if (distributionDate == null) {
            throw new IllegalArgumentException("Distribution date cannot be null");
        }
        this.distributionId = distributionId;
        this.requestId = requestId;
        this.distributionDate = distributionDate;
        this.pickupStatus = PickupStatus.SCHEDULED;
    }

    public int getDistributionId() {
        return distributionId;
    }
    public int getRequestId() {
        return requestId;
    }
    public LocalDate getDistributionDate() {
        return distributionDate;
    }
    public List<DistributionItem> getItems() {
        return new ArrayList<>(items);
    }
    public PickupStatus getPickupStatus() {
        return pickupStatus;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalTime pickupTime) {
        if (pickupTime == null) {
            throw new IllegalArgumentException(
                "Pickup time cannot be null"
            );
        }

        this.pickupTime = pickupTime;
    }
    public void addItem(DistributionItem item) {
         if (item == null) {
            throw new IllegalArgumentException(
                "Distribution item cannot be null"
            );
        }
        if (pickupStatus == PickupStatus.FULFILLED) {
            throw new IllegalStateException(
                "Cannot modify a fulfilled distribution"
            );
        }
        for (DistributionItem existingItem : items) {
            if (existingItem.getDistributionItemId() == item.getDistributionItemId()) {
                throw new IllegalArgumentException("Distribution item with this ID already exists");
            }
        }
        items.add(item);
    }

    public void markPickupStatus(PickupStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Pickup status cannot be null");
        }
        if(pickupStatus == PickupStatus.FULFILLED) {
            throw new IllegalStateException("Pickup can only be marked as fulfilled if it was scheduled");
        }
        if(newStatus == PickupStatus.FULFILLED) {
            if(pickupStatus != PickupStatus.SCHEDULED && pickupStatus != PickupStatus.DELAYED) {
                throw new IllegalStateException("Pickup can only be marked as fulfilled if it was scheduled");
            }
        }
        if (newStatus == PickupStatus.FULFILLED && items.isEmpty()) {
            throw new IllegalStateException(
                "Cannot fulfill an empty distribution"
            );
        }
        this.pickupStatus = newStatus;
    }
    public void removeItem(int inventoryId) {

        if (pickupStatus != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled distributions can be modified"
            );
        }

        boolean removed = items.removeIf(
            item -> item.getInventoryId() == inventoryId
        );

        if (!removed) {
            throw new IllegalArgumentException(
                "Distribution item not found"
            );
        }
    }
}
