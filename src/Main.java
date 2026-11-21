import database.DatabaseLoader;
import database.DatabaseWriter;
import Repository.*;
import Services.*;

public class Main {

    public static void main(String[] args) {
        DatabaseWriter.enabled = false;

        System.out.println("=== FOOD BANK SYSTEM ===");

        UserRepository userRepository = new UserRepository();

        CategoryRepository categoryRepository =
            new CategoryRepository();

        DonationRepository donationRepository =
            new DonationRepository();

        InventoryRepository inventoryRepository =
            new InventoryRepository();

        FoodHamperRequestRepository hamperRepository =
            new FoodHamperRequestRepository();

        DistributionRepository distributionRepository =
            new DistributionRepository();

        VolunteerShiftRepository shiftRepository =
            new VolunteerShiftRepository();

        NotificationRepository notificationRepository =
            new NotificationRepository();

        InventoryService inventoryService =
            new InventoryService(inventoryRepository);

        DatabaseLoader loader = new DatabaseLoader();

        loader.loadUsers(userRepository);
        loader.loadCategories(categoryRepository);
        loader.loadDonations(donationRepository);
        loader.loadInventory(inventoryRepository);
        loader.loadHamperRequests(hamperRepository);
        loader.loadDistributions(distributionRepository);
        loader.loadDistributionItems(distributionRepository);
        loader.loadDistributionStatuses(distributionRepository);
        loader.loadVolunteerShifts(shiftRepository);
        loader.loadNotifications(notificationRepository);
        loader.loadInventoryHistory(inventoryService);

        System.out.println("All database records loaded successfully");

        System.out.println("Users: " + userRepository.findAll().size());

        System.out.println("Categories: " + categoryRepository.findAll().size());

        System.out.println("Donations: " + donationRepository.findAll().size());

        System.out.println("Inventory: " +
            inventoryRepository.getAllInventoryItems().size());

        System.out.println("Notifications: " +
            notificationRepository.findAll().size());
        
        AuthService authService =
            new AuthService(userRepository);

        CoordinatorService coordinatorService =
            new CoordinatorService(userRepository, categoryRepository);


        FoodHamperRequestService hamperService =
            new FoodHamperRequestService(
                hamperRepository,
                distributionRepository
            );

        DistributionService distributionService =
            new DistributionService(
                distributionRepository,
                hamperRepository,
                inventoryService,
                inventoryRepository
            );

        VolunteerShiftService shiftService =
            new VolunteerShiftService(shiftRepository);

        DecliningStockDetector decliningStockDetector =
            new DecliningStockDetector(
                inventoryRepository, inventoryService
            );

        ExpiryRiskDetector expiryRiskDetector =
            new ExpiryRiskDetector(
                inventoryRepository, inventoryService
            );
        
        NotificationService notificationService = new NotificationService(notificationRepository, userRepository, distributionRepository, hamperRepository, categoryRepository, expiryRiskDetector, decliningStockDetector);

        notificationService.setShiftRepository(shiftRepository);
        shiftService.setUserRepository(userRepository);
        distributionService.setShiftRepository(shiftRepository);
        
        AIEngine aiEngine = new AIEngine();

        aiEngine.startScheduler(
            inventoryService,
            inventoryRepository,
            notificationService,
            decliningStockDetector
        );
    }
}