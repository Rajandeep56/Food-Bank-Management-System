package Services;

import Repository.InventoryRepository;
import model.InventoryItem;
import model.InventoryHistory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DecliningStockDetector {

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public DecliningStockDetector(
        InventoryRepository inventoryRepository,
        InventoryService inventoryService
    ) {
        this.inventoryRepository =
            Objects.requireNonNull(inventoryRepository);

        this.inventoryService =
            Objects.requireNonNull(inventoryService);
    }

    private double calculateSlope(double[] quantities) {

        double meanX = 3.5;
        double total = 0;
        for (double quantity : quantities) {
            total += quantity;
        }
        double meanY = total / quantities.length;

        double numerator = 0;
        double denominator = 0;

        for (int i = 0; i < 8; i++) {
            numerator += (i - meanX)
                       * (quantities[i] - meanY);

            denominator += Math.pow(i - meanX, 2);
        }

        return numerator / denominator;
    }

    private static class StockTrend {
        double currentStock;
        double slope;
        double projectedStock;
        double averageOutflow;
    }

    public String explain(int categoryId, String unit, LocalDate today) {
        StockTrend trend = calculateTrend(categoryId, unit, today);
        return String.format(Locale.ROOT,
            "Current stock %.1f; weekly slope %.2f; projected stock %.1f",
            trend.currentStock, trend.slope, trend.projectedStock);
    }

    public boolean isDeclining(int categoryId, String unit, LocalDate today) {
        StockTrend trend = calculateTrend(categoryId, unit, today);
        return trend.slope < 0 && trend.projectedStock < 2 * trend.averageOutflow;
    }

    private StockTrend calculateTrend(
        int categoryId,
        String unit,
        LocalDate today
    ) {

        double[] weeklyStock = new double[8];

        double totalDistributed = 0;
        double currentStock = 0;

        LocalDate start = today.minusDays(55);

        for (InventoryHistory history :
                inventoryService.getHistory()) {

            if (history.getCategoryId() != categoryId ||
                !unit.equals(history.getUnit())) {
                continue;
            }

            LocalDate date = history.getOccurredAt()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            if (date.isBefore(start) || date.isAfter(today)) {
                continue;
            }

            int week = (int) (
                ChronoUnit.DAYS.between(start, date) / 7
            );

            switch (history.getChangeType()) {

                case DONATION_RECEIVED:
                    weeklyStock[week] +=
                        history.getQuantityChange();
                    break;

                case DISTRIBUTED:
                    double distributed = Math.abs(
                        (long) history.getQuantityChange()
                    );

                    weeklyStock[week] -= distributed;
                    totalDistributed += distributed;
                    break;

                case WASTED:
                    weeklyStock[week] -= Math.abs(
                        (long) history.getQuantityChange()
                    );
                    break;
                
                case MANUAL_ADJUSTMENT:
                    weeklyStock[week] += history.getQuantityChange();
                    break;

                default:
                    break;
            }
        }

        for (InventoryItem item :
                inventoryRepository.getAllInventoryItems()) {

            if (item.getCategoryId() == categoryId &&
                unit.equals(item.getUnit()) && item.isAvailable() && !item.isExpired(today)) {

                currentStock += item.getQuantity();
            }
        }

        double slope = calculateSlope(weeklyStock);

        double averageOutflow = totalDistributed / 8.0;

        double projectedStock = currentStock + 2 * slope;

        StockTrend trend = new StockTrend();
        trend.currentStock = currentStock;
        trend.slope = slope;
        trend.projectedStock = projectedStock;
        trend.averageOutflow = averageOutflow;
        return trend;
    }
}