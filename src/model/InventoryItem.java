package model;

import java.time.*;

// Records whats currently available in the inventory
// behaviour : increase avaialble qty, decrease avialable qty, check if item is available, check expiry

public class InventoryItem {
    private final int inventoryId;
    private final String itemName;
    private int categoryId;
    private int quantity;
    private final String unit;
    private LocalDate expiryDate;
    private final Instant receivedDate;
    private  final int donationId;

    public InventoryItem(int inventoryId, String itemName, int categoryId, int quantity, String unit, LocalDate expiryDate, int donationId) {
        if (inventoryId <= 0 || itemName == null || itemName.strip().isEmpty() || categoryId <= 0 || quantity <= 0 || unit == null || unit.strip().isEmpty() || donationId <= 0) {
            throw new IllegalArgumentException("Invalid inventory item parameters");
        }
        this.inventoryId = inventoryId;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate =  expiryDate;
        this.receivedDate = Instant.now();
        this.donationId = donationId;
    }

    public int getInventoryId() {
        return inventoryId;
    }
    public String getItemName() {
        return itemName;
    }
    public int getCategoryId() {
        return categoryId;
    }
    public int getQuantity() {
        return quantity;
    }
    public String getUnit() {
        return unit;    
    }
    public LocalDate getExpiryDate() {
        return expiryDate;
    }
    public Instant getReceivedDate() {
        return receivedDate;
    }
    public int getDonationId() {
        return donationId;
    }
    public void adjustQuantity(int adjustment) {
        if (quantity + adjustment < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative, current available quantity: " + getQuantity() + ", attempted adjustment: " + adjustment);
        } 
        this.quantity += adjustment;
    }
   public  boolean isAtRisk(LocalDate referenceDate) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        return expiryDate != null && !expiryDate.isAfter(referenceDate.plusDays(7));
    }
   
    public boolean isExpired(LocalDate currentDate) {
        if (currentDate == null) {
            throw new IllegalArgumentException("Current date cannot be null");
        }
        return expiryDate != null && expiryDate.isBefore(currentDate);
    }

   }