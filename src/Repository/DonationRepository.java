package Repository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import model.Donation;

public class DonationRepository{
    // <key: 501, Donation: Beans, 502, Donation: Rice>
    private final Map<Integer, Donation> donations = new HashMap<>();

    // saves new donation
    public void save(Donation donation){
        if(donation == null) {
            throw new IllegalArgumentException("Donation cannot be null");
        }
        int id = donation.getDonationId();
        if(donations.containsKey(id)) {
            throw new IllegalArgumentException("Donation with ID " + id + " already exists");
        }
        donations.put(donation.getDonationId(), donation);
    }
    
    // retrieves donation by id
    //iterate through all the donations and returns the donation with matching id
    public List<Donation> getDonationById(int donationId){
        if(donationId <= 0) {
            throw new IllegalArgumentException("Donation ID must be positive");
        }
        List<Donation> result = new ArrayList<>();
        if(donations.containsKey(donationId)) {
            result.add(donations.get(donationId));
        }
        return result;
    }

    public List<Donation> findAll() {
        return new ArrayList<>(donations.values());
    }

    public List<Donation> searchDonations(
    int donorId,
    LocalDate startDate,
    LocalDate endDate,
    Integer categoryId
    ) {
        List<Donation> result = new ArrayList<>();

        for (Donation donation : donations.values()) {

            if (donation.getDonorId() != donorId) {
                continue;
            }

            LocalDate date = donation.getDonationDate()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            if (startDate != null && date.isBefore(startDate)) {
                continue;
            }

            if (endDate != null && date.isAfter(endDate)) {
                continue;
            }

            if (categoryId != null &&
                donation.getCategoryId() != categoryId) {
                continue;
            }

            result.add(donation);
        }

        return result;
    }
}