package demo;

import java.time.LocalDate;

import Repository.*;
import Services.CoordinatorService;
import Services.ExpiryRiskDetector;
import Services.InventoryService;
import Services.NotificationService;
import common.InventoryChangeType;
import common.PickupStatus;
import model.*;

// run this to test the coordinator and alert features
public class CoordinatorNotificationDemo {
    public static void main(String[] args) {
        UserRepository users = new UserRepository();
        CategoryRepository categories = new CategoryRepository();
        InventoryRepository inventory = new InventoryRepository();
        FoodHamperRequestRepository requests = new FoodHamperRequestRepository();
        DistributionRepository distributions = new DistributionRepository();
        NotificationRepository notifications = new NotificationRepository();

        InventoryService inventoryService = new InventoryService(inventory);
        ExpiryRiskDetector detector = new ExpiryRiskDetector(inventory, inventoryService);
        CoordinatorService coordinatorService = new CoordinatorService(users, categories);
        NotificationService notificationService = new NotificationService(
                notifications, users, distributions, requests, categories, detector);

        Coordinator coordinator = new Coordinator(1, "Alex Coordinator", "coordinator@foodbank.local", "hash", "604-555-0100");
        users.save(coordinator);
        Recipient recipient = new Recipient(2, "Lee Family", "lee@foodbank.local", "hash", "604-555-0122", 4, "Peanut allergy", "Jin Lee 604-555-0123");
        users.save(recipient);

        System.out.println("1) Pending recipients: " + coordinatorService.getPendingRecipients().size());
        coordinatorService.approveRecipient(1, 2);
        System.out.println("   Lee Family is now " + recipient.getApprovalStatus());

        Volunteer volunteer = coordinatorService.createVolunteerAccount(1, 3, "Sam Patel", "sam@foodbank.local", "hash", "604-555-0155", "Weekdays");
        System.out.println("2) Volunteer created: " + volunteer.getName() + " (ID " + volunteer.getUserId() + ")");

        coordinatorService.addCategory(1, 1, "Dairy");
        coordinatorService.addCategory(1, 2, "Canned goods");
        coordinatorService.addCategory(1, 3, "Produce");
        System.out.println("3) Categories: " + coordinatorService.getCategories().size());

        try {
            coordinatorService.deleteUser(3, 2); // volunteer -> should be blocked
        } catch (IllegalStateException e) {
            System.out.println("4) Volunteer tried to delete a user: " + e.getMessage());
        }

        LocalDate today = LocalDate.now();
        inventoryService.addInventoryItem(new InventoryItem(101, "Yogurt", 1, 30, "cups", today.plusDays(20), 501));
        inventoryService.addInventoryItem(new InventoryItem(102, "Milk", 1, 40, "litres", today.plusDays(5), 502));
        inventoryService.addInventoryItem(new InventoryItem(103, "Cheese", 1, 6, "blocks", today.plusDays(6), 503));
        inventoryService.addInventoryItem(new InventoryItem(104, "Canned beans", 2, 48, "cans", today.plusYears(1), 504));
        inventoryService.addInventoryItem(new InventoryItem(105, "Bread", 3, 10, "loaves", today.plusDays(1), 505));

        // 28 dairy given out in 2 weeks = 2 per day
        inventoryService.adjustStock(101, -28, InventoryChangeType.DISTRIBUTED);

        System.out.println("5) AI expiry check (Dairy is given out at "
                + detector.dailyDistributionRate(1) + " units/day):");
        int found = notificationService.checkInventoryAtRisk(today).size();
        System.out.println("   At-risk items: " + found + " (Milk: too much to give out in time, Bread: expires tomorrow)");
        System.out.println("   Not at risk: Cheese (6 on hand, about 12 can go out in time), Canned beans (a year left)");

        FoodHamperRequest request = new FoodHamperRequest(201, 2);
        request.schedulePickup(today);
        requests.save(request);
        distributions.save(new Distribution(301, 201, today));
        System.out.println("6) Volunteer marks pickup MISSED:");
        notificationService.markPickupStatus(301, PickupStatus.MISSED);

        System.out.println("Total alerts sent: " + notificationService.getAllNotifications().size());
    }
}
