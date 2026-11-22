package Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import Repository.InventoryRepository;
import model.InventoryItem;

public class AIEngine {
    private final ZoneId timezone = ZoneId.of(
        System.getenv().getOrDefault("AI_TIMEZONE", "America/Vancouver"));
    private Timer timer;
    private LocalDate lastRunDate;

    public void runDailyChecks(InventoryService inventoryService, InventoryRepository inventory,
            NotificationService notifications, DecliningStockDetector detector) {
        synchronized (inventory) {
            LocalDateTime now = LocalDateTime.now(timezone);
            LocalDate today = now.toLocalDate();

            notifications.retryPending();
            inventoryService.processExpiredInventory(today);
            notifications.checkInventoryAtRisk(today);
            notifications.checkMissedPickups(today);
            notifications.checkDelayedPickups(now);

            Set<String> checkedCategories = new HashSet<>();
            for (InventoryItem item : inventory.getAllInventoryItems()) {
                String categoryAndUnit = item.getCategoryId() + " - " + item.getUnit();
                if (checkedCategories.add(categoryAndUnit)) {
                    notifications.checkStockDecline(item.getCategoryId(), item.getUnit(), today, detector);
                }
            }
            System.out.println("Daily AI checks completed.");
        }
    }

    public synchronized void startScheduler(InventoryService inventoryService, InventoryRepository inventory,
            NotificationService notifications, DecliningStockDetector detector) {
        if (timer != null) {
            return;
        }

        // If today's 6 AM has passed, start with tomorrow's checks.
        LocalDateTime now = LocalDateTime.now(timezone);
        if (now.getHour() >= 6) {
            lastRunDate = now.toLocalDate();
        }

        timer = new Timer("Food bank daily checks");
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    checkSchedule(LocalDateTime.now(timezone), inventoryService, inventory, notifications, detector);
                } catch (RuntimeException e) {
                    System.err.println("Daily AI checks failed; check configuration and pending notifications");
                }
            }
        }, 0, 60_000); // Check the time once a minute.
    }

    public synchronized void checkSchedule(LocalDateTime now, InventoryService inventoryService,
            InventoryRepository inventory, NotificationService notifications, DecliningStockDetector detector) {
        LocalDate today = now.toLocalDate();
        if (now.getHour() < 6 || today.equals(lastRunDate)) {
            return;
        }
        lastRunDate = today;
        runDailyChecks(inventoryService, inventory, notifications, detector);
    }

    public synchronized void stopScheduler() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
