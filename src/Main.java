import java.time.ZoneId;
import java.util.TimeZone;

import Repository.*;
import Services.*;
import database.DatabaseLoader;
import database.DatabaseWriter;

public class Main {
    public static void main(String[] args) {
        String timezone = System.getenv().getOrDefault("AI_TIMEZONE", "America/Vancouver");
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timezone)));
        DatabaseWriter.enabled = true;
        System.out.println("=== FOOD BANK SYSTEM ===");

        UserRepository users = new UserRepository();
        CategoryRepository categories = new CategoryRepository();
        DonationRepository donations = new DonationRepository();
        InventoryRepository inventory = new InventoryRepository();
        FoodHamperRequestRepository requests = new FoodHamperRequestRepository();
        DistributionRepository distributions = new DistributionRepository();
        VolunteerShiftRepository shifts = new VolunteerShiftRepository();
        NotificationRepository notifications = new NotificationRepository();
        InventoryService inventoryService = new InventoryService(inventory);

        DatabaseLoader loader = new DatabaseLoader();
        loader.loadUsers(users);
        loader.loadCategories(categories);
        loader.loadDonations(donations);
        loader.loadInventory(inventory);
        loader.loadHamperRequests(requests);
        loader.loadDistributions(distributions);
        loader.loadDistributionItems(distributions);
        loader.loadDistributionStatuses(distributions);
        loader.loadVolunteerShifts(shifts);
        loader.loadNotifications(notifications);
        loader.loadInventoryHistory(inventoryService);

        System.out.println("All database records loaded successfully");
        System.out.println("Users: " + users.findAll().size());
        System.out.println("Categories: " + categories.findAll().size());
        System.out.println("Donations: " + donations.findAll().size());
        System.out.println("Inventory: " + inventory.getAllInventoryItems().size());
        System.out.println("Notifications: " + notifications.findAll().size());

        ExpiryRiskDetector expiry = new ExpiryRiskDetector(inventory, inventoryService);
        DecliningStockDetector stockTrend = new DecliningStockDetector(inventory, inventoryService);
        NotificationService alerts = new NotificationService(notifications, users,
            distributions, requests, categories, expiry, stockTrend);

        AIEngine aiEngine = new AIEngine();
        Runtime.getRuntime().addShutdownHook(new Thread(aiEngine::stopScheduler));
        aiEngine.startScheduler(inventoryService, inventory, alerts, stockTrend);
    }
}
