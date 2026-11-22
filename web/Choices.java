package web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import Services.AuthService;
import model.*;

public class Choices {
    public static String read(FoodBankApp app, AuthService auth, Map<String, String> query) {
        User current = auth.getCurrentUser();
        StringBuilder choices = new StringBuilder();
        String type = query.getOrDefault("type", "");
        String search = query.getOrDefault("q", "").trim().toLowerCase(java.util.Locale.ROOT);
        switch (type) {
            case "pending":
                for (Recipient person : app.coordinatorService.getPendingRecipients(auth)) {
                    add(choices, person.getUserId(), person.getName() + " (" + person.getEmail() + ")", search);
                }
                break;
            case "users":
                auth.requireRole("Coordinator");
                for (User person : app.users.findAll()) {
                    String label = person.getName() + " (" + person.getEmail() + ") — " + person.getRole();
                    if (!person.isActive()) label += " — inactive";
                    add(choices, person.getUserId(), label, search);
                }
                break;
            case "volunteers":
                auth.requireRole("Coordinator");
                for (User person : app.users.findByRole("Volunteer")) {
                    add(choices, person.getUserId(), person.getName() + " (" + person.getEmail() + ")", search);
                }
                break;
            case "donors":
                if (!current.getRole().equals("Coordinator")) auth.requireRole("Volunteer");
                for (User person : app.users.findByRole("Donor")) {
                    add(choices, person.getUserId(), person.getName() + " (" + person.getEmail() + ")", search);
                }
                break;
            case "categories":
                for (Category category : app.categories.findAll()) {
                    add(choices, category.getCategoryId(), category.getName(), search);
                }
                break;
            case "inventory":
            case "stock":
                auth.requireRole("Volunteer");
                for (InventoryItem item : app.inventory.getAllInventoryItems()) {
                    if (item.isAvailable()) {
                        if (type.equals("stock") && (item.getQuantity() == 0 || item.isExpired(LocalDate.now()))) continue;
                        String label = item.getItemName() + " — " + item.getQuantity() + " " + item.getUnit()
                            + " — expires " + (item.getExpiryDate() == null ? "not set" : item.getExpiryDate())
                            + " (batch " + item.getInventoryId() + ")";
                        add(choices, item.getInventoryId(), label, search);
                    }
                }
                break;
            case "pendingDonations":
                auth.requireRole("Volunteer");
                for (Donation donation : app.donations.findAll()) {
                    if (donation.getStatus() != common.DonationStatus.COMPLETED) {
                        String label = donation.getItemName() + " — " + app.users.findById(donation.getDonorId()).getName()
                            + " — " + donation.getQuantity() + " " + donation.getUnit() + " — "
                            + donation.getDonationDate().atZone(ZoneId.systemDefault()).toLocalDate() + " — " + donation.getStatus()
                            + " (ref " + donation.getDonationId() + ")";
                        add(choices, donation.getDonationId(), label, search);
                    }
                }
                break;
            case "requests":
                for (FoodHamperRequest request : app.hamperService.getRecipientRequests(auth)) {
                    if (request.getStatus() == common.HamperRequestStatus.PENDING || request.getStatus() == common.HamperRequestStatus.SCHEDULED) {
                        String pickup = "Pickup " + request.getPickupDate();
                        for (Distribution scheduled : app.distributions.findByRequestId(request.getRequestId())) {
                            pickup += " " + scheduled.getPickupTime();
                        }
                        add(choices, request.getRequestId(), pickup + " — " + request.getStatus() + " (ref " + request.getRequestId() + ")", search);
                    }
                }
                break;
            case "distributions":
            case "pickups":
                auth.requireRole("Volunteer");
                int pickupCount = 0;
                int myPickupCount = 0;
                int timedPickupCount = 0;
                int availablePickupCount = 0;
                for (Distribution distribution : app.distributions.findAll()) {
                    boolean scheduled = distribution.getPickupStatus() == common.PickupStatus.SCHEDULED;
                    if (!scheduled && (!type.equals("pickups") || distribution.getPickupStatus() == common.PickupStatus.FULFILLED)) continue;
                    pickupCount++;
                    if (distribution.getVolunteerId() != null && distribution.getVolunteerId() != current.getUserId()) continue;
                    myPickupCount++;
                    if (distribution.getPickupTime() == null) continue;
                    timedPickupCount++;
                    if (!canChange(app, distribution, current.getUserId())) continue;
                    availablePickupCount++;
                    int recipientId = app.requests.findById(distribution.getRequestId()).getRecipientId();
                    User recipient = app.users.findById(recipientId);
                    String label = recipient.getName() + " (" + recipient.getEmail() + ") — " + distribution.getDistributionDate()
                        + " " + distribution.getPickupTime() + " — " + distribution.getPickupStatus() + " (ref " + distribution.getDistributionId() + ")";
                    add(choices, distribution.getDistributionId(), label, search);
                }
                if (choices.length() == 0) {
                    if (pickupCount == 0) return "<option value=\"\">No scheduled pickups. A recipient must request a hamper first.</option>";
                    if (myPickupCount == 0) return "<option value=\"\">These pickups are assigned to other volunteers.</option>";
                    if (timedPickupCount == 0) return "<option value=\"\">The pickups need a pickup time before you can add items.</option>";
                    if (availablePickupCount == 0) return "<option value=\"\">No shift covers these pickups. A coordinator must assign your shift.</option>";
                    return "<option value=\"\">No pickups match your search.</option>";
                }
                break;
            case "distributionItems":
                auth.requireRole("Volunteer");
                String distributionId = query.get("distributionId");
                if (distributionId == null || distributionId.isBlank()) return "<option value=\"\">Choose a distribution first</option>";
                Distribution distribution = app.distributions.findById(Integer.parseInt(distributionId));
                for (DistributionItem item : distribution.getItems()) {
                    String label = app.inventory.getInventoryItemById(item.getInventoryId()).getItemName()
                        + " — " + item.getQuantity() + " " + item.getUnit();
                    add(choices, item.getInventoryId(), label, search);
                }
                break;
            case "shifts":
                auth.requireRole("Coordinator");
                for (VolunteerShift shift : app.shifts.findAll()) {
                    String label = app.users.findById(shift.getVolunteerId()).getName() + " — " + shift.getShiftDate()
                        + " " + shift.getStartTime() + " to " + shift.getEndTime() + " — " + shift.getAssignedTask();
                    add(choices, shift.getShiftId(), label, search);
                }
                break;
            default:
                throw new IllegalArgumentException("Choose a valid list");
        }
        if (choices.length() == 0) return "<option value=\"\">No matching records available</option>";
        return "<option value=\"\">Choose one</option>" + choices;
    }

    private static void add(StringBuilder options, int id, String label, String search) {
        if (label.toLowerCase(java.util.Locale.ROOT).contains(search)) {
            options.append("<option value=\"").append(id).append("\">").append(escape(label)).append("</option>");
        }
    }

    private static boolean canChange(FoodBankApp app, Distribution distribution, int volunteerId) {
        if (distribution.getVolunteerId() != null && distribution.getVolunteerId() != volunteerId) return false;
        if (distribution.getPickupTime() == null) return false;
        for (VolunteerShift shift : app.shifts.findByVolunteerId(volunteerId)) {
            if (shift.getShiftDate().equals(distribution.getDistributionDate())
                    && !distribution.getPickupTime().isBefore(shift.getStartTime())
                    && distribution.getPickupTime().isBefore(shift.getEndTime())) return true;
        }
        return false;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
