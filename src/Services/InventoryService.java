package Services;
import model.InventoryItem;
import common.InventoryChangeType;
import database.DatabaseWriter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import Repository.InventoryRepository;
import model.InventoryHistory;

public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final List<InventoryHistory> history = new ArrayList<>();
    private DatabaseWriter dbWriter = new DatabaseWriter();
    
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
        validateNewInventory(inventoryItem);
        InventoryHistory record = new InventoryHistory(inventoryItem.getInventoryId(), inventoryItem.getQuantity(), InventoryChangeType.DONATION_RECEIVED, inventoryItem.getCategoryId(), inventoryItem.getUnit());
        dbWriter.addInventory(inventoryItem, record);
        inventoryRepository.save(inventoryItem);
        history.add(record);
    }

    public void addInventoryItem(InventoryItem item, AuthService auth) {
        auth.requireRole("Volunteer");
        addInventoryItem(item);
    }

    void validateNewInventory(InventoryItem item) {
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("New stock must be positive");
        }
        for (InventoryItem existing : inventoryRepository.getAllInventoryItems()) {
            if (existing.getInventoryId() == item.getInventoryId()) {
                throw new IllegalArgumentException("Inventory ID already exists");
            }
        }
    }

    // Called only after the donation transaction has persisted intake and history.
    void recordInventoryItem(InventoryItem item) {
        inventoryRepository.save(item);
        history.add(new InventoryHistory(item.getInventoryId(), item.getQuantity(), InventoryChangeType.DONATION_RECEIVED, item.getCategoryId(), item.getUnit()));
    }

    public void adjustStock(int inventoryId, int adjustment, InventoryChangeType changeType) {
        if(adjustment == 0) {
            throw new IllegalArgumentException("Adjustment cannot be zero");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("Change type cannot be null");
        }
        InventoryItem item = inventoryRepository.getInventoryItemById(inventoryId);
        if (!item.isAvailable()) {
            throw new IllegalStateException("Inventory is no longer available");
        }
        int newQuantity = Math.addExact(item.getQuantity(), adjustment);

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Insufficient inventory");
        }

        InventoryHistory record = new InventoryHistory(
            inventoryId,
            adjustment,
            changeType,
            item.getCategoryId(),
            item.getUnit()
        );

        if (changeType != InventoryChangeType.DISTRIBUTED) {
            dbWriter.changeInventory(inventoryId, newQuantity, record);
        }
        item.adjustQuantity(adjustment);
        history.add(record);
        if (newQuantity == 0 && changeType == InventoryChangeType.WASTED) {
            LocalDate wasteDate = record.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDate();
            item.markWaste(wasteDate);
        }
    }

    public List<InventoryHistory> getHistory() {
        return new ArrayList<>(history);
    }

    public void processExpiredInventory(LocalDate today) {

        for (InventoryItem item : inventoryRepository.getAllInventoryItems()) {

            if (item.isAvailable() && item.isExpired(today) && item.getQuantity() > 0) {

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
    public void deleteInventory(int id, AuthService auth) {
        auth.requireRole("Volunteer");
        InventoryItem item = inventoryRepository.getInventoryItemById(id);
        if (!item.isAvailable()) {
            return;
        }
        if (item.getQuantity() > 0) {
            discardInventory(id, auth);
        } else {
            dbWriter.discardInventory(id);
            item.markWaste(LocalDate.now());
        }
    }

    public InventoryItem getInventoryItem(int id) {
        return inventoryRepository.getInventoryItemById(id);
    }

    public void updateInventoryDetails(int id, String name, LocalDate expiry, AuthService auth) {
        auth.requireRole("Volunteer");
        InventoryItem item = inventoryRepository.getInventoryItemById(id);
        if (!item.isAvailable()) {
            throw new IllegalStateException("Written-off stock cannot be modified");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name required");
        }
        dbWriter.updateInventoryDetails(id, name.trim(), expiry);
        item.updateDetails(name, expiry);
    }

    public void loadHistory(InventoryHistory record) {
        history.add(record);
    }
    
}
