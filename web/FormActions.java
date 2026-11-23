package web;

import java.time.*;
import java.util.Map;
import Services.*;
import model.*;
import common.ReportPeriodType;

class FormActions {
    static String submit(FoodBankApp app, WebSession session, Map<String, String> form) {
        AuthService auth = session.auth;
        String action = required(form, "action");
        if (!action.equals("login") && !action.equals("register") && !action.equals("logout")) {
            auth.getCurrentUser();
        }
        switch (action) {
            case "login":
                auth.login(required(form, "email"), required(form, "password"));
                return "Logged in.";
            case "logout":
                auth.logout();
                return "Logged out.";
            case "register":
                String email = required(form, "email").trim();
                validateWebEmail(email);
                String role = required(form, "role");
                User newUser;
                if (role.equals("Donor")) {
                    newUser = new Donor(app.nextUserId(), required(form, "name"), email,
                        required(form, "password"), required(form, "phone"));
                } else if (role.equals("Recipient")) {
                    newUser = new Recipient(app.nextUserId(), required(form, "name"), email,
                        required(form, "password"), required(form, "phone"), number(form, "householdSize"),
                        form.get("diet"), required(form, "contact"));
                } else {
                    throw new SecurityException("Only donors and recipients can sign up here");
                }
                auth.register(newUser);
                return "Account created. Log in to continue. Recipients need coordinator approval before requesting a hamper.";
            case "profile":
                User profile = auth.getCurrentUser();
                String name = form.getOrDefault("name", "");
                String profileEmail = form.getOrDefault("email", "");
                String phone = form.getOrDefault("phone", "");
                if (name.isBlank() && profileEmail.isBlank() && phone.isBlank()) {
                    throw new IllegalArgumentException("Enter a name, email, or phone number to update.");
                }
                if (name.isBlank()) name = profile.getName();
                if (profileEmail.isBlank()) profileEmail = profile.getEmail();
                if (phone.isBlank()) phone = profile.getPhone();
                auth.updateProfile(name, profileEmail, phone);
                return "Profile saved.";
            case "password":
                auth.changePassword(required(form, "oldPassword"), required(form, "newPassword"));
                return "Password changed.";
            case "recipientProfile":
                auth.updateRecipientProfile(number(form, "householdSize"), form.get("diet"), required(form, "contact"));
                return "Household details saved.";
            case "requestHamper":
                FoodHamperRequest request = app.hamperService.requestHamper(app.nextRequestId(), auth,
                    date(form, "date"), time(form, "time"));
                return "Hamper request #" + request.getRequestId() + " scheduled.";
            case "cancelRequest":
                app.hamperService.cancelRequest(number(form, "requestId"), auth);
                return "Request cancelled.";
            case "reviewRecipient":
                String decision = required(form, "decision");
                if (decision.equals("approve")) {
                    app.coordinatorService.approveRecipient(auth, number(form, "userId"));
                } else if (decision.equals("reject")) {
                    app.coordinatorService.rejectRecipient(auth, number(form, "userId"));
                } else {
                    throw new IllegalArgumentException("Choose approve or reject");
                }
                return "Recipient registration reviewed.";
            case "createVolunteer":
                validateWebEmail(required(form, "email"));
                app.coordinatorService.createVolunteerAccount(auth, app.nextUserId(), required(form, "name"),
                    required(form, "email"), required(form, "password"), required(form, "phone"), required(form, "availability"));
                return "Volunteer account created.";
            case "updateUser":
                app.coordinatorService.updateUser(auth, number(form, "userId"), required(form, "name"), required(form, "phone"));
                return "Account saved.";
            case "availability":
                app.coordinatorService.updateVolunteerAvailability(auth, number(form, "userId"), required(form, "availability"));
                return "Availability saved.";
            case "deactivateUser":
                app.coordinatorService.deleteUser(auth, number(form, "userId"));
                return "Account deactivated. History retained.";
            case "assignShift":
                auth.requireRole("Coordinator");
                app.shiftService.assignShift(new VolunteerShift(app.nextShiftId(), number(form, "volunteerId"),
                    date(form, "date"), time(form, "start"), time(form, "end"), required(form, "task"), auth.getCurrentUserId()), auth);
                return "Shift assigned.";
            case "deleteShift":
                app.shiftService.deleteShift(number(form, "shiftId"), auth);
                return "Shift removed.";
            case "addCategory":
                app.coordinatorService.addCategory(auth, app.nextCategoryId(), required(form, "name"));
                return "Category added.";
            case "renameCategory":
                app.coordinatorService.renameCategory(auth, number(form, "categoryId"), required(form, "name"));
                return "Category renamed.";
            case "deleteCategory":
                app.coordinatorService.deleteCategory(auth, number(form, "categoryId"), app.inventory, app.stock);
                return "Unused category deleted.";
            case "recordDonation":
                auth.requireRole("Volunteer");
                User donor = app.users.findById(number(form, "donorId"));
                if (!donor.isActive() || !donor.getRole().equals("Donor")) {
                    throw new IllegalArgumentException("Select an active donor");
                }
                app.categories.findById(number(form, "categoryId"));
                Donation donation = new Donation(app.nextDonationId(), donor.getUserId(), auth.getCurrentUserId(),
                    required(form, "name"), number(form, "categoryId"), number(form, "quantity"), required(form, "unit"));
                InventoryItem intake = new InventoryItem(app.nextInventoryId(), donation.getItemName(), donation.getCategoryId(),
                    donation.getQuantity(), donation.getUnit(), optionalDate(form.get("expiry")), donation.getDonationId());
                app.donationService.createDonation(donation, auth);
                try {
                    app.donationService.completeDonation(donation.getDonationId(), intake, auth);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Donation #" + donation.getDonationId()
                        + " was saved, but inventory was not updated. Use Retry a pending donation.", e);
                }
                return "Donation recorded and inventory updated.";
            case "completeDonation":
                auth.requireRole("Volunteer");
                Donation pending = app.donationService.getDonationById(number(form, "donationId"));
                app.donationService.completeDonation(pending.getDonationId(), new InventoryItem(app.nextInventoryId(),
                    pending.getItemName(), pending.getCategoryId(), pending.getQuantity(), pending.getUnit(),
                    optionalDate(form.get("expiry")), pending.getDonationId()), auth);
                return "Donation completed.";
            case "addInventory":
                auth.requireRole("Volunteer");
                app.categories.findById(number(form, "categoryId"));
                app.stock.addInventoryItem(new InventoryItem(app.nextInventoryId(), required(form, "name"),
                    number(form, "categoryId"), number(form, "quantity"), required(form, "unit"), optionalDate(form.get("expiry")), 0), auth);
                return "Inventory added.";
            case "inventoryQuantity":
                app.stock.updateInventoryQuantity(number(form, "inventoryId"), number(form, "quantity"), auth);
                return "Inventory quantity saved.";
            case "inventoryDetails":
                app.stock.updateInventoryDetails(number(form, "inventoryId"), required(form, "name"), optionalDate(form.get("expiry")), auth);
                return "Inventory details saved.";
            case "deleteInventory":
                app.stock.deleteInventory(number(form, "inventoryId"), auth);
                return "Inventory removed. History retained.";
            case "addDistributionItem":
                auth.requireRole("Volunteer");
                InventoryItem stock = app.inventory.getInventoryItemById(number(form, "inventoryId"));
                app.distributionService.addDistributionItem(number(form, "distributionId"), new DistributionItem(
                    app.nextDistributionItemId(), stock.getInventoryId(), number(form, "quantity"), stock.getUnit()), auth);
                return "Distribution item added.";
            case "editDistributionItem":
                String operation = required(form, "operation");
                if (operation.equals("remove")) {
                    app.distributionService.removeDistributionItem(number(form, "distributionId"), number(form, "inventoryId"), auth);
                } else if (operation.equals("update")) {
                    app.distributionService.updateDistributionItem(number(form, "distributionId"), number(form, "inventoryId"), number(form, "quantity"), auth);
                } else {
                    throw new IllegalArgumentException("Choose update or remove");
                }
                return "Distribution item saved.";
            case "pickupStatus":
                int distributionId = number(form, "distributionId");
                switch (required(form, "status")) {
                    case "FULFILLED": app.distributionService.fulfillDistribution(distributionId, auth); break;
                    case "MISSED": app.distributionService.markMissedPickup(distributionId, auth); break;
                    case "DELAYED": app.distributionService.markDelayedPickup(distributionId, auth); break;
                    default: throw new IllegalArgumentException("Invalid pickup status");
                }
                return "Pickup marked " + required(form, "status") + ".";
            case "deleteDistribution":
                app.distributionService.deleteDistribution(number(form, "distributionId"), auth);
                return "Scheduled distribution deleted.";
            case "report":
                auth.requireRole("Coordinator");
                Report report;
                String reportType = required(form, "reportType");
                if (reportType.equals("donations")) {
                    report = new DonationDistributionReport(app.donations, app.distributions, app.stock,
                        app.requests, app.users, app.categories);
                } else if (reportType.equals("waste")) {
                    report = new InventoryWasteReport(app.inventory, app.notifications, app.stock, app.categories);
                } else {
                    throw new IllegalArgumentException("Select a report type");
                }
                new ReportService(app.users).generateReport(auth, report, ReportPeriodType.valueOf(required(form, "period")),
                    number(form, "year"), number(form, "month"));
                session.report = report;
                return "Report generated.";
            default:
                throw new IllegalArgumentException("Unknown form action");
        }
    }

    private static void validateWebEmail(String email) {
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
    }

    private static String required(Map<String, String> form, String name) {
        String value = form.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Please fill in " + name);
        return value;
    }

    private static int number(Map<String, String> form, String name) {
        try { return Integer.parseInt(required(form, name)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a whole number for " + name); }
    }

    private static LocalDate optionalDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private static LocalDate date(Map<String, String> form, String name) {
        return LocalDate.parse(required(form, name));
    }

    private static LocalTime time(Map<String, String> form, String name) {
        return LocalTime.parse(required(form, name));
    }

}
