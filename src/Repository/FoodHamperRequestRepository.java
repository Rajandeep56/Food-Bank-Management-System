package Repository;

import java.util.*;

import database.DatabaseWriter;
import model.FoodHamperRequest;

public class FoodHamperRequestRepository {
    private final HashMap<Integer, FoodHamperRequest> hamperRequests = new HashMap<>();
    private DatabaseWriter dbWriter = new DatabaseWriter();
    
    public void save(FoodHamperRequest hamperRequest) {
        if (hamperRequest == null) {
            throw new IllegalArgumentException("Hamper request cannot be null");
        }
        if (hamperRequests.containsKey(hamperRequest.getRequestId())) {
            throw new IllegalArgumentException("Hamper request with this ID already exists");
        }
        hamperRequests.put(hamperRequest.getRequestId(), hamperRequest);
    }

    public FoodHamperRequest findById(int requestId) {
        FoodHamperRequest hamperRequest = hamperRequests.get(requestId);
        if (hamperRequest == null) {
            throw new IllegalArgumentException("Hamper request not found");
        }
        return hamperRequest;
    }

    public List<FoodHamperRequest> findByRecipientId(int recipientId) {
        List<FoodHamperRequest> requests = new ArrayList<>();
        for (FoodHamperRequest request : hamperRequests.values()) {
            if (request.getRecipientId() == recipientId) {
                requests.add(request);
            }
        }
        return requests;
    }
}
