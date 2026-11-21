package Services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Repository.DistributionRepository;
import Repository.FoodHamperRequestRepository;
import Repository.InventoryRepository;
import Repository.UserRepository;
import Repository.VolunteerShiftRepository;
import common.HamperRequestStatus;
import common.InventoryChangeType;
import common.PickupStatus;
import database.DatabaseWriter;
import model.Distribution;
import model.DistributionItem;
import model.FoodHamperRequest;
import model.InventoryItem;
import model.User;
import model.VolunteerShift;

public class DistributionService {
    private final DistributionRepository distributionRepository;
    private final FoodHamperRequestRepository hamperRequestRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private VolunteerShiftRepository shiftRepository;
    private NotificationService notificationService;

    public void setNotificationService(NotificationService service) {
        notificationService = java.util.Objects.requireNonNull(service);
    }
    private DatabaseWriter dbWriter = new DatabaseWriter();
    
    public DistributionService(DistributionRepository distributionRepository, FoodHamperRequestRepository hamperRequestRepository, InventoryService inventoryService, InventoryRepository inventoryRepository) {
       if (distributionRepository == null || hamperRequestRepository == null || inventoryService == null || inventoryRepository == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.distributionRepository = distributionRepository;
        this.hamperRequestRepository = hamperRequestRepository;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
    }

    public void validateDistribution(int distributionId) {
        if (distributionId <= 0) {
            throw new IllegalArgumentException("Distribution ID must be positive");
        }

        Distribution distribution = distributionRepository.findById(distributionId);

        FoodHamperRequest request = hamperRequestRepository.findById(distribution.getRequestId());

        if (distribution.getPickupStatus() == PickupStatus.FULFILLED) {
            throw new IllegalStateException(" Distribution is already fulfilled");
        }

        if(request.getStatus() != HamperRequestStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot validate distribution for a request that is not scheduled");
        }
        if (distribution.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot validate an empty distribution");
        }
    }

    public void validateInventory(int distributionId) {
        Distribution distribution = distributionRepository.findById(distributionId);

        Map<Integer, Integer> requiredQuantities = new HashMap<>();

        for (DistributionItem item : distribution.getItems()) {
            int id = item.getInventoryId();
            int quantity = item.getQuantity();
            int previous = requiredQuantities.getOrDefault(id, 0);
            int totalRequired = Math.addExact(previous, quantity);
            InventoryItem stock = inventoryRepository.getInventoryItemById(id);

            if (stock == null) {
                throw new IllegalStateException(
                    "Inventory item not found: " + id
                );
            }
            if (!stock.isAvailable() || stock.isExpired(LocalDate.now()) || !stock.getUnit().equals(item.getUnit()))
                throw new IllegalStateException("Stock must be available, unexpired and use the same unit");
            if (stock.getQuantity() < totalRequired) {
                throw new IllegalStateException("Insufficient stock for inventory item ID: " + id);
            }
            requiredQuantities.put(id, totalRequired);
        }
    }

    public void fulfillDistribution(int distributionId, AuthService authService){
        authService.requireRole("Volunteer");
        // Validation eligibility
        validateDistribution(distributionId);

        // Validate stock availability
        validateInventory(distributionId);

        // Find distribution
        Distribution distribution = distributionRepository.findById(distributionId);

        // Find hamper request
        FoodHamperRequest request = hamperRequestRepository.findById(distribution.getRequestId());
        // cancelled order fulfillment
        if (request.getStatus() == HamperRequestStatus.CANCELLED) {
            throw new IllegalStateException(
                "Cannot fulfill a cancelled hamper request"
            );
        }
        authorizeDistribution(distribution, authService);
        dbWriter.fulfillDistribution(distributionId, distribution.getRequestId(), authService.getCurrentUserId(), distribution.getItems());

        // adjust stock
        for(DistributionItem item: distribution.getItems()) {
            int inventoryId = item.getInventoryId();
            int adjustment = item.getQuantity();
            inventoryService.adjustStock(inventoryId, -adjustment, InventoryChangeType.DISTRIBUTED);  
        }

        distribution.setVolunteerId(authService.getCurrentUserId());
        distribution.markPickupStatus(PickupStatus.FULFILLED);
        request.markFulfilled();
        
    }

    public void markMissedPickup(int distributionId, AuthService authService){
        authService.requireRole("Volunteer");
        if(distributionId<=0){
            throw new IllegalArgumentException("distribution id = 0");
        }

        Distribution distribution = distributionRepository.findById(distributionId);

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED && distribution.getPickupStatus() != PickupStatus.DELAYED && distribution.getPickupStatus() != PickupStatus.MISSED) {
            throw new IllegalStateException(
                "Only scheduled or delayed pickups can be marked missed"
            );
        }
        authorizeDistribution(distribution, authService);
        requireNotifications().markPickupStatus(distributionId, PickupStatus.MISSED);

    }

