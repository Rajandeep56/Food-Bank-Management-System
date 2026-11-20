package Services;

import Repository.InventoryRepository;
import model.InventoryItem;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.*;

public class AIEngine {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void runDailyChecks(InventoryService inventoryService, InventoryRepository inventoryRepository, NotificationService notificationService, DecliningStockDetector detector ) {

        LocalDate today = LocalDate.now();

        inventoryService.processExpiredInventory(today);

        notificationService.checkInventoryAtRisk(today);

        notificationService.checkMissedPickups(today);

        Set<String> checked = new HashSet<>();

        for (InventoryItem item :
                inventoryRepository.getAllInventoryItems()) {

            String key = item.getCategoryId()
                + " - " + item.getUnit();

            if (checked.add(key)) {

                notificationService.checkStockDecline(
                    item.getCategoryId(),
                    item.getUnit(),
                    today,
                    detector
                );
            }
        }
        notificationService.checkDelayedPickups(LocalDateTime.now());
        System.out.println("Daily AI checks completed.");
    }

    public void startScheduler(InventoryService inventoryService, InventoryRepository inventoryRepository, NotificationService notificationService, DecliningStockDetector detector) {

        ZonedDateTime now = ZonedDateTime.now();

        ZonedDateTime nextRun = now.toLocalDate()
            .atTime(6, 0)
            .atZone(now.getZone());

        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1);
        }

        long delay = Duration.between(now, nextRun).toMillis();

        scheduler.schedule(() -> {
            try {
                runDailyChecks(
                    inventoryService,
                    inventoryRepository,
                    notificationService,
                    detector
                );
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                if (!scheduler.isShutdown()) {
                    startScheduler(
                        inventoryService,
                        inventoryRepository,
                        notificationService,
                        detector
                    );
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void stopScheduler() {
        scheduler.shutdownNow();
    }
}