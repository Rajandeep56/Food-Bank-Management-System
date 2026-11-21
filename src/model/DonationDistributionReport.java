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
    
        if (donationRepository == null || distributionRepository == null || inventoryService == null
                || requestRepository == null || userRepository == null || categoryRepository == null) {
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
        aggregateDonations();
        aggregateDistributions();

        for (Distribution distribution : distributionRepository.findAll()) {
            LocalDate date = distribution.getDistributionDate();

            if (date.isBefore(getStartDate()) || date.isAfter(getEndDate())) {
                continue;
            }

            totalDistributions++;
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
    }

    @Override
    public void display() {
        if (totalDonations == 0 && totalDistributions == 0) {
            System.out.println("No records found for the selected period.");
            return;
        }
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
        // The reporting date is the fulfilled distribution date, not the time history was loaded or written.
        for (Distribution distribution : distributionRepository.findAll()) {
            LocalDate date = distribution.getDistributionDate();
            if (distribution.getPickupStatus() != common.PickupStatus.FULFILLED
                    || date.isBefore(getStartDate()) || date.isAfter(getEndDate())) continue;
            for (DistributionItem item : distribution.getItems()) {
                InventoryItem inventory = inventoryService.getInventoryItem(item.getInventoryId());
                String key = inventory.getCategoryId() + " - " + item.getUnit();
                quantityDistributed.put(key, quantityDistributed.getOrDefault(key, 0L) + item.getQuantity());
            }
        }
    }

    public record Totals(int donations, int fulfilled, int missed, int delayed, int households, int people) {}
    public Totals getTotals() {
        return new Totals(totalDonations, fulfilledPickups, missedPickups, delayedPickups, householdsServed.size(), totalPeopleServed);
    }
    public Map<String, Long> getQuantityDonated() {
        return Map.copyOf(quantityDonated);
    }
    public Map<String, Long> getQuantityDistributed() {
        return Map.copyOf(quantityDistributed);
    }
    public int[] getMonthlyDonations() {
        return monthlyDonations.clone();
    }
    public int[] getMonthlyDistributions() {
        return monthlyDistributions.clone();
    }

    private void aggregateDonations() {

        for (Donation donation : donationRepository.findAll()) {

            LocalDate date = donation.getDonationDate()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            if (date.isBefore(getStartDate()) ||
                date.isAfter(getEndDate())) {
                continue;
            }

            String key = donation.getCategoryId()
                + " - " + donation.getUnit();

            totalDonations++;

            monthlyDonations[date.getMonthValue() - 1]++;

            donationCount.put(key,
                donationCount.getOrDefault(key, 0) + 1);

            quantityDonated.put(key,
                quantityDonated.getOrDefault(key, 0L)
                + donation.getQuantity());
        }
    }

    private void displayCategorySummary() {

        Set<String> keys = new TreeSet<>();

        keys.addAll(donationCount.keySet());
        keys.addAll(quantityDistributed.keySet());
        for (Category category : categoryRepository.findAll()) {
            String prefix = category.getCategoryId() + " - ";
            boolean found = false;
            for (String key : keys) {
                if (key.startsWith(prefix)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                keys.add(prefix + "–");
            }
        }

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
    public record MonthlySummary(int month, int donations, Map<String, Long> received, Map<String, Long> distributed,
            int fulfilled, int households, int people, int missed, int delayed) {}

    public List<MonthlySummary> getYearlyBreakdown() {
        List<MonthlySummary> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            int donations = 0, fulfilled = 0, missed = 0, delayed = 0, people = 0;
            Map<String, Long> received = new TreeMap<>(), distributed = new TreeMap<>();
            Set<Integer> households = new HashSet<>();
            for (Donation donation : donationRepository.findAll()) {
                LocalDate date = donation.getDonationDate().atZone(ZoneId.systemDefault()).toLocalDate();
                if (date.getMonthValue() == month && !date.isBefore(getStartDate()) && !date.isAfter(getEndDate())) {
                    donations++;
                    String key = donation.getCategoryId() + " - " + donation.getUnit();
                    received.put(key, received.getOrDefault(key, 0L) + donation.getQuantity());
                }
            }
            for (Distribution distribution : distributionRepository.findAll()) {
                LocalDate date = distribution.getDistributionDate();
                if (date.getMonthValue() != month || date.isBefore(getStartDate()) || date.isAfter(getEndDate())) continue;
                switch (distribution.getPickupStatus()) {
                    case FULFILLED:
                        fulfilled++;
                        int id = getRecipientId(distribution);
                        if (households.add(id)) people += ((Recipient) userRepository.findById(id)).getHouseholdSize();
                        for (DistributionItem item : distribution.getItems()) {
                            String key = inventoryService.getInventoryItem(item.getInventoryId()).getCategoryId() + " - " + item.getUnit();
                            distributed.put(key, distributed.getOrDefault(key, 0L) + item.getQuantity());
                        }
                        break;
                    case MISSED: missed++; break;
                    case DELAYED: delayed++; break;
                    default: break;
                }
            }
            months.add(new MonthlySummary(month, donations, Map.copyOf(received), Map.copyOf(distributed), fulfilled, households.size(), people, missed, delayed));
        }
        return List.copyOf(months);
    }

    private void displayMonthlyBreakdown() {
        System.out.println("Monthly Breakdown: donation count, fulfilled hampers, households, people, missed, delayed");
        for (MonthlySummary month : getYearlyBreakdown()) {
            System.out.printf("%s | %d | %d | %d | %d | %d | %d%n", java.time.Month.of(month.month()),
                month.donations(), month.fulfilled(), month.households(), month.people(), month.missed(), month.delayed());
            Set<String> keys = new TreeSet<>(month.received().keySet()); keys.addAll(month.distributed().keySet());
            for (String key : keys) System.out.println("  " + getCategoryLabel(key) + " | Received: "
                + month.received().getOrDefault(key, 0L) + " | Distributed: " + month.distributed().getOrDefault(key, 0L));
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
