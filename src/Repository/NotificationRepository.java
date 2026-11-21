package Repository;

import java.util.*;

import model.Notification;

public class NotificationRepository {
    private final Map<Integer, Notification> notifications = new HashMap<>();
    private int nextId = 1;

    public int nextId() {
        return nextId++;
    }

    public void save(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }
        if (notifications.containsKey(notification.getNotificationId())) {
            throw new IllegalArgumentException("Notification with this ID already exists");
        }
        notifications.put(notification.getNotificationId(), notification);
        nextId = Math.max(nextId, notification.getNotificationId() + 1);
    }

    public List<Notification> findAll() {
        return new ArrayList<>(notifications.values());
    }

    public List<Notification> findByRecipientUserId(int userId) {
        List<Notification> result = new ArrayList<>();
        for (Notification n : notifications.values()) {
            if (n.getRecipientUserId() == userId) {
                result.add(n);
            }
        }
        return result;
    }
}