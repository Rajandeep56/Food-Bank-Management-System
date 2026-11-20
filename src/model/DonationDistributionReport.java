package model;

import java.time.LocalDate;
import java.time.ZoneId;

import Repository.CategoryRepository;
import Repository.DistributionRepository;
import Repository.DonationRepository;
import Repository.FoodHamperRequestRepository;
import Repository.UserRepository;
import Services.InventoryService;
import common.InventoryChangeType;
import common.ReportPeriodType;

import java.util.*;

public class DonationDistributionReport extends Report {
    private final DonationRepository donationRepository;
    private final DistributionRepository distributionRepository;
    private final InventoryService inventoryService;
    private final FoodHamperRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private int totalDonations;
    private int totalDistributions;
    private int fulfilledPickups;
    private int missedPickups;
    private int delayedPickups;

    private final Map<String, Integer> donationCount = new HashMap<>();
    private final Map<String, Long> quantityDonated = new HashMap<>();
    private final Map<String, Long> quantityDistributed = new HashMap<>();
    private final Map<Integer, Integer> missedByRecipient = new HashMap<>();
    private final Map<Integer, Integer> delayedByRecipient = new HashMap<>();

    private final Set<Integer> householdsServed = new HashSet<>();
    private int totalPeopleServed;

    private final int[] monthlyDonations = new int[12];
    private final int[] monthlyDistributions = new int[12];

    public DonationDistributionReport(DonationRepository donationRepository, DistributionRepository distributionRepository, InventoryService inventoryService, FoodHamperRequestRepository requestRepository,UserRepository userRepository, CategoryRepository categoryRepository ){
    
        if (donationRepository == null || distributionRepository == null) {
        throw new IllegalArgumentException(
            "Repositories cannot be null"
            );
        }

        this.donationRepository = donationRepository;
        this.distributionRepository = distributionRepository;
        this.inventoryService = inventoryService;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

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
        missedByRecipient.clear();
        delayedByRecipient.clear();
        householdsServed.clear();
        Arrays.fill(monthlyDonations, 0);
        Arrays.fill(monthlyDistributions, 0);
        totalPeopleServed = 0;
        aggregateDistributions();

        for (Distribution distribution : distributionRepository.findAll()) {

            LocalDate date = distribution.getDistributionDate();

            if (date.isBefore(getStartDate()) ||
                date.isAfter(getEndDate())) {
                continue;
            }

            totalDistributions++;
            monthlyDonations[date.getMonthValue() - 1]++;

            switch (distribution.getPickupStatus()) {
                case FULFILLED:
                    fulfilledPickups++;
                    monthlyDistributions[date.getMonthValue() - 1]++;
                    countHousehold(distribution);
                    break;

                case MISSED:
                    missedPickups++;
                    missedByRecipient.merge(getRecipientId(distribution), 1, Integer::sum);
                    break;

                case DELAYED:
                    delayedPickups++;
                    delayedByRecipient.merge(getRecipientId(distribution), 1, Integer::sum);
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
        System.out.println("Households Served: " + householdsServed.size());

        System.out.println("People Served: " + totalPeopleServed);
        if (getPeriodType() == ReportPeriodType.YEARLY) {
            displayMonthlyBreakdown();
        }
        displayCategorySummary();
        displayPickupRanking();

    }

    private void aggregateDistributions() {

    for (InventoryHistory history : inventoryService.getHistory()) {

        if (history.getChangeType() != InventoryChangeType.DISTRIBUTED) {
            continue;
        }

        LocalDate date = history.getOccurredAt()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();

        if (date.isBefore(getStartDate()) ||
            date.isAfter(getEndDate())) {
            continue;
        }

        String key = history.getCategoryId()
            + " - " + history.getUnit();

        quantityDistributed.put(key,
            quantityDistributed.getOrDefault(key, 0L)
            + Math.abs((long) history.getQuantityChange()));
            }
    }
    private void displayCategorySummary() {

        Set<String> keys = new TreeSet<>();

        keys.addAll(donationCount.keySet());
        keys.addAll(quantityDistributed.keySet());

        System.out.printf("%-20s %-15s %-15s %-15s%n",
            "Category - Unit",
            "Donations",
            "Qty Donated",
            "Qty Distributed");

        for (String key : keys) {

            System.out.printf("%-20s %-15d %-15d %-15d%n",
                getCategoryLabel(key),
                donationCount.getOrDefault(key, 0),
                quantityDonated.getOrDefault(key, 0L),
                quantityDistributed.getOrDefault(key, 0L));
        }
    }

    private void countHousehold(Distribution distribution) {

        FoodHamperRequest request =
            requestRepository.findById(distribution.getRequestId());

        int recipientId = request.getRecipientId();

        if (householdsServed.add(recipientId)) {

            Recipient recipient =
                (Recipient) userRepository.findById(recipientId);

            totalPeopleServed += recipient.getHouseholdSize();
        }
    }
    private void displayMonthlyBreakdown() {

        System.out.println("\nMonthly Breakdown");

        System.out.printf("%-15s %-15s %-15s%n",
            "Month", "Donations", "Distributions");

        for (int i = 0; i < 12; i++) {

            System.out.printf("%-15s %-15d %-15d%n",
                java.time.Month.of(i + 1),
                monthlyDonations[i],
                monthlyDistributions[i]);
        }
    }

    private int getRecipientId(Distribution distribution) {
        return requestRepository.findById(distribution.getRequestId()).getRecipientId();
    }

    private void displayPickupRanking() {

        Set<Integer> ids = new HashSet<>();

        ids.addAll(missedByRecipient.keySet());
        ids.addAll(delayedByRecipient.keySet());

        List<Integer> recipients = new ArrayList<>(ids);

        recipients.sort((a, b) -> {
            int totalA = missedByRecipient.getOrDefault(a, 0)
                    + delayedByRecipient.getOrDefault(a, 0);

            int totalB = missedByRecipient.getOrDefault(b, 0)
                    + delayedByRecipient.getOrDefault(b, 0);

            if (totalA != totalB) {
                return Integer.compare(totalB, totalA);
            }

            return userRepository.findById(a).getName()
                .compareToIgnoreCase(
                    userRepository.findById(b).getName());
        });

        System.out.println("\nRecipient Pickup Ranking");

        for (int id : recipients) {

            int missed = missedByRecipient.getOrDefault(id, 0);
            int delayed = delayedByRecipient.getOrDefault(id, 0);

            System.out.println(
                userRepository.findById(id).getName()
                + " | Missed: " + missed
                + " | Delayed: " + delayed
                + " | Total: " + (missed + delayed)
            );
        }
    }
    private String getCategoryLabel(String key) {

        String[] parts = key.split(" - ", 2);

        int categoryId = Integer.parseInt(parts[0]);

        String categoryName = categoryRepository
            .findById(categoryId).getName();

        return categoryName + " - " + parts[1];
    }
}
