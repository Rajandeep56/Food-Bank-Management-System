package web;

import Repository.*;
import Services.*;
import database.DatabaseLoader;
import model.*;

public class FoodBankApp {
    public final UserRepository users = new UserRepository();
    public final CategoryRepository categories = new CategoryRepository();
    public final DonationRepository donations = new DonationRepository();
    public final InventoryRepository inventory = new InventoryRepository();
    public final FoodHamperRequestRepository requests = new FoodHamperRequestRepository();
    public final DistributionRepository distributions = new DistributionRepository();
    public final VolunteerShiftRepository shifts = new VolunteerShiftRepository();
    public final NotificationRepository notifications = new NotificationRepository();

    public final InventoryService stock = new InventoryService(inventory);
    public final DonationService donationService = new DonationService(donations, stock, requests, distributions);
    public final FoodHamperRequestService hamperService = new FoodHamperRequestService(requests, distributions);
    public final CoordinatorService coordinatorService = new CoordinatorService(users, categories);
    public final VolunteerShiftService shiftService = new VolunteerShiftService(shifts);
    public final DistributionService distributionService = new DistributionService(distributions, requests, stock, inventory);
    public final ExpiryRiskDetector expiry = new ExpiryRiskDetector(inventory, stock);
    public final DecliningStockDetector trend = new DecliningStockDetector(inventory, stock);
    public final NotificationService alerts = new NotificationService(notifications, users, distributions,
        requests, categories, expiry, trend);

    public FoodBankApp() {
        shiftService.setUserRepository(users);
        distributionService.setShiftRepository(shifts);
        distributionService.setNotificationService(alerts);
    }

    public void loadDatabase() {
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
        loader.loadInventoryHistory(stock);
    }

    public int nextUserId() {
        int id = 0;
        for (User user : users.findAll()) id = Math.max(id, user.getUserId());
        return id + 1;
    }

    public int nextDonationId() {
        int id = 0;
        for (Donation donation : donations.findAll()) id = Math.max(id, donation.getDonationId());
        return id + 1;
    }

    public int nextInventoryId() {
        int id = 0;
        for (InventoryItem item : inventory.getAllInventoryItems()) id = Math.max(id, item.getInventoryId());
        return id + 1;
    }

    public int nextRequestId() {
        int id = 0;
        for (FoodHamperRequest request : requests.findAll()) id = Math.max(id, request.getRequestId());
        return id + 1;
    }

    public int nextCategoryId() {
        int id = 0;
        for (Category category : categories.findAll()) id = Math.max(id, category.getCategoryId());
        return id + 1;
    }

    public int nextShiftId() {
        int id = 0;
        for (VolunteerShift shift : shifts.findAll()) id = Math.max(id, shift.getShiftId());
        return id + 1;
    }

    public int nextDistributionItemId() {
        int id = 0;
        for (Distribution distribution : distributions.findAll()) {
            for (DistributionItem item : distribution.getItems()) id = Math.max(id, item.getDistributionItemId());
        }
        return id + 1;
    }
}
