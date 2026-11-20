package model;

public class DistributionItem {
    private final int distributionItemId;
    private final int inventoryId;
    private final int quantity;
    private final String unit;

    public DistributionItem(int distributionItemId, int inventoryId, int quantity, String unit) {
        if (distributionItemId <= 0) {
            throw new IllegalArgumentException("Distribution Item ID must be positive");
        }
        if (inventoryId <= 0) {
            throw new IllegalArgumentException("Inventory ID must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit cannot be null or empty");
        }

        this.distributionItemId = distributionItemId;
        this.inventoryId = inventoryId;
        this.quantity = quantity;
        this.unit = unit;
    }
    public int getDistributionItemId() {
        return distributionItemId;
    }
    public int getInventoryId() {
        return inventoryId;
    }
    public int getQuantity() {
        return quantity;
    }
    public String getUnit() {
        return unit;
    }
    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "Quantity must be positive"
            );
        }

        this.quantity = quantity;
    }
}
