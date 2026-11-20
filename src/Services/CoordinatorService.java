package Services;

import java.util.*;

import Repository.CategoryRepository;
import Repository.InventoryRepository;
import Repository.UserRepository;
import common.RecipientApprovalStatus;
import model.Category;
import model.InventoryHistory;
import model.InventoryItem;
import model.Recipient;
import model.User;
import model.Volunteer;

// Coordinator (admin) action
// approve/reject recipients, create volunteer accounts, delete accounts, manage categories
public class CoordinatorService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public CoordinatorService(UserRepository userRepository, CategoryRepository categoryRepository) {
        if (userRepository == null || categoryRepository == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---------- recipient approval ----------

    public List<Recipient> getPendingRecipients(AuthService authService) {
            requireCoordinator(authService);

        List<Recipient> pending = new ArrayList<>();
        for (User user : userRepository.findByRole("Recipient")) {
            Recipient recipient = (Recipient) user;
            if (recipient.getApprovalStatus() == RecipientApprovalStatus.PENDING) {
                pending.add(recipient);
            }
        }
        return pending;
    }

    public void approveRecipient(AuthService authService, int recipientId) {
        reviewRecipient(authService, recipientId, RecipientApprovalStatus.APPROVED);
    }

    public void rejectRecipient(AuthService authService, int recipientId) {
        reviewRecipient(authService, recipientId, RecipientApprovalStatus.REJECTED);
    }

    private void reviewRecipient(AuthService authService, int recipientId, RecipientApprovalStatus decision) {
        requireCoordinator(authService);
        User user = userRepository.findById(recipientId);
        if (!(user instanceof Recipient)) {
            throw new IllegalArgumentException("User " + recipientId + " is not a recipient");
        }
        Recipient recipient = (Recipient) user;
        if (recipient.getApprovalStatus() != RecipientApprovalStatus.PENDING) {
            throw new IllegalStateException("Only pending registrations can be approved or rejected");
        }
        recipient.updateApprovalStatus(decision);
    }

    // ---------- volunteer accounts ----------

    // volunteers cannot sign up themselves, a coordinator creates their account
    public Volunteer createVolunteerAccount(AuthService authService, int volunteerId, String name, String email,
                                            String passwordHash, String phone, String availability) {
        requireCoordinator(authService);
        Volunteer volunteer = new Volunteer(volunteerId, name, email, passwordHash, phone, availability);
        userRepository.save(volunteer);
        return volunteer;
    }

    // ---------- delete accounts (only coordinators) ----------

    public void deleteUser(AuthService authService, int userId) {
        requireCoordinator(authService);
            int coordinatorId = authService.getCurrentUserId();
        if (coordinatorId == userId) {
            throw new IllegalArgumentException("Coordinators cannot delete their own account");
        }
        userRepository.delete(userId);
    }

    // ---------- inventory categories ----------

    public Category addCategory(AuthService authService, int categoryId, String name) {
        requireCoordinator(authService);
        for (Category existing : categoryRepository.findAll()) {
            if (existing.getName().equalsIgnoreCase(name.trim())) {
                throw new IllegalArgumentException("Category " + name + " already exists");
            }
        }
        Category category = new Category(categoryId, name.trim());
        categoryRepository.save(category);
        return category;
    }

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    // ---------- helper ----------


    private void requireCoordinator(AuthService authService) {
        authService.requireRole("Coordinator");
    }

    public void renameCategory( AuthService authService, int categoryId, String name) {
        requireCoordinator(authService);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "Category name cannot be empty"
            );
        }

        categoryRepository.updateCategory(
            categoryId, name.trim()
        );
    }

    public void deleteCategory(AuthService authService, int categoryId, InventoryRepository inventoryRepository,InventoryService inventoryService) {
        requireCoordinator(authService);

        for (InventoryItem item :
                inventoryRepository.getAllInventoryItems()) {

            if (item.getCategoryId() == categoryId) {
                throw new IllegalStateException(
                    "Category is associated with inventory"
                );
            }
        }

        for (InventoryHistory history :
                inventoryService.getHistory()) {

            if (history.getCategoryId() == categoryId) {
                throw new IllegalStateException(
                    "Category has historical records"
                );
            }
        }

        categoryRepository.deleteCategory(categoryId);
    }

    public void updateUser(
        AuthService authService,
        int userId,
        String name,
        String phone
    ) {
       requireCoordinator(authService);

        User user = userRepository.findById(userId);

        user.updateProfile(name, user.getEmail(), phone);
    }

    public void updateVolunteerAvailability(
        AuthService authService,
        int volunteerId,
        String availability
    ) {
       requireCoordinator(authService);


        User user = userRepository.findById(volunteerId);

        if (!(user instanceof Volunteer)) {
            throw new IllegalArgumentException(
                "User is not a volunteer"
            );
        }

        Volunteer volunteer = (Volunteer) user;

        volunteer.setAvailability(availability);
    }
}
