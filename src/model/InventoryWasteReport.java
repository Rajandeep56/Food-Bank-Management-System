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
import common.NotificationType;

public class InventoryWasteReport extends Report {

    private final InventoryRepository inventoryRepository;
    private final NotificationRepository notificationRepository;
    private final CategoryRepository categoryRepository;
    
    private final InventoryService inventoryService;
    private record CategoryUnit(int categoryId, String unit) {}
    private static class WasteSummary {
        int itemsWasted;
        long quantityWasted;
        long quantityReceived;
        final Set<Integer> wastedItemIds = new HashSet<>();

    }

    private final Map<CategoryUnit, WasteSummary> summary = new HashMap<>();
    private int expiryAlerts;

    public InventoryWasteReport(InventoryRepository inventoryRepository,NotificationRepository notificationRepository,InventoryService inventoryService,  CategoryRepository categoryRepository) {
            if (inventoryRepository == null || notificationRepository == null || inventoryService == null || categoryRepository == null) {
                throw new IllegalArgumentException(
                    "Dependencies cannot be null"
                );
            }

            this.inventoryRepository = inventoryRepository;
            this.notificationRepository = notificationRepository;
            this.inventoryService = inventoryService;
            this.categoryRepository = categoryRepository;
        }

    @Override
    public void generate() {

        if (getStartDate() == null || getEndDate() == null) {
            throw new IllegalStateException(
                "Report period must be specified first"
            );
        }

        summary.clear();
        expiryAlerts = 0;

       for (InventoryHistory history : inventoryService.getHistory()) {

            LocalDate date = history.getOccurredAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            if (date.isBefore(getStartDate()) ||
                date.isAfter(getEndDate())) {
                continue;
            }
            CategoryUnit key = new CategoryUnit(history.getCategoryId(),history.getUnit());
            WasteSummary row = summary.computeIfAbsent(key, k -> new WasteSummary());
            switch (history.getChangeType()) {

                case DONATION_RECEIVED:
                    row.quantityReceived += history.getQuantityChange();
                    break;

                case WASTED:
                    if (row.wastedItemIds.add(history.getInventoryId())) {
                        row.itemsWasted++;
                    }

                    row.quantityWasted += Math.abs(
                        (long) history.getQuantityChange()
                    );
                    break;

                default:
                    break;
            }
        }
        countExpiryAlerts();
    }

    @Override
    public void display() {

        displayHeader("Inventory Waste Summary");

        if (summary.isEmpty()) {
            System.out.println("No inventory records found.");
        } else {

            List<Map.Entry<CategoryUnit, WasteSummary>> rows =
                new ArrayList<>(summary.entrySet());

            rows.sort((a, b) -> {
                int comparison = Long.compare(
                    b.getValue().quantityWasted,
                    a.getValue().quantityWasted
                );

                if (comparison != 0) {
                    return comparison;
                }

                return getCategoryName(a.getKey().categoryId())
                    .compareToIgnoreCase(
                        getCategoryName(b.getKey().categoryId())
                    );
            });

            System.out.printf(
                "%-20s %-10s %-15s %-15s %-15s %-10s%n",
                "Category", "Unit", "Items Wasted",
                "Qty Wasted", "Qty Received", "Waste Rate"
            );

            for (Map.Entry<CategoryUnit, WasteSummary> entry : rows) {

                CategoryUnit key = entry.getKey();
                WasteSummary row = entry.getValue();

                System.out.printf(
                    "%-20s %-10s %-15d %-15d %-15d %-10s%n",
                    getCategoryName(key.categoryId()),
                    key.unit(),
                    row.itemsWasted,
                    row.quantityWasted,
                    row.quantityReceived,
                    calculateWasteRate(row)
                );
            }
        }

        System.out.println("Expiry Risk Alerts: " + expiryAlerts);
    }
    private String calculateWasteRate(WasteSummary row) {

        if (row.quantityReceived == 0) {
            return "–";
        }

        double wasteRate = 
            (row.quantityWasted * 100.0) / row.quantityReceived;

        return String.format("%.1f%%", wasteRate);
    }

  
    private void countExpiryAlerts() {
        expiryAlerts = 0;

        for (Notification notification : notificationRepository.findAll()) {

            if (notification.getType() != NotificationType.INVENTORY_AT_RISK) {
                continue;
            }

            if (!notification.isSent()) {
                continue;
            }

            LocalDate date = notification.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            if (date.isBefore(getStartDate()) ||
                date.isAfter(getEndDate())) {
                continue;
            }

            expiryAlerts++;
        }
    }

    private String getCategoryName(int categoryId) {
        return categoryRepository.findById(categoryId).getName();
    }
}