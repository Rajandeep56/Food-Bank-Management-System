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

    private final NotificationRepository notificationRepository;
    private final CategoryRepository categoryRepository;
    
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
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
        for (InventoryItem item : inventoryRepository.getAllInventoryItems()) {
            LocalDate received = item.getReceivedDate().atZone(ZoneId.systemDefault()).toLocalDate();
            CategoryUnit key = new CategoryUnit(item.getCategoryId(), item.getUnit());
            WasteSummary row = summary.computeIfAbsent(key, k -> new WasteSummary());
            if (!received.isBefore(getStartDate()) && !received.isAfter(getEndDate())) row.quantityReceived += item.getQuantityReceived();
            // Legacy rows may predate the inventory history migration.
            if (row.wastedItemIds.contains(item.getInventoryId())) continue;
            LocalDate waste = item.getWasteDate() == null ? item.getExpiryDate() : item.getWasteDate();
            if ((item.getStatus().equals("Expired") || item.getStatus().equals("Discarded")) && waste != null
                    && !waste.isBefore(getStartDate()) && !waste.isAfter(getEndDate()) && item.getQuantity() > 0) {
                row.wastedItemIds.add(item.getInventoryId()); row.itemsWasted++; row.quantityWasted += item.getQuantity();
            }
        }
        for (Category category : categoryRepository.findAll()) {
            if (summary.keySet().stream().noneMatch(key -> key.categoryId() == category.getCategoryId()))
                summary.put(new CategoryUnit(category.getCategoryId(), "–"), new WasteSummary());
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
    public record Row(int categoryId, String unit, int itemsWasted, long quantityWasted, long quantityReceived) {
        public Double wasteRate() { return quantityReceived == 0 ? null : quantityWasted * 100.0 / quantityReceived; }
    }
    public List<Row> getRows() {
        List<Row> rows = new ArrayList<>();
        for (var entry : summary.entrySet()) {
            var key = entry.getKey(); var value = entry.getValue();
            rows.add(new Row(key.categoryId(), key.unit(), value.itemsWasted, value.quantityWasted, value.quantityReceived));
        }
        rows.sort(java.util.Comparator.comparingLong(Row::quantityWasted).reversed()
            .thenComparing(row -> getCategoryName(row.categoryId())));
        return List.copyOf(rows);
    }
    public int getExpiryAlerts() { return expiryAlerts; }

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

            if (!notification.isSent() || !notification.isExpiryAlert()) {
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