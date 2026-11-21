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
            int totalRequired = previous + quantity;
            InventoryItem stock = inventoryRepository.getInventoryItemById(id);

            if (stock == null) {
                throw new IllegalStateException(
                    "Inventory item not found: " + id
                );
            }
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
        validateShift(authService.getCurrentUserId(), distribution.getDistributionDate(), distribution.getPickupTime());
        dbWriter.fulfillDistribution(distributionId, distribution.getRequestId(), authService.getCurrentUserId(), distribution.getItems());

        // adjust stock
        for(DistributionItem item: distribution.getItems()) {
            int inventoryId = item.getInventoryId();
            int adjustment = item.getQuantity();
            inventoryService.adjustStock(inventoryId, -adjustment, InventoryChangeType.DISTRIBUTED);  
        }

        distribution.markPickupStatus(PickupStatus.FULFILLED);
        request.markFulfilled();
        
    }

    public void markMissedPickup(int distributionId, AuthService authService){
        authService.requireRole("Volunteer");
        if(distributionId<=0){
            throw new IllegalArgumentException("distribution id = 0");
        }

        Distribution distribution = distributionRepository.findById(distributionId);

        if (distribution==null){
            throw new IllegalStateException("distribution not found");
        }

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED && distribution.getPickupStatus() != PickupStatus.DELAYED) {
            throw new IllegalStateException(
                "Only scheduled or delayed pickups can be marked missed"
            );
        }
        dbWriter.updatePickupStatus(distributionId, "MISSED");
        distribution.markPickupStatus(PickupStatus.MISSED);

    }

    public void markDelayedPickup(int distributionId,AuthService authService){
        authService.requireRole("Volunteer");
        if(distributionId<=0){
            throw new IllegalArgumentException("distribution id = 0");
        }

        Distribution distribution = distributionRepository.findById(distributionId);

        if (distribution==null){
            throw new IllegalStateException("distribution not found");
        }

        if(distribution.getPickupStatus() != PickupStatus.SCHEDULED){
            throw new IllegalArgumentException("pickup was not scheduled");
        }
        dbWriter.updatePickupStatus(distributionId, "DELAYED");
        distribution.markPickupStatus(PickupStatus.DELAYED);
    }

    public void setPickupTime(int distributionId, LocalTime pickupTime) {

        Distribution distribution =
            distributionRepository.findById(distributionId);

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled pickups can be modified"
            );
        }

        distribution.setPickupTime(pickupTime);
    }

    public void setShiftRepository(VolunteerShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    private void validateShift(int volunteerId, LocalDate date, LocalTime time) {
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

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled distributions can be deleted"
            );
        }
        dbWriter.deleteDistribution(distributionId);
        distributionRepository.delete(distributionId);
    }

    public void updateDistributionItem( int distributionId, int inventoryId,  int newQuantity, AuthService authService  ) {
        authService.requireRole("Volunteer");
        Distribution distribution = distributionRepository.findById(distributionId);

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled distributions can be modified"
            );
        }

        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity");
        }

        InventoryItem inventory =
            inventoryRepository.getInventoryItemById(inventoryId);

        if (newQuantity > inventory.getQuantity()) {
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
        authService.requireRole("Volunteer");
        Distribution distribution = distributionRepository.findById(distributionId);

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled distributions can be modified"
            );
        }

        for (DistributionItem item : distribution.getItems()) {
            if (item.getInventoryId() == inventoryId) {
                dbWriter.deleteDistributionItem(
                    item.getDistributionItemId()
                );
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
