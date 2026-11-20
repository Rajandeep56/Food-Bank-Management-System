package Services;
import model.InventoryItem;
import common.InventoryChangeType;

import java.time.LocalDate;
import java.util.*;

import Repository.InventoryRepository;
import model.InventoryHistory;

public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final List<InventoryHistory> history = new ArrayList<>();
    
    public InventoryService(InventoryRepository inventoryRepository) {
        if(inventoryRepository == null) {
            throw new IllegalArgumentException("InventoryRepository cannot be null");
        }
        this.inventoryRepository = inventoryRepository;
    }

    public void addInventoryItem(InventoryItem inventoryItem) {
        if(inventoryItem == null) {
            throw new IllegalArgumentException("Inventory item cannot be null");
        }
        inventoryRepository.save(inventoryItem);
        history.add(new InventoryHistory(inventoryItem.getInventoryId(), inventoryItem.getQuantity(), InventoryChangeType.DONATION_RECEIVED, inventoryItem.getCategoryId(), inventoryItem.getUnit()));
    }

    public void adjustStock(int inventoryId, int adjustment, InventoryChangeType changeType) {
        if(adjustment == 0) {
            throw new IllegalArgumentException("Adjustment cannot be zero");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("Change type cannot be null");
        }
        InventoryItem item = inventoryRepository.getInventoryItemById(inventoryId);
        item.adjustQuantity(adjustment);
        history.add(new InventoryHistory(inventoryId, adjustment, changeType, item.getCategoryId(), item.getUnit()));
    }

     public List<InventoryHistory> getHistory() {
            return new ArrayList<>(history);
        }
    public void processExpiredInventory(LocalDate today) {

        for (InventoryItem item : inventoryRepository.getAllInventoryItems()) {

            if (item.isExpired(today) && item.getQuantity() > 0) {

                adjustStock(
                    item.getInventoryId(),
                    -item.getQuantity(),
                    InventoryChangeType.WASTED
                );
            }
        }
    }

    public List<InventoryItem> searchInventory(String itemName, Integer categoryId,  AuthService authService) {
        authService.requireRole("Volunteer");
        List<InventoryItem> result = new ArrayList<>();

        for (InventoryItem item :
                inventoryRepository.getAllInventoryItems()) {

            if (itemName != null &&
                !item.getItemName().toLowerCase()
                    .contains(itemName.trim().toLowerCase())) {
                continue;
            }

            if (categoryId != null &&
                item.getCategoryId() != categoryId) {
                continue;
            }

            result.add(item);
        }

        return result;
    }

    public void discardInventory(int inventoryId, AuthService authService) {
        authService.requireRole("Volunteer");
        InventoryItem item = inventoryRepository.getInventoryItemById(inventoryId);

        discardQuantity(inventoryId, item.getQuantity(), authService);
    }

    public void discardQuantity(int inventoryId, int quantity, AuthService authService) {
        authService.requireRole("Volunteer");
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "Quantity must be positive"
            );
        }

        adjustStock(
            inventoryId,
            -quantity,
            InventoryChangeType.WASTED
        );
    }

    public void updateInventoryQuantity(int inventoryId, int newQuantity, AuthService authService) {
        authService.requireRole("Volunteer");
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                "Quantity cannot be negative"
            );
        }

        InventoryItem item =
            inventoryRepository.getInventoryItemById(inventoryId);

        int difference = newQuantity - item.getQuantity();

        if (difference == 0) {
            return;
        }

        adjustStock(
            inventoryId,
            difference,
            InventoryChangeType.MANUAL_ADJUSTMENT
        );
    }
    
}
