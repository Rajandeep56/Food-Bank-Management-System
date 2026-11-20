package model;
import common.InventoryChangeType;
import java.time.*;

public class InventoryHistory {
    private final int inventoryId;
    private final int quantityChange;
    private final InventoryChangeType changeType;
    private final Instant occurredAt;
    private final int categoryId;
    private final String unit;

    public InventoryHistory(int inventoryId, int quantityChange, InventoryChangeType changeType, int categoryId, String unit) {
        if (inventoryId <= 0) {
            throw new IllegalArgumentException("Inventory ID must be positive");
        }

        if (changeType == null) {
            throw new IllegalArgumentException("Change type cannot be null");
        }

        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException(
                "Unit cannot be empty"
            );
        }

        this.inventoryId = inventoryId;
        this.quantityChange = quantityChange;
        this.changeType = changeType;
         this.categoryId = categoryId;
        this.unit = unit;
        this.occurredAt = Instant.now();
    }

        public int getInventoryId() {
            return inventoryId;
        }

        public int getQuantityChange() {
            return quantityChange;
        }
        public InventoryChangeType getChangeType() {
            return changeType;
        }
        public Instant getOccurredAt() {
            return occurredAt;
        }
       public int getCategoryId() {
            return categoryId;
        }

        public String getUnit() {
            return unit;
}

    }