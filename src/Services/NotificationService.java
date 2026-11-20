package Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import Repository.CategoryRepository;
import Repository.DistributionRepository;
import Repository.FoodHamperRequestRepository;
import Repository.NotificationRepository;
import Repository.UserRepository;
import common.NotificationType;
import common.PickupStatus;
import database.DatabaseWriter;
import model.Distribution;
import model.FoodHamperRequest;
import model.InventoryItem;
import model.Notification;
import model.User;


public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DistributionRepository distributionRepository;
    private final FoodHamperRequestRepository hamperRequestRepository;
    private final CategoryRepository categoryRepository;
    private final ExpiryRiskDetector expiryRiskDetector;
    private DatabaseWriter dbWriter = new DatabaseWriter();

    // stops the same item being alerted twice
    private final Set<Integer> alertedInventoryIds = new HashSet<>();
    private final Map<String, LocalDate> lastStockAlert = new HashMap<>();

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                               DistributionRepository distributionRepository,
                               FoodHamperRequestRepository hamperRequestRepository,
                               CategoryRepository categoryRepository, ExpiryRiskDetector expiryRiskDetector, DecliningStockDetector decliningStockDetector) {
        if (notificationRepository == null || userRepository == null || distributionRepository == null
                || hamperRequestRepository == null || categoryRepository == null || expiryRiskDetector == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.distributionRepository = distributionRepository;
        this.hamperRequestRepository = hamperRequestRepository;
        this.categoryRepository = categoryRepository;
        this.expiryRiskDetector = expiryRiskDetector;
    }

    // missed or delayed pickup -> alert every coordinator
    public void markPickupStatus(int distributionId, PickupStatus newStatus) {
        Distribution distribution = distributionRepository.findById(distributionId);
        distribution.markPickupStatus(newStatus);

        if (newStatus == PickupStatus.MISSED || newStatus == PickupStatus.DELAYED) {
            FoodHamperRequest request = hamperRequestRepository.findById(distribution.getRequestId());
            User recipient = userRepository.findById(request.getRecipientId());
            NotificationType type = newStatus == PickupStatus.MISSED
                    ? NotificationType.MISSED_PICKUP : NotificationType.DELAYED_PICKUP;
            String message = "Pickup " + newStatus + " for recipient " + recipient.getName()
                    + " (phone " + recipient.getPhone() + "), distribution #" + distributionId
                    + " on " + distribution.getDistributionDate() + ". Please follow up.";
            for (User coordinator : userRepository.findByRole("Coordinator")) {
                send(type, coordinator, message);
            }
        }
    }

    // daily check: items the AI flags go to coordinators and volunteers
    public List<InventoryItem> checkInventoryAtRisk(LocalDate today) {
        List<InventoryItem> atRisk = expiryRiskDetector.findAtRiskItems(today);
        for (InventoryItem item : atRisk) {
            if (alertedInventoryIds.contains(item.getInventoryId())) {
                continue;
            }
            String category = categoryRepository.findById(item.getCategoryId()).getName();
            String message = item.getItemName() + " (" + category + ", expires " + item.getExpiryDate() + "): "
                    + expiryRiskDetector.explain(item, today)
                    + ". Please give it priority in the next distribution.";
            for (User coordinator : userRepository.findByRole("Coordinator")) {
                send(NotificationType.INVENTORY_AT_RISK, coordinator, message);
            }
            for (User volunteer : userRepository.findByRole("Volunteer")) {
                send(NotificationType.INVENTORY_AT_RISK, volunteer, message);
            }
            alertedInventoryIds.add(item.getInventoryId());
        }
        return atRisk;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    // email is printed to the console until the web app has real email
    private void send(NotificationType type, User to, String message) {
        Notification notification = new Notification(notificationRepository.nextId(), type,
                to.getUserId(), to.getEmail(), message);
        dbWriter.addNotification(
            notification.getNotificationId(),
            notification.getRecipientUserId(),
            notification.getRecipientEmail(),
            notification.getType().name(),
            notification.getMessage()
        );
        notificationRepository.save(notification);
        System.out.println("[EMAIL to " + to.getEmail() + "] " + type + ": " + message);
        dbWriter.markNotificationSent(
            notification.getNotificationId()
        );
        notification.markSent();
    }

    public void checkStockDecline( int categoryId,  String unit,  LocalDate today,  DecliningStockDetector detector   ) {
            String key = categoryId + " - " + unit;

            LocalDate lastAlert = lastStockAlert.get(key);

            if (lastAlert != null &&
                today.isBefore(lastAlert.plusDays(7))) {
                return;
            }

            if (detector.isDeclining(categoryId, unit, today)) {

                String message = "Inventory At-Risk: Stock declining for " + key;

                for (User coordinator : userRepository.findByRole("Coordinator")) {

                    send(NotificationType.INVENTORY_AT_RISK, coordinator, message );

                lastStockAlert.put(key, today);
            }
         }
    }

    public void checkMissedPickups(LocalDate today) {

        for (Distribution distribution : distributionRepository.findAll()) {

            if (distribution.getDistributionDate().isBefore(today)) {

                if (distribution.getPickupStatus() == PickupStatus.SCHEDULED || distribution.getPickupStatus() == PickupStatus.DELAYED) {

                    markPickupStatus(distribution.getDistributionId(),PickupStatus.MISSED);
                }
            }
        }
    }

    public void checkDelayedPickups(LocalDateTime now) {

        for (Distribution distribution :
                distributionRepository.findAll()) {

            if (distribution.getPickupStatus() != PickupStatus.SCHEDULED ||
                distribution.getPickupTime() == null ||
                !distribution.getDistributionDate().equals(now.toLocalDate())) {
                continue;
            }

            LocalDateTime deadline = LocalDateTime.of(
                distribution.getDistributionDate(),
                distribution.getPickupTime()
            ).plusHours(2);

            if (now.isAfter(deadline)) {

                markPickupStatus(
                    distribution.getDistributionId(),
                    PickupStatus.DELAYED
                );
            }
        }
    }
}
