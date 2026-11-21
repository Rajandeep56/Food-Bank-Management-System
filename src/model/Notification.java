package model;

import java.time.Instant;

import common.NotificationType;

// one alert sent to one person (coordinator or volunteer)
public class Notification {
    private final int notificationId;
    private final NotificationType type;
    private final int recipientUserId;
    private final String recipientEmail;
    private final String message;
    private  Instant createdAt;
    private boolean sent;
    private String reason;

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public boolean isExpiryAlert() {
        return reason != null && (reason.contains(":Expiry:") || reason.equals("ExpiryRisk") || reason.equals("Expiry"));
    }

    public Notification(int notificationId, NotificationType type, int recipientUserId, String recipientEmail, String message) {
        if (notificationId <= 0 || type == null || recipientUserId <= 0) {
            throw new IllegalArgumentException("Invalid notification parameters");
        }
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and message cannot be null or empty");
        }
        this.notificationId = notificationId;
        this.type = type;
        this.recipientUserId = recipientUserId;
        this.recipientEmail = recipientEmail;
        this.message = message;
        this.createdAt = Instant.now();
        this.sent = false;
    }

    public int getNotificationId() {
        return notificationId;
    }
    public NotificationType getType() {
        return type;
    }
    public int getRecipientUserId() {
        return recipientUserId;
    }
    public String getRecipientEmail() {
        return recipientEmail;
    }
    public String getMessage() {
        return message;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
    public boolean isSent() {
        return sent;
    }
    public void markSent() {
        this.sent = true;
    }

    public void restoreFromDatabase(Instant createdAt, boolean sent) {
        this.createdAt = createdAt;
        this.sent = sent;
    }
}