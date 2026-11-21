package Services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import Repository.DistributionRepository;
import Repository.FoodHamperRequestRepository;
import common.RecipientApprovalStatus;
import database.DatabaseWriter;
import model.Distribution;
import model.FoodHamperRequest;
import model.Recipient;
import model.User;

public class FoodHamperRequestService {

    private final FoodHamperRequestRepository requestRepository;
    private final DistributionRepository distributionRepository;
    private DatabaseWriter dbWriter = new DatabaseWriter();

    public FoodHamperRequestService(FoodHamperRequestRepository requestRepository, DistributionRepository distributionRepository) {

        if (requestRepository == null || distributionRepository == null) {
            throw new IllegalArgumentException(
                "Repository cannot be null"
            );
        }

        this.requestRepository = requestRepository;
        this.distributionRepository = distributionRepository;
    }

    private void validateRecipient(Recipient recipient){
        if(recipient==null){
            throw new IllegalArgumentException("recipeint is null");

        }
        if(recipient.getApprovalStatus() != RecipientApprovalStatus.APPROVED){
            throw new IllegalArgumentException(" Recipient status is not approved ");
        }
    }

    public FoodHamperRequest requestHamper(int requestId,  AuthService authService, LocalDate pickupDate, LocalTime pickupTime){
        Recipient recipient = (Recipient) authService.requireRole("Recipient");
        validateRecipient(recipient);
        if(requestId<=0){
            throw new IllegalArgumentException("recipient id is 0");
        }
        if(pickupDate==null || pickupDate.isBefore(LocalDate.now())){
            throw new IllegalArgumentException("Issue is with pickupdate");
        }
        if (pickupTime == null) {
            throw new IllegalArgumentException("Pickup time is required");
        }
        if (!distributionRepository.findByRequestId(requestId).isEmpty()) {
            throw new IllegalArgumentException("Request already exists");
        }
        for (FoodHamperRequest existing : requestRepository.findByRecipientId(recipient.getUserId())) {
            if (existing.getRequestId() == requestId) {
                throw new IllegalArgumentException("Request already exists");
            }
        }
        FoodHamperRequest request = new FoodHamperRequest(requestId, recipient.getUserId());
        request.schedulePickup(pickupDate);

        Distribution distribution = new Distribution(requestId, requestId, pickupDate);
        distribution.setPickupTime(pickupTime);
        dbWriter.createHamperRequest(
            requestId,
            authService.getCurrentUserId(),
            pickupDate, pickupTime
        );
        requestRepository.save(request);
        distributionRepository.save(distribution);
        return request;
    }

    public FoodHamperRequest requestHamper(int requestId, AuthService authService, LocalDate pickupDate) {

        return requestHamper(requestId, authService, pickupDate, LocalTime.of(10, 0));
    }

    public void cancelRequest(int requestId,     AuthService authService){
        User recipient = authService.requireRole("Recipient");
        FoodHamperRequest request =
        requestRepository.findById(requestId);

        if (request.getRecipientId() != recipient.getUserId()) {
            throw new SecurityException("Access denied");
        }
        if (request.getStatus() == common.HamperRequestStatus.FULFILLED) {
            throw new IllegalStateException("Cannot cancel fulfilled request");
        }
        dbWriter.cancelHamperRequest(requestId);
        for (Distribution d : distributionRepository.findByRequestId(requestId)) {
            if (d.getPickupStatus() == common.PickupStatus.SCHEDULED || d.getPickupStatus() == common.PickupStatus.DELAYED)
                distributionRepository.delete(d.getDistributionId());
        }
        request.cancel();
    }
    
    public List<FoodHamperRequest> getRecipientRequests(AuthService authService){
        User recipient = authService.requireRole("Recipient");

        return requestRepository.findByRecipientId(
            recipient.getUserId()
        );
    }
}