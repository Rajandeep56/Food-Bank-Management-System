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
import common.HamperRequestStatus;
import common.InventoryChangeType;
import common.PickupStatus;
import model.Distribution;
import model.DistributionItem;
import model.FoodHamperRequest;
import model.InventoryItem;
import model.User;

public class DistributionService {
    private final DistributionRepository distributionRepository;
    private final FoodHamperRequestRepository hamperRequestRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    
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
        // adjust stock
        for(DistributionItem item: distribution.getItems()) {
            int inventoryId = item.getInventoryId();
            int adjustment = item.getQuantity();
            inventoryService.adjustStock(inventoryId, -adjustment, InventoryChangeType.DISTRIBUTED);  
        }

        distribution.markPickupStatus(PickupStatus.FULFILLED);
        request.markFulfilled();
        
    }
    public void markMissedPickup(int distributionId){
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

        distribution.markPickupStatus(PickupStatus.MISSED);

    }

    public void markDelayedPickup(int distributionId){
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

    public List<Distribution> searchDistributions(
    String recipientName,
    LocalDate startDate,
    LocalDate endDate,
    UserRepository userRepository,
    AuthService authService
    ) {
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
    public void deleteDistribution(int distributionId) {

        Distribution distribution =
            distributionRepository.findById(distributionId);

        if (distribution.getPickupStatus() != PickupStatus.SCHEDULED) {
            throw new IllegalStateException(
                "Only scheduled distributions can be deleted"
            );
        }

        distributionRepository.delete(distributionId);
    }
    public void updateDistributionItem(
    int distributionId,
    int inventoryId,
    int newQuantity
    ) {
        Distribution distribution =
            distributionRepository.findById(distributionId);

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
                item.setQuantity(newQuantity);
                return;
            }
        }

        throw new IllegalArgumentException("Distribution item not found");
    }
    public void removeDistributionItem(
    int distributionId,
    int inventoryId,
    AuthService authService
        ) {
            authService.requireRole("Volunteer");
            Distribution distribution =
                distributionRepository.findById(distributionId);

            distribution.removeItem(inventoryId);
        }
}