    public void markDelayedPickup(int distributionId,AuthService authService){
        authService.requireRole("Volunteer");
        if(distributionId<=0){
            throw new IllegalArgumentException("distribution id = 0");
        }

        Distribution distribution = distributionRepository.findById(distributionId);

        if(distribution.getPickupStatus() != PickupStatus.SCHEDULED && distribution.getPickupStatus() != PickupStatus.DELAYED){
            throw new IllegalArgumentException("pickup was not scheduled");
        }
        authorizeDistribution(distribution, authService);
        requireNotifications().markPickupStatus(distributionId, PickupStatus.DELAYED);
    }

    @Deprecated
    public void setPickupTime(int distributionId, LocalTime pickupTime) {
        throw new SecurityException("Use authenticated setPickupTime overload");
    }

    public void setPickupTime(int distributionId, LocalTime time, AuthService auth) {
        Distribution distribution = distributionRepository.findById(distributionId);
        auth.requireRole("Volunteer");
        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED || time == null)
            throw new IllegalArgumentException("Scheduled pickup and time required");
        authorizeDistribution(distribution, auth);
        validateShift(auth.getCurrentUserId(), distribution.getDistributionDate(), time);
        dbWriter.updateDistribution(distributionId, distribution.getPickupStatus().name(), time, distribution.getVolunteerId());
        distribution.setPickupTime(time);
    }

    public void setShiftRepository(VolunteerShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    private void authorizeDistribution(Distribution distribution, AuthService auth) {
        auth.requireRole("Volunteer");
        Integer assignedVolunteer = distribution.getVolunteerId();
        if (assignedVolunteer != null && assignedVolunteer != auth.getCurrentUserId()) {
            throw new SecurityException("Distribution belongs to another volunteer");
        }
        validateShift(auth.getCurrentUserId(), distribution.getDistributionDate(), distribution.getPickupTime());
    }

    private Distribution editableDistribution(int id, AuthService auth) {
        auth.requireRole("Volunteer");
        Distribution distribution = distributionRepository.findById(id);
        authorizeDistribution(distribution, auth);
        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled distributions can be modified");
        }
        return distribution;
    }

    public void addDistributionItem(int distributionId, DistributionItem item, AuthService auth) {
        Distribution distribution = editableDistribution(distributionId, auth);
        if (item == null) {
            throw new IllegalArgumentException("Item required");
        }
        InventoryItem inventory = inventoryRepository.getInventoryItemById(item.getInventoryId());
        if (!inventory.isAvailable() || inventory.isExpired(LocalDate.now()) || !inventory.getUnit().equals(item.getUnit()))
            throw new IllegalArgumentException("Available inventory and matching unit required");
        for (Distribution existingDistribution : distributionRepository.findAll()) {
            for (DistributionItem existing : existingDistribution.getItems()) {
                if (existing.getDistributionItemId() == item.getDistributionItemId()) {
                    throw new IllegalArgumentException("Item ID already exists");
                }
            }
        }
        int total = item.getQuantity();
        for (DistributionItem existing : distribution.getItems()) {
            if (existing.getInventoryId() == item.getInventoryId()) {
                throw new IllegalArgumentException("Update existing inventory line instead");
            }
        }
        if (total > inventory.getQuantity()) {
            throw new IllegalStateException("Insufficient inventory");
        }
        dbWriter.addDistributionItem(item.getDistributionItemId(), distributionId, item.getInventoryId(), total, item.getUnit());
        distribution.addItem(item);
    }

    private NotificationService requireNotifications() {
        if (notificationService == null) {
            throw new IllegalStateException("Notification service must be configured");
        }
        return notificationService;
    }

    private void validateShift(int volunteerId, LocalDate date, LocalTime time) {
        if (shiftRepository == null || time == null) {
            throw new IllegalStateException("Shift repository and pickup time are required");
        }
        for (VolunteerShift shift : shiftRepository.findByVolunteerId(volunteerId)) {
            if (shift.getShiftDate().equals(date) && !time.isBefore(shift.getStartTime()) && time.isBefore(shift.getEndTime())) {
                return;
            }
        }
        throw new SecurityException("No assigned shift");
    }

    public List<Distribution> searchDistributions(String recipientName, LocalDate startDate, LocalDate endDate, UserRepository userRepository, AuthService authService  ) {
        authService.requireRole("Volunteer");
        List<Distribution> result = new ArrayList<>();

        if (startDate != null && endDate != null &&
            startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        for (Distribution distribution : distributionRepository.findAll()) {

            FoodHamperRequest request =
                hamperRequestRepository.findById(distribution.getRequestId());

            User recipient =
                userRepository.findById(request.getRecipientId());

            if (recipientName != null &&
                !recipient.getName().toLowerCase()
                    .contains(recipientName.trim().toLowerCase())) {
                continue;
            }

            LocalDate date = distribution.getDistributionDate();

            if (startDate != null && date.isBefore(startDate)) {
                continue;
            }

            if (endDate != null && date.isAfter(endDate)) {
                continue;
            }

            result.add(distribution);
        }

        return result;
    }

    public void deleteDistribution(int distributionId, AuthService authService) {
        authService.requireRole("Volunteer");
        Distribution distribution = distributionRepository.findById(distributionId);
        authorizeDistribution(distribution, authService);

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled distributions can be deleted"
            );
        }
        dbWriter.deleteDistribution(distributionId);
        distributionRepository.delete(distributionId);
    }

    public void updateDistributionItem( int distributionId, int inventoryId,  int newQuantity, AuthService authService  ) {
        Distribution distribution = editableDistribution(distributionId, authService);

        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity");
        }

        InventoryItem inventory =
            inventoryRepository.getInventoryItemById(inventoryId);

        if (!inventory.isAvailable() || inventory.isExpired(LocalDate.now())) {

            throw new IllegalStateException("Stock unavailable");

        }
        int required = newQuantity;
        boolean first = true;
        for (DistributionItem line : distribution.getItems()) {
            if (line.getInventoryId() == inventoryId) {
                if (first) first = false;
                else required = Math.addExact(required, line.getQuantity());
            }
        }
        if (required > inventory.getQuantity()) {
            throw new IllegalStateException("Insufficient inventory");
        }

        for (DistributionItem item : distribution.getItems()) {
            if (item.getInventoryId() == inventoryId) {
                dbWriter.updateDistributionItem(
                    item.getDistributionItemId(),
                    newQuantity
                );
                item.setQuantity(newQuantity);
                return;
            }
        }

        throw new IllegalArgumentException("Distribution item not found");
    }
    public void removeDistributionItem(int distributionId, int inventoryId, AuthService authService) {
        Distribution distribution = editableDistribution(distributionId, authService);

        for (DistributionItem item : distribution.getItems()) {
            if (item.getInventoryId() == inventoryId) {
                dbWriter.deleteDistributionItems(distributionId, inventoryId);
                distribution.removeItem(inventoryId);
                return;
            }
        }
        throw new IllegalArgumentException("Distribution item not found");
    }
    
    public List<Distribution> searchDistributionHistory(AuthService authService, LocalDate startDate,LocalDate endDate) {
        User recipient = authService.requireRole("Recipient");
        List<Distribution> result = new ArrayList<>();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        for (Distribution distribution : distributionRepository.findAll()) {
            FoodHamperRequest request = hamperRequestRepository.findById( distribution.getRequestId()  );

            if (request.getRecipientId() != recipient.getUserId()) {
                continue;
            }
            LocalDate date = distribution.getDistributionDate();

            if (startDate != null && date.isBefore(startDate)) {
                continue;
            }
            if (endDate != null && date.isAfter(endDate)) {
                continue;
            }
            result.add(distribution);
        }

        return result;
    }
}
