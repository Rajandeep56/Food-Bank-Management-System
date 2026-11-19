package Services;

import java.util.*;

import Repository.CategoryRepository;
import Repository.UserRepository;
import common.RecipientApprovalStatus;
import model.Category;
import model.Coordinator;
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

    public List<Recipient> getPendingRecipients() {
        List<Recipient> pending = new ArrayList<>();
        for (User user : userRepository.findByRole("Recipient")) {
            Recipient recipient = (Recipient) user;
            if (recipient.getApprovalStatus() == RecipientApprovalStatus.PENDING) {
                pending.add(recipient);
            }
        }
        return pending;
    }

    public void approveRecipient(int coordinatorId, int recipientId) {
        reviewRecipient(coordinatorId, recipientId, RecipientApprovalStatus.APPROVED);
    }

    public void rejectRecipient(int coordinatorId, int recipientId) {
        reviewRecipient(coordinatorId, recipientId, RecipientApprovalStatus.REJECTED);
    }

    private void reviewRecipient(int coordinatorId, int recipientId, RecipientApprovalStatus decision) {
        requireCoordinator(coordinatorId);
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
    public Volunteer createVolunteerAccount(int coordinatorId, int volunteerId, String name, String email,
                                            String passwordHash, String phone, String availability) {
        requireCoordinator(coordinatorId);
        Volunteer volunteer = new Volunteer(volunteerId, name, email, passwordHash, phone, availability);
        userRepository.save(volunteer);
        return volunteer;
    }

    // ---------- delete accounts (only coordinators) ----------

    public void deleteUser(int coordinatorId, int userId) {
        requireCoordinator(coordinatorId);
        if (coordinatorId == userId) {
            throw new IllegalArgumentException("Coordinators cannot delete their own account");
        }
        userRepository.delete(userId);
    }

    // ---------- inventory categories ----------

    public Category addCategory(int coordinatorId, int categoryId, String name) {
        requireCoordinator(coordinatorId);
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

    private void requireCoordinator(int userId) {
        User user = userRepository.findById(userId);
        if (!(user instanceof Coordinator)) {
            throw new IllegalStateException("Only coordinators can do this");
        }
    }
}
