package Services;

import model.Distribution;
import model.Donation;
import model.FoodHamperRequest;
import model.InventoryItem;
import model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import Repository.DistributionRepository;
import Repository.DonationRepository;
import Repository.FoodHamperRequestRepository;
import common.DonationStatus;

public class DonationService {
    private final DonationRepository donationRepository; // donationrepository handles donation
    private final InventoryService inventoryService; // inventoryservice handles inventory stock updates and history for auditing.
    private final DistributionRepository distributionRepository;
    private final FoodHamperRequestRepository requestRepository;

    public DonationService(DonationRepository donationRepository, InventoryService inventoryService, FoodHamperRequestRepository requestRepository, DistributionRepository distributionRepository) {
        if(donationRepository == null) {
            throw new IllegalArgumentException("DonationRepository cannot be null");
        }
        if(inventoryService == null) {
            throw new IllegalArgumentException("InventoryService cannot be null");
        }
        this.donationRepository = donationRepository;
        this.inventoryService = inventoryService;
        this.distributionRepository = distributionRepository;
        this.requestRepository =  requestRepository;
    }

    //validates and save a new donation
    public void createDonation(Donation donation) {
        if(donation == null) {
            throw new IllegalArgumentException("Donation cannot be null");
        }
        donationRepository.save(donation);
    }

    // create inventory and mark the donation comepleted
    
    public void completeDonation(
            int donationId,
            InventoryItem inventoryItem) {

        List<Donation> donationList = donationRepository.getDonationById(donationId);

        if (donationList.isEmpty()) {
            throw new IllegalArgumentException("Donation not found");
        }

        Donation donation = donationList.get(0);

        if (donation.getStatus() == DonationStatus.COMPLETED) {
            throw new IllegalStateException("Donation already completed");
        }

        if (inventoryItem == null || inventoryItem.getDonationId() != donationId) {
            throw new IllegalArgumentException(
                "Inventory must belong to this donation"
            );
        }

        if (donation.getStatus() == DonationStatus.CREATED ||
            donation.getStatus() == DonationStatus.FAILED) {
            donation.setStatus(DonationStatus.PENDING);
        }

        try {
            inventoryService.addInventoryItem(inventoryItem);
            donation.setStatus(DonationStatus.COMPLETED);
        } catch (RuntimeException ex) {
            if (donation.getStatus() == DonationStatus.PENDING) {
                donation.setStatus(DonationStatus.FAILED);
            }
            throw ex;
        }
    }

    // retrieve a donation
    public Donation getDonationById(int donationId) {
        if(donationId <= 0) {
            throw new IllegalArgumentException("Donation ID must be positive");
        }
        List<Donation> donationList = donationRepository.getDonationById(donationId);
        if (donationList.isEmpty()) {
            throw new IllegalArgumentException("Donation not found");
        }
        return donationList.get(0);
    }


    public List<Distribution> searchDistributionHistory(
        AuthService authService,
        LocalDate startDate,
        LocalDate endDate
    ) {
         User recipient = authService.requireRole("Recipient");

        int recipientId = recipient.getUserId();
        List<Distribution> result = new ArrayList<>();

        if (startDate != null && endDate != null &&
            startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        for (Distribution distribution : distributionRepository.findAll()) {

            FoodHamperRequest request =
                requestRepository.findById(distribution.getRequestId());

            if (request.getRecipientId() != recipientId) {
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

    public List<Donation> searchDonationHistory(
        AuthService authService,
        LocalDate startDate,
        LocalDate endDate,
        Integer categoryId
    ) {
        User donor = authService.requireRole("Donor");

        if (startDate != null && endDate != null &&
            startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }

        return donationRepository.searchDonations(
            donor.getUserId(),
            startDate,
            endDate,
            categoryId
        );
    }
}