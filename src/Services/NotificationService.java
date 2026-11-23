package Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import Repository.*;
import common.NotificationType;
import common.PickupStatus;
import database.DatabaseWriter;
import database.DatabaseConnection;
import model.*;

public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DistributionRepository distributionRepository;
    private final FoodHamperRequestRepository hamperRequestRepository;
    private final CategoryRepository categoryRepository;
    private final ExpiryRiskDetector expiryRiskDetector;
    private final DatabaseWriter dbWriter = new DatabaseWriter();
    private final EmailService emailService;
    private final String emailMode;
    private final String coordinatorEmail;

    public NotificationService(NotificationRepository notifications, UserRepository users,
            DistributionRepository distributions, FoodHamperRequestRepository requests,
            CategoryRepository categories, ExpiryRiskDetector expiry, DecliningStockDetector stock) {
        this(notifications, users, distributions, requests, categories, expiry, stock,
            new EmailService(), "LIVE", System.getenv("COORDINATOR_EMAIL"));
    }

    // Explicit delivery dependency makes provider failure testable without sending email.
    public NotificationService(NotificationRepository notifications, UserRepository users,
            DistributionRepository distributions, FoodHamperRequestRepository requests,
            CategoryRepository categories, ExpiryRiskDetector expiry, DecliningStockDetector stock,
            EmailService emailService, String mode, String coordinatorEmail) {
        if (notifications == null || users == null || distributions == null || requests == null
                || categories == null || expiry == null || stock == null || emailService == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.notificationRepository = notifications;
        this.userRepository = users;
        this.distributionRepository = distributions;
        this.hamperRequestRepository = requests;
        this.categoryRepository = categories;
        this.expiryRiskDetector = expiry;
        this.emailService = emailService;
        if (mode == null) {
            throw new IllegalArgumentException("Email mode is required");
        }
        this.emailMode = mode.toUpperCase(Locale.ROOT);
        if (!emailMode.equals("DEMO") && !emailMode.equals("SIMULATION") && !emailMode.equals("LIVE")) {
            throw new IllegalArgumentException("EMAIL_MODE must be DEMO, SIMULATION or LIVE");
        }
        this.coordinatorEmail = coordinatorEmail;
    }

    public void setShiftRepository(VolunteerShiftRepository shifts) {
        Objects.requireNonNull(shifts, "Shift repository cannot be null");
    }

    public synchronized void markPickupStatus(int distributionId, PickupStatus status) {
        if (status != PickupStatus.MISSED && status != PickupStatus.DELAYED) {
            throw new IllegalArgumentException("Use DistributionService to fulfill pickups");
        }
        Distribution distribution = distributionRepository.findById(distributionId);
        PickupStatus current = distribution.getPickupStatus();
        if (current != status) {
            boolean scheduled = current == PickupStatus.SCHEDULED;
            boolean delayedToMissed = current == PickupStatus.DELAYED && status == PickupStatus.MISSED;
            if (!scheduled && !delayedToMissed) {
                throw new IllegalStateException("Invalid pickup status transition");
            }
            dbWriter.updatePickupStatus(distributionId, status.name());
            distribution.markPickupStatus(status);
        }
        FoodHamperRequest request = hamperRequestRepository.findById(distribution.getRequestId());
        User recipient = userRepository.findById(request.getRecipientId());
        String message = "Pickup " + status + " for " + recipient.getName() + ", distribution #"
            + distributionId + " on " + distribution.getDistributionDate() + " at "
            + distribution.getPickupTime() + ". Please follow up.";
        NotificationType type = NotificationType.DELAYED_PICKUP;
        if (status == PickupStatus.MISSED) {
            type = NotificationType.MISSED_PICKUP;
        }
        notifyCoordinators(type, status + ":" + distributionId, message, LocalDate.now(), false);
    }

    public synchronized List<InventoryItem> checkInventoryAtRisk(LocalDate today) {
        List<InventoryItem> items = expiryRiskDetector.findAtRiskItems(today);
        for (InventoryItem item : items) {
            String category = categoryRepository.findById(item.getCategoryId()).getName();
            String message = item.getItemName() + " (" + category + ", expires " + item.getExpiryDate()
                + "): " + expiryRiskDetector.explain(item, today) + ". Prioritize the next distribution.";
            notifyCoordinators(NotificationType.INVENTORY_AT_RISK,
                "Expiry:" + item.getInventoryId() + ":" + item.getExpiryDate(), message, today, false);
        }
        return items;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    private void notifyCoordinators(NotificationType type, String reason, String message, LocalDate today, boolean weekly) {
        List<User> coordinators = userRepository.findByRole("Coordinator");
        if (coordinators.isEmpty()) {
            throw new IllegalStateException("No active coordinator configured for alerts");
        }
        // A shared address is sent once, linked to the first active coordinator.
        coordinators.sort(Comparator.comparingInt(User::getUserId));
        boolean sharedInbox = coordinatorEmail != null && !coordinatorEmail.isBlank();
        for (User coordinator : coordinators) {
            String to = coordinator.getEmail();
            if (sharedInbox) {
                to = coordinatorEmail.trim();
            }
            send(type, coordinator.getUserId(), to, reason, message, today, weekly);
            if (sharedInbox) {
                break;
            }
        }
    }

    private void send(NotificationType type, int userId, String to, String reason,
            String message, LocalDate today, boolean weekly) {
        EmailService.validateRecipient(to);
        String key = emailMode + ":" + reason;
        Notification notification = findExistingNotification(key, to, today, weekly);
        if (notification != null && notification.isSent()) {
            return;
        }
        if (notification == null) {
            notification = new Notification(notificationRepository.nextId(), type, userId, to, message);
            notification.setReason(key);
            dbWriter.addNotification(notification);
            notificationRepository.save(notification);
        }
        deliver(notification);
    }

    private Notification findExistingNotification(String key, String to, LocalDate today, boolean weekly) {
        Notification pending = null;
        for (Notification existing : notificationRepository.findAll()) {
            boolean sameAlert = key.equals(existing.getReason());
            boolean sameRecipient = to.equalsIgnoreCase(existing.getRecipientEmail());
            if (!sameAlert || !sameRecipient) {
                continue;
            }
            LocalDate sentDate = existing.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            boolean withinLastWeek = !sentDate.isAfter(today) && sentDate.isAfter(today.minusDays(7));
            if (weekly && !withinLastWeek) {
                continue;
            }
            if (existing.isSent()) {
                return existing;
            }
            pending = existing;
        }
        return pending;
    }

    private void deliver(Notification notification) {
        try {
            if (emailMode.equals("LIVE")) {
                String deliveryKey = DatabaseConnection.getSchemaName() + "/notification/" + notification.getNotificationId();
                emailService.sendEmail(notification.getRecipientEmail(), notification.getType().name(),
                    notification.getMessage(), deliveryKey);
                System.out.println("Email accepted | To: " + notification.getRecipientEmail() + " | Type: " + notification.getType());
            } else {
                System.out.println("[" + emailMode + "] To: " + notification.getRecipientEmail() + " | Type: " + notification.getType());
            }
            dbWriter.markNotificationSent(notification.getNotificationId());
            notification.markSent();
        } catch (Exception e) {
            System.err.println("Email pending retry | To: " + notification.getRecipientEmail() + " | Type: " + notification.getType());
        }
    }

    public synchronized void retryPending() {
        for (Notification notification : notificationRepository.findAll()) {
            if (!notification.isSent() && notification.getReason() != null
                    && notification.getReason().startsWith(emailMode + ":")) {
                deliver(notification);
            }
        }
    }

    public synchronized void checkStockDecline(int categoryId, String unit, LocalDate today, DecliningStockDetector detector) {
        if (detector.isDeclining(categoryId, unit, today)) {
            notifyCoordinators(NotificationType.INVENTORY_AT_RISK, "StockDecline:" + categoryId + ":" + unit,
                "Inventory At-Risk: declining stock for " + categoryRepository.findById(categoryId).getName()
                    + " (" + unit + "). " + detector.explain(categoryId, unit, today), today, true);
        }
    }

    public void checkMissedPickups(LocalDate today) {
        for (Distribution distribution : distributionRepository.findAll()) {
            if (distribution.getDistributionDate().isBefore(today)
                    && (distribution.getPickupStatus() == PickupStatus.SCHEDULED || distribution.getPickupStatus() == PickupStatus.DELAYED)) {
                markPickupStatus(distribution.getDistributionId(), PickupStatus.MISSED);
            }
        }
    }

    public void checkDelayedPickups(LocalDateTime now) {
        for (Distribution distribution : distributionRepository.findAll()) {
            if (distribution.getPickupStatus() == PickupStatus.SCHEDULED && distribution.getPickupTime() != null
                    && distribution.getDistributionDate().equals(now.toLocalDate())
                    && now.isAfter(LocalDateTime.of(distribution.getDistributionDate(), distribution.getPickupTime()).plusHours(2))) {
                markPickupStatus(distribution.getDistributionId(), PickupStatus.DELAYED);
            }
        }
    }
}
