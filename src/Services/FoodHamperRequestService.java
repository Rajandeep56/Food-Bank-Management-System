package Services;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import Repository.FoodHamperRequestRepository;
import common.RecipientApprovalStatus;
import model.FoodHamperRequest;
import model.Recipient;
import model.User;

public class FoodHamperRequestService {

    private final FoodHamperRequestRepository requestRepository;

    public FoodHamperRequestService(FoodHamperRequestRepository requestRepository) {

        if (requestRepository == null) {
            throw new IllegalArgumentException(
                "Repository cannot be null"
            );
        }

        this.requestRepository = requestRepository;
    }

    private void validateRecipient(Recipient recipient){
        if(recipient==null){
            throw new IllegalArgumentException("recipeint is null");

        }
        if(recipient.getApprovalStatus() != RecipientApprovalStatus.APPROVED){
            throw new IllegalArgumentException(" Recipient status is not approved ");
        }
    }

        public FoodHamperRequest requestHamper(
        int requestId,
        AuthService authService,
        LocalDate pickupDate
    ){
        Recipient recipient = (Recipient) authService.requireRole("Recipient");
        validateRecipient(recipient);
        if(requestId<=0){
            throw new IllegalArgumentException("recipient id is 0");
        }
        if(pickupDate==null || pickupDate.isBefore(LocalDate.now())){
            throw new IllegalArgumentException("Issue is with pickupdate");
        }
        FoodHamperRequest request = new FoodHamperRequest(requestId, recipient.getUserId());
        request.schedulePickup(pickupDate);
        requestRepository.save(request);
        return request;
    }

    public void cancelRequest(int requestId,     AuthService authService){
        User recipient = authService.requireRole("Recipient");
        FoodHamperRequest request =
        requestRepository.findById(requestId);

        if (request.getRecipientId() != recipient.getUserId()) {
            throw new SecurityException("Access denied");
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