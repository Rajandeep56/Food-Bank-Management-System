package model;

import java.time.LocalDate;
import java.time.ZoneId;

import Repository.DistributionRepository;
import Repository.DonationRepository;
import java.util.*;

public class DonationDistributionReport extends Report {
    private final DonationRepository donationRepository;
    private final DistributionRepository distributionRepository;

    private int totalDonations;
    private int totalDistributions;
    private int fulfilledPickups;
    private int missedPickups;
    private int delayedPickups;

    private final Map<String, Integer> donationCount = new HashMap<>();
    private final Map<String, Long> quantityDonated = new HashMap<>();
    private final Map<String, Long> quantityDistributed = new HashMap<>();

    @Override
    public void generate() {
        if (getStartDate() == null || getEndDate() == null) {
        throw new IllegalStateException(
            "Report period must be specified first"
        );
        }
        totalDonations = 0;
        totalDistributions = 0;
        fulfilledPickups = 0;
        missedPickups = 0;
        delayedPickups = 0;
        donationCount.clear();
        quantityDonated.clear();
        quantityDistributed.clear();
        for (Distribution distribution : distributionRepository.findAll()) {

            LocalDate date = distribution.getDistributionDate();

            if (date.isBefore(getStartDate()) ||
                date.isAfter(getEndDate())) {
                continue;
            }

            totalDistributions++;

            switch (distribution.getPickupStatus()) {
                case FULFILLED:
                    fulfilledPickups++;
                    break;

                case MISSED:
                    missedPickups++;
                    break;

                case DELAYED:
                    delayedPickups++;
                    break;

                default:
                    break;
            }
        }
        for (Donation donation : donationRepository.findAll()) {


            LocalDate date = donation.getDonationDate().atZone(ZoneId.systemDefault()).toLocalDate();

            if (date.isBefore(getStartDate()) ||
                date.isAfter(getEndDate())) {
                continue;
            }

            totalDonations++;
            String key = donation.getCategoryId() + " - " + donation.getUnit();

            donationCount.put(key, donationCount.getOrDefault(key, 0) + 1);

            quantityDonated.put(key, quantityDonated.getOrDefault(key, 0L) + donation.getQuantity());
        }

    }

    @Override
    public void display() {
        displayHeader("Donation and Distribution Summary");
        System.out.println("Period: " + getStartDate() + " to " + getEndDate());
        System.out.println("Total Donations: " + totalDonations);
        System.out.println("Total Distributions: " + totalDistributions);
        System.out.println("Fulfilled Pickups: " + fulfilledPickups);
        System.out.println("Missed Pickups: " + missedPickups);
        System.out.println("Delayed Pickups: " + delayedPickups);
    }

    public DonationDistributionReport(DonationRepository donationRepository, DistributionRepository distributionRepository){
    
        if (donationRepository == null || distributionRepository == null) {
        throw new IllegalArgumentException(
            "Repositories cannot be null"
            );
        }

        this.donationRepository = donationRepository;
        this.distributionRepository = distributionRepository;
    }
}
