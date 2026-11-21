package Services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import Repository.InventoryRepository;
import common.InventoryChangeType;
import model.InventoryHistory;
import model.InventoryItem;

// AI pattern detection: finds inventory at risk of expiring before it can be given out.
//
// How it decides:
// 1. Learn the pattern: from the inventory history, work out how many units of each
//    category were distributed per day over the last 14 days.
// 2. Predict: for each item, (units given out per day) x (days left before expiry)
//    = how much of it we can expect to give out in time.
// 3. Flag the item if it has more on hand than we can give out before it expires,
//    or if it expires within 2 days.
// 4. If a category has no distribution history yet, fall back to the simple rule
//    in InventoryItem.isAtRisk (expires within 7 days!!!!!!).
public class ExpiryRiskDetector {
    private static final int HISTORY_DAYS = 14;
    private static final int URGENT_DAYS = 2;

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public ExpiryRiskDetector(InventoryRepository inventoryRepository, InventoryService inventoryService) {
        if (inventoryRepository == null || inventoryService == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    // step 1: average units distributed per day for one category
    public double dailyDistributionRate(int categoryId) {
        return dailyDistributionRate(categoryId, null, LocalDate.now());
    }

    public double dailyDistributionRate(int categoryId, String unit, LocalDate today) {
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        Instant until = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant since = today.minusDays(HISTORY_DAYS - 1).atStartOfDay(zone).toInstant();
        long distributed = 0;
        for (InventoryHistory change : inventoryService.getHistory()) {
            if (change.getChangeType() != InventoryChangeType.DISTRIBUTED
                    || change.getOccurredAt().isBefore(since) || !change.getOccurredAt().isBefore(until)) continue;
            if (change.getCategoryId() == categoryId && (unit == null || unit.equals(change.getUnit())))
                distributed += Math.abs((long) change.getQuantityChange());
        }
        return (double) distributed / HISTORY_DAYS;
    }

    // steps 2-4: is this item at risk?
    public boolean isAtRisk(InventoryItem item, LocalDate today) {
        if (!item.isAvailable() || item.getExpiryDate() == null || item.getQuantity() <= 0 || item.isExpired(today)) {
            return false;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, item.getExpiryDate());
        if (daysLeft <= URGENT_DAYS) {
            return true;
        }
        double perDay = dailyDistributionRate(item.getCategoryId(), item.getUnit(), today);
        if (perDay == 0) {
            return item.isAtRisk(today); // no history yet: expires within 7 days
        }
        double canGiveOut = perDay * daysLeft;
        return item.getQuantity() > canGiveOut;
    }

    // all at-risk items, soonest expiry first
    public List<InventoryItem> findAtRiskItems(LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        List<InventoryItem> atRisk = new ArrayList<>();
        for (InventoryItem item : inventoryRepository.getAllInventoryItems()) {
            if (isAtRisk(item, today)) {
                atRisk.add(item);
            }
        }
        atRisk.sort(Comparator.comparing(InventoryItem::getExpiryDate));
        return atRisk;
    }

    // plain-English reason, used in the alert email
    public String explain(InventoryItem item, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, item.getExpiryDate());
        double perDay = dailyDistributionRate(item.getCategoryId(), item.getUnit(), today);
        if (daysLeft <= URGENT_DAYS) {
            return "expires in " + daysLeft + " day(s)";
        }
        if (perDay == 0) {
            return "expires in " + daysLeft + " days and this category has no recent distributions";
        }
        long canGiveOut = Math.round(perDay * daysLeft);
        return "only about " + canGiveOut + " " + item.getUnit() + " can be given out in the "
                + daysLeft + " days before it expires, but " + item.getQuantity() + " are on hand";
    }
}