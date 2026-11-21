package model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Repository.CategoryRepository;
import Repository.InventoryRepository;
import Repository.NotificationRepository;
import Services.InventoryService;
import common.InventoryChangeType;
import common.NotificationType;

public class InventoryWasteReport extends Report {
    private final NotificationRepository notificationRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final Map<CategoryUnit, WasteSummary> summary = new HashMap<>();
    private int expiryAlerts;

    // The category and unit together identify one report row.
    private record CategoryUnit(int categoryId, String unit) {}

    private static class WasteSummary {
        int itemsWasted;
        long quantityWasted;
        long quantityReceived;
        Set<Integer> wastedItemIds = new HashSet<>();
    }

    public InventoryWasteReport(InventoryRepository inventoryRepository,
            NotificationRepository notificationRepository, InventoryService inventoryService,
            CategoryRepository categoryRepository) {
        if (inventoryRepository == null || notificationRepository == null
                || inventoryService == null || categoryRepository == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.inventoryRepository = inventoryRepository;
        this.notificationRepository = notificationRepository;
        this.inventoryService = inventoryService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void generate() {
        if (getStartDate() == null || getEndDate() == null) {
            throw new IllegalStateException("Report period must be specified first");
        }
        summary.clear();
        countWasteHistory();
        countInventory();
        addEmptyCategories();
        countExpiryAlerts();
    }

    private boolean inReportPeriod(LocalDate date) {
        return date != null && !date.isBefore(getStartDate()) && !date.isAfter(getEndDate());
    }

    private WasteSummary findRow(int categoryId, String unit) {
        CategoryUnit key = new CategoryUnit(categoryId, unit);
        WasteSummary row = summary.get(key);
        if (row == null) {
            row = new WasteSummary();
            summary.put(key, row);
        }
        return row;
    }

    private void countWasteHistory() {
        for (InventoryHistory history : inventoryService.getHistory()) {
            LocalDate date = history.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!inReportPeriod(date)) {
                continue;
            }
            WasteSummary row = findRow(history.getCategoryId(), history.getUnit());
            if (history.getChangeType() == InventoryChangeType.WASTED) {
                if (row.wastedItemIds.add(history.getInventoryId())) {
                    row.itemsWasted++;
                }
                row.quantityWasted += Math.abs((long) history.getQuantityChange());
            }
        }
    }

    private void countInventory() {
        for (InventoryItem item : inventoryRepository.getAllInventoryItems()) {
            WasteSummary row = findRow(item.getCategoryId(), item.getUnit());
            LocalDate received = item.getReceivedDate().atZone(ZoneId.systemDefault()).toLocalDate();
            if (inReportPeriod(received)) {
                row.quantityReceived += item.getQuantityReceived();
            }

            // Older inventory may not have a matching waste history entry.
            LocalDate wasteDate = item.getWasteDate();
            if (wasteDate == null) {
                wasteDate = item.getExpiryDate();
            }
            boolean writtenOff = item.getStatus().equals("Expired") || item.getStatus().equals("Discarded");
            boolean alreadyCounted = row.wastedItemIds.contains(item.getInventoryId());
            if (writtenOff && !alreadyCounted && inReportPeriod(wasteDate) && item.getQuantity() > 0) {
                row.wastedItemIds.add(item.getInventoryId());
                row.itemsWasted++;
                row.quantityWasted += item.getQuantity();
            }
        }
    }

    private void addEmptyCategories() {
        for (Category category : categoryRepository.findAll()) {
            boolean found = false;
            for (CategoryUnit key : summary.keySet()) {
                if (key.categoryId() == category.getCategoryId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                findRow(category.getCategoryId(), "–");
            }
        }
    }

    public record Row(int categoryId, String unit, int itemsWasted,
            long quantityWasted, long quantityReceived) {
        public Double wasteRate() {
            if (quantityReceived == 0) {
                return null;
            }
            return quantityWasted * 100.0 / quantityReceived;
        }
    }

    public List<Row> getRows() {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<CategoryUnit, WasteSummary> entry : summary.entrySet()) {
            CategoryUnit key = entry.getKey();
            WasteSummary value = entry.getValue();
            rows.add(new Row(key.categoryId(), key.unit(), value.itemsWasted,
                value.quantityWasted, value.quantityReceived));
        }
        rows.sort((first, second) -> {
            int quantityOrder = Long.compare(second.quantityWasted(), first.quantityWasted());
            if (quantityOrder != 0) {
                return quantityOrder;
            }
            return getCategoryName(first.categoryId()).compareToIgnoreCase(getCategoryName(second.categoryId()));
        });
        return List.copyOf(rows);
    }

    @Override
    public void display() {
        displayHeader("Inventory Waste Summary");
        if (summary.isEmpty()) {
            System.out.println("No inventory records found.");
        } else {
            System.out.printf("%-20s %-10s %-15s %-15s %-15s %-10s%n",
                "Category", "Unit", "Items Wasted", "Qty Wasted", "Qty Received", "Waste Rate");
            for (Row row : getRows()) {
                String rate = "–";
                if (row.quantityReceived() > 0) {
                    rate = String.format("%.1f%%", row.wasteRate());
                }
                System.out.printf("%-20s %-10s %-15d %-15d %-15d %-10s%n",
                    getCategoryName(row.categoryId()), row.unit(), row.itemsWasted(),
                    row.quantityWasted(), row.quantityReceived(), rate);
            }
        }
        System.out.println("Expiry Risk Alerts: " + expiryAlerts);
    }

    private void countExpiryAlerts() {
        expiryAlerts = 0;
        for (Notification notification : notificationRepository.findAll()) {
            if (notification.getType() != NotificationType.INVENTORY_AT_RISK
                    || !notification.isSent() || !notification.isExpiryAlert()) {
                continue;
            }
            LocalDate date = notification.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (inReportPeriod(date)) {
                expiryAlerts++;
            }
        }
    }

    public int getExpiryAlerts() {
        return expiryAlerts;
    }

    private String getCategoryName(int categoryId) {
        return categoryRepository.findById(categoryId).getName();
    }
}
