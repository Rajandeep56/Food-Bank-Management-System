package web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import Services.AuthService;
import model.*;

public class Records {
    public static String read(FoodBankApp app, AuthService auth, Map<String, String> query, Report report) {
        User user = auth.getCurrentUser();
        StringBuilder output = new StringBuilder();
        switch (query.getOrDefault("type", "profile")) {
            case "profile":
                output.append("Name: ").append(user.getName()).append("\nEmail: ").append(user.getEmail())
                    .append("\nPhone: ").append(user.getPhone()).append("\nRole: ").append(user.getRole()).append('\n');
                if (user instanceof Recipient) {
                    Recipient recipient = (Recipient) user;
                    output.append("Household size: ").append(recipient.getHouseholdSize())
                        .append("\nDietary restrictions: ").append(text(recipient.getDietaryRestrictions()))
                        .append("\nEmergency contact: ").append(recipient.getEmergencyContact())
                        .append("\nApproval: ").append(recipient.getApprovalStatus()).append('\n');
                }
                break;
            case "categories":
                output.append("ID | Category\n");
                for (Category category : app.categories.findAll()) {
                    if (matches(category.getName(), query.get("name"))) {
                        output.append(category.getCategoryId()).append(" | ").append(category.getName()).append('\n');
                    }
                }
                break;
            case "donations":
                LocalDate from = date(query.get("from"));
                LocalDate to = date(query.get("to"));
                Integer categoryId = category(query);
                checkRange(from, to);
                List<Donation> donations;
                if (user.getRole().equals("Donor")) donations = app.donationService.searchDonationHistory(auth, from, to, categoryId);
                else { auth.requireRole("Volunteer"); donations = app.donations.findAll(); }
                output.append("ID | Donor | Item | Category | Quantity | Date | Status\n");
                for (Donation donation : donations) {
                    LocalDate day = donation.getDonationDate().atZone(ZoneId.systemDefault()).toLocalDate();
                    User donor = app.users.findById(donation.getDonorId());
                    if (!inRange(day, from, to)) continue;
                    if (categoryId != null && donation.getCategoryId() != categoryId) continue;
                    if (!matches(donation.getItemName(), query.get("item"))) continue;
                    if (!matches(donor.getName(), query.get("donor"))) continue;
                    output.append(donation.getDonationId()).append(" | ").append(donor.getName())
                        .append(" | ").append(donation.getItemName()).append(" | ").append(app.categories.findById(donation.getCategoryId()).getName())
                        .append(" | ").append(donation.getQuantity()).append(' ').append(donation.getUnit())
                        .append(" | ").append(day).append(" | ").append(donation.getStatus()).append('\n');
                }
                break;
            case "donors":
                if (!user.getRole().equals("Coordinator")) auth.requireRole("Volunteer");
                output.append("ID | Donor name\n");
                for (User donor : app.users.findByRole("Donor")) {
                    if (matches(donor.getName() + " " + donor.getEmail(), query.get("name"))) {
                        output.append(donor.getUserId()).append(" | ").append(donor.getName()).append('\n');
                    }
                }
                break;
            case "inventory":
                output.append("ID | Item | Category | Quantity | Expiry | Status\n");
                for (InventoryItem item : app.stock.searchInventory(query.get("item"), category(query), auth))
                    output.append(item.getInventoryId()).append(" | ").append(item.getItemName())
                        .append(" | ").append(app.categories.findById(item.getCategoryId()).getName())
                        .append(" | ").append(item.getQuantity()).append(' ').append(item.getUnit())
                        .append(" | ").append(text(item.getExpiryDate())).append(" | ").append(item.getStatus()).append('\n');
                break;
            case "requests":
                LocalDate requestFrom = date(query.get("from"));
                LocalDate requestTo = date(query.get("to"));
                checkRange(requestFrom, requestTo);
                output.append("ID | Pickup date | Status\n");
                for (FoodHamperRequest request : app.hamperService.getRecipientRequests(auth)) {
                    if (inRange(request.getPickupDate(), requestFrom, requestTo)) {
                        output.append(request.getRequestId()).append(" | ").append(text(request.getPickupDate()))
                            .append(" | ").append(request.getStatus()).append('\n');
                    }
                }
                break;
            case "distributions":
                List<Distribution> distributions;
                if (user.getRole().equals("Recipient"))
                    distributions = app.distributionService.searchDistributionHistory(auth, date(query.get("from")), date(query.get("to")));
                else {
                    auth.requireRole("Volunteer");
                    distributions = app.distributionService.searchDistributions(query.get("recipient"), date(query.get("from")), date(query.get("to")), app.users, auth);
                }
                output.append("ID | Recipient | Date | Time | Status\n");
                for (Distribution distribution : distributions) {
                    int recipientId = app.requests.findById(distribution.getRequestId()).getRecipientId();
                    output.append(distribution.getDistributionId()).append(" | ").append(app.users.findById(recipientId).getName())
                        .append(" | ").append(distribution.getDistributionDate()).append(" | ").append(text(distribution.getPickupTime()))
                        .append(" | ").append(distribution.getPickupStatus()).append('\n');
                    for (DistributionItem item : distribution.getItems())
                        output.append("  ").append(app.inventory.getInventoryItemById(item.getInventoryId()).getItemName())
                            .append(": ").append(item.getQuantity()).append(' ').append(item.getUnit()).append('\n');
                }
                break;
            case "users":
                auth.requireRole("Coordinator");
                output.append("ID | Name | Email | Phone | Role | Active\n");
                for (User person : app.users.findAll()) {
                    if (!matches(person.getRole(), query.get("role"))) continue;
                    if (matches(person.getName() + " " + person.getEmail(), query.get("name"))) {
                        output.append(person.getUserId()).append(" | ").append(person.getName()).append(" | ").append(person.getEmail())
                            .append(" | ").append(person.getPhone()).append(" | ").append(person.getRole()).append(" | ").append(person.isActive()).append('\n');
                    }
                }
                break;
            case "pending":
                output.append("ID | Name | Email | Household size\n");
                for (Recipient person : app.coordinatorService.getPendingRecipients(auth)) {
                    if (matches(person.getName() + " " + person.getEmail(), query.get("name"))) {
                        output.append(person.getUserId()).append(" | ").append(person.getName()).append(" | ").append(person.getEmail())
                            .append(" | ").append(person.getHouseholdSize()).append('\n');
                    }
                }
                break;
            case "shifts":
                List<VolunteerShift> shifts;
                if (user.getRole().equals("Coordinator")) shifts = app.shifts.findAll();
                else { auth.requireRole("Volunteer"); shifts = app.shiftService.getVolunteerShifts(auth); }
                LocalDate shiftFrom = date(query.get("from"));
                LocalDate shiftTo = date(query.get("to"));
                checkRange(shiftFrom, shiftTo);
                output.append("ID | Volunteer | Date | Start | End | Task\n");
                for (VolunteerShift shift : shifts) {
                    String volunteerName = app.users.findById(shift.getVolunteerId()).getName();
                    if (!matches(volunteerName, query.get("name"))) continue;
                    if (!inRange(shift.getShiftDate(), shiftFrom, shiftTo)) continue;
                    output.append(shift.getShiftId()).append(" | ").append(volunteerName)
                        .append(" | ").append(shift.getShiftDate()).append(" | ").append(shift.getStartTime())
                        .append(" | ").append(shift.getEndTime()).append(" | ").append(shift.getAssignedTask()).append('\n');
                }
                break;
            case "report":
                auth.requireRole("Coordinator");
                if (report == null || !report.isGenerated()) return "Generate a report first.\n";
                output.append("Period: ").append(report.getStartDate()).append(" to ").append(report.getEndDate())
                    .append(report.isToDate() ? " (to date)" : "").append("\nCoordinator: ").append(report.getCoordinatorName()).append('\n');
                if (report instanceof DonationDistributionReport) {
                    DonationDistributionReport summary = (DonationDistributionReport) report;
                    DonationDistributionReport.Totals totals = summary.getTotals();
                    output.append("Donations: ").append(totals.donations()).append("\nFulfilled hampers: ").append(totals.fulfilled())
                        .append("\nHouseholds served: ").append(totals.households()).append("\nPeople served: ").append(totals.people())
                        .append("\nMissed pickups: ").append(totals.missed()).append("\nDelayed pickups: ").append(totals.delayed()).append('\n');
                    output.append("Received by category ID and unit:\n");
                    for (String key : summary.getQuantityDonated().keySet())
                        output.append(key).append(" | ").append(summary.getQuantityDonated().get(key)).append('\n');
                    output.append("Distributed by category ID and unit:\n");
                    for (String key : summary.getQuantityDistributed().keySet())
                        output.append(key).append(" | ").append(summary.getQuantityDistributed().get(key)).append('\n');
                } else if (report instanceof InventoryWasteReport) {
                    InventoryWasteReport summary = (InventoryWasteReport) report;
                    output.append("Category | Unit | Items wasted | Wasted | Received | Waste rate\n");
                    for (InventoryWasteReport.Row row : summary.getRows())
                        output.append(app.categories.findById(row.categoryId()).getName()).append(" | ").append(row.unit())
                            .append(" | ").append(row.itemsWasted()).append(" | ").append(row.quantityWasted())
                            .append(" | ").append(row.quantityReceived()).append(" | ").append(row.wasteRate() == null ? "-" : row.wasteRate() + "%").append('\n');
                    output.append("Expiry alerts sent: ").append(summary.getExpiryAlerts()).append('\n');
                }
                break;
            default:
                throw new IllegalArgumentException("Choose a record type");
        }
        return output.toString();
    }

    private static boolean matches(String value, String search) {
        if (search == null || search.isBlank()) return true;
        return value != null && value.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT));
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before to date");
        }
    }

    private static boolean inRange(LocalDate day, LocalDate from, LocalDate to) {
        if (day == null) return from == null && to == null;
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    private static LocalDate date(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
    private static Integer category(Map<String, String> query) {
        String value = query.get("category");
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
    private static String text(Object value) { return value == null ? "-" : value.toString(); }
}
